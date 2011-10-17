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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;


import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.compat.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.compat.JSONObject;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of query factories 
 * as specified in the OSLC version 2 spec. It tests both equality and comparison
 * capabilities as well as that of full text search, using the properties and 
 * expected values from the properties file.
 * 
 * Tests XML responses only
 * TODO: Add RDF/XML, ATOM and JSON tests
 */
@RunWith(Parameterized.class)
public class QueryTests extends SimplifiedQueryBaseTests {
	public QueryTests(String baseUri){
		super(baseUri);
	}
	
	@Test
	public void validEqualsQueryContainsExpectedDefect() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		//Form the equality query
		
		String query = getQueryBase() + "oslc.where=" + queryProperty + 
		URLEncoder.encode("=\"" + queryPropertyValue + "\"", "UTF-8") + "&oslc.select=" + queryProperty;
        String responseBody = runQuery(query, OSLCConstants.CT_XML);
        //Get XML Doc from response
	    Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
	    NodeList results = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", 
	    		doc, XPathConstants.NODESET);
	    if (results == null)
	    	results = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", 
		    		doc, XPathConstants.NODESET);
	    assertNotNull(results);
	    assertTrue("Expected query results > 0", results.getLength() > 0);
	    //Check that the property elements are equal to the expected value
	    checkEqualityProperty(results, queryProperty, queryPropertyValue, doc);
	}
	
	@Test
	public void validNotEqualQueryContainsExpectedDefect() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException
	{
		//Form the inequality query
		String query = getQueryBase() + "oslc.where=" + queryProperty + 
		URLEncoder.encode("!=\"" + queryPropertyValue + "\"", "UTF-8") + "&oslc.select=" + queryProperty;
		
        String responseBody = runQuery(query, OSLCConstants.CT_XML);
        
        //Get XML Doc from response
	    Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
	    NodeList results = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", 
	    		doc, XPathConstants.NODESET);
	    assertTrue(results != null);
	    assertTrue(results.getLength() > 0);
	    //Check that the property elements are not equal to the value in the previous test
	    checkInequalityProperty(results, queryProperty, queryPropertyValue, doc);
	}

	protected String runQuery(String queryURL, String contentType) throws IOException {
		HttpResponse response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl + queryURL, basicCreds, 
        		contentType, headers);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String responseBody = EntityUtils.toString(response.getEntity());
		return responseBody;
	}
	
	@Test
	public void validLessThanQueryContainsExpectedDefects() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, ParseException
	{        
        //Build the query using the specified comparison property
		String query = getQueryBase() + "oslc.where=" + queryComparisonProperty + 
				URLEncoder.encode("<\"" + queryComparisonValue + "\"", "UTF-8") + "&oslc.select="
				+ queryComparisonProperty;
		//Get response
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl + query, basicCreds,
				"application/xml", headers);
		String respBody = EntityUtils.toString(resp.getEntity());
		Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);
		
		NodeList results = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", 
	    		doc, XPathConstants.NODESET);
		if (results == null)
			results = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", 
		    		doc, XPathConstants.NODESET);
	    assertTrue(results != null);
	    assertTrue("Expecting query results >0", results.getLength() > 0);
		//Check that the property elements are less than the query comparison property
		checkLessThanProperty(results, queryComparisonProperty, queryComparisonValue, doc);
	}
	
	@Test
	public void validGreaterThanQueryContainsExpectedDefects() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, ParseException
	{        
        //Build the query using the specified comparison property
		String query = getQueryBase() + "oslc.where=" + queryComparisonProperty + 
				URLEncoder.encode(">=\"" + queryComparisonValue + "\"", "UTF-8") + "&oslc.select="
				+ queryComparisonProperty;
		String respBody = runQuery(query, OSLCConstants.CT_XML);
		Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);
		
		NodeList results = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", 
	    		doc, XPathConstants.NODESET);
		if (results == null) {
			results = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", 
		    		doc, XPathConstants.NODESET);
		}
	    assertTrue(results != null);
	    assertTrue("Expected query results >0", results.getLength() > 0);
		//Check that the property elements are greater than the query comparison property
		checkGreaterThanProperty(results, queryComparisonProperty, queryComparisonValue, doc);
	}
	
	public String getCompoundQueryContainsExpectedDefectQuery() throws UnsupportedEncodingException {
		return getQueryBase() + "oslc.where=" + queryProperty + 
		URLEncoder.encode("=\"" + queryPropertyValue +"\" and " + queryComparisonProperty + ">=\"" + 
				queryComparisonValue + "\"", "UTF-8") + "&oslc.select=" + queryProperty + "," + 
				queryComparisonProperty;
	}
	@Test
	public void validCompoundQueryContainsExpectedDefect() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException
	{		
		String respBody = runQuery(getCompoundQueryContainsExpectedDefectQuery(), OSLCConstants.CT_XML);
		Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);
		
		//Make sure each entry has a matching property element with a value that matches the query
		NodeList lst = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, 
	    		XPathConstants.NODESET);
		checkEqualityProperty(lst, queryProperty, queryPropertyValue, doc);
		checkGreaterThanProperty(lst, queryComparisonProperty, queryComparisonValue, doc);
	}
	
	@Test
	public void validCompoundQueryContainsExpectedDefectJson() throws IOException, NullPointerException, JSONException
	{		
		//Get response
		String respBody = runQuery(getCompoundQueryContainsExpectedDefectQuery(), OSLCConstants.CT_JSON);
		JSONArtifact userData = JSON.parse(respBody);
		JSONObject resultJson = null;
		if (userData instanceof JSONArtifact) {
			resultJson = (JSONObject)userData;
		}
		//Make sure each entry has a matching property element with a value that matches the query
		//Verify that all results are less than the comparison value
		JSONArray results = (JSONArray)resultJson.get("oslc:results");
		
		assertTrue("Query did not return any result", results.length()>0);
		// TODO: Add JSON logic
	}
	
	@Test
	public void fullTextSearchContainsExpectedResults() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		//Build the fulltext query using oslc.searchTerms
		String query = "?" + additionalParameters + "&oslc.searchTerms=" + 
		URLEncoder.encode("\"" + fullTextSearchTerm + "\"", "UTF-8");
		
		String responseBody = runQuery(query, OSLCConstants.CT_XML);
        
        //Get XML Doc from response
	    Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
	    
	    NodeList lst = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, 
	    		XPathConstants.NODESET);
	    if (lst == null || lst.getLength() == 0) {
	    	lst = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", doc, 
		    		XPathConstants.NODESET);
	    }
	    assertNotNull(lst);
	    
	    //Verify our fulltext search returned a result
	    assertTrue("Exptected full text to respond with results", lst.getLength() > 0);
	}
	
	public void checkEqualityProperty(NodeList resultList, String queryProperty, String qVal, Document doc)
	{
		String queryPropertyNS = "*";
		String queryPropertyName = queryProperty;
		if (queryProperty.contains(":"))
		{
			queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
			queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
		}
		for (int i = 0; i < resultList.getLength(); i++)
		{
			NodeList elements = resultList.item(i).getChildNodes();
			for (int j = 0; j < elements.getLength(); j++)
			{
				Node element = elements.item(j);
				if (element.getLocalName() != null && element.getLocalName().equals(queryPropertyName) && 
					(element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS)) || queryPropertyNS.equals("*")))
				{
					// TODO: Determine if OSLC queries are case-sensitive.
					assertTrue(qVal.equalsIgnoreCase(element.getTextContent()));
				}
			}
		}
	}
	
	public void checkInequalityProperty(NodeList resultList, String queryProperty, String qVal, Document doc)
	{
		String queryPropertyNS = "*";
		String queryPropertyName = queryProperty;
		if (queryProperty.contains(":"))
		{
			queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
			queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
		}
		for (int i = 0; i < resultList.getLength(); i++)
		{
			NodeList elements = resultList.item(i).getChildNodes();
			for (int j = 0; j < elements.getLength(); j++)
			{
				Node element = elements.item(j);
				if (element.getLocalName() != null && element.getLocalName().equals(queryPropertyName) && 
					(element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS)) || queryPropertyNS.equals("*")))
				{
					assertTrue(!element.getTextContent().equals(qVal));
				}
			}
		}
	}
	
	public void checkLessThanProperty(NodeList resultList, String queryProperty, String qVal, Document doc)
	{
		String queryPropertyNS = "*";
		String queryPropertyName = queryProperty;
		if (queryProperty.contains(":"))
		{
			queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
			queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
		}
		for (int i = 0; i < resultList.getLength(); i++)
		{
			NodeList elements = resultList.item(i).getChildNodes();
			for (int j = 0; j < elements.getLength(); j++)
			{
				Node element = elements.item(j);
				if (element.getLocalName() != null && element.getLocalName().equals(queryPropertyName) && 
					(element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS)) || queryPropertyNS.equals("*")))
				{
					assertTrue(element.getTextContent().compareTo(qVal) < 0);
				}
			}
		}
	}
	
	public void checkGreaterThanProperty(NodeList resultList, String queryProperty, String qVal, Document doc)
	{
		String queryPropertyNS = "*";
		String queryPropertyName = queryProperty;
		if (queryProperty.contains(":"))
		{
			queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
			queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
		}
		for (int i = 0; i < resultList.getLength(); i++)
		{
			NodeList elements = resultList.item(i).getChildNodes();
			for (int j = 0; j < elements.getLength(); j++)
			{
				Node element = elements.item(j);
				if (element.getLocalName() != null && element.getLocalName().equals(queryPropertyName) && 
					(element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS)) || queryPropertyNS.equals("*")))
				{
					assertTrue(element.getTextContent().compareTo(qVal) >= 0);
				}
			}
		}
	}
}
