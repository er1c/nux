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

import javax.xml.stream.XMLInputFactory;

import nu.xom.Document;
import nu.xom.NodeFactory;
import nux.xom.io.StaxUtil;

/**
 * Integration test for StaxUtil.createBuilder().build().
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.18 $, $Date: 2006/04/02 05:59:49 $
 */
public class StaxBuilderTest extends IOTest {	
		
	public static void main(String[] args) throws Throwable {
		System.setProperty("nu.xom.Verifier.checkURI", "false");
			 
		int bugs = 0;
		int k = 0;
		for (int i=0; i < args.length; i++) {
			File[] files = IOTestUtil.listXMLFiles(args[i]);
			for (int j=0; j < files.length; j++, k++) {
				File file = files[j];
				if (bogus(file) || ignore(file) || file.isDirectory()) {
					System.out.println("\n" + k + ": IGNORING " + file + " ...");
					continue;
				}
				
				System.out.println("\n" + k + ": now processing " + file + " ...");
				try {
					new StaxBuilderTest().run(file);
				} catch (Throwable e) {
					bugs++;
					if (FAIL_FAST) throw e;
					System.err.println("\nOopsla:" + k + ":" + bugs + ":" + file + ":");
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("\nNumber of bugs detected: " + bugs);
	}
		
	private void run(File file) throws Exception {
		Document expected = getBuilder().build(file);
		
		NodeFactory factory = new NodeFactory();
		XMLInputFactory staxFactory = createXMLInputFactory();
		Document actual = StaxUtil.createBuilder(staxFactory, factory).build(file);
		
		IOTestUtil.xomAssertEquals(expected, actual);
		IOTestUtil.canonicalAssertEquals(expected, actual);		
	}
	
	//  temporarily ignore tests that do not yet pass
	private static boolean ignore(File xmlFile) {		
		String file = xmlFile.getAbsolutePath();

		// XOM testsuite:
		if (endsWith(file, "/data/xinclude/input/text/EBCDIC.xml")) return true; // Unsupported encoding (EBCDIC)

		
		// W3C XML testsuite:
//		boolean skipWstxBug = false; // nu.xom.IllegalNameException: The Attribute class is not used for namespace declaration attributes.		
//		if (endsWith(file, "atrs/atrs15.xml")) return true;
//		if (endsWith(file, "copy/copy19.xsl")) return true;
//		if (endsWith(file, "copy/copy20.xsl")) return true;
//		if (endsWith(file, "charmap/charmap007.xsl")) return true; // surrogate pair
//		if (endsWith(file, "charmap/charmap010.xsl")) return true; // surrogate pair
//		if (endsWith(file, "expr/expr85.xml")) return true; // surrogate pair
//		if (endsWith(file, "expression/expression012.xml")) return true;
//		if (endsWith(file, "outp/outp59.xml")) return true;
//		if (endsWith(file, "outp/outp59.xsl")) return true;
//		if (endsWith(file, "outp/outp60.xsl")) return true;
//		if (endsWith(file, "outp/outp76.xsl")) return true; // char encoding
//		if (endsWith(file, "outp/outp78.xml")) return true; // NPE
//		if (endsWith(file, "str/str122.xsl")) return true; // char encoding
		
//		if (endsWith(file, "wolf-xom/097.xml")) return true;
//		if (skipWstxBug && endsWith(file, "wolf-xom/dtdtest.xhtml")) return true; // nu.xom.IllegalNameException: The Attribute class is not used for namespace declaration attributes.
//		if (endsWith(file, "wolf-xom/externalDTDtest.xml")) return true;
//		if (endsWith(file, "wolf-xom/nfctests.xml")) return true; // char encoding
//		if (endsWith(file, "wolf-xom/canonical/input/commentbeforeroot.xml")) return true; // fixed DTD attribute
//		if (endsWith(file, "wolf-xom/canonical/input/test3.3.xml")) return true;
//		if (endsWith(file, "wolf-xom/canonical/input/test3.4.xml")) return true;
//		if (endsWith(file, "wolf-xom/canonical/input/test3.5.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/invalid/not-sa/022.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/ext-sa/008.xml")) return true;
//		if (contains(file, "wolf/w3c-xmlconformance/xmltest/valid/ext-sa/0")) return true;
		
//		if (contains(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/044.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/045.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/046.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/052.xml")) return true; //  nu.xom.ParsingException: Illegal character (NULL, unicode 0) encountered: not valid in any context
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/058.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/064.xml")) return true; // char encoding
//		if (endsWith(file, "xmltest/valid/sa/068.xml")) return true; // escaping
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/076.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/080.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/090.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/091.xml")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/094.xml")) return true;
//		if (endsWith(file, "xmltest/valid/sa/096.xml")) return true; // whitespace
//		if (endsWith(file, "xmltest/valid/sa/110.xml")) return true; // whitespace

//		if (contains(file, "wolf/w3c-xmlconformance/xmltest/valid/not-sa/")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/not-sa/020.xml")) return true; // Unrecognized DTD directive '<!ATTLISTddoca1 >'
//		if (endsWith(file, "xmltest/valid/not-sa/003.xml")) return true; // regression: com.ctc.wstx.exc.WstxEOFException: Unexpected end of input block in external DTD subset
//		if (contains(file, "xmlconf/sun/invalid/")) return true;
//		if (contains(file, "xmlconf/sun/invalid/dtd06.xml")) return true; // Undeclared entity 'undefined'.
//		if (contains(file, "xmlconf/sun/invalid/id03.xml")) return true;
//		if (contains(file, "wolf/w3c-xmlconformance/sun/invalid/")) return true;		
//		if (contains(file, "wolf/w3c-xmlconformance/sun/valid/")) return true;
//		if (endsWith(file, "wolf/w3c-xmlconformance/sun/valid/ext02.xml")) return true;		
//		if (contains(file, "wolf/w3c-xmlconformance/japanese/")) return true;

//		if (endsWith(file, "eduni/errata-2e/E2a.xml")) return true; // WstxParsingException: Attribute 'bar' (of element <foo>): Duplicate enumeration value 'one'
//		if (endsWith(file, "eduni/errata-2e/E2b.xml")) return true; // WstxParsingException: Attribute 'bar' (of element <foo>): Duplicate enumeration value 'one'
//		if (contains(file, "ibm/invalid/")) return true; // similar pattern
		// recent wstx regression:
//		if (endsWith(file, "xmlconf/ibm/invalid/P32/ibm32i01.xml")) return true; // standalone="yes" ???
//		if (endsWith(file, "xmlconf/ibm/invalid/P32/ibm32i03.xml")) return true; // standalone="yes" ???
//		if (endsWith(file, "xmlconf/sun/invalid/not-sa02.xml")) return true; // unparsed entity whitespace?
//		if (endsWith(file, "xmlconf/sun/invalid/not-sa04.xml")) return true; // standalone="yes" ???
//		if (endsWith(file, "xmlconf/sun/invalid/not-sa05.xml")) return true; // standalone="yes" ???
//		if (endsWith(file, "xmlconf/sun/invalid/not-sa06.xml")) return true; // 
//		if (contains(file, "xmlconf/sun/invalid/not-sa")) return true; // 
		
//		if (endsWith(file, "ibm/invalid/P45/ibm45i01.xml")) return true; // WstxParsingException: Trying to redefine element "not_unique" 
//		if (endsWith(file, "ibm/invalid/P51/ibm51i03.xml")) return true; // WstxParsingException: Element <e>): duplicate child element <a> in mixed content model
//		if (endsWith(file, "ibm/invalid/P56/ibm56i03.xml")) return true; // WstxParsingException: Attribute 'UniqueName' (of element <tokenizer>): has type ID; can not have a default (or #FIXED) value (XML 1.0/#3.3.1)
//		if (endsWith(file, "ibm/invalid/P56/ibm56i06.xml")) return true; // WstxParsingException: Attribute 'UniqueName' (of element <tokenizer>): has type ID; can not have a default (or #FIXED) value (XML 1.0/#3.3.1)
//		if (contains(file, "ibm/invalid/P68")) return true; // undeclared entity		
//		if (contains(file, "ibm/invalid/P69")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P68/ibm68i01.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P68/ibm68i02.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P68/ibm68i03.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P68/ibm68i04.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P69/ibm69i01.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P69/ibm69i02.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P69/ibm69i03.xml")) return true; // undeclared entity
//		if (endsWith(file, "ibm/invalid/P69/ibm69i04.xml")) return true; // undeclared entity	 
//		if (contains(file, "ibm/invalid/P56")) return true; // similar pattern
//		if (contains(file, "ibm/valid/P12/ibm12v01.xml")) return true; // missing PublicID
//		if (contains(file, "ibm/valid/P12/ibm12v02.xml")) return true; // missing PublicID
//		if (contains(file, "ibm/valid/P13/ibm13v01.xml")) return true; // nu.xom.IllegalDataException: Trailing white space in public IDs is not round trippable
//		if (contains(file, "ibm/valid/P85/out/ibm85v01.xml")) return true; // WstxUnexpectedCharException: Unexpected character '?' (code 246)excepted either space or "?>" after PI target
//		if (endsWith(file, "oasis/e2.xml")) return true; // WstxParsingException: Attribute 'at' (of element <el>): Duplicate enumeration value 'two'
//		if (endsWith(file, "oasis/p07pass1.xml")) return true; // WstxUnexpectedCharException: Unexpected character ':' (code 58) in internal DTD subset; expected a NMTOKEN character to start a NMTOKEN
		
		// regression Feb 22 1am:
//		if (endsWith(file, "xmlconf/xmlconf/ibm/valid/P09/ibm09v05.xml")) return true; // whitespace escaping 
		
		
		// with "don't swallow exception" fix:
//		if (endsWith(file, "xmltest/valid/sa/089.xml")) return true; // nu.xom.IllegalDataException: Malformed internal DTD subset: The character reference must end with the ';' delimiter.
		
		// feedvalidator testsuite:
//		if (skipWstxBug && endsWith(file, "atom/must/feed_missing2.xml")) return true; // nu.xom.IllegalNameException: The Attribute class is not used for namespace declaration attributes.
//		if (endsWith(file, "rss/must/doctype.xml")) return true; // systemID suppressed
//		if (endsWith(file, "rss/must/doctype_not_entity.xml")) return true; // systemID suppressed
//		if (endsWith(file, "rss/must/doctype_wrong_version.xml")) return true; // systemID suppressed
//		if (skipWstxBug && endsWith(file, "rss/must/missing_rss2.xml")) return true; // nu.xom.IllegalNameException: The Attribute class is not used for namespace declaration attributes.
		
		
		// feedparser testsuite:
//		if (endsWith(file, "feedparser/tests/wellformed/rdf/doctype_contains_entity_decl.xml")) return true; // missing comments in internal DTD subset?
		
		
		// Apache Batik SVG testsuite:
//		if (endsWith(file, "tests/spec/scripting/bbox.svg")) return true; // com.ctc.wstx.exc.WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type 		
//		if (endsWith(file, "tests/spec/scripting/boundsTransformChange.svg")) return true; // com.ctc.wstx.exc.WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type 		
//		if (endsWith(file, "tests/spec/scripting/circle.svg")) return true; // com.ctc.wstx.exc.WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type 		
//		if (endsWith(file, "batik-1.6/samples/moonPhases.svg")) return true; // xerces infinite loop?		
		
		
		// W3C XSLT testsuite:
//		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/Plants.xml")) return true; // missing xsl namespace
////		if (endsWith(file, "MSFT_Conformance_Tests/BVTs/out-html.xsl")) return true; // com.ctc.wstx.exc.WstxUnexpectedCharException: Unexpected character '?' (code 254) excepted space, or '>' or "/>"
//		if (endsWith(file, "MSFT_Conformance_Tests/Completeness/plants.xml")) return true; // missing xsl namespace
//		if (endsWith(file, "MSFT_Conformance_Tests/Elements/plants.xml")) return true; // missing xsl namespace
//		if (endsWith(file, "MSFT_Conformance_Tests/Namespace/plants.xml")) return true; // missing xsl namespace
//		if (endsWith(file, "MSFT_Conformance_Tests/Namespace/plantsW.xml")) return true; // missing xsl namespace
		
//		if (endsWith(file, "MSFT_Conformance_Tests/Keys/input.xml")) return true; // xerces-2.7.1 performance degradation! large PCDATA blocks!?

		
		// W3C XInclude testsuite:
		if (endsWith(file, "XInclude-Test-Suite/Harold/test/text/EBCDIC.xml")) return true; // unsupported encoding (EBCDIC)
//		if (endsWith(file, "XInclude-Test-Suite/EdUni/test/book.xml")) return true; // <!ATTLIST include xmlns CDATA #FIXED "http://www.w3.org/2001/XInclude">   OR   nu.xom.NamespaceConflictException: Additional namespace http://www.w3.org/2001/XInclude conflicts with existing default namespace at nu.xom.NamespacesInScope.checkNamespaceConflict(NamespacesInScope.java:163)
//		if (endsWith(file, "XInclude-Test-Suite/EdUni/test/extract.xml")) return true; // <!ATTLIST include xmlns CDATA #FIXED "http://www.w3.org/2001/XInclude">   OR   nu.xom.NamespaceConflictException: Additional namespace http://www.w3.org/2001/XInclude conflicts with existing default namespace at nu.xom.NamespacesInScope.checkNamespaceConflict(NamespacesInScope.java:163)
//		if (endsWith(file, "XInclude-Test-Suite/Harold/test/text/EBCDIC.xml")) return true; // unsupported encoding
//		if (endsWith(file, "XInclude-Test-Suite/Harold/test/text/UTF32BE.xml")) return true; // unsupported encoding
//		if (endsWith(file, "XInclude-Test-Suite/Harold/test/text/UTF32LE.xml")) return true; // unsupported encoding
		
		
		// XOM testsuite:
//		if (endsWith(file, "xinclude/input/text/UTF32BE.xml")) return true; // unsupported encoding
//		if (endsWith(file, "xinclude/input/text/UTF32LE.xml")) return true; // unsupported encoding
		
		
		// W3C WS testsuite:
//		if (endsWith(file, "2002/ws/desc/primer/wsdl-primer-20020527.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/primer/wsdl-primer-2002xxxx.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/requirements/ws-desc-reqs.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/schema-patterns/xml-schema-patterns.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/wsdl12/wsdl12-bindings.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/wsdl12/wsdl12-fandp.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/wsdl12/wsdl12-patterns.xml")) return true; // missing xlink namespace
//		
//		if (endsWith(file, "2002/ws/desc/wsdl20/altschemalangs.xml")) return true; // missing namespace???
//		if (endsWith(file, "2002/ws/desc/wsdl20/wsdl20-adjuncts.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/wsdl20/wsdl20-primer.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2002/ws/desc/wsdl20/wsdl20.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2004/ws/addressing/ws-addr-core.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2004/ws/addressing/ws-addr-soap.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2004/ws/addressing/ws-addr-wsdl.xml")) return true; // missing xlink namespace
		
		// spurious failures: 
//		if (endsWith(file, "2004/ws/addressing/ws-addr-testsuite.xml")) return true; // missing xlink namespace
//		if (endsWith(file, "2004/ws/addressing/ws-addressing.xml")) return true; // missing xlink namespace
		

		// libxml2 testsuite:
//		if (endsWith(file, "test/defattr.xml")) return true; // WstxValidationException: Unbound namespace prefix 'tst' for default attribute tst:att
//		if (endsWith(file, "test/defattr2.xml")) return true; // WstxValidationException: Unbound namespace prefix 'tst' for default attribute tst:att
//		if (endsWith(file, "test/SVG/4rects.xml")) return true; // missing namespace
//		if (contains(file, "/test/SVG/")) return true; // same pattern
//		if (endsWith(file, "test/valid/127772.xml")) return true; // undeclared prefix
//		if (endsWith(file, "test/valid/ns.xml")) return true; // undeclared prefix
//		if (endsWith(file, "test/valid/t4.xml")) return true; // Undeclared entity (GE from def. attr. value)
//		if (endsWith(file, "test/valid/t4a.xml")) return true; // Undeclared entity (GE from def. attr. value)
//		if (endsWith(file, "test/valid/t6.xml")) return true; // Undeclared entity (GE from def. attr. value)
		
		
		// libxslt testsuite:
//		if (endsWith(file, "libxslt-1.1.15/doc/xsltproc.xml")) return true; // missing namespace
//		if (endsWith(file, "libxslt-1.1.15/tests/docbook/test/book2.xml")) return true; // whitespace
//		if (endsWith(file, "libxslt-1.1.15/tests/docbook/test/condition.xml")) return true; // whitespace
//		if (endsWith(file, "libxslt-1.1.15/tests/xmlspec/REC-xml-20001006.xml")) return true; //  Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "libxslt-1.1.15/win32/defgen.xsl")) return true; // whitespace
		
		
		// mathml testsuite:
//		if (contains(file, "/testsuite/Characters/EntityNames/")) return true; // missing namespace
//		if (contains(file, "/testsuite/Characters/NumericRefs/")) return true; // missing namespace
//		if (contains(file, "/testsuite/Characters/UTF8/")) return true; // missing namespace
//		if (endsWith(file, "Characters/EntityNames/a.xml")) return true; // missing namespace
		
		
		// smil testsuite:
//		if (contains(file, "SMIL21-testsuite-2006/interop2/animation/")) return true; // see below
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-add-BE-09.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-extRef-image1.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-extRef-image2.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-extRef-image3.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type

		
		// ws-core testsuite:
//		if (endsWith(file, "ws-core-4.0.1/java/common/source/share/tomcat/web.xml")) return true; // nu.xom.IllegalDataException: The character 0xa is not allowed in public IDs
		
		
		// cocoon testsuite:
//		if (endsWith(file, "cocoon-2.1.8/src/blocks/faces/java/META-INF/faces-config.xml")) return true; // missing namespace
//		if (endsWith(file, "cocoon-2.1.8/src/blocks/faces/WEB-INF/faces-config.xml")) return true; // missing namespace
		
		
		// svg11 testsuite:
		// spurious failures: 		
//		if (endsWith(file, "svg11/svg/interact-cursor-01-f.svg")) return true; // Tatu: public-id/system-id mismatch
//		
//		if (endsWith(file, "svg11/svg/color-prof-01-f.svg")) return true; // ??? attribute ???
//		if (endsWith(file, "svg11/svg/render-elems-08-t.svg")) return true; // ??? attribute ???
//		if (endsWith(file, "svg11/svg/color-prop-02-f.svg")) return true; // ??? spurious mismatch ???


		// BuilderPool with xerces-2.7.1 and parser.setProperty("http://apache.org/xml/properties/internal/grammar-pool", ClassLoaderUtil.newInstance(clazz)):
//		if (endsWith(file, "feedvalidator/testcases/rss/must/missing_rss2.xml")) return true; 
//		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/22-3-element_name.xsl")) return true; 
//		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/22-3-pi_name.xsl")) return true; 
//		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/22-3-repeat_for.xsl")) return true; 
//		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/22-3-use-templates_for.xsl")) return true; 
//		if (endsWith(file, "MSFT_Conformance_Tests/Elements/ek-b-use-templates.xsl")) return true; 
//		if (endsWith(file, "MSFT_Conformance_Tests/Keys/91860.xml")) return true; 
//		if (endsWith(file, "Xalan_Conformance_Tests/copy/copy20.xsl")) return true; 
//		if (endsWith(file, "xmlconf/eduni/namespaces/1.1/rmt-ns11.xml")) return true; 
//		if (endsWith(file, "xmlconf/ibm/valid/P11/ibm11v04.xml")) return true; 
//		if (endsWith(file, "xmlconf/japanese/pr-xml-iso-2022-jp.xml")) return true; 
//		if (endsWith(file, "xmlconf/japanese/pr-xml-euc-jp.xml")) return true; 
//		if (endsWith(file, "xmlconf/japanese/pr-xml-shift_jis.xml")) return true; 
//		if (endsWith(file, "xmlconf/japanese/pr-xml-utf-8.xml")) return true; 
//		if (endsWith(file, "xmlconf/japanese/pr-xml-utf-16.xml")) return true;
//		if (endsWith(file, "xmlconf/oasis/p08pass1.xml")) return true;
//		if (endsWith(file, "xmlconf/oasis/p52pass1.xml")) return true;
//		if (endsWith(file, "xmlconf/oasis/p60pass1.xml")) return true;
//		if (endsWith(file, "xmlconf/sun/valid/not-sa03.xml")) return true;
//		if (endsWith(file, "xmlconf/sun/valid/not-sa04.xml")) return true;
//		if (endsWith(file, "xmlconf/sun/valid/sa04.xml")) return true;
//		if (endsWith(file, "xmlconf/sun/valid/sgml01.xml")) return true;
//		if (endsWith(file, "xmlconf/xmltest/valid/ext-sa/013.xml")) return true;
//		if (endsWith(file, "xmlconf/xmltest/valid/not-sa/011.xml")) return true;
//		if (endsWith(file, "xmlconf/xmltest/valid/not-sa/012.xml")) return true;
//		if (endsWith(file, "xmlconf/xmltest/valid/not-sa/026.xml")) return true;
//		if (contains(file, "xmlconf/xmltest/valid/sa/")) return true;
//		if (endsWith(file, "xmlconf/xmltest/valid/sa/001.xml")) return true;
//		if (endsWith(file, "xmlconf/xmltest/valid/sa/002.xml")) return true;
//		if (endsWith(file, "xmlconf/xmlconf.xml")) return true;
//		if (endsWith(file, "xom/data/097.xml")) return true;
		
		return false;
	}

	private static XMLInputFactory createXMLInputFactory() {
		String clazz = System.getProperty("javax.xml.stream.XMLInputFactory");
		if (clazz == null) return null; // use Nux default
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		System.out.println("staxFactory=" + staxFactory.getClass().getName());
		return staxFactory;
	}
	
}
