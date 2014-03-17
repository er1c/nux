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
import java.io.UnsupportedEncodingException;

import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.Text;

/**
 * Using memory consumption close to zero, this streaming variant of the XOM
 * {@link Serializer} enables writing arbitrarily large XML documents; writes a
 * standard textual XML document via a thin layer on top of the normal XOM
 * Serializer.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.10 $, $Date: 2006/03/10 18:42:45 $
 */
final class StreamingXMLSerializer extends Serializer implements StreamingSerializer {
	
	/*
	 * setUnicodeNormalizationFormC, setPreserveBaseURI, setMaxLength and
	 * setLineSeparator are handled fine as expected.
	 * 
	 * TODO: what about setIndent(>0) with xml:space="preserve"? 
	 * TODO: coalesce adjacent Text nodes if getIndent() > 0
	 */

	private final StreamingVerifier verifier = new StreamingVerifier();
	
	private final NamespacesInScope namespaces = new NamespacesInScope();

	// cosmetics for beauty contestants: <foo></foo> vs. <foo/>
	private static final boolean EXPAND_EMPTY_ELEMENTS = true;
	private Element startTag = null;
	private boolean hasChildren = false;
	
	private static final boolean DEBUG = false;
	
	
	/**
	 * Constructs a new object that writes to the given underlying output
	 * stream, using UTF-8 encoding for char to byte conversions.
	 * 
	 * @param out
	 *            the underlying stream to write to
	 * @see Serializer#Serializer(OutputStream)
	 */
	public StreamingXMLSerializer(OutputStream out) {
		super(out);
	}
	
	/**
	 * Constructs a new object that writes to the given underlying output
	 * stream, using the given character encoding for char to byte conversions.
	 * 
	 * @param out
	 *            the underlying stream to write to
	 * @param encoding
	 *            the character encoding to use (e.g. "UTF-8")
	 * @see Serializer#Serializer(OutputStream, String)
	 */
	public StreamingXMLSerializer(OutputStream out, String encoding)  
		throws UnsupportedEncodingException {
		
		super(out, encoding);
	}
	
	private void clear() {
		verifier.reset();
		namespaces.reset();
		startTag = null;
		hasChildren = false;
	}
		
	/** {@inheritDoc} */
	public void setOutputStream(OutputStream out) throws IOException {
		super.setOutputStream(out);
		clear();
	}
	
	/** {@inheritDoc} */
	public void writeStartTag(Element elem) throws IOException {
		verifier.writeStartTag(elem);
		
		// assert elem != null;
		if (EXPAND_EMPTY_ELEMENTS) {
			namespaces.push();
			super.writeStartTag(elem); // calls writeNamespaceDeclarations(elem)
		} else {
			writeDelayedStartTag();
			
			// don't write the current start tag just yet, buffer and delay  
			// until we can decide whether it's an empty or non-empty element
			startTag = elem;
			hasChildren = false;
		}
	}
	
	private void writeDelayedStartTag() throws IOException {
		if (EXPAND_EMPTY_ELEMENTS) return;
		if (startTag != null) {
			namespaces.push();
			super.writeStartTag(startTag); // calls writeNamespaceDeclarations(elem)
			
			startTag = null;
			hasChildren = true;
		}
	}
	
	/** {@inheritDoc} */
	public void writeEndTag() throws IOException {
		Element elem = verifier.writeEndTag();
		if (EXPAND_EMPTY_ELEMENTS || hasChildren) { 
			writeEndTag(elem); // start tag has already been written
		} else { // start tag has not yet been written
			namespaces.push();
			writeEmptyElementTag(elem); // calls writeNamespaceDeclarations(elem)
			startTag = null;
		}
		namespaces.pop();
		hasChildren = true;
	}
	
	/**
	 * Custom override taking care of streaming writes of attached and detached
	 * elements.
	 *
	 * @param elem
	 *            the element to write namespaces for
	 * @throws IOException
	 *             if the underlying output stream encounters an I/O error
	 */
	protected void writeNamespaceDeclarations(Element elem) throws IOException {
		// TODO: could be implemented more efficiently via 
		// elem.getAdditionalNamespaceDeclarations + attributes iter + elem.getNamespaceURI
		int count = elem.getNamespaceDeclarationCount();
		for (int i=0; i < count; i++) {
			String prefix = elem.getNamespacePrefix(i);
			String uri = elem.getNamespaceURI(prefix);
			if (namespaces.addIfAbsent(prefix, uri)) {
				writeRaw(" ");
				writeNamespaceDeclaration(prefix, uri);				
			}
		}
	}
	
	/** {@inheritDoc} */
	public void write(Document doc) throws IOException {
		super.write(doc);
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
			writeDelayedStartTag();
			writeEscaped(value);
		}
	}
	
	/** {@inheritDoc} */
	public void write(Comment comment) throws IOException {
		verifier.write(comment);
		writeDelayedStartTag();
		super.write(comment);
	}
	
	/** {@inheritDoc} */
	public void write(ProcessingInstruction instruction) throws IOException {
		verifier.write(instruction);
		writeDelayedStartTag();
		super.write(instruction);
	}
	
	/** {@inheritDoc} */
	public void write(DocType docType) throws IOException {
		verifier.write(docType);
		super.write(docType);
	}
	
	/** {@inheritDoc} */
	public void writeEndDocument() throws IOException {
		for (int i=verifier.depth(); --i >= 0; ) {
			writeEndTag(); // close all remaining open tags 
		}
		verifier.writeEndDocument();
		flush();
		clear();
	}

	/** {@inheritDoc} */
	public void writeXMLDeclaration() throws IOException {
		// Voodoo workaround because Serializer.escaper.reset() isn't public
		// Wouldn't be necessary if Serializer.reset() would be a protected method
		if (!calledFromSuperclass) {
			verifier.writeXMLDeclaration(); // sanity check
			clear();
			verifier.writeXMLDeclaration(); // sets hasXMLDeclaration = true
			
			calledFromSuperclass = true;
			try {
				// calls Serializer.escaper.reset(), then again writeXMLDeclaration()
				super.write(EMPTY_DOCUMENT);
			} finally {
				calledFromSuperclass = false;
			}
		} else {
			super.writeXMLDeclaration();
		}
	}
	
	private boolean calledFromSuperclass = false;
	private static final Document EMPTY_DOCUMENT = new EmptyDocument();
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////

	/** Voodoo workaround because Serializer.escaper.reset() isn't public */
	private static final class EmptyDocument extends Document {
		private EmptyDocument() {
			super(new Element("dummy"));
		}
		public int getChildCount() {
			return 0; // we only want to call escaper.reset(), not write any children
		}		
	}
	
}