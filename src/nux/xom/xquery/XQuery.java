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
package nux.xom.xquery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trace.XQueryTraceListener;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.xom.DocumentWrapper;
import nu.xom.Builder;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nux.xom.pool.BuilderPool;
import nux.xom.pool.DocumentFactory;
import nux.xom.pool.DocumentURIResolver;

import org.xml.sax.InputSource;

/**
 * Compiled representation of a W3C XQuery (thread-safe).
 * Since XQuery can be seen as a superset of XPath 2.0 this class can also be used with 
 * plain XPath expressions as queries.
 * <p> 
 * Instances are considered immutable and thread-safe; The same compiled query may 
 * be executed (evaluated) many times in series or in parallel,
 * just like {@link nu.xom.xslt.XSLTransform} objects.
 * A compiled query is conceptually similar to a JDBC {@link java.sql.PreparedStatement}.
 * 
 * <h4>Example usage</h4>
 * 
 * <pre>
 *     Document doc = new Builder().build(new File("samples/data/periodic.xml"));
 *  
 *     // find the atom named 'Zinc' in the periodic table:
 *     Node result = XQueryUtil.xquery(doc, "/PERIODIC_TABLE/ATOM[NAME = 'Zinc']").get(0);
 *     System.out.println("result=" + result.toXML());
 * 
 *     // equivalent via the more powerful underlying API:
 *     XQuery xquery = new XQuery("/PERIODIC_TABLE/ATOM[NAME = 'Zinc']", null);
 *     Node result = xquery.execute(doc).next();
 * 
 *     // count the numer of elements in a document tree:
 *     int count = XQueryUtil.xquery(doc, "//*").size();
 *     System.out.println("count=" + count);
 * 
 *     // equivalent via the XPath count() function:
 *     int count = Integer.parseInt(XQueryUtil.xquery(doc, "count(//*)").get(0).getValue());
 *     System.out.println("count=" + count);
 * </pre>
 * 
 * A query to find the links of all images (or all JPG images) in a XHTML-like document:
 * <pre>
 *     Document doc = new Builder().build(new File("/tmp/test.xml"));
 *     Nodes results = XQueryUtil.xquery(doc, "//*:img/@src");
 *     // Nodes results = XQueryUtil.xquery(doc, "//*:img/@src[matches(., '.jpg')]");
 * 
 *     for (int i=0; i < results.size(); i++) {
 *         System.out.println("node "+i+": "+results.get(i).toXML());
 *         //System.out.println("node "+i+": "+ XOMUtil.toPrettyXML(results.get(i)));
 *     }
 * </pre>
 *
 *
 * <h4>Namespaces</h4>
 * 
 * A query can use namespaces. 
 * Here is an example that lists the titles of Tim Bray's blog articles via the Atom feed:
 * <pre>
 * declare namespace atom = "http://www.w3.org/2005/Atom"; 
 * declare namespace xsd = "http://www.w3.org/2001/XMLSchema";
 * doc("http://www.tbray.org/ongoing/ongoing.atom")/atom:feed/atom:entry/atom:title
 * </pre>
 * 
 * Namespace declarations can be defined inline within the query prolog via <code>declare namespace</code>, 
 * and <code>declare default element namespace</code> directives, as described above. 
 * They can also be defined via the <code>declareNamespace()</code> methods and 
 * <code>setDefaultElementNamespace()</code> method of a {@link StaticQueryContext}.
 * 
 * 
 * <h4>Passing variables to a query</h4>
 * 
 * A query can declare <i>local</i> variables, for example:
 * <pre>
 *     declare variable $i := 7;
 *     declare variable $j as xs:integer := 7;
 *     declare variable $math:pi as xs:double := 3.14159E0;
 *     declare variable $bookName := 'War and Peace';
 * </pre>
 * 
 * A query can access variable values via the standard 
 * <code>$varName</code> syntax, as in <code>return ($x, $math:pi)</code> or 
 * <code>/books/book[@name = $bookName]/author[@name = $authorName]</code>.
 * <p>
 * A query can declare <i>external global</i> variables, for example: 
 * <pre>
 *     declare variable $foo     as xs:string external; 
 *     declare variable $size    as xs:integer external; 
 *     declare variable $myuri   as xs:anyURI external;
 *     declare variable $mydoc   as document-node() external;
 *     declare variable $myelem  as element() external;
 *     declare variable $mynodes as node()* external;
 * </pre>
 * 
 * External global variables can be bound and passed to the query as follows:
 * <pre>
 *     Map vars = new HashMap();
 *     vars.put("foo", "hello world");
 *     vars.put("size", new Integer(99));
 *     vars.put("myuri", "http://www.w3.org/2001/XMLSchema");
 *     vars.put("mydoc", new Document(new Element("xyz")));
 *     vars.put("myelem", new Element("abc"));
 *     vars.put("mynodes", new Node[] {new Document(new Element("elem1")), new Element("elem2"))});
 *     vars.put("mydocs", new Node[] {
 *         new Builder().build(new File("samples/data/articles.xml")), 
 *         new Builder().build(new File("samples/data/p2pio.xml")) });
 *     
 *     String query = "for $d in $mydocs return $size * count($d)";
 *     Nodes results = new XQuery(query, null).execute(doc, null, vars).toNodes();
 *     new ResultSequenceSerializer().write(results, System.out);
 * </pre>
 * 
 * 
 * <h4>Standard functions, user defined functions and extension functions</h4>
 * 
 * The <a target="_blank" href="http://www.saxonica.com/documentation/functions/intro.html">Standard XQuery functions</a> can be used directly.
 * Also note that XPath 2.0 supports regular expressions via the standard
 * <code>fn:matches</code>, <code>fn:replace</code>, and <code>fn:tokenize</code> 
 * functions. For example:
 * <pre>
 * string-length('hello world');
 * </pre>
 *  
 * <p>
 * A query can employ <i>user defined functions</i>, for example: 
 * <pre>
 *     declare namespace ipo = "http://www.example.com/IPO";
 *     
 *     declare function local:total-price( $i as element(item)* ) as xs:double {
 *         let $subtotals := for $s in $i return $s/quantity * $s/USPrice
 *         return sum($subtotals)
 *     }; 
 *     
 *     for $p in doc("ipo.xml")/ipo:purchaseOrder
 *     where $p/shipTo/name="Helen Zoe" and $p/@orderDate = xs:date("1999-12-01")
 *     return local:total-price($p//item) 
 * </pre>
 * 
 * <i>Custom extension functions</i> written in Java can be defined and used as explained in the
 * <a target="_blank" href="http://www.saxonica.com/documentation/extensibility/functions.html">Saxon Extensibility Functions</a> documentation.
 * For example, here is query that outputs the square root of a number via a method in java.lang.Math,
 * as well as calls static methods and constructors of java.lang.String, java.util.Date
 * as well as other extension functions.
 * <pre>
 *     declare namespace exslt-math = "http://exslt.org/math";
 *     declare namespace math   = "java:java.lang.Math";
 *     declare namespace date   = "java:java.util.Date"; 
 *     declare namespace string = "java:java.lang.String"; 
 *     declare namespace saxon  = "http://saxon.sf.net/";
 * 
 *     declare variable $query := string(doc("query.xml")/queries/query[1]);
 * 
 *     (
 *     exslt-math:sin(3.14)
 *     math:sqrt(16),
 *     math:pow(2,16),
 *     string:toUpperCase("hello"),
 *     date:new(),                    (: print current date :)
 *     date:getTime(date:new())       (: print current date in milliseconds :)
 * 
 *     saxon:eval(saxon:expression($query))  (: run a dynamically constructed query :)
 *     )
 * </pre>
 *
 *
 * <h4>Modules</h4>
 * 
 * An XQuery module is a file containing a set of variable and function declarations. 
 * For decomposition and reuse of functionality a module can import declarations from 
 * other modules. Here are two example modules generating the factorial of a number:
 * 
 * <pre>
 *     (: file modules/factorial.xq :)
 *     module namespace factorial = "http://example.com/factorial";
 *     declare function factorial:fact($i as xs:integer) as xs:integer {
 *         if ($i <= 1)
 *             then 1
 *             else $i * factorial:fact($i - 1)
 *     };
 *
 *     (: file main.xq :)
 *     import module namespace factorial = "http://example.com/factorial" at "modules/factorial.xq";
 *     factorial:fact(4)
 * 
 *     [hoschek /Users/hoschek/unix/devel/nux] fire-xquery main.xq
 *     &lt;atomic-value xsi:type="xs:integer" xmlns="http://dsd.lbl.gov/nux" 
 *         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">24&lt;/atomic-value>
 * </pre>
 * 
 * 
 * <h4>Customizing the doc() function</h4>
 * 
 * A custom document URI resolver for the XQuery/XPath <code>doc()</code> function can be defined 
 * in the constructor of this class.
 * Other miscellaneous options can be made available to the query by calling configuration methods
 * on a {@link DynamicQueryContext} (per execution),
 * or on the configuration object of a {@link StaticQueryContext} (per query).
 * 
 * 
 * <h4>Performance</h4>
 * 
 * For simple XPath expressions you can get a throughput of up to
 * 1000-20000 (100000) executions/sec over 200 (0.5) KB input documents, 
 * served from memory (commodity PC 2004, JDK 1.5, server VM).
 * Be aware that this is an example ballpark figure at best, because use cases, 
 * documents and the complexity of queries vary wildly in practise.
 * In any case, it is safe to assume that this XQuery/XPath implementation is 
 * one of the fastest available. For details, see {@link nux.xom.sandbox.XQueryBenchmark}.
 *
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.125 $, $Date: 2006/06/18 20:42:25 $
 */
public class XQuery {
	
	private final XQueryExpression expression; // immutable hence implicitly thread-safe
	private final StaticQueryContext staticContext;
			
	/** enable low-level Saxon instruction tracing output on System.err? */
	private static final boolean TRACE = 
		XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.trace", false);
	
	/** enable Nux debug output on System.err? */
	private static final boolean DEBUG = 
		XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.debug", false);
	
	/** allow calls to extension functions implemented in Java (security/trust)? */
	private static final boolean ALLOW_EXTERNAL_FUNCTIONS = 
		XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.allowExternalFunctions", true);
	
	/** Use the same or separate name pools for each XQuery object? */
	private static final boolean SHARE_NAMEPOOLS = 
		XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.shareNamePools", true);
		

	/**
	 * Constructs a new compiled XQuery from the given query.
	 * 
	 * @param query
	 *            the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function. (May be <code>null</code> in
	 *            which case it defaults to the current working directory).
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type mismatches.
	 */		
	public XQuery(String query, URI baseURI) throws XQueryException  {
		this(query, baseURI, null, null);
	}
	
	/**
	 * Constructs a new compiled XQuery from the given query, base URI, static
	 * context and resolver.
	 * 
	 * @param query
	 *            the query to compile
	 * @param baseURI
	 *            an absolute URI, used when necessary in the resolution of
	 *            relative URIs found in the query. Used by the XQuery
	 *            <code>doc</code> function, and hence the resolver. May be
	 *            <code>null</code> in which case it defaults to the current
	 *            working directory.
	 * @param staticContext
	 *            the context and configuration to use; per query (may be
	 *            <code>null</code>).
	 * @param resolver
	 *            an object that is called by the XQuery processor to turn a URI
	 *            passed to the XQuery <code>doc()</code> function into a XOM
	 *            {@link Document}. May be <code>null</code> in which case
	 *            non-validating non-pooled default resolution is used.
	 * 
	 * @throws XQueryException
	 *             if the query has a syntax error, or if it references
	 *             namespaces, variables, or functions that have not been
	 *             declared, or contains other static errors such as type
	 *             mismatches.
	 */
	public XQuery(String query, URI baseURI, StaticQueryContext staticContext, 
				DocumentURIResolver resolver) throws XQueryException {
		
		if (query == null) throw new IllegalArgumentException("query must not be null");
		if (staticContext == null) {
			staticContext = new StaticQueryContext(createConfiguration());
		} else {
			staticContext = staticContext.copy();
		}
		
		this.staticContext = staticContext;
		if (baseURI != null) staticContext.setBaseURI(baseURI.toASCIIString());
		
		final Configuration config = staticContext.getConfiguration();
		if (resolver == null) resolver = new DefaultDocumentURIResolver(config);
		final DocumentURIResolver myResolver = resolver;
		config.setURIResolver(
			new URIResolver() {
				public Source resolve(String href, String baseURI) throws TransformerException {
					try {
						Document doc = myResolver.resolve(href, baseURI);
						if (doc == null) { // fallback to default mechanism
							doc = new DefaultDocumentURIResolver(config).resolve(href, baseURI);
						}
						return wrap(doc, null);
					} catch (ParsingException e) {
						throw new TransformerException(e);
					} catch (IOException e) {
						throw new TransformerException(e);
					}
				}
			}
		);
		
		try { // generate Saxon's compiled query representation
			this.expression = staticContext.compileQuery(query);
		} catch (TransformerException e) {
			throw new XQueryException(e);
		}				
	}

	/**
	 * Executes (evaluates) the query against the given node.
	 * Results are returned in 
	 * <a target="_blank" href="http://www.w3.org/TR/xpath#dt-document-order">document order</a>, 
	 * unless specified otherwise by the query.
	 * 
	 * @param contextNode
	 *            the context node to execute the query against. The context
	 *            node is available to the query as the value of the query
	 *            expression ".". If this parameter is <code>null</code>, the
	 *            context node will be undefined.
	 * @return a result sequence iterator producing zero or more results
	 * @throws XQueryException
	 *             if an error occurs during execution, for example division
	 *             overflow, or a type error caused by type mismatch, or an
	 *             error raised by the XQuery function fn:error().
	 */
	public ResultSequence execute(Node contextNode) throws XQueryException  {
		return execute(contextNode, null, null);
	}
	
	/**
	 * Executes (evaluates) the query against the given node, using
	 * the given dynamic context and external variables.
	 * Results are returned in 
	 * <a target="_blank" href="http://www.w3.org/TR/xpath#dt-document-order">document order</a>, 
	 * unless specified otherwise by the query.
	 * <p>
	 * Argument <code>variables</code> specifies external global variables in
	 * the form of zero or more <code>variableName --> variableValue</code>
	 * map associations. Each map entry's key and value are interpreted as
	 * follows:
	 * <ul>
	 * <li>variableName (key): The name of the variable in "{uri}local-name"
	 * format. It is not an error to supply a value for a variable that has not
	 * been declared in the query, the variable will simply be ignored. If the
	 * variable has been declared in the query (as an external global variable)
	 * then it will be initialized with the value supplied.</li>
	 * 
	 * <li>variableValue (value): The value of the variable. This can be any
	 * valid Java object. In particular it can be a {@link Node} object or
	 * a {@link Node}[] array or a {@link Nodes} object. It follows the same conversion rules as a
	 * value returned from a Saxon extension function. An error will occur at
	 * query execution time if the supplied value cannot be converted to the
	 * required type as declared in the query. For precise control of the type
	 * of the value, instantiate one of the classes in the
	 * {@link net.sf.saxon.value} package, for example
	 * {@link net.sf.saxon.value.DateTimeValue}.</li>
	 * </ul>
	 * 
	 * @param contextNode
	 *            the context node to execute the query against. The context
	 *            node is available to the query as the value of the query
	 *            expression ".". If this parameter is <code>null</code>, the
	 *            context node will be undefined.
	 * @param dynamicContext
	 *            optional dynamic context of this execution (may be
	 *            <code>null</code>). If not <code>null</code>, the
	 *            Configuration object of the dynamic context must be the
	 *            same as the Configuration object that was used when creating
	 *            the StaticQueryContext.
	 * @param variables
	 *            optional external global variables to be bound on the dynamic
	 *            context; per execution (may be <code>null</code>).
	 * @return a result sequence iterator producing zero or more results
	 * @throws XQueryException
	 *             if an error occurs during execution, for example division
	 *             overflow, or a type error caused by type mismatch, or an
	 *             error raised by the XQuery function fn:error().
	 */
	public ResultSequence execute(Node contextNode, 
			DynamicQueryContext dynamicContext, Map variables) throws XQueryException {
		
		if (dynamicContext == null) {
			dynamicContext = new DynamicQueryContext(getStaticContext().getConfiguration());
		}
		
		try {
			setupDynamicContext(contextNode, dynamicContext, variables);
		} catch (TransformerException e) {
			throw new XQueryException(e);
		}
		
		return newResultSequence(this.expression, dynamicContext);
	}
	
	/**
	 * Returns a description of the compiled and optimized expression tree;
	 * useful for advanced performance diagnostics only.
	 * 
	 * @return a string description
	 */
	public String explain() {
		// work around for incompatibility introduced in saxon-8.7.1
		try {
			return explain871();
		} catch (Throwable t) {
			try {
//				this.expression.getExpression().display(
//					0, getStaticContext().getNamePool(), new PrintStream(out));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Class[] types = new Class[] { 
						Integer.TYPE, 
						NamePool.class, 
						PrintStream.class};
				Object[] args = new Object[] { 
						new Integer(0), 
						getStaticContext().getNamePool(), 
						new PrintStream(out)};
				Expression.class.getMethod("display", types).invoke(
						this.expression.getExpression(), args);
				return out.toString();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	// saxon >= 8.7.1
	private String explain871() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		this.expression.getExpression().display(
				0, new PrintStream(out), getStaticContext().getConfiguration());
		return out.toString();
	}
	
	/**
	 * Callback that returns a result sequence for the current query execution.
	 * <p>
	 * An XQuery result sequence may, apart from "normal" nodes, also contain
	 * <b>top-level</b> values of <a target="_blank"
	 * href="http://www.w3.org/TR/xpath-functions/#datatypes">atomic types </a>
	 * such as xs:string, xs:integer, xs:double, xs:boolean, etc, all of which are not XML nodes.
	 * Hence, a way to convert atomic values to normal XML nodes is needed.
	 * <p>
	 * This method's result sequence implementation converts each <b>top-level</b>
	 * atomic value to an {@link nu.xom.Element} named "atomic-value" with a child
	 * {@link nu.xom.Text} node holding the atomic value's standard XPath 2.0 string representation. 
	 * An "atomic-value" element is decorated with a namespace and a W3C XML Schema type attribute.
	 * "Normal" nodes and anything not at top-level is returned "as is", without conversion.
	 * <p>
	 * Overrride this default implementation if you need custom conversions in
	 * result sequence implementations. Note however, that conversions of atomic
	 * values should rarely be used. It is often more desirable to avoid such
	 * atomic conversions altogether. This can almost always easily be achieved
	 * by formulating the <b>xquery string</b> such that it wraps atomic values via
	 * standard XQuery constructors (rather than via Java methods) into an element
	 * or attribute or text or document. That way, the query always produces a "normal"
	 * XML node sequence as output, and never produces a sequence of atomic
	 * values as output, and thus custom conversion by this class are never
	 * needed and invoked.
	 * <p>
	 * For example, by default the following query 
	 * <pre>
	 *     for $i in (1, 2, 3) return $i
	 * </pre>
	 * yields this (perhaps somewhat unexpected) output:
	 * <pre>
	 *     &lt;item:atomic-value xsi:type="xs:integer" xmlns:item="http://dsd.lbl.gov/nux" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">1&lt;/atomic-value>
	 *     &lt;item:atomic-value xsi:type="xs:integer" xmlns:item="http://dsd.lbl.gov/nux" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">2&lt;/atomic-value>
	 *     &lt;item:atomic-value xsi:type="xs:integer" xmlns:item="http://dsd.lbl.gov/nux" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">3&lt;/atomic-value>
	 * </pre>
	 * <p>
	 * This format is good for software processing, but not very human readable.
	 * Hence, most likely you will want to rewrite the query along the following lines: 
	 * <pre>
	 *     for $i in (1, 2, 3) return &lt;item> {$i} &lt;/item>
	 * </pre>
	 * now yielding as output three {@link nu.xom.Element} nodes:
	 * <pre>
	 *     &lt;item>1&lt;/item> 
	 *     &lt;item>2&lt;/item>
	 *     &lt;item>3&lt;/item>
	 * </pre>
	 * Or you might want to rewrite the query along the following lines: 
	 * <pre>
	 *     for $i in (1, 2, 3) return text {$i}
	 * </pre>
	 * now yielding as output three {@link nu.xom.Text} nodes:
	 * <pre>
	 *     1 
	 *     2
	 *     3
	 * </pre>
	 * Observe that the rewritten query converts top-level atomic values 
	 * precisely as desired by the user, without needing any Java-level conversion.
	 * 
	 * @param expression
	 *            the compiled query expression
	 * @param dynamicContext
	 *            the dynamic context of this execution
	 * @return a result sequence implementation
	 * @throws XQueryException
	 *             if an error occurs during execution
	 */
	protected ResultSequence newResultSequence(XQueryExpression expression, 
			DynamicQueryContext dynamicContext) throws XQueryException {
		
		try {
			return new DefaultResultSequence(
				expression.iterator(dynamicContext), 
				getStaticContext().getConfiguration());
		} catch (TransformerException e) {
			throw new XQueryException(e);
		}
	}
	
	/** Creates and returns a default configuration object. */
	private static Configuration createConfiguration() {
		Configuration config = new Configuration(); 
		config.setHostLanguage(Configuration.XQUERY);
		config.setErrorListener(new DefaultErrorListener());
		config.setAllowExternalFunctions(ALLOW_EXTERNAL_FUNCTIONS);
		if (!SHARE_NAMEPOOLS) config.setNamePool(new NamePool());
//		config.setAllNodesUntyped(true);
//		config.setLazyConstructionMode(true);
//		config.setModuleURIResolver(new DefaultModuleURIResolver());
		
		if (TRACE) { 
			/*
			 * Caution: XQueryTraceListener is not strictly thread-safe. This is
			 * mostly harmless given its current implementation. Nonetheless,
			 * it's better not to use tracing in multi-threaded production use;
			 * use this only in development/profiling stages.
			 */
			config.setTraceListener(new XQueryTraceListener());
			config.setTraceExternalFunctions(true);
//			config.setTiming(true);
		}
		
		return config;
	}
	
	/**
	 * Returns a Node that saxon can read from.
	 * 
	 * This implementation is somewhat complicated because it allocates nomore
	 * DocWrapper instances than absolutely required. Nodes in the same subtree
	 * are considered part of the same virtual document if they are physically
	 * document-less. This yields more meaningful inter document order sorts,
	 * and improves performance for variables containing many nodes.
	 * Note that for XOM, equality of node keys in a hash table means identity.
	 * 
	 * docWrappers == null disables the allocation minimization feature (not recommended).
	 */
	private NodeInfo wrap(Node node, HashMap docWrappers) throws TransformerException {
		if (node == null) 
			throw new TransformerException("node must not be null");
		if (node instanceof DocType)
			throw new TransformerException("DocType can't be queried by XQuery/XPath");
		
		Node root = node;
		while (root.getParent() != null) {
			root = root.getParent();
		}

		DocumentWrapper docWrapper = null;
		if (docWrappers != null) docWrapper = (DocumentWrapper) docWrappers.get(root);
		
		if (docWrapper == null) { // root has not been seen before
			docWrapper = new DocumentWrapper(root, root.getBaseURI(), getStaticContext().getConfiguration());
			
			// remember the DocWrapper for the given root so we can reuse it later
			if (docWrappers != null) docWrappers.put(root, docWrapper);
		}
		
		return docWrapper.wrap(node);
	}
	
	/** Setup variables of context, if any */
	private void setupDynamicContext(
			Node contextNode, DynamicQueryContext dynamicContext, Map variables) 
			throws TransformerException {
		
		// setup context node
		HashMap docWrappers = null;
		if (variables != null && variables.size() > 0) { 
			docWrappers = new HashMap(); // slow path
		}
		if (contextNode != null) {
			dynamicContext.setContextItem(wrap(contextNode, docWrappers));
		}
		if (docWrappers == null) {
			return; // fast path
		}
		
		// setup variables
		Iterator iter = variables.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			if (entry.getKey() == null) 
				throw new IllegalArgumentException("variable key must not be null");
			String name = entry.getKey().toString();
			if (name.length() > 0 && name.charAt(0) == '$') {
				name = name.substring(1); // silently fixup harmless user bug
			}
			Object value = entry.getValue();
			
			if (value instanceof CharSequence) {
				value = new UntypedAtomicValue((CharSequence) value);
			}
			else if (value instanceof Node) {
				value = wrap((Node)value, docWrappers);
				// pass xml document(s) such that saxon understands
				// available to query via
				// declare variable $foo as node() external;
				// or
				// declare variable $foo as node()* external;
			}
			else if (value instanceof Nodes) {
				Nodes nodes = (Nodes) value;
				int size = nodes.size();
				ArrayList sources = new ArrayList(size);
				for (int i = 0; i < size; i++) {
					sources.add(wrap(nodes.get(i), docWrappers));
				}
				value = sources;
			}
			else if (value instanceof Node[]) {
				Node[] nodes = (Node[]) value;
				int size = nodes.length;
				ArrayList sources = new ArrayList(size);
				for (int i = 0; i < size; i++) { 
					sources.add(wrap(nodes[i], docWrappers));
				}
				value = sources;
			}
			else if (value instanceof File || value instanceof File[]) {
				// undocumented feature (temporary?): build it with XOM
				if (value instanceof File) 
					value = new File[] { (File) value };
				File[] files = (File[]) value;
				ArrayList sources = new ArrayList(files.length);
				Builder builder = BuilderPool.GLOBAL_POOL.getBuilder(false);
				for (int i = 0; i < files.length; i++) {
					if (!files[i].isDirectory()) {
						Document doc;
						try {
							doc = builder.build(files[i]);
						} catch (Exception e) {
							throw new TransformerException(e);
						}
						
						sources.add(wrap(doc, docWrappers));
					}
				}
				value = sources;
			}
			else if (value instanceof InputSource || value instanceof InputSource[]) {
				// undocumented feature (temporary?)
				// build it with Saxon's native tree model (tinytree by default)
				// WARNING: Saxon's error messages do not well indicate what kind of 
				// well-formedness or validity problem a bad file has - if the file 
				// is bad you are left without much clue.
				if (value instanceof InputSource) 
					value = new InputSource[] { (InputSource) value };
				InputSource[] files = (InputSource[]) value;
				ArrayList sources = new ArrayList(files.length);
				for (int i = 0; i < files.length; i++) {
					sources.add(new SAXSource(
						getStaticContext().getConfiguration().getSourceParser(), files[i]));
				}
				value = sources;
			}
			
			dynamicContext.setParameter(name, value);
		}
	}
	
	private StaticQueryContext getStaticContext() {
		try {
			return getStaticContext84(); // saxon >= 8.4
		} catch (Error e) { 
			return this.staticContext; // saxon < 8.4
		}
	}	
	
	private StaticQueryContext getStaticContext84() {
		return this.expression.getStaticContext();
	}		

		
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
		
	private static final class DefaultErrorListener implements ErrorListener {
		private static final boolean WARN = 
			XQueryUtil.getSystemProperty("nux.xom.xquery.XQuery.warn", true);
		
		public void error(TransformerException e) throws TransformerException {
			throw e;
		}
		public void fatalError(TransformerException e) throws TransformerException {
			throw e;
		}
		public void warning(TransformerException e) throws TransformerException {
			if (WARN) System.err.println("Warning: " + e);
		}
	}
	
	/** Thread-safe yet efficient impl. using a non-validating XOM Builder */
	private static final class DefaultDocumentURIResolver implements DocumentURIResolver {
		
		private final Configuration config;
		
		private DefaultDocumentURIResolver(Configuration config) {
			this.config = config;
		}
		
		public Document resolve(String href, String baseURI) 
			throws ParsingException, IOException, TransformerException {
			
			String systemID = new net.sf.saxon.StandardURIResolver(config).
				resolve(href, baseURI).getSystemId();
			if (DEBUG) {
				System.err.println("href="+ href);
				System.err.println("baseURI="+ baseURI);
				System.err.println("systemID="+ systemID);
			}
			if (systemID != null && systemID.endsWith(".bnux")) {
				return new DocumentFactory().getBinaryXMLFactory().
					createDocument(null, URI.create(systemID));
			}
			return BuilderPool.GLOBAL_POOL.getBuilder(false).build(systemID);
		}
	}
		
}
