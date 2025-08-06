/*
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
 *    Yuhong Yin
 */
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the basic validation of query factories
 * as specified in the OSLC version 2 spec. This version of the query tests only
 * tests the basic status code and form of the query responses, as without
 * shapes implemented it is difficult to represent the needed various templates
 * of different change request types and to query for the templates.
 */
@RunWith(Parameterized.class)
public class SimplifiedQueryJsonTests extends SimplifiedQueryBaseTests {

    public SimplifiedQueryJsonTests(String thisUri) {
        super(thisUri);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the
        // REST service in order to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively,
        // in order to find the URLs of all
        // query factories of the REST service.
        String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";
        ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
        ArrayList<String> capabilityURLsUsingXML =
                TestsBase.getCapabilityURLsUsingXML(v, serviceUrls, true);
        return toCollection(capabilityURLsUsingXML);
    }

    protected void validateNonEmptyResponse(String query)
            throws XPathExpressionException,
                    IOException,
                    ParserConfigurationException,
                    SAXException,
                    JSONException {
        String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);

        // Send JSON request
        Response response =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, queryUrl, creds, OSLCConstants.CT_JSON, headers);

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
        assertTrue(
                "query response is in Json format.",
                JSON.parse(responseBody) instanceof JSONArtifact);
    }

    @Test
    public void validEqualsQueryContainsExpectedResource()
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryUrlForValidEqualsQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validNotEqualQueryContainsExpectedResource()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryUrlForValidNotEqualQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validLessThanQueryContainsExpectedResources()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryUrlForValidLessThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validGreaterThanQueryContainsExpectedDefects()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryUrlForValidGreaterThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void validCompoundQueryContainsExpectedResource()
            throws IOException,
                    SAXException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    JSONException {
        String query = getQueryUrlForValidCompoundQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @Test
    public void fullTextSearchContainsExpectedResults()
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException,
                    JSONException {
        if (!getFullTextSearch()) return;

        String query = getQueryUrlForFullTextSearchContainsExpectedResults();
        validateNonEmptyResponse(query);
    }
}
