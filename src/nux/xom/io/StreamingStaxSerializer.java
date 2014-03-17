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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.IllegalAddException;
import nu.xom.Node;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;

/**
 * A {@link StreamingSerializer} implementation that delegates to an underlying
 * StAX {@link javax.xml.stream.XMLStreamWriter}.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek $
 * @version $Revision: 1.23 $, $Date: 2006/06/19 04:42:36 $
 */
final class StreamingStaxSerializer implements StreamingSerializer {
	
	/** The underlying StAX writer to delegate to. */
	private final XMLStreamWriter writer;
	
	/** StAX impls tend to not perform wellformedness checks, so we do it ourselves. */
	private final StreamingVerifier verifier = new StreamingVerifier();
	
	/**
	 * StAX impls tend to do a poor job wrt. eliminating unnecessary namespace
	 * declarations, so we do it ourselves.
	 */
	private final NamespacesInScope namespaces = new NamespacesInScope();
	
	private static final boolean DEBUG = false;
		
	
	/**
	 * Constructs a new object that writes to the given underlying StAX
	 * XMLStreamWriter.
	 * 
	 * @param writer
	 *            the underlying XMLStreamWriter to write to
	 */
	StreamingStaxSerializer(XMLStreamWriter writer) {
		if (writer == null) 
			throw new IllegalArgumentException("XMLStreamWriter must not be null");

		if (!isNamespaceAware(writer))
			throw new IllegalArgumentException("XMLStreamWriter must be namespace aware");
		
		this.writer = writer;
	}
	
	StreamingStaxSerializer(OutputStream out, String encoding) throws XMLStreamException {
		this(XMLOutputFactory.newInstance().createXMLStreamWriter(out, encoding));
	}
	
	private void reset() {
		verifier.reset();
		namespaces.reset();
	}

	/** {@inheritDoc} */
	public void flush() throws IOException {
		try {
			writer.flush();
		} catch (XMLStreamException e) {
			wrapException(e);
		}
	}
	
	/** {@inheritDoc} */
	public void writeStartTag(Element elem) throws IOException {
		verifier.writeStartTag(elem);
		namespaces.push();
		try {
			writer.writeStartElement(
				elem.getNamespacePrefix(), 
				elem.getLocalName(), 
				elem.getNamespaceURI());
		} catch (XMLStreamException e) {
			wrapException(e);
		}
		
		writeAttributes(elem);		
		writeNamespaceDeclarations(elem);
	}
	
	private void writeNamespaceDeclarations(Element elem) throws IOException {
		// TODO: could be implemented more efficiently via 
		// elem.getAdditionalNamespaceDeclarations + attributes iter + elem.getNamespaceURI
		int count = elem.getNamespaceDeclarationCount();
		for (int i=0; i < count; i++) {
			String prefix = elem.getNamespacePrefix(i);
			String uri = elem.getNamespaceURI(prefix);
			if (namespaces.addIfAbsent(prefix, uri)) {
				try {
					writer.writeNamespace(prefix, uri);
				} catch (XMLStreamException e) {
					wrapException(e);
				}
			}
		}
	}
	
	private void writeAttributes(Element elem) throws IOException {
		int count = elem.getAttributeCount();
		for (int i=0; i < count; i++) {
			Attribute attr = elem.getAttribute(i);
			try {
				writer.writeAttribute(
					attr.getNamespacePrefix(), 
					attr.getNamespaceURI(), 
					attr.getLocalName(), 
					attr.getValue());
			} catch (XMLStreamException e) {
				wrapException(e);
			}
		}
	}
	
	/** {@inheritDoc} */
	public void writeEndTag() throws IOException {
		verifier.writeEndTag();
		try {
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			wrapException(e);
		}
		namespaces.pop();
	}
	
	/** {@inheritDoc} */
	public void write(Document doc) throws IOException {
		writeXMLDeclaration();
		for (int i = 0; i < doc.getChildCount(); i++) {
			writeChild(doc.getChild(i));
		}
		writeEndDocument();
	}
	
	/** {@inheritDoc} */
	public void write(Element element) throws IOException {
		writeStartTag(element);
		for (int i=0; i < element.getChildCount(); i++) {
			writeChild(element.getChild(i));
		}
		writeEndTag();
	}
	
	/** {@inheritDoc} */
	public void write(Text text) throws IOException {
		verifier.write(text);
		String value = text.getValue();
		if (value.length() > 0) {
			try {
				writer.writeCharacters(value);
			} catch (XMLStreamException e) {
				wrapException(e);
			}
		}
	}
	
	/** {@inheritDoc} */
	public void write(Comment comment) throws IOException {
		verifier.write(comment);
		try {
			writer.writeComment(comment.getValue());
		} catch (XMLStreamException e) {
			wrapException(e);
		}
	}
	
	/** {@inheritDoc} */
	public void write(ProcessingInstruction instruction) throws IOException {
		verifier.write(instruction);
		try {
			writer.writeProcessingInstruction(
					instruction.getTarget(), 
					instruction.getValue());
		} catch (XMLStreamException e) {
			wrapException(e);
		}
	}
	
	/** {@inheritDoc} */
	public void write(DocType docType) throws IOException {
		verifier.write(docType);
		try {
			writer.writeDTD(docType.toXML());
		} catch (XMLStreamException e) {
			wrapException(e);
		}
	}
	
	/** {@inheritDoc} */
	public void writeEndDocument() throws IOException {
		for (int i=verifier.depth(); --i >= 0; ) {
			writeEndTag(); // close all remaining open tags 
		}
		verifier.writeEndDocument();
		try {
			writer.writeEndDocument();
		} catch (XMLStreamException e) {
			wrapException(e);
		}
		flush();
		reset();
	}
	
	/** {@inheritDoc} */
	public void writeXMLDeclaration() throws IOException {
		verifier.writeXMLDeclaration(); // do sanity check
		reset();
		verifier.writeXMLDeclaration(); // sets hasXMLDeclaration = true
		try {			
			/*
			 * We don't know the underlying encoding; it would be wrong to claim
			 * it's UTF-8. Thus, the following code attempts to specify whatever
			 * encoding the client has used on XMLStreamWriter instantiation.
			 */
			writer.writeStartDocument(getEncoding(), "1.0");
			
			// specifies UTF-8 in XML declaration, which would be wrong
//			writer.writeStartDocument(); // equivalent to writeStartDocument("utf-8");
			
			writer.writeCharacters("\n"); // line separator
		} catch (XMLStreamException e) {
			wrapException(e);
		}
	}
	
	private void writeChild(Node node) throws IOException {
		if (node instanceof Element) {
			write((Element) node);
		} else if (node instanceof Text) {
			write((Text) node);
		} else if (node instanceof Comment) {
			write((Comment) node);
		} else if (node instanceof ProcessingInstruction) {
			write((ProcessingInstruction) node);
		} else if (node instanceof DocType) {
			write((DocType) node);
		} else {
			throw new IllegalAddException("Cannot write node type: " + node);
		}
	}

	// TODO: find a generic way to detect whether the writer supports namespaces
	private static boolean isNamespaceAware(XMLStreamWriter writer) {
		return true;
//		Boolean isNamespaceAware = (Boolean) writer.getProperty("org.codehaus.stax2.namespaceAware");
//		if (DEBUG) System.err.println("isNamespaceAware=" + isNamespaceAware);
//		return isNamespaceAware != null && isNamespaceAware.booleanValue();
	}
		
	private static void wrapException(Throwable t) throws IOException {
		IOException ex = new IOException();
		ex.initCause(t);
		throw ex;
	}
	
	/** thread-safe cache of XMLStreamWriter2.getEncoding() reflection methods */
	private static SoftReference ENCODING_METHODS = new SoftReference(null);
	
	/** dummy marker object indicating absence of XMLStreamWriter2 interface */
	private static final Method NOT_AVAILABLE; 
	
	static {
		try {
			// any method other than getEncoding() could be used just as well
			NOT_AVAILABLE = Object.class.getMethod("wait", null); 
		} catch (Exception e) {
			throw new RuntimeException(e); // can never happen
		}
	}
	
	/**
	 * Use XMLStreamWriter2 encoding property if available. (Efficient)
	 * workaround to avoid making optional StAX2 method a dependency.
	 */
	private String getEncoding() {
//		if (writer instanceof XMLStreamWriter2) {
//			return ((XMLStreamWriter2) writer).getEncoding();
//		}
		
		Map methods = (Map) ENCODING_METHODS.get();
		if (methods == null) {
			methods = Collections.synchronizedMap(new HashMap());
			ENCODING_METHODS = new SoftReference(methods);
		}

		Method method = (Method) methods.get(writer.getClass().getName());
		if (method == null) { // not yet cached?
			method = NOT_AVAILABLE;
			try {
				method = writer.getClass().getMethod("getEncoding", null);
			} catch (SecurityException e) {
				;
			} catch (NoSuchMethodException e) {
				;
			}
			
			// cache getEncoding() method, avoiding expensive 
			// getMethod("getEncoding", null) calls on subsequent documents
			methods.put(writer.getClass().getName(), method);
		}
		
		String encoding = null;		
		if (method != null && method != NOT_AVAILABLE) {
			try {
				encoding = (String) method.invoke(writer, null);
			} catch (Throwable t) {
				; // we can live with that
			}
		}

		if (DEBUG) System.err.println("encoding=" + encoding);
		return encoding;
	}
	
}