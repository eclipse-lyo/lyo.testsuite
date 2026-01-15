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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** This class provides JUnit tests for the validation of the OSLCv2 fetching of resources. */
public class FetchResourceTests extends TestsBase {

    public void initFetchResourceTests(String url) {

        setup(url);
    }

    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {
        staticSetup();
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively, in order to find the
        // URLs of all
        // query capability of the REST service, then fetch the resource using the query service.

        ArrayList<String> capabilityURLsUsingRdfXml = null;
        String useThisQuery = setupProps.getProperty("useThisQuery");

        if (useThisQuery != null) {
            capabilityURLsUsingRdfXml = new ArrayList<String>();
            capabilityURLsUsingRdfXml.add(useThisQuery);
        } else {
            ArrayList<String> serviceUrls =
                    getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
            capabilityURLsUsingRdfXml =
                    TestsBase.getCapabilityURLsUsingRdfXml(OSLCConstants.QUERY_BASE_PROP, serviceUrls, true);
        }
        String where = setupProps.getProperty("changeRequestWhere");
        if (where == null) {
            String queryProperty = setupProps.getProperty("queryEqualityProperty");
            String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
            where = queryProperty + "=\"" + queryPropertyValue + "\"";
        }

        String additionalParameters = setupProps.getProperty("queryAdditionalParameters", "");
        String query = (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&";
        query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";

        ArrayList<String> results = new ArrayList<String>();
        for (String queryBaseUri : capabilityURLsUsingRdfXml) {
            String queryUrl = OSLCUtils.addQueryStringToURL(queryBaseUri, query);
            Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds, OSLCConstants.CT_RDF, headers);
            Model queryModel = ModelFactory.createDefaultModel();
            queryModel.read(resp.readEntity(InputStream.class), queryBaseUri, OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(queryModel);

            Property member = queryModel.createProperty(OSLCConstants.RDFS_MEMBER);
            Resource queryBase = queryModel.getResource(queryBaseUri);
            StmtIterator statements = queryModel.listStatements(queryBase, member, (RDFNode) null);
            while (statements.hasNext()) {
                results.add(statements.nextStatement().getObject().toString());
                if (onlyOnce) return toCollection(results);
            }
            if (!results.isEmpty() && onlyOnce) break;
        }
        return toCollection(results);
    }

    protected String getValidResourceUsingContentType(String requestType) throws IOException {
        Response resp = OSLCUtils.getResponseFromUrl(currentUrl, currentUrl, creds, requestType, headers);

        String responseBody = resp.readEntity(String.class);
        resp.close();
        assertEquals(
                Response.Status.OK.getStatusCode(),
                resp.getStatus(),
                "Expected response code 200 but received " + resp.getStatus());

        String contentType = OSLCUtils.getContentType(resp);

        assertEquals(requestType, contentType, "Expected content-type " + requestType + " but received " + contentType);

        return responseBody;
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void getValidResourceUsingRdfXml(String url) throws IOException {
        initFetchResourceTests(url);
        String body = getValidResourceUsingContentType(OSLCConstants.CT_RDF);

        Model rdfModel = ModelFactory.createDefaultModel();
        rdfModel.read(
                new ByteArrayInputStream(body.getBytes()),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(rdfModel);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void getValidResourceUsingXml(String url) throws IOException, ParserConfigurationException, SAXException {
        initFetchResourceTests(url);
        String body = getValidResourceUsingContentType(OSLCConstants.CT_XML);

        Document doc = OSLCUtils.createXMLDocFromResponseBody(body);
        assertNotNull(doc, "XML document did not parse successfully");
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void getValidResourceUsingCOMPACT(String url)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        initFetchResourceTests(url);
        String body = getValidResourceUsingContentType(OSLCConstants.CT_COMPACT);

        Document doc = OSLCUtils.createXMLDocFromResponseBody(body);

        Node compactNode = (Node) OSLCUtils.getXPath().evaluate("/*/oslc_v2:Compact", doc, XPathConstants.NODE);
        assertNotNull(compactNode);

        // Everything is optional in the oslc:Compact representation.

        NodeList nodeList = (NodeList) OSLCUtils.getXPath().evaluate("./dc:title", compactNode, XPathConstants.NODESET);
        int numNodes = nodeList.getLength();
        assertTrue(numNodes <= 1, "Expected number of dcterms:titles to be <=1 but was: " + numNodes);

        nodeList =
                (NodeList) OSLCUtils.getXPath().evaluate("./oslc_v2:shortTitle", compactNode, XPathConstants.NODESET);
        numNodes = nodeList.getLength();
        assertTrue(numNodes <= 1, "Expected number of oslc:shortTitles to be <=1 but was: " + numNodes);

        nodeList = (NodeList) OSLCUtils.getXPath().evaluate("./oslc_v2:icon", compactNode, XPathConstants.NODESET);
        numNodes = nodeList.getLength();
        assertTrue(numNodes <= 1, "Expected number of oslc:icon to be <=1 but was: " + numNodes);

        String iconUrl = null;
        if (numNodes == 1) {
            Node rdfAbout = nodeList.item(0).getAttributes().getNamedItemNS(OSLCConstants.RDF, "resource");
            assertNotNull(rdfAbout, "oslc:icon in oslc:Compact missing rdf:about attribute");
            iconUrl = rdfAbout.getTextContent();

            Response response = OSLCUtils.getResponseFromUrl(iconUrl, iconUrl, creds, "*/*", headers);
            int statusCode = response.getStatus();
            response.close();
            assertTrue(
                    200 <= statusCode && statusCode < 400,
                    "Fetching icon from " + iconUrl + " did not respond with expected code, received " + statusCode);
        }

        nodeList =
                (NodeList) OSLCUtils.getXPath().evaluate("./oslc_v2:smallPreview", compactNode, XPathConstants.NODESET);
        numNodes = nodeList.getLength();
        assertTrue(numNodes <= 1, "Expected number of oslc:smallPreview is 0 or 1 but was: " + numNodes);
        if (numNodes == 1) validateCompactPreview(nodeList);

        nodeList =
                (NodeList) OSLCUtils.getXPath().evaluate("./oslc_v2:largePreview", compactNode, XPathConstants.NODESET);
        numNodes = nodeList.getLength();
        assertTrue(numNodes <= 1, "Expected number of oslc:largePreview is 0 or 1 but was: " + numNodes);
        if (numNodes == 1) validateCompactPreview(nodeList);
    }

    /** Assume that nodeList.getLength()==1 */
    protected void validateCompactPreview(NodeList nodeList) throws IOException, XPathExpressionException {
        Node node = (Node) OSLCUtils.getXPath()
                .evaluate("./oslc_v2:Preview/oslc_v2:document/@rdf:resource", nodeList.item(0), XPathConstants.NODE);
        assertNotNull(node, "Expected number of oslc:Preview/oslc:document/@rdf:resource");
        String previewUrl = node.getTextContent();
        Response response = OSLCUtils.getResponseFromUrl(previewUrl, previewUrl, creds, "*/*", headers);

        int statusCode = response.getStatus();
        String contentType = response.getHeaderString("Content-Type");
        response.close();
        assertTrue(
                200 <= statusCode && statusCode < 400,
                "Fetching document preview from "
                        + previewUrl
                        + " did not respond with expected code, received "
                        + statusCode);
        assertTrue(
                contentType.startsWith("text/html"),
                "Expected HTML content type from preview document but received " + contentType);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void getResourceUsingInvalidContentType(String url) throws IOException {
        initFetchResourceTests(url);
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, "invalid/content-type", headers);
        String respType = OSLCUtils.getContentType(resp);
        resp.close();
        assertTrue(
                resp.getStatus() == 406 || respType.contains("invalid/content-type"),
                "Expected 406 but received "
                        + resp.getStatus()
                        + ", requested Content-type='invalid/content-type' but received "
                        + respType);
    }
}
