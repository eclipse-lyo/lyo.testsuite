/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution. 
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *    Wu Kai
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;

public abstract class OSLCCoreVersionTestBase extends TestsBase {

	@SuppressWarnings("rawtypes")
	public static String[] getCreateTemplateTypes() throws FileNotFoundException {
		if (rdfXmlUpdateTemplate == null) {
			return null;
		}

		Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(rdfXmlUpdateTemplate), "http://base.url", OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(m);
		Property rdfType = m.getProperty(OSLCConstants.RDF_TYPE_PROP);
		Selector select = new SimpleSelector(null, rdfType, (RDFNode)null); 
		List l = m.listStatements(select).toList();
		String[] types = new String[l.size()];
		for (int i=0; i<l.size(); i++) {
			types[i] = ((Statement)l.get(i)).getObject().toString();
		}
		return types;
	}
	
	public OSLCCoreVersionTestBase(String thisUrl) {
		super(thisUrl);

		staticSetup();
	}
	/**
	 * Gets the content type for these tests. The type will be used for both the
	 * Accept header and the Content-Type header in POST and PUT requests.
	 * 
	 * @return the content type
	 */
	public abstract String getContentType();
    
	@Test
	public void verifyOslcCoreVersionCurrentVersionV2() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "2.0");
		assertEquals("2.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionSubversionOfV2() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "2.0.2");
		assertEquals("2.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionOldMainVersion() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "1.0");
		assertEquals("2.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionNoHeader() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), null);
		assertEquals("1.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionFutureVersion() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "4.0");
		assertEquals("2.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionIligalVersion() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "-1.0");
		assertEquals("2.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionEmptyValue() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "");
		assertEquals("2.0", version);
	}
	
	@Test
	public void verifyOslcCoreVersionOldSubVersion() throws ClientProtocolException, IOException{
		String version = OSLCUtils.getOslcVersion(setupBaseUrl, basicCreds, getContentType(), "1.0.0.1");
		assertEquals("2.0", version);
	}
}
