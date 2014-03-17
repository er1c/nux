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
import nu.xom.NodeFactory;
import nux.xom.pool.BuilderPool;

/**
 * Base class for parsing/serialization tests.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek $
 * @version $Revision: 1.16 $, $Date: 2006/06/19 04:37:48 $
 */
abstract class IOTest {

	private static final boolean ENABLE_BUILDER_POOL = 
		IOTestUtil.getSystemProperty("nux.xom.sandbox.IOTest.enableBuilderPool", false);

	protected static final boolean FAIL_FAST = 
		IOTestUtil.getSystemProperty("nux.xom.sandbox.IOTest.failFast", true);

	
	// not wellformed XML or not XML at all
	protected static boolean bogus(File xmlFile) {
		String file = xmlFile.getAbsolutePath();
		
		// non-XML stuff:		
		if (endsWith(file, ".html")) return true;
//		if (endsWith(file, ".htm")) return true;
		if (endsWith(file, ".out")) return true;
		if (endsWith(file, ".txt")) return true;
		if (endsWith(file, ".bat")) return true;
		if (endsWith(file, ".sh")) return true;
		if (endsWith(file, ".sxx")) return true;
		if (endsWith(file, ".ssx")) return true;
		if (endsWith(file, ".dtd")) return true;
		if (endsWith(file, ".xq")) return true;
		if (endsWith(file, ".zip")) return true;
		if (endsWith(file, ".ZIP")) return true;
		if (endsWith(file, ".tar")) return true;
		if (endsWith(file, ".gz")) return true;
		if (endsWith(file, ".jpg")) return true;
		if (endsWith(file, ".gif")) return true;
		if (endsWith(file, ".dsl")) return true;
		
		
		// sxn test suite:
		if (endsWith(file, "nspc/nspc05x.xml")) return true; // not wellformed
		if (endsWith(file, "copy/ent22.xml")) return true; // not wellformed
		if (endsWith(file, "exslt/2")) return true; // not wellformed

		if (contains(file, "err.")) return true;
		if (contains(file, "ERR.")) return true;
		if (contains(file, "Untitled")) return true;
		
		
		// XOM test suite:
//		if (endsWith(file, "CVS")) return true; // CVS messes up char encoding
		if (endsWith(file, "data/xinclude/input/ucs4bigendian.xml")) return true; // CVS messes up char encoding
		if (endsWith(file, "data/xinclude/input/ucs4littleendian.xml")) return true; // CVS messes up char encoding
		if (endsWith(file, "data/xinclude/input/utf8.xml")) return true; // CVS messes up char encoding
////		if (endsWith(file, "097.ent")) return true; // CVS messes up char encoding
		if (endsWith(file, "/data/test_ebcdic.xml")) return true; // CVS messes up char encoding
////		if (endsWith(file, "xinclude/input/utf8bom.xml")) return true; // crimson can't handle BOMs
////		if (endsWith(file, "xinclude/output/langtest3.xml")) return true; // crimson bug
//		if (endsWith(file, "xinclude/input/text/EBCDIC.xml")) return true; // CVS messes up char encoding
		if (endsWith(file, "/data/xmlid/tests/009_ok11.xml")) return true; // XML version "1.1" is not supported

		// 2 cases where XOM with trusted xerces builder fails to detect xml:id value invalidity due to missing checkNCName() check in XOM Attribute.build():
		// fixed in xom-1.2-CVS
//		if (endsWith(file, "/data/xmlid/tests/001_normalize.xml")) return true; // NCNames cannot start with the character 20
//		if (endsWith(file, "/data/xmlid/tests/011_oknormalize.xml")) return true; // NCNames cannot start with the character 20

		
		// W3C XML test suite:
		if (endsWith(file, ".ent")) return true; 
		if (endsWith(file, "valid/sa/012.xml")) return true; // Attribute name "null" associated with an element type "doc" must be followed by the ' = ' character
//		if (endsWith(file, "valid/sa/066.xml")) return true; // xerces-2.6.2 internal subset
//		if (endsWith(file, "valid/sa/101.xml")) return true; // xerces-2.6.2 internal subset
//		if (endsWith(file, "valid/sa/090.xml")) return true; // crimson internal subset
		if (contains(file, "fail")) return true;
		if (contains(file, "eduni/namespaces/1.0/")) return true;
		if (endsWith(file, "eduni/namespaces/1.0/009.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/010.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/011.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/012.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/013.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/014.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/015.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/016.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/023.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/025.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/026.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/029.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/030.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.0/031.xml")) return true; // not wellformed
		if (contains(file, "eduni/namespaces/1.1/")) return true;
		if (endsWith(file, "eduni/namespaces/1.1/001.xml")) return true; // not wellformed
		if (endsWith(file, "eduni/namespaces/1.1/005.xml")) return true; // not wellformed
		if (contains(file, "eduni/xml-1.1/")) return true;
		if (endsWith(file, "eduni/errata-2e/E27.xml")) return true; // invalid char encoding
		if (endsWith(file, "eduni/errata-2e/E38.xml")) return true; // XML 1.1
		if (endsWith(file, "eduni/errata-2e/E50.xml")) return true; // XML 1.1
		if (endsWith(file, "eduni/errata-2e/E61.xml")) return true; // not wellformed
		if (endsWith(file, "ibm/invalid/P49/ibm49i02.xml")) return true; // file not found
		if (endsWith(file, "ibm/invalid/P49/out/ibm49i02.xml")) return true; // premature end of file
		if (contains(file, "/ibm/not-wf/")) return true; // not wellformed
		if (contains(file, "/ibm/xml-1.1/")) return true;
		if (endsWith(file, "oasis/p04pass1.xml")) return true; 
		if (endsWith(file, "oasis/p05pass1.xml")) return true; // not wellformed
		if (endsWith(file, "sun/sun-invalid.xml")) return true; // not wellformed
		if (endsWith(file, "sun/sun-not-wf.xml")) return true; // not wellformed
		if (endsWith(file, "sun/sun-valid.xml")) return true; // not wellformed
		if (contains(file, "/sun/not-wf/")) return true; // not wellformed
		if (contains(file, "/sun/xml-1.1/")) return true; // not wellformed
		if (contains(file, "/xmltest/not-wf")) return true; // not wellformed
		if (endsWith(file, "xmltest/valid/sa/out/012.xml")) return true; // not wellformed
		
		
		// W3C WS testsuite:
		if (endsWith(file, "2004/ws/addressing/acknowledgements.xml")) return true; // The entity "acknowledgements-current" was referenced, but not declared
		
		
		// Apache Batik SVG testsuite:
//		if (contains(file, "tests/spec/color/")) return true; // more crimson bugs
//		if (contains(file, "tests/spec/coordinates/")) return true;
//		if (contains(file, "tests/spec/filters/")) return true;
//		if (contains(file, "tests/spec/fonts/")) return true;
						
		
		// svg11 testsuite:
		if (endsWith(file, "svg11/images/shapes-ellipse-01-b.svg")) return true; // file not found
		if (endsWith(file, "svg11/images/shapes-rect-01-b.svg")) return true; // file not found
		if (endsWith(file, "svg11/images/struct-frag-01-B.svg")) return true; // file not found
		
		
		// feedvalidator testsuite:
		if (endsWith(file, "3.1.1.3/xhtml_named_entity.xml")) return true; // undefined external xhtml entity
		if (endsWith(file, "atom/must/feed_copyright_is_inline.xml")) return true; // not wellformed XML
		if (endsWith(file, "atom/must/feed_copyright_is_inline_2.xml")) return true; // not wellformed XML
		if (endsWith(file, "atom/must/feed_missing.xml")) return true; // not wellformed XML
		if (endsWith(file, "atom/must/feed_namespace_missing_dc.xml")) return true; // namespace not wellformed XML
		if (endsWith(file, "testcases/atom/must/invalid_xml.xml")) return true; // not wellformed XML
		if (endsWith(file, "opml/errors/invalidCharacters.opml")) return true; 
		if (endsWith(file, "opml/errors/notEncoded.opml")) return true; 
		if (endsWith(file, "opml/errors/notwellformed.opml")) return true; 
		if (endsWith(file, "rss/must/invalid_xml.xml")) return true; 
		if (endsWith(file, "rss/must/missing_namespace.xml")) return true; 
		if (endsWith(file, "rss/must/missing_namespace2.xml")) return true; 
		if (endsWith(file, "rss/must/missing_namespace_attr_only.xml")) return true; 
		if (endsWith(file, "rss/must/rss91u_entity.xml")) return true; 	// entity referenced but not declared	
		if (endsWith(file, "rss/must/xmlversion_11.xml")) return true; 
		if (endsWith(file, "rss/should/system_entity.xml")) return true; // file not found
		if (endsWith(file, "rss/should/system_entity_http.xml")) return true; // connection refused
		if (endsWith(file, "rss/should/xml_obscure_encoding.xml")) return true;
		if (endsWith(file, "xml/must/invalid_namespace_prefix_attribute.xml")) return true;
		if (endsWith(file, "xml/must/invalid_namespace_prefix_element.xml")) return true;
		if (endsWith(file, "xml/must/xml_declares_unknown_encoding.xml")) return true; 
		if (endsWith(file, "xml/must/xml_declares_wrong_encoding.xml")) return true; 				
		
		
		// feedparser testsuite:
		if (endsWith(file, "wellformed/encoding/csucs4.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/csunicode.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/http_text_xml_charset_2.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/iso-10646-ucs-4.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/no_content_type_encoding.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/u16.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/no_content_type_default.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/ucs-2.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/ucs-4.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-16le-autodetect.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-16le-bom.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-32be-autodetect.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-32be-bom.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-32be.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-32le-autodetect.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-32le-bom.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf-32le.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf16.xml")) return true; // unknown encoding 		
		if (endsWith(file, "wellformed/encoding/utf_32.xml")) return true; // unknown encoding 		
		if (contains(file, "wellformed/encoding/x80")) return true; // unknown encoding 		
		
		
		// W3C XSLT testsuite:
		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/domtest1.xml")) return true; // redefining xml prefix
		if (endsWith(file, "MSFT_Conformance_Tests/Attributes/xslt_attribute_XmlnsExplicitAsAttributeNs.xsl")) return true; // redefining xmlns prefix
//		if (endsWith(file, "MSFT_Conformance_Tests/AVTs/XSLT17106.xsl")) return true; // redefining xmlns prefix
		if (endsWith(file, "MSFT_Conformance_Tests/AVTs/XSLT17107.xsl")) return true; // redefining xmlns prefix
		if (endsWith(file, "MSFT_Conformance_Tests/BVTs/inc/dtd-notfound.xml")) return true; // premature EOF
		if (endsWith(file, "MSFT_Conformance_Tests/ConflictResolution/XSLT08009.xsl")) return true; // content not allowed in prolog
		if (endsWith(file, "MSFT_Conformance_Tests/Import/91146a.xsl")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Include/xslt03011a.xsl")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Include/xslt_include_ParentIncChildContext2.xsl")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Keys/91836a.xml")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Keys/91836b.xml")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Miscellaneous/bug46988.xml")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Namespace_XPath/namespace_xpath_6.xsl")) return true; // redefining xml prefix
		if (endsWith(file, "MSFT_Conformance_Tests/Namespace_XPath/prefixes.xml")) return true; // redefining xml prefix
		if (endsWith(file, "MSFT_Conformance_Tests/ProcessingInstruction/XSLT16001.xsl")) return true; // java.net.UnknownHostException: webxtest
		if (endsWith(file, "MSFT_Conformance_Tests/ProcessingInstruction/XSLT16002.xsl")) return true; // java.net.UnknownHostException: webxtest
		if (endsWith(file, "MSFT_Conformance_Tests/ProcessingInstruction/XSLT16004.xsl")) return true; // java.net.UnknownHostException: webxtest
		if (endsWith(file, "MSFT_Conformance_Tests/ProcessingInstruction/XSLT16007.xsl")) return true; // java.net.UnknownHostException: webxtest
		if (endsWith(file, "MSFT_Conformance_Tests/Sorting/2_5_16_repeat.xsl")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Sorting/2_5_16_use-templates.xsl")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Sorting/2_5_5_use-templates.xsl")) return true; // not wellformed
		if (endsWith(file, "MSFT_Conformance_Tests/Sorting/plants.xml")) return true; // file not found
		if (endsWith(file, "MSFT_Conformance_Tests/Sorting/xslt19012.xsl")) return true; // redefining xml prefix
		if (endsWith(file, "MSFT_Conformance_Tests/Stylesheet/xslt_stylesheet_XmlnsNsOnStylesheet.xsl")) return true; // redefining xmlns prefix
		if (endsWith(file, "MSFT_Conformance_Tests/Text/XSLT04002.xsl")) return true; // redefining xml prefix
		if (endsWith(file, "MSFT_Conformance_Tests/Text/XSLT04104.xsl")) return true; // redefining xml prefix
		if (endsWith(file, "MSFT_Conformance_Tests/Text/XSLT04115.xsl")) return true; // redefining xml prefix
		
		if (endsWith(file, "Xalan_Conformance_Tests/output/wml_11.xml")) return true; // not wellformed
//		if (endsWith(file, "Xalan_Conformance_Tests/namespace/namespace110.xsl")) return true; // Element type "xsl:element" must be followed by either attribute specifications, ">" or "/>"

		/*
		CPU SAMPLES BEGIN (total = 1469) Tue Feb  7 14:52:22 2006
		rank   self  accum   count trace method
		   1 53.44% 53.44%     785 300219 org.apache.xerces.impl.XMLScanner.scanComment
		   2 44.38% 97.82%     652 300217 org.apache.xerces.util.XMLStringBuffer.append
		   
		parses 1 MB in 30 secs --> only 33 KB/s throughput
		*/
//		if (endsWith(file, "MSFT_Conformance_Tests/Include/bloated.xsl")) return true; // catastrophic xerces-2.7.1 performance degradation for large PCDATA blocks; fixed in xerces-2.8.0

		
		// W3C XInclude testsuite:
		if (endsWith(file, "XInclude-Test-Suite/Harold/test/ucs4bigendian.xml")) return true; // Content is not allowed in prolog
		if (endsWith(file, "XInclude-Test-Suite/Harold/test/ucs4littleendian.xml")) return true; // Content is not allowed in prolog
		if (endsWith(file, "XInclude-Test-Suite/Harold/test/utf8.xml")) return true; // Content is not allowed in prolog		
		if (endsWith(file, "XInclude-Test-Suite/Nist/result/nist-include-49.xml")) return true; // file not found
		if (endsWith(file, "XInclude-Test-Suite/Nist/test/ents/nwf1.xml")) return true; // not wellformed
		if (endsWith(file, "XInclude-Test-Suite/Nist/test/ents/nwf2.xml")) return true; // not wellformed
		if (endsWith(file, "XInclude-Test-Suite/Nist/test/ents/nwfsomething.xml")) return true; // not wellformed
		if (endsWith(file, "XInclude-Test-Suite/Nist/test/ents/part1.xml")) return true; // prefix not bound
		
		
		// W3C WS testsuite:
		if (endsWith(file, "2002/ws/desc/tools/diffmk.xml")) return true; // file not found		
		if (endsWith(file, "2002/ws/desc/media-types/xml-media-types.xml")) return true; // skipped entity
		if (endsWith(file, "2002/ws/desc/wsdl20/adjuncts-assertion-summary.xml")) return true; // entity referenced but not declared
		if (endsWith(file, "2002/ws/desc/wsdl20/assertion-summary.xml")) return true; // entity referenced but not declared
		if (endsWith(file, "2002/ws/desc/wsdl20/status-adjuncts.xml")) return true; // entity referenced but not declared
		if (endsWith(file, "2002/ws/desc/wsdl20/status-primer.xml")) return true; // entity referenced but not declared
		if (contains(file, "2002/ws/desc/wsdl20/status")) return true; // entity referenced but not declared
		if (endsWith(file, "2002/ws/desc/wsdl20/wsdl20-soap11-binding.xml")) return true; // Could not resolve entity document.status.soap11
		if (endsWith(file, "2004/ws/addressing/Makefile.wsdl")) return true; // not wellformed
		if (endsWith(file, "2004/ws/addressing/status.xml")) return true; // entity referenced but not declared
		if (endsWith(file, "2004/ws/addressing/test-cases/Microsoft/2005Mar-0209/request-8.xml")) return true; // missing xenc namespace

		
		// libxml2 testsuite:
		if (endsWith(file, "test/bigentname.xml")) return true; // StringIndexOutOfBounds Exception in Xerces
		if (endsWith(file, "test/utf16bebom.xml")) return true; // encoding
		if (endsWith(file, "test/wap.xml")) return true; // unknown http host
		if (contains(file, "/test/errors/")) return true; // not wellformed
		if (contains(file, "/err_")) return true; // not wellformed
		if (contains(file, "/test/threads/")) return true; // file not found
		if (endsWith(file, "test/valid/t8.xml")) return true; // not wellformed
		if (endsWith(file, "test/valid/t8a.xml")) return true; // not wellformed
//		if (endsWith(file, "test/xmlid/id_err1.xml")) return true; // nu.xom.IllegalNameException: NCNames cannot start with the character 30
		if (endsWith(file, "test/xmlid/id_tst2.xml")) return true; // not wellformed
		if (endsWith(file, "test/xmlid/id_tst3.xml")) return true; // not wellformed
		 			
		
		// libxslt testsuite:
		if (endsWith(file, "libxslt-1.1.15/doc/tutorial2/libxslt_pipes.xml")) return true; // file not found
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/dtd/simple/3.1.7.1/testcust.xml")) return true; // file not found
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/dtd/simple/3.1.7.1/testrefcust.xml")) return true; // file not found
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/dtd/simple/4.1.2.4/testcust.xml")) return true; // file not found
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/dtd/simple/4.1.2.4/testrefcust.xml")) return true; // file not found
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/result/xhtml/book2.xhtml")) return true; // The entity "nbsp" was referenced, but not declared.
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/result/xhtml/condition.xhtml")) return true; // The entity "nbsp" was referenced, but not declared.
		if (endsWith(file, "libxslt-1.1.15/tests/docbook/template/biblioentry.xsl")) return true; // xsl prefix not bound
		if (endsWith(file, "libxslt-1.1.15/tests/documents/result.xhtml")) return true; // file not found
		if (endsWith(file, "libxslt-1.1.15/tests/multiple/result.xml")) return true; // premature eof
		if (endsWith(file, "libxslt-1.1.15/tests/REC2/html.xml")) return true; // not wellformed
		if (endsWith(file, "libxslt-1.1.15/tests/REC2/vrml.xml")) return true; // not wellformed
			
		
		// mathml testsuite:
		if (endsWith(file, "Content/ElementaryFunctions/arccos/arccos3.mml")) return true; // entity referenced but not declared
		if (endsWith(file, "ErrorHandling/BadChildren/emptyContent1.mml")) return true; // not wellformed
		if (endsWith(file, "ErrorHandling/BadEntities/badEntity1.mml")) return true; // entity referenced but not declared
		if (endsWith(file, "ErrorHandling/BadEntities/badEntity1.xml")) return true; // entity referenced but not declared
		if (endsWith(file, "General/Math/mathAdisplay1.mml")) return true; // not wellformed
		if (endsWith(file, "General/Math/mathAdisplay2.mml")) return true; // not wellformed
		if (endsWith(file, "General/Math/mathAmode1.mml")) return true; // not wellformed
		if (endsWith(file, "TortureTests/Complexity/complex3.mml")) return true; // not wellformed
		if (endsWith(file, "TortureTests/Complexity/complex4.mml")) return true; // not wellformed
		if (endsWith(file, "TortureTests/Size/10.mml")) return true; // not wellformed
		if (endsWith(file, "TortureTests/Size/100.mml")) return true; // not wellformed
		if (endsWith(file, "TortureTests/Size/1000.mml")) return true; // not wellformed
		if (endsWith(file, "TortureTests/Size/10000.mml")) return true; // not wellformed
			
		
		// rdf testsuite:
		if (endsWith(file, "rdf-charmod-literals/error001.rdf")) return true; // not wellformed
		if (endsWith(file, "rdf-charmod-literals/error002.rdf")) return true; // not wellformed
		if (endsWith(file, "rdf-containers-syntax-vs-schema/test005.rdf")) return true; // not wellformed
		if (endsWith(file, "xmlbase/test012.rdf")) return true; // not wellformed
		
		
		// smil testsuite:
//		if (contains(file, "SMIL21-testsuite-2006/interop2/animation/")) return true; // see below
		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/copyright-documents-19990405.smil")) return true; // premature EOF
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-add-BE-09.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-extRef-image1.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-extRef-image2.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
//		if (endsWith(file, "SMIL21-testsuite-2006/interop2/animation/animation-extRef-image3.svg")) return true; // WstxValidationException: Unbound namespace prefix 'xlink' for default attribute xlink:type
			
		
		// activemq:
//		if (endsWith(file, "activemq-3.2.2/etc/checkstyle.xml")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed
		if (endsWith(file, "activemq-3.2.2/modules/systest/jmscts/config/log4j.xml")) return true; // file not found
		
//		if (contains(file, "/activemq-3.2.2/")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
//		if (endsWith(file, "/activemq.xml")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
//		if (endsWith(file, "activemq-3.2.2/modules/assembly/src/release/conf/activemq.xml")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
//		if (endsWith(file, "activemq-3.2.2/modules/assembly/src/release/example/conf/activemq-stomp.xml")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
//		if (contains(file, "activemq-3.2.2/modules/assembly/src/sample-conf/")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
//		if (endsWith(file, "activemq-3.2.2/modules/assembly/src/test/org/activemq/config/config.xml")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
				
		  
		// geronimo:		
		if (endsWith(file, "geronimo-1.0-src/applications/console-framework/src/webapp/WEB-INF/data/xml/pageregistrymapping.xml")) return true; // unknown host	
		if (endsWith(file, "geronimo-1.0-src/applications/console-framework/src/webapp/WEB-INF/data/xml/portletdefinitionmapping.xml")) return true; // unknown host	
		if (endsWith(file, "geronimo-1.0-src/applications/console-framework/src/webapp/WEB-INF/data/xml/portletentitymapping.xml")) return true; // unknown host	
		if (endsWith(file, "geronimo-1.0-src/applications/console-framework/src/webapp/WEB-INF/data/xml/servletdefinitionmapping.xml")) return true; // unknown host	
		if (endsWith(file, "geronimo-1.0-src/assemblies/j2ee-installer/src/izpack/izpack-process.xml")) return true; // -- not allowed within comments	
		if (endsWith(file, "geronimo-1.0-src/etc/geronimo_checks.xml")) return true; // not wellformed dtd	
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-builder/src/test-ear13/test-rar/META-INF/geronimo-ra.xml")) return true; // premature EOF	
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-builder/src/test-ear13/test-rar/META-INF/ra.xml")) return true; // premature EOF	
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-schema/src/j2ee_1_2schema/ejb-jar_1_1.xsd")) return true; // prefix not bound	
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-schema/src/j2ee_1_2schema/web-app_2_2.xsd")) return true; // prefix not bound		
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-schema/src/j2ee_1_2schema/web-app_2_3.xsd")) return true; // prefix not bound	
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-schema/src/j2ee_1_3schema/ejb-jar_2_0.xsd")) return true; // prefix not bound
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-schema/src/j2ee_1_3schema/web-app_2_3.xsd")) return true; // prefix not bound	
		if (endsWith(file, "geronimo-1.0-src/modules/j2ee-schema/src/j2ee_1_4schema/xml.xsd")) return true; // file not found	
		if (endsWith(file, "geronimo-1.0-src/modules/webservices/src/java/org/apache/geronimo/webservices/webservices_1_1.xml")) return true; // unknown host			

		
		// xercesj-2.7.1 testsuite:
		if (endsWith(file, "xerces2/tests/dom/dom3/both-error.xml")) return true; // file not found
		if (endsWith(file, "xerces2/tests/dom/dom3/both.xml")) return true; // file not found
		if (endsWith(file, "xerces2/tests/xinclude/included/not-well-formed.xml")) return true; // not wellformed
		

		// activesoap testsuite:
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T12_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T17_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T25_fromA.xml")) return true; // file not found
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T31_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T35_fromB.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T44_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T57_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T57_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T58_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T58_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T59_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T59_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T60_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T60_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T61_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T61_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T64_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T64_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T65_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T66_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T73_fromC.xml")) return true; // prefix not bound
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T76_fromA.xml")) return true; // prefix not bound
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T76_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T77_fromA.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/test/org/codehaus/activesoap/soap/T77_fromC.xml")) return true; // not wellformed
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/wsdl/XML.xsd")) return true; // file not found
		if (endsWith(file, "activesoap-1.0-SNAPSHOT/src/wsdl/XMLSchema.xsd")) return true; // file not found
		
		
		// servicemix testuite:
		if (endsWith(file, "servicemix-2.0.2/components/base/src/test/resources/org/servicemix/components/saaj/response.xml")) return true; // file not found
		if (endsWith(file, "servicemix-2.0.2/ws/jaxws/wspojo/etc/web.xml")) return true; // malformed
		if (endsWith(file, "servicemix-2.0.2/tooling/maven-jbi-plugin/test.xml")) return true; // not wellformed
		if (endsWith(file, "servicemix-2.0.2/tooling/maven-jbi-plugin/src/plugin-resources/services.xml")) return true; // not wellformed
		
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/assembly/src/release/conf/log4j.xml")) return true; // file not found
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-components/src/test/resources/org/servicemix/components/saaj/response.xml")) return true; // not wellformed
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-console/src/webapp/WEB-INF/data/xml/pageregistrymapping.xml")) return true; // not wellformed
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-console/src/webapp/WEB-INF/data/xml/portletdefinitionmapping.xml")) return true; // unknown host	
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-console/src/webapp/WEB-INF/data/xml/portletentitymapping.xml")) return true; // unknown host	
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-console/src/webapp/WEB-INF/data/xml/servletdefinitionmapping.xml")) return true; // unknown host	
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-core/pom.xml")) return true; // prefix not bound
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/src/release/conf/log4j.xml")) return true; // file not found
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/tooling/maven-jbi-plugin/test.xml")) return true; // not wellformed
		if (endsWith(file, "servicemix-2.1-SNAPSHOT/tooling/maven-jbi-plugin/src/plugin-resources/services.xml")) return true; // not wellformed
		
		// triggered by incompatible website update of DTD:
//		if (contains(file, "/servicemix-2.1-SNAPSHOT/") && endsWith(file, "/broker.xml")) return true; // The markup declarations contained or pointed to by the document type declaration must be well-formed.
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/assembly/src/release/conf/activemq.xml")) return true; // ditto
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/assembly/src/release/examples/jms-binding/activemq.xml")) return true; // ditto
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-components/src/test/resources/org/servicemix/components/jabber/broker.xml")) return true; // ditto
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-components/src/test/resources/org/servicemix/components/jca/broker.xml")) return true; // ditto
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-components/src/test/resources/org/servicemix/components/wsif/broker.xml")) return true; // ditto
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-wsnotification/src/su/classes/broker-vmpersistence.xml")) return true; // ditto
//		if (endsWith(file, "servicemix-2.1-SNAPSHOT/servicemix-wsnotification/src/test/resources/broker-vmpersistence.xml")) return true; // ditto
		
		
		// xmlbeans testsuite:
		if (endsWith(file, "xmlbeans-2.1.0/samples/vxsdb/src/java/log4j.xml")) return true; // file not found
		if (endsWith(file, "xmlbeans-2.1.0/src/xmlschema/schema/XML.xsd")) return true; // not wellformed
		if (endsWith(file, "xmlbeans-2.1.0/src/xsdschema/schema/XMLSchema.xsd")) return true; // not wellformed
		
		
		// dom4j testsuite:
		if (endsWith(file, "dom4j-1.6.1/xml/test/badcomment.xml")) return true; // not wellformed
		if (endsWith(file, "dom4j-1.6.1/xml/test/junk.xml")) return true; // not wellformed
		if (endsWith(file, "dom4j-1.6.1/src/conf/xsa.xml")) return true; // no route to host
		
		
		// axis testsuite:
		if (contains(file, "/axis-1_3/") && contains(file, "/build.xml")) return true; // file not found
		if (endsWith(file, "/axis-1_3/buildSamples.xml")) return true; // file not found
		if (endsWith(file, "/axis-1_3/buildTest.xml")) return true; // file not found
		if (endsWith(file, "/axis-1_3/tcpmon.xml")) return true; // file not found
		if (endsWith(file, "/axis-1_3/test/wsdl/interop3/groupE/Interop3GroupE.xml")) return true; // file not found
		if (endsWith(file, "/axis-1_3/tools/test.xml")) return true; // file not found
		if (contains(file, "/axis-1_3/xmls/")) return true; // file not found
		
		
		// pubscribe testsuite:
		if (endsWith(file, "pubscribe-1.1/src/examples/pubsubclient/jndi-config_wse.xml")) return true; // not wellformed
		if (endsWith(file, "pubscribe-1.1/src/site/content/xdocs/tabs.xml")) return true; // file not found
		if (endsWith(file, "pubscribe-1.1/src/site/content/xdocs/forrest_samples/linking.xml")) return true; // file not found
		if (endsWith(file, "pubscribe-1.1/src/site/content/xdocs/forrest_samples/sample2.xml")) return true; // file not found
		
		
		// apache lenya testsuite:
		if (endsWith(file, "apache-lenya-1.2.4-src/src/test/webtest/tests.xml")) return true; // file not found
		if (endsWith(file, "apache-lenya-1.2.4-src/src/webapp/lenya/content/menus/info.xsp")) return true; // not wellformed
		if (endsWith(file, "apache-lenya-1.2.4-src/src/webapp/lenya/pubs/default/test/anteater/tests.xml")) return true; // not wellformed
		if (endsWith(file, "apache-lenya-1.2.4-src/src/webapp/lenya/pubs/default/test/webtest/tests.xml")) return true; // not wellformed
		if (endsWith(file, "apache-lenya-1.2.4-src/src/webapp/WEB-INF/log4j.xconf")) return true; // not wellformed
		
		
		// cocoon testsuite:
		if (contains(file, "/jetty")) return true; // java.net.SocketException: Unexpected end of file from server	
		if (endsWith(file, "cocoon-2.1.8/src/blocks/chaperon/test/org/apache/cocoon/transformation/parsertest-result3.xml")) return true; // not wellformed
		if (endsWith(file, "cocoon-2.1.8/src/blocks/databases/samples/xsp/esql.xsd")) return true; // file not found
		if (contains(file, "cocoon-2.1.8/src/blocks/mail/samples/mail/docs/mid-col-2/")) return true; // file not found
		if (endsWith(file, "cocoon-2.1.8/src/blocks/ojb/conf/repository_database.xml")) return true; // not wellformed
		if (endsWith(file, "cocoon-2.1.8/src/blocks/ojb/conf/repository_internal.xml")) return true; // not wellformed
		if (endsWith(file, "cocoon-2.1.8/src/blocks/ojb/conf/repository_user.xml")) return true; // not wellformed
		if (endsWith(file, "cocoon-2.1.8/src/blocks/portal/java/org/apache/cocoon/portal/pluto/om/portletdefinitionmapping.xml")) return true; // unknown host
		if (endsWith(file, "cocoon-2.1.8/src/blocks/portal/java/org/apache/cocoon/portal/pluto/om/servletdefinitionmapping.xml")) return true; // unknwown host
		if (endsWith(file, "cocoon-2.1.8/src/blocks/portal/samples/tools/auth.xml")) return true; // not wellformed
		if (endsWith(file, "cocoon-2.1.8/src/confpatch/mount-table.xmap")) return true; // prefix not found
		if (endsWith(file, "cocoon-2.1.8/src/documentation/templates/sitemap-component.xml")) return true; // file not found
		if (contains(file, "cocoon-2.1.8/src/documentation/xdocs/")) return true; // file not found
		if (endsWith(file, "cocoon-2.1.8/src/webapp/samples/aggregation/content/itest2.xml")) return true; // file not found
		if (contains(file, "cocoon-2.1.8/src/webapp/samples/catalog/")) return true; // file not found
		if (endsWith(file, "cocoon-2.1.8/src/webapp/samples/errorhandling/exception/error-giving-page.xml")) return true; // file not found
		if (contains(file, "cocoon-2.1.8/src/webapp/samples/text-wrap/")) return true; // file not found
		if (endsWith(file, "cocoon-2.1.8/src/webapp/test-suite/xdocs/index.xml")) return true; // file not found
		if (endsWith(file, "cocoon-2.1.8/src/webapp/WEB-INF/log4j.xconf")) return true; // file not found
		if (endsWith(file, "cocoon-2.1.8/tools/jetty/conf/admin.xml")) return true; // DTD connection timeout		
		
		return false;
	}

	// operating system insensitive file name comparison
	protected static boolean endsWith(String x, String y) {
		x = x.replace('/', File.separatorChar);
		x = x.replace('\\', File.separatorChar);
		y = y.replace('/', File.separatorChar);
		y = y.replace('\\', File.separatorChar);
		
		return x.endsWith(y);
	}
	
	// operating system insensitive file name comparison
	protected static boolean contains(String x, String y) {
		x = x.replace('/', File.separatorChar);
		x = x.replace('\\', File.separatorChar);
		y = y.replace('/', File.separatorChar);
		y = y.replace('\\', File.separatorChar);
		
		return x.indexOf(y) >= 0;
	}

	protected static Builder getBuilder() {
//		if (!ENABLE_BUILDER_POOL) return new Builder();
		if (!ENABLE_BUILDER_POOL) return new Builder(new NodeFactory() {});
		return BuilderPool.GLOBAL_POOL.getBuilder(false);
	}
	
}
