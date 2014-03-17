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

import nu.xom.NamespaceConflictException;

/**
 * Stack of namespace declarations for a path in an XML tree. A stack entry and
 * its ancestors represent the namespace context of an element in an XML tree.
 * <p>
 * This class is more efficient than {@link org.xml.sax.helpers.NamespaceSupport}, 
 * in particular in the presence of many namespace declarations.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.10 $, $Date: 2006/02/07 18:00:10 $
 */
final class NamespacesInScope { // not a public class

	// null indicates the begin of a new namespace context
	// ["", null, prefix0, prefix1, null, prefix2, null]
	private final ArrayList prefixes = new ArrayList();
	
	// ["", null, uri0,    uri1,    null, uri2,    null]
	private final ArrayList uris = new ArrayList();
	
	// [("",""), null, (prefix0,uri0), (prefix1,uri1), null, (prefix2,uri2)]
//	private ArrayList stack = new ArrayList();
	
	/** Constructs a new stack. */
	NamespacesInScope() {
		reset();
	}
	
	public void reset() {
		prefixes.clear();
		uris.clear();
		
		// initially the default namespace is in scope
		prefixes.add("");
		uris.add("");
		
		// the "xml" prefix bound to "http://www.w3.org/XML/1998/namespace"
		// is always in scope, but never reported by XOM, so there's no need
		// to handle it in any way.
	}
	
	/** Indicates the beginning of a new namespace context (aka element). */
	public void push() {
		prefixes.add(null);
		uris.add(null);
	}
	
	/** Indicates the end of a namespace context (aka element). */
	public void pop() {
		// remove backwards until we find a new namespace context (null)
		for (int i = prefixes.size(); --i >= 0; ) {
			uris.remove(i);
			if (prefixes.remove(i) == null) return;
		}
		
		throw new IllegalStateException(
			"NamespacesInScope stack underflow: pop() called more often than push()");
	}
	
	/**
	 * Adds the given namespace declaration to the current namespace context
	 * if not already present in the current context or an ancestor context.
	 * Returns true if the declaration has been added as a result.
	 */
	public boolean addIfAbsent(String prefix, String uri) {
		if (prefix == null) prefix = "";
		if (uri == null) uri = "";
		
		// find prefix in current or ancestor contexts, walking towards the root
		boolean inCurrent = true;
		int i = prefixes.size();
		while (--i >= 0) {
			String existingPrefix = (String) prefixes.get(i);
			if (existingPrefix == null) {
				inCurrent = false;
			} else if (prefix.equals(existingPrefix)) {
				break; // found it
			}
		}
		
		if (inCurrent && i >= 0) { // rare case; slow path
			checkNamespaceConflict(i, uri);
		} else if (i < 0 || !(uri.equals(uris.get(i)))) { // not yet present
			if (i < 0) checkPrefix(prefix);
			checkNamespaceURI(uri);
//			if (DEBUG) System.err.println(
//				"adding namespace: prefix=" + prefix + ", uri=" + uri);
			prefixes.add(prefix);
			uris.add(uri);
			return true;
		}
		return false; // already present
	}
	
	/**
	 * A namespace conflict in the current context occurs if URIs for the
	 * same prefix are different. This can never happen with XOM but it may
	 * happen when used elsewhere.
	 */
	private void checkNamespaceConflict(int i, String uri) {
		String existingURI = (String) uris.get(i);
		if (!uri.equals(existingURI)) {
			String prefix = (String) prefixes.get(i);
			String message;
			if (prefix.length() == 0) {
				message = "Additional namespace " + uri
						+ " conflicts with existing default namespace "
						+ existingURI;
			} else {
				message = "Additional namespace " + uri
						+ " for the prefix " + prefix
						+ " conflicts with existing namespace binding "
						+ existingURI;
			}
			throw new NamespaceConflictException(message);
		}
	}
	
	private static void checkPrefix(String prefix) {
		// can never happen with XOM (dead code)
	}
	
	private static void checkNamespaceURI(String uri) {
		// can never happen with XOM (dead code)
	}
	
}
