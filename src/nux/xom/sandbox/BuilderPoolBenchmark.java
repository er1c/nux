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
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.NodeFactory;
import nux.xom.pool.BuilderFactory;
import nux.xom.pool.BuilderPool;
import nux.xom.pool.FileUtil;
import nux.xom.pool.PoolConfig;
import nux.xom.pool.XOMUtil;

/**
 * Benchmark / stress test of {@link BuilderPool}.
 * <p>
 * Running this bench for our intended target usages indicates that W3C Schema Validation 
 * of small files with medium complex schemas is about 10-20 times faster when pooled, 
 * and that parsing without validation of said files is about 3-10 times faster when pooled
 * (JDK 1.5 server VM).
 * <p>
 * As always, your usecases and mileage may substantially vary - check against your own 
 * applications.
 * <p>
 * Example usage:
 * <pre>
 * java nux.xom.sandbox.BuilderPoolBenchmark pooled examples/ok2.xml validateschema 50000 nomemory 1 p2pio.xsd http://dsd.lbl.gov/p2pio-1.0
 * java nux.xom.sandbox.BuilderPoolBenchmark nopooled examples/ok2.xml validateschema 50000 nomemory 1 p2pio.xsd http://dsd.lbl.gov/p2pio-1.0
 * 
 * java nux.xom.sandbox.BuilderPoolBenchmark pooled examples/ok2.xml novalidateschema 100000 memory 1 p2pio.xsd http://dsd.lbl.gov/p2pio-1.0
 * java nux.xom.sandbox.BuilderPoolBenchmark nopooled examples/ok2.xml novalidateschema 100000 memory 1 p2pio.xsd http://dsd.lbl.gov/p2pio-1.0
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.1 $, $Date: 2005/11/11 23:18:28 $
 */
public final class BuilderPoolBenchmark {

//	private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(BuilderPoolBenchmark.class);
	
	private BuilderPoolBenchmark() {}

	public static void main(String[] args) throws Exception {
		int k = 0;
		final boolean isPooled = args[k++].equals("pooled");
		final String fileName = args[k++];
		final String validate = args[k++];
		final int runs = Integer.parseInt(args[k++]);
		final boolean memory = args[k++].equals("memory");
		final int numThreads = Integer.parseInt(args[k++]); // set this to 1 for normal use
		final String schema = args.length > k ? args[k++] : null;
		//final String schema = args.length > k ? getAbsoluteURI(args[k++]) : null;
		final String namespace = args.length > k ? args[k++] : null;
		boolean nullNodeFactory = args.length > k ? new Boolean(args[k++]).booleanValue() : false;
		
		if (!isPooled && validate.equals("validateschema")) { // disable pool
			System.setProperty("nux.xom.pool.BuilderPool.maxPoolEntries", "0");
		}
		System.out.println("fileName="+fileName);
		System.out.println("schema="+schema);
		System.out.println("namespace="+namespace);
		if (! new File(schema).exists()) throw new RuntimeException("File '"+schema+"' not found.");
		final HashMap map = new HashMap();
		map.put(new File(schema), namespace);
		//map.put(schema, namespace);
		
		byte[] b = new byte[0];
		if (memory && !fileName.equals("nofile")) b = FileUtil.toByteArray(new FileInputStream(fileName));
		final byte[] bytes = b;
		
		final BuilderPool pool = !nullNodeFactory ? BuilderPool.GLOBAL_POOL : new BuilderPool(new PoolConfig(), 
				new BuilderFactory() {
					protected NodeFactory createNodeFactory() {
						return XOMUtil.getNullNodeFactory();
					}
				}
			);
			
		// warmup hotspot VM
		for (int i=0; i < 100; i++) {
			new Builder(false);
			new Builder(true);
			pool.getW3CBuilder(map);			
		}
		System.gc();
		Thread.sleep(1000); // give hotspot some time to optimize

		// run the benchmark
		long start = System.currentTimeMillis();
		for (int i = 0; i < numThreads; i++) { // stress gc on threads and ThreadLocal via container-like scenario
			Thread thread = new Thread() {
				int checksum = 0; // make dead-code elimination impossible for hotspot vm
				public void run() {
					try {
						for (int i=0; i < runs; i++) { // user code inside multi-threaded container
							bench();
						}
					} catch (Exception e) { throw new RuntimeException(e); }				
				}
				private void bench() throws Exception {
					Builder builder;
					if (validate.equals("validateschema")) {
						builder = pool.getW3CBuilder(map); 
					}
					else {
						builder = isPooled ? 
							pool.getBuilder(validate.equals("validate")) : 
							new Builder(validate.equals("validate"));	
					}
					checksum += builder.hashCode();
					if (! fileName.equals("nofile")) {
						Document doc = memory ? 
								builder.build(new ByteArrayInputStream(bytes, 0, bytes.length)) :
								builder.build(new FileInputStream(fileName));
						//String str = doc.toXML();
						//System.out.print('.');
						checksum += doc.getChildCount();
					}				
				}
			};
			thread.start();
			thread.join();
		}
		
		// report average throughput
		long end = System.currentTimeMillis();
		System.out.println("\n" + runs + " runs took " + (end-start)/1000.0f + " secs");
		System.out.println("--> " + (numThreads * runs / ((end-start)/1000.0f)) + " iters/secs");
	}

	// hack to convert relative schema file names properly to an absolute URI,
	// taking care of Unix/Windows crossplatform issues
	private static String getAbsoluteURI(String fileOrURI) {
		URI uri = null;
		try {
			uri = new URI(fileOrURI);
//			log.debug("uri="+uri);
//			log.debug("uri.getPath="+uri.getPath());
		}
		catch (URISyntaxException e) {
//			log.debug("URISyntaxException", e);
			uri = new File(fileOrURI).toURI();
		}
		
//		log.debug("uriScheme="+ uri.getScheme());
		if (uri.getScheme() == null) {
			uri = new File(fileOrURI).toURI();
//			log.debug("uriFile="+ uri);
		}

		if (uri.getScheme().equals("file")) {
			if (! new File(uri.getPath()).exists()) throw new RuntimeException("File '"+fileOrURI+"' not found.");
		}
		
		return uri.toString();
	}
	
}

