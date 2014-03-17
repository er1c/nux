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

import java.util.HashSet;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import nux.xom.pool.XQueryPool;

/**
 * Various utilities avoiding redundant code in several classes.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.34 $, $Date: 2006/03/11 06:52:48 $
 */
public class XQueryUtil {
	
	private static final Nodes EMPTY = new Nodes();
		
	private XQueryUtil() {} // not instantiable
		
	/**
	 * Executes the given W3C XQuery or XPath against the given context node
	 * (subtree); convenience method. Equivalent to
	 * 
	 * <pre>
	 * return XQueryPool.GLOBAL_POOL.getXQuery(query, null).execute(contextNode).toNodes();
	 * </pre>
	 * 
	 * Example usage:
	 * <pre>
	 * // find the atom named 'Zinc' in the periodic table:
	 * Document doc = new Builder().build(new File("samples/data/periodic.xml"));
	 * Node result = XQueryUtil.xquery(doc, "/PERIODIC_TABLE/ATOM[NAME = 'Zinc']").get(0);
	 * System.out.println("result=" + result.toXML());
	 * </pre>
	 * 
	 * @param contextNode
	 *            the context node to execute the query against. The context
	 *            node is available to the query as the value of the query
	 *            expression ".". If this parameter is <code>null</code>, the
	 *            context node will be undefined.
	 * @param query
	 *            the XQuery or XPath string
	 * @return the nodes of the result sequence.
	 * @throws RuntimeException
	 *             if an XQueryException occurs (unchecked exception for
	 *             convenience)
	 * @see XQuery
	 * @see XQueryPool
	 */
	public static Nodes xquery(Node contextNode, String query) {
		try {
			return XQueryPool.GLOBAL_POOL.getXQuery(query, null).execute(contextNode).toNodes();
		} catch (XQueryException e) { // part of the "convenience"
			throw new RuntimeException(e);
		}
	}

	/**
	 * EXPERIMENTAL; Simple yet powerful and efficient in-place <i>morphing </i> for use as an
	 * XQuery/XPath insert, update and delete facility; particularly useful for
	 * structurally small tree transformations without requiring (potentially
	 * huge) XML tree copies. Morphing is not in general intended as a
	 * replacement for constructive XQuery or XSLT.
	 * <p>
	 * To get started, see the <a href="doc-files/update-examples.txt">example
	 * use cases </a> and observe that various types of insert, update and
	 * delete can be concisely expressed via morphing.
	 * <p>
	 * The morphing algorithm works as follows: For each node <code>N</code>
	 * in the given <code>nodes</code> sequence, let <code>results</code> be
	 * the node sequence returned by <code>morpher.execute(N, null, vars)</code>.
	 * Now...
	 * <ul>
	 * <li>If <code>results</code> is the empty sequence then <code>N</code>
	 * is detached (i.e. deleted) from its parent.</li>
	 * 
	 * <li>Otherwise, let <code>atomic</code> be the string concatenation of
	 * the standard XPath string value of all atomic values in
	 * <code>results</code> (separating string values by a single space character), 
	 * and let <code>nonAtomic</code> be the list of
	 * all non-atomic items in <code>results</code>. If there is at least one
	 * atomic value, replace the content of <code>N</code> with
	 * <code>atomic</code>. If there is at least one non-atomic value,
	 * replace <code>N</code> with <code>nonAtomic</code>, updating the
	 * parent of <code>N</code>.</li>
	 * </ul>
	 * <p>
	 * <a target="_blank"
	 * href="http://www.w3.org/TR/xpath-functions/#datatypes">Atomic types </a>
	 * include xs:string, xs:integer, xs:double, xs:boolean, etc, all of which
	 * are not XML nodes.
	 * <p>
	 * Note that if a morpher result sequence contains multiple 
	 * identical nodes, copies of those nodes will be made.
	 * <p>
	 * Example usage:
	 * <pre>
	 *     // read document from file
	 *     Document doc = new Builder().build(new File("samples/data/articles.xml"));
	 * 
	 *     // make all articles a bit cheaper
	 *     XQueryUtil.update(doc, "//article/prize", ". * 0.95");
	 * 
	 *     // delete all chairs
	 *     XQueryUtil.update(doc, "//article[@name='chair']", "()");
	 * 
	 *     // write updated document to file
	 *     FileOutputStream out = new FileOutputStream("samples/data/articles2.xml");
	 *     Serializer ser = new Serializer(out);
	 *     ser.write(doc);
	 *     out.close();
	 * </pre>
	 * 
	 * @param nodes
	 *            the list of nodes to morph
	 * @param morpher
	 *            an XQuery or XPath query morphing each node in
	 *            <code>nodes</code> into a new form (may be <code>null</code>
	 *            in which case all nodes will be detached, i.e. deleted)
	 * @param variables
	 *            the morpher's external global variables (may be
	 *            <code>null</code>).
	 * @throws RuntimeException
	 *             if an XQueryException occurs (unchecked exception for
	 *             convenience)
	 */
	public static void update(Nodes nodes, XQuery morpher, Map variables) {
		if (nodes == null)
			throw new IllegalArgumentException("nodes must not be null");
		
		/*
		 * It is recommended (but not strictly necessary) that parameter
		 * <code>nodes</code> does not contain duplicates wrt. node identity.
		 */
		HashSet identities = null;
		for (int i=nodes.size(); --i >= 0; ) {
			Node node = nodes.get(i);
			Nodes results = EMPTY;
			if (morpher != null) {
				try {
					results = morpher.execute(node, null, variables).toNodes();
				} catch (XQueryException e) { // part of the "convenience"
					throw new RuntimeException(e);
				}
			}
			
			int size = results.size();
			if (size == 0) { // pure delete?
				node.detach();
				continue; // not really needed; just for clarity
			}
			
			if (size == 1 && node == results.get(0)) {
				continue; // nothing to do (replace X with X)
			}

			ParentNode parent = node.getParent();
			StringBuffer atomics = null;
			boolean isInitialized = false;
			int position = 0;
			if (size > 1) {
				if (identities == null) {
					identities = new HashSet();
				} else {
					identities.clear();
				}
			}
			
			for (int j=0; j < size; j++) {
				Node result = results.get(j);				
				if (DefaultResultSequence.isAtomicValue(result)) { // concat atomic values
					String value = result.getValue();
					if (atomics == null) {
						atomics = new StringBuffer(value.length());
					} else {
						atomics.append(' ');
					}
					atomics.append(value);
				} else if (parent != null) {
					if (size > 1 && !identities.add(result)) { 
						result = result.copy(); // multiple identical nodes in results
//						throw new MultipleParentException(
//						"XQuery morpher result sequence must not contain multiple identical nodes");
					}
					boolean isRoot = parent instanceof Document && node instanceof Element;
					if (!isInitialized) {
						if (!(node instanceof Attribute)) position = parent.indexOf(node);
						if (!isRoot) node.detach();
						isInitialized = true;
					}
					
					if (result instanceof Attribute) {
						result.detach();
						((Element) parent).addAttribute((Attribute)result);
					} else {
						if (isRoot && result instanceof Element) {
							parent.replaceChild(node, result);
						} else {
							result.detach();
							parent.insertChild(result, position);
						}
						position++;
					}
				}
			}
			
			if (atomics != null) { // found at least one atomic value?
				setValue(node, atomics.toString());
			}
		}
	}
	
	/** sets the XPath value of the given node to the given value */
	private static void setValue(Node node, String value) {
		if (node instanceof Document) {
			// remove all children except root element (XOM docs must have a root element) 
			Document doc = (Document) node;
			Element root = doc.getRootElement();
			for (int k = doc.getChildCount(); --k >= 0; ) {
				if (doc.getChild(k) != root) doc.removeChild(k);
			}
			node = root; // replace root element's content
		}

		if (node instanceof Element) {
			Element elem = (Element) node;
			elem.removeChildren();
			elem.appendChild(value);
		} else if (node instanceof Attribute) {
			((Attribute) node).setValue(value);
		} else if (node instanceof Text) {
			((Text) node).setValue(value);
		} else if (node instanceof Comment) {
			((Comment) node).setValue(value);
		} else if (node instanceof ProcessingInstruction) {
			((ProcessingInstruction) node).setValue(value);
		} else {
			// ignore DocType (XPath spec has no concept of a DocType)
			// ignore Namespace (Saxon XQuery doesn't support them anymore)
		}		
	}

	/**
	 * EXPERIMENTAL; Convenience morphing method. Equivalent to
	 * 
	 * <pre>
	 * XQuery xmorpher = XQueryPool.GLOBAL_POOL.getXQuery(morpher, null);
	 * update(xquery(contextNode, select), xmorpher, null);
	 * </pre>
	 * 
	 * @param contextNode
	 *            the context node to execute the select query against
	 * @param select
	 *            an XQuery or XPath query selecting the nodes to morph.
	 * @param morpher
	 *            an XQuery or XPath query morphing each node in
	 *            <code>xquery(contextNode, select)</code> into a new form
	 *            (may be <code>null</code> in which case all nodes will be
	 *            detached, i.e. deleted)
	 * @throws RuntimeException
	 *             if an XQueryException occurs (unchecked exception for
	 *             convenience)
	 */
	public static void update(Node contextNode, String select, String morpher) {
		if (contextNode == null)
			throw new IllegalArgumentException("contextNode must not be null");
		if (select == null)
			throw new IllegalArgumentException("select must not be null");
		
		XQuery xmorpher = null;
		if (morpher != null && !morpher.trim().equals("()")) {
			try {
				xmorpher = XQueryPool.GLOBAL_POOL.getXQuery(morpher, null);
			} catch (XQueryException e) { // part of the "convenience"
				throw new RuntimeException(e);
			}
		}
		
		Nodes nodes = xquery(contextNode, select);
		update(nodes, xmorpher, null);
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
	
//	// TODO
//	public static Element xqueryFirstElement(Node contextNode, String query) {
//		Nodes results = xquery(contextNode, query);
//		switch (results.size()) {
//			case 0: return null;
//			case 1: return (Element) results.get(0);
//			default: throw new IllegalStateException(
//					"result sequence must not contain more than one element; " + 
//					"but contains " + results.size() + " nodes");
//		}
//	}	

}