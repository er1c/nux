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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nux.xom.pool.BuilderPool;
import nux.xom.pool.XOMUtil;

/**
 * Simple quick n'dirty helper to load and save benchmark data across VM
 * invocations, and finally export it to XML and/or CSV for import into MS-Excel
 * or similar .
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2006/01/08 00:58:30 $
 */
class XMLMatrix { // TODO: public or not?
	
	private Document doc;
	private File file;
	
	public XMLMatrix(File file, boolean append) throws ParsingException, IOException {
		this.file = file;
		if (append && file.exists()) {
			this.doc = BuilderPool.GLOBAL_POOL.getBuilder(false).build(file);
		} else {
			this.doc = new Document(new Element(file.getName()));
		}
	}
	
	public void put(String rowName, String columnName, String value) {
		Elements rows = doc.getRootElement().getChildElements(rowName);
		if (rows.size() == 0) {
			doc.getRootElement().appendChild(new Element(rowName));
			rows = doc.getRootElement().getChildElements(rowName);
		}
		
		Elements cols = rows.get(0).getChildElements(columnName);
		if (cols.size() == 0) {
			rows.get(0).appendChild(new Element(columnName));
			cols = rows.get(0).getChildElements(columnName);
		}
		
		Element col = cols.get(0);
		col.removeChildren();
		col.appendChild(value);
	}
	
	public String get(String rowName, String columnName) {
		Elements rows = doc.getRootElement().getChildElements(rowName);
		if (rows.size() > 0) {
			Elements cols = rows.get(0).getChildElements(columnName);
			if (cols.size() > 0) {
				return cols.get(0).getValue();
			}
		} 
		
		return "";
	}
	
	// Saves data as a simple XML file
	public void saveAsXML() throws IOException {
		if (!file.exists()) file.getParentFile().mkdirs();
		
		OutputStream out = new FileOutputStream(file);
		Serializer ser = new Serializer(out);
		ser.setIndent(4);
		ser.write(doc);
		out.close();
	}
	
	// Save data as Comma Separated Values for import into MS-Excel or similar
	public void saveAsCSV() throws IOException {
		if (!file.exists()) file.getParentFile().mkdirs();
		
		// TODO: should really be a Writer
		OutputStream out = new FileOutputStream(new File(file.getParentFile(), file.getName() + ".csv"));
		out.write(toCSV().getBytes());
		out.flush();
		out.close();
	}
	
	public String toString() {
		return XOMUtil.toPrettyXML(doc);
	}
	
	public String[] columnNames() {
		LinkedHashSet names = new LinkedHashSet();
		Elements rows = doc.getRootElement().getChildElements();
		for (int i=0; i < rows.size(); i++) {
			Elements cols = rows.get(i).getChildElements();
			for (int j=0; j < cols.size(); j++) {
				names.add(cols.get(j).getLocalName());
			}
		}
		
		String[] columnNames = new String[names.size()];
		names.toArray(columnNames);
		return columnNames;
	}
	
	public String[] rowNames() {
		Elements rows = doc.getRootElement().getChildElements();
		String[] rowNames = new String[rows.size()];
		for (int i=0; i < rowNames.length; i++) {
			rowNames[i] = rows.get(i).getLocalName();
		}
		return rowNames;
	}
	
	public String toCSV() {
		String[] rowNames = rowNames();
		String[] columnNames = columnNames();
		StringBuffer buf = new StringBuffer();
		
		for (int j=0; j < columnNames.length; j++) {
			buf.append(',');
			buf.append(columnNames[j]);
		}
		
		for (int i=0; i < rowNames.length; i++) {
			buf.append('\r');
			buf.append(rowNames[i]);
			for (int j=0; j < columnNames.length; j++) {
				buf.append(',');
				buf.append(get(rowNames[i], columnNames[j]));
			}
		}
		
		return buf.toString();
	}
	
//	public static void main(String[] args) throws Exception {
//		XMLMatrix matrix = new XMLMatrix(new File("/tmp/nuxbench/test"), false);
//		matrix.put("periodic.xml", "bnux0", "10.0");
//		matrix.put("periodic.xml", "xom", "20.0");
//		matrix.put("periodic.xml", "bnux0", "30.0");
//		matrix.put("soap.xml", "bnux0", "300.0");
//		System.out.println(matrix);
//		System.out.println("csv="+matrix.toCSV());
//		matrix.saveAsXML();
//		matrix.saveAsCSV();
//	}
		
}