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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.MissingResourceException;

import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;

/**
 * Efficient thread-safe pool/cache of {@link XQuery} objects, creating and
 * holding at most <code>maxEntries</code> XQuery objects (each
 * representing a compiled query). On cache miss, a new XQuery 
 * is created via a factory, cached for future reuse, and then returned.
 * On cache hit an XQuery is returned <em>instantly</em>. 
 * Pool eviction is based on a LRU (least recently used) policy, 
 * or if the JVM runs low on free memory.
 * <p>
 * This class helps to avoid the large overhead involved in constructing (i.e.
 * compiling) an XQuery instance, in particul for complex queries over small
 * input XML documents. Most useful in high throughput server container
 * environments (e.g. large-scale Peer-to-Peer messaging network infrastructures
 * over high-bandwidth networks, scalable MOMs, etc).
 * <p>
 * Note that this class caches queries, not their results or result fragments.
 * In particular, no materialized view maintenance is done. 
 * If desired, result caching can be implemented in an application-specific 
 * manner on top of this class, in a large variety of ways, for example via
 * {@link nux.xom.pool.DocumentMap}.
 * <p>
 * Example usage (in any arbitrary thread and any arbitrary object):
 * <pre>
 *     XQuery xquery = XQueryPool.GLOBAL_POOL.getXQuery(new File("samples/xmark/q03.xq"));
 *     //XQuery xquery = XQueryPool.GLOBAL_POOL.getXQuery("for $i in /* return $i/headline_text", null);
 *     Document doc = BuilderPool.GLOBAL_POOL.getBuilder(false).build(new File(samples/xmark/auction-0.01.xml));
 *     Nodes results = xquery.execute(doc).toNodes();
 *     for (int i=0; i < results.size(); i++) {
 *         System.out.println("node "+i+": "+results.get(i).toXML());
 *     }
 * </pre>
 * <p>
 * Note: Internally uses extremely short-lived locks; the resulting potential lock
 * contention is completely negligible.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.40 $, $Date: 2005/12/05 06:53:05 $
 */
public class XQueryPool {
	
	/**
	 * A default pool (can be shared freely across threads without harm); 
	 * global per class loader.
	 */
	public static final XQueryPool GLOBAL_POOL = new XQueryPool();
	
	/**
	 * The factory used to create new XQuerys from scratch.
	 */
	private final XQueryFactory factory;
	
	/** Current pool entries */
	private final Map entries;
	
	/**
	 * Creates a new pool with default parameters.
	 */
	public XQueryPool() {
		this(new PoolConfig(), new XQueryFactory());
	}
	
	/**
	 * Creates a new pool with the given configuration that uses the given
	 * factory on cache misses.
	 * 
	 * @param config
	 *            the configuration to use
	 * @param factory
	 *            the factory creating new XQuery instances on cache misses
	 */
	public XQueryPool(PoolConfig config, XQueryFactory factory) {
		if (config == null) 
			throw new IllegalArgumentException("config must not be null");
		if (factory == null) 
			throw new IllegalArgumentException("factory must not be null");
		this.factory = factory;
		this.entries = Pool.createPool(config);
	}
	
	/**
	 * Returns an <code>XQuery</code> for the given input query.
	 * 
	 * @param query the query to compile
	 * @return an XQuery
	 * 
	 * @throws IOException
	 *             if an I/O error occured while reading the query. 
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery getXQuery(File query) throws XQueryException, IOException {
		if (query == null) 
			throw new IllegalArgumentException("query must not be null");

		Object key = query;
		XQuery xquery = (XQuery) entries.get(key);
		if (xquery == null) {
			xquery = factory.createXQuery(query);
			entries.put(key, xquery);
		}
		return xquery;
	}

	/**
	 * Returns an <code>XQuery</code> for the given input query, using the
	 * given base URI.
	 * 
	 * @param query the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 *            working directory.
	 * @return an XQuery
	 * 
	 * @throws IOException
	 *             if an I/O error occured while reading the query. 
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery getXQuery(File query, URI baseURI) throws XQueryException, IOException {
		if (query == null) 
			throw new IllegalArgumentException("query must not be null");

		Object key = Pool.createHashKeys(new Object[] {query, baseURI, null, null});
		XQuery xquery = (XQuery) entries.get(key);
		if (xquery == null) {
			xquery = factory.createXQuery(query, baseURI);
			entries.put(key, xquery);
		}
		return xquery;
	}

	/**
	 * Returns an <code>XQuery</code> for the given input query, using the
	 * given base URI.
	 * 
	 * @param query
	 *            the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 *            working directory.
	 * @return an XQuery
	 * 
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery getXQuery(String query, URI baseURI) throws XQueryException {
		if (query == null) 
			throw new IllegalArgumentException("query must not be null");
		
		Object key;
		if (baseURI == null)
			key = query; // fast path
		else 
			key = Pool.createHashKeys(new Object[] {query, baseURI});
		
		XQuery xquery = (XQuery) entries.get(key);
		if (xquery == null) {
			xquery = factory.createXQuery(query, baseURI);
			entries.put(key, xquery);
		}
		return xquery;
	}

	/**
	 * Returns an <code>XQuery</code> for the input stream obtained from
	 * resolving the given resourceName against the given resolver.
	 * 
	 * @param resolver
	 *            an object that can produce an input stream for a given
	 *            resource name.
	 * @param resourceName
	 *            the resource name (e.g. a path or URL)
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 *            working directory.
	 *            Need not be the actual URI of the resolver's stream.
	 * @return an XQuery
	 * 
	 * @throws MissingResourceException
	 *             if the resolver could not find the resource (unchecked exception)
	 * @throws IOException
	 *             if an I/O error occured while reading the query from the stream
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */
	public XQuery getXQuery(ResourceResolver resolver, String resourceName, URI baseURI) 
			throws XQueryException, IOException, MissingResourceException {

		if (resolver == null) 
			throw new IllegalArgumentException("resolver must not be null");
		if (resourceName == null) 
			throw new IllegalArgumentException("resourceName must not be null");
		
		Object key = Pool.createHashKeys(new Object[] {resourceName, baseURI, null});
		XQuery xquery = (XQuery) entries.get(key);
		if (xquery == null) {
			InputStream query = resolver.getResourceAsStream(resourceName);
			if (query == null) {
				throw new MissingResourceException(
					"Resource '" + resourceName + "' could not be found by resolver: " + 
					resolver.getClass().getName(), 
					resolver.getClass().getName(), 
					resourceName);
			}
			xquery = factory.createXQuery(query, baseURI);
			entries.put(key, xquery);
		}
		return xquery;
	}
	
}