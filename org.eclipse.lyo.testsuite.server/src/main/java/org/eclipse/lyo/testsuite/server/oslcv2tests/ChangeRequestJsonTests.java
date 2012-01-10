/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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
 *    Yuhong Yin - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server.oslcv2tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a change request returned by accessing the change
 * request's URL directly. It runs the equality query from the properties file and grabs the first result
 * to test against, checking the relationship of elements in the JSON representation of the change request.
 */
@RunWith(Parameterized.class)
public class ChangeRequestJsonTests extends TestsBase {
	private HttpResponse response;
	private String responseBody;
	private JSONObject doc;
	
	public ChangeRequestJsonTests(String thisUrl) 
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, NullPointerException, JSONException
	{
		super(thisUrl);

		// If currentUrl is null, it means that the query didn't match any
		// records. This isn't exactly a failure, but there's nothing more we
		// can test.
		assumeNotNull(currentUrl);
        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, 
        		                                OSLCConstants.CT_JSON, headers);
        responseBody = EntityUtils.toString(response.getEntity());
        int sc = response.getStatusLine().getStatusCode();

		// Some records in the system might not be accessible to this user. This
		// isn't a failure, but there's nothing more we can test.
        assumeTrue(sc != HttpStatus.SC_FORBIDDEN && sc != HttpStatus.SC_UNAUTHORIZED);
        
        // Make sure the request succeeded before continuing.
        assertEquals(HttpStatus.SC_OK, sc);
        
        //Get JSON doc from response
		JSONArtifact userData = JSON.parse(responseBody);
		
		if (userData instanceof JSONArtifact) {
			doc = (JSONObject)userData;
		}
	    
	}
	
	@Before
	public void setup() 
		throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		super.setup();
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() 
	    throws IOException, NullPointerException, XPathException, ParserConfigurationException, SAXException, JSONException
	{
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//query factories of the REST service.
		
	    ArrayList<String> serviceUrls = getServiceProviderURLsUsingJson(setupProps.getProperty("baseUri"), onlyOnce);
		
	    ArrayList<String> capabilityURLsUsingJson = getCapabilityURLsUsingJson(OSLCConstants.QUERY_BASE_PROP, serviceUrls, true);
	    
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
		for (String queryBaseUri : capabilityURLsUsingJson) {
			
			String queryUrl = OSLCUtils.addQueryStringToURL(queryBaseUri, query);
            
			HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, basicCreds, 
					OSLCConstants.CT_JSON, headers);
			
			String respBody = EntityUtils.toString(resp.getEntity());
			
			// Parse the response
			JSONArtifact userData = null;
			try {
				userData = JSON.parse(respBody);
			}
			catch (JSONException e) {
				// parsing error - we imply the response is not in JSON format
				
			}
			
			JSONObject resultJson = null;
			if (userData instanceof JSONArtifact) {
				resultJson = (JSONObject)userData;
			}
			
			JSONArray s = null;
			if ( resultJson.containsKey("oslc:results") ) {
				s = (JSONArray)resultJson.get("oslc:results");
			}
			else if ( resultJson.containsKey("rdfs:member") ) {
				s = (JSONArray)resultJson.getJSONArray("rdfs:member"); 
			}
			
			JSONObject r = (JSONObject) s.get(0);			
			String one = null; 
			
			if ( r.containsKey("rdf:resource") ) {
				one = r.getString("rdf:resource");
			}
			else if ( r.containsKey("rdf:about") ) {
				one = r.getString("rdf:about");
			}
			
			results.add(one);
			
			if (!results.isEmpty() && onlyOnce)
				break;
		}

		return toCollection(results);
	}	

	@Test
	//  
	// Verify that the Change Request has one and only one dcterms:title
	//
	public void changeRequestHasOneTitle() throws JSONException
	{
		assertTrue(doc.get(OSLCConstants.DCTERMS_TITLE) instanceof String);		
	}
	
	@Test
	public void changeRequestHasAtMostOneDescription() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_DESC) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_DESC) instanceof String);
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneIdentifier() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_ID) ) { 
			assertTrue( (doc.get(OSLCConstants.DCTERMS_ID) instanceof String) ||
					(doc.get(OSLCConstants.DCTERMS_ID) instanceof Integer));
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneName() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_NAME) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_NAME) instanceof String);
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneCreatedDate() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_CREATED) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_CREATED) instanceof String);
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneModifiedDate() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_MODIFIED) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_MODIFIED) instanceof String);
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneDiscussion() throws JSONException
	{
		if ( doc.containsKey("oslc:discussion") ) { 
			assertTrue(doc.get("oslc:discussion") instanceof JSONObject);
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneInstanceShape() throws JSONException
	{
		if ( doc.containsKey("oslc:instanceShape") ) { 
			assertTrue(doc.get("oslc:instanceShape") instanceof JSONObject);
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneCloseDate() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:closedDate") ) { 
			assertTrue(doc.get("oslc_cm:closedDate") instanceof String);
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneStatus() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:status") ) { 
			assertTrue(doc.get("oslc_cm:status") instanceof String);
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneClosedElement() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:closed") ) { 
			assertTrue( (doc.get("oslc_cm:closed") instanceof Boolean) ||
					    (doc.get("oslc_cm:closed") instanceof String));
		}		
	}
	
	@Test
	public void changeRequestHasAtMostInProgressElement() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:inprogress") ) { 
			assertTrue( (doc.get("oslc_cm:inprogress") instanceof Boolean) ||
				        (doc.get("oslc_cm:inprogress") instanceof String));
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneFixedElement() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:fixed") ) { 
			assertTrue( (doc.get("oslc_cm:fixed") instanceof Boolean) ||
	                    (doc.get("oslc_cm:fixed") instanceof String));
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneApprovedElement() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:approved") ) { 
			assertTrue( (doc.get("oslc_cm:approved") instanceof Boolean) ||
			            (doc.get("oslc_cm:approved") instanceof String));
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneReviewedElement() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:reviewed") ) { 
			assertTrue( (doc.get("oslc_cm:reviewed") instanceof Boolean) ||
                        (doc.get("oslc_cm:reviewed") instanceof String));
		}		
	}
	
	@Test
	public void changeRequestHasAtMostOneVerifiedElement() throws JSONException
	{
		if ( doc.containsKey("oslc_cm:verified") ) { 
			assertTrue( (doc.get("oslc_cm:verified") instanceof Boolean) ||
                        (doc.get("oslc_cm:verified") instanceof String));
		}		
	}
}
