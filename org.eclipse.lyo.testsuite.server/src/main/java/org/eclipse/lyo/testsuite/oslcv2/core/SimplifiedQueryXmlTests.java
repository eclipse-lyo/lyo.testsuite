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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the basic validation of query factories as specified in the OSLC version 2 spec.
 * This version of the query tests only tests the basic status code and form of the query responses, as without shapes
 * implemented it is difficult to represent the needed various templates of different change request types and to query
 * for the templates.
 */
@RunWith(Parameterized.class)
public class SimplifiedQueryXmlTests extends SimplifiedQueryBaseTests {

    public SimplifiedQueryXmlTests(String thisUri) {
        super(thisUri);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {

        staticSetup();

        ArrayList<String> capabilityURLsUsingXML = new ArrayList<String>();

        String useThisQuery = setupProps.getProperty("useThisQuery");

        if (useThisQuery != null) {
            capabilityURLsUsingXML.add(useThisQuery);
        } else {
            // Checks the ServiceProviderCatalog at the specified baseUrl of the
            // REST service in order to grab all urls
            // to other ServiceProvidersCatalogs contained within it, recursively,
            // in order to find the URLs of all
            // query factories of the REST service.
            String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";
            ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);

            capabilityURLsUsingXML = TestsBase.getCapabilityURLsUsingXML(v, serviceUrls, true);
        }

        return toCollection(capabilityURLsUsingXML);
    }

    protected void validateNonEmptyResponse(String query)
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);
        Response response = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds, OSLCConstants.CT_XML, headers);
        int statusCode = response.getStatus();
        if (Response.Status.OK.getStatusCode() != statusCode) {
            response.close();
            throw new IOException("Response code: " + statusCode + " for " + queryUrl);
        }

        String responseBody = response.readEntity(String.class);

        Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
        Node results = (Node) OSLCUtils.getXPath().evaluate("//oslc:ResponseInfo/@rdf:about", doc, XPathConstants.NODE);

        // Only test oslc:ResponseInfo if found
        if (results != null) {
            results = (Node) OSLCUtils.getXPath().evaluate("//oslc:totalCount", doc, XPathConstants.NODE);
            if (results != null) {
                int totalCount = Integer.parseInt(results.getTextContent());
                assertTrue("Expected oslc:totalCount > 0", totalCount > 0);
            }

            NodeList resultList = (NodeList)
                    OSLCUtils.getXPath().evaluate("//rdf:Description/rdfs:member", doc, XPathConstants.NODESET);
            assertNotNull("Expected rdfs:member(s)", resultList);
            assertNotNull("Expected > 1 rdfs:member(s)", resultList.getLength() > 0);
        }
    }

    @Test
    public void validEqualsQueryContainsExpectedResource()
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String query = getQueryUrlForValidEqualsQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validNotEqualQueryContainsExpectedResource()
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        String query = getQueryUrlForValidNotEqualQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validLessThanQueryContainsExpectedResources()
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, ParseException {
        String query = getQueryUrlForValidLessThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validGreaterThanQueryContainsExpectedDefects()
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, ParseException {
        String query = getQueryUrlForValidGreaterThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validCompoundQueryContainsExpectedResource()
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        String query = getQueryUrlForValidCompoundQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void fullTextSearchContainsExpectedResults()
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        if (!getFullTextSearch()) return;

        String query = getQueryUrlForFullTextSearchContainsExpectedResults();
        validateNonEmptyResponse(query);
    }
}
