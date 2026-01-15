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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** This class provides JUnit tests for the validation of OSLCv2 ServiceProvider documents */
public class ServiceProviderXmlTests extends TestsBase {

    private Response response;
    private static String fContentType = OSLCConstants.CT_XML;
    private String responseBody;
    private Document doc;

    public void initServiceProviderXmlTests(String url) throws IOException, ParserConfigurationException, SAXException {

        setup(url);

        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);
        try {
            if (response.getStatus() <= 299) {
                responseBody = response.readEntity(String.class);
                // Get XML Doc from response
                doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
            } else {
                throw new IllegalStateException("Request failed");
            }
        } finally {
            response.close();
        }
    }

    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively, in order to find the
        // URLs of all
        // service description documents of the REST service.

        staticSetup();

        Collection<Object[]> coll = getReferencedUrls(setupProps.getProperty("baseUri"));
        return coll;
    }

    public static Collection<Object[]> getReferencedUrls(String base)
            throws IOException, XPathException, ParserConfigurationException, SAXException {
        ArrayList<String> serviceURLsUsingXML = TestsBase.getServiceProviderURLsUsingXML(base, false);
        return toCollection(serviceURLsUsingXML);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void baseUrlIsValid(String url) {
        initServiceProviderXmlTests(url);
        assertNotNull(setupBaseUrl, "Could not locate a service provider document");

        // Get the status, make sure 200 OK
        assertTrue(
                response.getStatus() == 200,
                "Expected 200-Ok but received " + response.getStatusInfo().getStatusCode());

        // Verify we got a response
        assertNotNull(responseBody);
    }

    @ParameterizedTest
    @Disabled("Neither HTTP/1.1 nor OSLC Core 2.0 REQUIRE a 406 Not Acceptable response. "
            + "It doesn't appear to be mentioned in the OSLC 2.0 Core specification. "
            + "This is a SHOULD per HTTP/1.1, but not a MUST. See "
            + "http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1")
    @MethodSource("getAllDescriptionUrls")
    public void invalidContentTypeGivesNotSupportedOPTIONAL(String url) throws IOException {
        initServiceProviderXmlTests(url);
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, "invalid/content-type", headers);
        String respType = (resp.getHeaderString("Content-Type") == null) ? "" : resp.getHeaderString("Content-Type");
        resp.close();
        assertTrue(
                resp.getStatus() == 406 || respType.contains("application/svg+xml"),
                "Expected 406 but received "
                        + resp.getStatus()
                        + ",Content-type='invalid/content-type' but received "
                        + respType);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void responseContentTypeIsXML(String url) throws IOException {
        initServiceProviderXmlTests(url);
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);
        // Make sure the response to this URL was of valid type
        resp.close();
        String contentType = resp.getHeaderString("Content-Type");
        String contentTypeSplit[] = contentType.split(";");
        contentType = contentTypeSplit[0];

        assertTrue(contentType.equalsIgnoreCase("application/xml")
                || contentType.equalsIgnoreCase("application/rdf+xml")
                || contentType.equalsIgnoreCase("text/xml"));
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void misplacedParametersDoNotEffectResponse(String url) throws IOException {
        initServiceProviderXmlTests(url);
        var baseResp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);

        Model baseRespModel = ModelFactory.createDefaultModel();
        baseRespModel.read(
                baseResp.readEntity(InputStream.class),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(baseRespModel);

        String badParmUrl = currentUrl + "?oslc_cm:query";

        var parameterResp = OSLCUtils.getResponseFromUrl(setupBaseUrl, badParmUrl, creds, fContentType, headers);

        Model badParmModel = ModelFactory.createDefaultModel();
        badParmModel.read(
                parameterResp.readEntity(InputStream.class),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, badParmUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(badParmModel);

        assertTrue(baseRespModel.isIsomorphicWith(badParmModel));
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasAtMostOneTitle(String url) throws XPathException {
        initServiceProviderXmlTests(url);
        // Verify that the ServiceProvider has at most one dc:title child element
        NodeList providerChildren =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider/*", doc, XPathConstants.NODESET);

        int numTitles = 0;
        for (int i = 0; i < providerChildren.getLength(); i++) {
            Node child = providerChildren.item(i);
            if (child.getLocalName() != null
                    && child.getLocalName().equals("title")
                    && child.getNamespaceURI().equals(OSLCConstants.DC)) {
                numTitles++;
            }
        }
        assertTrue(numTitles <= 1, "Expected number of dcterms:titles to be <=1 but was:" + numTitles);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasAtMostOnePublisher(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get the listed ServiceProvider elements
        NodeList providerChildren =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider/*", doc, XPathConstants.NODESET);
        int numPublishers = 0;
        for (int i = 0; i < providerChildren.getLength(); i++) {
            Node child = providerChildren.item(i);
            if (child.getLocalName() != null
                    && child.getNamespaceURI().equals(OSLCConstants.DC)
                    && child.getLocalName().equals("publisher")) {
                numPublishers++;
            }
        }
        assert (numPublishers <= 1);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void publisherElementsAreValid(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get all Publisher xml blocks
        NodeList publishers = (NodeList) OSLCUtils.getXPath().evaluate("//dc:publisher/*", doc, XPathConstants.NODESET);

        // Verify that each block contains a title and identifier, and at most one icon and label
        for (int i = 0; i < publishers.getLength(); i++) {
            NodeList publisherElements = publishers.item(i).getChildNodes();
            int titleCount = 0;
            int identifierCount = 0;
            int iconCount = 0;
            int labelCount = 0;
            for (int j = 0; j < publisherElements.getLength(); j++) {
                Node ele = publisherElements.item(j);
                if (ele.getLocalName() == null) {
                    continue;
                }
                if (ele.getNamespaceURI().equals(OSLCConstants.DC)
                        && ele.getLocalName().equals("title")) {
                    titleCount++;
                }
                if (ele.getNamespaceURI().equals(OSLCConstants.DC)
                        && ele.getLocalName().equals("identifier")) {
                    identifierCount++;
                }
                if (ele.getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                        && ele.getLocalName().equals("label")) {
                    labelCount++;
                }
                if (ele.getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                        && ele.getLocalName().equals("icon")) {
                    iconCount++;
                }
            }
            assertTrue(titleCount == 1);
            assertTrue(identifierCount == 1);
            assertTrue(iconCount <= 1);
            assertTrue(labelCount <= 1);
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasService(String url) throws XPathException {
        initServiceProviderXmlTests(url);
        // Verify the ServiceProvider has at least one rdf:service child element
        Node service = (Node)
                OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider/oslc_v2:service", doc, XPathConstants.NODE);
        assertNotNull(service);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasValidDetails(String url) throws XPathException, DOMException, IOException {
        initServiceProviderXmlTests(url);
        // Verify the ServiceProvider has a valid oslc:details attribute
        Node details = (Node)
                OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider/oslc_v2:details", doc, XPathConstants.NODE);
        assertNotNull(details, "oslc:details element is required for oslc:ServiceProfile");
        Node node = details.getAttributes().getNamedItemNS(OSLCConstants.RDF, "resource");
        assertNotNull(node.getNodeValue());
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, node.getNodeValue(), creds, "");

        resp.readEntity(String.class);
        resp.close();

        assertFalse(resp.getStatus() == 404);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void prefixDefinitionsAreValid(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get all the prefix definitions
        NodeList prefixes =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:PrefixDefinition", doc, XPathConstants.NODESET);

        for (int i = 0; i < prefixes.getLength(); i++) {
            NodeList subNodes =
                    (NodeList) OSLCUtils.getXPath().evaluate("./*", prefixes.item(i), XPathConstants.NODESET);
            int prefixCount = 0;
            int baseCount = 0;
            // Check all the children of this prefix definition
            for (int j = 0; j < subNodes.getLength(); j++) {
                Node pChild = subNodes.item(j);
                if (pChild.getLocalName() == null) {
                    continue;
                }
                if (pChild.getLocalName().equals("prefix")
                        && pChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    prefixCount++;
                }
                if (pChild.getLocalName().equals("prefixBase")
                        && pChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    baseCount++;
                }
            }
            // Make sure the prefix definition had 1 prefix and 1 prefixBase
            assertEquals(1, prefixCount);
            assertEquals(1, baseCount);
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasAtMostOneOAuthElement(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Check root for OAuth block, make sure it only has at most one
        NodeList rootChildren =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider/*", doc, XPathConstants.NODESET);
        int numOAuthElements = 0;
        for (int i = 0; i < rootChildren.getLength(); i++) {
            if (rootChildren.item(i).getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                    && rootChildren.item(i).getLocalName().equals("oauthConfiguration")) {
                numOAuthElements++;
            }
        }
        assert (numOAuthElements <= 1);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void oAuthElementsAreValid(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get all oauthAuthorization xml blocks
        NodeList oAuthElement =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:oauthConfiguration/*", doc, XPathConstants.NODESET);

        // Verify the block contains the required expected elements
        for (int i = 0; i < oAuthElement.getLength(); i++) {
            NodeList oAuthChildren = oAuthElement.item(i).getChildNodes();
            int reqTokenCount = 0;
            int authCount = 0;
            int accessCount = 0;
            for (int j = 0; j < oAuthChildren.getLength(); j++) {
                Node oAuthNode = oAuthChildren.item(j);
                if (oAuthNode.getLocalName() == null) {
                    continue;
                }
                if (oAuthNode.getLocalName().equals("oauthRequestTokenURI")
                        && oAuthNode.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    reqTokenCount++;
                }
                if (oAuthNode.getLocalName().equals("authorizationURI")
                        && oAuthNode.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    authCount++;
                }
                if (oAuthNode.getLocalName().equals("oauthAccessTokenURI")
                        && oAuthNode.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    accessCount++;
                }
            }
            assertTrue(reqTokenCount == 1);
            assertTrue(authCount == 1);
            assertTrue(accessCount == 1);
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void eachServiceHasOneDomain(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get the services referenced
        NodeList services = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_v2:service/oslc_v2:Service", doc, XPathConstants.NODESET);

        for (int i = 0; i < services.getLength(); i++) {
            NodeList serviceChildren = services.item(i).getChildNodes();
            int numDomains = 0;
            // Check the service for domain child elements
            for (int j = 0; j < serviceChildren.getLength(); j++) {
                Node sChild = serviceChildren.item(j);

                if (sChild.getLocalName() == null) {
                    continue;
                }
                if (sChild.getLocalName().equals("domain")
                        && sChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numDomains++;
                }
            }
            // Make sure the service has exactly one domain element
            assertEquals(1, numDomains);
        }

        // Test to make sure the domain contains the test version
        NodeList detail = (NodeList) OSLCUtils.getXPath()
                .evaluate("//oslc_v2:service/oslc_v2:Service/oslc_v2:domain", doc, XPathConstants.NODESET);
        boolean domainFound = false;
        for (int i = 0; i < detail.getLength(); i++) {
            Node node = detail.item(i);
            if (node.getAttributes()
                    .getNamedItemNS(OSLCConstants.RDF, "resource")
                    .getNodeValue()
                    .equals(testVersion)) {
                domainFound = true;
            }
        }
        assertTrue(domainFound, "Domain " + testVersion + " not found for any Service resource");
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void creationFactoriesAreValid(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get all creation factories
        NodeList factories =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:CreationFactory", doc, XPathConstants.NODESET);
        for (int i = 0; i < factories.getLength(); i++) {
            NodeList factoryChildren = factories.item(i).getChildNodes();
            int numTitles = 0;
            int numLabels = 0;
            int numCreation = 0;
            // Check children of current creation factory
            for (int j = 0; j < factoryChildren.getLength(); j++) {
                Node fChild = factoryChildren.item(j);
                if (fChild.getLocalName() == null) {
                    continue;
                }
                if (fChild.getLocalName().equals("title")
                        && fChild.getNamespaceURI().equals(OSLCConstants.DC)) {
                    numTitles++;
                }
                if (fChild.getLocalName().equals("creation")
                        && fChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numCreation++;
                }
                if (fChild.getLocalName().equals("label")
                        && fChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numLabels++;
                }
            }
            // Make sure the factory has a title, a creation element, and at most 1 label
            assertTrue(numTitles == 1);
            assertTrue(numCreation == 1);
            assertTrue(numLabels <= 1);
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void queryCapabilityBlocksAreValid(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get all query blocks
        NodeList queryBlocks =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:QueryCapability", doc, XPathConstants.NODESET);

        for (int i = 0; i < queryBlocks.getLength(); i++) {
            NodeList queryChildren = queryBlocks.item(i).getChildNodes();
            int numTitles = 0;
            int numLabels = 0;
            int numQueryBase = 0;
            int numResourceShape = 0;
            // Check children of each block
            for (int j = 0; j < queryChildren.getLength(); j++) {
                Node qChild = queryChildren.item(j);
                if (qChild.getLocalName() == null) {
                    continue;
                }
                if (qChild.getLocalName().equals("title")
                        && qChild.getNamespaceURI().equals(OSLCConstants.DC)) {
                    numTitles++;
                }
                if (qChild.getLocalName().equals("queryBase")
                        && qChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numQueryBase++;
                }
                if (qChild.getLocalName().equals("label")
                        && qChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numLabels++;
                }
                if (qChild.getLocalName().equals("resourceShape")
                        && qChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numResourceShape++;
                }
            }
            // Make sure we have a title, a queryBase, and at most one label/resourceShape
            assertTrue(numTitles == 1);
            assertTrue(numQueryBase == 1);
            assertTrue(numLabels <= 1);
            assertTrue(numResourceShape <= 1);
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void dialogsAreValid(String url) throws XPathExpressionException {
        initServiceProviderXmlTests(url);
        // Get all dialogs
        NodeList dialogs = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:Dialog", doc, XPathConstants.NODESET);

        for (int i = 0; i < dialogs.getLength(); i++) {
            NodeList dialogChildren = dialogs.item(i).getChildNodes();
            int numTitles = 0;
            int numLabels = 0;
            int numDialog = 0;
            int numHintWidth = 0;
            int numHintHeight = 0;
            // Check children of dialog
            for (int j = 0; j < dialogChildren.getLength(); j++) {
                Node dChild = dialogChildren.item(j);
                if (dChild.getLocalName() == null) {
                    continue;
                }
                if (dChild.getLocalName().equals("title")
                        && dChild.getNamespaceURI().equals(OSLCConstants.DC)) {
                    numTitles++;
                }
                if (dChild.getLocalName().equals("dialog")
                        && dChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numDialog++;
                }
                if (dChild.getLocalName().equals("label")
                        && dChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numLabels++;
                }
                if (dChild.getLocalName().equals("hintWidth")
                        && dChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numHintWidth++;
                }
                if (dChild.getLocalName().equals("hintHeight")
                        && dChild.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    numHintHeight++;
                }
            }
            // Make sure we have a title, a dialog child element, at most one
            // label/hintWidth/hintHeight
            assertTrue(numTitles == 1);
            assertTrue(numDialog == 1);
            assertTrue(numLabels <= 1);
            assertTrue(numHintWidth <= 1);
            assertTrue(numHintHeight <= 1);
        }
    }
}
