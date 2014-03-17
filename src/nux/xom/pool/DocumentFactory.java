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
package nux.xom.pool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nux.xom.binary.BinaryXMLCodec;

/**
 * Creates and returns new <code>Document</code> objects using flexible
 * parametrization (thread-safe).
 * 
 * This implementation is thread-safe.
 * 
 * @see Builder
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.19 $, $Date: 2005/12/05 06:53:05 $
 */
public class DocumentFactory {
	
	/**
	 * Creates a factory instance.
	 */
	public DocumentFactory() {}
	
	/**
	 * Creates and returns a new document for the given input file.
	 * 
	 * @param input
	 *            the file to read from
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if an XML parsing error occurs
	 * @return a new document
	 */
	public Document createDocument(File input) throws ParsingException, IOException {
		return createDocument(new FileInputStream(input), input.toURI());
	}

	/**
	 * Creates and returns a new document for the given input stream and base
	 * URI.
	 * <p>
	 * At least one of the parameters <code>input</code> and
	 * <code>baseURI</code> must not be <code>null</code>.
	 * 
	 * @param input
	 *            the stream to read from (may be <code>null</code>)
	 * @param baseURI
	 *            the base URI of the document (may be <code>null</code>)
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if an XML parsing error occurs
	 * @return a new document
	 */
	public Document createDocument(InputStream input, URI baseURI) 
			throws ParsingException, IOException {
		
		try {
			if (input == null && baseURI == null) 
				throw new IllegalArgumentException("input and baseURI must not both be null");
			
			Builder builder = newBuilder();
			if (baseURI == null)
				return builder.build(input);
			else
				return builder.build(input, baseURI.toASCIIString());			
		}
		finally { // better safe than sorry
			if (input != null) input.close();
		}
	}
	
	/**
	 * Overridable callback that returns a Builder for XML parsing;
	 * This default implementation returns a non-validating Builder.
	 *  
	 * @return a builder
	 */
	protected Builder newBuilder() {
		return BuilderPool.GLOBAL_POOL.getBuilder(false);
	}
	
	/**
	 * Returns a factory that reads binary XML files using {@link BinaryXMLCodec}.
	 *  
	 * @return a binary XML factory
	 */
	public DocumentFactory getBinaryXMLFactory() {
		return new BinaryXMLDocumentFactory(this);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// Nested classes:
	///////////////////////////////////////////////////////////////////////////////
	private static final class BinaryXMLDocumentFactory extends DocumentFactory {
		
		private final DocumentFactory parent;
		
		private BinaryXMLDocumentFactory(DocumentFactory parent) {
			this.parent = parent;
		}
		
		public Document createDocument(InputStream input, URI baseURI) 
			throws ParsingException, IOException {
			
			if (input == null && baseURI == null) 
				throw new IllegalArgumentException("input and baseURI must not both be null");
			if (input == null) input = baseURI.toURL().openStream();
			try {
				Document doc = XOMUtil.getBinaryXMLCodec().deserialize(input, null);
				if (baseURI != null) doc.setBaseURI(baseURI.toASCIIString());
				return doc;
			} finally {
				input.close(); // do what SAX XML parsers do
			}
		}
		
	}
	
}