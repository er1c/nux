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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.memory.AnalyzerUtil;
import org.apache.lucene.index.memory.PatternAnalyzer;
import org.apache.lucene.queryParser.ParseException;

/**
 * Thread-safe XQuery/XPath fulltext search utilities; implemented with the
 * Lucene engine and a custom high-performance adapter for 
 * on-the-fly main memory indexing with smart caching for indexes, queries and results.
 * <p>
 * Complementing the standard XPath string and regular 
 * expression matching functionality, Lucene has a powerful query syntax with support
 * for word stemming, fuzzy searches, similarity searches, approximate searches,
 * boolean operators, wildcards, grouping, range searches, term boosting, etc.
 * For details see the <a target="_blank"
 * href="http://lucene.apache.org/java/docs/queryparsersyntax.html">Lucene Query
 * Syntax and Examples</a>.
 * Also see {@link org.apache.lucene.index.memory.MemoryIndex} 
 * and {@link PatternAnalyzer} for detailed documentation.
 * <p>
 * Example Java usage:
 * <pre>
 * Analyzer analyzer = PatternAnalyzer.DEFAULT_ANALYZER;
 * float score = FullTextUtil.match(
 *    "Readings about Salmons and other select Alaska fishing Manuals", 
 *    "+salmon~ +fish* manual~", 
 *    analyzer, analyzer);
 * if (score &gt; 0.0f) {
 *     // query matches text
 * } else {
 *     // query does not match text
 * }
 * </pre>
 * 
 * Example XQuery/XPath usage:
 * <pre>
 * declare namespace lucene = "java:nux.xom.pool.FullTextUtil"; 
 * lucene:match(
 *    "Readings about Salmons and other select Alaska fishing Manuals", 
 *    "+salmon~ +fish* manual~")
 * </pre>
 * 
 * Example XQuery/XPath usage to find all books that have a title about salmon fishing:
 * <pre>
 * declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
 * /books/book[lucene:match(title, "+salmon~ +fish* manual~") &gt; 0.0]
 * </pre>
 * 
 * An XQuery that finds all books authored by "James" that have something to do with "salmon fishing manuals", 
 * sorted by relevance:
 * <pre>
 * declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
 * declare variable $query := "+salmon~ +fish* manual~"; (: any arbitrary Lucene query can go here :)
 * (: declare variable $query as xs:string external; :)
 * 
 * for $book in /books/book[author="James" and lucene:match(abstract, $query) > 0.0]
 * let $score := lucene:match($book/abstract, $query)
 * order by $score descending
 * return $book
 * </pre>
 * 
 * Extracting sentences:
 * <pre>
 * for $book in /books/book
 *     for $s in lucene:sentences($book/abstract, 0)
 *         return
 *             if (lucene:match($s, "+salmon~ +fish* manual~") > 0.0) 
 *             then normalize-space($s)
 *             else ()
 * </pre>
 * 
 * Using a custom text tokenizer/analyzer, limiting to the first 100 words, with debug logging:
 * <pre>
 * declare namespace lucene          = "java:nux.xom.pool.FullTextUtil"; 
 * declare namespace analyzerUtil    = "java:org.apache.lucene.index.memory.AnalyzerUtil";
 * declare namespace patternAnalyzer = "java:org.apache.lucene.index.memory.PatternAnalyzer";
 * declare namespace system          = "java:java.lang.System";
 * 
 * lucene:match(
 *    "Readings about Salmons and other select Alaska fishing Manuals", 
 *    "+salmon~ +fish* manual~",
 *    analyzerUtil:getLoggingAnalyzer(
 *       analyzerUtil:getMaxTokenAnalyzer(
 *          patternAnalyzer:DEFAULT_ANALYZER(), 
 *          100),
 *       system:err(), 
 *       "log"), 
 *    patternAnalyzer:DEFAULT_ANALYZER()
 * )
 * </pre>
 * 
 * @author whoschek@lbl.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.31 $, $Date: 2005/11/14 21:38:02 $
 */
public class FullTextUtil {

	private FullTextUtil() {}
	
	/**
	 * Lucene fulltext search convenience method; equivalent to 
	 * <code>match(text, query, null, null)</code>.
	 * 
	 * @param text
	 *            the string to match the query against
	 * @param query
	 *            the Lucene fulltext query expression
	 * @return the relevance score; a number in the range [0.0 .. 1.0]
	 * @throws ParseException
	 *             if the query expression has a syntax error
	 */
	public static float match(String text, String query) throws ParseException {
		return match(text, query, null, null);
	}
	
	/**
	 * Lucene fulltext search convenience method; Returns the relevance score by
	 * matching the given text string against the given Lucene query expression.
	 * The score is in the range [0.0 .. 1.0], with 0.0 indicating no match. The
	 * higher the number the better the match.
	 * <p>
	 * Typically, both analyzers are identical, but this need not be the case.
	 * 
	 * @param text
	 *            the string to match the query against
	 * @param query
	 *            the Lucene fulltext query expression
	 * @param textAnalyzer
	 *            Stream tokenizer that extracts query terms from query
	 *            according to some policy. May be null, in which case a default is used.
	 * @param queryAnalyzer
	 *            Stream tokenizer that extracts index terms from text
	 *            according to some policy. May be null, in which case a default is used.
	 * @return the relevance score; a number in the range [0.0 .. 1.0]
	 * @throws ParseException
	 *             if the query expression has a syntax error
	 */
	public static float match(String text, String query, Analyzer textAnalyzer, Analyzer queryAnalyzer) throws ParseException {
		if (textAnalyzer == null) textAnalyzer = PatternAnalyzer.DEFAULT_ANALYZER;
		if (queryAnalyzer == null) queryAnalyzer = PatternAnalyzer.DEFAULT_ANALYZER;
		return FullTextPool.GLOBAL_POOL.match(text, query, textAnalyzer, queryAnalyzer);
	}
	
	/**
	 * Returns at most the first N paragraphs of the given text. Delimiting
	 * characters are excluded from the results. Each returned paragraph is
	 * whitespace-trimmed via String.trim(), potentially an empty string.
	 * 
	 * @param text
	 *            the text to tokenize into paragraphs
	 * @param limit
	 *            the maximum number of paragraphs to return; zero indicates "as
	 *            many as possible".
	 * @return the first N paragraphs
	 */
	public static String[] paragraphs(String text, int limit) {
		return AnalyzerUtil.getParagraphs(text, limit);
	}
		
	/**
	 * Returns at most the first N sentences of the given text. Delimiting
	 * characters are excluded from the results. Each returned sentence is
	 * whitespace-trimmed via String.trim(), potentially an empty string.
	 * 
	 * @param text
	 *            the text to tokenize into sentences
	 * @param limit
	 *            the maximum number of sentences to return; zero indicates "as
	 *            many as possible".
	 * @return the first N sentences
	 */
	public static String[] sentences(String text, int limit) {
		return AnalyzerUtil.getSentences(text, limit);
	}	

//	/**
//	 * Returns (frequency:text) pairs for distinct texts, sorted descending by
//	 * frequency (and ascending by text, if tied).
//	 * <p>
//	 * Example 1: frequencies(["hello", "world", "hello", "vietnam", "foo"], 1)
//	 * yields ["2:hello", "1:foo", "1:vietnam"]
//	 * <p>
//	 * Example 1: frequencies(["hello", "world", "hello", "vietnam", "foo"], 2)
//	 * yields ["2:hello"]
//	 * 
//	 * @param texts
//	 *            the texts (or words) to analyze; may contain duplicates in any
//	 *            order.
//	 * @param minFrequency
//	 *            return only pairs that have at least the given frequency count
//	 * @return an array of (frequency:text) pairs in the form of (freq0:text0,
//	 *         freq1:text1, ..., freqN:textN). Each pair is a single string
//	 *         separated by a ':' delimiter.
//	 */
//	public static String[] frequencies(String[] texts, int minFrequency) {
//		// compute frequencies of distinct texts
//		HashMap map = new HashMap();
//		for (int i=0; i < texts.length; i++) {
//			if (texts[i] == null) 
//				throw new IllegalArgumentException("text must not be null");
//				
//			Integer count = (Integer) map.get(texts[i]);
//			if (count == null) {
//				count = new Integer(1);
//			} else {
//				count = new Integer(count.intValue() + 1);
//			}
//			map.put(texts[i], count);
//		}
//		
//		Map.Entry[] entries = new Map.Entry[map.size()];
//		map.entrySet().toArray(entries);
//		
//		// Swap entries with frequency < minFrequency to end of array.
//		// There's no need to sort and return those.
//		int size = entries.length;
//		for (int i=entries.length; --i >= 0; ) {
//			Map.Entry e = entries[i];
//			int freq = ((Integer) e.getValue()).intValue();
//			if (freq < minFrequency) {
//				size--;
//				entries[i] = entries[size];
//				entries[size] = e;
//			}
//		}
//		
//		Arrays.sort(entries, 0, size, new Comparator() {
//			public int compare(Object o1, Object o2) {
//				Map.Entry e1 = (Map.Entry) o1;
//				Map.Entry e2 = (Map.Entry) o2;
//				int f1 = ((Integer) e1.getValue()).intValue();
//				int f2 = ((Integer) e2.getValue()).intValue();
//				if (f2 - f1 != 0) return f2 - f1;
//				String s1 = (String) e1.getKey();
//				String s2 = (String) e2.getKey();
//				return s1.compareTo(s2);
//			}
//		});
//		
//		map = null;
//		String[] pairs = new String[size];
//		for (int i=0; i < size; i++) {
//			pairs[i] = entries[i].getValue() + ":" + entries[i].getKey();
//		}
//		return pairs;
//	}
	
}
