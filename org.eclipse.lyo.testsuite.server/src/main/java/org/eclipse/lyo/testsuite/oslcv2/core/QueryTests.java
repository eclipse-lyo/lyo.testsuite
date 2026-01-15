/*
 * Copyright (c) 2011, 2014, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.compat.JSONArray;
import org.apache.wink.json4j.compat.JSONObject;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of query factories as specified in the OSLC version 2 spec. It
 * tests both equality and comparison capabilities as well as that of full text search, using the properties and
 * expected values from the properties file.
 *
 * <p>Tests XML responses only TODO: Add RDF/XML and JSON tests
 */
public class QueryTests extends SimplifiedQueryBaseTests {
    public QueryTests() {
        super(null);
    }

    protected void setup(String baseUri) {

        currentUrl = baseUri;
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void validEqualsQueryContainsExpectedDefect(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        setup(thisUrl);
        // Form the equality query

        String query = getQueryBase()
                + "oslc.where="
                + queryProperty
                + URLEncoder.encode("=\"" + queryPropertyValue + "\"", "UTF-8")
                + "&oslc.select="
                + queryProperty;
        String responseBody = runQuery(query, OSLCConstants.CT_XML);
        // Get XML Doc from response
        Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
        NodeList results =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, XPathConstants.NODESET);
        if (results == null)
            results = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", doc, XPathConstants.NODESET);
        assertNotNull(results);
        assertTrue(results.getLength() > 0, "Expected query results > 0");
        // Check that the property elements are equal to the expected value
        checkEqualityProperty(results, queryProperty, queryPropertyValue, doc);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void validNotEqualQueryContainsExpectedDefect(String thisUrl)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        setup(thisUrl);
        // Form the inequality query
        String query = getQueryBase()
                + "oslc.where="
                + queryProperty
                + URLEncoder.encode("!=\"" + queryPropertyValue + "\"", "UTF-8")
                + "&oslc.select="
                + queryProperty;

        String responseBody = runQuery(query, OSLCConstants.CT_XML);

        // Get XML Doc from response
        Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
        NodeList results =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, XPathConstants.NODESET);
        assertTrue(results != null);
        assertTrue(results.getLength() > 0);
        // Check that the property elements are not equal to the value in the previous test
        checkInequalityProperty(results, queryProperty, queryPropertyValue, doc);
    }

    protected String runQuery(String queryURL, String contentType) throws IOException {
        Response response =
                OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl + queryURL, creds, contentType, headers);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseBody = response.readEntity(String.class);
        return responseBody;
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void validLessThanQueryContainsExpectedDefects(String thisUrl)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, ParseException {
        setup(thisUrl);
        // Build the query using the specified comparison property
        String query = getQueryBase()
                + "oslc.where="
                + queryComparisonProperty
                + URLEncoder.encode("<\"" + queryComparisonValue + "\"", "UTF-8")
                + "&oslc.select="
                + queryComparisonProperty;
        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl + query, creds, "application/xml", headers);
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        NodeList results =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, XPathConstants.NODESET);
        if (results == null)
            results = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", doc, XPathConstants.NODESET);
        assertTrue(results != null);
        assertTrue(results.getLength() > 0, "Expecting query results >0");
        // Check that the property elements are less than the query comparison property
        checkLessThanProperty(results, queryComparisonProperty, queryComparisonValue, doc);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void validGreaterThanQueryContainsExpectedDefects(String thisUrl)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, ParseException {
        setup(thisUrl);
        // Build the query using the specified comparison property
        String query = getQueryBase()
                + "oslc.where="
                + queryComparisonProperty
                + URLEncoder.encode(">=\"" + queryComparisonValue + "\"", "UTF-8")
                + "&oslc.select="
                + queryComparisonProperty;
        String respBody = runQuery(query, OSLCConstants.CT_XML);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        NodeList results =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, XPathConstants.NODESET);
        if (results == null) {
            results = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", doc, XPathConstants.NODESET);
        }
        assertTrue(results != null);
        assertTrue(results.getLength() > 0, "Expected query results >0");
        // Check that the property elements are greater than the query comparison property
        checkGreaterThanProperty(results, queryComparisonProperty, queryComparisonValue, doc);
    }

    public String getCompoundQueryContainsExpectedDefectQuery() throws UnsupportedEncodingException {
        return getQueryBase()
                + "oslc.where="
                + queryProperty
                + URLEncoder.encode(
                        "=\""
                                + queryPropertyValue
                                + "\" and "
                                + queryComparisonProperty
                                + ">=\""
                                + queryComparisonValue
                                + "\"",
                        "UTF-8")
                + "&oslc.select="
                + queryProperty
                + ","
                + queryComparisonProperty;
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void validCompoundQueryContainsExpectedDefect(String thisUrl)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        setup(thisUrl);
        String respBody = runQuery(getCompoundQueryContainsExpectedDefectQuery(), OSLCConstants.CT_XML);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // Make sure each entry has a matching property element with a value that matches the query
        NodeList lst =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, XPathConstants.NODESET);
        checkEqualityProperty(lst, queryProperty, queryPropertyValue, doc);
        checkGreaterThanProperty(lst, queryComparisonProperty, queryComparisonValue, doc);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void validCompoundQueryContainsExpectedDefectJson(String thisUrl)
            throws IOException, NullPointerException, JSONException {
        setup(thisUrl);
        // Get response
        String respBody = runQuery(getCompoundQueryContainsExpectedDefectQuery(), OSLCConstants.CT_JSON);
        JSONArtifact userData = JSON.parse(respBody);
        JSONObject resultJson = null;
        if (userData instanceof JSONArtifact) {
            resultJson = (JSONObject) userData;
        }
        // Make sure each entry has a matching property element with a value that matches the query
        // Verify that all results are less than the comparison value
        JSONArray results = (JSONArray) resultJson.get("oslc:results");

        assertTrue(results.length() > 0, "Query did not return any result");
        // TODO: Add JSON logic
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void fullTextSearchContainsExpectedResults(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        setup(thisUrl);
        // Build the fulltext query using oslc.searchTerms
        String query = "?"
                + additionalParameters
                + "&oslc.searchTerms="
                + URLEncoder.encode("\"" + fullTextSearchTerm + "\"", "UTF-8");

        String responseBody = runQuery(query, OSLCConstants.CT_XML);

        // Get XML Doc from response
        Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);

        NodeList lst =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest", doc, XPathConstants.NODESET);
        if (lst == null || lst.getLength() == 0) {
            lst = (NodeList) OSLCUtils.getXPath().evaluate("//rdf:Description", doc, XPathConstants.NODESET);
        }
        assertNotNull(lst);

        // Verify our fulltext search returned a result
        assertTrue(lst.getLength() > 0, "Exptected full text to respond with results");
    }

    public void checkEqualityProperty(NodeList resultList, String queryProperty, String qVal, Document doc) {
        String queryPropertyNS = "*";
        String queryPropertyName = queryProperty;
        if (queryProperty.contains(":")) {
            queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
            queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
        }
        for (int i = 0; i < resultList.getLength(); i++) {
            NodeList elements = resultList.item(i).getChildNodes();
            for (int j = 0; j < elements.getLength(); j++) {
                Node element = elements.item(j);
                if (element.getLocalName() != null
                        && element.getLocalName().equals(queryPropertyName)
                        && (element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS))
                                || queryPropertyNS.equals("*"))) {
                    // TODO: Determine if OSLC queries are case-sensitive.
                    assertTrue(qVal.equalsIgnoreCase(element.getTextContent()));
                }
            }
        }
    }

    public void checkInequalityProperty(NodeList resultList, String queryProperty, String qVal, Document doc) {
        String queryPropertyNS = "*";
        String queryPropertyName = queryProperty;
        if (queryProperty.contains(":")) {
            queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
            queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
        }
        for (int i = 0; i < resultList.getLength(); i++) {
            NodeList elements = resultList.item(i).getChildNodes();
            for (int j = 0; j < elements.getLength(); j++) {
                Node element = elements.item(j);
                if (element.getLocalName() != null
                        && element.getLocalName().equals(queryPropertyName)
                        && (element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS))
                                || queryPropertyNS.equals("*"))) {
                    assertTrue(!element.getTextContent().equals(qVal));
                }
            }
        }
    }

    public void checkLessThanProperty(NodeList resultList, String queryProperty, String qVal, Document doc) {
        String queryPropertyNS = "*";
        String queryPropertyName = queryProperty;
        if (queryProperty.contains(":")) {
            queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
            queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
        }
        for (int i = 0; i < resultList.getLength(); i++) {
            NodeList elements = resultList.item(i).getChildNodes();
            for (int j = 0; j < elements.getLength(); j++) {
                Node element = elements.item(j);
                if (element.getLocalName() != null
                        && element.getLocalName().equals(queryPropertyName)
                        && (element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS))
                                || queryPropertyNS.equals("*"))) {
                    assertTrue(element.getTextContent().compareTo(qVal) < 0);
                }
            }
        }
    }

    public void checkGreaterThanProperty(NodeList resultList, String queryProperty, String qVal, Document doc) {
        String queryPropertyNS = "*";
        String queryPropertyName = queryProperty;
        if (queryProperty.contains(":")) {
            queryPropertyNS = queryProperty.substring(0, queryProperty.indexOf(':'));
            queryPropertyName = queryProperty.substring(queryProperty.indexOf(':') + 1);
        }
        for (int i = 0; i < resultList.getLength(); i++) {
            NodeList elements = resultList.item(i).getChildNodes();
            for (int j = 0; j < elements.getLength(); j++) {
                Node element = elements.item(j);
                if (element.getLocalName() != null
                        && element.getLocalName().equals(queryPropertyName)
                        && (element.getNamespaceURI().equals(doc.lookupNamespaceURI(queryPropertyNS))
                                || queryPropertyNS.equals("*"))) {
                    assertTrue(element.getTextContent().compareTo(qVal) >= 0);
                }
            }
        }
    }
}
