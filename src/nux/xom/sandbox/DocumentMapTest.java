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

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nux.xom.pool.DocumentMap;
import nux.xom.pool.PoolConfig;

/**
 * Stress test putting infinitely many documents into a DocumentMap; an
 * OutOfMemoryError should never occur as a result of doing this, with
 * <code>java -Xmx8m -verbose:gc</code> and
 * <code>java -Xmx512m -verbose:gc</code> and without any -Xmx limit, both
 * with JDK 1.4 and 1.5 (Sun and IBM), both with client and server VM.
 * <p>
 * Test results:
 * <p>
 * Code and VMs works as expected without any leak, except that IBM JDK 1.4.2
 * (build cxia32142sr1a-20050209) fails due to a VM bug when
 * zlibCompressionLevel == -1 (this level is almost always a bad idea anyway).
 * <p>
 * Sun JDK >= 1.4 guarantees that all SoftReferences will be collected before
 * considering to throw an OutOfMemoryError. This is precisely the expected
 * behaviour (the idea that an auxiliary cache can lead to an OutOfMemoryError
 * would be insane).
 * 
 * @author whoschek@lbl.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.1 $, $Date: 2005/11/11 23:18:28 $
 */
public final class DocumentMapTest {

	public static void main(String[] args) throws Exception {
		System.setProperty("nux.xom.pool.Pool.debug", "true"); // watch evictions
//		System.setProperty("nux.xom.pool.PoolConfig.invalidationPeriod", "1000"); 
		
		final PoolConfig config = new PoolConfig();
		int k = -1;
		
		Document doc = null;
		if (args.length > ++k && !args[k].equals("-")) 
			doc = new Builder().build(args[k]);
		
		int threads = 1;
		if (args.length > ++k) threads = Math.max(1, Integer.parseInt(args[k]));
		
		if (args.length > ++k) config.setCompressionLevel(Integer.parseInt(args[k]));
		
		if (args.length > ++k) config.setMaxEntries(Integer.parseInt(args[k]));
		
		if (args.length > ++k) config.setFileMonitoring(new Boolean(args[k]).booleanValue());
		
		if (args.length > ++k) config.setCapacity(Long.parseLong(args[k]));
		
		if (args.length > ++k) config.setMaxIdleTime(Long.parseLong(args[k]));
		
		if (args.length > ++k) config.setMaxLifeTime(Long.parseLong(args[k]));
		
		int printStep = 10;
		if (args.length > ++k) printStep = Math.max(1, Integer.parseInt(args[k]));
		
		final DocumentMap pool = new DocumentMap(config);
		
		for (int j=0; j < threads; j++) {
			final int t = j;
			final Document xmlDoc = doc;
			final int step = printStep;
			Runnable runner = new Runnable() {
				public void run() {
					try {
						int i = 0;
						while (true) {
							if (i % step == 0) System.out.println("t="+ t + ", index=" + i);
							Document doc;
							if (xmlDoc == null) {
								Element root = new Element("root");
								Element child = new Element("child");
								root.appendChild(child);
								for (int j=0; j < 10000; j++) child.appendChild("xxxxxxxxxxxxxxxxxx" + j);
								doc = new Document(root);
							}
							else {
								doc = xmlDoc;
								if (config.getCompressionLevel() == -1) doc = new Document(doc);
							}
							
							Object key = new Integer(i + t*100);
//							Object key = new Integer(-1);
							pool.putDocument(key, doc);
							
//							// simulate hot cache items (gc should bias against collecting these)
							pool.getDocument(new Integer(8)); 
							
//							pool.getDocument(key);
							i++;
						}
					}
					catch (Throwable t) {
						t.printStackTrace();
						System.exit(-1);
					}
				}
			};
			new Thread(runner).start();
		}
		
		Thread.sleep(Long.MAX_VALUE); // wait forever
	}

}