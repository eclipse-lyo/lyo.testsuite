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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;


import org.apache.http.HttpResponse;
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
 * This class provides JUnit tests for the validation of OSLC ServiceDescriptionDocuments,
 * currently focusing on the validation of the OSLC_CM definition of ServiceDescriptionDocument.
 */
@RunWith(Parameterized.class)
public class ServiceDescriptionTests {
	
	//Base URL of the OSLC Service Description Document to be tested
	private static String baseUrl;
	private static Credentials basicCreds;
	
	private String currentUrl;	
	private HttpResponse response;
	private String responseBody;
	private Document doc;
	
	public ServiceDescriptionTests(String url)
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
		baseUrl = setupProps.getProperty("baseUri");
		String userId = setupProps.getProperty("userId");
		String pw =  setupProps.getProperty("pw");
		basicCreds = new UsernamePasswordCredentials(userId, pw);
        response = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds, OSLCConstants.CT_DISC_DESC_XML);
        responseBody = EntityUtils.toString(response.getEntity());
        //Get XML Doc from response
	    doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//service description documents of the REST service.
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
		
		//If our 'base' is a ServiceDescription we don't need to recurse as this is the only one we can find
		if (resp.getEntity().getContentType().getValue().contains(OSLCConstants.CT_DISC_DESC_XML))
		{
			Collection<Object[]> data = new ArrayList<Object[]>();
			data.add(new Object[] { base });
			EntityUtils.consume(resp.getEntity());
			return data;
		}
		
		Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(EntityUtils.toString(resp.getEntity()));
		
	    //ArrayList to contain the urls from all SPCs
	    Collection<Object[]> data = new ArrayList<Object[]>();
	    
	    //Get all the ServiceDescriptionDocuments from this ServiceProviderCatalog
	    NodeList sDescs = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:services/@rdf:resource", baseDoc,
	    		XPathConstants.NODESET);
	    for (int i = 0; i < sDescs.getLength(); i++)
	    {
	    	String uri = sDescs.item(i).getNodeValue();
	    	uri = OSLCUtils.absoluteUrlFromRelative(base, uri);
	    	data.add(new Object[] { uri });
	    }
	    
	    //Get all ServiceProviderCatalog urls from the base document in order to recursively add all the
	    //description documents from them as well.
	    NodeList spcs = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:entry/oslc_disc:ServiceProviderCatalog/@rdf:about",
	    		baseDoc, XPathConstants.NODESET);
	    for (int i = 0; i < spcs.getLength(); i++)
	    {
	    	String uri = spcs.item(i).getNodeValue();
	    	uri = OSLCUtils.absoluteUrlFromRelative(base, uri);
	    	if (!uri.equals(base))
	    	{
	    		Collection<Object[]> subCollection = getReferencedUrls(uri);
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
	public void baseUrlIsValid()
	{
        //Get the status, make sure 200 OK
        assertTrue(response.getStatusLine().toString(), response.getStatusLine().getStatusCode() == 200);
        
        //Verify we got a response
	    assertNotNull(responseBody);
	}
	
	@Test
	public void invalidContentTypeGivesNotSupported() throws IOException
	{
		HttpResponse resp = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds, "application/svg+xml");
		String respType =  resp.getEntity().getContentType().getValue();
		EntityUtils.consume(resp.getEntity());
		assertTrue(resp.getStatusLine().getStatusCode() == 406 || respType.contains("application/svg+xml"));
	}
	
	
	@Test
	public void contentTypeIsCMServiceDescription() throws IOException
	{
		HttpResponse resp = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds,
				OSLCConstants.CT_DISC_DESC_XML);
		//Make sure the response to this URL was of valid type
		EntityUtils.consume(resp.getEntity());
		assertTrue(resp.getEntity().getContentType().getValue().contains(OSLCConstants.CT_DISC_DESC_XML));
	}
	
	@Test
	public void misplacedParametersDoNotEffectResponse() throws IOException
	{
		HttpResponse baseResp = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds,
				OSLCConstants.CT_DISC_DESC_XML);
		String baseRespValue = EntityUtils.toString(baseResp.getEntity());
		
		HttpResponse parameterResp = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl + "?oslc_cm:query", basicCreds,
				OSLCConstants.CT_DISC_DESC_XML);
		String parameterRespValue = EntityUtils.toString(parameterResp.getEntity());
		
		assertTrue(baseRespValue.equals(parameterRespValue));
	}
	
	@Test
	public void serviceDescriptionHasTitle() throws XPathException
	{
		//Verify that the ServiceDescription has a dc:title child element
		Node title = (Node) OSLCUtils.getXPath().evaluate("/oslc_cm:ServiceDescriptor/dc:title", doc, 
				XPathConstants.NODE);
		assertNotNull(title);
		assertFalse(title.getTextContent().isEmpty());
		
		//Verify that the dc:title child element has no children
		NodeList children = (NodeList) OSLCUtils.getXPath().evaluate("/oslc_cm:ServiceDescriptor/dc:title/*", doc, 
				XPathConstants.NODESET);
		assertTrue(children.getLength() == 0);
	}
	
	@Test
	public void serviceDescriptionHasDescription() throws XPathException
	{
		//Verify the ServiceDescription has a dc:description child element		
		Node description = (Node) OSLCUtils.getXPath().evaluate("/oslc_cm:ServiceDescriptor/dc:description", doc, 
				XPathConstants.NODE);
		assertNotNull(description);
	}
	
	@Test
	public void serviceDescriptionContributorHasIdentifier() throws XPathException
	{
		//If the ServiceDescription has a dc:contributor element, make sure it has a dc:identifier child element
		NodeList contrib = (NodeList)OSLCUtils.getXPath().evaluate("//dc:contributor", doc, XPathConstants.NODE);
		if (contrib != null)
		{
			Node identifier = (Node)OSLCUtils.getXPath().evaluate("//dc:contributor/dc:identifier", doc,
					XPathConstants.NODE);
			assertNotNull(identifier);
		}
	}
	
	@Test
	public void changeManagementServiceDescriptionHasValidFactory() throws XPathException
	{
		//If ServiceDescription is oslc_cm, make sure it has a valid factory child element
		Node cmRequest = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests", doc, XPathConstants.NODE);
		if (cmRequest != null)
		{
			Node fac = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:factory", doc,
					XPathConstants.NODE);
			assertNotNull(fac);
			Node facUrl = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:factory/oslc_cm:url",
					doc, XPathConstants.NODE);
			assertNotNull(facUrl);
			Node facTitle = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:factory/dc:title",
					doc, XPathConstants.NODE);
			assertNotNull(facTitle);
			assertFalse(facTitle.getTextContent().isEmpty());
		}
	}
	
	@Test
	public void changeManagementServiceDescriptionHasValidSimpleQuery() throws XPathException
	{
		//If ServiceDescription is oslc_cm, make sure it has a valid simple query child element
		Node cmRequest = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests", doc, XPathConstants.NODE);
		if (cmRequest != null)
		{
			NodeList sQ = (NodeList)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:simpleQuery", doc,
					XPathConstants.NODESET);
			assertTrue(sQ.getLength() == 1);
			Node url = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:simpleQuery/oslc_cm:url",
					doc, XPathConstants.NODE);
			assertNotNull(url);
			Node title = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:simpleQuery/dc:title",
					doc, XPathConstants.NODE);
			assertNotNull(title);
			assertFalse(title.getTextContent().isEmpty());
		}
	}
	
	@Test
	public void changeManagementServiceDescriptionHasValidSelectionDialog() throws XPathException
	{
		//If ServiceDescription is oslc_cm, make sure it has a valid selection dialog child element
		Node cmRequest = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests", doc, XPathConstants.NODE);
		if (cmRequest != null)
		{
			NodeList sD = (NodeList)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:selectionDialog",
					doc, XPathConstants.NODESET);
			for (int i = 0; i < sD.getLength(); i++)
			{
				Node sQUrl = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:selectionDialog["
						+ (i + 1) + "]/oslc_cm:url", doc, XPathConstants.NODE);
				assertNotNull(sQUrl);
				Node sDtitle = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:selectionDialog["
						+ (i + 1) + "]/dc:title", doc, XPathConstants.NODE);
				assertNotNull(sDtitle);
				assertFalse(sDtitle.getTextContent().isEmpty());
			}
		}
	}
	
	@Test
	public void changeManagementServiceDescriptionHasValidCreationDialog() throws XPathException
	{
		//If ServiceDescription is oslc_cm, make sure it has a valid creation dialog child element
		Node cmRequest = (Node) OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests", doc, XPathConstants.NODE);
		if (cmRequest != null)
		{
			NodeList sD = (NodeList)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:creationDialog", 
					doc, XPathConstants.NODESET);
			for (int i = 0; i < sD.getLength(); i++)
			{
				Node sQUrl = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:creationDialog[" +
						(i + 1) + "]/oslc_cm:url", doc, XPathConstants.NODE);
				assertNotNull(sQUrl);
				Node sDtitle = (Node)OSLCUtils.getXPath().evaluate("//oslc_cm:changeRequests/oslc_cm:creationDialog[" 
						+ (i + 1) + "]/dc:title", doc, XPathConstants.NODE);
				assertNotNull(sDtitle);
				assertFalse(sDtitle.getTextContent().isEmpty());
			}
		}
	}
	
	@Test
	public void validateUrlsInServiceDescription() throws IOException, XPathException
	{
		//Get all referenced oslc_cm:url elements in the Description Document
		NodeList urlElements = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm:url", doc, XPathConstants.NODESET);
		for (int i = 0; i < urlElements.getLength(); i++)
		{
			Node urlElement = urlElements.item(i);
			String url = urlElement.getTextContent();
			
			//Perform HTTP GET request on the URL and verify it exists in some form
			HttpResponse urlResponse = OSLCUtils.getResponseFromUrl(baseUrl, url, basicCreds, "*/*");
			EntityUtils.consume(urlResponse.getEntity());
			assertFalse(urlResponse.getStatusLine().getStatusCode() == 404);
		}
	}
	
	@Test
	public void homeElementHasTitleAndUrlChildElements() throws XPathException
	{
		//Make sure each home element has a title and url
		NodeList hElements = (NodeList)OSLCUtils.getXPath().evaluate("//oslc_cm:home", doc, XPathConstants.NODESET);
		for (int i = 0; i < hElements.getLength(); i++)
		{
			Node hUrl = (Node)OSLCUtils.getXPath().evaluate("./oslc_cm:url", hElements.item(i), 
					XPathConstants.NODE);
			assertNotNull(hUrl);
			Node hTitle = (Node)OSLCUtils.getXPath().evaluate("./dc:title", hElements.item(i), 
					XPathConstants.NODE);
			assertNotNull(hTitle);
			assertFalse(hTitle.getTextContent().isEmpty());
		}
	}
}
