/*
 * Copyright (c) 2012, 2014, 2025 IBM Corporation and others
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the basic validation of query factories as specified in the OSLC version 2 spec.
 * This version of the query tests only tests the basic status code and form of the query responses, as without shapes
 * implemented it is difficult to represent the needed various templates of different change request types and to query
 * for the templates.
 */
public class SimplifiedQueryJsonTests extends SimplifiedQueryBaseTests {

    public SimplifiedQueryJsonTests(String thisUri) {
        super(thisUri);
    }

    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the
        // REST service in order to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively,
        // in order to find the URLs of all
        // query factories of the REST service.
        String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";
        ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
        ArrayList<String> capabilityURLsUsingXML = TestsBase.getCapabilityURLsUsingXML(v, serviceUrls, true);
        return toCollection(capabilityURLsUsingXML);
    }

    protected void validateNonEmptyResponse(String query)
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException, JSONException {
        String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);

        // Send JSON request
        Response response = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds, OSLCConstants.CT_JSON, headers);

        int statusCode = response.getStatus();
        if (Response.Status.OK.getStatusCode() != statusCode) {
            response.close();
            throw new IOException("Response code: " + statusCode + " for " + queryUrl);
        }

        String responseBody = response.readEntity(String.class);

        //
        // Validate JSON response
        //
        // TODO: add more detailed validation
        assertTrue(JSON.parse(responseBody) instanceof JSONArtifact, "query response is in Json format.");
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validEqualsQueryContainsExpectedResource(String thisUri)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, JSONException {
        initSimplifiedQueryJsonTests(thisUri);
        String query = getQueryUrlForValidEqualsQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validNotEqualQueryContainsExpectedResource(String thisUri)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, JSONException {
        initSimplifiedQueryJsonTests(thisUri);
        String query = getQueryUrlForValidNotEqualQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validLessThanQueryContainsExpectedResources(String thisUri)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, JSONException {
        initSimplifiedQueryJsonTests(thisUri);
        String query = getQueryUrlForValidLessThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validGreaterThanQueryContainsExpectedDefects(String thisUri)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, JSONException {
        initSimplifiedQueryJsonTests(thisUri);
        String query = getQueryUrlForValidGreaterThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validCompoundQueryContainsExpectedResource(String thisUri)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, JSONException {
        initSimplifiedQueryJsonTests(thisUri);
        String query = getQueryUrlForValidCompoundQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void fullTextSearchContainsExpectedResults(String thisUri)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, JSONException {
        initSimplifiedQueryJsonTests(thisUri);
        if (!getFullTextSearch()) return;

        String query = getQueryUrlForFullTextSearchContainsExpectedResults();
        validateNonEmptyResponse(query);
    }
}
