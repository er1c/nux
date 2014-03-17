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

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * StAX XMLStreamWriter base class sitting on top of a child XMLStreamWriter,
 * easing implementation of filter chains.
 * <p>
 * By default each method does nothing but call the corresponding method on the
 * underlying child interface. Subclasses may override specific methods to add
 * desired filtering capabilities.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2006/03/26 02:42:07 $
 */
public abstract class StaxFilteredStreamWriter implements XMLStreamWriter {

	/** The underlying writer to delegate to */
	private final XMLStreamWriter child;
	
	/**
	 * Constructs an instance that delegates to the given child writer.
	 * 
	 * @param child
	 *            the writer to delegate to
	 */
	protected StaxFilteredStreamWriter(XMLStreamWriter child) {
		if (child == null) 
			throw new IllegalArgumentException("child must not be null");
		
		this.child = child;
	}

	/** {@inheritDoc} */
	public void writeStartElement(String localName) throws XMLStreamException {
		child.writeStartElement(localName);
	}

	/** {@inheritDoc} */
	public void writeStartElement(String namespaceURI, String localName)
			throws XMLStreamException {
		
		child.writeStartElement(namespaceURI, localName);
	}

	/** {@inheritDoc} */
	public void writeStartElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {

		child.writeStartElement(prefix, localName, namespaceURI);
	}

	/** {@inheritDoc} */
	public void writeEmptyElement(String namespaceURI, String localName)
			throws XMLStreamException {
		
		child.writeEmptyElement(namespaceURI, localName);
	}

	/** {@inheritDoc} */
	public void writeEmptyElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		
		child.writeEmptyElement(prefix, localName, namespaceURI);
	}

	/** {@inheritDoc} */
	public void writeEmptyElement(String localName) throws XMLStreamException {
		child.writeEmptyElement(localName);
	}

	/** {@inheritDoc} */
	public void writeEndElement() throws XMLStreamException {
		child.writeEndElement();
	}

	/** {@inheritDoc} */
	public void writeEndDocument() throws XMLStreamException {
		child.writeEndDocument();
	}

	/** {@inheritDoc} */
	public void close() throws XMLStreamException {
		child.close();
	}

	/** {@inheritDoc} */
	public void flush() throws XMLStreamException {
		child.flush();
	}

	/** {@inheritDoc} */
	public void writeAttribute(String localName, String value)
			throws XMLStreamException {
		
		child.writeAttribute(localName, value);
	}

	/** {@inheritDoc} */
	public void writeAttribute(String prefix, String namespaceURI,
			String localName, String value) throws XMLStreamException {
		
		child.writeAttribute(prefix, namespaceURI, localName, value);
	}

	/** {@inheritDoc} */
	public void writeAttribute(String namespaceURI, String localName,
			String value) throws XMLStreamException {
		
		child.writeAttribute(namespaceURI, localName, value);
	}

	/** {@inheritDoc} */
	public void writeNamespace(String prefix, String namespaceURI)
			throws XMLStreamException {
		
		child.writeNamespace(prefix, namespaceURI);
	}

	/** {@inheritDoc} */
	public void writeDefaultNamespace(String namespaceURI)
			throws XMLStreamException {
		
		child.writeDefaultNamespace(namespaceURI);
	}

	/** {@inheritDoc} */
	public void writeComment(String data) throws XMLStreamException {
		child.writeComment(data);
	}

	/** {@inheritDoc} */
	public void writeProcessingInstruction(String target)
			throws XMLStreamException {
		
		child.writeProcessingInstruction(target);
	}

	/** {@inheritDoc} */
	public void writeProcessingInstruction(String target, String data)
			throws XMLStreamException {
		
		child.writeProcessingInstruction(target, data);
	}

	/** {@inheritDoc} */
	public void writeCData(String data) throws XMLStreamException {
		child.writeCData(data);
	}

	/** {@inheritDoc} */
	public void writeDTD(String dtd) throws XMLStreamException {
		child.writeDTD(dtd);
	}

	/** {@inheritDoc} */
	public void writeEntityRef(String name) throws XMLStreamException {
		child.writeEntityRef(name);
		/** {@inheritDoc} */
	}

	/** {@inheritDoc} */
	public void writeStartDocument() throws XMLStreamException {
		child.writeStartDocument();
	}

	/** {@inheritDoc} */
	public void writeStartDocument(String version) throws XMLStreamException {
		child.writeStartDocument(version);
	}

	/** {@inheritDoc} */
	public void writeStartDocument(String encoding, String version)
			throws XMLStreamException {
		
		child.writeStartDocument(encoding, version);
	}

	/** {@inheritDoc} */
	public void writeCharacters(String text) throws XMLStreamException {
		child.writeCharacters(text);
	}

	/** {@inheritDoc} */
	public void writeCharacters(char[] text, int start, int len)
			throws XMLStreamException {
		
		child.writeCharacters(text, start, len);
	}

	/** {@inheritDoc} */
	public String getPrefix(String uri) throws XMLStreamException {
		return child.getPrefix(uri);
	}

	/** {@inheritDoc} */
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		child.setPrefix(prefix, uri);
	}

	/** {@inheritDoc} */
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		child.setDefaultNamespace(uri);
	}

	/** {@inheritDoc} */
	public void setNamespaceContext(NamespaceContext context)
			throws XMLStreamException {
		
		child.setNamespaceContext(context);
	}

	/** {@inheritDoc} */
	public NamespaceContext getNamespaceContext() {
		return child.getNamespaceContext();
	}

	/** {@inheritDoc} */
	public Object getProperty(String name) throws IllegalArgumentException {
		return child.getProperty(name);
	}

}
