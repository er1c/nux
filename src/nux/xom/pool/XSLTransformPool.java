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

import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.xslt.XSLException;
import nu.xom.xslt.XSLTransform;

/**
 * Efficient thread-safe pool/cache of XOM {@link XSLTransform} objects,
 * creating and holding at most <code>maxEntries</code> XSLTransform
 * objects (each representing a compiled stylesheet). On cache miss, a new XSLTransform 
 * is created via a factory, cached for future reuse, and then returned. 
 * On cache hit an XSLTransform is returned <em>instantly</em>.
 * Pool eviction is based on a LRU (least recently used) policy,
 * or if the JVM runs low on free memory.
 * <p>
 * This class helps to avoid the large overhead involved in constructing (i.e.
 * compiling) an XSLTransform instance, in particul for complex transforms over small
 * input XML documents. Most useful in high throughput server container
 * environments (e.g. large-scale Peer-to-Peer messaging network infrastructures
 * over high-bandwidth networks, scalable MOMs, etc).
 * <p>
 * Example usage (in any arbitrary thread and any arbitrary object):
 * <pre>
 *     XSLTransform trans = XSLTransformPool.GLOBAL_POOL.getTransform(new File("/tmp/test.xsl"));
 *     Document doc = BuilderPool.GLOBAL_POOL.getBuilder(false).build(new File("/tmp/test.xml"));
 *     Nodes nodes = trans.transform(doc);
 *     for (int i=0; i < nodes.size(); i++) {
 *         System.out.println("node "+i+": "+nodes.get(i).toXML());
 *     }
 * </pre>
 * <p>
 * Note: Internally uses extremely short-lived locks; the resulting potential lock
 * contention is completely negligible.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.24 $, $Date: 2005/12/05 06:53:05 $
 */
public class XSLTransformPool {
	
	/**
	 * A default pool (can be shared freely across threads without harm); global per class loader.
	 */
	public static final XSLTransformPool GLOBAL_POOL = new XSLTransformPool();
	
	/**
	 * The factory used to create new XSLTransforms from scratch.
	 */
	private final XSLTransformFactory factory;
	
	/** Current pool entries */
	private final Map entries;
	
	/**
	 * Creates a new pool with default parameters.
	 */
	public XSLTransformPool() {
		this(new PoolConfig(), new XSLTransformFactory());
	}
	
	/**
	 * Creates a new pool with the given configuration that uses the given
	 * factory on cache misses.
	 * 
	 * @param config
	 *            the configuration to use
	 * @param factory
	 *            the factory creating new XSLTransform instances on cache misses
	 */
	public XSLTransformPool(PoolConfig config, XSLTransformFactory factory) {
		if (config == null) 
			throw new IllegalArgumentException("config must not be null");
		if (factory == null) 
			throw new IllegalArgumentException("factory must not be null");
		this.factory = factory;
		this.entries = Pool.createPool(config);
	}
	
	/**
	 * Returns an <code>XSLTransform</code> for the given stylesheet.
	 * 
	 * @param stylesheet
	 *            the stylesheet to compile
	 * @return an XSL transform
	 * @throws XSLException
	 *             if the XSLTransform can't be created, e.g. because of an XSL
	 *             syntax error.
	 */
	public XSLTransform getTransform(Document stylesheet) throws XSLException {
		if (stylesheet == null) 
			throw new IllegalArgumentException("stylesheet must not be null");
		XSLTransform transform = (XSLTransform) entries.get(stylesheet);
		if (transform == null) {
			transform = factory.createTransform(stylesheet);
			entries.put(stylesheet, transform);
		}
		return transform;
	}

	/**
	 * Returns an <code>XSLTransform</code> for the given stylesheet.
	 * 
	 * @param stylesheet
	 *            the stylesheet to compile
	 * @return an XSL transform
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if the stylesheet is not well-formed XML
	 * @throws XSLException
	 *             if the XSLTransform can't be created, e.g. because of an XSL syntax error.
	 */
	public XSLTransform getTransform(File stylesheet) throws XSLException, ParsingException, IOException {
		if (stylesheet == null) 
			throw new IllegalArgumentException("stylesheet must not be null");
		Object key = stylesheet;
		XSLTransform transform = (XSLTransform) entries.get(key);
		if (transform == null) {
			transform = factory.createTransform(stylesheet);
			entries.put(key, transform);
		}
		return transform;
	}

	/**
	 * Returns an <code>XSLTransform</code> for the input stream obtained from
	 * resolving the given resourceName against the given resolver.
	 * 
	 * @param resolver
	 *            an object that can produce an input stream for a given
	 *            resource name.
	 * @param resourceName
	 *            the resource name (e.g. a path or URL)
	 * @param baseURI
	 *            the (absolute) base URI of the transform (may be <code>null</code>)
	 *            Need not be the actual URI of the resolver's stream.
	 * @return an XSL transform
	 * @throws MissingResourceException
	 *             if the resolver could not find the resource (unchecked
	 *             exception)
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if the stylesheet is not well-formed XML
	 * @throws XSLException
	 *             if the XSLTransform can't be created, e.g. because of an XSL syntax error.
	 */
	public XSLTransform getTransform(ResourceResolver resolver, String resourceName, URI baseURI) 
			throws XSLException, ParsingException, IOException, MissingResourceException {
		
		if (resolver == null) 
			throw new IllegalArgumentException("resolver must not be null");
		if (resourceName == null) 
			throw new IllegalArgumentException("resourceName must not be null");
		
		Object key = Pool.createHashKeys(new Object[] {resourceName, baseURI});
		XSLTransform transform = (XSLTransform) entries.get(key);
		if (transform == null) {
			InputStream stylesheet = resolver.getResourceAsStream(resourceName);
			if (stylesheet == null) {
				throw new MissingResourceException(
					"Resource '" + resourceName + "' could not be found by resolver: " + 
					resolver.getClass().getName(), 
					resolver.getClass().getName(), 
					resourceName);
			}
			transform = factory.createTransform(stylesheet, baseURI);
			entries.put(key, transform);
		}
		return transform;
	}
}