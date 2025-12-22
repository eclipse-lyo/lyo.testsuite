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
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of OSLC Service Provider Catalogs, for the 2.0 version of the OSLC
 * standard, as defined by the OSLC Core Spec.
 */
public class ServiceProviderCatalogXmlTests extends ServiceProviderCatalogBaseTests {

    private static final Logger log = LoggerFactory.getLogger(ServiceProviderCatalogXmlTests.class);
    // Base URL of the OSLC Service Provider Catalog to be tested
    protected String responseBody;
    protected Document doc;
    protected Response response = null;

    public void initServiceProviderCatalogXmlTests(String url) throws IOException, ParserConfigurationException, SAXException {
        super(url);

        fContentType = OSLCConstants.CT_XML;

        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);
        responseBody = response.readEntity(String.class);
        // Get XML Doc from response
        doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
    }

    public static Collection<Object[]> getAllServiceProviderCatalogUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the
        // REST service in order to grab all urls
        // to other ServiceProviders contained within it, recursively.

        staticSetup();

        Collection<Object[]> coll = getReferencedCatalogUrlsUsingXML(setupProps.getProperty("baseUri"));
        return coll;
    }

    public static Collection<Object[]> getReferencedCatalogUrlsUsingXML(String base)
            throws IOException, ParserConfigurationException, SAXException, XPathException {

        staticSetup();

        Response resp = OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_XML, headers);

        try {
            int statusCode = resp.getStatus();
            if (Response.Status.OK.getStatusCode() != statusCode) {
                throw new IllegalStateException("Response code: "
                        + statusCode
                        + " for "
                        + base
                        + " ("
                        + resp.getStatusInfo().getReasonPhrase()
                        + ")");
            }

            String respBody = resp.readEntity(String.class);
            Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(respBody);

            // ArrayList to contain the urls from all SPCs
            Collection<Object[]> data = new ArrayList<Object[]>();
            Node rootElement = (Node) OSLCUtils.getXPath().evaluate("/rdf:RDF/*", baseDoc, XPathConstants.NODE);
            if (rootElement.getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                    && rootElement.getLocalName().equals("ServiceProviderCatalog")) {
                data.add(new Object[] {base});
            }

            // Get all ServiceProviderCatalog urls from the base document in order
            // to test them as well,
            // recursively checking them for other ServiceProviderCatalogs further
            // down.
            NodeList spcs = (NodeList) OSLCUtils.getXPath()
                    .evaluate(
                            "//oslc_v2:serviceProviderCatalog/oslc_v2:ServiceProviderCatalog/@rdf:about",
                            baseDoc,
                            XPathConstants.NODESET);
            for (int i = 0; i < spcs.getLength(); i++) {
                if (!spcs.item(i).getNodeValue().equals(base)) {
                    Collection<Object[]> subCollection =
                            getReferencedCatalogUrlsUsingXML(spcs.item(i).getNodeValue());
                    Iterator<Object[]> iter = subCollection.iterator();
                    while (iter.hasNext()) {
                        data.add(iter.next());
                    }
                }
            }
            return data;
        } finally {
            resp.close();
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void baseUrlIsValid(String url) throws IOException {
        initServiceProviderCatalogXmlTests(url);
        // Get the status, make sure 200 OK
        assertTrue(
                response.getStatus() == Response.Status.OK.getStatusCode(), response.getStatusInfo().getReasonPhrase());

        // Verify we got a response
        assertNotNull(responseBody);
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void catalogRootIsRdfNamespaceDeclaration(String url) throws XPathException {
        initServiceProviderCatalogXmlTests(url);
        // Make sure our root element is the RDF namespace declarations
        Node rootNode = (Node) OSLCUtils.getXPath().evaluate("/rdf:RDF", doc, XPathConstants.NODE);
        assertNotNull(rootNode);
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void catalogRootAboutElementPointsToSelf(String url) throws XPathException, IOException {
        initServiceProviderCatalogXmlTests(url);
        // Make sure that we our root element has an rdf:about that points the
        // same server provider catalog
        Node aboutRoot = (Node)
                OSLCUtils.getXPath().evaluate("/*/oslc_v2:ServiceProviderCatalog/@rdf:about", doc, XPathConstants.NODE);
        assertNotNull(aboutRoot);
        assertTrue(currentUrl.equals(aboutRoot.getNodeValue()));
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderCatalogsHaveAtMostOneTitle(String url) throws XPathException {
        initServiceProviderCatalogXmlTests(url);
        // Check root to make sure it has at most one title.
        NodeList rootChildren = (NodeList)
                OSLCUtils.getXPath().evaluate("/rdf:RDF/oslc_v2:ServiceProviderCatalog/*", doc, XPathConstants.NODESET);
        int numTitles = 0;
        for (int i = 0; i < rootChildren.getLength(); i++) {
            if (rootChildren.item(i).getNamespaceURI().equals(OSLCConstants.DC)
                    && rootChildren.item(i).getLocalName().equals("title")) {
                numTitles++;
            }
        }
        assert (numTitles <= 1);

        // Get all service provider catalogs listed
        NodeList nestedSPCs = (NodeList)
                OSLCUtils.getXPath().evaluate("/*/*//oslc_v2:serviceProviderCatalog", doc, XPathConstants.NODESET);

        for (int i = 0; i < nestedSPCs.getLength(); i++) {
            NodeList spcChildren = (NodeList) OSLCUtils.getXPath()
                    .evaluate("/*/*//oslc_v2:serviceProviderCatalog[" + i + "]/*/*", doc, XPathConstants.NODESET);
            int titleCount = 0;
            // Go through the service provider catalog's children, make sure it
            // contains at most one title.
            for (int j = 0; j < spcChildren.getLength(); j++) {
                if (spcChildren.item(j).getNamespaceURI().equals(OSLCConstants.DC)
                        && spcChildren.item(j).getLocalName().equals("title")) {
                    titleCount++;
                }
            }
            assert (titleCount <= 1);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProvidersHaveAtMostOneTitle(String url) throws XPathException {
        initServiceProviderCatalogXmlTests(url);
        // Get all service providers listed
        NodeList nestedSPCs =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:serviceProvider", doc, XPathConstants.NODESET);

        for (int i = 0; i < nestedSPCs.getLength(); i++) {
            NodeList spcChildren =
                    (NodeList) OSLCUtils.getXPath().evaluate("./*/*", nestedSPCs.item(i), XPathConstants.NODESET);
            int titleCount = 0;

            // Go through the service provider's children, make sure it contains
            // at most one title.
            for (int j = 0; j < spcChildren.getLength(); j++) {
                if (spcChildren.item(j).getNamespaceURI().equals(OSLCConstants.DC)
                        && spcChildren.item(j).getLocalName().equals("title")) {
                    titleCount++;
                }
            }
            assert (titleCount <= 1);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderCatalogsHaveAtMostOnePublisher(String url) throws XPathExpressionException {
        initServiceProviderCatalogXmlTests(url);
        // Check root for Publisher, make sure it only has at most one
        NodeList rootChildren = (NodeList)
                OSLCUtils.getXPath().evaluate("/rdf:RDF/oslc_v2:ServiceProviderCatalog/*", doc, XPathConstants.NODESET);
        int numPublishers = 0;
        for (int i = 0; i < rootChildren.getLength(); i++) {
            if (rootChildren.item(i).getNamespaceURI().equals(OSLCConstants.DC)
                    && rootChildren.item(i).getLocalName().equals("publisher")) {
                numPublishers++;
            }
        }
        assert (numPublishers <= 1);

        // Get list of other ServiceProviderCatalog elements
        NodeList nestedSPCs = (NodeList)
                OSLCUtils.getXPath().evaluate("/*/*//oslc_v2:serviceProviderCatalog", doc, XPathConstants.NODESET);
        // Go through the children of each catalog
        for (int i = 0; i < nestedSPCs.getLength(); i++) {
            NodeList spcChildren = (NodeList) OSLCUtils.getXPath()
                    .evaluate("/*/*//oslc_v2:serviceProviderCatalog[" + i + "]/*/*", doc, XPathConstants.NODESET);
            int publisherCount = 0;
            // Make sure there's at most one Publisher blocks
            for (int j = 0; j < spcChildren.getLength(); j++) {
                if (spcChildren.item(j).getNamespaceURI().equals(OSLCConstants.DC)
                        && spcChildren.item(j).getLocalName().equals("publisher")) {
                    publisherCount++;
                }
            }
            assert (publisherCount <= 1);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProvidersHaveAtMostOnePublisher(String url) throws XPathExpressionException {
        initServiceProviderCatalogXmlTests(url);
        // Get the listed ServiceProvider elements
        NodeList nestedSPCs =
                (NodeList) OSLCUtils.getXPath().evaluate("/*/*//oslc_v2:serviceProvider", doc, XPathConstants.NODESET);

        // Make sure that for each one it only has at most one Publisher block
        for (int i = 0; i < nestedSPCs.getLength(); i++) {
            NodeList spcChildren = (NodeList) OSLCUtils.getXPath()
                    .evaluate("/*/*//oslc_v2:serviceProvider[" + i + "]/*/*", doc, XPathConstants.NODESET);
            int publisherCount = 0;
            for (int j = 0; j < spcChildren.getLength(); j++) {
                if (spcChildren.item(j).getNamespaceURI().equals(OSLCConstants.DC)
                        && spcChildren.item(j).getLocalName().equals("publisher")) {
                    publisherCount++;
                }
            }
            assert (publisherCount <= 1);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void publisherElementsAreValid(String url) throws XPathExpressionException {
        initServiceProviderCatalogXmlTests(url);
        // Get all Publisher xml blocks
        NodeList publishers = (NodeList) OSLCUtils.getXPath().evaluate("//dc:publisher/*", doc, XPathConstants.NODESET);

        // Verify that each block contains a title and identifier, and at most
        // one icon and label
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

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderCatalogsHaveAtMostOneOAuthElement(String url) throws XPathExpressionException {
        initServiceProviderCatalogXmlTests(url);
        // Check root for OAuth block, make sure it only has at most one
        NodeList rootChildren = (NodeList)
                OSLCUtils.getXPath().evaluate("/rdf:RDF/oslc_v2:ServiceProviderCatalog/*", doc, XPathConstants.NODESET);
        int numOAuthElements = 0;
        for (int i = 0; i < rootChildren.getLength(); i++) {
            if (rootChildren.item(i).getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                    && rootChildren.item(i).getLocalName().equals("oauthConfiguration")) {
                numOAuthElements++;
            }
        }
        assert (numOAuthElements <= 1);

        // Get list of other ServiceProviderCatalog elements
        NodeList nestedSPCs = (NodeList)
                OSLCUtils.getXPath().evaluate("/*/*//oslc_v2:serviceProviderCatalog", doc, XPathConstants.NODESET);
        // Go through the children of each catalog
        for (int i = 0; i < nestedSPCs.getLength(); i++) {
            NodeList spcChildren = (NodeList) OSLCUtils.getXPath()
                    .evaluate("/*/*//oslc_v2:serviceProviderCatalog[" + i + "]/*/*", doc, XPathConstants.NODESET);
            int oAuthCount = 0;
            // Make sure there's at most one OAuth blocks
            for (int j = 0; j < spcChildren.getLength(); j++) {
                if (spcChildren.item(j).getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                        && spcChildren.item(j).getLocalName().equals("oauthConfiguration")) {
                    oAuthCount++;
                }
            }
            assert (oAuthCount <= 1);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProvidersHaveAtMostOneOAuthElement(String url) throws XPathExpressionException {
        initServiceProviderCatalogXmlTests(url);
        // Get list of other service provider elements
        NodeList nestedSPCs =
                (NodeList) OSLCUtils.getXPath().evaluate("/*/*//oslc_v2:serviceProvider", doc, XPathConstants.NODESET);
        // Go through the children of each provider
        for (int i = 0; i < nestedSPCs.getLength(); i++) {
            NodeList spcChildren = (NodeList) OSLCUtils.getXPath()
                    .evaluate("/*/*//oslc_v2:serviceProvider[" + i + "]/*/*", doc, XPathConstants.NODESET);
            int oAuthCount = 0;
            // Make sure there's at most one OAuth blocks
            for (int j = 0; j < spcChildren.getLength(); j++) {
                if (spcChildren.item(j).getNamespaceURI().equals(OSLCConstants.OSLC_V2)
                        && spcChildren.item(j).getLocalName().equals("oauthConfiguration")) {
                    oAuthCount++;
                }
            }
            assert (oAuthCount <= 1);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void oAuthElementsAreValid(String url) throws XPathExpressionException {
        initServiceProviderCatalogXmlTests(url);
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

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderCatalogsHaveValidResourceUrl(String url) throws XPathException, IOException {
        initServiceProviderCatalogXmlTests(url);
        // Get all ServiceProviderCatalog elements and their rdf:about
        // attributes, making sure we have a URL
        // to the resource for each catalog
        NodeList catalogAbouts = (NodeList) OSLCUtils.getXPath()
                .evaluate("//oslc_v2:ServiceProviderCatalog/@rdf:about", doc, XPathConstants.NODESET);
        NodeList catalogs = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProviderCatalog", doc, XPathConstants.NODESET);
        assertTrue(catalogAbouts.getLength() == catalogs.getLength());

        // Verify the urls are valid
        for (int i = 0; i < catalogAbouts.getLength(); i++) {
            String url = catalogAbouts.item(i).getNodeValue();
            assertFalse(url.isEmpty());
            Response response = OSLCUtils.getResponseFromUrl(setupBaseUrl, url, creds, "*/*");
            response.close();
            assertFalse(response.getStatus() == 404);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void servicesProvidersHaveValidResourceUrl(String url) throws XPathException, IOException {
        initServiceProviderCatalogXmlTests(url);
        // Get all ServiceProvider elements
        NodeList services =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider", doc, XPathConstants.NODESET);
        // Get all resource attributes from the ServiceProviders
        NodeList resources = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_v2:ServiceProvider/@rdf:about", doc, XPathConstants.NODESET);
        // Make sure each ServiceProvider element has an attribute to reference
        // it
        assertTrue(services.getLength() == resources.getLength());
        // Verify that the resource urls are valid
        for (int i = 0; i < resources.getLength(); i++) {
            String url = resources.item(i).getNodeValue();
            assertNotNull(url);
            Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, url, creds, "*/*");
            resp.close();
            assertFalse(resp.getStatus() == 404);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void detailsElementsHaveValidResourceAttribute(String url) throws IOException, XPathException {
        initServiceProviderCatalogXmlTests(url);
        // Get all details elements
        NodeList detailsElements =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_v2:details", doc, XPathConstants.NODESET);
        // Get all resource attributes of the details elements
        NodeList resources = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_v2:details/@rdf:resource", doc, XPathConstants.NODESET);
        // Make sure they match up 1-to-1
        assertTrue(detailsElements.getLength() == resources.getLength());
        // Verify that the resource has a url
        for (int i = 0; i < resources.getLength(); i++) {
            String url = resources.item(i).getNodeValue();
            assertNotNull(url);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void misplacedParametersDoNotEffectResponse(String url) throws IOException {
        initServiceProviderCatalogXmlTests(url);
        log.warn("misplacedParametersDoNotEffectResponse");
        log.warn("setupBaseUrl: {}", setupBaseUrl);
        log.warn("currentUrl: {}", currentUrl);
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
}
