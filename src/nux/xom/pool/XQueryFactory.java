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
package nux.xom.pool;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;

/**
 * Creates and returns new <code>XQuery</code> objects using flexible parametrization (thread-safe).
 * <p>
 * This implementation is thread-safe.
 *
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.15 $, $Date: 2005/12/10 01:05:49 $
 */
public class XQueryFactory {
	
	private final Charset charset;
	private final DocumentURIResolver resolver;
	
	/**
	 * Equivalent to <code>new XQueryFactory(null, null)</code>.
	 */
	public XQueryFactory() {
		this(null, null);
	}
	
	/**
	 * Creates a factory instance that uses the given DocumentURIResolver and
	 * character encoding.
	 * 
	 * @param charset
	 *            the charset to convert byte streams to a query string, e.g.
	 *            <code>Charset.forName("UTF-8")</code>. May be
	 *            <code>null</code> in which case the system's default
	 *            platform encoding is used.
	 * @param resolver
	 *            an object that is called by the XQuery processor to turn a URI
	 *            passed to the XQuery <code>doc()</code> function into a XOM
	 *            {@link nu.xom.Document}. May be <code>null</code> in which
	 *            case non-validating default resolution is used.
	 */
	public XQueryFactory(Charset charset, DocumentURIResolver resolver) {
		this.charset = charset;
		this.resolver = resolver;
	}
	
	/**
	 * Creates and returns a new <code>XQuery</code> for the given input
	 * query. Equivalent to <code>createXQuery(query, query.toURI())</code>.
	 * 
	 * @param query
	 *            the query to compile
	 * @return a new XQuery
	 * 
	 * @throws IOException
	 *             if an I/O error occured while reading the query from the file
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery createXQuery(File query) throws XQueryException, IOException {
		return createXQuery(query, query.toURI());
	}

	/**
	 * Creates and returns a new <code>XQuery</code> for the given input
	 * query, using the given base URI.
	 * 
	 * @param query
	 *            the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 *            working directory.
	 * @return a new XQuery
	 * 
	 * @throws IOException
	 *             if an I/O error occured while reading the query from the file
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery createXQuery(File query, URI baseURI) throws XQueryException, IOException {
		return createXQuery(new FileInputStream(query), baseURI);
	}

	/**
	 * Creates and returns a new <code>XQuery</code> for the given input
	 * query, using the given base URI.
	 * <p>
	 * If desired, overrride this default implementation to construct and 
	 * return a custom object or a custom XQuery subclass.
	 * 
	 * @param query
	 *            the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 * @return a new XQuery
	 *            working directory.
	 * 
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery createXQuery(String query, URI baseURI) throws XQueryException {
		return new XQuery(query, baseURI, null, resolver);
	}

	/**
	 * Creates and returns a new <code>XQuery</code> for the given input
	 * query, using the given base URI.
	 * 
	 * @param query
	 *            the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 *            working directory.
	 *            Need not be the stream's actual URI. 
	 * @return a new XQuery
	 * 
	 * @throws IOException
	 *             if an I/O error occurs while reading the query from the
	 *             stream
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery createXQuery(InputStream query, URI baseURI) throws XQueryException, IOException {
		String str = FileUtil.toString(query, charset);
		return createXQuery(str, baseURI);
	}

}