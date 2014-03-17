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
package nux.xom.io;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLStreamWriter;

import nux.xom.binary.BinaryXMLCodec;

/**
 * Factory creating instances of {@link StreamingSerializer} implementations.
 * <p>
 * Currently there are three implementations. One writes a standard textual XML
 * document via a thin layer on top of the normal XOM {@link nu.xom.Serializer}.
 * Another writes a bnux binary XML document via a {@link BinaryXMLCodec}.
 * Yet another delegates to an underlying StAX {@link XMLStreamWriter}.
 * Future releases may include implementations that delegate to SAX and DOM.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.24 $, $Date: 2006/05/01 18:53:35 $
 */
public class StreamingSerializerFactory {
	
	/**
	 * Constructs a new factory instance; The serializer instances it creates
	 * can be reused serially, but are not thread safe.
	 */
	public StreamingSerializerFactory() {}
	
	/**
	 * Returns a new streaming serializer that writes bnux binary
	 * XML to the given underlying output stream, using the given ZLIB
	 * compression level.
	 * <p>
	 * An optional zlib compression level ranging from 0 (no ZLIB compression;
	 * best performance) to 1 (little ZLIB compression; reduced performance) to
	 * 9 (strongest ZLIB compression; worst performance) allows one to configure
	 * the CPU/memory consumption trade-off.
	 * <p>
	 * Unless there is a good reason to the contrary, you should always use
	 * level 0: the bnux algorithm typically already precompresses considerably.
	 * 
	 * @param out
	 *            the underlying output stream to write to
	 * @param zlibCompressionLevel
	 *            a number in the range 0..9
	 * @return a streaming serializer that writes bnux binary XML
	 */
	public StreamingSerializer createBinaryXMLSerializer(OutputStream out, int zlibCompressionLevel) {
//		BinaryXMLCodec codec = XOMUtil.getBinaryXMLCodec();
		BinaryXMLCodec codec = new BinaryXMLCodec();
		return codec.createStreamingSerializer(out, zlibCompressionLevel);
	}

	/**
	 * Returns a new streaming serializer that writes standard textual XML to
	 * the given underlying output stream, using the given character encoding
	 * for char to byte conversions.
	 * <p>
	 * The standard XOM {@link nu.xom.Serializer} options are used, in
	 * particular {@link nu.xom.Serializer#setIndent(int)} with argument zero,
	 * i.e. no extra indentation.
	 * 
	 * @param out
	 *            the underlying stream to write to
	 * @param encoding
	 *            the character encoding to use (e.g. "UTF-8")
	 * @return a new streaming serializer
	 * @see nu.xom.Serializer#Serializer(OutputStream, String)
	 */
	public StreamingSerializer createXMLSerializer(OutputStream out, String encoding) {
		if (encoding == null) encoding = "UTF-8";
		try {
			return new StreamingXMLSerializer(out, encoding);
		} catch (UnsupportedEncodingException e) {
			// doesn't happen often enough to warrant a checked exception
			throw new RuntimeException(e); 
		}
	}

	/**
	 * Returns a new streaming serializer that writes standard textual XML to
	 * the given underlying StAX XMLStreamWriter.
	 * <p>
	 * In theory, any StAX implementation will work, but in practise Woodstox is
	 * the only StAX implementation known to be exceptionally conformant, reliable,
	 * complete <i>and</i> efficient.
	 * <p>
	 * The returned instance auto-suppresses redundant namespace declarations
	 * irrespective of the underlying StAX implementation. Consequently, XML
	 * output will be identical irrespective of the XMLStreamWriter's
	 * {@link javax.xml.stream.XMLOutputFactory#IS_REPAIRING_NAMESPACES 
	 * namespace repairing mode} and javax.xml.stream.isPrefixDefaulting mode.
	 * In other words, there's no need for a user to specify these modes, and
	 * there's no need to change the default settings of the underlying StAX
	 * implementation. The returned StreamingSerializer "does the right thing"
	 * no matter what.
	 * 
	 * @param writer
	 *            the underlying StAX XMLStreamWriter to write to
	 * @return a new streaming serializer
	 * @see javax.xml.stream.XMLOutputFactory
	 */
	public StreamingSerializer createStaxSerializer(XMLStreamWriter writer) {
		return new StreamingStaxSerializer(writer);
	}

//	public StreamingSerializer createStaxSerializer(XMLStreamWriter writer, boolean isFragmentMode) {
//		if (isFragmentMode) writer = new nux.xom.sandbox.StaxFragmentStreamWriter(writer);
//		return new StreamingStaxSerializer(writer);
//	}

}