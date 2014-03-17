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

import java.io.File;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nux.xom.xquery.XQueryUtil;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.index.memory.PatternAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Benchmarks fulltext MemoryIndex searches.
 * <p>
 * Example usage:
 * <pre>
 * java -server nux.xom.sandbox.MemoryIndexBenchmark 300 3 samples/data/randj.xml '//line' 'Romeo friend*'
 * java -server nux.xom.sandbox.MemoryIndexBenchmark 200000 3 samples/data/randj10.xml '/' 'Romeo friend*'
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2006/03/24 03:30:09 $
 */
public class MemoryIndexBenchmark {
	
	private MemoryIndexBenchmark() {}

	/** Runs the benchmark */
	public static void main(String[] args) throws Exception {
		int iters = Integer.parseInt(args[0]); // e.g. 1000
		int runs = Integer.parseInt(args[1]);  // e.g. 3
		Document doc = new Builder().build(new File(args[2])); // e.g. "data/samples/randj.xml"
		String path = args[3]; // e.g. "//line"
		String queryExpr = args[4]; // e.g. "Capul* thou"
		
		Analyzer textAnalyzer = PatternAnalyzer.DEFAULT_ANALYZER;
		Analyzer queryAnalyzer = PatternAnalyzer.DEFAULT_ANALYZER;
		
		String field = "f";
		Nodes lines = XQueryUtil.xquery(doc, path);
		System.out.println("lines=" + lines.size());
		MemoryIndex[] indexes = new MemoryIndex[lines.size()];
		for (int i=0; i < lines.size(); i++) {
			indexes[i] = new MemoryIndex();
			indexes[i].addField(field, lines.get(i).getValue(), textAnalyzer);
		}
		doc = null;   // help gc
		lines = null; // help gc
		
		Query query = new QueryParser(field, queryAnalyzer).parse(queryExpr);
		
		float sum = 0;
		for (int run=0; run < runs; run++) {
			System.out.println("\nrun=" + run);
			long start = System.currentTimeMillis();
			for (int i=0; i < iters; i++) {
				for (int j=0; j < indexes.length; j++) {
					sum += indexes[j].search(query);
				}
			}
			long end = System.currentTimeMillis();
			System.out.println("secs = " + ((end-start) / 1000.0f));
			System.out.println("queries/sec = " + (indexes.length * iters / ((end-start) / 1000.0f)));
		}
		System.out.println("checksum=" + sum);
	}

}
