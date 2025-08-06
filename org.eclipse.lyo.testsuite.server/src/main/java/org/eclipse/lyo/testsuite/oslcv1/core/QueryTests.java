/*
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
 *
 */
package org.eclipse.lyo.testsuite.oslcv1.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import jakarta.ws.rs.core.Response;
import org.apache.http.auth.Credentials;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.SetupProperties;
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
 * This class provides JUnit tests for the validation of OSLC Simply Query services,
 * currently focusing on the validation of the OSLC_CM definition of Queries.
 */
@RunWith(Parameterized.class)
public class QueryTests {

    private static String baseUrl;
    private static TestsBase.UserCredentials basicCreds;

    private String currentUrl;
    private String queryProperty;
    private String queryPropertyValue;
    private String additionalParameters;
    private String queryComparisonProperty;
    private String queryComparisonValue;
    private String fullTextSearchTerm;

    public QueryTests(String url) {
        this.currentUrl = url;
    }

    @Before
    public void setup()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        Properties setupProps = SetupProperties.setup(null);
        if (setupProps.getProperty("testBackwardsCompatability") != null
                && Boolean.parseBoolean(setupProps.getProperty("testBackwardsCompatability"))) {
            setupProps = SetupProperties.setup(setupProps.getProperty("version1Properties"));
        }
        baseUrl = setupProps.getProperty("baseUri");
        String userId = setupProps.getProperty("userId");
        String pw = setupProps.getProperty("pw");
        basicCreds = new TestsBase.UserPassword(userId, pw);
        queryProperty = setupProps.getProperty("queryEqualityProperty");
        queryPropertyValue = setupProps.getProperty("queryEqualityValue");
        queryComparisonProperty = setupProps.getProperty("queryComparisonProperty");
        queryComparisonValue = setupProps.getProperty("queryComparisonValue");
        additionalParameters = setupProps.getProperty("queryAdditionalParameters");
        fullTextSearchTerm = setupProps.getProperty("fullTextSearchTerm");
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively, in order to find the
        // URLs of all
        // OSLC CM simple query services of the REST service.
        Properties setupProps = SetupProperties.setup(null);
        Collection<Object[]> coll = getReferencedUrls(setupProps.getProperty("baseUri"));
        return coll;
    }

    public static Collection<Object[]> getReferencedUrls(String base)
            throws IOException, XPathException, ParserConfigurationException, SAXException {
        Properties setupProps = SetupProperties.setup(null);
        String userId = setupProps.getProperty("userId");
        String pw = setupProps.getProperty("pw");

        Response resp =
                OSLCUtils.getResponseFromUrl(
                        base,
                        base,
                        new TestsBase.UserPassword(userId, pw),
                        OSLCConstants.CT_DISC_CAT_XML + ", " + OSLCConstants.CT_DISC_DESC_XML);

        // If our 'base' is a ServiceDescription, find and add the simpleQuery service url
        if (resp.getHeaderString("Content-Type").contains(OSLCConstants.CT_DISC_DESC_XML)) {
            Document baseDoc =
                    OSLCUtils.createXMLDocFromResponseBody(resp.readEntity(String.class));
            Node simpleQueryUrl =
                    (Node)
                            OSLCUtils.getXPath()
                                    .evaluate(
                                            "//oslc_cm:simpleQuery/oslc_cm:url",
                                            baseDoc,
                                            XPathConstants.NODE);
            Collection<Object[]> data = new ArrayList<Object[]>();
            data.add(new Object[] {simpleQueryUrl.getTextContent()});
            return data;
        }

        String respBody = resp.readEntity(String.class);
        Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // ArrayList to contain the urls from all of the SPCs
        Collection<Object[]> data = new ArrayList<Object[]>();

        // Get all the ServiceDescriptionDocuments from this ServiceProviderCatalog
        NodeList sDescs =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate(
                                        "//oslc_disc:services/@rdf:resource",
                                        baseDoc,
                                        XPathConstants.NODESET);
        for (int i = 0; i < sDescs.getLength(); i++) {
            String serviceUrl =
                    OSLCUtils.absoluteUrlFromRelative(base, sDescs.item(i).getNodeValue());
            Collection<Object[]> subCollection = getReferencedUrls(serviceUrl);
            Iterator<Object[]> iter = subCollection.iterator();
            while (iter.hasNext()) {
                data.add(iter.next());
            }
        }

        // Get all ServiceProviderCatalog urls from the base document in order to recursively add
        // all the
        // simple query services from the eventual service description documents from them as well.
        NodeList spcs =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate(
                                        "//oslc_disc:entry/oslc_disc:ServiceProviderCatalog/@rdf:about",
                                        baseDoc,
                                        XPathConstants.NODESET);
        for (int i = 0; i < spcs.getLength(); i++) {
            String uri = spcs.item(i).getNodeValue();
            uri = OSLCUtils.absoluteUrlFromRelative(base, uri);
            if (!uri.equals(base)) {
                Collection<Object[]> subCollection = getReferencedUrls(uri);
                Iterator<Object[]> iter = subCollection.iterator();
                while (iter.hasNext()) {
                    data.add(iter.next());
                }
            }
        }
        return data;
    }

    @Test
    public void validEqualsTypeQueryContainsExpectedDefect()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryProperty
                        + URLEncoder.encode("=\"" + queryPropertyValue + "\"", "UTF-8")
                        + "&oslc_cm.properties="
                        + queryProperty;
        // Get the response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml", null);
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);
        // Check for the expected result
        NodeList lst = doc.getElementsByTagNameNS("*", queryProperty);
        lst = (lst.getLength() == 0) ? doc.getElementsByTagName(queryProperty) : lst;
        assertTrue(lst.getLength() > 0);
        boolean containsExpectedDefectResults = true;
        for (int i = 0; i < lst.getLength(); i++) {
            if (!lst.item(i).getTextContent().equals(queryPropertyValue)) {
                containsExpectedDefectResults = false;
            }
        }
        assertTrue(containsExpectedDefectResults);
    }

    @Test
    public void validTypeQueryReturnsCorrectType()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryProperty
                        + URLEncoder.encode("=\"" + queryPropertyValue + "\"", "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryProperty;

        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, OSLCConstants.CT_XML);
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // Make sure each entry has a matching property element with a value that matches the query
        NodeList propertyEntries = doc.getElementsByTagNameNS("*", queryProperty);
        propertyEntries =
                (propertyEntries.getLength() == 0)
                        ? doc.getElementsByTagName(queryProperty)
                        : propertyEntries;
        assertTrue(propertyEntries.getLength() > 0);
        for (int i = 0; i < propertyEntries.getLength(); i++) {
            assertTrue(propertyEntries.item(i).getTextContent().equals(queryPropertyValue));
        }
    }

    @Test
    public void validCompoundQueryContainsExpectedDefect()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + URLEncoder.encode(
                                queryProperty
                                        + "=\""
                                        + queryPropertyValue
                                        + "\" and "
                                        + queryComparisonProperty
                                        + ">=\""
                                        + queryComparisonValue
                                        + "\"",
                                "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryProperty
                        + ","
                        + queryComparisonProperty;

        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml");
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // Make sure each entry has a matching property element with a value that matches the query
        NodeList lst = doc.getElementsByTagNameNS("*", queryProperty);
        lst = (lst.getLength() == 0) ? doc.getElementsByTagName(queryProperty) : lst;
        assertTrue(lst.getLength() > 0);
        for (int i = 0; i < lst.getLength(); i++) {
            assertTrue(lst.item(i).getTextContent().equals(queryPropertyValue));
        }
        lst = doc.getElementsByTagNameNS("*", queryComparisonProperty);
        lst = (lst.getLength() == 0) ? doc.getElementsByTagName(queryComparisonProperty) : lst;
        assertTrue(lst.getLength() > 0);
        for (int i = 0; i < lst.getLength(); i++) {
            assertTrue(lst.item(i).getTextContent().compareTo(queryComparisonValue) >= 0);
        }
    }

    @Test
    public void validNotEqualQueryContainsExpectedDefect()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryProperty
                        + URLEncoder.encode("!=\"" + queryPropertyValue + "\"", "UTF-8")
                        + "&oslc_cm.properties="
                        + queryProperty;

        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml");
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // verify that the results did not contain entries whose property = propertyValue
        NodeList lst = doc.getElementsByTagNameNS("*", queryProperty);
        for (int i = 0; i < lst.getLength(); i++) {
            assertFalse(lst.item(i).getTextContent().equals(queryPropertyValue));
        }
    }

    @Test
    public void validLessThanQueryContainsExpectedDefects()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    ParseException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryComparisonProperty
                        + URLEncoder.encode("<=\"" + queryComparisonValue + "\"", "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryComparisonProperty;

        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml");
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // Verify that all returned items were modified before yesterday
        NodeList lst = doc.getElementsByTagNameNS("*", queryComparisonProperty);
        lst = (lst.getLength() == 0) ? doc.getElementsByTagName(queryComparisonProperty) : lst;
        assertTrue(lst.getLength() > 0);
        // TODO: How to validate the list is what is expected?
    }

    @Test
    public void validGreaterThanQueriesContainExpectedDefects()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    ParseException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryComparisonProperty
                        + URLEncoder.encode(">=\"" + queryComparisonValue + "\"", "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryComparisonProperty;

        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml");
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // Verify that all returned items were modified before yesterday
        NodeList lst = doc.getElementsByTagNameNS("*", queryComparisonProperty);
        lst = (lst.getLength() == 0) ? doc.getElementsByTagName(queryComparisonProperty) : lst;
        assertTrue(lst.getLength() > 0);
        for (int i = 0; i < lst.getLength(); i++) {
            assertTrue(lst.item(i).getTextContent().compareTo(queryComparisonValue) >= 0);
        }
    }

    @Test
    public void invalidQueryReturnsErrorCode()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException {
        String query = getQueryBase();
        query = query + "oslc_cm.query=notrealthing" + URLEncoder.encode("=\"defect\"", "UTF-8");
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml");
        resp.close();
        // Make sure we get a 400 (BAD REQUEST) for an invalid field
        assertTrue(resp.getStatus() == 400);
    }

    @Test
    public void fulltextSearchReturnsScoreValueInResults()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query=oslc_cm:searchTerms="
                        + URLEncoder.encode("\"" + fullTextSearchTerm + "\"", "UTF-8");
        // Get response
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/xml");
        String respBody = resp.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

        // Verify that each score element is non-negative
        NodeList scores =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate("//oslc_cm:score", doc, XPathConstants.NODESET);
        assertTrue(scores.getLength() > 0);
        for (int i = 0; i < scores.getLength(); i++) {
            Node score = scores.item(i);
            assertNotNull(score);
            assertTrue(Integer.parseInt(score.getTextContent()) >= 0);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validEqualsTypeQueryContainsExpectedDefectJson()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        // Form the query
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryProperty
                        + URLEncoder.encode("=\"" + queryPropertyValue + "\"", "UTF-8")
                        + "&oslc_cm.properties="
                        + queryProperty;
        // Get the response in Json
        Iterator<HashMap<String, String>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, String> lhm = iter.next();
            assertTrue(lhm.get(queryProperty).equals(queryPropertyValue));
        }
    }

    private String getQueryBase() {
        String query =
                (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&";
        return query;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validTypeQueryReturnsOnlyTypeJson()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryProperty
                        + URLEncoder.encode("=\"" + queryPropertyValue + "\"", "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryProperty;

        Iterator<HashMap<String, String>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, String> lhm = iter.next();
            assertTrue(lhm.keySet().contains(queryProperty));
            assertTrue(lhm.get(queryProperty).equals(queryPropertyValue));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validCompoundQueryContainsExpectedDefectJson()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + URLEncoder.encode(
                                queryProperty
                                        + "=\""
                                        + queryPropertyValue
                                        + "\" and "
                                        + queryComparisonProperty
                                        + ">=\""
                                        + queryComparisonValue
                                        + "\"",
                                "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryProperty
                        + ","
                        + queryComparisonProperty;

        Iterator<HashMap<String, String>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, String> lhm = iter.next();
            assertTrue(lhm.keySet().contains(queryProperty));
            assertTrue(lhm.get(queryProperty).equals(queryPropertyValue));
            assertTrue(lhm.keySet().contains(queryComparisonProperty));
            assertTrue(lhm.get(queryComparisonProperty).compareTo(queryComparisonValue) >= 0);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validNotEqualQueryContainsExpectedDefectJson()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryProperty
                        + URLEncoder.encode("!=\"" + queryPropertyValue + "\"", "UTF-8")
                        + "&oslc_cm.properties="
                        + queryProperty;

        Iterator<HashMap<String, String>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, String> lhm = iter.next();
            assertFalse(lhm.get(queryProperty).equals(queryPropertyValue));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validLessThanQueryContainsExpectedDefectsJson() throws IOException, JSONException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryComparisonProperty
                        + URLEncoder.encode("<=\"" + queryComparisonValue + "\"", "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryComparisonProperty;

        // Get response in json
        Iterator<HashMap<String, String>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, String> lhm = iter.next();
            assertTrue(lhm.keySet().contains(queryComparisonProperty));
            // assertTrue(lhm.get(queryComparisonProperty).compareTo(queryComparisonValue) <= 0);
        }
    }

    private JSONArray processJSONQuery(String query) throws IOException, JSONException {
        Response resp =
                OSLCUtils.getResponseFromUrl(
                        baseUrl, currentUrl + query, basicCreds, "application/json");
        String respBody = resp.readEntity(String.class);
        // Create mapping of json variables
        JSONArtifact userData = JSON.parse(respBody);
        JSONObject resultJson = null;
        if (userData instanceof JSONArtifact) {
            resultJson = (JSONObject) userData;
        }
        int totalCount = (Integer) resultJson.get("oslc_cm:totalCount");
        assertTrue(totalCount > 0);
        // Verify that all results are less than the comparison value
        JSONArray results = (JSONArray) resultJson.get("oslc_cm:results");
        return results;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validGreaterThanQueriesContainExpectedDefectsJson()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    ParseException,
                    JSONException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query="
                        + queryComparisonProperty
                        + URLEncoder.encode(">=\"" + queryComparisonValue + "\"", "UTF-8")
                        + "&"
                        + URLEncoder.encode("oslc_cm.properties", "UTF-8")
                        + "="
                        + queryComparisonProperty;

        // Get response in json
        Iterator<HashMap<String, String>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, String> lhm = iter.next();
            assertTrue(lhm.keySet().contains(queryComparisonProperty));
            assertTrue(lhm.get(queryComparisonProperty).compareTo(queryComparisonValue) >= 0);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fulltextSearchReturnsScoreValueInResultsJson()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryBase();
        query =
                query
                        + "oslc_cm.query=oslc_cm:searchTerms="
                        + URLEncoder.encode("\"" + fullTextSearchTerm + "\"", "UTF-8");

        // Get response in json
        Iterator<HashMap<String, Object>> iter = processJSONQuery(query).iterator();
        while (iter.hasNext()) {
            HashMap<String, Object> lhm = iter.next();
            assertTrue(lhm.keySet().contains("oslc_cm:score"));
            assertTrue((Integer) lhm.get("oslc_cm:score") >= 0);
        }
    }
}
