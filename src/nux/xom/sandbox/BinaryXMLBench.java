/*
 * Copyright (c) 2005, The Regents of the University of California, through
 * Lawrence Berkeley National Laboratory (subject to receipt of any required
 * approvals from the U.S. Dept. of Energy). All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * (2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * (3) Neither the name of the University of California, Lawrence Berkeley
 * National Laboratory, U.S. Dept. of Energy nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * You are under no obligation whatsoever to provide any bug fixes, patches, or
 * upgrades to the features, functionality or performance of the source code
 * ("Enhancements") to anyone; however, if you choose to make your Enhancements
 * available either publicly, or directly to Lawrence Berkeley National
 * Laboratory, without imposing a separate written license agreement for such
 * Enhancements, then you hereby grant the following license: a non-exclusive,
 * royalty-free perpetual license to install, use, modify, prepare derivative
 * works, incorporate into other computer software, distribute, and sublicense
 * such enhancements or derivative works thereof, in binary and source code
 * form.
 */
package nux.xom.sandbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.query.StaticQueryContext;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.NodeFactory;
import nu.xom.Serializer;
import nu.xom.converters.SAXConverter;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.io.StaxParser;
import nux.xom.io.StaxUtil;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;
import nux.xom.pool.BuilderFactory;
import nux.xom.pool.BuilderPool;
import nux.xom.pool.FileUtil;
import nux.xom.pool.XOMUtil;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Extensively benchmarks bnux and other parsing/serialization models;
 * to be run via build.xml ant tasks.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.11 $, $Date: 2006/05/07 05:52:09 $
 */
public final class BinaryXMLBench {
	
//	private static final int minWarmupTime = 20;
//	private static final int minMeasureTime = 30;
//	private static final int minWarmupTime = 1;
//	private static final int minMeasureTime = 1;
	
	private static final int minWarmupTime = 10;
	private static final int minMeasureTime = 10;

	
	// bnux and XOM
	private NodeFactory bnuxFactory;
	private BinaryXMLCodec codec;
	private Builder builder;
	
	// saxon
	private StaticQueryContext context;
	private Transformer saxonSerializer;
	private NodeInfo saxonDoc;
	
	// DOM
	private DocumentBuilder domBuilder;
	private Transformer domSerializer;
	private org.w3c.dom.Document domDoc;
	
	// FastInfoSet
	private Object fiSerializer;
	private Builder fiBuilder;
	private Method fiMethod;
	
	private XMLStreamReader fistaxReader;
	private java.lang.reflect.Method fistaxMethod;

	
	// StAX
	private Builder staxBuilder;
	private XMLInputFactory staxInputFactory;
	private XMLOutputFactory staxOutputFactory;
	
	// all models:
	private File file;
	private String cmd; // ser|deser|serdeser|test
	private boolean isDeserCmd;
	private String mode; // bnux|xom|saxon|dom|fi
	private int compressionLevel; // 0..9
	private byte[] data;
	private byte[] fileData;
	private Document doc;
	private int checksum = 0; // prevents hotspot VM from optimizing away dead code
		
	private BinaryXMLBench() {}
	
	// called once per file
	private void prepare() throws Exception {
		data = null;
		doc = null;
		fileData = null;
		fileData = FileUtil.toByteArray(new FileInputStream(file));
		
		if (mode.startsWith("bnux")) {
			doc = new Builder().build(new ByteArrayInputStream(fileData));
			data = new BinaryXMLCodec().serialize(doc, compressionLevel);
			if (!cmd.equals("deser")) {
				doc = codec.deserialize(data); // use "interned" strings
				data = null;
			}
			if (cmd.equals("deser")) {
				doc = null;
			}
			fileData = null;
		} 

		if (mode.startsWith("xom")) {
			if (!cmd.equals("deser")) {
				doc = new Builder().build(new ByteArrayInputStream(fileData));
			}
		}
		
		domDoc = null;
		if (mode.equals("dom")) {
			if (!cmd.equals("deser")) {
				domDoc = domBuilder.parse(new ByteArrayInputStream(fileData));
			}
		}
		
		saxonDoc = null;
		if (mode.equals("saxon")) {
			if (!cmd.equals("deser")) {
				saxonDoc = context.buildDocument(new StreamSource(new ByteArrayInputStream(fileData)));
			}
		}

		if (mode.startsWith("fi")) {
			doc = new Builder().build(new ByteArrayInputStream(fileData));
			if (cmd.equals("deser")) { 
				if (mode.indexOf("stax") >= 0) {
//					data = serializeWithStax(doc, staxOutputFactory);
					data = serializeWithFastInfosetStax(doc, (XMLStreamWriter)fiSerializer, fiMethod, new ByteArrayOutputStream());
				} else {
					data = serializeWithFastInfoset(doc, (ContentHandler)fiSerializer, fiMethod, new ByteArrayOutputStream());
				}
				doc = null;
			}
			fileData = null;
		}
		
		if (!cmd.equals("deser")) {
			fileData = null;
		}
		
		System.gc();
	}
	
	// the heart of the benchmark; called N times per file
	private void run() throws Exception {
		// serialize
		if (!isDeserCmd && (cmd.equals("ser") || cmd.equals("serdeser") || cmd.equals("test"))) {
			data = null;
			ByteArrayOutputStream out = createOutputStream(cmd.equals("ser"));
			if (mode.startsWith("bnux")) {				
				codec.serialize(doc, compressionLevel, out);
				data = out.toByteArray();
			} else if (mode.startsWith("xom")) {
				if (mode.indexOf("stax") >= 0) {
					data = serializeWithStax(doc, staxOutputFactory, out);
				} else {
					data = serializeWithXOM(doc, out);
				}
			} else if (mode.equals("saxon")) {
				saxonSerializer.transform(saxonDoc, new StreamResult(out));
				data = out.toByteArray();
			} else if (mode.equals("dom")) {
				domSerializer.transform(new DOMSource(domDoc), new StreamResult(out));
				data = out.toByteArray();
			} else if (mode.startsWith("fi")) {
				if (mode.indexOf("stax") >= 0) {
//					data = serializeWithStax(doc, staxOutputFactory);
					data = serializeWithFastInfosetStax(doc, (XMLStreamWriter)fiSerializer, fiMethod, out);
				} else {
					data = serializeWithFastInfoset(doc, (ContentHandler)fiSerializer, fiMethod, out);
				}
			} else {
				throw new IllegalArgumentException("illegal mode");
			}
			checksum += data.length;
		}
		
		// deserialize
		Document doc2 = null;
		if (isDeserCmd || cmd.equals("serdeser") || cmd.equals("test")) {
			if (mode.startsWith("bnux")) {
				doc2 = codec.deserialize(new ByteArrayInputStream(data), bnuxFactory);
			} else if (mode.startsWith("xom") && mode.indexOf("stax") >= 0) { 
//				XMLStreamReader reader = staxFactory.createXMLStreamReader(new ByteArrayInputStream(fileData));
				doc2 = StaxUtil.createBuilder(staxInputFactory, staxBuilder.getNodeFactory())
					.build(new ByteArrayInputStream(fileData));
//				doc2 = staxBuilder.build(staxreader);					
			} else if (mode.startsWith("xom")) {
				doc2 = builder.build(new ByteArrayInputStream(fileData));								
			} else if (mode.equals("saxon")) { // just for deser comparison
				context.buildDocument(new StreamSource(new ByteArrayInputStream(fileData)));
			} else if (mode.equals("dom")) {
				domDoc = null;
				domDoc = domBuilder.parse(new ByteArrayInputStream(fileData));
//					System.err.println(domDoc.getClass().getName());
			} else if (mode.startsWith("fi") && mode.indexOf("stax") >= 0) {
				fistaxMethod.invoke(fistaxReader, new Object[] {new ByteArrayInputStream(data)});
				doc2 = new StaxParser(fistaxReader, staxBuilder.getNodeFactory()).build();
			} else if (mode.startsWith("fi")) {
//				NodeFactory factory = null;
//				if (mode.equals("fi0-NNF")) factory = XOMUtil.getNullNodeFactory();
//				XMLReader parser = (XMLReader) Class.forName("com.sun.xml.fastinfoset.sax.SAXDocumentParser").newInstance();
//				fiBuilder = new Builder(parser, false, factory);
				doc2 = fiBuilder.build(new ByteArrayInputStream(data));
			} else {
				throw new IllegalArgumentException("illegal mode");
			}							
			
			if (doc2 != null) checksum += doc2.getBaseURI().length();				
		}			
	}
	
	// called once per mode (i.e. VM invocation)
	private void init() throws Exception {	 // TODO: merge with constructor?
		// bnux and XOM
		codec = new BinaryXMLCodec();
		bnuxFactory = null;
		if (mode.startsWith("bnux")) {
			if (mode.indexOf("NNF") >= 0) {
				bnuxFactory = XOMUtil.getNullNodeFactory();
			}
		}
		
		builder = new Builder();
		if (mode.indexOf("pool") >= 0) {
			builder = BuilderPool.GLOBAL_POOL.getBuilder(false);
		}
		
		if (mode.equals("xom-V")) {
			builder = new Builder(new NodeFactory() {});
		} else if (mode.equals("xom-V-pool")) {
			builder = new BuilderFactory() {
				protected Builder newBuilder(XMLReader parser, boolean validate) {
					return new Builder(parser, false, new NodeFactory() {}); 		
				}
			}.createBuilder(false);
		}
		
		if (mode.equals("xom-NNF")) {
			builder = new Builder(XOMUtil.getNullNodeFactory());
		} else if (mode.equals("xom-NNF-pool")) {
			builder = new BuilderFactory() {
				protected Builder newBuilder(XMLReader parser, boolean validate) {
					return new Builder(parser, false, XOMUtil.getNullNodeFactory()); 		
				}
			}.createBuilder(false);
		}
		
		// saxon
		context = null;
		saxonSerializer = null;
		if (mode.equals("saxon")) {
			context = new StaticQueryContext(new Configuration());
			saxonSerializer = createIdentityTransform(
					new String[] {"net.sf.saxon.TransformerFactoryImpl"});
		}
		
		// DOM
		domBuilder = null;
		domSerializer = null;
		if (mode.equals("dom")) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			try {
				factory.setAttribute("http://apache.org/xml/features/dom/defer-node-expansion", Boolean.FALSE);
			} catch (IllegalArgumentException e) {
				// crimson does not implement this attribute
			}
			domBuilder = factory.newDocumentBuilder();
			domSerializer = createIdentityTransform(new String[] {
					"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", 
					"org.apache.xalan.processor.TransformerFactoryImpl"});
			System.err.println(domSerializer.getClass().getName());
		}
		
		// FastInfoSet
		fiBuilder = null;	
		fiSerializer = null;
		fiMethod = null;
		if (mode.startsWith("fi")) {
			NodeFactory factory = null;
			if (mode.indexOf("NNF") >= 0) factory = XOMUtil.getNullNodeFactory();
			XMLReader parser = (XMLReader) Class.forName("com.sun.xml.fastinfoset.sax.SAXDocumentParser").newInstance();
			fiBuilder = new Builder(parser, false, factory);
			
			if (mode.indexOf("stax") >= 0) {
				fiSerializer = (XMLStreamWriter) Class.forName("com.sun.xml.fastinfoset.stax.StAXDocumentSerializer").newInstance();				
			} else {
				fiSerializer = (ContentHandler) Class.forName("com.sun.xml.fastinfoset.sax.SAXDocumentSerializer").newInstance();
			}
			
			if (mode.startsWith("fi1")) { // enable "full indexing"
				Method method;
				method = fiSerializer.getClass().getMethod(
						"setAttributeValueSizeLimit", new Class[] {Integer.TYPE});
				method.invoke(fiSerializer, new Object[] {new Integer(Integer.MAX_VALUE)});
				method = fiSerializer.getClass().getMethod(
						"setCharacterContentChunkSizeLimit", new Class[] {Integer.TYPE});
				method.invoke(fiSerializer, new Object[] {new Integer(Integer.MAX_VALUE)});
			}
			
			fiMethod = fiSerializer.getClass().getMethod(
					"setOutputStream", new Class[] { OutputStream.class});
			
			if (mode.indexOf("stax") >= 0) {
				fistaxReader = (XMLStreamReader) Class.forName("com.sun.xml.fastinfoset.stax.StAXDocumentParser").newInstance();
				fistaxMethod = fistaxReader.getClass().getMethod(
						"setInputStream", new Class[] { InputStream.class});
			}
		}		
		
		// StAX
		staxBuilder = null;
		if (mode.indexOf("stax") >= 0) {
			NodeFactory factory = null;
			if (mode.indexOf("NNF") >= 0) factory = XOMUtil.getNullNodeFactory();
			staxBuilder = StaxUtil.createBuilder(staxInputFactory, factory);
		}	
	}
	
	// called once per file
	private float runBenchmarks(long minWarmupTime, long minMeasureTime) throws Exception {
		// prepare just once, avoiding repeated overheads:
		prepare();
		
		// warmup:
		System.out.println("now warming up...");
		long start = System.currentTimeMillis();
		long end;
		int iters = 0;
		do {
			run();
			iters++;
			end = System.currentTimeMillis();
		} while (end-start < minWarmupTime);

		float secs = (end-start) / 1000.0f;
		System.gc();
		System.out.println("warmup:   secs=" + secs + ", iters/sec=" + (iters/secs));
	
		// measurement run:
		// uses calibration to minimize System.currentTimeMillis() overheads and inaccuraries
		int limit = Math.max(1, (int) (iters/secs/10));
		iters = 0;
		start = System.currentTimeMillis();
		do {
			for (int i=0; i < limit; i++) {
				run(); // do the real work
				iters++;
			}
			end = System.currentTimeMillis();
		} while (end-start < minMeasureTime);
		
		secs = (end-start) / 1000.0f;
		System.out.println("measured: secs=" + secs + ", iters/sec=" + (iters/secs));
		
		return iters/secs; // report mean of runs
	}
	
	public static void main(final String args[]) throws Exception {
		int k = 0;
		String cmd = args[k++]; // ser|deser|serdeser|test
		String mode = args[k++]; // bnux|xom|saxon|dom|fi
		int compressionLevel = 0;
		if (mode.startsWith("bnux")) {
			try {
				String s = mode.substring("bnux".length());
				int i=0;
				while (i < s.length() && Character.isDigit(s.charAt(i))) {
					i++;
				}
				compressionLevel = Integer.parseInt(s.substring(0, i));
			} catch (NumberFormatException e) {
				compressionLevel = 0;
			}
			System.out.println("compressionLevel=" + compressionLevel);
		}
		
		if (mode.equals("bnux0-NV")) { // init before BinaryXMLCodec
			// temporary (?) performance hack via patch: disable some expensive sanity checks 
			System.setProperty("nu.xom.Verifier.checkPCDATA", "false");
			System.setProperty("nu.xom.Verifier.checkURI", "false");
			System.out.println("patchesEnabled=true");
		}
		
		XMLInputFactory staxInputFactory = null;
		if (mode.indexOf("stax") >= 0 && mode.indexOf("fi") < 0) {
			if (mode.indexOf("sun") >= 0) {
				staxInputFactory = (XMLInputFactory) Class.forName("com.sun.xml.stream.ZephyrParserFactory").newInstance();
//				System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.stream.ZephyrParserFactory");
//				System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.stream.ZephyrWriterFactory");
//				System.setProperty("javax.xml.stream.XMLEventFactory", "com.sun.xml.stream.events.ZephyrEventFactory");
			} else if (mode.indexOf("bea") >= 0) {
				staxInputFactory = (XMLInputFactory) Class.forName("com.bea.xml.stream.MXParserFactory").newInstance();
			} else if (mode.indexOf("wood") >= 0) {
				staxInputFactory = (XMLInputFactory) Class.forName("com.ctc.wstx.stax.WstxInputFactory").newInstance();
			}
		}
		
		XMLOutputFactory staxOutputFactory = null;
		if (mode.indexOf("stax") >= 0) {
			staxOutputFactory = createXMLOutputFactory(mode);
		}
		
		File outputDir = new File(args[k++]);
		
		URI[] uris = FileUtil.listFiles(args[k++], false, "*.xml *.xsl", "");
		Arrays.sort(uris, new Comparator() {
			public int compare(Object o1, Object o2) {
				URI u1 = (URI) o1;
				URI u2 = (URI) o2;
				return (int) (new File(u1).length() - new File(u2).length());
			}			
		});
//		System.out.println("files=" + Arrays.asList(uris));
				
		BinaryXMLBench bench = new BinaryXMLBench();
		bench.cmd = cmd; 
		bench.isDeserCmd = cmd.equals("deser");
		bench.mode = mode;
		bench.compressionLevel = compressionLevel;
		bench.staxInputFactory = staxInputFactory;
		bench.staxOutputFactory = staxOutputFactory;
		bench.init();				
		XMLMatrix matrix = new XMLMatrix(new File(outputDir, bench.cmd + ".xml"), true);			

		for (k = 0; k < uris.length; k++) {
			bench.file = new File(uris[k]);
			if (bench.file.isDirectory()) continue;
			System.out.println("\nnow processing " + bench.file);
			
			double itersPerSec = 0.0;
			try {
				itersPerSec = bench.runBenchmarks(1000 * minWarmupTime, 1000 * minMeasureTime);
			} catch (Exception e) {
				if (hasCause(e, "org.jvnet.fastinfoset.FastInfosetException", "ParseError at [row,col]")) {
					; // FI bug; account as 0 MB/s and continue unbothered
					e.printStackTrace(System.out);					
				} else {
					e.printStackTrace(System.out);
					return;
//					throw e;
				}
			}
			
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			format.setMaximumFractionDigits(1);		
			
			double mbs = itersPerSec * bench.file.length() / (1024*1024);
			double kb =  bench.file.length() / 1024.0f;
			matrix.put(bench.file.getName(), bench.mode, format.format(mbs));
			matrix.put(bench.file.getName(), "XMLsize", format.format(kb));
			if (cmd.equals("ser")) {
				matrix.put(bench.file.getName(), mode + "-csize", format.format(bench.data.length / 1024.0));
			}
			System.out.println("MB/s=" + mbs);
		}
		
		System.out.println("matrix=" + matrix);
		matrix.saveAsXML();
		matrix.saveAsCSV();
		System.out.println("checksum=" + bench.checksum);
	}
	
	private static byte[] serializeWithXOM(Document doc, ByteArrayOutputStream out) throws IOException {
//		return doc.toXML().getBytes();
		Serializer ser = new Serializer(out);
//		ser.setIndent(4);
		ser.write(doc);
		return out.toByteArray();
	}
	
	private static byte[] serializeWithFastInfoset(Document doc, ContentHandler fiSerializer, java.lang.reflect.Method fiMethod, ByteArrayOutputStream out) throws Exception {
		// work-around to avoid making FastInfoSet.jar a hard dependency.
		// for best performance on very small documents reflection should better not be used...
//		fiSerializer = (ContentHandler) Class.forName("com.sun.xml.fastinfoset.sax.SAXDocumentSerializer").newInstance();
		try {
			fiMethod.invoke(fiSerializer, new Object[] {out});
		} catch (Throwable t) {
			throw new Error(t);
		}
		
		new SAXConverter(fiSerializer).convert(doc);
		return out.toByteArray();
	}
	
	private static byte[] serializeWithFastInfosetStax(Document doc, XMLStreamWriter fiSerializer, Method method, ByteArrayOutputStream out) throws Exception {
		// work-around to avoid making FastInfoSet.jar a hard dependency.
		// for best performance on very small documents reflection should better not be used...
		try {
			method.invoke(fiSerializer, new Object[] {out});
		} catch (Throwable t) {
			throw new Error(t);
		}
		
		new StreamingSerializerFactory().createStaxSerializer(fiSerializer).write(doc);
		return out.toByteArray();
	}
	
	private static byte[] serializeWithStax(Document doc, XMLOutputFactory staxOutputFactory, ByteArrayOutputStream out) throws XMLStreamException, IOException {
		XMLStreamWriter writer = staxOutputFactory.createXMLStreamWriter(out, "UTF-8");
		StreamingSerializer ser = new StreamingSerializerFactory().createStaxSerializer(writer);
		ser.write(doc);
		return out.toByteArray();
	}
	
	private static XMLOutputFactory createXMLOutputFactory(String mode) {
		if (mode.indexOf("sun") >= 0) {
			System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.stream.ZephyrWriterFactory");
		} else if (mode.indexOf("bea") >= 0) {
			System.setProperty("javax.xml.stream.XMLOutputFactory", "com.bea.xml.stream.XMLOutputFactoryBase");
		} else if (mode.indexOf("wood") >= 0) {
			System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory");
		} else if (mode.indexOf("fi") >= 0) {
			System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.fastinfoset.stax.factory.StAXOutputFactory");
		}
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		System.out.println("outFactory=" + factory.getClass().getName());
		return factory;
	}
	
	private static Transformer createIdentityTransform(String[] clazzes) throws Exception {
		Exception t = null;
		for (int i=0; i < clazzes.length; i++) {
			if (clazzes[i] != null) {
				System.setProperty("javax.xml.transform.TransformerFactory", clazzes[i]);
			}
			try {
				Transformer trans = TransformerFactory.newInstance().newTransformer();
				System.out.println("idTransform = " + trans.getClass().getName());
				return trans;
			} catch (TransformerConfigurationException e) {
				t = e; // keep on trying
			} catch (TransformerFactoryConfigurationError e) {
				t = new RuntimeException(e); // keep on trying
			}
		}
		throw t;
	}
	
	private static boolean hasCause(Throwable e, String clazz, String msg) {
		while (e != null) {
//			System.out.println("chain="+ e.getClass().getName());
			if (e.getClass().getName().equals(clazz)) return true;
			if (msg != null && e.getMessage() != null && 
					e.getMessage().indexOf(msg) >= 0) return true;
			// walk exception cause chain:
			if (e instanceof SAXException) {
				e = ((SAXException)e).getException();
			} else if (e instanceof XMLStreamException) {
				e = ((XMLStreamException)e).getNestedException();
			} else {
				e = e.getCause();
			}
		}
		return false;
	}
	
	public static ByteArrayOutputStream createOutputStream(boolean nullStream) {
		return nullStream ? new NullOutputStream() : new ByteArrayOutputStream(256); 
	}
	
	// throws away all data, but ensures that hotspot VM can't optimize away dead code.
	private static final class NullOutputStream extends ByteArrayOutputStream {

		public void write(int b) {
			count++;
		}
		
		public void write(byte b[], int off, int len) {
			count += off + len;
			if (len > 0) count += b[len-1];
		}
		
		public byte[] toByteArray() {
			return new byte[] {(byte)count};
		}
		
		public void close() {
			count = 0;
		}
	}
				
}