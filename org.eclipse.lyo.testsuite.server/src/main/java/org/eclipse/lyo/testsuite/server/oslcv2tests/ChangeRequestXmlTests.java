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
package org.eclipse.lyo.testsuite.server.oslcv2tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
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
 * This class provides JUnit tests for the validation of a change request returned by accessing the change
 * request's URL directly. It runs the equality query from the properties file and grabs the first result
 * to test against, checking the relationship of elements in the XML representation of the change request.
 */
@RunWith(Parameterized.class)
public class ChangeRequestXmlTests extends TestsBase {
	private HttpResponse response;
	private String responseBody;
	private Document doc;
	
	public ChangeRequestXmlTests(String thisUrl) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		super(thisUrl);

		// If currentUrl is null, it means that the query didn't match any
		// records. This isn't exactly a failure, but there's nothing more we
		// can test.
		assumeNotNull(currentUrl);
        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, OSLCConstants.CT_XML,
        		headers);
        responseBody = EntityUtils.toString(response.getEntity());
        int sc = response.getStatusLine().getStatusCode();

		// Some records in the system might not be accessible to this user. This
		// isn't a failure, but there's nothing more we can test.
        assumeTrue(sc != HttpStatus.SC_FORBIDDEN && sc != HttpStatus.SC_UNAUTHORIZED);
        
        // Make sure the request succeeded before continuing.
        assertEquals(HttpStatus.SC_OK, sc);
        
        //Get XML Doc from response
	    doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
	}
	
	@Before
	public void setup() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		super.setup();
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//query factories of the REST service.
		String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
		ArrayList<String> capabilityURLsUsingXML = TestsBase.getCapabilityURLsUsingXML(v, serviceUrls, true);
		
		// Once we have the query URL, look for a resource to validate
		String where = setupProps.getProperty("changeRequestsWhere");
		if (where == null) {
			String queryProperty = setupProps.getProperty("queryEqualityProperty");
			String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
			where = queryProperty + "=\"" + queryPropertyValue + "\"";
		}

		String additionalParameters = setupProps.getProperty("queryAdditionalParameters");
		String query = (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&"; 
		query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";
	
		ArrayList<String> results = new ArrayList<String>();
		for (String queryBase : capabilityURLsUsingXML) {
			HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryBase + query, basicCreds, 
					OSLCConstants.CT_XML, headers);
			String respBody = EntityUtils.toString(resp.getEntity());
			resp.getEntity().consumeContent();
			assertTrue("Received " +resp.getStatusLine(), (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK));
			//Get XML Doc from response
			Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);
			Node result = (Node) OSLCUtils.getXPath().evaluate("//rdfs:member/@rdf:resource", 
					doc, XPathConstants.NODE);
			if (result != null)
				results.add(result.getNodeValue());
			if (onlyOnce)
				break;
		}
		return toCollection(results);
	}	
	
	@Test
	public void changeRequestHasOneDcTitle() throws XPathExpressionException
	{
		// All change requests have exactly one title.
		NodeList titles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/dc:title",
	    		doc, XPathConstants.NODESET);
		assertEquals(getFailureMessage(), 1, titles.getLength());
	}
	
	@Test
	public void changeRequestHasAtMostOneDescription() throws XPathExpressionException
	{
		NodeList descriptions = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/dc:description",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), descriptions.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneIdentifier() throws XPathExpressionException
	{
		NodeList ids = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/dc:identifier",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), ids.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneName() throws XPathExpressionException
	{
		NodeList names = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/dc:name",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), names.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneCreatedDate() throws XPathExpressionException
	{
		NodeList createdDates = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/dc:created",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), createdDates.getLength() <= 1);
		//If there is a created date, verify the format.
		if (createdDates.getLength() > 0)
		{
			try
			{
				DatatypeConverter.parseDateTime(createdDates.item(0).getTextContent());
			}
			catch (Exception e)
			{
				fail("Created date not in valid XSD format");
			}
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneModifiedDate() throws XPathExpressionException
	{
		NodeList modifiedDates = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/dc:modified",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), modifiedDates.getLength() <= 1);
		//If there is a modified date, verify the format.
		if (modifiedDates.getLength() > 0)
		{
			try
			{
				final String dateString = modifiedDates.item(0).getTextContent();
				DatatypeConverter.parseDateTime(dateString);
			}
			catch (Exception e)
			{
				fail("Modified date not in valid XSD format");
			}
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneDiscussion() throws XPathExpressionException
	{
		NodeList discussions = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/oslc:discussion",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), discussions.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneInstanceShape() throws XPathExpressionException
	{
		NodeList instances = (NodeList)OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/oslc:instanceShape",
	    		doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), instances.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneCloseDate() throws XPathExpressionException
	{
		NodeList closeDates = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:closeDate", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), closeDates.getLength() <= 1);
		//If there is a close date, verify the format.
		if (closeDates.getLength() > 0)
		{
			try
			{
				DatatypeConverter.parseDateTime(closeDates.item(0).getTextContent());
			}
			catch (Exception e)
			{
				fail("Modified date not in valid XSD format");
			}
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneStatus() throws XPathExpressionException
	{
		NodeList statuses = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:status", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), statuses.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneClosedElement() throws XPathExpressionException
	{
		NodeList closedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:closed", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), closedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostInProgressElement() throws XPathExpressionException
	{
		NodeList inProgressEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:inprogress", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), inProgressEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneFixedElement() throws XPathExpressionException
	{
		NodeList fixedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:fixed", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), fixedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneApprovedElement() throws XPathExpressionException
	{
		NodeList approvedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:approved", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), approvedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneReviewedElement() throws XPathExpressionException
	{
		NodeList reviewedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:reviewed", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), reviewedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneVerifiedElement() throws XPathExpressionException
	{
		NodeList verifiedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:verified", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), verifiedEles.getLength() <= 1);
	}
	
	private String getFailureMessage() {
		return "Problems with XML representation of OSLC ChangeRequest <" + currentUrl + ">";
	}
}
