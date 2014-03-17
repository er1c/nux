package nux.xom.sandbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import nu.xom.Builder;
import nu.xom.DocType;
import nu.xom.Document;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.pool.XOMUtil;

/**
 * Similar to BinaryXMLTest, but tests multi-document streams and/or changing
 * compression levels.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2005/12/08 05:24:15 $
 */
class BinaryXMLMultiTest {

	public static void main(String[] args) throws Exception {
		final String cmd = args[0]; // ser|deser|serdeser|test
		final String mode = args[1]; // bnux|xom|saxon|dom|fi
		int compressionLevel = Integer.parseInt(args[2]); // 0..9
		final int iterations = Integer.parseInt(args[3]); // 1..infinity
		final int runs = Integer.parseInt(args[4]); // 1..infinity
		final boolean enablePerformancePatches = "true".equals(
			System.getProperty("nux.xom.sandbox.BinaryXMLTest.enablePatches", "true"));
		System.out.println("patchesEnabled=" + enablePerformancePatches);
		if (enablePerformancePatches) { // init before BinaryXMLCodec
			// temporary (?) performance hack via patch: disable some expensive sanity checks 
//			System.setProperty("nu.xom.Verifier.checkPCDATA", "false");
			System.setProperty("nu.xom.Verifier.checkURI", "false");
		}
		final boolean testCompressionLevels = true;
		
		BinaryXMLCodec codec = new BinaryXMLCodec();
		for (int j=5; j < args.length; j++) {
			if (ignore(args[j])) continue;
			File file = new File(args[j]);
			if (file.isDirectory()) continue;
			System.out.println("now processing " + file);
			
			Document doc = new Builder().build(file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			for (int p=0; p < iterations; p++) {
				System.out.println("compressionLevel=" + compressionLevel);
				codec.serialize(doc, compressionLevel, out);
				if (testCompressionLevels) { // alternate on each iteration
					compressionLevel = (compressionLevel + 1) % 10;
				}
			}
			
			InputStream in = new ByteArrayInputStream(out.toByteArray());
			for (int p=0; p < iterations; p++) {
				Document doc2 = codec.deserialize(in, null);
				// check correctness
				if (cmd.equals("test")) {
					if (! Arrays.equals(XOMUtil.toCanonicalXML(doc), XOMUtil.toCanonicalXML(doc2))) {
						System.err.println("Canonical XML Mismatch: ");
						System.err.println("expected: " + doc.toXML());
						System.err.println("actual: " + doc2.toXML());									
						printDiff(doc, doc2);
						System.exit(0);
					}
					if (!equalsDocTypeEquals(doc.getDocType(), doc2.getDocType())) {	
						System.err.println("DocType Mismatch: ");
						System.err.println("expected: " + doc.toXML());
						System.err.println("actual: " + doc2.toXML());
						System.exit(0);
					}
				}
			}		
		}		
	}

	// print snippet of the offending area to gain some debugging clues
	private static void printDiff(Document expected, Document actual) {
		int window = 100;
//		int window = 300;
//		int window = 10;
		System.err.println("Canonical XML Diff Location Snippet:");
		byte[] e = XOMUtil.toCanonicalXML(expected);
		byte[] a = XOMUtil.toCanonicalXML(actual);
		if (e.length != a.length) {
			System.err.println("e.length="+ e.length + ", a.length=" + a.length);
		}
		int size = Math.min(e.length, a.length);
		for (int i=0; i < size; i++) {
			if (e[i] != a[i]) {
				System.err.println("diff at i=" + i + ", e[i]=" + e[i] + 
						", a[i]=" + a[i]);
				int off = Math.max(0, i-window);
//				int off = Math.max(0, i-0);
				int len = Math.min(2*window, size-off);
				try {
					System.err.println("e='"+ new String(e, off, len, "UTF-8") + "'");
					System.err.println("a='"+ new String(a, off, len, "UTF-8") + "'");
//					System.err.println("eb='"+ new ArrayByteList(new String(e, off, len, "UTF-8").getBytes("UTF-8")));
//					System.err.println("ab='"+ new ArrayByteList(new String(a, off, len, "UTF-8").getBytes("UTF-8")));
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException("can never happen");
				}
				break;
			}
		}
	}
		
	private static boolean equalsDocTypeEquals(DocType x, DocType y) {
		if (x != y) {
			if (x == null && y != null)
				return false;
			if (x != null && y == null)
				return false;
			
			if (!x.getInternalDTDSubset().equals(y.getInternalDTDSubset()))
				return false;
			if (!eq(x.getPublicID(), y.getPublicID()))
				return false;
			if (!eq(x.getSystemID(), y.getSystemID()))
				return false;
			if (!eq(x.getRootElementName(), y.getRootElementName()))
				return false;
			return true;
		}

		return true;
	}
	
	private static boolean eq(Object x, Object y) {
		if (x != y) {
			if (x == null && y != null)
				return false;
			if (x != null && y == null)
				return false;
			if (!x.equals(y))
				return false;
		}

		return true;
	}
	
	// ignore some stuff from the test suite dirs
	private static boolean ignore(String file) {
		if (file.endsWith(".html")) return true;
		if (file.endsWith(".out")) return true;
		if (file.endsWith(".txt")) return true;
		if (file.endsWith(".bat")) return true;
		if (file.endsWith(".sh")) return true;
		if (file.endsWith(".sxx")) return true;
		if (file.endsWith(".ssx")) return true;
		if (file.endsWith(".dtd")) return true;
		if (file.endsWith(".xq")) return true;
		if (file.endsWith(".zip")) return true;
		if (file.endsWith(".gz")) return true;
		if (file.endsWith(".jpg")) return true;
		if (file.endsWith(".gif")) return true;
//		if (file.endsWith("atrs13.xml")) return true; // char encoding
//		if (file.endsWith("atrs15.xml")) return true; // char encoding
		
//		if (file.endsWith("atrs47.xsl")) return true;
//		if (file.endsWith("atrs48.xsl")) return true;
//		if (file.endsWith("idky04.xml")) return true;
//		if (file.endsWith("idky10.xml")) return true;
//		if (file.endsWith("idky104.xml")) return true;
//		if (file.endsWith("idky11.xml")) return true;
//		if (file.endsWith("idky122.xml")) return true;
//		if (file.endsWith("idky123.xml")) return true;
		
		if (file.endsWith("nspc05x.xml")) return true; // not wellformed
//		if (file.endsWith("idky28.xml")) return true; // char encoding
//		if (file.endsWith("idky28wolf.xml")) return true; // char encoding
//		if (file.endsWith("idky35.xml")) return true; // char encoding
//		if (file.endsWith("stdxmlfile.xml")) return true; // char encoding
//		if (file.endsWith("copy19.xsl")) return true; // char encoding
//		if (file.endsWith("copy20.xsl")) return true; // char encoding
//		if (file.endsWith("copy22.xml")) return true; // char encoding
//		if (file.endsWith("copy38.xsl")) return true; // char encoding
		if (file.endsWith("ent22.xml")) return true; // not wellformed
		if (file.endsWith("2")) return true; // not wellformed

//		if (file.endsWith("expr85.xml")) return true; // char encoding
//		if (file.endsWith("numberformat06.xsl")) return true; // char encoding
//		if (file.endsWith("numberformat07.xsl")) return true; // char encoding
//		if (file.endsWith("str122.xsl")) return true; // char encoding
//		if (file.endsWith("str126.xsl")) return true; // char encoding
//		if (file.endsWith("str127.xsl")) return true; // char encoding
//		
//		if (file.indexOf("entref") >= 0) return true; // char encoding
		
		if (file.indexOf("err.") >= 0) return true;
		if (file.indexOf("ERR.") >= 0) return true;
//		if (file.indexOf("fail") >= 0) return true;
		if (file.indexOf("Untitled") >= 0) return true;
		
		// xom test suite:
		if (file.endsWith("CVS")) return true; // CVS messes up char encoding
		if (file.endsWith("ucs4bigendian.xml")) return true; // CVS messes up char encoding
		if (file.endsWith("ucs4littleendian.xml")) return true; // CVS messes up char encoding
		if (file.endsWith("utf8.xml")) return true; // CVS messes up char encoding
		if (file.endsWith("097.ent")) return true; // CVS messes up char encoding
		if (file.endsWith("test_ebcdic.xml")) return true; // CVS messes up char encoding
		
		// W3C XML test suite:
		if (file.endsWith(".ent")) return true; 
		if (file.endsWith("xmltest/valid/sa/012.xml")) return true; // invalid
		if (file.indexOf("fail") >= 0) return true;
		if (file.endsWith("p04pass1.xml")) return true; 
		if (file.endsWith("namespaces/1.0/009.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/010.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/011.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/013.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/014.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/015.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/016.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/023.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/025.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/026.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/029.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.0/030.xml")) return true; // not wellformed
		if (file.endsWith("namespaces/1.1/005.xml")) return true; // not wellformed
		
//		if (file.endsWith("valid/sa/066.xml")) return true; // xerces-2.6.2 internal subset
//		if (file.endsWith("valid/sa/101.xml")) return true; // xerces-2.6.2 internal subset
//		if (file.endsWith("valid/sa/090.xml")) return true; // crimson internal subset
		
		if (file.endsWith("xinclude/input/utf8bom.xml")) return true; // crimson can't handle BOMs
		if (file.endsWith("wolf-xom/dtdtest.xhtml")) return true; // crimson bug
		if (file.endsWith("wolf-xom/xinclude/output/langtest3.xml")) return true; // crimson bug
		if (file.indexOf("wolf/large/svg/tests/spec/color/") >= 0) return true; // more crimson bugs
		if (file.indexOf("wolf/large/svg/tests/spec/coordinates/") >= 0) return true;
		if (file.indexOf("wolf/large/svg/tests/spec/filters/") >= 0) return true;
		if (file.indexOf("wolf/large/svg/tests/spec/fonts/") >= 0) return true;
		
		return false;
	}
		
	
}
