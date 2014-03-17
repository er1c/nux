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
import java.util.MissingResourceException;

import nu.xom.Document;
import nu.xom.ParsingException;

/**
 * Efficient compact thread-safe pool/cache of XOM XML {@link Document} objects,
 * storing documents in a {@link DocumentMap}. On cache miss, a new document is
 * created via a factory, cached for future reuse, and then returned. On cache
 * hit a document is returned <em>almost instantly</em>.
 * <p>
 * Pool eviction is delegated to the DocumentMap/PoolConfig store.
 * <p>
 * By default returns a <i>copy</i> of a cached document on each invocation of 
 * methods <code>getDocument(...)</code>.
 * This ensures that documents contained inside the cache cannot be modified by clients,
 * which would be unsafe in the presense of multiple threads and application modules 
 * using a shared DocumentPool instance (a shared instance reduces overall memory
 * consumption).
 * <p>
 * This class helps to avoid the large overhead involved in parsing XML
 * documents. Most useful in high throughput server container environments (e.g.
 * large-scale Peer-to-Peer messaging network infrastructures over
 * high-bandwidth networks, scalable MOMs, etc).
 * <p>
 * Example usage (in any arbitrary thread and any arbitrary object):
 * 
 * <pre>
 *     // parse file with non-validating parser
 *     Document doc = DocumentPool.GLOBAL_POOL.getDocument(new File("samples/data/periodic.xml"));
 * 
 *     // pool with custom document factory for W3C XML Schema validation
 *     DocumentFactory factory = new DocumentFactory() { 
 *         protected Builder newBuilder() { 
 *             return BuilderPool.GLOBAL_POOL.getW3CBuilder(someW3CXMLSchema);
 *             // return new Builder(); 
 *         }
 *     };
 *     DocumentPool pool = new DocumentPool(new DocumentMap(new PoolConfig()), factory);
 *     Document doc = pool.getDocument(new File("samples/data/periodic.xml"));
 * 
 *     // pool reading binary xml documents
 *     DocumentPool pool = new DocumentPool(new DocumentMap(new PoolConfig()), 
 *         new DocumentFactory().getBinaryXMLFactory());
 *     Document doc = pool.getDocument(new File("samples/data/periodic.xml.bnux"));
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.19 $, $Date: 2005/12/05 06:53:05 $
 */
public class DocumentPool {
	/**
	 * A default pool (can be shared freely across threads without harm); 
	 * global per class loader.
	 */
	public static final DocumentPool GLOBAL_POOL = new DocumentPool();
	
	/**
	 * The factory used to create new documents from scratch.
	 */
	private final DocumentFactory factory;
	
	/**
	 * Current pool entries.
	 */
	private final DocumentMap entries;
	
	/**
	 * Creates a new pool with default parameters.
	 */
	public DocumentPool() {
		this(new DocumentMap(new PoolConfig()), new DocumentFactory());
	}
	
	/**
	 * Creates a new pool that uses the given document cache data structure
	 * and factory.
	 * 
	 * @param entries
	 *            an empty document cache data structure
	 * @param factory
	 *            the factory creating new Document instances on cache misses
	 */
	public DocumentPool(DocumentMap entries, DocumentFactory factory) {
		if (factory == null) 
			throw new IllegalArgumentException("factory must not be null");
		this.factory = factory;
		if (entries == null) 
			throw new IllegalArgumentException("entries must not be null");
		this.entries = entries;
	}
	
	/**
	 * Returns a document for the given input file.
	 * 
	 * @param input
	 *            the file to read from
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if an XML parsing error occurs
	 * @return a document
	 */
	public Document getDocument(File input) throws ParsingException, IOException {
		if (input == null) 
			throw new IllegalArgumentException("input must not be null");

		Object key = input;
		Document doc = entries.getDocument(key);
		if (doc == null) {
			doc = factory.createDocument(input);
			entries.putDocument(key, doc);
		}
		return doc;
	}

	/**
	 * Returns a document for the given URL.
	 * 
	 * @param systemID
	 *            the URL of the document
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if an XML parsing error occurs
	 * @return a document
	 */
	public Document getDocument(URI systemID) throws ParsingException, IOException {
		if (systemID == null) 
			throw new IllegalArgumentException("systemID must not be null");

		Object key = systemID;
		Document doc = entries.getDocument(key);
		if (doc == null) {
			doc = factory.createDocument(null, systemID);
			entries.putDocument(key, doc);
		}
		return doc;
	}
	
	/**
	 * Returns a document for the input stream obtained from
	 * resolving the given resourceName against the given resolver.
	 * 
	 * @param resolver
	 *            an object that can produce an input stream for a given
	 *            resource name.
	 * @param resourceName
	 *            the resource name (e.g. a path or URL)
	 * @param baseURI
	 *            the base URI of the document (may be <code>null</code>)
	 *            Need not be the actual URI of the resolver's stream.
	 * @throws MissingResourceException
	 *             if the resolver could not find the resource (unchecked
	 *             exception)
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if an XML parsing error occurs
	 * @return a document
	 */
	public Document getDocument(ResourceResolver resolver, String resourceName, URI baseURI) 
		throws ParsingException, IOException, MissingResourceException {

		if (resolver == null) 
			throw new IllegalArgumentException("resolver must not be null");
		if (resourceName == null) 
			throw new IllegalArgumentException("resourceName must not be null");
		
		Object key = Pool.createHashKeys(new Object[] {resourceName, baseURI});
		Document doc = entries.getDocument(key);
		if (doc == null) {
			InputStream input = resolver.getResourceAsStream(resourceName);
			if (input == null) {
				throw new MissingResourceException(
					"Resource '" + resourceName + "' could not be found by resolver: " + 
					resolver.getClass().getName(), 
					resolver.getClass().getName(), 
					resourceName);
			}
			doc = factory.createDocument(input, baseURI);
			entries.putDocument(key, doc);
		}
		return doc;
	}

}