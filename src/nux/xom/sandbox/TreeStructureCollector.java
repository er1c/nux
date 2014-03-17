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

import java.io.File;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nux.xom.xquery.ResultSequenceSerializer;
import nux.xom.xquery.XQueryUtil;

/**
 * Streaming demo that collects and prints a hierarchical statistics summary of
 * element and attribute instances. In practice, the summary can be seen as a
 * first approximation of a formal schema, though it is inferred from a
 * schemaless document.
 * <p>
 * Time complexity is O(nrNodes), i.e. a single pass algorithm. Space complexity
 * is O(maxTreeDepth) for real-world documents, i.e. memory consumption is
 * negligible; the algorithm can be run over arbitrarily large input documents.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.4 $, $Date: 2005/11/30 00:40:26 $
 */
public final class TreeStructureCollector extends NodeFactory {

	private Element current;
	
	private final Nodes NONE = new Nodes();
	private final Element DUMMY = new Element("dummy");
	
	public static void main(String[] args) throws Exception {
		System.out.println("\nTree structure summary:");
		System.out.println("***********************\n");
		NodeFactory factory = new TreeStructureCollector();
		Document summary = new Builder(factory).build(new File(args[0]));
		Serializer ser = new Serializer(System.out);
		ser.setIndent(4);
		ser.write(summary);
//		System.out.println(XOMUtil.toPrettyXML(summary));

		System.out.println("\nPaths sorted by instance counts:");
		System.out.println("********************************\n");
		String query = 
			"for $e in //* " +
			"where xs:integer($e/@instances) > 1 " + 
//				"and (sum($e/*/@instances) != $e/@instances) " +
//				"and (every $x in $e/*/@instances satisfies $x != $e/@instances) " +
				"and (every $x in $e/../@instances satisfies $x != $e/@instances) " +
			"order by xs:integer($e/@instances) descending " +
			"return element {node-name($e)} {" + 
				"attribute percent {round-half-to-even(100.0 * $e/@instances div /*/@descendants-or-self, 1)}," +
				"attribute instances {$e/@instances}, " + 
				"attribute xpath {$e/saxon:path()}" + 
			"}";
		Nodes nodes = XQueryUtil.xquery(summary, query);
		ResultSequenceSerializer rser = new ResultSequenceSerializer();
		rser.setIndent(4);
		rser.write(nodes, System.out);		
	}
	
	public TreeStructureCollector() {}
	
	// TODO: obsolete?
	public Document collect(Document doc) {
		startMakingDocument();
		collect(doc.getRootElement());
//		finishMakingDocument()
		return new Document(current);
	}
	
	// TODO: obsolete?
	private void collect(Element elem) {
		startMakingElement(elem.getQualifiedName(), elem.getNamespaceURI());
		
		for (int i=0; i < elem.getAttributeCount(); i++) {
			Attribute a = elem.getAttribute(i);
			makeAttribute(a.getQualifiedName(), a.getNamespaceURI(), 
					a.getValue(), a.getType());
		}
		for (int i=0; i < elem.getChildCount(); i++) {
			Node node = elem.getChild(i);
			if (node instanceof Element) {
				collect((Element) node); // recurse
			}
		}
		
		finishMakingElement(elem);
	}
	
	/** {@inheritDoc} */
	public Document startMakingDocument() {
		current = new Element("root");
		return super.startMakingDocument();
	}
	
	/** {@inheritDoc} */
	public Nodes finishMakingElement(Element elem) {
		current = (Element) current.getParent(); // recurse up
		if (elem.getParent() instanceof Document) { // done with root element?
			current = (Element) current.removeChild(0);
			int total = Integer.parseInt(
				current.getAttributeValue("descendants-or-self"));
			addAccumulatedPercentages(current, total);			
			return new Nodes(current);
		}
		return NONE;
	}
	
	private static void addAccumulatedPercentages(Element elem, int total) {
		String value = elem.getAttributeValue("descendants-or-self");
		if (value == null) value = elem.getAttributeValue("instances");
		incrementValue(elem, "descendants-or-self-percent", 
				(int) Math.round(100.0 * Integer.parseInt(value) / total));
		
		for (int i=0; i < elem.getChildCount(); i++) {
			Node node = elem.getChild(i);
			if (node instanceof Element) {
				addAccumulatedPercentages((Element)node, total); // recurse
			}
		}
	}
	
	/** {@inheritDoc} */
	public Element startMakingElement(String qname, String namespaceURI) {
		String localName = getLocalName(qname);
		Element first = current.getFirstChildElement(localName, namespaceURI);
		if (first == null) {
			first = new Element(qname, namespaceURI);
			incrementValue(first, "descendants-or-self", 0);
			incrementValue(first, "instances", 0);
			incrementValue(first, "children", 0);
			incrementValue(first, "attributes", 0);
			current.appendChild(first);
		}
		
		incrementValue(first, "descendants-or-self", 1);
		incrementValue(first, "instances", 1);
		Element parent = (Element) first.getParent();
		incrementValue(parent, "children", 1);
		incrementDescendantOrSelf(parent);
			
		current = first; // recurse down
		
		return new Element(DUMMY);
	}
	
	/** {@inheritDoc} */
	public Nodes makeAttribute(String qname, String namespaceURI, String value, Attribute.Type type) {
		String localName = makeAttributeName(getLocalName(qname));
		Element first = current.getFirstChildElement(localName, namespaceURI);
		if (first == null) {
			first = new Element(makeAttributeName(qname), namespaceURI);
			current.appendChild(first);
		}
		
		incrementValue(first, "instances", 1);
		Element parent = (Element) first.getParent();
		incrementValue(parent, "attributes", 1);
		incrementDescendantOrSelf(parent);

		return NONE;
	}
	
	private static void incrementDescendantOrSelf(Element parent) {
		do {
			incrementValue(parent, "descendants-or-self", 1);
			parent = (Element) parent.getParent();
		} while (parent != null);		
	}
	
	private static void incrementValue(Element elem, String name, int value) {
		Attribute attr = elem.getAttribute(name);
		if (attr == null) {
			attr = new Attribute(name, String.valueOf(value));
			elem.addAttribute(attr);
		} else {
			int v = Integer.parseInt(attr.getValue());
			attr.setValue(String.valueOf(v + value));
		}
	}
	
	private static String getLocalName(String qname) {
		int i = qname.indexOf(':');
		return i < 0 ? qname : qname.substring(i+1);
	}
	
	private static String makeAttributeName(String name) {
		return name + "_";
	}
	
	/** {@inheritDoc} */
	public Nodes makeComment(String data) {
		return NONE;
	}

	/** {@inheritDoc} */
	public Nodes makeDocType(String rootElementName, String publicID, String systemID) {
		return NONE;
	}

	/** {@inheritDoc} */
	public Nodes makeProcessingInstruction(String target, String data) {
		return NONE;
	}
	
	/** {@inheritDoc} */
	public Nodes makeText(String text) {
		return NONE;
	}

}
