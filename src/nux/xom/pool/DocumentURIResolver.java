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

import java.io.IOException;

import javax.xml.transform.TransformerException;

import nu.xom.Document;
import nu.xom.ParsingException;

/**
 * Called by the XQuery processor to turn a URI passed to the XQuery <code>doc()</code> 
 * function into a XOM {@link Document} object.
 * <p>
 * This interface allows to intercept and customize URI resolution. For example
 * here one can plugin XML schema validation, constraint checking, {@link nu.xom.xinclude.XIncluder},
 * streaming path filters such as {@link nux.xom.xquery.StreamingPathFilter}, 
 * other preprocessing steps, and/or document caching (e.g. using 
 * {@link nux.xom.binary.BinaryXMLCodec} with {@link nux.xom.pool.DocumentPool}, 
 * <a target="_blank"
 * href="http://ehcache.sourceforge.net/">EHCache</a>, <a target="_blank"
 * href="http://www.opensymphony.com/oscache/">OSCache</a> or similar products).
 * <p>
 * Via a {@link ResourceResolver} one might also read a document from the web, 
 * a classpath, a jar file, a war file, a JDBC database, a JNDI context/data
 * source, or similar.
 * <p>
 * This callback interface is just like the
 * {@link javax.xml.transform.URIResolver}interface, except that it returns a
 * <code>Document</code> instead of a <code>Source</code> object.
 * <p>
 * For example, a simple implementation could look like this:
 * 
 * <pre>
 *     String systemID = new net.sf.saxon.StandardURIResolver().resolve(href, baseURI).getSystemId();
 *     return BuilderPool.GLOBAL_POOL.getBuilder(false).build(systemID);
 *     //return BuilderPool.GLOBAL_POOL.getW3CBuilder(...).build(systemID);
 * 
 *     // a variant using a document pool:
 *     String systemID = new net.sf.saxon.StandardURIResolver().resolve(href, baseURI).getSystemId();
 *     return DocumentPool.GLOBAL_POOL.getDocument(new URI(systemID));
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.19 $, $Date: 2005/06/09 22:48:26 $
 */
public interface DocumentURIResolver {

    /**
	 * Called by the XQuery processor when it encounters a <code>doc()</code> function.
	 * 
	 * @param href
	 *            An href attribute, which may be relative or absolute.
	 * @param baseURI
	 *            The (absolute) base URI in effect when the href attribute was
	 *            encountered.
	 * 
	 * @return A Document object, or null if the href cannot be resolved and the
	 *         processor should try to resolve the URI itself.
	 * 
	 * @throws ParsingException
	 *             if an error occurs when trying to resolve the URI.
	 * @throws IOException
	 *             if an error occurs when trying to resolve the URI.
	 * @throws TransformerException
	 *             if an error occurs when trying to resolve the URI.
	 */
	public Document resolve(String href, String baseURI) 
		throws ParsingException, IOException, TransformerException;

}
