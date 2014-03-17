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
import nux.xom.pool.BuilderFactory;

/**
 * Quick'n dirty test for BuilderFactory.getW3CBuilder().
 * <p>
 * Example usage:
 * <pre>
 * ant download-testdata
 * java nux.xom.sandbox.SchemaValidatingBuilderTest ../nux-testdata/xsts/Tests/Datatypes
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.5 $, $Date: 2006/03/24 03:26:38 $
 */
public class SchemaValidatingBuilderTest extends IOTest {
	
	private static final boolean USE_SCHEMA_VALIDATING_BUILDER = true;
	
	public static void main(String[] args) throws Exception {
		System.setProperty("nu.xom.Verifier.checkURI", "false");
		
		int k = 0;
		Builder pooledBuilder = getW3CBuilder();
		for (int i=0; i < args.length; i++) {
			File[] files = IOTestUtil.listXMLFiles(args[i], "*.xml");
			for (int q=0; q < 1; q++) {
				for (int j=0; j < files.length; j++, k++) {
					File file = files[j];
					if (bogus(file) || ignore(file) || file.isDirectory()) {
						System.out.println("\n" + k + ": IGNORING " + file + " ...");
						continue;
					}
					if (file.getAbsolutePath().indexOf("-II-") >= 0) continue; // ignore invalid files
					
					System.out.println("\n" + k + ": now processing " + file + " ...");
					for (int p=0; p < 1; p++) {
						Document doc1 = getW3CBuilder().build(file);						
						Document doc2 = pooledBuilder.build(file);
						
						IOTestUtil.xomAssertEquals(doc1, doc2);
						IOTestUtil.canonicalAssertEquals(doc1, doc2);
					}
				}
			}
		}
	}
	
	private static Builder getW3CBuilder() {
		if (USE_SCHEMA_VALIDATING_BUILDER) {
			return new BuilderFactory().createW3CBuilder(null);
		}
		return new Builder();
	}
	
	// ignore some stuff from the test suite dirs
	private static boolean ignore(File file) {
		return false;
	}
	
}
