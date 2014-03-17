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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Thread-safe XQuery/XPath fulltext index/search cache; implemented with the
 * Lucene index/search engine in on-the-fly main memory indexing mode with adaptive 
 * result caching.
 * 
 * @author whoschek@lbl.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.15 $, $Date: 2005/07/09 00:45:58 $
 */
class FullTextPool { // not a public class for now

	private static final long DEFAULT_CAPACITY = 
		XOMUtil.getSystemProperty("nux.xom.pool.FullTextPool.capacity", 40L * 1024 * 1024);
	
	private static final String FIELD_NAME = "f";	
	
	/**
	 * A default pool (can be shared freely across threads without harm); global
	 * per class loader.
	 */
	public static final FullTextPool GLOBAL_POOL = new FullTextPool();
	
	/**
	 * Heuristic cache for recently observed queries and results.
	 */
	private final Map entries;

	
	/**
	 * Creates a new pool with default parameters.
	 */
	public FullTextPool() {
		this(createConfig());
	}

	/**
	 * Creates a new pool with the given configuration.
	 * 
	 * @param config
	 *            the configuration to use
	 */
	public FullTextPool(PoolConfig config) {
		if (config == null) 
			throw new IllegalArgumentException("config must not be null");
		this.entries = Pool.createPool(config);
	}

	private static PoolConfig createConfig() {
		PoolConfig config = new PoolConfig();
		config.setMaxIdleTime(30000);
		long capacity = Runtime.getRuntime().maxMemory() / 5;
		capacity = Math.min(capacity, DEFAULT_CAPACITY);
		config.setCapacity(capacity);
		return config;
	}
	
	/**
	 * Returns the relevance score by matching the given text string against the
	 * given Lucene query expression. The score is in the range [0.0 .. 1.0],
	 * with 0.0 indicating no match. The higher the number the better the match.
	 * Typically, both analyzers are identical, but this need not be the case.
	 * 
	 * @param text
	 *            the string to match the query against
	 * @param query
	 *            the Lucene fulltext query expression
	 * @param textAnalyzer
	 *            Stream tokenizer that extracts query terms from query
	 *            according to some policy.
	 * @param queryAnalyzer
	 *            Stream tokenizer that extracts index terms from text
	 *            according to some policy.
	 * @return a number in the range [0.0 .. 1.0]
	 * @throws ParseException
	 *             if the query expression has a syntax error
	 */
	public float match(String text, String query, Analyzer textAnalyzer, Analyzer queryAnalyzer) throws ParseException {
		if (text == null) 
			throw new IllegalArgumentException("text must not be null");
		if (query == null) 
			throw new IllegalArgumentException("query expression must not be null");
		if (textAnalyzer == null) 
			throw new IllegalArgumentException("textAnalyzer must not be null");
		if (queryAnalyzer == null) 
			throw new IllegalArgumentException("queryAnalyzer must not be null");
		
		if (DEFAULT_CAPACITY <= 0) { // cache disabled?
			MemoryIndex index = new MemoryIndex();
			index.addField(FIELD_NAME, text, textAnalyzer);
			return index.search(parse(query, queryAnalyzer));
		}

		Object key = Pool.createHashKeys(new Object[] {text, query, textAnalyzer, queryAnalyzer});
		Float score = (Float) entries.get(key); // hit/miss ratio is app specific
//		Float score = null;
		if (score == null) { // cache miss
			Object qkey = Pool.createHashKeys(new Object[] {query, queryAnalyzer});
			Query luceneQuery = (Query) entries.get(qkey); // typically good hit/miss ratio
//			Query luceneQuery = null;
			if (luceneQuery == null) { // cache miss
				luceneQuery = parse(query, queryAnalyzer);
				entries.put(qkey, luceneQuery);
			}
			
			Object tkey = Pool.createHashKeys(new Object[] {text, textAnalyzer, null});
			MemoryIndex index = (MemoryIndex) entries.get(tkey);
//			MemoryIndex index = null;
			if (index == null) { // cache miss
				index = new MemoryIndex();
				index.addField(FIELD_NAME, text, textAnalyzer);
				entries.put(tkey, index);
			}

			/*
			 * TODO: Reduce the following lock scope, minimizing lock
			 * contention? Though not publicly documented anywhere, with the
			 * current impl, a MemoryIndex instance can actually safely have
			 * multiple concurrent readers, but I'm not sure that's also true
			 * for the luceneQuery instance. For the moment better safe than
			 * sorry...
			 */
			synchronized (luceneQuery) {
				score = new Float(index.search(luceneQuery));
			}
			
			entries.put(key, score);
		}
		return score.floatValue();
	}
	
	/** parses a Lucene query */
	private Query parse(String query, Analyzer analyzer) throws ParseException {
		QueryParser parser = new QueryParser(FIELD_NAME, analyzer);
		return parser.parse(query);
	}
	
}
