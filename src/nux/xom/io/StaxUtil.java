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
package nux.xom.io;

import java.io.InputStream;
import java.lang.ref.SoftReference;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import nu.xom.Builder;
import nu.xom.Node;
import nu.xom.NodeFactory;
import nu.xom.ParsingException;
import nu.xom.XMLException;

/**
 * Various utilities; a XOM Builder implementation that uses a StAX
 * parser instead of a SAX parser; a XMLStreamReader implementation reading from
 * an underlying XOM Document or fragment; plus other tools.
 * <p>
 * Also see <a target="_blank" href="http://www-128.ibm.com/developerworks/xml/library/x-axiom/">
 * AXIOM StAX introduction</a>.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek $
 * @version $Revision: 1.21 $, $Date: 2006/06/18 21:27:25 $
 */
public class StaxUtil {
	
	private static final boolean DEBUG = 
		getSystemProperty("nux.xom.io.StaxUtil.debug", false);

	private static final boolean CACHE_DTDS = 
		getSystemProperty("nux.xom.io.StaxUtil.cacheDTDs", true);

	private static final DefaultXMLInputFactory defaultInputFactory = 
		new DefaultXMLInputFactory();
	
	private StaxUtil() {} // not instantiable
	
	/**
	 * Constructs and returns a StAX {@link XMLStreamReader} pull parser
	 * implementation that reads from an underlying XOM Node; typically a 
	 * Document or fragment (subtree); Ideal for efficient conversion of a 
	 * XOM tree to SOAP/AXIOM, JAXB 2, JiBX or XMLBeans, 
	 * for example when incrementally converting XQuery results via an
	 * {@link javax.xml.bind.Unmarshaller}, perhaps in combination with a
	 * {@link nux.xom.xquery.StreamingPathFilter}.
	 * <p>
	 * The parser is namespace aware, non-validating and text coalescing.
	 * <p>
	 * Example usage:
	 * 
	 * <pre>
	 *  Document doc = new Builder().build("samples/data/articles.xml");
	 *  Nodes results = XQueryUtil.xquery(doc, "/articles/article");
	 *  Unmarshaller unmarshaller = JAXBContext.newInstance(...).createUnmarshaller();
	 *  
	 *  for (int i=0; i < results.size(); i++) {
	 *      XMLStreamReader reader = StaxUtil.createXMLStreamReader(results.get(i));
	 *      Object jaxbObject = unmarshaller.unmarshall(reader);
	 *      ... do something with the JAXB object
	 *  }
	 * </pre>
	 * 
	 * @param root
	 *            the root node of the subtree to read from; typically a
	 *            Document or Element; can be parentless.
	 *            XMLStreamConstants.START_DOCUMENT and
	 *            XMLStreamConstants.END_DOCUMENT events will not be emitted if
	 *            the root is an Element, i.e. a fragment. If the root is not a
	 *            {@link nu.xom.ParentNode}, the XMLStreamReader's method 
	 *            <code>hasNext()</code> will always return <code>false</code>.
	 * @return a StAX pull parser reading from an underlying XOM Node
	 */
	public static XMLStreamReader createXMLStreamReader(Node root) {
		return new StaxReader(root);
	}
	
	/**
	 * Constructs and returns a Builder implementation that uses a StAX parser instead 
	 * of a SAX parser. Can be used for polymorphic pluggability of SAX vs. StAX.
	 * 
	 * @param inputFactory
	 *            a factory constructing StAX {@link XMLStreamReader} instances.
	 *            May be <code>null</code> in which case a default factory is
	 *            used, producing a parser that will be namespace-aware,
	 *            DTD-aware, non-validating, and in text-coalescing mode.
	 *            In this case the preferred implementation is Woodstox, if available.
	 * @param factory
	 *            the node factory to stream into. May be <code>null</code> in
	 *            which case the default XOM NodeFactory is used, building the
	 *            full XML tree.
	 * @return a Builder implementation using StAX instead of SAX
	 */
	public static Builder createBuilder(XMLInputFactory inputFactory, NodeFactory factory) {
		if (inputFactory == null) inputFactory = getDefaultInputFactory();
		return new StaxBuilder(inputFactory, factory);
	}
	
	/**
	 * Returns a read-only StAX factory producing parsers that will be namespace
	 * aware, non-validating, support DTDs, and in text coalescing mode.
	 * 
	 * @return a StAX factory
	 */
	private static XMLInputFactory getDefaultInputFactory() {
		XMLInputFactory inputFactory = defaultInputFactory.getInputFactory();
		if (DEBUG) System.err.println("got XMLInputFactory=" + inputFactory.getClass().getName());
		return inputFactory;
	}
	
	/**
	 * Constructs and returns a StAX XML pull parser for the given input stream.
	 * <p>
	 * The StAX parser will be namespace-aware, DTD-aware, non-validating, and 
	 * in text-coalescing mode.
	 * 
	 * @param input
	 *            the input stream to read from (must not be null)
	 * @param baseURI
	 *            the base URI for the input's document (may be null or empty)
	 * @return a StAX pull parser
	 * @throws ParsingException
	 *             if there is an error processing the underlying XML source
	 */
	public static XMLStreamReader createXMLStreamReader(InputStream input, String baseURI) 
			throws ParsingException {
		
		if (input == null) 
			throw new IllegalArgumentException("input must not be null");
		
		if (baseURI != null && baseURI.length() == 0) baseURI = null;
		//baseURI = canonicalizeURL(baseURI); // TODO
		
		XMLInputFactory inputFactory = getDefaultInputFactory();
		try {
			synchronized (inputFactory) {
				// Grabbing a lock is necessary for SJSXP, but not for woodstox 
				// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6365687
				return inputFactory.createXMLStreamReader(baseURI, input);
			}
		} catch (XMLStreamException e) {
			wrapException(e);
			return null; // unreachable
		}
	}
	
	static void wrapException(XMLStreamException ex) throws ParsingException {
		if (DEBUG) ex.printStackTrace();
		String systemID = null;
		int lineNumber = -1;
		int columnNumber = -1;
		Location location = ex.getLocation();
		if (location != null) {
			lineNumber = location.getLineNumber();
			columnNumber = location.getColumnNumber();
			systemID = location.getSystemId();
			if ("".equals(systemID)) systemID = null;
		}
		
		throw new ParsingException(ex.getMessage(), 
				systemID, lineNumber, columnNumber, ex);
	}
	
	/**
	 * Returns a debug string representation for the given StAX event type.
	 * 
	 * @param eventType
	 *            the StAX event type, e.g.
	 *            {@link XMLStreamConstants#START_ELEMENT}.
	 * @return a debug string
	 */
	public static String toString(int eventType) {
		switch (eventType) {
			case XMLStreamConstants.START_ELEMENT: return "START_ELEMENT";
			case XMLStreamConstants.END_ELEMENT: return "END_ELEMENT";
			case XMLStreamConstants.PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
			case XMLStreamConstants.CHARACTERS: return "CHARACTERS";
			case XMLStreamConstants.COMMENT: return "COMMENT";
			case XMLStreamConstants.SPACE: return "SPACE";
			case XMLStreamConstants.START_DOCUMENT: return "START_DOCUMENT";
			case XMLStreamConstants.END_DOCUMENT: return "END_DOCUMENT";
			case XMLStreamConstants.ENTITY_REFERENCE: return "ENTITY_REFERENCE";
			case XMLStreamConstants.ATTRIBUTE: return "ATTRIBUTE";
			case XMLStreamConstants.DTD: return "DTD";
			case XMLStreamConstants.CDATA: return "CDATA";
			case XMLStreamConstants.NAMESPACE: return "NAMESPACE";
			case XMLStreamConstants.NOTATION_DECLARATION: return "NOTATION_DECLARATION";
			case XMLStreamConstants.ENTITY_DECLARATION: return "ENTITY_DECLARATION";
			default: return "Unrecognized event type: " + eventType;
		}
	}
	
	/** little helper for safe reading of boolean system properties */
	static boolean getSystemProperty(String key, boolean defaults) {
		try { 
			return "true".equalsIgnoreCase(
					System.getProperty(key, String.valueOf(defaults)));
		} catch (Throwable e) { // better safe than sorry (applets, security managers, etc.) ...
			return defaults; // we can live with that
		}		
	}

	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns a StAX parser in preferred order. Some parsers are much more
	 * reliable than others...
	 * <p>
	 * Caching the XMLInputFactory avoids expensive lookup and classpath
	 * scanning.
	 */
	private static final class DefaultXMLInputFactory {
		
		/** Cached StAX XMLInputFactory; shared; thread safe */
		private SoftReference factoryRef = new SoftReference(null);
		
		private synchronized XMLInputFactory getInputFactory() {
			XMLInputFactory inputFactory = (XMLInputFactory) factoryRef.get();
			if (inputFactory == null) {
				inputFactory = createInputFactory();
				factoryRef = new SoftReference(inputFactory);
			}
			return inputFactory;
		}
		
		/**
		 * StAX parsers in preferred order. Some parsers are much more reliable than
		 * others...
		 */
		private static final String[] StAX_FACTORIES = {
			"com.ctc.wstx.stax.WstxInputFactory", // Woodstox (Codehaus, Apache license)
			"com.sun.xml.stream.ZephyrParserFactory", // sjsxp (Sun)
			"oracle.xml.stream.OracleXMLInputFactory", // Oracle
			"com.bea.xml.stream.MXParserFactory", // BEA
		};
		
//		private static void foo() {			
//		 I can never find or remember the values of those constants, 
//		 so here they are for future reference:
//		System.setProperty("javax.xml.stream.XMLInputFactory",  "com.ctc.wstx.stax.WstxInputFactory");
//		System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory");
//		System.setProperty("javax.xml.stream.XMLEventFactory",  "com.ctc.wstx.stax.evt.WstxEventFactory");
//		
//		System.setProperty("javax.xml.stream.XMLInputFactory",  "com.sun.xml.stream.ZephyrParserFactory");
//		System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.stream.ZephyrWriterFactory");
//		System.setProperty("javax.xml.stream.XMLEventFactory",  "com.sun.xml.stream.events.ZephyrEventFactory");
//		
//		System.setProperty("javax.xml.stream.XMLInputFactory",  "com.bea.xml.stream.MXParserFactory");
//	    System.setProperty("javax.xml.stream.XMLOutputFactory", "com.bea.xml.stream.XMLOutputFactoryBase");
//	    System.setProperty("javax.xml.stream.XMLEventFactory",  "com.bea.xml.stream.EventFactory");
//	}
	
		private XMLInputFactory createInputFactory() {
			XMLInputFactory factory;
			for (int i = 0; i < StAX_FACTORIES.length; i++) {
				try {
					factory = (XMLInputFactory) ClassLoaderUtil.newInstance(StAX_FACTORIES[i]);
					setupProperties(factory);				
					if (DEBUG) System.err.println("using XMLInputFactory=" + factory.getClass().getName());
					return factory;
				} catch (IllegalArgumentException e) {
					// keep on trying
				} catch (NoClassDefFoundError err) {
					// keep on trying
				} catch (Exception err) {
					// keep on trying
				}
			}
			
			try { // StAX default
				factory = XMLInputFactory.newInstance();
				setupProperties(factory);
			} catch (IllegalArgumentException ex) {
				throw new XMLException(
						"Could not find or create a suitable StAX parser"
								+ " - check your classpath", ex);
			} catch (Exception ex) {
				throw new XMLException(
						"Could not find or create a suitable StAX parser"
								+ " - check your classpath", ex);
			} catch (NoClassDefFoundError ex) {
				throw new XMLException(
						"Could not find or create a suitable StAX parser"
								+ " - check your classpath", ex);
			}
			
			if (DEBUG) System.err.println("using default XMLInputFactory="
						+ factory.getClass().getName());
			return factory;
		}
		
		private static void setupProperties(XMLInputFactory factory) {		
			factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
			factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);	
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
//			factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.TRUE);
			
			try {
				factory.setProperty(
					XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
			} catch (IllegalArgumentException e) {
				; // we can live with that
			}

			String factoryName = factory.getClass().getName();
			if (factoryName.equals("com.ctc.wstx.stax.WstxInputFactory")) { 
				try {
					// it's safer to disable woodstox lazy parsing, in particular with DTDs
					// see http://woodstox.codehaus.org/ConfiguringStreamReaders
					// see com.ctc.wstx.api.WstxInputProperties
					String P_LAZY_PARSING = "com.ctc.wstx.lazyParsing";
					factory.setProperty(P_LAZY_PARSING, Boolean.FALSE);
				} catch (IllegalArgumentException e) {
					; // shouldn't happen, but we can live with that
				}
				
				try {
					// enable/disable DTD caching (wstx default is to enable it)
					String P_CACHE_DTDS = "com.ctc.wstx.cacheDTDs";
					factory.setProperty(P_CACHE_DTDS, Boolean.valueOf(CACHE_DTDS));
				} catch (IllegalArgumentException e) {
					; // shouldn't happen, but we can live with that
				}
//			} else if (factory.isPropertySupported("report-cdata-event")) {}
			} else if (factoryName.equals("com.sun.xml.stream.ZephyrParserFactory")) {
				try {
					// workaround to tell sjsxp to not ignore CDATA events
					// see sjsxp-1_0/docs/ReleaseNotes.html
					String P_REPORT_CDATA = "report-cdata-event";
//					String P_REPORT_CDATA = "http://java.sun.com/xml/stream/properties/report-cdata-event";
					factory.setProperty(P_REPORT_CDATA, Boolean.TRUE);
				} catch (IllegalArgumentException e) {
					; // shouldn't happen, but we can live with that
				}
			}
		}
		
	}

	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	private static final class ClassLoaderUtil {

		// if there is a weird ClassLoader related problem, try switching this flag 
		// and see if that helps
		private static final boolean SIMPLE_MODE = false; 
		
		private static final boolean DEBUG = false; 
		
		private ClassLoaderUtil() {} // not instantiable

		public static Object newInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
			if (SIMPLE_MODE) return Class.forName(className).newInstance();

			// instantiate a temporary dummy object and get its context class loader
			ClassLoader classLoader = new ClassLoaderFinder().getContextClassLoader();
			
			Class clazz;
			if (classLoader == null) {
				if (DEBUG) System.err.println("No context class loader found");
				clazz = Class.forName(className);
			} else {
				if (DEBUG) System.err.println("Context class loader found");
				clazz = classLoader.loadClass(className);
			}
			return clazz.newInstance();
		}

		private static final class ClassLoaderFinder {
			public ClassLoader getContextClassLoader() {
				return Thread.currentThread().getContextClassLoader();
			}
		}

	}
		
}