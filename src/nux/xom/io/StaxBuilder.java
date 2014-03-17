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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.NodeFactory;
import nu.xom.ParsingException;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Imlementation of {@link nu.xom.Builder} that constructs a XOM document using an
 * underlying StAX pull parser rather than a SAX push parser, inverting control
 * flow. 
 * <p>
 * All the hard work is delegated to {@link nux.xom.io.StaxParser}.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.11 $, $Date: 2006/03/26 03:13:32 $
 */
 final class StaxBuilder extends Builder { // not a public class
	 
	/** The optional XOM factory to push into. */
	private final NodeFactory factory;
	
	/** The StAX factory used to create StAX parser objects. */
	private final XMLInputFactory inputFactory;
	
	/**
	 * Constructs a new instance that pushes into the given node factory.
	 * 
	 * @param inputFactory
	 *            a factory constructing StAX {@link XMLStreamReader} instances.
	 *            Must not be <code>null</code>.
	 * @param factory
	 *            the node factory to stream into. May be <code>null</code> in
	 *            which case the default XOM NodeFactory is used, building the
	 *            full XML tree.
	 */
	StaxBuilder(XMLInputFactory inputFactory, NodeFactory factory) {
		super(new DummyXMLReader(), false, factory);
		if (inputFactory == null) 
			throw new IllegalArgumentException("XMLInputFactory must not be null");		
		this.inputFactory = inputFactory;
		if (factory == null) factory = new NodeFactory();
		this.factory = factory;
	}

	/** Inherited from {@link Builder#build(InputStream)}. */
	public Document build(InputStream input) throws ParsingException, IOException {
		return build(input, null);
	}
	
	/** Inherited from {@link Builder#build(InputStream, String)}. */
	public Document build(InputStream input, String baseURI) 
			throws ParsingException, IOException {
		
		try {
			XMLStreamReader reader = createXMLStreamReader(input, baseURI);
			return new StaxParser(reader, factory).build();
		} finally {
			if (input != null) input.close(); // for SAX compatibility
		}
	}
	
	/** Inherited from {@link Builder#build(File)}. */
	public Document build(File file) throws ParsingException, IOException {
		String baseURI = file.toURI().toASCIIString();
		return build(new FileInputStream(file), baseURI);
	}
	
	/** Inherited from {@link Builder#build(Reader)}. */
	public Document build(Reader input) throws ParsingException, IOException {
		return build(input, null);
	}
	
	/** Inherited from {@link Builder#build(Reader, String)}. */
	public Document build(Reader input, String baseURI) 
			throws ParsingException, IOException {
		
		try {
			XMLStreamReader reader = createXMLStreamReader(input, baseURI);
			return new StaxParser(reader, factory).build();
		} finally {
			if (input != null) input.close(); // for SAX compatibility
		}
	}
	
	/** Inherited from {@link Builder#build(String)}. */
	public Document build(String systemID) throws ParsingException, IOException {
		if (systemID == null) 
			throw new IllegalArgumentException("systemID must not be null");
		
		return build(URI.create(systemID).toURL().openStream(), systemID);
//		return build(new URL(systemID).openStream(), systemID);
	}
	
	private XMLStreamReader createXMLStreamReader(InputStream input, String baseURI) 
			throws ParsingException {
		
		if (input == null) 
			throw new IllegalArgumentException("input must not be null");
		
		if (baseURI != null && baseURI.length() == 0) baseURI = null;
		//baseURI = canonicalizeURL(baseURI); // TODO
		
		try {
			synchronized (inputFactory) { 
				// Grabbing a lock is necessary for SJSXP, but not for woodstox 
				// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6365687
				return inputFactory.createXMLStreamReader(baseURI, input);
			}
		} catch (XMLStreamException e) {
			StaxUtil.wrapException(e);
			return null; // unreachable
		}
	}
	
	private XMLStreamReader createXMLStreamReader(Reader input, String baseURI) 
			throws ParsingException {

		if (input == null)
			throw new IllegalArgumentException("input must not be null");
		
		if (baseURI != null && baseURI.length() == 0) baseURI = null;
		//baseURI = canonicalizeURL(baseURI); // TODO

		try {
			synchronized (inputFactory) {
				// Grabbing a lock is necessary for SJSXP, but not for woodstox 
				// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6365687
				return inputFactory.createXMLStreamReader(baseURI, input);
			}
		} catch (XMLStreamException e) {
			StaxUtil.wrapException(e);
			return null; // unreachable
		}
	}

	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Workaround to make StaxBuilder an efficient subclass of Builder. The only
	 * methods the nu.xom.Builder superclass constructor ever calls on this
	 * object are setXYZ(). These calls are simply ignored.
	 */
	private static final class DummyXMLReader implements XMLReader {

		// TODO: might be safer to store properties and features, 
		// so they're available on getXYZ.
		
		private DummyXMLReader() {
		}

		public boolean getFeature(String name) throws SAXNotRecognizedException,
				SAXNotSupportedException {
			return false;
		}

		public void setFeature(String name, boolean value)
				throws SAXNotRecognizedException, SAXNotSupportedException {
		}

		public Object getProperty(String name) throws SAXNotRecognizedException,
				SAXNotSupportedException {
			return null;
		}

		public void setProperty(String name, Object value)
				throws SAXNotRecognizedException, SAXNotSupportedException {
		}

		public void setEntityResolver(EntityResolver resolver) {
		}

		public EntityResolver getEntityResolver() {
			return null;
		}

		public void setDTDHandler(DTDHandler handler) {
		}

		public DTDHandler getDTDHandler() {
			return null;
		}

		public void setContentHandler(ContentHandler handler) {
		}

		public ContentHandler getContentHandler() {
			return null;
		}

		public void setErrorHandler(ErrorHandler handler) {
		}

		public ErrorHandler getErrorHandler() {
			return null;
		}

		public void parse(InputSource input) throws IOException, SAXException {
		}

		public void parse(String systemId) throws IOException, SAXException {
		}

	}
		
}
