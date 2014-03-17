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

import nu.xom.Node;
import nu.xom.Nodes;

/**
 * A forward-only iterator representing an XQuery result sequence of zero or
 * more ordered items.
 * <p>
 * The XQuery/XPath data model is based on the notion of a result sequence. A
 * result sequence is an ordered collection of zero or more items. An item may
 * be a node (document, element, attribute, namespace, text,
 * processing-instruction or comment) or an atomic value. This means that a
 * result sequence is not, in general, an XML document.
 * <p>
 * This interface does not mandate how an implementation should convert
 * top-level atomic values to {@link Node} objects; an implementation is
 * encouraged to document how it converts such atomic values. For example, see
 * the default implementation in
 * {@link XQuery#newResultSequence(net.sf.saxon.query.XQueryExpression, 
 * net.sf.saxon.query.DynamicQueryContext)}.
 * <p>
 * This interface allows to stream (pipeline) execution results, or to
 * conveniently collect them in a batched manner.
 * <p>
 * In <i>streamed </i> manner, result nodes are lazily produced via the
 * pipelined <code>next()</code> method, one result at a time. This is useful
 * if output is very large, since only one node at a time needs to be
 * materialized in memory. Here, each node can immediately be garbage collected
 * after the application has processed it (e.g. has written it to disk or the
 * network). Further, an application should choose streamed mode if it is known
 * that it will use only the first or the first few result nodes anyway,
 * ignoring other potentially remaining results.
 * <p>
 * In <i>batched </i> manner all execution output results are eagerly collected
 * together into a node list containing zero or more {@link Node} objects, via
 * the <code>toNodes()</code> method. This is more convenient, but also more
 * memory-intensive. In fact, <code>toNodes()</code> is typically just a
 * convenience loop around <code>next()</code>.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.17 $, $Date: 2005/12/10 01:06:05 $
 */
public interface ResultSequence {
	
	/**
	 * Returns the next node from the result sequence, or <code>null</code>
	 * if there are no more nodes available due too iterator exhaustion.
	 * 
	 * @return the next node, or <code>null</code> if no more nodes are available
	 * @throws XQueryException
	 *             if an error occurs during execution
	 */
	public Node next() throws XQueryException;
	
	/**
	 * Returns all remaining nodes from the result sequence, collected into
	 * a list of zero or more <code>Node</code> objects.
	 * 
	 * @return a node list
	 * @throws XQueryException
	 *             if an error occurs during execution
	 */ 
	 public Nodes toNodes() throws XQueryException;
	
}
