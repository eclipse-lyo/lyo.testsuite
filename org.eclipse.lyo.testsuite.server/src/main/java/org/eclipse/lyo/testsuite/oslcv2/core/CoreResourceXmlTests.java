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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class CoreResourceXmlTests extends TestsBase {
    protected Response response;
    protected String responseBody;
    protected Document doc;
    protected String node = "";
    protected static String resourceTypeQuery = "";
    protected static String xpathSubStmt = "";

    public void initCoreResourceXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        super(thisUrl);

        // If currentUrl is null, it means that the query didn't match any
        // records. This isn't exactly a failure, but there's nothing more we
        // can test.
        assumeNotNull(currentUrl);
        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, OSLCConstants.CT_XML, headers);
        responseBody = response.readEntity(String.class);
        int sc = response.getStatus();

        // Some records in the system might not be accessible to this user. This
        // isn't a failure, but there's nothing more we can test.
        assumeTrue(sc != Status.FORBIDDEN.getStatusCode() && sc != Status.UNAUTHORIZED.getStatusCode());

        // Make sure the request succeeded before continuing.
        assertEquals(Response.Status.OK.getStatusCode(), sc);

        // Get XML Doc from response
        doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
    }

    protected static Collection<Object[]> getAllDescriptionUrls(String eval)
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        ArrayList<String> results = new ArrayList<String>();

        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively, in order to find the
        // URLs of all
        // query factories of the REST service.

        String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";

        ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
        ArrayList<String> capabilityURLsUsingXML =
                TestsBase.getCapabilityURLsUsingXML(v, getxpathSubStmt(), getResourceTypeQuery(), serviceUrls, true);

        // Once we have the query URL, look for a resource to validate
        String where = setupProps.getProperty("changeRequestsWhere");
        if (where == null) {
            String queryProperty = setupProps.getProperty("queryEqualityProperty");
            String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
            where = queryProperty + "=\"" + queryPropertyValue + "\"";
        }

        String additionalParameters = setupProps.getProperty("queryAdditionalParameters", "");
        String query = (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&";
        query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";

        for (String queryBase : capabilityURLsUsingXML) {
            String queryUrl = OSLCUtils.addQueryStringToURL(queryBase, query);
            Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds, OSLCConstants.CT_XML, headers);
            String respBody = resp.readEntity(String.class);
            resp.close();
            assertTrue((resp.getStatus() == Response.Status.OK.getStatusCode()), "Received " + resp.getStatus());

            // Get XML Doc from response
            Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

            // Check for results by reference (rdf:resource)
            Node result = (Node) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODE);

            if (result == null)
                // No results by reference. Check for inline results (rdf:about)
                result = (Node) OSLCUtils.getXPath()
                        .evaluate("//rdfs:member/oslc_cm_v2:ChangeRequest/@rdf:about", doc, XPathConstants.NODE);
            if (result != null) results.add(result.getNodeValue());
            if (onlyOnce) break;
        }

        return toCollection(results);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasOneTitle(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "dc:title";

        NodeList titles = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertEquals(1, titles.getLength(), "dc:title" + getFailureMessage());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneDescription(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "dc:description";

        NodeList descriptions = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue(descriptions.getLength() <= 1, "dc:description" + getFailureMessage());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneIdentifier(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "dc:identifier";

        NodeList ids = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(ids.getLength() <= 1, getFailureMessage());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneName(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "dc:name";

        NodeList names = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(names.getLength() <= 1, getFailureMessage());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneCreatedDate(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "dc:created";

        NodeList createdDates = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(createdDates.getLength() <= 1, getFailureMessage());
        // If there is a created date, verify the format.
        if (createdDates.getLength() > 0) {
            try {
                DatatypeConverter.parseDateTime(createdDates.item(0).getTextContent());
            } catch (Exception e) {
                fail("Created date not in valid XSD format");
            }
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneModifiedDate(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "dc:modified";

        NodeList modifiedDates = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(modifiedDates.getLength() <= 1, getFailureMessage());

        // If there is a modified date, verify the format.
        if (modifiedDates.getLength() > 0) {
            try {
                final String dateString = modifiedDates.item(0).getTextContent();
                DatatypeConverter.parseDateTime(dateString);
            } catch (Exception e) {
                fail("Modified date not in valid XSD format");
            }
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneDiscussion(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc:discussion";

        NodeList discussions = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(discussions.getLength() <= 1, getFailureMessage());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void CoreResourceHasAtMostOneInstanceShape(String thisUrl) throws XPathExpressionException {
        initCoreResourceXmlTests(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc:instanceShape";

        NodeList instances = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(instances.getLength() <= 1, getFailureMessage());
    }

    protected String getFailureMessage() {
        return " failed for  <" + currentUrl + ">";
    }

    protected void setNode(String namespace, String resource) {
        node = namespace + ":" + resource;
    }

    protected String getNode() {
        return node;
    }

    protected static void setResourceTypeQuery(String rT) {
        resourceTypeQuery = rT;
    }

    protected static String getResourceTypeQuery() {
        return resourceTypeQuery;
    }

    protected static void setxpathSubStmt(String x) {
        xpathSubStmt = x;
    }

    protected static String getxpathSubStmt() {
        return xpathSubStmt;
    }
}
