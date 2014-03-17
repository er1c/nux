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
import java.io.FileWriter;

import nu.xom.Builder;
import nu.xom.Document;
import nux.xom.pool.DocumentMap;
import nux.xom.pool.PoolConfig;

/**
 * Stress test for file change monitoring of DocumentMap. Determines how much
 * overhead File.lastModified() has when monitoring thousands or tens of
 * thousands of files.
 * 
 * @author whoschek@lbl.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2005/11/30 00:22:09 $
 */
final class DocumentMapFileMonitorTest {

	public static void main(String[] args) throws Exception {
		System.setProperty("nux.xom.pool.Pool.debug", "true"); // watch evictions
//		System.setProperty("nux.xom.pool.PoolConfig.invalidationPeriod", "5000"); 
		
		final PoolConfig config = new PoolConfig();
		int k = -1;
		
		int numFiles = 1000;
		if (args.length > ++k) numFiles = Math.max(1, Integer.parseInt(args[k]));

		int threads = 1;
		if (args.length > ++k) threads = Math.max(1, Integer.parseInt(args[k]));
		
		if (args.length > ++k) config.setCompressionLevel(Integer.parseInt(args[k]));
		
		if (args.length > ++k) config.setMaxEntries(Integer.parseInt(args[k]));
		
		if (args.length > ++k) config.setFileMonitoring(new Boolean(args[k]).booleanValue());
		
		if (args.length > ++k) config.setCapacity(Long.parseLong(args[k]));
		
		if (args.length > ++k) config.setMaxIdleTime(Long.parseLong(args[k]));
		
		if (args.length > ++k) config.setMaxLifeTime(Long.parseLong(args[k]));
		
		final DocumentMap pool = new DocumentMap(config);
		
		final File[] files = new File[numFiles];
		new File("tmp").mkdir();
		for (int j=0; j < numFiles; j++) {
			files[j] = new File("tmp/file" + j + ".xml");
			System.out.println("writing " + files[j]);
			FileWriter out = new FileWriter(files[j]);
			out.write("<hello/>");
			out.flush();
			out.close();
		}
		
		for (int j=0; j < threads; j++) {
			final int t = j;
			Runnable runner = new Runnable() {
				public void run() {
					try {
						Builder builder = new Builder();
						int i = 0;
						while (i < files.length) {
							System.out.println("t="+ t + ", index="+i);
							Document doc = builder.build(files[i]);
							Object key = files[i];
							pool.putDocument(key, doc);
							i++;
						}
						System.out.println("done");
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