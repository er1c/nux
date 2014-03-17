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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.IllegalAddException;
import nu.xom.Node;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.ParsingException;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import nu.xom.WellformednessException;
import nu.xom.XMLException;
import nux.xom.binary.NodeBuilder;

/**
 * Similar to the XOM {@link nu.xom.Builder} except that it builds a XOM
 * document using an underlying StAX pull parser rather than a SAX push parser,
 * inverting control flow.
 * <p>
 * StAX allows to explicitly iterate over the nodes of a document, in document
 * order, via streaming methods such as <code>next()</code> and
 * <code>hasNext()</code>. Processing can be stopped and resumed, and parts
 * of a document can easily be skipped or filtered. In particular, individual 
 * nodes or fragments (i.e. subtrees) can be pulled and converted to XOM via methods
 * {@link #buildNode()} and {@link #buildFragment()}, respectively.
 * <p>
 * Perhaps more importantly, control flow, data flow as well as state and resource 
 * management can often be controlled more tightly with a pull iterator API, rather 
 * than a callback driven push API such as SAX. For example, database query execution 
 * subsystems are typically based on (distributed) pull operator trees.
 * Similarly, modular SOAP stacks typically prefer StAX, as outlined in 
 * <a href="http://www-128.ibm.com/developerworks/xml/library/x-axiom/">AXIOM StAX introduction</a>.
 * Requiring an application to convert a push API to a pull API is 
 * both complex and inefficient (whereas the reverse is not true).
 * <p>
 * This class requires the StAX interfaces and a StAX parser implementation to
 * be on the classpath. For example Woodstox (recommended) or Sun's sjsxp.
 * Woodstox is the only StAX parser known to be exceptionally conformant, reliable, 
 * complete <i>and</i> efficient.
 * At this time, other underlying StAX parsers may not 
 * perform full wellformedness checking, tend to have incomplete or buggy
 * support for DTD, entities, external references, and are in general not 
 * as mature as underlying SAX parsers such as Xerces. 
 * <p>
 * An instance of this class is not thread-safe.
 * <p>
 * 
 * Example Usage: Print each article in a list of millions of articles via 
 * <code>buildFragment()</code>:
 * 
 * <pre>
 * InputStream in = new FileInputStream("samples/data/articles.xml");
 * XMLStreamReader reader = StaxUtil.createXMLStreamReader(in, null);
 * reader.require(XMLStreamConstants.START_DOCUMENT, null, null);
 * reader.nextTag(); // move to "articles" root element
 * reader.require(XMLStreamConstants.START_ELEMENT, null, "articles");
 * 
 * while (reader.nextTag() == XMLStreamConstants.START_ELEMENT) { // yet another article
 *     reader.require(XMLStreamConstants.START_ELEMENT, null, "article");
 *     
 *     Document fragment = new StaxParser(reader, new NodeFactory()).buildFragment();
 *      
 *     // do something useful with the fragment...
 *     System.out.println("fragment = "+ fragment.getRootElement().toXML());    
 * }	
 * 
 * reader.close();
 * in.close();
 * </pre>
 * 
 * 
 * Example: Print all events in document order via <code>buildNode()</code>:
 * 
 * <pre>
 * InputStream in = new FileInputStream("samples/data/articles.xml");
 * XMLStreamReader reader = StaxUtil.createXMLStreamReader(in, null);
 * StaxParser parser = new StaxParser(reader, new NodeFactory()); 
 * int depth = 0;
 * int ev;
 * while ((ev = reader.getEventType()) != XMLStreamConstants.END_DOCUMENT) {
 *     if (ev == XMLStreamConstants.START_ELEMENT) depth++;
 *     
 *     // do something useful with the node...
 *     Node node = parser.buildNode();
 *     System.out.println(depth + ":" + StaxUtil.toString(ev) + ":" + node.toXML());
 *     
 *     if (ev == XMLStreamConstants.END_ELEMENT) depth--;
 *     reader.next();
 * }
 * 
 * reader.close();
 * in.close();
 * </pre>
 * 
 * Using JDBC 4's SQLXML data type, you could retrieve a user's blog entries from a database as follows:
 * <pre>
 * Connection conn = myDataSource.getConnection();
 * PreparedStatement st = conn.prepareStatement("select userid, blog_entry from user_has_blog");
 * ResultSet rs = st.executeQuery();
 * while (rs.next()) {
 *     SQLXML blog = st.getSQLXML("blog_entry");
 *     javax.xml.stream.XMLStreamReader reader = blog.createXMLStreamReader();
 *     Document doc = new StaxParser(reader, new NodeFactory()).build();
 *     System.out.println(doc.toXML());
 *     blog.free();
 * }
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek $
 * @version $Revision: 1.20 $, $Date: 2006/06/19 01:38:21 $
 */
 public class StaxParser {
	 
	// TODO: optimization: use XMLStreamReader2.getPrefixedName() if available?
	 
	/** The underlying StAX parser. */
	private final XMLStreamReader reader;
		
	/** The optional XOM factory to push into. */
	private final NodeFactory factory;
	
	/** Small fast cache for the most recent elements and attributes. */
	private NodeBuilder nodeBuilder;
	
	private static final Nodes NONE = new Nodes();
	
	/** Map of StAX String --> XOM Attribute.Type */
	private static final HashMap attrTypes = createAttributeTypes();
	
	private static final boolean DEBUG = false;
	
	/**
	 * Constructs a new instance that pushes into the given node factory.
	 * 
	 * @param reader
	 *            the underlying StAX pull parser to read from
	 * @param factory
	 *            the node factory to stream into. May be <code>null</code> in
	 *            which case the default XOM NodeFactory is used, building the
	 *            full XML tree.
	 */
	public StaxParser(XMLStreamReader reader, NodeFactory factory) {
		if (reader == null) 
			throw new IllegalArgumentException("reader must not be null");	
		this.reader = reader;
		if (factory == null) factory = new NodeFactory();
		this.factory = factory;
		if (DEBUG) System.err.println("StAX parser=" + reader.getClass().getName());
		if (!isNamespaceAware(reader)) 
			throw new IllegalArgumentException("reader must be namespace aware");
	}

	/**
	 * Returns the StAX pull parser previously given on instance construction.
	 * 
	 * @return the underlying StAX pull parser.
	 */
	public XMLStreamReader getXMLStreamReader() {
		return reader;
	}
	
	/**
	 * Builds the current document until the corresponding <code>END_DOCUMENT</code>
	 * event is seen. Requires that the reader is positioned over a 
	 * <code>START_DOCUMENT</code> event.
	 * <p>
	 * Example usage:
	 * <pre>
	 * InputStream in = new FileInputStream("samples/data/articles.xml");
	 * XMLStreamReader reader = StaxUtil.createXMLStreamReader(in, null);
	 * Document doc = new StaxParser(reader, new NodeFactory()).build();
	 * System.out.println(doc.toXML());
	 * in.close();
	 * </pre>
	 * 
	 * @return the parsed XOM document
	 * 
	 * @throws IllegalStateException
	 *             if <code>reader.getEventType() != XMLStreamConstants.START_DOCUMENT</code>
	 * @throws ParsingException 
	 *             if there is an error processing the underlying XML source
	 */
	public Document build() throws ParsingException {
		return build(false);
	}
	
	/**
	 * Builds the current element subtree until the corresponding <code>END_ELEMENT</code>
	 * event is seen; returns a document rooted at that element. Requires that the 
	 * reader is positioned over a <code>START_ELEMENT</code> event.
	 * <p>
	 * If this method returns successfully the cursor will be positioned over the 
	 * corresponding <code>END_ELEMENT</code>.
	 * 
	 * @return the parsed XOM document
	 * 
	 * @throws IllegalStateException
	 *             if <code>reader.getEventType() != XMLStreamConstants.START_ELEMENT</code>
	 * @throws ParsingException 
	 *             if there is an error processing the underlying XML source
	 */
	public Document buildFragment() throws ParsingException {
		return build(true);
	}
	
	/**
	 * Creates and returns a new shallow XOM Node for the current StAX event the
	 * cursor is positioned over.
	 * <p>
	 * If the current event is a START_ELEMENT, defined attributes and
	 * namespaces are added to the returned element. If the current event is an
	 * END_ELEMENT, only defined namespaces are added to the returned element.
	 * <p>
	 * This method does not advance the cursor/iterator, and it does not
	 * use a NodeFactory. Currently ignores
	 * XMLStreamConstants.ENTITY_DECLARATION and
	 * XMLStreamConstants.NOTATION_DECLARATION, returning null for these cases.
	 * 
	 * @return a shallow XOM Node corresponding to the current StAX event.
	 * @throws ParsingException 
	 *             if there is an error processing the underlying XML source
	 */
	public Node buildNode() throws ParsingException {
		if (nodeBuilder == null) nodeBuilder = new NodeBuilder();
		
		switch (reader.getEventType()) {
			case XMLStreamConstants.START_ELEMENT: {
				Element elem = readStartTag();
				addAttributes(elem);
				addNamespaceDeclarations(elem);
				return elem;
			}
			case XMLStreamConstants.END_ELEMENT: {
				Element elem = readStartTag();
				// StAX does not report attributes on END_ELEMENT
				addNamespaceDeclarations(elem);
				return elem;
			}
			case XMLStreamConstants.ATTRIBUTE: { // TODO: optimize
				Element elem = nodeBuilder.createElement("dummy", "");
				addAttributes(elem);
				return elem.getAttribute(0).copy();
			}
			case XMLStreamConstants.START_DOCUMENT:
			case XMLStreamConstants.END_DOCUMENT:
				return new NodeFactory().startMakingDocument();
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				return new ProcessingInstruction(
						reader.getPITarget(), reader.getPIData());
			case XMLStreamConstants.COMMENT:
				return new Comment(reader.getText());
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.ENTITY_REFERENCE: 
			case XMLStreamConstants.CHARACTERS:
				return readText();
			case XMLStreamConstants.DTD: {
				Nodes nodes = null;
				try {
					nodes = readDocType(new NodeFactory());
				} catch (XMLStreamException e) {
					StaxUtil.wrapException(e);
				}
				if (nodes.size() > 0) return nodes.get(0);
				return null; // unsupported extended DTD API
			}
			case XMLStreamConstants.ENTITY_DECLARATION: 
				return null; // ignore
			case XMLStreamConstants.NOTATION_DECLARATION: 
				return null; // ignore
//			case XMLStreamConstants.NAMESPACE: // FIXME
//				// StAX spec does not specify how to read prefix 
//				// and URI of namespace node !?
//				
//				// requires xom >= 1.1
//				return new nu.xom.Namespace(
//					reader.getPrefix(), reader.getNamespaceURI(), null); 
			default:
				throw new XMLException("Unrecognized event type: " 
						+ reader.getEventType());
		}
	}
	
	private Document build(boolean isFragmentMode) throws ParsingException {
		try {
			try {
				return buildTree(isFragmentMode);
			} finally {
				if (!isFragmentMode && reader != null) reader.close();
			}
		} catch (XMLStreamException e) {
			StaxUtil.wrapException(e);
			return null; // unreachable
		}
	}	
	
	private Document buildTree(boolean isFragmentMode) throws XMLStreamException {
		if (isFragmentMode) {
			reader.require(XMLStreamConstants.START_ELEMENT, null, null);
		} else {
			reader.require(XMLStreamConstants.START_DOCUMENT, null, null);
		}
		
		Document doc = factory.startMakingDocument();
		boolean hasRootElement = false;
		boolean done = false;
		int i = 0;
		
		while (!done && reader.getEventType() != XMLStreamConstants.END_DOCUMENT) {
			
			Nodes nodes;
//			if (DEBUG) System.out.println(toString(reader.getEventType()));
			switch (reader.getEventType()) {
				case XMLStreamConstants.START_ELEMENT: {
					if (hasRootElement) throw new IllegalAddException(
						"StAX reader must not return multiple root elements");

					if (factory.getClass() == NodeFactory.class) { // fast path
						if (nodeBuilder == null) nodeBuilder = new NodeBuilder();
						Element root = readStartTag();
						addAttributes(root);
						addNamespaceDeclarations(root);
						readElement(root); // reads entire subtree
						nodes = new Nodes(root);
					} else { // slow path			
						Element root = readStartTagF(true);
						if (root == null) {
							throw new NullPointerException(
								"Factory failed to create root element.");
						}
						doc.setRootElement(root);
						addAttributesF(root);
						addNamespaceDeclarations(root);
						readElementF(root); // read entire subtree
						nodes = factory.finishMakingElement(root);
					}
					reader.require(XMLStreamConstants.END_ELEMENT, null, null);
					if (isFragmentMode) done = true;
					break;
				}
				case XMLStreamConstants.END_ELEMENT:
					throw new IllegalAddException(
						"A document must not have more than one root element");
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					nodes = factory.makeProcessingInstruction(
							reader.getPITarget(), reader.getPIData());
					break;
				case XMLStreamConstants.CHARACTERS:
					nodes = NONE; // ignore text in prolog/epilog
					break;
				case XMLStreamConstants.COMMENT:
					nodes = factory.makeComment(reader.getText());
					break;
				case XMLStreamConstants.SPACE:
					nodes = NONE; // ignore text in prolog/epilog
					break;
				case XMLStreamConstants.START_DOCUMENT:
					nodes = NONE; // has already been handled previously
					break;
				case XMLStreamConstants.END_DOCUMENT:
					throw new IllegalStateException("unreachable");
				case XMLStreamConstants.CDATA:
					nodes = NONE; // ignore text in prolog/epilog
					break;
				case XMLStreamConstants.ATTRIBUTE:
					throw new IllegalAddException(
						"Illegal attribute in prolog/epilog");
				case XMLStreamConstants.NAMESPACE:
					throw new IllegalAddException(
						"Illegal namespace declaration in prolog/epilog");
				case XMLStreamConstants.DTD: 
					nodes = readDocType(factory); // FIXME
					break;
				case XMLStreamConstants.ENTITY_DECLARATION: 
					nodes = NONE; // ignore (missing StAX support)
					break;
				case XMLStreamConstants.NOTATION_DECLARATION: 
					nodes = NONE; // ignore (missing StAX support)
					break;
				case XMLStreamConstants.ENTITY_REFERENCE: 
					nodes = NONE; // ignore text in prolog/epilog
					break;
				default:
					throw new XMLException("Unrecognized Stax event type: " 
							+ reader.getEventType());
			}
			
			// append nodes:
			for (int j=0; j < nodes.size(); j++) {
				Node node = nodes.get(j);
				if (node instanceof Element) { // replace fake root with real root
					if (hasRootElement) {
						throw new IllegalAddException(
							"Factory returned multiple root elements");
					}
					doc.setRootElement((Element) node); 
					hasRootElement = true;
				} else {
					doc.insertChild(node, i);
				}
				i++;
			}
			
			if (!isFragmentMode) reader.next();
		}
		
		if (!isFragmentMode) {
			reader.require(XMLStreamConstants.END_DOCUMENT, null, null);
		}
		if (!hasRootElement) {
			throw new WellformednessException(
					"Factory attempted to remove the root element");
		}
		factory.finishMakingDocument(doc);
				
		// Set baseURI unless already set previously by NodeFactory
		// to ensure exact same behaviour as nu.xom.Builder.build(InputSource)
		if ("".equals(doc.getBaseURI())) {
			Location loc = reader.getLocation();
			String baseURI = loc == null ? null : loc.getSystemId();
			if (baseURI != null && baseURI.length() > 0) {
				doc.setBaseURI(baseURI);
			}
		}
		
		return doc;
	}
	
	/** Iterative pull parser reading an entire element subtree */
	private void readElement(Element current) throws XMLStreamException {
		
		while (true) {
			Node node = null;
			
			switch (reader.next()) {
				case XMLStreamConstants.START_ELEMENT: {
					Element elem = readStartTag();
					current.insertChild(elem, current.getChildCount());
					addAttributes(elem);
					addNamespaceDeclarations(elem);
					current = elem; // recurse down
					continue;
				}
				case XMLStreamConstants.END_ELEMENT: {
					current = (Element) current.getParent(); // recurse up
					if (current == null) return; // we're done with the root element
					continue;
				}
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					node = new ProcessingInstruction(
							reader.getPITarget(), reader.getPIData());
					break;
				case XMLStreamConstants.COMMENT:
					node = new Comment(reader.getText());
					break;
				case XMLStreamConstants.SPACE:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.ENTITY_REFERENCE: 
				case XMLStreamConstants.CHARACTERS:
					node = readText();
					break;
					
				default:
					throw new XMLException("Unrecognized Stax event type: " 
							+ reader.getEventType());			
			}
			
			// assert node != null
//			if (IS_EXTENDED_XOM) { // xom-1.1 + patch // TODO
//				current.fastInsertChild(node, current.getChildCount());
//			} else {
				current.insertChild(node, current.getChildCount());
//			}
		}
	}
		
	/** Iterative pull parser reading an entire element subtree, using NodeFactory. */
	private void readElementF(Element current) throws XMLStreamException {
		
		final ArrayList stack = new ArrayList();
		stack.add(current); // push

		while (true) {
			Nodes nodes;
			switch (reader.next()) {	
				case XMLStreamConstants.START_ELEMENT: {
					Element elem = readStartTagF(false);
					stack.add(elem); // push even if it's null
					if (elem != null) { 
						current.appendChild(elem);
						addAttributesF(elem);
						addNamespaceDeclarations(elem);
						current = elem; // recurse down
					}
					continue;
				}
				case XMLStreamConstants.END_ELEMENT: {
					Element elem = (Element) stack.remove(stack.size()-1); // pop
					if (elem == null) {
						continue; // skip element
					}
					ParentNode parent = elem.getParent();
					if (parent == null) throwTamperedWithParent();
					if (parent instanceof Document) {
						return; // we're done with the root element
					}
					
					current = (Element) parent; // recurse up
					nodes = factory.finishMakingElement(elem);
										 
					if (nodes.size()==1 && nodes.get(0)==elem) { // same node? (common case)
						continue; // optimization: no need to remove and then readd same element
					}
					
					if (current.getChildCount()-1 < 0) throwTamperedWithParent();				
					current.removeChild(current.getChildCount()-1);
					break;
				}
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					nodes = factory.makeProcessingInstruction(
							reader.getPITarget(), reader.getPIData());
					break;
				case XMLStreamConstants.COMMENT:
					nodes = factory.makeComment(reader.getText());
					break;
				case XMLStreamConstants.SPACE:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.ENTITY_REFERENCE: 
				case XMLStreamConstants.CHARACTERS:
					nodes = factory.makeText(reader.getText());
					break;
					
				default:
					throw new XMLException("Unrecognized Stax event type: " 
							+ reader.getEventType());
			}
			
			appendNodes(current, nodes);
		}
	}
	
	private Element readStartTag() {
		String prefix = reader.getPrefix();
		String qname = reader.getLocalName();
		if (prefix != null && prefix.length() > 0) {
			qname = prefix + ':' + qname;
		}
	
		String namespaceURI = reader.getNamespaceURI();
		return nodeBuilder.createElement(qname, namespaceURI);
//		return new Element(qname, namespaceURI);
	}

	private Element readStartTagF(boolean isRoot) {
		String prefix = reader.getPrefix();
		String qname = reader.getLocalName();
		if (prefix != null && prefix.length() > 0) {
			qname = prefix + ':' + qname;
		}
		
		String namespaceURI = reader.getNamespaceURI();
		if (namespaceURI == null) namespaceURI = "";
		
		return isRoot ? 
			factory.makeRootElement(qname, namespaceURI) :
			factory.startMakingElement(qname, namespaceURI);
	}

	private static void appendNodes(Element elem, Nodes nodes) {
		if (nodes != null) {
			int size = nodes.size();
			for (int i=0; i < size; i++) {
				Node node = nodes.get(i);
				if (node instanceof Attribute) {
					elem.addAttribute((Attribute) node);
				} else {
					elem.insertChild(node, elem.getChildCount());
				}
			}
		}
	}
	
	private static void throwTamperedWithParent() {
		throw new XMLException("Factory has tampered with a parent pointer " + 
				"of ancestor-or-self in finishMakingElement()");
	}
	
	private void addNamespaceDeclarations(Element elem) {
		int count = reader.getNamespaceCount();
		for (int i = 0; i < count; i++) {
			String prefix = reader.getNamespacePrefix(i);
			if (prefix == null) prefix = "";
			String uri = reader.getNamespaceURI(i);
			
			/*
			 * Supress superflous namespace redeclarations no matter what the
			 * XMLStreamReader reports. This is actually unnecessary for
			 * woodstox, but it may well be necessary for other StAX impls.
			 */
			String uriInScope = elem.getNamespaceURI(prefix);
			boolean isAbsent = uriInScope == null || !uriInScope.equals(uri);
			
			if (isAbsent) elem.addNamespaceDeclaration(prefix, uri);
		}
	}
	
	private void addAttributes(Element elem) {
		int count = reader.getAttributeCount();
		for (int i = 0; i < count; i++) {
			String prefix = reader.getAttributePrefix(i);
			String qname = reader.getAttributeLocalName(i);		
			if (prefix != null && prefix.length() > 0) {
				qname = prefix + ':' + qname;
			}
			
			String namespaceURI = reader.getAttributeNamespace(i);
			String value = reader.getAttributeValue(i);			
			Attribute.Type type = convertAttributeType(reader.getAttributeType(i));
			
			Attribute attr = nodeBuilder.createAttribute(qname, namespaceURI, value, type);
//			Attribute attr = new Attribute(qname, namespaceURI, value, type);
			elem.addAttribute(attr);
		}
	}
	
	private void addAttributesF(Element elem) {
		int count = reader.getAttributeCount();
		for (int i = 0; i < count; i++) {
			String prefix = reader.getAttributePrefix(i);
			String qname = reader.getAttributeLocalName(i);		
			if (prefix != null && prefix.length() > 0) {
				qname = prefix + ':' + qname;
			}
			
			String namespaceURI = reader.getAttributeNamespace(i);
			if (namespaceURI == null) namespaceURI = "";
			
			String value = reader.getAttributeValue(i);
			Attribute.Type type = convertAttributeType(reader.getAttributeType(i));
			
			appendNodes(elem, factory.makeAttribute(qname, namespaceURI, value, type));
		}
	}
	
	private static Attribute.Type convertAttributeType(String staxType) {
		if (staxType != null && staxType.length() > 0) {
			Attribute.Type xomType = (Attribute.Type) attrTypes.get(staxType);
			if (xomType != null) return xomType;
		}
		return Attribute.Type.UNDECLARED;
	}
	
	private Text readText() {
		return new Text(reader.getText());
	}
    
	/**
	 * Standard StAX API does not provide enough info... We use a hacky
	 * workaround via woodstox >= 2.0.x StAX extensions if found to be available
	 * via reflection, so woodstox doesn't become a dependency. see
	 * org.codehaus.stax2.XMLStreamReader2.getDTDInfo()
	 * 
	 * TODO: find additional workaround for Sun's StAX impl. 
	 */
	private static final String MISSING_StAX2 = new String("missing_stax2"); // unique object

	private Nodes readDocType(NodeFactory nodeFactory) throws XMLStreamException {
//		if (DEBUG) System.err.println("DTD reader="+reader.getClass().getName());
		Object info = invoke(reader, "getDTDInfo");
		if (info == null || info == MISSING_StAX2) return NONE;
//		Nodes nodes = factory.makeDocType(
//			info.getDTDRootName(), 
//			info.getDTDPublicId(),
//			info.getDTDSystemId());
//		return nodes;
		
		String rootName = (String) invoke(info, "getDTDRootName");
		if (rootName == MISSING_StAX2) return NONE;
		String publicID = (String) invoke(info, "getDTDPublicId");
		if (publicID == MISSING_StAX2) return NONE;
		String systemID = (String) invoke(info, "getDTDSystemId");
		if (systemID == MISSING_StAX2) return NONE;
		
		Nodes nodes = nodeFactory.makeDocType(rootName, publicID, systemID);
		for (int k=0; k < nodes.size(); k++) {
			Node node = nodes.get(k);
			if (node instanceof DocType) {
				DocType docType = (DocType) node;
				if (docType.getInternalDTDSubset().length() == 0) {
					// xom >= 1.1 only
					String subset = (String) invoke(info, "getDTDInternalSubset");
					if (subset == MISSING_StAX2) return nodes;
					docType.setInternalDTDSubset(subset);
				}
			}
		}
		return nodes;
	}
	
	private static Object invoke(Object obj, String methodName) throws XMLStreamException {
		try {
			return obj.getClass().getMethod(methodName, null).invoke(obj, null);
		} catch (IllegalArgumentException e) {
			if (DEBUG) e.printStackTrace();
			return MISSING_StAX2;
		} catch (SecurityException e) {
			if (DEBUG) e.printStackTrace();
			return MISSING_StAX2;
		} catch (IllegalAccessException e) {
			if (DEBUG) e.printStackTrace();
			return MISSING_StAX2;
		} catch (NoSuchMethodException e) {
			if (DEBUG) e.printStackTrace();
			return MISSING_StAX2;
		} catch (InvocationTargetException e) {
			if (DEBUG) e.printStackTrace();
			Throwable cause = e.getCause();
			if (cause instanceof XMLStreamException) {
				throw (XMLStreamException) cause;
			} else if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		}
	}
	
	private static HashMap createAttributeTypes() {
		HashMap typeMappings = new HashMap();
		typeMappings.put("CDATA", Attribute.Type.CDATA);
		typeMappings.put("cdata", Attribute.Type.CDATA);
		typeMappings.put("ID", Attribute.Type.ID);
		typeMappings.put("id", Attribute.Type.ID);
		typeMappings.put("IDREF", Attribute.Type.IDREF);
		typeMappings.put("idref", Attribute.Type.IDREF);
		typeMappings.put("IDREFS", Attribute.Type.IDREFS);
		typeMappings.put("idrefs", Attribute.Type.IDREFS);
		typeMappings.put("ENTITY", Attribute.Type.ENTITY);
		typeMappings.put("entity", Attribute.Type.ENTITY);
		typeMappings.put("ENTITIES", Attribute.Type.ENTITIES);
		typeMappings.put("entities", Attribute.Type.ENTITIES);
		typeMappings.put("NMTOKEN", Attribute.Type.NMTOKEN);
		typeMappings.put("nmtoken", Attribute.Type.NMTOKEN);
		typeMappings.put("NMTOKENS", Attribute.Type.NMTOKENS);
		typeMappings.put("nmtokens", Attribute.Type.NMTOKENS);
		typeMappings.put("NOTATION", Attribute.Type.NOTATION);
		typeMappings.put("notation", Attribute.Type.NOTATION);
		typeMappings.put("ENUMERATED", Attribute.Type.ENUMERATION);
		typeMappings.put("enumerated", Attribute.Type.ENUMERATION);
		return typeMappings;
	}

	private static boolean isNamespaceAware(XMLStreamReader reader) {
		Boolean isNamespaceAware = (Boolean) reader.getProperty(
				XMLInputFactory.IS_NAMESPACE_AWARE);
		if (DEBUG) System.err.println("isNamespaceAware=" + isNamespaceAware);
		return isNamespaceAware != null && isNamespaceAware.booleanValue();
	}

	/**
	 * TODO: make this public?
	 * 
	 * Returns the sequence of XOM Nodes represented by the underlying StAX
	 * XMLStreamReader, for example an XPath or XQuery result sequence.
	 * <p>
	 * TODO: how do we model an empty result sequence? 
	 * a) inital state undefined except hasNext()? 
	 * b) flag empty sequence via initial getEventType() < 0? (EMPTY_SEQUENCE) 
	 * c) wrap sequence into another outer START_DOCUMENT, ..., END_DOCUMENT event pair, or START_INPUT, ..., END_INPUT event pair?
	 * <p> 
	 * TODO: method returns Nodes or nux.xom.xquery.ResultSequence (pipelined)?
	 * TODO: how to model an XPath atomic value? as an individual CHARACTERS event?
	 * TODO: how does all this correspond to W3C XQuery Serialization spec, XQTS, saxon pull provider, DataDirect/BEA? 
	 * e.g. in the area of top level attributes, namespace nodes and sequence normalization?
	 * 
	 * @return a node sequence
	 * @throws ParsingException
	 */
	private Nodes buildSequence() throws ParsingException {
		// approach a)
		// assert: we are currently positioned *before* the first event, with the
		// behaviour of all methods initially undefined, except for reader.hasNext()
		try {
			Nodes results = new Nodes();
			while (reader.hasNext()) {
				Node node;
				switch (reader.next()) {
					case XMLStreamConstants.START_DOCUMENT:
						node = build(); // FIXME don't auto-close reader?
						break;
					case XMLStreamConstants.START_ELEMENT:
						node = buildFragment();
						break;
					default:
						node = buildNode();
						break;
				}
				results.append(node);
			}
			return results;
		} catch (XMLStreamException e) {
			StaxUtil.wrapException(e);
			return null; // unreachable
		}
		
		// can't model empty sequence:
		// results = ();
		// do {
		//    if its a START_DOC do build(), if its a START_ELEM du buildFragment(), else do buildNode()
	    //    result.append(x)
		//    boolean hasNext = hasNext()
		//    if (hasNext) next();
		// } while (hasNext)
	}

}
