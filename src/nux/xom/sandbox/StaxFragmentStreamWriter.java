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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * StAX XMLStreamWriter that delegates all calls to the given underlying child
 * writer, except that calls to all <code>close</code>,
 * <code>writeStartDocument</code> and <code>writeEndDocument</code>
 * flavours are silently ignored.
 * <p>
 * Consider SOAP frameworks (or similar), where the framework writes the SOAP
 * header to an XMLStreamWriter, and the application writes the SOAP body (i.e.
 * a fragment aka element subtree) via a {@link nux.xom.io.StreamingSerializer}
 * to the same XMLStreamWriter. The application needs a way to write a fragment
 * without writing START_DOCUMENT and END_DOCUMENT events, but
 * StreamingSerializer doesn't really allow that, in order to ensure
 * wellformedness.
 * <p>
 * This class enables StreamingSerializer applications to do just that, without
 * requiring weird and unsafe extensions to the StreamingSerializer interface.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.2 $, $Date: 2006/03/26 02:42:07 $
 */
public final class StaxFragmentStreamWriter extends StaxFilteredStreamWriter {

	/**
	 * Constructs an instance that delegates to the given child writer.
	 * 
	 * @param child
	 *            the writer to delegate to
	 */
	public StaxFragmentStreamWriter(XMLStreamWriter child) {
		super(child);
	}
	
	public void close() throws XMLStreamException {
		; // ignore
	}
	
	public void writeEndDocument() throws XMLStreamException {
		; // ignore
	}

	public void writeStartDocument() throws XMLStreamException {
		; // ignore
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		; // ignore
	}

	public void writeStartDocument(String encoding, String version) 
		throws XMLStreamException {
		; // ignore
	}

}
