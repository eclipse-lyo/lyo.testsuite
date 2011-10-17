/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
 *    Steve Speicher - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server.oslcv1tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;


import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * This class provides JUnit tests for the validation of the OSLC CM creation and updating of
 * change requests. It uses the template files specified in setup.properties as the entity
 * to be POST or PUT, for creation and updating respectively.
 * 
 * After each test, it attempts to perform a DELETE call on the resource that was presumably
 * created, but this DELETE call is not technically required in the OSLC CM spec, so the
 * created change request may still exist.
 */
@RunWith(Parameterized.class)
public class CreationAndUpdateTests {
	private static Credentials basicCreds;
	
	private String currentUrl;
	private String templatedDocument;
	private String updateDocument;
	private String jsonDocument;
	private String jsonUpdate;
	
	public CreationAndUpdateTests(String url)
	{
		this.currentUrl = url;
	}
	
	@Before
	public void setup() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		Properties setupProps = SetupProperties.setup(null);
		if (setupProps.getProperty("testBackwardsCompatability") != null &&
			Boolean.parseBoolean(setupProps.getProperty("testBackwardsCompatability")))
		{
			setupProps = SetupProperties.setup(setupProps.getProperty("version1Properties"));
		}
		String userId = setupProps.getProperty("userId");
		String pw =  setupProps.getProperty("pw");
		basicCreds = new UsernamePasswordCredentials(userId, pw);
		templatedDocument = OSLCUtils.readFileAsString(new File(setupProps.getProperty("createTemplateXmlFile")));
		updateDocument = OSLCUtils.readFileAsString(new File(setupProps.getProperty("updateTemplateXmlFile")));
		jsonDocument = OSLCUtils.readFileAsString(new File(setupProps.getProperty("createTemplateJsonFile")));
		jsonUpdate = OSLCUtils.readFileAsString(new File(setupProps.getProperty("updateTemplateJsonFile")));
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//OSLC CM simple query services of the REST service.
		Properties setupProps = SetupProperties.setup(null);
		Collection<Object[]> coll = getReferencedUrls(setupProps.getProperty("baseUri"));
		return coll;
	}
	
	public static Collection<Object[]> getReferencedUrls(String base) throws IOException, XPathException, ParserConfigurationException, SAXException
	{
		Properties setupProps = SetupProperties.setup(null);
		String userId = setupProps.getProperty("userId");
		String pw =  setupProps.getProperty("pw");
		
		HttpResponse resp = OSLCUtils.getResponseFromUrl(base, base, new UsernamePasswordCredentials(userId, pw),
				OSLCConstants.CT_DISC_CAT_XML + ", " + OSLCConstants.CT_DISC_DESC_XML);
		
		//If our 'base' is a ServiceDescription, find and add the factory service url
		if (resp.getEntity().getContentType().getValue().contains(OSLCConstants.CT_DISC_DESC_XML))
		{
			Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(EntityUtils.toString(resp.getEntity()));
			Node factoryUrl = (Node) OSLCUtils.getXPath().evaluate("//oslc_cm:factory/oslc_cm:url", baseDoc,
		    		XPathConstants.NODE);
			Collection<Object[]> data = new ArrayList<Object[]>();
			data.add(new Object[] { factoryUrl.getTextContent()});
			return data;
		}
		
		Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(EntityUtils.toString(resp.getEntity()));
		
	    //ArrayList to contain the urls from all of the SPCs
	    Collection<Object[]> data = new ArrayList<Object[]>();
	    
	    //Get all the ServiceDescriptionDocuments from this ServiceProviderCatalog
	    NodeList sDescs = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:services/@rdf:resource", baseDoc,
	    		XPathConstants.NODESET);
	    for (int i = 0; i < sDescs.getLength(); i++)
	    {
	    	Collection<Object[]> subCollection = getReferencedUrls(sDescs.item(i).getNodeValue());
    		Iterator<Object[]> iter = subCollection.iterator();
    		while (iter.hasNext())
    		{
    			data.add(iter.next());
    		}
	    }
	    
	    //Get all ServiceProviderCatalog urls from the base document in order to recursively add all the
	    //simple query services from the eventual service description documents from them as well.
	    NodeList spcs = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:entry/oslc_disc:ServiceProviderCatalog/@rdf:about",
	    		baseDoc, XPathConstants.NODESET);
	    for (int i = 0; i < spcs.getLength(); i++)
	    {
	    	if (!spcs.item(i).getNodeValue().equals(base))
	    	{
	    		Collection<Object[]> subCollection = getReferencedUrls(spcs.item(i).getNodeValue());
	    		Iterator<Object[]> iter = subCollection.iterator();
	    		while (iter.hasNext())
	    		{
	    			data.add(iter.next());
	    		}
	    	}
	    }	    
		return data;
	}
	
	@Test
	public void createValidCMDefectUsingXmlTemplate() throws IOException
	{
		//Issue post request using the provided template
		//Using Content-type header of OSLCConstants as required by the OSLC CM spec
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_XML, 
				OSLCConstants.CT_CR_XML, templatedDocument);

		//Assert the response gave a 201 Created
		resp.getEntity().consumeContent();
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine().getStatusCode());
		Header location = resp.getFirstHeader("Location");
		//Assert that we were given a Location header pointing to the resource
		assertFalse(location == null);
		//Attempt to clean up after the test by calling delete on the given url,
		//which is not a MUST according to the oslc cm spec
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "*/*");
		if (resp.getEntity() != null)
		{
			resp.getEntity().consumeContent();
		}
	}
	
	@Test
	public void createValidCMDefectUsingJsonTemplate() throws IOException
	{
		//Issue post request using the provided template
		//Using Content-type header of OSLCConstants as required by the OSLC CM spec
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_JSON, 
				OSLCConstants.CT_CR_JSON, jsonDocument);

		//Assert the response gave a 201 Created
		resp.getEntity().consumeContent();
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine().getStatusCode());
		Header location = resp.getFirstHeader("Location");
		
		//Assert that we were given a Location header pointing to the resource
		assertNotNull(location);
		//Attempt to clean up after the test by calling delete on the given url,
		//which is not a MUST according to the oslc cm spec
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "/*");
		if (resp.getEntity() != null)
		{
			resp.getEntity().consumeContent();
		}
	}
	
	@Test
	public void createCMDefectWithInvalidContentType() throws IOException
	{
		//Issue post request using the provided template and an invalid contentType
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_XML, 
				"weird/type", templatedDocument);
		resp.getEntity().consumeContent();
		assertEquals(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, resp.getStatusLine().getStatusCode());
	}
	
	@Test
	public void createCMDefectWithInvalidContent() throws IOException
	{
		//Issue post request using the provided template and an invalid contentType
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_XML, 
				OSLCConstants.CT_CR_XML, "notvalidxmldefect");
		resp.getEntity().consumeContent();
		assertEquals(HttpStatus.SC_BAD_REQUEST, resp.getStatusLine().getStatusCode());
	}
	
	@Test
	public void createCMDefectAndUpdateItUsingXml() throws IOException
	{
		//Issue post request using the provided template
		//Using Content-type header of OSLCConstants as required by the OSLC CM spec
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_XML, 
				OSLCConstants.CT_CR_XML, templatedDocument);

		//Assert the response gave a 201 Created
		resp.getEntity().consumeContent();
		assertEquals(HttpStatus.SC_CREATED, resp.getStatusLine().getStatusCode());
		Header location = resp.getFirstHeader("Location");
		//Assert that we were given a Location header pointing to the resource
		assertNotNull(location);
		
		//Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, OSLCConstants.CT_CR_XML, OSLCConstants.CT_CR_XML, updateDocument);
		if (resp.getEntity() != null)
		{
			resp.getEntity().consumeContent();
		}
		//Assert that a proper PUT resulted in a 200 OK
		assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
		
		//Clean up after the test by attempting to delete the created resource
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "*/*");
		if (resp.getEntity() != null)
			resp.getEntity().consumeContent();
	}
	
	@Test
	public void createCMDefectAndUpdateItUsingJson() throws IOException
	{
		//Issue post request using the provided template
		//Using Content-type header of OSLCConstants as required by the OSLC CM spec
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_JSON, 
				OSLCConstants.CT_CR_JSON, jsonDocument);

		//Assert the response gave a 201 Created
		resp.getEntity().consumeContent();
		assertTrue(resp.getStatusLine().getStatusCode() == 201);
		Header location = resp.getFirstHeader("Location");
		
		//Assert that we were given a Location header pointing to the resource
		assertFalse(location == null);
		
		//Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, OSLCConstants.CT_CR_JSON, OSLCConstants.CT_CR_JSON, jsonUpdate);
		if (resp.getEntity() != null)
		{
			resp.getEntity().consumeContent();
		}
		//Assert that a proper PUT resulted in a 200 OK
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
		
		//Clean up after the test by attempting to delete the created resource
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "*/*");
		if (resp.getEntity() != null)
			resp.getEntity().consumeContent();
	}
	
	@Test
	public void updateCreatedDefectWithBadRequest() throws IOException
	{
		//Issue post request using the provided template
		//Using Content-type header of OSLCConstants as required by the OSLC CM spec
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_XML, 
				OSLCConstants.CT_CR_XML, templatedDocument);

		//Assert the response gave a 201 Created
		resp.getEntity().consumeContent();
		assertTrue(resp.getStatusLine().getStatusCode() == 201);
		Header location = resp.getFirstHeader("Location");
		//Assert that we were given a Location header pointing to the resource
		assertFalse(location == null);
		
		//Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, "*/*", OSLCConstants.CT_CR_XML, "NOTVALIDXML");
		if (resp.getEntity() != null)
		{
			resp.getEntity().consumeContent();
		}
		//Assert that an invalid PUT resulted in a 400 BAD REQUEST
		assertTrue(resp.getStatusLine().getStatusCode() == 400);
		
		//Clean up after the test by attempting to delete the created resource
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "");
		if (resp.getEntity() != null)
			resp.getEntity().consumeContent();
	}
	
	@Test
	public void updateCreatedDefectWithBadType() throws IOException
	{
		//Issue post request using the provided template
		//Using Content-type header of OSLCConstants as required by the OSLC CM spec
		HttpResponse resp = OSLCUtils.postDataToUrl(currentUrl, basicCreds, OSLCConstants.CT_CR_XML, 
				OSLCConstants.CT_CR_XML, templatedDocument);

		//Assert the response gave a 201 Created
		resp.getEntity().consumeContent();
		assertTrue(resp.getStatusLine().getStatusCode() == 201);
		Header location = resp.getFirstHeader("Location");
		
		//Assert that we were given a Location header pointing to the resource
		assertFalse(location == null);
		
		//Now, go to the url of the new change request and update it.
		resp = OSLCUtils.putDataToUrl(location.getValue(), basicCreds, "*/*", "text/html", updateDocument);
		if (resp.getEntity() != null)
		{
			resp.getEntity().consumeContent();
		}
		//Assert that an invalid PUT resulted in a 400 BAD REQUEST
		assertTrue(resp.getStatusLine().getStatusCode() == 415);
		
		//Clean up after the test by attempting to delete the created resource
		resp = OSLCUtils.deleteFromUrl(location.getValue(), basicCreds, "");
		if (resp.getEntity() != null)
			resp.getEntity().consumeContent();
	}
}
