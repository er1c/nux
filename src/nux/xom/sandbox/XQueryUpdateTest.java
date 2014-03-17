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
import nu.xom.Node;
import nux.xom.pool.XOMUtil;
import nux.xom.xquery.XQueryUtil;

/**
 * Usecase examples for XQuery/XPath update facility.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.1 $, $Date: 2005/11/11 23:18:28 $
 */
public class XQueryUpdateTest {
		
	private int count = 0;

	private XQueryUpdateTest() {} // not instantiable
			
	/** runs the test */
	public static void main(String[] args) throws Exception {
		new XQueryUpdateTest().run(args);
	}
	
	private void run(String[] args) throws Exception {
		Document doc = new Builder().build(new File("samples/data/articles.xml"));
		System.out.println("input=\n" + XOMUtil.toPrettyXML(doc));
//		update(doc, "//article[@name='chair']", "(. , ., .)", "make copies");
		update(doc, "//node() | //@*", ".", "identity transform");
		update(doc, "//@*", "()", "delete all attributes");
		update(doc, "//article[@name='chair']", "()", "delete all chairs");
		update(doc, "//article", "if (@name='chair') then () else .", "delete all chairs (equivalent)");
		update(doc, "//article/@onStock[../@name='chair']", "()", "delete stock info of chairs");
		update(doc, "//article[@name='chair']/prize", "9.5", "set prize of chair to 9.5");
		update(doc, "//article[@name='chair']/@onStock", "0", "set attribute value to 0");
		update(doc, "//article[@name='chair']/@onStock", ". + 50", "increment attribute value by 50");
		update(doc, "//article/prize", ". * 0.95", "make all articles a bit cheaper");
		update(doc, "//article[1]", "(//article[2], .)", "move article position: move second article before first article");
		update(doc, "/articles/article[last()]", "(., <foo/>, <article name='sofa' onStock='40'> <prize>30.0</prize> <quantity>500</quantity> </article>)", "append some new elements at end");
		update(doc, "//@onStock", "attribute {'availability'} { string(.) }", "rename an attribute");
		update(doc, "//article/prize", "<price>{string(.)}</price>", "rename an element");
		update(doc, "//article/prize", "attribute {'price'} {string(.)}", "turn an element into an attribute");
		update(doc, "/*", "(comment {'database of articles on stock'}, ., comment {'end of database'} )", "add comments before and after root element");
		update(doc, "//article", "prize", "delete all articles, only retaining their prize");
		update(doc, "//article", "<envelope total='{prize * quantity}'>{.}</envelope>", "wrap each article into a new element with a derived attribute");
		update(doc, "//article/*[1]", "(<summary total='{../prize * ../quantity}'></summary>, .)", "add a summary element to each article");
		update(doc, "//article", "(comment {index-of(../*, .)}, .)", "add item number ID comment before each article");
		update(doc, "//article/*[1]", "(., attribute {'id'} {index-of(../../*, ..)} )", "add item number ID attribute to each article");
	}
		
	private void update(Node contextNode, String select, String morpher, String msg) {
		contextNode = contextNode.copy(); // each test is independent of the other tests
		XQueryUtil.update(contextNode, select, morpher);
		System.out.println("\nQ" + count + ": "+ msg + ": update("+ select + ", "+ morpher +")\noutput=\n" + XOMUtil.toPrettyXML(contextNode));
		count++;
	}
}