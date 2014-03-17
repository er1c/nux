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

import java.util.ArrayList;
import java.util.Arrays;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.IllegalAddException;
import nu.xom.Node;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.WellformednessException;
import nu.xom.XMLException;

/**
 * Streams input into one or more independent underlying node factories.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.1 $, $Date: 2006/01/30 02:05:55 $
 */
public final class MulticastNodeFactory extends NodeFactory {
	
	// TODO: how to add additional namespace declarations?

	private final NodeFactory[] receivers;
	private final RuntimeException[] exceptions;
//	private final Document[] docs;
	private final ParentNode[] currents;
	private final ArrayList[] stacks;
	private final boolean[] addAttributesAndNamespaces;
	private final boolean[] hasRootElement;
	private final boolean failFast;
	
	private final Nodes NONE = new Nodes();
	
	/**
	 * Constructs a new instance. In failFast mode fails immediately when at
	 * least one child throws an exception. Otherwise continues to stream into
	 * the remaining working factories and reports all collected failures on
	 * finishMakingDocument().
	 * 
	 * @param receivers
	 *            the child factories to push into
	 * @param failFast
	 *            whether or not to immediately report an exception on the first
	 *            child failure
	 */
	public MulticastNodeFactory(NodeFactory[] receivers, boolean failFast) {
		if (receivers == null) 
			throw new IllegalArgumentException("factories must not be null");
		for (int i=0; i < receivers.length; i++) {
			if (receivers[i] == null) 
				throw new IllegalArgumentException("factory must not be null");
		}
		this.receivers = new NodeFactory[receivers.length];
		System.arraycopy(receivers, 0, this.receivers, 0, receivers.length);				
		this.exceptions = new RuntimeException[receivers.length];
//		this.docs = new Document[children.length];
		this.currents = new ParentNode[receivers.length];
		this.stacks = new ArrayList[receivers.length];
		this.addAttributesAndNamespaces = new boolean[receivers.length];
		this.hasRootElement = new boolean[receivers.length];
		this.failFast = failFast;
		reset();
	}
	
	private void reset() {
		Arrays.fill(exceptions, null);
//		Arrays.fill(docs, null);
		Arrays.fill(currents, null);
		for (int i=0; i < stacks.length; i++) {
			if (stacks[i] == null) stacks[i] = new ArrayList();
			stacks[i].clear();
		}
		Arrays.fill(addAttributesAndNamespaces, true);
		Arrays.fill(hasRootElement, false);
	}
	
	/** Returns the document build for each of the factories. */
	public Document[] getDocuments() {
		Document[] documents = new Document[currents.length];
		for (int i=0; i < currents.length; i++) {
			documents[i] = (Document) currents[i];
		}
		return documents;
	}
	
	private void onException(int i, RuntimeException e) {
		exceptions[i] = e;
		if (failFast) throw e;		
	}
	
	/** {@inheritDoc} */
	public Document startMakingDocument() {
		reset();
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				currents[i] = receivers[i].startMakingDocument();
				if (currents[i] == null) throw new NullPointerException(
						"startMakingDocument must not return null.");
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		return super.startMakingDocument();
	}
	
	/** {@inheritDoc} */
	public void finishMakingDocument(Document doc) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				receivers[i].finishMakingDocument((Document)currents[i]);
				if (!hasRootElement[i]) throw new WellformednessException(
					"Factory attempted to remove the root element");
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		super.finishMakingDocument(doc);
		
		// report any exceptions collected when not in failFast mode:
		for (int i=0; !failFast && i < receivers.length; i++) {
			if (exceptions[i] != null) {
				throw new MultipleCausesException(exceptions);			
			}
		}
	}
		
	/** {@inheritDoc} */
	public Element makeRootElement(String qname, String namespaceURI) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Element elem = receivers[i].makeRootElement(qname, namespaceURI);
				if (elem == null) throw new NullPointerException(
						"Factory failed to create root element.");
				stacks[i].add(elem); // push
				((Document)currents[i]).setRootElement(elem);
				currents[i] = elem; // recurse down
				addAttributesAndNamespaces[i] = true;
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		
		return new Element(qname, namespaceURI);
	}

	/** {@inheritDoc} */
	public Element startMakingElement(String qname, String namespaceURI) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Element elem = receivers[i].startMakingElement(qname, namespaceURI);
				stacks[i].add(elem); // push even if it's null
				if (elem != null) {
					currents[i].insertChild(elem, currents[i].getChildCount());
					currents[i] = elem; // recurse down
				}
				addAttributesAndNamespaces[i] = elem != null;
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		
		return new Element(qname, namespaceURI);
	}
	
	/** {@inheritDoc} */
	public Nodes finishMakingElement(Element unused) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Element elem = (Element) stacks[i].remove(stacks[i].size()-1); // pop
				if (elem == null) {
					continue; // skip element
				}
				ParentNode parent = elem.getParent();
				if (parent == null) throwTamperedWithParent();
				currents[i] = parent; // recurse up
				
				Nodes nodes = receivers[i].finishMakingElement(elem);
				if (nodes.size()==1 && nodes.get(0)==elem) { // same node? (common case)
					if (parent instanceof Document) hasRootElement[i] = true;
					continue; // optimization: no need to remove and then readd same element
				}

				if (parent.getChildCount()-1 < 0) throwTamperedWithParent();				
				if (parent instanceof Element) { // can't remove root element
					parent.removeChild(parent.getChildCount()-1);
				}
				appendNodes(parent, nodes, i);
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		
		if (unused.getParent() instanceof Document) {
			return new Nodes(unused); // XOM documents must have a root element
		}
		return NONE;
	}
	
	/** {@inheritDoc} */
	public Nodes makeAttribute(String qname, String namespaceURI, String value, Attribute.Type type) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			if (!addAttributesAndNamespaces[i]) continue;
			try {
				Nodes nodes = receivers[i].makeAttribute(
						qname, namespaceURI, value, type);
				appendNodes(currents[i], nodes, i);
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		return NONE;
	}

	
	// makeComment(), makeDocType(), makeProcessingInstruction(), makeText()
	// are essentially all the same :-(
	
	/** {@inheritDoc} */
	public Nodes makeComment(String data) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Nodes nodes = receivers[i].makeText(data);
				appendNodes(currents[i], nodes, i);
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		return NONE;
	}

	/** {@inheritDoc} */
	public Nodes makeDocType(String rootElementName, String publicID, String systemID) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Nodes nodes = receivers[i].makeDocType(
					rootElementName, publicID, systemID);
				appendNodes(currents[i], nodes, i);
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		return NONE;
	}

	/** {@inheritDoc} */
	public Nodes makeProcessingInstruction(String target, String data) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Nodes nodes = receivers[i].makeProcessingInstruction(target, data);
				appendNodes(currents[i], nodes, i);
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		return NONE;
	}
	
	/** {@inheritDoc} */
	public Nodes makeText(String text) {
		for (int i=0; i < receivers.length; i++) {
			if (exceptions[i] != null) continue; // ignore failed factory
			try {
				Nodes nodes = receivers[i].makeText(text);
				appendNodes(currents[i], nodes, i);
			} catch (RuntimeException e) {
				onException(i, e);
			}
		}
		return NONE;
	}

	private void appendNodes(ParentNode parent, Nodes nodes, int k) {
		if (parent instanceof Element) {
			appendNodesToElement((Element) parent, nodes);
		} else {
			appendNodesToDocument((Document) parent, nodes, k);
		}
	}
	
	private static void appendNodesToElement(Element elem, Nodes nodes) {
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
	
	private void appendNodesToDocument(Document doc, Nodes nodes, int k) {
		if (nodes != null) {
			int size = nodes.size();
			for (int i=0; i < size; i++) {
				Node node = nodes.get(i);
				if (node instanceof Element) {
					if (hasRootElement[k]) {
						throw new IllegalAddException(
							"Factory returned multiple root elements");
					}
					doc.setRootElement((Element) node);
					hasRootElement[k] = true;
				} else {
					int j = doc.getChildCount();
					if (!hasRootElement[k]) j--;
					doc.insertChild(node, j);
				}
			}
		}
	}
	
	private static void throwTamperedWithParent() {
		throw new XMLException("Factory has tampered with a parent pointer " + 
				"of ancestor-or-self in finishMakingElement()");
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	
	/**
	 * A RuntimeException containing an array of one or more causes
	 * (Throwables). Some or all cause elements may be <code>null</code>.
	 */
	public static final class MultipleCausesException extends RuntimeException {
		
		private final Throwable[] causes;
		
		private MultipleCausesException(Throwable[] causes) {
			this.causes = new Throwable[causes.length];
			System.arraycopy(causes, 0, this.causes, 0, causes.length);
		}
		
		public Throwable[] getCauses() {
			return causes;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			for (int i=0; i < causes.length; i++) {
				buf.append("Cause " + i + ": ");
				buf.append(causes[i]);
				if (i < causes.length-1) buf.append("\n");
			}
			return buf.toString();
		}
	}

}
