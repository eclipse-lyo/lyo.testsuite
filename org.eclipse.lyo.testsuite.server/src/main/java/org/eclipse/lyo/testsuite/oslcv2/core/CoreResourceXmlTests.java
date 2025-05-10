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
 *    Samuel Padgett - don't fail if queryAdditionalParameters is not defined
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
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

@RunWith(Parameterized.class)
public abstract class CoreResourceXmlTests extends TestsBase {
    protected HttpResponse response;
    protected String responseBody;
    protected Document doc;
    protected String node = "";
    protected static String resourceTypeQuery = "";
    protected static String xpathSubStmt = "";

    public CoreResourceXmlTests(String thisUrl)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {
        super(thisUrl);

        // If currentUrl is null, it means that the query didn't match any
        // records. This isn't exactly a failure, but there's nothing more we
        // can test.
        assumeNotNull(currentUrl);
        response =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, currentUrl, creds, OSLCConstants.CT_XML, headers);
        responseBody = EntityUtils.toString(response.getEntity());
        int sc = response.getStatusLine().getStatusCode();

        // Some records in the system might not be accessible to this user. This
        // isn't a failure, but there's nothing more we can test.
        assumeTrue(sc != HttpStatus.SC_FORBIDDEN && sc != HttpStatus.SC_UNAUTHORIZED);

        // Make sure the request succeeded before continuing.
        assertEquals(HttpStatus.SC_OK, sc);

        // Get XML Doc from response
        doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
    }

    @Parameters
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
                TestsBase.getCapabilityURLsUsingXML(
                        v, getxpathSubStmt(), getResourceTypeQuery(), serviceUrls, true);

        // Once we have the query URL, look for a resource to validate
        String where = setupProps.getProperty("changeRequestsWhere");
        if (where == null) {
            String queryProperty = setupProps.getProperty("queryEqualityProperty");
            String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
            where = queryProperty + "=\"" + queryPropertyValue + "\"";
        }

        String additionalParameters = setupProps.getProperty("queryAdditionalParameters", "");
        String query =
                (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&";
        query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";

        for (String queryBase : capabilityURLsUsingXML) {
            String queryUrl = OSLCUtils.addQueryStringToURL(queryBase, query);
            HttpResponse resp =
                    OSLCUtils.getResponseFromUrl(
                            setupBaseUrl, queryUrl, creds, OSLCConstants.CT_XML, headers);
            String respBody = EntityUtils.toString(resp.getEntity());
            EntityUtils.consume(resp.getEntity());
            assertTrue(
                    "Received " + resp.getStatusLine(),
                    (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK));

            // Get XML Doc from response
            Document doc = OSLCUtils.createXMLDocFromResponseBody(respBody);

            // Check for results by reference (rdf:resource)
            Node result = (Node) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODE);

            if (result == null)
                // No results by reference. Check for inline results (rdf:about)
                result =
                        (Node)
                                OSLCUtils.getXPath()
                                        .evaluate(
                                                "//rdfs:member/oslc_cm_v2:ChangeRequest/@rdf:about",
                                                doc,
                                                XPathConstants.NODE);
            if (result != null) results.add(result.getNodeValue());
            if (onlyOnce) break;
        }

        return toCollection(results);
    }

    @Test
    public void CoreResourceHasOneTitle() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "dc:title";

        NodeList titles =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertEquals("dc:title" + getFailureMessage(), 1, titles.getLength());
    }

    @Test
    public void CoreResourceHasAtMostOneDescription() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "dc:description";

        NodeList descriptions =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue("dc:description" + getFailureMessage(), descriptions.getLength() <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneIdentifier() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "dc:identifier";

        NodeList ids = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(getFailureMessage(), ids.getLength() <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneName() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "dc:name";

        NodeList names =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(getFailureMessage(), names.getLength() <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneCreatedDate() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "dc:created";

        NodeList createdDates =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(getFailureMessage(), createdDates.getLength() <= 1);
        // If there is a created date, verify the format.
        if (createdDates.getLength() > 0) {
            try {
                DatatypeConverter.parseDateTime(createdDates.item(0).getTextContent());
            } catch (Exception e) {
                fail("Created date not in valid XSD format");
            }
        }
    }

    @Test
    public void CoreResourceHasAtMostOneModifiedDate() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "dc:modified";

        NodeList modifiedDates =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(getFailureMessage(), modifiedDates.getLength() <= 1);

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

    @Test
    public void CoreResourceHasAtMostOneDiscussion() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "oslc:discussion";

        NodeList discussions =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(getFailureMessage(), discussions.getLength() <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneInstanceShape() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "oslc:instanceShape";

        NodeList instances =
                (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);
        assertTrue(getFailureMessage(), instances.getLength() <= 1);
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
