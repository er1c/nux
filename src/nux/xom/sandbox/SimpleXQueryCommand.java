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
import nu.xom.Nodes;
import nux.xom.pool.XOMUtil;
import nux.xom.pool.XQueryPool;
import nux.xom.xquery.XQuery;

/**
 * Simple command line demo that runs a given XQuery against a file and
 * prints the result sequence. A query can be read from a file or given inline
 * between curly braces. This demo is for new users to get
 * started, and not intended for production use.
 * <p>
 * Example usage (using Unix shell quoting with the ' character):
 * 
 * <pre>
 * export CLASSPATH=lib/nux.jar:lib/saxon8.jar:lib/xom.jar
 * 
 * java nux.xom.sandbox.SimpleXQueryCommand '{doc("samples/data/periodic.xml")/PERIODIC_TABLE/ATOM[NAME = "Zinc"]}'
 * java nux.xom.sandbox.SimpleXQueryCommand '{count(//*)}' samples/data/periodic.xml 
 * java nux.xom.sandbox.SimpleXQueryCommand samples/xmark/q03.xq samples/xmark/auction-0.01.xml
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.1 $, $Date: 2005/11/11 23:18:28 $
 */
public final class SimpleXQueryCommand  {
	
	private SimpleXQueryCommand() {}
	
	public static void main(String[] args) throws Exception {		
		String query = args[0];
		Document doc = null;
		if (args.length > 1) doc = new Builder().build(new File(args[1]));
		
		XQuery xquery;
		if (query.startsWith("{") && query.endsWith("}")) { 
			// query is given inline between curly brackets, ala Saxon command line tool
			String strippedQuery = query.substring(1, query.length()-1);
			xquery = XQueryPool.GLOBAL_POOL.getXQuery(strippedQuery, null);
		} else { 
			// otherwise it refers to a file containing the query string
			xquery = XQueryPool.GLOBAL_POOL.getXQuery(new File(query));
		}
		
		Nodes results = xquery.execute(doc).toNodes();
		
		for (int j=0; j < results.size(); j++) {
			boolean prettyPrint = true;
			if (prettyPrint)
				System.out.println(XOMUtil.toPrettyXML(results.get(j)));
			else 
				System.out.println(results.get(j).toXML());
		}
	}
}
