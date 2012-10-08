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
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests with JSON format for the validation of an OSLC core resource.
 * 
 */
@RunWith(Parameterized.class)
public abstract class CoreResourceJsonTests extends TestsBase {
	private HttpResponse response;
	private String responseBody;
	protected JSONObject doc;
	
	public CoreResourceJsonTests(String thisUrl) 
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
		
	@Test
	//  
	// Verify that the OSLC Core Resource has one and only one dcterms:title
	//
	public void CoreResourceHasOneTitle() throws JSONException
	{
		assertTrue(doc.get(OSLCConstants.DCTERMS_TITLE) instanceof String);		
	}
	
	@Test
	public void CoreResourceHasAtMostOneDescription() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_DESC) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_DESC) instanceof String);
		}
	}
	
	@Test
	public void CoreResourceHasAtMostOneIdentifier() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_ID) ) { 
			assertTrue( (doc.get(OSLCConstants.DCTERMS_ID) instanceof String) ||
					(doc.get(OSLCConstants.DCTERMS_ID) instanceof Integer));
		}
	}
	
	@Test
	public void CoreResourceHasAtMostOneName() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_NAME) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_NAME) instanceof String);
		}
	}
	
	@Test
	public void CoreResourceHasAtMostOneCreatedDate() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_CREATED) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_CREATED) instanceof String);
		}
	}
	
	@Test
	public void CoreResourceHasAtMostOneModifiedDate() throws JSONException
	{
		if ( doc.containsKey(OSLCConstants.DCTERMS_MODIFIED) ) { 
			assertTrue(doc.get(OSLCConstants.DCTERMS_MODIFIED) instanceof String);
		}		
	}
	
	@Test
	public void CoreResourceHasAtMostOneDiscussion() throws JSONException
	{
		if ( doc.containsKey("oslc:discussion") ) { 
			assertTrue(doc.get("oslc:discussion") instanceof JSONObject);
		}		
	}
	
	@Test
	public void CoreResourceHasAtMostOneInstanceShape() throws JSONException
	{
		if ( doc.containsKey("oslc:instanceShape") ) { 
			assertTrue(doc.get("oslc:instanceShape") instanceof JSONObject);
		}		
	}
}
