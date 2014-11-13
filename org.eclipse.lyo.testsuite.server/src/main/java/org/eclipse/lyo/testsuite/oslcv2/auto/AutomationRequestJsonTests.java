/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation.
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
 *    Michael Fiedler - updated for OSLC Automation V2 spec
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.auto;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.oslcv2.CoreResourceJsonTests;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a automation request returned by accessing the auto
 * request's URL directly. It runs the equality query from the properties file and grabs the first result
 * to test against, checking the relationship of elements in the JSON representation of the auto request
 */
@RunWith(Parameterized.class)
public class AutomationRequestJsonTests extends CoreResourceJsonTests {
		
	public AutomationRequestJsonTests(String thisUrl) throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException, NullPointerException, JSONException {
		
		super(thisUrl);		
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() 
	    throws IOException, NullPointerException, XPathException, ParserConfigurationException, SAXException, JSONException
	{
		ArrayList<String> results = new ArrayList<String>();
		
		staticSetup();
		
		String useThisAutoRequest = setupProps.getProperty("useThisAutoRequest");
		if ( useThisAutoRequest != null ) {
			results = new ArrayList<String>();
			results.add(useThisAutoRequest);
			return toCollection(results);
		}

		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//query factories of the REST service.
		
	    ArrayList<String> serviceUrls = getServiceProviderURLsUsingJson(setupProps.getProperty("baseUri"), onlyOnce);
		
	    ArrayList<String> capabilityURLsUsingJson = getCapabilityURLsUsingJson(OSLCConstants.QUERY_BASE_PROP, serviceUrls, true);
	    
	    String where = setupProps.getProperty("autoRequestsWhere");
		if (where == null) {
			String queryProperty = setupProps.getProperty("queryEqualityProperty");
			String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
			where = queryProperty + "=\"" + queryPropertyValue + "\"";
		}

		String additionalParameters = setupProps.getProperty("queryAdditionalParameters");
		String query = (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&"; 
		query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";
				
		for (String queryBaseUri : capabilityURLsUsingJson) {
			
			String queryUrl = OSLCUtils.addQueryStringToURL(queryBaseUri, query);
            
			HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds, 
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
	public void autoRequestHasAtLeastOneState() throws JSONException
	{
		assertTrue((doc.get(OSLCConstants.AUTO_OSLC_AUTO_STATE) instanceof JSONObject) ||
				   (doc.get(OSLCConstants.AUTO_OSLC_AUTO_STATE) instanceof JSONArray));		
	}
	
	@Test
	public void autoRequestHasAtMostOneDesiredState() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.AUTO_OSLC_AUTO_DESIRED_STATE) ) { 
			assertTrue(doc.get(OSLCConstants.AUTO_OSLC_AUTO_DESIRED_STATE) instanceof JSONObject);
		}
	}
	
	@Test
	public void autoRequestHasOneExecutesLink() throws JSONException
	{
		assertTrue((doc.get(OSLCConstants.AUTO_OSLC_AUTO_EXECUTES_AUTO_PLAN) instanceof JSONObject) ||
				   (doc.get(OSLCConstants.AUTO_OSLC_AUTO_EXECUTES_AUTO_PLAN) instanceof JSONArray));		
	}
	
	
}
