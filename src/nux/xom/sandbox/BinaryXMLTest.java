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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.query.StaticQueryContext;
import nu.xom.Builder;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.NodeFactory;
import nu.xom.Serializer;
import nu.xom.converters.SAXConverter;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.io.StaxParser;
import nux.xom.io.StaxUtil;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;
import nux.xom.pool.FileUtil;
import nux.xom.pool.XOMUtil;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Tests and benchmarks bnux format against a set of standard textual XML files.
 * Command line options allow to round-trip and check canonical XML results.
 * They also allow to benchmark serialization, deserialization or both, 
 * either via bnux or xom.
 * <p>
 * Example usage: 
 * <pre>
 * java -server nux.xom.sandbox.BinaryXMLTest test bnux 0 1 1 samples/data/*.xml
 * java -server nux.xom.sandbox.BinaryXMLTest ser bnux 0 10000 5 samples/data/periodic.xml
 * java -server nux.xom.sandbox.BinaryXMLTest deser bnux 0 10000 5 samples/data/periodic.xml
 * java -server nux.xom.sandbox.BinaryXMLTest serdeser bnux 0 5000 5 samples/data/periodic.xml
 * java -server nux.xom.sandbox.BinaryXMLTest serdeser xom 0 5000 5 samples/data/periodic.xml
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.14 $, $Date: 2006/05/07 05:52:09 $
 */
public final class BinaryXMLTest extends IOTest {
	
	private BinaryXMLTest() {}
	
	public static void main(final String args[]) throws Exception {
		
		final String cmd = args[0]; // ser|deser|serdeser|test
		final String mode = args[1]; // bnux|xom|saxon|dom|fi
		int compressionLevel = Integer.parseInt(args[2]); // 0..9
		final int iterations = Integer.parseInt(args[3]); // 1..infinity
		final int runs = Integer.parseInt(args[4]); // 1..infinity
		final boolean enablePerformancePatches = "true".equals(
			System.getProperty("nux.xom.sandbox.BinaryXMLTest.enablePatches", "true"));
		System.out.println("patchesEnabled=" + enablePerformancePatches);
		final boolean testCompressionLevels = false;

		for (int run=0; run < runs; run++) { // make k large to stress test for a couple of hours 
			long time = 0;
			long doneEncoded = 0;
			long done = 0;
			int checksum = 0;
			
			if (enablePerformancePatches) { // init before BinaryXMLCodec
				// temporary (?) performance hack via patch: disable some expensive sanity checks 
				System.setProperty("nu.xom.Verifier.checkPCDATA", "false");
				System.setProperty("nu.xom.Verifier.checkURI", "false");
			}
			
			XMLInputFactory staxInputFactory = null;
			if (mode.indexOf("stax") >= 0 && mode.indexOf("fi") < 0) {
				if (mode.indexOf("sun") >= 0) {
					staxInputFactory = (XMLInputFactory) Class.forName("com.sun.xml.stream.ZephyrParserFactory").newInstance();
//					System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.stream.ZephyrParserFactory");
//					System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.stream.ZephyrWriterFactory");
//					System.setProperty("javax.xml.stream.XMLEventFactory", "com.sun.xml.stream.events.ZephyrEventFactory");
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

			
			// bnux and XOM
			BinaryXMLCodec codec = new BinaryXMLCodec();
			NodeFactory bnuxFactory = null;
			if (mode.startsWith("bnux")) {
				if (mode.indexOf("NNF") >= 0) {
					bnuxFactory = XOMUtil.getNullNodeFactory();
				}
			}

			Builder builder = new Builder();
//			Builder builder = BuilderPool.GLOBAL_POOL.getBuilder(false);
			if (mode.equals("xom-V")) {
				builder = new Builder(new NodeFactory() {});
			}
			if (mode.equals("xom-NNF")) {
				builder = new Builder(XOMUtil.getNullNodeFactory());
			}
			
			// saxon
			StaticQueryContext context = null;
			Transformer saxonSerializer = null;
			if (mode.equals("saxon")) {
				context = new StaticQueryContext(new Configuration());
				String clazz = "net.sf.saxon.TransformerFactoryImpl";
				System.setProperty("javax.xml.transform.TransformerFactory", clazz);
				saxonSerializer = TransformerFactory.newInstance().newTransformer();
			}
			
			// DOM
			DocumentBuilder domBuilder = null;
			Transformer domSerializer = null;
			if (mode.equals("dom")) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setAttribute("http://apache.org/xml/features/dom/defer-node-expansion", Boolean.FALSE);
				domBuilder = factory.newDocumentBuilder();
				String clazz = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
//				String clazz = "org.apache.xalan.processor.TransformerFactoryImpl";
				System.setProperty("javax.xml.transform.TransformerFactory", clazz);
				domSerializer = TransformerFactory.newInstance().newTransformer();
				System.err.println(domSerializer.getClass().getName());
			}
			
			// FastInfoSet
			Object fiSerializer = null;
//			XMLStreamWriter fiStaxSerializer = null;
			Builder fiBuilder = null;	
			Method fiMethod = null;
			
			XMLStreamReader fistaxReader = null;
			Method fistaxMethod = null;
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
			Builder staxBuilder = null;
			if (mode.indexOf("stax") >= 0) {
				NodeFactory factory = null;
				if (mode.indexOf("NNF") >= 0) factory = XOMUtil.getNullNodeFactory();
				staxBuilder = StaxUtil.createBuilder(staxInputFactory, factory);
			}
			
			for (int j=5; j < args.length; j++) {
				try {
					File[] files = IOTestUtil.listXMLFiles(args[j]);
					for (int k=0; k < files.length; k++) {
						File file = files[k];
						if (bogus(file) || ignore(file) || file.isDirectory()) {
							System.out.println("\n" + ": IGNORING " + file + " ...");
							continue;
						}
						
						System.out.println("now processing " + file);
	//					System.out.print(".");
						
						// prepare
						// file:/path/to/file --> file:///path/to/file
						String baseURI = file.toURI().toASCIIString();
						if (baseURI.startsWith("file:/")) {
							baseURI = baseURI.substring("file:/".length());
							if (!baseURI.startsWith("//")) baseURI = "//" + baseURI;
							baseURI = "file:/" + baseURI;
						}
	//					System.out.println("baseURI: " + baseURI);
						Document doc = new Builder().build(file);
						
						byte[] data = codec.serialize(doc, compressionLevel);
						if (!cmd.equals("test")) doc = new BinaryXMLCodec().deserialize(data); // use "interned" strings
						byte[] fileData = FileUtil.toByteArray(new FileInputStream(file));
						long fileLength = file.length();
						int encodedSize = 0;
						
						org.w3c.dom.Document domDoc = null;
						if (mode.equals("dom")) {
							domDoc = domBuilder.parse(file);
						}
						
						NodeInfo saxonDoc = null;
						if (mode.equals("saxon")) {
							saxonDoc = context.buildDocument(new StreamSource(new ByteArrayInputStream(fileData)));
						}
		
						if (mode.startsWith("fi")) {
							if (mode.indexOf("stax") >= 0) {
//								data = serializeWithStax(doc, staxOutputFactory);
								data = serializeWithFastInfosetStax(doc, (XMLStreamWriter)fiSerializer, fiMethod, new ByteArrayOutputStream());
							} else {
								data = serializeWithFastInfoset(doc, (ContentHandler)fiSerializer, fiMethod, new ByteArrayOutputStream());
							}
						}
		
						// run the benchmark
						long start = System.currentTimeMillis();
						for (int i = 0; i < iterations; i++) {
							try {
								// serialize
								if (cmd.equals("ser") || cmd.equals("serdeser") || cmd.equals("test")) {
									ByteArrayOutputStream out = createOutputStream(cmd.equals("ser"));
									if (mode.startsWith("bnux")) {
										if (mode.indexOf("stream") < 0) {
											codec.serialize(doc, compressionLevel, out);
											data = out.toByteArray();
										} else {
											data = serializeWithStreamingBnux(doc, compressionLevel, out);
										}
									} else if (mode.startsWith("xom")) {
										if (mode.indexOf("stax") >= 0) {
											data = serializeWithStax(doc, staxOutputFactory, out);
										} else if (mode.indexOf("stream") < 0) {
											data = serializeWithXOM(doc, out);
										} else {
											data = serializeWithStreamingXOM(doc, out);
										}
									} else if (mode.equals("saxon")) {
										saxonSerializer.transform(saxonDoc, new StreamResult(out));
										data = out.toByteArray();
									} else if (mode.equals("dom")) {
										domSerializer.transform(new DOMSource(domDoc), new StreamResult(out));
										data = out.toByteArray();
									} else if (mode.startsWith("fi")) {
										if (mode.indexOf("stax") >= 0) {
//											data = serializeWithStax(doc, staxOutputFactory);
											data = serializeWithFastInfosetStax(doc, (XMLStreamWriter)fiSerializer, fiMethod, out);
										} else {
											data = serializeWithFastInfoset(doc, (ContentHandler)fiSerializer, fiMethod, out);
										}
									} else {
										throw new IllegalArgumentException("illegal mode");
									}
									checksum += data.length;
								}
								encodedSize = data.length;
								doneEncoded += encodedSize;
								
								// deserialize
								Document doc2 = null;
								if (cmd.equals("deser") || cmd.equals("serdeser") || cmd.equals("test")) {
									if (mode.startsWith("bnux")) {
										doc2 = codec.deserialize(new ByteArrayInputStream(data), bnuxFactory);
									} else if (mode.startsWith("xom") && mode.indexOf("stax") >= 0) { 
										doc2 = staxBuilder.build(new ByteArrayInputStream(fileData));					
									} else if (mode.startsWith("xom")) { 
										if (mode.indexOf("stream") < 0) {
											doc2 = builder.build(new ByteArrayInputStream(fileData), baseURI);
										} else {
											doc2 = builder.build(new ByteArrayInputStream(data), baseURI);
										}
									} else if (mode.equals("saxon")) { // just for deser comparison
										context.buildDocument(new StreamSource(new ByteArrayInputStream(fileData)));
									} else if (mode.equals("dom")) {
										domDoc = domBuilder.parse(new ByteArrayInputStream(fileData));
	//									System.err.println(domDoc.getClass().getName());
									} else if (mode.startsWith("fi") && mode.indexOf("stax") >=0 ) {
										fistaxMethod.invoke(fistaxReader, new Object[] {new ByteArrayInputStream(data)});
	//									doc2 = staxBuilder.build(fistaxReader);
										doc2 = new StaxParser(fistaxReader, staxBuilder.getNodeFactory()).build();
									} else if (mode.startsWith("fi")) {
										doc2 = fiBuilder.build(new ByteArrayInputStream(data));
									} else {
										throw new IllegalArgumentException("illegal mode");
									}							
									if (doc2 != null) checksum += doc2.getBaseURI().length();
								}
								
								// check correctness
								if (cmd.equals("test")) {
									IOTestUtil.xomAssertEquals(doc, doc2);
									IOTestUtil.canonicalAssertEquals(doc, doc2);
	//								if (! Arrays.equals(XOMUtil.toCanonicalXML(doc), XOMUtil.toCanonicalXML(doc2))) {
	//									System.err.println("Canonical XML Mismatch: ");
	//									System.err.println("expected: " + doc.toXML());
	//									System.err.println("actual: " + doc2.toXML());									
	//									printDiff(doc, doc2);
	//									System.exit(0);
	//								}
									if (!equalsDocTypeEquals(doc.getDocType(), doc2.getDocType())) {	
										System.err.println("DocType Mismatch: ");
										System.err.println("expected: " + doc.toXML());
										System.err.println("actual: " + doc2.toXML());
										System.exit(0);
									}
								}
							} catch (RuntimeException e) {
								System.err.println("FATAL ERROR: " + e);
								System.err.println("expected" + doc.toXML());
								throw e;
							}
							done += fileLength;
						}
						time += (System.currentTimeMillis() - start);
						float cfactor = fileLength * 1.0f / encodedSize;
						System.out.println("compression factor = " + cfactor);			
						if (testCompressionLevels) // alternate on each iteration
							compressionLevel = (compressionLevel + 1) % 10;
					}
				} catch (Exception e) {
					if (hasCause(e, "org.jvnet.fastinfoset.FastInfosetException")) {
						; // FI bug; account as 0 MB/s and continue unbothered
						e.printStackTrace(System.out);
					} else {
						throw e;
					}
				}
			}
					
			
			System.out.println("\n****** SUMMARY ******");
			System.out.println("files = " + iterations * (args.length - 5));
			System.out.println("secs = " + (time / 1000.0f));
			System.out.println("mean throughput MB/s = "
					+ ((done / (1024.0f * 1024.0f))
					/ (time / 1000.0f)));
			float cfactor = done * 1.0f / doneEncoded;
			System.out.println("mean compression factor = " + cfactor);		
			System.out.println("files/sec = " + (iterations * (args.length - 5) / (time / 1000.0f)));		
			System.out.println("checksum = " + checksum);	
		}		
	}
	
	private static byte[] serializeWithXOM(Document doc, ByteArrayOutputStream out) throws IOException {
//		return doc.toXML().getBytes();
		Serializer ser = new Serializer(out);
//		ser.setIndent(4);
		ser.write(doc);
		return out.toByteArray();
	}
	
	private static byte[] serializeWithStreamingXOM(Document doc, ByteArrayOutputStream out) throws IOException {
		StreamingSerializerFactory factory = new StreamingSerializerFactory();
		StreamingSerializer ser = factory.createXMLSerializer(out, "UTF-8");
//		ser.setIndent(4);
		ser.write(doc);
		return out.toByteArray();
	}
	
	private static byte[] serializeWithStreamingBnux(Document doc, int compressionLevel, ByteArrayOutputStream out) throws IOException {
		StreamingSerializerFactory factory = new StreamingSerializerFactory();
		StreamingSerializer ser = factory.createBinaryXMLSerializer(out, compressionLevel);
		ser.write(doc);
		return out.toByteArray();
	}
	
	private static byte[] serializeWithFastInfoset(Document doc, ContentHandler fiSerializer, Method method, ByteArrayOutputStream out) throws Exception {
		// work-around to avoid making FastInfoSet.jar a hard dependency.
		// for best performance on very small documents reflection should better not be used...
		try {
			method.invoke(fiSerializer, new Object[] {out});
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
	
	private static boolean hasCause(Throwable e, String clazz) {
		while (e != null) {
//			System.out.println("chain="+ e.getClass().getName());
			if (e.getClass().getName().equals(clazz)) return true;
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
	
	// print snippet of the offending area to gain some debugging clues
	private static void printDiff(Document expected, Document actual) {
		int window = 100;
//		int window = 300;
//		int window = 10;
		System.err.println("Canonical XML Diff Location Snippet:");
		byte[] e = XOMUtil.toCanonicalXML(expected);
		byte[] a = XOMUtil.toCanonicalXML(actual);
		if (e.length != a.length) {
			System.err.println("e.length="+ e.length + ", a.length=" + a.length);
		}
		int size = Math.min(e.length, a.length);
		for (int i=0; i < size; i++) {
			if (e[i] != a[i]) {
				System.err.println("diff at i=" + i + ", e[i]=" + e[i] + 
						", a[i]=" + a[i]);
				int off = Math.max(0, i-window);
//				int off = Math.max(0, i-0);
				int len = Math.min(2*window, size-off);
				try {
					System.err.println("e='"+ new String(e, off, len, "UTF-8") + "'");
					System.err.println("a='"+ new String(a, off, len, "UTF-8") + "'");
//					System.err.println("eb='"+ new ArrayByteList(new String(e, off, len, "UTF-8").getBytes("UTF-8")));
//					System.err.println("ab='"+ new ArrayByteList(new String(a, off, len, "UTF-8").getBytes("UTF-8")));
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException("can never happen");
				}
				break;
			}
		}
	}
	
//	private static void parseDOM(File file) throws Exception {
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder domBuilder = factory.newDocumentBuilder();
//		org.w3c.dom.Document dom = domBuilder.parse(file);
////		System.out.println(dom.getClass());
//
//		System.setProperty("javax.xml.transform.TransformerFactory",
//				"org.apache.xalan.processor.TransformerFactoryImpl");
//		TransformerFactory transFactory = TransformerFactory.newInstance();
//		Transformer idTransform = transFactory.newTransformer();
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		idTransform.transform(new DOMSource(dom), new StreamResult(out));
//		System.out.println(out.toString());
//	}
	
	private static boolean equalsDocTypeEquals(DocType x, DocType y) {
		if (x != y) {
			if (x == null && y != null)
				return false;
			if (x != null && y == null)
				return false;
			
			if (!x.getInternalDTDSubset().equals(y.getInternalDTDSubset()))
				return false;
			if (!eq(x.getPublicID(), y.getPublicID()))
				return false;
			if (!eq(x.getSystemID(), y.getSystemID()))
				return false;
			if (!eq(x.getRootElementName(), y.getRootElementName()))
				return false;
			return true;
		}

		return true;
	}
	
	private static boolean eq(Object x, Object y) {
		if (x != y) {
			if (x == null && y != null)
				return false;
			if (x != null && y == null)
				return false;
			if (!x.equals(y))
				return false;
		}

		return true;
	}
	
	// ignore some stuff from the test suite dirs
	private static boolean ignore(File xmlFile) {
		String file = xmlFile.getAbsolutePath();
		
//		if (endsWith(file, "MSFT_Conformance_Tests/Include/bloated.xsl")) return true; // catastrophic xerces-2.7.1 performance degradation for large PCDATA blocks; fixed in xerces-2.8.0

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