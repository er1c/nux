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

import java.util.Map;

import nu.xom.Document;
import nux.xom.binary.BinaryParsingException;

/**
 * Efficient compact thread-safe main memory cache map of XOM XML
 * {@link Document} objects; for example used by {@link DocumentPool};
 * Pool eviction is based on a LRU (least recently used) policy as defined by 
 * a tunable {@link PoolConfig} configuration. 
 * <p>
 * Eviction occurs if at least one of the following conditions holds:
 * <ul>
 * <li>The JVM runs low on free memory (soft references). Hence, the client
 * application will never ever experience OutOfMemoryErrors.</li>
 * <li>The maximum number of documents is exceeded (<code>maxEntries</code>).
 * </li>
 * <li>The maximum memory limit is exceeded by the sum of all contained
 * documents (<code>capacity</code>).</li>
 * <li>The time an entry has been inactive is exceeded (
 * <code>maxIdleTime</code>). That is, a key's entry is removed if the key
 * has not been recalled for some time via <code>getDocument</code>.</li>
 * <li>The time since the entry's creation via <code>putDocument</code> is
 * exceeded (<code>maxLifeTime</code>). 
 * [There must hold: <code>maxIdleTime &lt;= maxLifeTime</code>].</li>
 * <li>A key's file has been modified or deleted since the entry has been 
 * inserted into the pool (<code>fileMonitoring</code>).</li>
 * </ul>
 * <p>
 * The pool can internally hold a compact representation of a document using the
 * bnux {@link nux.xom.binary.BinaryXMLCodec} algorithm. A zlib compression level 
 * ranging from
 * -1 (no bnux, stores a reference to the document object "as is", best
 * performance) to 0 (bnux with no ZLIB compression; good performance) to 1
 * (bnux with little ZLIB compression; reduced performance) to 9 (bnux with
 * strongest ZLIB compression; worst performance) allows one to configure the
 * CPU/memory consumption trade-off.
 * <p>
 * Unless there is a good reason to the contrary, you should always use level 0:
 * the bnux algorithm typically already precompresses considerably, typically by
 * a factor 20 over a XOM main memory tree. Level 9 can yield another factor 5
 * or so over that.
 * <p>
 * Method <code>getDocument()</code> returns a reference to the document
 * previously stored if</code> level == -1</code>, otherwise it stores a compact
 * immutable document <i>copy </i> and returns a separate copy on each
 * invocation (for safety).
 * <p>
 * Keys must properly implement methods {@link Object#equals(Object)} and
 * {@link Object#hashCode()} as required for a normal HashMap.
 * <p>
 * Example usage (in any arbitrary thread and any arbitrary object):
 * 
 * <pre>
 *     PoolConfig config = new PoolConfig();
 *     config.setCompressionLevel(0);          // no zlib compression
 *     config.setMaxEntries(10000);            // max 10000 documents
 *     config.setCapacity(100 * 1024 * 1024);  // max 100 MB capacity
 *     config.setMaxIdleTime(60 * 1000);       // keep inactive entries for at most 60 secs
 *     config.setMaxLifeTime(60 * 60 * 1000);  // keep any entry for at most 1 hour
 *     config.setFileMonitoring(true);         // auto-remove cached files on file modification
 *     DocumentMap map = new DocumentMap(config);
 * 
 *     File key = new File("samples/data/periodic.xml"); // file to parse
 *     Document doc = map.getDocument(key);
 *     if (doc == null) { // not yet cached
 *         // build document from source
 *         doc = BuilderPool.GLOBAL_POOL.getBuilder(false).build(key);
 * 
 *         // cache it for future reuse
 *         map.putDocument(key, doc); 
 *     }
 * 
 *     // do something useful with the document
 *     System.out.println(doc.toXML());
 * </pre>
 * 
 * Note: Internally uses extremely short-lived locks; the resulting potential
 * lock contention is completely negligible.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.35 $, $Date: 2005/12/10 01:05:49 $
 */
public class DocumentMap {
	
	private final Map entries;
	private final int compressionLevel;	
	private final int maxEntries;
		
	/**
	 * Creates a new pool that uses the given configuration.
	 * 
	 * @param config
	 *            the configuration to use
	 */
	public DocumentMap(PoolConfig config) {
		if (config == null) 
			throw new IllegalArgumentException("config must not be null");
		
		this.compressionLevel = config.getCompressionLevel();
		
		int maxEntries = config.getMaxEntries();
		if (config.getCapacity() <= 0 || config.getMaxIdleTime() <= 0 || config.getMaxLifeTime() <= 0) {
			maxEntries = 0; // performance (no need to call binary codec)
		}
		this.maxEntries = maxEntries;
		
		this.entries = Pool.createPool(config);
	}
	
	/**
	 * Returns the document associated with the given key. Returns
	 * <code>null</code> if the pool contains no mapping for this key.
	 * 
	 * @param key
	 *            the key of the document to retrieve
	 * @return the document associated with the given key
	 */
	public Document getDocument(Object key) {
		if (maxEntries == 0) return null;
		
		Object value = entries.get(key);
		if (value == null) return null;
		if (value instanceof Document) return (Document) value;
		
		try {
			return XOMUtil.getBinaryXMLCodec().deserialize((byte[])value);
		} catch (BinaryParsingException e) {
			throw new RuntimeException(e); // can never happen
		}
	}

	/**
	 * Associates the given document with the given key. If the pool previously
	 * contained a mapping for this key, the old document is replaced by the
	 * given document. <code>doc == null</code> indicates that the mapping
	 * should be removed, if it exists.
	 * 
	 * @param key
	 *            the key of the document
	 * @param doc
	 *            the document to be associated with the given key; or null to
	 *            remove the mapping if it exists
	 */
	public void putDocument(Object key, Document doc) {
		if (maxEntries == 0) return;
		
		if (doc == null)
			entries.remove(key);
		else if (compressionLevel == -1)
			entries.put(key, doc);
		else
			entries.put(key, 
				XOMUtil.getBinaryXMLCodec().serialize(doc, compressionLevel));
	}
		
//	/**
//	 * Streams (pushes) the document associated with the given key through the
//	 * given node factory, returning a new result document, filtered according
//	 * to the policy implemented by the node factory. This method exactly mimics
//	 * the NodeFactory based behaviour of the XOM {@link nu.xom.Builder}.
//	 * <p>
//	 * Returns <code>null</code> if the pool contains no mapping for this key.
//	 * 
//	 * @param key
//	 *            the key of the document to retrieve
//	 * @param factory
//	 *            the node factory to stream into (may be <code>null</code>).
//	 * @return the document produced by the given node factory
//	 */
//	public Document getDocument(Object key, NodeFactory factory) {
//		if (maxEntries == 0) return null;
//		
//		Object value = entries.get(key);
//		if (value == null) return null;
//		
//		if (value instanceof Document) {
//			//if (factory == null || factory.getClass() == NodeFactory.class) return (Document) value;
//			return XOMUtil.build((Document) value, factory);
//		}
//		
//		try {
//			return XOMUtil.getBinaryXMLCodec().deserialize(
//				new ByteArrayInputStream((byte[])value), factory);
//		} catch (BinaryParsingException e) {
//			throw new RuntimeException(e); // can never happen
//		} catch (IOException e) {
//			throw new RuntimeException(e); // can never happen
//		}
//	}
	

//	/**
//	 * Removes all mappings from this map.
//	 */
//	public void clear() {
//		entries.clear();
//	}
//

}