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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import nu.xom.Document;
import nu.xom.NodeFactory;
import nu.xom.ParsingException;
import nu.xom.xslt.XSLException;
import nu.xom.xslt.XSLTransform;

/**
 * Creates and returns new <code>XSLTransform</code> objects using flexible parametrization (thread-safe). 
 * TRAX factories, attributes and {@link URIResolver} of the underlying TRAX 
 * {@link TransformerFactory} can be specified by overriding the protected 
 * <code>getPreferredTransformerFactories</code> and <code>initFactory</code> methods.
 * <p>
 * For anything but simple/basic use cases, this API is more robust,
 * configurable and convenient than the underlying XOM XSLTransform constructor API.
 * <p>
 * This implementation is thread-safe.
 * <p>
 * Example usage:
 * <pre>
 *   // without custom factories:
 *   XSLTransform trans = new XSLTransformFactory().createTransform(new File("/tmp/test.xsl"));
 *   Document doc = BuilderPool.GLOBAL_POOL.getBuilder(false).build(new File("/tmp/test.xml"));
 *   Nodes nodes = trans.transform(doc);
 *   for (int i=0; i < nodes.size(); i++) {
 *      System.out.println("node "+i+": "+nodes.get(i).toXML());
 *   }
 * 
 *   // with custom factories:
 *   XSLTransformFactory factory = new XSLTransformFactory() {
 *     protected String[] getPreferredTransformerFactories() {
 *       return new String[] {
 *           "net.sf.saxon.TransformerFactoryImpl",
 *           "org.apache.xalan.processor.TransformerFactoryImpl"
 *       };
 *     }
 *   };
 *   XSLTransform trans = factory.createTransform(new File("/tmp/test.xsl"));
 *   Document doc = BuilderPool.GLOBAL_POOL.getBuilder(false).build(new File("/tmp/test.xml"));
 *   Nodes nodes = trans.transform(doc);
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.14 $, $Date: 2005/12/05 06:53:05 $
 */
public class XSLTransformFactory {
	
	private static final String[] DEFAULT_TRAX_FACTORIES = {
		"org.apache.xalan.processor.TransformerFactoryImpl", // slow but mostly correct
		"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", // fast and mostly correct
		"net.sf.saxon.TransformerFactoryImpl", // can do XSLT 2.0 and XQuery 1.0
		"org.apache.xalan.xsltc.trax.TransformerFactoryImpl", // fast but buggy
		"com.icl.saxon.TransformerFactoryImpl",
		"oracle.xml.jaxp.JXSAXTransformerFactory",
		"jd.xml.xslt.trax.TransformerFactoryImpl",	
	};
	
	// A TRAX TransformerFactory is not thread-safe, so we use a ThreadLocal to make it thread-safe
	private final ThreadLocal threadLocal;
	
	private final DocumentFactory factory = new DocumentFactory();
	
	private static final boolean DEBUG = 
		XOMUtil.getSystemProperty("nux.xom.pool.XSLTransformFactory.debug", false);
	
	/**
	 * Creates a factory instance.
	 */
	public XSLTransformFactory() {
		this.threadLocal = new SoftThreadLocal() {
			protected Object initialSoftValue() { // lazy init
				// find a TRAX factory that works
				final String[] traxFactories = getPreferredTransformerFactories();
				for (int i = 0; traxFactories != null && i < traxFactories.length; i++) {
//					if (DEBUG) System.err.println("trying TRAX=" + traxFactories[i]);
					try {
						TransformerFactory factory = (TransformerFactory) ClassLoaderUtil.newInstance(traxFactories[i]);
						initFactory(factory);
						if (DEBUG) System.err.println("using TRAX TransformerFactory=" + factory.getClass().getName());
						return factory;
					} catch (ClassNotFoundException e) {
						continue; // keep on trying
					} catch (NoClassDefFoundError err) {
						continue; // keep on trying
					} catch (Throwable e) {
						continue; // keep on trying
					}
				}
				
				// try default TRAX initialization
//				if (DEBUG) System.err.println("trying default TRAX");
				TransformerFactory factory = TransformerFactory.newInstance();
				initFactory(factory);
				if (DEBUG) System.err.println("using default TRAX TransformerFactory=" + factory.getClass().getName());
				return factory;
			}
		};
	}
	
	/**
	 * Creates and returns a new <code>XSLTransform</code> for the given stylesheet.
	 * 
	 * @param stylesheet
	 *            the stylesheet to compile
	 * @return an XSL transform
	 * @throws XSLException
	 *             if the XSLTransform can't be created, e.g. because of an XSL syntax error.
	 */
	public XSLTransform createTransform(Document stylesheet) throws XSLException {
		return newTransform(stylesheet, (TransformerFactory) threadLocal.get());
	}

	/**
	 * Creates and returns a new <code>XSLTransform</code> for the given stylesheet.
	 * 
	 * @param stylesheet
	 *            the stylesheet to compile
	 * @return an XSL transform
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if the stylesheet is not well-formed XML
	 * @throws XSLException
	 *             if the XSLTransform can't be created, e.g. because of an XSL syntax error.
	 */
	public XSLTransform createTransform(File stylesheet) throws XSLException, ParsingException, IOException {
		return createTransform(factory.createDocument(stylesheet));
	}

	/**
	 * Creates and returns a new <code>XSLTransform</code> for the given stylesheet.
	 * 
	 * @param stylesheet
	 *            the stylesheet to compile
	 * @param baseURI
	 *            the (absolute) baseURI of the stylesheet (may be <code>null</code>)
	 *            Need not be the stream's actual URI.
	 * @return an XSL transform
	 * @throws IOException
	 *             if an I/O error occurs while reading from the stream
	 * @throws ParsingException
	 *             if the stylesheet is not well-formed XML
	 * @throws XSLException
	 *             if the XSLTransform can't be created, e.g. because of an XSL syntax error.
	 */
	public XSLTransform createTransform(InputStream stylesheet, URI baseURI) throws XSLException, ParsingException, IOException {
		return createTransform(factory.createDocument(stylesheet, baseURI));
	}
	
	/**
	 * Callback that creates and returns a new <code>XSLTransform</code> for the given
	 * stylesheet and TransformerFactory.
	 * <p>
	 * Override this method if you want to create custom subclasses of XSLTransform.
	 * 
	 * @param stylesheet
	 *            the stylesheet to compile
	 * @param transformerFactory
	 *            the TransformerFactory
	 * @return an XSL transform
	 * @throws XSLException
	 *             if no XSLTransform can be obtained for the given stylesheet;
	 *             in particular when the supplied stylesheet is not
	 *             syntactically correct XSLT
	 */
	protected XSLTransform newTransform(Document stylesheet, TransformerFactory transformerFactory) throws XSLException {
		return new XSLTransform(stylesheet, new NodeFactory(), transformerFactory);
	}
	
	/**
	 * Callback that returns a search list of fully qualified class names of TRAX
	 * {@link TransformerFactory} implementations, given in order of
	 * preference from left to right. May return <code>null</code>.
	 * <p>
	 * Override this method for custom behaviour. 
	 * This default implementation returns a search list for the most popular 
	 * TRAX implementations.
	 * 
	 * @return a search list
	 */
	protected String[] getPreferredTransformerFactories() {
		return DEFAULT_TRAX_FACTORIES;
	}

	/**
	 * Callback that initializes the supplied TransformerFactory with
	 * application-specific attributes and a URIResolver, if so desired.
	 * <p>
	 * Override this method if you need custom attributes/resolvers. This
	 * default implementation does nothing.
	 * <p>
	 * Note: Attributes and resolver are not part of the constructor because
	 * they may well be stateful and mutable, hence it may well be unsafe to
	 * share them among multiple XSLTransforms in a multi-threaded context. By
	 * providing this method, an application can create attributes/resolvers as
	 * needed via straightforward subclassing/overriding of this method.
	 * 
	 * @param factory
	 *            the factory to initialize
	 */
	protected void initFactory(TransformerFactory factory) {
	}
	
}