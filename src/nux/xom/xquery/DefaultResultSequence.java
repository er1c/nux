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
package nux.xom.xquery;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.transform.TransformerException;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.VirtualNode;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.AtomicValue;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.IllegalAddException;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;

/**
 * A forward-only iterator representing a Saxon XQuery result sequence of zero or more 
 * ordered results; allows to stream (pipeline) execution output, or to conveniently 
 * collect it in a batched manner.
 * <p>
 * Warning: Only to be used by XQuery.java
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek $
 * @version $Revision: 1.23 $, $Date: 2006/06/18 21:19:18 $
 */
final class DefaultResultSequence implements ResultSequence {
	
	/** the underlying saxon iterator producing result items */
	private SequenceIterator results;
	
	/** needed for getItemType() */
	private Configuration config; 
	
	/**
	 * The identities of non-XOM NodeInfos that have already been converted to
	 * XOM as part of this result sequence. This helps to maintain correct node
	 * identities for mixed sequences of non-XOM NodeInfos. Note that for XOM
	 * NodeWrappers this map is actually not needed and hence not used.
	 * 
	 * For example: seq := ($x, $x, root($x), $x/parent::*)
	 * 
	 * Here the identity of "$x" and its ancestors and descendants will be the
	 * same in all output (sub)trees.
	 */
	private HashMap alreadyConverted = null; // lazy instantiation on demand
	
	/** enable Nux debug output on System.err? */
	private static final boolean DEBUG = 
		XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.debug", false);
	
	/** strictly preserve Node identities even with non-XOM object models? */
	private static final boolean PRESERVE_IDENTITIES = 
		XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.preserveIdentities", true);
	
	private static final Element ATOMIC_VALUE;
	static { // avoids reverification; also let's share strings via a template element
		ATOMIC_VALUE = new Element("item:atomic-value", "http://dsd.lbl.gov/nux");
		ATOMIC_VALUE.addAttribute(
			new Attribute("xsi:type", "http://www.w3.org/2001/XMLSchema-instance", ""));
		ATOMIC_VALUE.appendChild("");
	}
	
	DefaultResultSequence(SequenceIterator results, Configuration config) {
		this.results = results;
		this.config = config;
	}
	
	public Nodes toNodes() throws XQueryException {
		Nodes nodes = new Nodes();
		Node next;
		while ((next = next()) != null) {
			nodes.append(next);
		}
		return nodes;
	}

	public Node next() throws XQueryException {
		if (this.results != null) {
			Item item;
			try { // pull from underlying saxon iterator
				item = this.results.next();
			} catch (TransformerException e) {
				throw new XQueryException(e);
			}
			if (item != null) return convertItem(item);
			this.results = null; // iterator exhausted; help gc
			this.config = null; // help gc
			this.alreadyConverted = null; // help gc
		}
		return null; 
	}
	
	/** Converts a saxon execution result node to XOM. */
	private Node convertItem(Item item) {
		Object base = item;
		while (base instanceof VirtualNode) { // unwrap XOM NodeWrapper, if any
			base = ((VirtualNode) base).getUnderlyingNode();
			if (base instanceof Node) { 
				return (Node)base; // the most common case; in particular XPath
			}
		}
		if (DEBUG) System.err.println(
			"item.getClass=" + (item == null ? "null" : item.getClass().getName()));
		
		if (item instanceof NodeInfo) { // coming from XQuery node constructors or similar
			return convertTree((NodeInfo)item);
		}
		else if (item instanceof AtomicValue) {
			return convertAtomicValue((AtomicValue)item);
		}
		else { // should never happen 
			throw new IllegalArgumentException(
				"Oops! Expected atomic value but found non-atomic type: " + 
				(item == null ? "null" : item.getClass().getName()));	
		}
	}
	
	/** Converts a saxon result node coming from XQuery node constructors to XOM. */
	private Node convertTree(NodeInfo node) {
		if (PRESERVE_IDENTITIES && this.alreadyConverted == null) {
			this.alreadyConverted = new HashMap();
		}
		Node value = convertNodeInfo(node);
		if (PRESERVE_IDENTITIES && node.getNodeKind() != Type.NAMESPACE) {
			NodeInfo root = node.getRoot();
			if (!root.isSameNodeInfo(node)) {
				if (DEBUG) System.err.println(
						"ROOT converting nodeinfo: " + root.getStringValue() + " : " + root);
				convertNodeInfo(root); // link value together with it's ancestor pointers
			}
		}
		return value;
	}
	
	/** Converts a saxon result node coming from XQuery node constructors to XOM. */
	private Node convertNodeInfo(NodeInfo node) {
		Node value;
		Object key = null;
		if (PRESERVE_IDENTITIES) {
			// if the node has already been converted return the previous conversion result.
			if (saxon86GenerateIdMethod == null) {
				key = node; // saxon >= 8.7
			} else {
				key = new NodeInfoIdentityKey(node); // saxon < 8.7		
			}
			value = (Node) this.alreadyConverted.get(key);
			if (value != null) {
				if (DEBUG) System.err.println("FOUND already converted nodeinfo: " 
						+ node.getStringValue() + " : " + node);
				return value;
			}
		}
		if (DEBUG) System.err.println("NEW  converting nodeinfo: " + 
				node.getStringValue() + " : " + node);
		
		switch (node.getNodeKind()) {
			case Type.ATTRIBUTE:
				value = convertAttributeNodeInfo(node);
				break;
			case Type.ELEMENT: 
				value = convertElementNodeInfo(node);
				break;
			case Type.DOCUMENT:
				value = convertDocumentNodeInfo(node);
				break;
			case Type.TEXT:
				value = new Text(node.getStringValue());
				break;
			case Type.COMMENT:
				value = new Comment(node.getStringValue());
				break;
			case Type.PROCESSING_INSTRUCTION:
				value = new ProcessingInstruction(node.getLocalPart(), node.getStringValue());
				break;
			case Type.NAMESPACE:
				value = convertAtomicValue(new AnyURIValue(node.getStringValue()));
//				value = new nu.xom.Namespace(node.getLocalPart(), node.getStringValue(), (Element) convertNodeInfo(node.getParent()));
				break;
			default: 
				throw new IllegalArgumentException(
					"Illegal NodeInfo kind: " + node.getClass().getName());
		}
		
		if (PRESERVE_IDENTITIES) {
			this.alreadyConverted.put(key, value); // remember conversion result
		}
		return value;
	}

	private Attribute convertAttributeNodeInfo(NodeInfo node) {
		return new Attribute(
			node.getDisplayName(), node.getURI(), node.getStringValue());			
	}
	
	private Document convertDocumentNodeInfo(NodeInfo node) {
		Document doc = new Document(new Element("fakeRoot"));
		doc.setBaseURI(node.getBaseURI());
		
		boolean hasRootElement = false;
		int i = 0;
		NodeInfo next;
		AxisIterator iter = node.iterateAxis(Axis.CHILD);
		while ((next = (NodeInfo) iter.next()) != null) {
			Node child = convertNodeInfo(next);
			if (child instanceof Element) { // replace fake root with real root
				if (hasRootElement) throw new IllegalAddException(
					"A XOM document must not have more than one root element.");
				doc.setRootElement((Element) child); 
				hasRootElement = true;
			} else {
				doc.insertChild(child, i);
			}
			i++;
		}
		if (!hasRootElement) throw new IllegalAddException(
			"Missing document root element; A XOM document must have a root element.");
		return doc;
	}
	
	private Element convertElementNodeInfo(NodeInfo node) {
		if (DEBUG) System.err.println("converting element=" + node.getDisplayName());
		Element elem = new Element(node.getDisplayName(), node.getURI());
		NodeInfo next;
		
		// Append attributes
		AxisIterator iter = node.iterateAxis(Axis.ATTRIBUTE);
		ArrayList prefixes = null;
		while ((next = (NodeInfo) iter.next()) != null) {
			elem.addAttribute((Attribute) convertNodeInfo(next));
			
			// keep track of attributes with prefixes, so we can avoid adding costly
			// additional namespaces declarations below. This safes potentially vast
			// amounts of memory due too XOM's expensive NS declaration storage scheme.
			String prefix = next.getPrefix();
			if (prefix.length() != 0) { // e.g. SOAP
				 if (prefixes == null) prefixes = new ArrayList(1);
				 prefixes.add(prefix);
			}
		}
		
		// Append namespace declarations (avoids unnecessary redeclarations).
		// For background see net.sf.saxon.om.NamespaceIterator and
		// net.sf.saxon.om.NamespaceDeclarationsImpl
		int[] namespaces = node.getDeclaredNamespaces(NodeInfo.EMPTY_NAMESPACE_LIST);
		int i = 0;
		int nsCode;
		NamePool pool = null;
		while (i < namespaces.length && (nsCode = namespaces[i]) != -1) {
			short uriCode = (short) (nsCode & 0xffff);
			if (uriCode != 0) { // it is not an undeclaration
				if (pool == null) pool = node.getNamePool();
				String uri = pool.getURIFromURICode(uriCode);
				String prefix = pool.getPrefixFromNamespaceCode(nsCode);
				
				if (prefixes != null && prefixes.contains(prefix)) {
					if (DEBUG) System.err.println(
						"Safely ignoring additional namespace declaration for prefix=" 
						+ prefix + ", uri=" + uri);
				}
				else {
					if (DEBUG) System.err.println(
						"Adding additional namespace declaration for prefix=" 
						+ prefix + ", uri=" + uri);
					elem.addNamespaceDeclaration(prefix, uri);
				}
			}
			i++;
		}
		prefixes = null; // help gc
		namespaces = null; // help gc
		
//		// Append namespace declarations (would introduce lots of redundant redeclarations)
//		iter = node.iterateAxis(Axis.NAMESPACE);
//		while ((next = (NodeInfo) iter.next()) != null) {
//			String prefix = next.getLocalPart();
//			String uri = next.getStringValue();
//			if (DEBUG) System.err.println("converted prefix=" + prefix + ", uri=" + uri);
//			elem.addNamespaceDeclaration(prefix, uri);
//		}
		
		// Append children (recursively)
		iter = node.iterateAxis(Axis.CHILD);
		while ((next = (NodeInfo) iter.next()) != null) {
			elem.appendChild(convertNodeInfo(next));
		}
		
		return elem;
	}
	
	/** Converts saxon's atomic value to XOM. */
	private Node convertAtomicValue(AtomicValue value) { 
		if (DEBUG) System.err.println("atomicValue.getClass="+value.getClass().getName());
		Element elem = new Element(ATOMIC_VALUE);  // copy to avoid reverification
		elem.getAttribute(0).setValue(getItemType(value)); // e.g. "xs:integer"
		Text text = ((Text)elem.getChild(0));
		text.setValue(value.getStringValue()); // e.g. "123"
		return elem;
	}
	
	// work-around for incompatibility introduced in saxon-8.6 and again in 8.7.1
	private static final Method saxon85Method = findGetItemTypeMethod85();
	private static final Method saxon86Method = findGetItemTypeMethod86();
	
	private String getItemType(AtomicValue value) {
		if (saxon85Method == null && saxon86Method == null) {
			try { // saxon >= 8.7.1
				return getItemType871(value);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		
		if (saxon86Method != null) { // saxon < 8.7.1 && >= 8.6
			try { 
				// value.getItemType(config.getNamePool().getTypeHierarchy()).toString();
				TypeHierarchy h = (TypeHierarchy) saxon86Method.invoke(config.getNamePool(), null);
				return value.getItemType(h).toString();
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}

		if (saxon85Method != null) { // saxon < 8.6
			try { 
				// return value.getItemType().toString();
				return saxon85Method.invoke(value, null).toString();
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		
		throw new RuntimeException("Unsupported saxon version?");
	}
	
	// saxon >= 8.7.1
	private String getItemType871(AtomicValue value) {
		return value.getItemType(config.getTypeHierarchy()).toString();
	}
	
	// saxon >= 8.6 && < 8.7.1
	private static Method findGetItemTypeMethod86() {
		try {
			return NamePool.class.getMethod("getTypeHierarchy", null);
		} catch (Throwable t) {
			return null;
		}
	}
	
	// saxon < 8.6
	private static Method findGetItemTypeMethod85() {
		try {
			return AtomicValue.class.getMethod("getItemType", null);
		} catch (Throwable t) {
			return null;
		}
	}
	

	
	// work-around for incompatibility introduced in saxon-8.7
	private static final Method saxon86GenerateIdMethod = findGenerateIdMethod86();
	
	private static Method findGenerateIdMethod86() {
		try {
			return NodeInfo.class.getMethod("generateId", null);
		} catch (Throwable t) {
			return null;
		}
	}
	
	private static String generateId86(NodeInfo node) {
		// saxon < 8.7
//		return node.generateId();
		try { 
			return saxon86GenerateIdMethod.invoke(node, null).toString();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
	
	static boolean isAtomicValue(Node node) {
		if (node instanceof Element) {
			Element elem = (Element) node;
			return elem.getLocalName().equals("atomic-value") && 
				elem.getNamespaceURI().equals("http://dsd.lbl.gov/nux");
		}
		return false;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	/** A key in the "alreadyConverted" map; saxon < 8.7 */
	private static final class NodeInfoIdentityKey {
		private final NodeInfo key;
		
		public NodeInfoIdentityKey(NodeInfo key) {
			this.key = key;
		}
		
		public boolean equals(Object other) {
			if (other instanceof NodeInfoIdentityKey) {
				return this.key.isSameNodeInfo(((NodeInfoIdentityKey)other).key);
			}
			return false;
		}
		
		public int hashCode() { 
			return generateId86(this.key).hashCode();
//			return this.key.generateId().hashCode();
////			key.getDocumentNumber(), key.getNodeKind(), key.tree.nodeNr;
		}
	}
				
}