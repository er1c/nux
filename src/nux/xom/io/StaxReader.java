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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;

/**
 * StAX {@link XMLStreamReader} pull parser implementation that reads from an
 * underlying XOM Node; typically a Document or Element (subtree); Ideal for efficient
 * conversion of a XOM tree to JAXB 2, for example when incrementally converting
 * XQuery results via an {@link javax.xml.bind.Unmarshaller}, perhaps in 
 * combination with a {@link nux.xom.xquery.StreamingPathFilter}.
 * <p>
 * Example usage:
 * 
 * <pre>
 *  Document doc = new Builder().build("samples/data/articles.xml");
 *  Nodes results = XQueryUtil.xquery(doc, "/articles/article");
 *  Unmarshaller unmarshaller = JAXBContext.newInstance(...).createUnmarshaller();
 *  
 *  for (int i=0; i < results.size(); i++) {
 *      Object jaxbObject = unmarshaller.unmarshall(new StaxReader(results.get(i)));
 *      ... do something with the JAXB object
 *  }
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.13 $, $Date: 2006/04/29 03:00:54 $
 */
final class StaxReader implements XMLStreamReader { // not a public class
	
	// TODO: integrate with XOMUtil.jaxbUnmarshal() if JAXB2 detected
	// TODO: find a better way to pass DocType through? perhaps via getProperty("nu.xom.DocType")
	// TODO: add handling for nu.xom.Namespace
	
	/** The root of the (sub)tree to expose via StAX. */
	private ParentNode root;
	
	/** The node the cursor is currently positioned over. */
	private Node current;
	
	/** The event the cursor is currently positioned over. */
	private int eventType;
	
	/**
	 * Merge adjacent Text nodes into a single virtual text node, ala XPath.
	 * Empty text nodes are ignored irrespective of this flag.
	 */
	private final boolean isCoalescing;
	
	/** Text value of current CHARACTERS event; potentially coalesced. */
	private String textValue;
	
	private static final boolean DEBUG = false;
	
	
	/**
	 * Constructs an instance that reads nodes from the given subtree. The
	 * parser is non-validating and is text coalescing.
	 * 
	 * @param root
	 *            the root node of the subtree to read from; typically a
	 *            Document or Element; can be parentless.
	 *            XMLStreamConstants.START_DOCUMENT and
	 *            XMLStreamConstants.END_DOCUMENT events will not be emitted if
	 *            the root is an Element, i.e. a fragment. If the root is not a
	 *            {@link nu.xom.ParentNode}, the XMLStreamReader's method 
	 *            <code>hasNext()</code> will always return <code>false</code>.
	 */
	public StaxReader(Node root) {
		this(root, true);
	}

	// TODO: any need to make this public?
	private StaxReader(Node root, boolean isCoalescing) {
		if (root == null) throw new IllegalArgumentException(
			"root node must not be null");
			
		// TODO requires xom >= 1.1
//		if (root instanceof nu.xom.Namespace) throw new UnsupportedOperationException(
//			"namespace node not yet supported");
		
		this.isCoalescing = isCoalescing;
		this.current = root;
		setEventType(root);
		
		this.textValue = null;
		if (root instanceof Text) this.textValue = root.getValue();
		
		this.root = null;
		if (root instanceof ParentNode) this.root = (ParentNode) root;
	}

	/** {@inheritDoc} */
	public void close() throws XMLStreamException {
		this.root = null;
		this.current = null;
		this.textValue = null;
		this.eventType = END_DOCUMENT;
	}

	/** {@inheritDoc} */
	public int getEventType() {
		return eventType;
	}

	/** {@inheritDoc} */
	public boolean hasNext() throws XMLStreamException {
		if (root == null) return false; // root was not a ParentNode on instance construction
		int ev = getEventType();
		return current != root || (ev != END_ELEMENT && ev != END_DOCUMENT);
	}

	/** {@inheritDoc} */
	public int next() throws XMLStreamException {
		/*
		 * Note that the following event types are never returned: CDATA, SPACE,
		 * ENTITY_REFERENCE, ENTITY_DECLARATION, NOTATION_DECLARATION,
		 * NAMESPACE. These event types are not present in the XOM object model.
		 */   
		do { // Move to next node on the descendants axis
			if (!hasNext()) throw new NoSuchElementException(
					"Attempted to read beyond end of XML tree");
			
			textValue = null;
			int i = 0;
			if (isEndElement() || current.getChildCount() <= 0) {
				if (isStartElement()) { 
					return setEventType(END_ELEMENT); // empty elem (zero children)
				}
				
				// move to next sibling
				i = current.getParent().indexOf(current) + 1; // fast with Nux O(1) indexOf() patch
				current = current.getParent(); // recurse up
				
				if (i >= current.getChildCount()) { // last child?
					// if we've come all the way back to the start root we're done
					int ev = (current == root && current instanceof Document) ?
							END_DOCUMENT : END_ELEMENT;
					return setEventType(ev);
				}
			}
			
			current = nextChild(i); // and recurse down
			setEventType(current);
		} while (textValue != null && textValue.length() == 0); // ignore empty Text ala XPath
		
		return getEventType();
	}
	
	private Node nextChild(int i) {
		Node node = current.getChild(i);
		if (node instanceof Text) { 
			textValue = node.getValue();
			if (isCoalescing) { 
				// Merge adjacent Text nodes into a single virtual text node, ala XPath
				Node n;
				int count = current.getChildCount();
				while (++i < count && ((n = current.getChild(i)) instanceof Text)) {
					node = n;
					textValue += node.getValue(); // rare case
				}
			}
		}
		return node;
	}
	
	private int setEventType(Node node) {
		int ev;
		if (node instanceof Element) {
			ev = START_ELEMENT;
		} else if (node instanceof Text) {
			ev = CHARACTERS;
		} else if (node instanceof Comment) {
			ev = COMMENT;
		} else if (node instanceof Attribute) {
			ev = ATTRIBUTE;
		} else if (node instanceof ProcessingInstruction) {
			ev = PROCESSING_INSTRUCTION;
		} else if (node instanceof DocType) {
			ev = DTD;
		} else if (node instanceof Document) {
			ev = START_DOCUMENT;
		} else {
			throw new IllegalStateException("Cannot read node type: " + node);
		}
		
		return setEventType(ev);
	}

	private int setEventType(int eventType) {
		this.eventType = eventType;
		return eventType;
	}

	/** {@inheritDoc} */
	public int nextTag() throws XMLStreamException {
		while (true) {
			int ev = next();

			switch (ev) {
				case SPACE:
				case COMMENT:
				case PROCESSING_INSTRUCTION:
					break; // skip to next event
				case CDATA:
				case CHARACTERS:
					if (!isWhiteSpace()) throwXMLStreamException(
							"Required whitespace-only CHARACTERS|CDATA");
					break; // skip to next event
				case START_ELEMENT:
				case END_ELEMENT:
					return ev;
				default: 
					throwXMLStreamException("type", "START_ELEMENT|END_ELEMENT",
							toString(ev));
			}
		}
	}

	/** {@inheritDoc} */
	public boolean isStartElement() {
		return getEventType() == START_ELEMENT;
	}

	/** {@inheritDoc} */
	public boolean isEndElement() {
		return getEventType() == END_ELEMENT;
	}

	/** {@inheritDoc} */
	public boolean isCharacters() {
		return getEventType() == CHARACTERS;
	}

	/** {@inheritDoc} */
	public boolean isWhiteSpace() {
		return (isCharacters() || getEventType() == CDATA) 
			&& isWhitespaceOnly(getText());
	}

	private Element currentElement() {
		if (current instanceof Element) {
			return (Element) current;
		}
		throwIllegalStateException("START_ELEMENT|END_ELEMENT");
		return null; // unreachable
	}
	
	private Element currentStartElement() {
		if (getEventType() != START_ELEMENT) {
			throwIllegalStateException("START_ELEMENT");
		}
		return (Element) current;
	}
	
	private ProcessingInstruction currentPI() {
		if (current instanceof ProcessingInstruction) {
			return (ProcessingInstruction) current;
		}
		throwIllegalStateException("PROCESSING_INSTRUCTION");
		return null; // unreachable
	}
	
	private DocType currentDocType() {
		if (current instanceof DocType) {
			return (DocType) current;
		}
		throwIllegalStateException("DTD");
		return null; // unreachable
	}
	
	private Attribute currentAttribute(int index) {
		if (getEventType() == ATTRIBUTE) { // @ATTR
			return (Attribute) current;
		}
		return currentStartElement().getAttribute(index);
	}

	/** {@inheritDoc} */
	public String getLocalName() {
		return currentElement().getLocalName();
	}

	/** {@inheritDoc} */
	public String getPrefix() {
		String prefix = currentElement().getNamespacePrefix();
//		prefix = toNull(prefix);
		return prefix;
	}

	/** {@inheritDoc} */
	public String getNamespaceURI() {
		String uri = currentElement().getNamespaceURI();
//		uri = toNull(uri);
		return uri;
	}

	/** {@inheritDoc} */
	public QName getName() {
		return new QName(getNamespaceURI(), getLocalName(), getPrefix());
	}
	
	/** {@inheritDoc} */
	public int getAttributeCount() {
		if (getEventType() == ATTRIBUTE) return 1; // @ATTR
		return currentStartElement().getAttributeCount();
	}

	/** {@inheritDoc} */
	public QName getAttributeName(int index) {
		return new QName(
			getAttributeNamespace(index), 
			getAttributeLocalName(index), 
			getAttributePrefix(index));
	}
	
	/** {@inheritDoc} */
	public String getAttributeNamespace(int index) {
		return currentAttribute(index).getNamespaceURI();
	}

	/** {@inheritDoc} */
	public String getAttributeLocalName(int index) {
		return currentAttribute(index).getLocalName();
	}

	/** {@inheritDoc} */
	public String getAttributePrefix(int index) {
		return currentAttribute(index).getNamespacePrefix();
	}

	/** {@inheritDoc} */
	public String getAttributeType(int index) {
		Attribute.Type attrType = currentAttribute(index).getType();
		if (attrType == Attribute.Type.ENUMERATION) return "ENUMERATED"; // TODO: ???
		return attrType.getName(); // StAX spec isn't clear on what's legal here
	}

	/** {@inheritDoc} */
	public String getAttributeValue(int index) {
		return currentAttribute(index).getValue();
	}

	/** {@inheritDoc} */
	public String getAttributeValue(String namespaceURI, String localName) {
		if (localName == null) 
			throw new IllegalArgumentException("localName must not be null");
		
		if (getEventType() != ATTRIBUTE) { // @ATTR
			if (namespaceURI != null) {
				return currentElement().getAttributeValue(localName, namespaceURI);				
			}
		}
		
		int count = getAttributeCount();
		for (int i=0; i < count; i++) {
			if (localName.equals(getAttributeLocalName(i))) {
				if (namespaceURI == null || namespaceURI.equals(getAttributeNamespace(i))) {
					return getAttributeValue(i);
				}
			}
		}
		return null; // not found
	}

	/** {@inheritDoc} */
	public boolean isAttributeSpecified(int index) {
		return false; // info not available from XOM
	}

	/** {@inheritDoc} */
	public String getElementText() throws XMLStreamException {
		require(START_ELEMENT, null, null);
		String str = null;
		
		while (true) {
			switch (next()) {
				case END_ELEMENT:
					return str == null ? "" : str; // we're done
				case CDATA:
				case SPACE:
				case ENTITY_REFERENCE:
				case CHARACTERS: { // accumulate strings
					String text = getText();
					str = str == null ? text : str + text;
					break;
				}
				case PROCESSING_INSTRUCTION:
				case COMMENT:
					break; // skip to next event
				case END_DOCUMENT:
					throwXMLStreamException(
						"unexpected end of document when reading element text content");
				case START_ELEMENT:
					throwXMLStreamException(
						"element text content may not contain START_ELEMENT");
				default:
					throwXMLStreamException(
						"Unexpected event type: " + toString(getEventType()));
			}				
		}
	}

	/** {@inheritDoc} */
	public int getNamespaceCount() {
		// TODO: ideally should only report bindings going into scope, 
		// excluding duplicate redeclarations
		return currentElement().getNamespaceDeclarationCount();
	}

	/** {@inheritDoc} */
	public String getNamespacePrefix(int index) {
		String prefix = currentElement().getNamespacePrefix(index);
//		if (prefix.length() == 0) prefix = null; // ???
		return prefix;
	}

	/** {@inheritDoc} */
	public String getNamespaceURI(int index) {
		return getNamespaceURI(getNamespacePrefix(index));
	}

	/** {@inheritDoc} */
	public String getNamespaceURI(String prefix) {
		return getNamespaceURI(prefix, currentElement());
	}

	private static String getNamespaceURI(String prefix, Element elem) {
		if (prefix == null) 
			throw new IllegalArgumentException("prefix must not be null");
		
		// "xmlns" --> "http://www.w3.org/2000/xmlns/"
		// XOM would return "", see Element.getLocalNamespaceURI(String prefix)
		if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI; 
		}
		return elem.getNamespaceURI(prefix);
	}

	/** {@inheritDoc} */
	public NamespaceContext getNamespaceContext() {
		return new NamespaceContextImpl(currentElement());
	}

	/** {@inheritDoc} */
	public String getPITarget() {
		return currentPI().getTarget();
	}

	/** {@inheritDoc} */
	public String getPIData() {
		return currentPI().getValue();
	}
	
	/** {@inheritDoc} */
	public String getText() {
		switch (getEventType()) {
			case CHARACTERS: return textValue;
			case CDATA: return textValue;
			case SPACE: return textValue;
			case COMMENT: return current.getValue();
			case DTD: return currentDocType().toXML(); // TODO: ???
			default: {
				throwIllegalStateException("CHARACTERS|CDATA|SPACE|COMMENT|DTD");
				return null; // unreachable
			}
		}
	}

	/** {@inheritDoc} */
	public char[] getTextCharacters() {
		return getText().toCharArray();
	}

	/** {@inheritDoc} */
	public int getTextCharacters(int sourceStart, char[] target,
			int targetStart, int length) throws XMLStreamException {
		
		String text = getText();
		int sourceEnd = Math.min(text.length(), sourceStart + length);
		text.getChars(sourceStart, sourceEnd, target, targetStart);
		return sourceEnd - sourceStart;
	}

	/** {@inheritDoc} */
	public int getTextStart() {
		return 0;
	}

	/** {@inheritDoc} */
	public int getTextLength() {
		return getText().length();
	}

	/** {@inheritDoc} */
	public boolean hasName() {
		return isStartElement() || isEndElement();
	}

	/** {@inheritDoc} */
	public boolean hasText() {
		switch (getEventType()) {
			case CHARACTERS:
			case COMMENT:
			case SPACE:
			case CDATA:
			case DTD: // ???
			case ENTITY_REFERENCE: return true;
			default: return false;
		}
	}

	/** {@inheritDoc} */
	public String getEncoding() {
		return null;
	}

	/** {@inheritDoc} */
	public Location getLocation() {
		return new UnknownLocation();
	}

	/** {@inheritDoc} */
	public String getVersion() {
		return null;
	}

	/** {@inheritDoc} */
	public boolean isStandalone() {
		return false;
	}

	/** {@inheritDoc} */
	public boolean standaloneSet() {
		return false;
	}

	/** {@inheritDoc} */
	public String getCharacterEncodingScheme() {
		return null;
	}

	/** {@inheritDoc} */
	public Object getProperty(String name) throws IllegalArgumentException {
		// TODO: more effective lookup via a HashMap?
		if (name == null) 
			throw new IllegalArgumentException("name must not be null");	
		
		if (name.equals(XMLInputFactory.IS_NAMESPACE_AWARE)) {
			return Boolean.TRUE;
		}
		if (name.equals(XMLInputFactory.IS_COALESCING)) {
			return isCoalescing ? Boolean.TRUE : Boolean.FALSE;
		}
		if (name.equals(XMLInputFactory.IS_VALIDATING)) {
			return Boolean.FALSE;
		}
		if (name.equals(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
			return Boolean.TRUE; // by virtue of XOM
		}
		if (name.equals(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
			return Boolean.TRUE; // by virtue of XOM
		}
		if (name.equals(XMLInputFactory.SUPPORT_DTD)) {
			return Boolean.TRUE; // by virtue of XOM
		}
		if (name.equals(XMLInputFactory.REPORTER)) {
			return null;
		}
		if (name.equals(XMLInputFactory.RESOLVER)) {
			return null;
		}
		if (name.equals(XMLInputFactory.ALLOCATOR)) {
			return null;
		}
		
		throw new IllegalArgumentException("Unsupported property: " + name);
//		return null;
	}
	
	/**
	 * The standard StAX API does not provide enough info, so this is an
	 * experimental StAX extension borrowed from woodstox, to be used via pure
	 * runtime reflection. 
	 * <p>
	 * Also see StaxParser.readDocType()
	 */
	public Object getDTDInfo() {
		return new DTDInfo(currentDocType());
	}

	/** {@inheritDoc} */
	public void require(int type, String namespaceURI, String localName)
			throws XMLStreamException {
		
		if (getEventType() != type) {
			throwXMLStreamException("type", toString(type), toString(getEventType()));
		}		
		
		if (localName != null && !localName.equals(getLocalName())) {
			throwXMLStreamException("localName", localName, getLocalName());
		}
		
		if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) {
			throwXMLStreamException("namespaceURI", namespaceURI, getNamespaceURI());
		}
		
	}

	// moved slow path out of hotspot to enable better inlining
	private void throwIllegalStateException(String expected) {
		throw new IllegalStateException("Required type: " + expected 
				+ ", actual type: " + toString(getEventType()));		
	}
	
	private void throwXMLStreamException(String msg, String expected, String actual)
			throws XMLStreamException {
		
		msg = "Required " + msg + ":'" + expected + "', actual " + msg + ":'" + actual + "'";
		throwXMLStreamException(msg);
	}
	
	private void throwXMLStreamException(String msg) throws XMLStreamException {
		throw new XMLStreamException(msg, getLocation());
	}
	
	private static boolean isWhitespaceOnly(String str) {
		for (int i=str.length(); --i >= 0; ) {
			if (!isWhitespace(str.charAt(i))) return false; 
		}
		return true;
	}
	
	/** see XML spec */
	private static boolean isWhitespace(char c) {
		switch (c) {
			case '\t': return true;
			case '\n': return true;
			case '\r': return true;
			case ' ' : return true;
			default  : return false;			
		}
	}
	
	private static String toNull(String str) {
		return (str != null && str.length() > 0) ? str : null; 
	}
	
	private static String toEmpty(String str) {
		return str == null ? "" : str;
	}
	
	/** {@inheritDoc} */
	public String toString() {
		return toString(getEventType());		
	}
	
	private static String toString(int ev) {
		return StaxUtil.toString(ev);
	}
	
//	/** Fires into the given SAX ContentHandler */
//	public void convertToSAX(ContentHandler saxHandler) throws XMLStreamException {
//		new javanet.staxutils.XMLStreamReaderToContentHandler(this, saxHandler).bridge();
//	}
	
//	/** Makes this object appears as an XMLEventReader pull parser */
//	public XMLEventReader asStaxEventReader() {
//		return new javanet.staxutils.XMLStreamEventReader(this);
//		return XMLInputFactory.newInstance().createXMLEventReader(this);
//	}

	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	private static final class NamespaceContextImpl implements NamespaceContext {
		
		private final Element element;
		
		private NamespaceContextImpl(Element element) {
			this.element = element;
		}
		
		/** {@inheritDoc} */
		public String getNamespaceURI(String prefix) {
			return StaxReader.getNamespaceURI(prefix, element);
		}
				
		/** {@inheritDoc} */
		public String getPrefix(String namespaceURI) {
			// TODO: could be implemented much more efficiently,
			Iterator iter = getPrefixes(namespaceURI);
			if (iter.hasNext()) return (String) iter.next();
			return null; // not found
		}

		/** {@inheritDoc} */
		public Iterator getPrefixes(String namespaceURI) {
			// TODO: could be implemented much more efficiently,
			// for example ala nux.xom.io.NamespacesInScope		
			if (namespaceURI == null) 
				throw new IllegalArgumentException("namespaceURI must not be null");
			
			// "http://www.w3.org/XML/1998/namespace" --> "xml"
			if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
				return getSingletonIterator(XMLConstants.XML_NS_PREFIX);
			}
			// "http://www.w3.org/2000/xmlns/" --> "xmlns"
			if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
				return getSingletonIterator(XMLConstants.XMLNS_ATTRIBUTE);
			}
			
			ArrayList prefixes = new ArrayList(1);
			Element elem = element;
			do {
				int count = elem.getNamespaceDeclarationCount();
				for (int i = 0; i < count; i++) {
					String prefix = elem.getNamespacePrefix(i);
					String uri = elem.getNamespaceURI(prefix);
					if (namespaceURI.equals(uri)) {
						if (!prefixes.contains(prefix)) {
							prefixes.add(prefix);
						}
					}
				}
				
				ParentNode parent = elem.getParent();
				elem = (parent instanceof Element ? (Element) parent : null);
			} while (elem != null); // walk towards the root

			return Collections.unmodifiableList(prefixes).iterator();
		}
		
		// TODO: replace with SingletonIterator class?
		private static Iterator getSingletonIterator(String str) {
			List prefixes = Arrays.asList(new String[] {str});
			return Collections.unmodifiableList(prefixes).iterator();
		}
					
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	private final class UnknownLocation implements Location {

		/** {@inheritDoc} */
		public int getLineNumber() { return -1; }
		
		/** {@inheritDoc} */
		public int getColumnNumber() { return -1; }
		
		/** {@inheritDoc} */
		public int getCharacterOffset() { return -1; }

		/** {@inheritDoc} */
		public String getPublicId() {
			// TODO: could try to find a DocType and take info from there, if available
			return null;
		}

		/** {@inheritDoc} */
		public String getSystemId() {
			String systemID = null;
			if (root != null) {
				// a baseURI isn't exactly a systemID, but better than nothing
				systemID = root.getBaseURI();
				if (systemID != null && systemID.length() == 0) systemID = null;
			}
			return systemID;
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	/** StAX extension hack to pass through DocType related info. */
	private static final class DTDInfo {

		private final DocType docType;

		private DTDInfo(DocType docType) {
			this.docType = docType;
		}

		public String getDTDRootName() {
			return docType.getRootElementName();
		}

		public String getDTDSystemId() {
			return docType.getSystemID();
		}

		public String getDTDPublicId() {
			return docType.getPublicID();
		}

		public String getDTDInternalSubset() {
			return docType.getInternalDTDSubset();
		}

	}

	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
//	/** Just in case user does not have javax.xml.XMLConstants on classpath. */
//	private static final class XMLConstants {
//		
//		private static final String XML_NS_PREFIX = "xml";
//		private static final String XML_NS_URI = "http://www.w3.org/XML/1998/namespace";
//				
//		private static final String XMLNS_ATTRIBUTE = "xmlns";
//		private static final String XMLNS_ATTRIBUTE_NS_URI = "http://www.w3.org/2000/xmlns/";
//		
//	}
	
}
