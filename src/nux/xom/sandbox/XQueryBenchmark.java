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
import java.io.FileInputStream;
import java.io.FileOutputStream;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.XPathException;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.pool.FileUtil;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryUtil;

/**
 * Simple benchmark measuring XQuery and/or XPath performance of a given set of queries.
 * <p>
 * For XPath, the default queries and test data are taken from <a target="_blank" 
 * href="http://www.k2.dion.ne.jp/~hirsh/xpath/engineComparison-en.html">
 * Ryan Cox's and Shirasu Hiroyuki's XPath comparison</a>. 
 * (The results are drastically different than the one's they reported.)
 * The queries can be used to measure
 * rough overall performance of basic XPath expression building blocks.
 * <p>
 * For XQuery, the default queries and test data are taken from the <a target="_blank" 
 * href="http://www.xml-benchmark.org/">XMark XQuery benchmark</a>.
 * XMark consists of 20 carefully chosen queries, each stressing key performance aspects of 
 * exact match, ordered access, regular XPath expressions, following references, 
 * construction of complex results, join on values, search for missing elements, and so on.
 * See their tech report for details of the underlying rationale.
 * <p>
 * The default queries and test data are included in the download, along with other samples.
 * <p>
 * Disclaimer: Note that any given queries may or may not reflect your specific application usage
 * profile. As always, your mileage may vary, and your applications may exercise
 * significantly different query operations. Hence, make sure to cross-check
 * with your own application benchmarks.
 * <p>
 * Example usage:
 * 
 * <pre>
 * export CLASSPATH=lib/nux.jar:lib/saxon8.jar:lib/xom.jar
 * # XPATH benchmark
 * java -server nux.xom.sandbox.XQueryBenchmark 1000 3 cache samples/data/romeo.xml samples/xpath/queries1.xml 
 * java -server nux.xom.sandbox.XQueryBenchmark 1000 3 nocache samples/data/romeo.xml samples/xpath/queries1.xml 
 * java -server nux.xom.sandbox.XQueryBenchmark 1000 3 xom samples/data/romeo.xml samples/xpath/queries1.xml 
 * 
 * # XQUERY benchmark
 * java -server nux.xom.sandbox.XQueryBenchmark 1000 3 cache samples/xmark/auction-0.01.xml samples/xmark/*.xq 
 * </pre>
 * 
 * The first three examples run each XPath query found in the file queries1.xml 1000 times against
 * the romeo.xml data file (230KB), repeating all of it for 3 repetition blocks. 
 * Results are given in milliseconds and, perhaps more interestingly, queries/sec.
 * <p>
 * A parameter says that the benchmark should be separately run for Nux
 * with the precompiled query cache enabled ("cache") or disabled ("nocache"), 
 * and then with the Jaxen based XPath implementation of xom-1.1 CVS ("xom").
 * <p>
 * The last example runs all XQueries (*.xq files) in the xmark/ directory 1000 times 
 * against the auction-0.01.xml file (1 MB), repeating all of it for 3 repetition blocks.
 * <p>
 * Note that the time of the first 2 repetition blocks should not be considered for timing 
 * comparisons, because the JVM hotspot compiler introduces strong perturbations on warmup. 
 * Ignore those and scroll down to the last repetition block.
 * <p>
 * Here are example Nux XPath outputs for a
 * <a href="doc-files/xpathbench-nux-cache.txt">230 KB</a> and 
 * <a href="doc-files/xpathbench-nux-cache-1000.txt">230 MB</a> 
 * romeo.xml file with JDK 1.5 server VM on a dual Pentium4 @ 2.8Ghz, 2 GB, Redhat 9, 
 * a typical commodity cluster node configuration.
 * Note that the 230 MB file requires some 1.0 GB for the XOM main memory tree.
 * Overall, both scenarios demonstrate excellent performance, with execution time 
 * growing at most linearly with the problem size even under harsh conditions.
 * <p>
 * Here are example Nux XQuery XMark outputs for a
 * <a href="doc-files/xmark-0.01.txt">1 MB</a> and 
 * <a href="doc-files/xmark-0.1.txt">10 MB</a> 
 * auction.xml file with JDK 1.5 server VM on a dual Pentium4 @ 2.8Ghz, 2 GB, Redhat 9, 
 * a typical commodity cluster node configuration.
 * Again, both scenarios demonstrate excellent overall performance, with the caveat that
 * JOIN performance inside saxonb-8.6 currently does not (yet) scale well with data size
 * (q08.xq - q12.xq).
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2006/04/29 03:05:33 $
 */
public final class XQueryBenchmark {
	
	public static boolean IS_BENCHMARK = false;
	public static int dummy = 0;
	
	private XQueryBenchmark() {}

	/**
	 * Reads an XML file and multiplies its size by concatenating it N times.
	 * This can be used to produce benchmark data files of a given desired size.
	 * For example, this will produce an output file that is 100 times larger 
	 * than the input file, containing the same data 100 times:
	 * <pre>
	 * java nux.xom.tests.XQueryBenchmark data/romeo.xml 100 data/romeo100.xml
	 * </pre>
	 */
	public static void generateTestData(String[] args) throws Exception {
		Document doc = new Builder().build(new File(args[0])); // e.g. "romeo.xml"
		int times = Integer.parseInt(args[1]);    // e.g. 100
		Elements children = doc.getRootElement().getChildElements();
		for (int k=0; k < times; k++) {
			for (int i=0; i < children.size(); i++) {
				doc.getRootElement().appendChild(children.get(i).copy());
			}
		}
		
		FileOutputStream out = new FileOutputStream(args[2]); // e.g. "romeo100.xml"
		Serializer ser = new Serializer(out);
		ser.write(doc);
		ser.flush();
		out.close();
	}

	private static Document readDocument(String fileName) throws Exception {
		System.out.print("Now reading " + fileName);
		long start = System.currentTimeMillis();
		Document doc;
		if (fileName.endsWith(".bnux")) { // it's a binary xml file
			byte data[] = FileUtil.toByteArray(new FileInputStream(fileName));
			doc = new BinaryXMLCodec().deserialize(data);
		}
		else { // it's a standard textual XML file
			doc = new Builder().build(new File(fileName));
		}
		long end = System.currentTimeMillis();
		System.out.println(" ms=" + (end-start));
		return doc;
	}
		
	/** Runs the benchmark */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) { System.err.println("See javadoc for help on usage."); return; }
		System.out.println("Environment: " + getSystemInfo());
		String str = "";
		for (int i =0; i < args.length; i++) str += args[i] + " ";
		System.out.println("Now running java nux.xom.tests.XQueryBenchmark " + str);
		
		int runs = Integer.parseInt(args[0]);    // e.g. 1000
		int repeats = Integer.parseInt(args[1]); // e.g. 3
		int mode = -1; // "static"
		if (args[2].equalsIgnoreCase("cache")) mode = 0;
		if (args[2].equalsIgnoreCase("nocache")) mode = 1;
		if (args[2].equalsIgnoreCase("xom")) mode = 2;
//		if (args[2].equalsIgnoreCase("jaxpxom")) mode = 3;
		boolean check = (runs == 1);
		Document doc = readDocument(args[3]); // e.g. "romeo.xml"
		
		for (int k=0; k < repeats; k++) { // for each repetition block
			for (int q=4; q < args.length; q++) { // for each query file
				Node[] contexts;
				String[] selects;
				int[] types;
				Object[] expected;
				
				if (args[q].endsWith(".xq")) { // it is an XQuery
					System.out.println("Now reading " + args[q]);
					contexts = new Node[] { doc };
					selects = new String[] { FileUtil.toString(new FileInputStream(args[q]), null) };
					types = new int[] {0};
					expected = new Object[] { "" };
				}
				else { // it is a document containing zero or more XPath queries
					Document queries = readDocument(args[q]); // e.g. "xpath/queries1.xml"
					
					Nodes paths = XQueryUtil.xquery(queries, "/paths/document/path");		
					contexts = new Node[paths.size()];
					selects = new String[paths.size()];
					types = new int[paths.size()];
					expected = new Object[paths.size()];
					
					// precompute all the info necessary to run the bench
					for (int i=0; i < paths.size(); i++) {
						Element path = (Element) paths.get(i);
						Attribute ctxAttr = path.getAttribute("context");
						
						contexts[i] = ctxAttr == null ? doc : XQueryUtil.xquery(doc, ctxAttr.getValue()).get(0);
						selects[i] = path.getAttribute("select").getValue();
						types[i] = 0;
						if (path.getAttribute("type") != null) {
							String[] flavours = {"count", "string", "double", "boolean"};
							types[i] = java.util.Arrays.asList(flavours).indexOf(path.getAttribute("type").getValue());
						}
						expected[i] = path.getValue();
					}
				}
				
				// for each query
				for (int i=0; i < selects.length; i++) {
					int actualRuns = runs;
					if (selects[i].equals("//*[contains(string(.),'Capulet')]")) {
//						actualRuns = Math.min(runs, 10); // this one would take too long
						continue; // ignore 
					}
					System.out.print("query = " + selects[i] + "  ");
					if (actualRuns == 1) System.out.println(
						"\nexplain = \n" + new XQuery(selects[i], null).explain());
					XQuery xquery = new XQuery(selects[i], null);
									
					// now execute the query N times and measure execution time
					long start = System.currentTimeMillis();
					IS_BENCHMARK = true;
					Nodes results = run(contexts[i], selects[i], actualRuns, mode, types[i], xquery);
					IS_BENCHMARK = false;
					long end = System.currentTimeMillis();
					
					if (check && results != null) { // found the right results?
						for (int j=0; j < results.size(); j++) {
							System.out.println("node " + j + ": " + results.get(j).toXML());
						}
						Node first = results.size() == 0 ? null : results.get(0);	
						Object actual = null;
						switch (types[i]) {
							case 0 : actual = String.valueOf(results.size()); break;
							case 1 : actual = first == null ? "" : first.getValue(); break;
							case 2 : actual = first == null ? "0.0" : new Double(first.getValue()).toString(); break;
							case 3 : actual = first == null ? "false" : first.getValue().equals("true") ? "true" : "false"; break;
							default: throw new IllegalStateException();
						}				
						if (expected[i] instanceof String && ((String)expected[i]).length() > 0 && !expected[i].equals(actual)) {
							System.out.print("expected="+expected[i]);
							System.out.println(", actual="+actual);
							System.exit(0);
						}
						System.out.print(" result=" + actual + ",");
					}
					
					System.out.println(" ms=" + (end-start) + ", queries/sec=" + actualRuns/((end-start)/1000.0f));
				}
			}
			System.out.println("done with repetition "+ k + "\n\n");
			if (k < repeats-1) Thread.sleep(3000); // give hotspot VM some time to finish compilation
		}
	}
	
	private static Nodes run(Node contextNode, String query, int runs, int mode, int type, XQuery xquery) throws Exception {
		Nodes results = null;
		for (int run=0; run < runs; run++) {
			switch (mode) {
				case -1 : // Nux with static query
//					dummy += xquery.execute(contextNode).next().getBaseURI().length();
					results = xquery.execute(contextNode).toNodes();
					break;
				case 0 : // Nux with query cache
					results = XQueryUtil.xquery(contextNode, query);
					break;
				case 1 : // Nux without query cache
					results = new XQuery(query, null).execute(contextNode).toNodes();
					break;
				case 2 : // xom-1.1
					try {
						results = contextNode.query(query);
					} catch (XPathException e) {
						System.out.println("****** XOM can't handle this query: "+ e.getMessage());
						return null;
					}
					break;
//				case 3: // saxon JAXP-1.3 XOM API
//					results = new JaxpXOMHelper().query(contextNode, query, type);
//					break;
				default: throw new IllegalStateException("unknown mode");
			}
		}
		return results;
	}
	
	private static String getSystemInfo() {
		String str = "java " + System.getProperty("java.version") + ", " + System.getProperty("java.vm.name");
		str += ", " + System.getProperty("java.vendor") + ", " + System.getProperty("os.name");
		str += ", " + System.getProperty("os.version") + ", " + System.getProperty("os.arch");
		return str;
	}
	
//	///////////////////////////////////////////////////////////////////////////////
//	// Nested classes:
//	///////////////////////////////////////////////////////////////////////////////
//	private static final class JaxpXOMHelper { 
//		// this nested class avoids JAXP-1.3 ClassNotFoundExceptions if it's actually not called
//		
//		private static final XPath xpath;
//		
//		static { // setup expensive things once and for all
//			System.setProperty("javax.xml.xpath.XPathFactory:"+NamespaceConstant.OBJECT_MODEL_XOM,
//            	"net.sf.saxon.xpath.XPathFactoryImpl");
//			try {
//				XPathFactory factory = XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_XOM);
//				xpath = factory.newXPath();
//			} catch (XPathFactoryConfigurationException e) {
//				throw new RuntimeException(e);
//			}
//		}
//		
//		public Nodes query(Node contextNode, String query, int type) throws Exception {
//			XPathExpression exp = xpath.compile(query);
//			Nodes results = new Nodes();
//			switch (type) {
//				case 0 :
//					List list = (List) exp.evaluate(contextNode, XPathConstants.NODESET);
//					if (list != null) {
//						Iterator iter = list.iterator();
//						while (iter.hasNext()) {
//							Node node = (Node) iter.next();
//							results.append(node);
//						}
//					}
//					break;
//				case 1 : 
//					results.append(new Text(String.valueOf(
//							exp.evaluate(contextNode, XPathConstants.STRING))));
//					break;
//				case 2 : 
//					results.append(new Text(String.valueOf(
//							exp.evaluate(contextNode, XPathConstants.NUMBER))));
//					break;
//				case 3 : 
//					results.append(new Text(String.valueOf(
//							exp.evaluate(contextNode, XPathConstants.BOOLEAN))));
//					break;
//				default : throw new IllegalStateException();
//			}
//			return results;
//		}		
//	}

}