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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLOutputFactory;

import nu.xom.Document;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;

/**
 * Integration test for round-tripping of StreamingSerializerFactory.createStaxSerializer().write().
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek $
 * @version $Revision: 1.9 $, $Date: 2006/06/19 04:42:02 $
 */
public class StaxSerializerTest extends IOTest {
	
	public static void main(String[] args) throws Throwable {
		String[] encodings = new String[] {
				"UTF-8", 
				"UTF-16",
				"UTF-16BE",
				"UTF-16LE",
//				"Big5",
//				"Shift_JIS",
//				"Cp856",
//				"ISCII91",
		};
		
		System.setProperty("nu.xom.Verifier.checkURI", "false");
		XMLOutputFactory outFactory = createXMLOutputFactory(args[0]);
		StreamingSerializerFactory factory = new StreamingSerializerFactory();
		
		int bugs = 0;
		int k = 0;
		for (int i=1; i < args.length; i++) {
			File[] files = IOTestUtil.listXMLFiles(args[i]);
			for (int j=0; j < files.length; j++, k++) {
				File file = files[j];
				if (bogus(file) || ignore(file) || file.isDirectory()) {
					System.out.println("\n" + k + ": IGNORING " + file + " ...");
					continue;
				}
						
				System.out.println("\n" + k + ": now processing " + file + " ...");
				Document expected = getBuilder().build(file);
//				System.out.println(expected.toXML());
				System.out.print("*");
				
				for (int enc = 0; enc < encodings.length; enc++) {
					try {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						
						StreamingSerializer ser;
//						ser = factory.createXMLSerializer(out, encoding);
//						((nu.xom.Serializer) ser).setIndent(4);
						ser = factory.createStaxSerializer(outFactory.createXMLStreamWriter(out, encodings[enc]));
						ser.write(expected);
						
//						String s = new String(out.toByteArray());
//						s =  s.substring(0, Math.min(2000, s.length()));
//						System.out.println("\n" + s + "\n");
//						log(out.toByteArray());
						InputStream in = new ByteArrayInputStream(out.toByteArray());
						Document actual = getBuilder().build(in, file.toURI().toASCIIString());
						
						IOTestUtil.xomAssertEquals(expected, actual);
						IOTestUtil.canonicalAssertEquals(expected, actual);		
					} catch (Throwable e) {
						bugs++;
						if (FAIL_FAST) throw e;
						System.err.println("\nOopsla:" + k + ":" + bugs + ":" + file + ":" + encodings[enc] + ":");
						e.printStackTrace();
					}
				}
			}
		}		
		
		System.out.println("\nNumber of bugs detected: " + bugs);
	}
	
	private static XMLOutputFactory createXMLOutputFactory(String staxMode) {
		if (staxMode.equals("sun")) {
			System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.stream.ZephyrWriterFactory");
		}
		else if (staxMode.equals("wood")) {
			System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory");
		}		
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		System.out.println("outFactory=" + factory.getClass().getName());
		return factory;
	}
	
	private static void log(byte[] bytes) throws IOException {
		FileOutputStream os = new FileOutputStream("/tmp/test.xml"); 
		os.write(bytes);
		os.flush();
		os.close();		
	}
	
	// ignore some stuff from the test suite dirs
	private static boolean ignore(File xmlFile) {
		String file = xmlFile.getAbsolutePath();
		
		// streaming xml ser:
//		if (endsWith(file, "data/map-spain.svg.xml")) return true; // ???
		
		// XOM test suite:
//		if (endsWith(file, "XOM/data/pe.xml")) return true; // XOM URIUtil bug with xerces >= 2.7.0; fixed in xom-1.2
		
		// wstx-2.9:
//		if (endsWith(file, "data/teams.xml")) return true; // escaping \r
//		if (endsWith(file, "data/teams-noindent.xml")) return true; // escaping \r
//		if (endsWith(file, "data-ant/xerces.xml")) return true; // escaping &#xA;
//		if (endsWith(file, "entref/entref01.xsl")) return true; // escaping &#x9;
//		if (endsWith(file, "entref/entref02.xsl")) return true; // escaping &#x0D;
//		if (endsWith(file, "entref/entref03.xsl")) return true; // escaping &#x0D;
//		if (endsWith(file, "nspc/impnspc13.xsl")) return true; // escaping 
//		if (endsWith(file, "nspc/incnspc13.xsl")) return true; // escaping 
//		if (endsWith(file, "outp/outp66.xml")) return true; //  LF <-> CR
//		if (endsWith(file, "outp/outp66.xsl")) return true; //  attr escaping
//		if (endsWith(file, "str/str010.xsl")) return true; //  escaping
//		if (endsWith(file, "type/type035.xsl")) return true; //  escaping
//		if (endsWith(file, "wolf-xom/canonical/input/test3.4.xml")) return true; //  escaping
//		if (endsWith(file, "wolf-xom/xinclude/output/lineends.xml")) return true; //  escaping
//		if (contains(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/")) return true; //  escaping
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/067.xml")) return true; //  escaping
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/068.xml")) return true; //  escaping
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/105.xml")) return true; //  escaping
//		if (endsWith(file, "wolf/w3c-xmlconformance/xmltest/valid/sa/106.xml")) return true; //  escaping
//		
//		if (endsWith(file, "wolf/w3c-xmlconformance/sun/invalid/not-sa02.xml")) return true; //  escaping
//		if (endsWith(file, "wolf/w3c-xmlconformance/sun/invalid/not-sa08.xml")) return true; //  escaping
		
		
		// W3C XSLT testsuite:
//		if (endsWith(file, "Xalan_Conformance_Tests/namespace/namespace110.xsl")) return true; // Element type "xsl:element" must be followed by either attribute specifications, ">" or "/>"
//		if (endsWith(file, "MSFT_Conformance_Tests/Include/bloated.xsl")) return true; // catastrophic xerces-2.7.1 performance degradation for large PCDATA blocks; fixed in xerces-2.8.0
				
		return false;
	}
	
}
