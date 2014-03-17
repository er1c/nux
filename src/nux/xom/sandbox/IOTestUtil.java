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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import nu.xom.Document;
import nu.xom.tests.XOMTestCase;
import nux.xom.pool.FileUtil;
import nux.xom.pool.XOMUtil;

/**
 * Utilities for simple tests.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.6 $, $Date: 2006/02/19 12:32:59 $
 */
class IOTestUtil {
	
	private static final boolean compareInternalDTDSubset = false;
			
	public static void xomAssertEquals(Document expected, Document actual) {
		try {
			XOMTestCase.assertEquals(expected, actual);
			if (compareInternalDTDSubset) {
				if (expected.getDocType() != null || actual.getDocType() != null) {
					String s1 = expected.getDocType().getInternalDTDSubset();
					String s2 = actual.getDocType().getInternalDTDSubset();
//					s1 = normalize(s1);
//					s2 = normalize(s2);
					Assert.assertEquals("getInternalDTDSubset" + getDifference(s1, s2) + "\n", 
						s1, s2);
				}
			}
		} catch (AssertionFailedError e) {
			System.out.println("expected = "+ expected.toXML());
			System.out.println("actual   = "+ actual.toXML());
			System.out.println("Canonical XML Diff: " + getCanonicalDifference(expected, actual));
			throw e;
		}
	}
	
	public static void canonicalAssertEquals(Document expected, Document actual) {
		String diff = getCanonicalDifference(expected, actual);
		if (diff != null) {
			throw new RuntimeException("Canonical XML Diff: " + diff);
		}
	}
	
	// returns null if there's no diff, otherwise returns a description of first differing window
	public static String getCanonicalDifference(Document expected, Document actual) {
		int window = 20;
		byte[] e = XOMUtil.toCanonicalXML(expected);
		byte[] a = XOMUtil.toCanonicalXML(actual);
		
		if (!Arrays.equals(e, a)) {
			// print snippet of the offending area to gain some debugging clues
			String diff = "";
			if (e.length != a.length) {
				diff += "e.length=" + e.length + ", a.length=" + a.length + "\n";
//				diff += "eb="+ new ArrayByteList(e) + "\n";
//				diff += "ab="+ new ArrayByteList(a) + "\n";
			}
			int size = Math.min(e.length, a.length);
			for (int i=0; i < size; i++) {
				if (e[i] != a[i]) {
					diff += "diff at i=" + i + ", e[i]=" + e[i] + 
							", a[i]=" + a[i] + "\n";
					int off = Math.max(0, i-window);
					int len1 = Math.min(2*window, e.length-off);
					int len2 = Math.min(2*window, a.length-off);
					try {
						diff += "e='"+ new String(e, off, len1, "UTF-8") + "'\n";
						diff += "a='"+ new String(a, off, len2, "UTF-8") + "'\n";
	//					System.out.println("e1='"+ new String(e, "UTF-8") + "'");
	//					System.out.println("a1='"+ new String(a, "UTF-8") + "'");
					} catch (UnsupportedEncodingException x) {
						throw new RuntimeException(x); // can never happen
					}
					return diff;
				}
			}
		}
		return null;
	}
	
	// returns null if there's no diff, otherwise returns a description of first differing window
	private static String getDifference(String expected, String actual) {
		int window = 20;
		String e = expected;
		String a = actual;
		if (!e.equals(a)) {
			// print snippet of the offending area to gain some debugging clues
			String diff = "";
			if (e.length() != a.length()) {
				diff += "e.length=" + e.length() + ", a.length=" + a.length() + "\n";
			}
			int size = Math.min(e.length(), a.length());
			for (int i=0; i < size; i++) {
				if (e.charAt(i) != a.charAt(i)) {
					diff += "diff at i=" + i + ", e[i]=" + e.charAt(i) + 
							", a[i]=" + a.charAt(i) + "\n";
					int off = Math.max(0, i-window);
					int len1 = Math.min(2*window, e.length()-off);
					int len2 = Math.min(2*window, a.length()-off);
					diff += "e='"+ e.substring(off, off + len1) + "'\n";
					diff += "a='"+ a.substring(off, off + len2) + "'\n";
					return diff;
				}
			}
		}
		return null;
	}
	
	public static File[] listXMLFiles(String fileName) {
		String cocoon = "*.xmap *.xsp *.xconf *.xsamples *.xweb *.xtest";
		String includes = "*.xml *.xsl *.xsd *.rdf *.svg *.wsdl *.xhtml *.mml *.smil *.smi *.wsdd" + " " + cocoon;
		return listXMLFiles(fileName, includes);
	}
	
	public static File[] listXMLFiles(String fileName, String includes) {
		File file = new File(fileName);
		URI[] uris;
		if (!file.exists()) {
			uris = new URI[0];
		} else if (file.isDirectory()) {
			uris = FileUtil.listFiles(file.getAbsolutePath(), true, includes, "");
		} else {
			uris = new URI[] { file.toURI() };
		}
		
		File[] files = new File[uris.length];
		for (int i=0; i < uris.length; i++) {
			files[i] = new File(uris[i]);
		}
		return files;
	}

	/** little helper for safe reading of string system properties */
	static String getSystemProperty(String key, String def) {
		try { 
			return System.getProperty(key, def);
		} catch (Throwable e) { // better safe than sorry (applets, security managers, etc.) ...
			return def; // we can live with that
		}		
	}
	
	/** little helper for safe reading of boolean system properties */
	static boolean getSystemProperty(String key, boolean def) {
		try { 
			return "true".equalsIgnoreCase(System.getProperty(key, String.valueOf(def)));
		} catch (Throwable e) { // better safe than sorry (applets, security managers, etc.) ...
			return def; // we can live with that
		}		
	}
	
//	private static String normalize(String text) {
//	Element wrapper = new Element("dummy");
//	wrapper.appendChild(text);
//	XOMUtil.Normalizer.COLLAPSE.normalize(wrapper);
//	return wrapper.getValue();
//}

//public static void xomAssertEquals2(Document expected, Document actual) {
//	try {
//		XOMTestCase.assertEquals(expected, actual);
//	} catch (AssertionFailedError e) {
//		String s = "";
//		s += "expected = "+ expected.toXML() + "\n";
//		s += "actual   = "+ actual.toXML() + "\n";
//		s += "Canonical XML Diff: " + getCanonicalDifference(expected, actual) + "\n";
////		System.out.println(s);
//		throw new AssertionFailedError(s);
////		throw e;
//	}
//}

	
}
