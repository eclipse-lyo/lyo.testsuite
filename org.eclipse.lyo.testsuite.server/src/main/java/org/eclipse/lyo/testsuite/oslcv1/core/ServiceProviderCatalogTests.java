/*
 * Copyright (c) 2011, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite.oslcv1.core;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.SetupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of OSLC Service Provider Catalogs, as outlined by the OSLC Core
 * Spec.
 */
public class ServiceProviderCatalogTests {

    // Base URL of the OSLC Service Provider Catalog to be tested
    private static String baseUrl;
    private static TestsBase.UserCredentials basicCreds;

    private String currentUrl;
    private Response response;
    private String responseBody;
    private Document doc;

    public void initServiceProviderCatalogTests(String url) {
        this.currentUrl = url;
    }

    @BeforeEach
    public void setupTest() throws IOException, ParserConfigurationException, SAXException {
        Properties setupProps = SetupProperties.setup(null);
        if (setupProps.getProperty("testBackwardsCompatability") != null
                && Boolean.parseBoolean(setupProps.getProperty("testBackwardsCompatability"))) {
            setupProps = SetupProperties.setup(setupProps.getProperty("version1Properties"));
        }
        baseUrl = setupProps.getProperty("baseUri");
        String userId = setupProps.getProperty("userId");
        String pw = setupProps.getProperty("pw");
        basicCreds = new TestsBase.UserPassword(userId, pw);
        response = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds, OSLCConstants.CT_DISC_CAT_XML, null);
        responseBody = response.readEntity(String.class);
        // Get XML Doc from response
        doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
    }

    public static Collection<Object[]> getAllServiceProviderCatalogUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProviders contained within it, recursively.
        Properties setupProps = SetupProperties.setup(null);
        Collection<Object[]> coll = getReferencedCatalogUrls(setupProps.getProperty("baseUri"));
        return coll;
    }

    public static Collection<Object[]> getReferencedCatalogUrls(String base)
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        Properties setupProps = SetupProperties.setup(null);
        String userId = setupProps.getProperty("userId");
        String pw = setupProps.getProperty("pw");

        Response resp = OSLCUtils.getResponseFromUrl(
                base, null, new TestsBase.UserPassword(userId, pw), OSLCConstants.CT_DISC_CAT_XML);
        // If we're not looking at a catalog, return empty list.
        if (!resp.getHeaderString("Content-Type").contains(OSLCConstants.CT_DISC_CAT_XML)) {
            System.out.println("The url: " + base + " does not refer to a ServiceProviderCatalog.");
            System.out.println(
                    "The content-type of a ServiceProviderCatalog should be " + OSLCConstants.CT_DISC_CAT_XML);
            System.out.println("The content-type returned was " + resp.getHeaderString("Content-Type"));
            resp.close();
            return new ArrayList<Object[]>();
        }
        Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(resp.readEntity(String.class));

        // ArrayList to contain the urls from all SPCs
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] {base});

        // Get all ServiceProviderCatalog urls from the base document in order to test them as well,
        // recursively checking them for other ServiceProviderCatalogs further down.
        NodeList spcs = (NodeList) OSLCUtils.getXPath()
                .evaluate(
                        "//oslc_disc:entry/oslc_disc:ServiceProviderCatalog/@rdf:about",
                        baseDoc,
                        XPathConstants.NODESET);
        for (int i = 0; i < spcs.getLength(); i++) {
            String uri = spcs.item(i).getNodeValue();
            uri = OSLCUtils.absoluteUrlFromRelative(base, uri);
            if (!uri.equals(base)) {
                Collection<Object[]> subCollection = getReferencedCatalogUrls(uri);
                Iterator<Object[]> iter = subCollection.iterator();
                while (iter.hasNext()) {
                    data.add(iter.next());
                }
            }
        }
        return data;
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void baseUrlIsValid(String url) throws IOException {
        initServiceProviderCatalogTests(url);
        // Get the status, make sure 200 OK
        assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());

        // Verify we got a response
        assertNotNull(responseBody);
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void invalidContentTypeGivesNotSupported(String url) throws IOException {
        initServiceProviderCatalogTests(url);
        Response resp = OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds, "application/svg+xml", null);
        String respType = resp.getHeaderString("Content-Type");
        resp.close();
        assertTrue(resp.getStatus() == 406 || respType.contains("application/svg+xml"));
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void contentTypeIsServiceProviderCatalog(String url) throws IOException {
        initServiceProviderCatalogTests(url);
        Response resp =
                OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds, OSLCConstants.CT_DISC_CAT_XML, null);
        resp.close();
        // Make sure the response to this URL was of valid type
        assertTrue(resp.getHeaderString("Content-Type").contains(OSLCConstants.CT_DISC_CAT_XML));
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void misplacedParametersDoNotEffectResponse(String url) throws IOException {
        initServiceProviderCatalogTests(url);
        var baseResp =
                OSLCUtils.getResponseFromUrl(baseUrl, currentUrl, basicCreds, OSLCConstants.CT_DISC_CAT_XML, null);
        String baseRespValue = baseResp.readEntity(String.class);

        var parameterResp = OSLCUtils.getResponseFromUrl(
                baseUrl, currentUrl + "?oslc_cm:query", basicCreds, OSLCConstants.CT_DISC_CAT_XML, null);
        String parameterRespValue = parameterResp.readEntity(String.class);

        assertTrue(baseRespValue.equals(parameterRespValue));
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void catalogRootIsServiceProviderCatalog(String url) throws XPathException {
        initServiceProviderCatalogTests(url);
        // Make sure our root element is a ServiceProviderCatalog
        Node rootNode =
                (Node) OSLCUtils.getXPath().evaluate("/oslc_disc:ServiceProviderCatalog", doc, XPathConstants.NODE);
        assertNotNull(rootNode);
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void catalogRootAboutElementPointsToSelf(String url) throws XPathException, IOException {
        initServiceProviderCatalogTests(url);
        // Make sure that we our root element has an rdf:about that points the same server provider
        // catalog
        Node aboutRoot = (Node)
                OSLCUtils.getXPath().evaluate("/oslc_disc:ServiceProviderCatalog/@rdf:about", doc, XPathConstants.NODE);
        assertNotNull(aboutRoot);
        Response resp = OSLCUtils.getResponseFromUrl(baseUrl, aboutRoot.getNodeValue(), basicCreds, "*/*", null);
        // Verify the catalogs we get are identical (ie: the same resource)
        assertTrue(responseBody.equals(resp.readEntity(String.class)));
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderCatalogsHaveValidTitles(String url) throws XPathException {
        initServiceProviderCatalogTests(url);
        // Check root
        Node rootCatalogTitle = (Node)
                OSLCUtils.getXPath().evaluate("/oslc_disc:ServiceProviderCatalog/dc:title", doc, XPathConstants.NODE);
        assertNotNull(rootCatalogTitle);
        assertFalse(rootCatalogTitle.getTextContent().isEmpty());
        NodeList titleSub = (NodeList) OSLCUtils.getXPath()
                .evaluate("/oslc_disc:ServiceProviderCatalog/dc:title/*", doc, XPathConstants.NODESET);
        assertTrue(titleSub.getLength() == 0);

        // Get all entries, parse out which have embedded catalogs and check the titles
        NodeList catalogs =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:entry/*", doc, XPathConstants.NODESET);
        for (int i = 0; i < catalogs.getLength(); i++) {
            Node catalog = (Node) OSLCUtils.getXPath()
                    .evaluate(
                            "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProviderCatalog",
                            doc,
                            XPathConstants.NODE);
            // This entry has a catalog, check that it has a title
            if (catalog != null) {
                Node cTitle = (Node) OSLCUtils.getXPath()
                        .evaluate(
                                "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProviderCatalog/dc:title",
                                doc,
                                XPathConstants.NODE);
                assertNotNull(cTitle);
                // Make sure the child isn't empty
                assertFalse(cTitle.getTextContent().isEmpty());
                Node child = (Node) OSLCUtils.getXPath()
                        .evaluate(
                                "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProviderCatalog/dc:title/*",
                                doc,
                                XPathConstants.NODE);
                // Make sure the title has no child elements
                assertTrue(child == null);
            }
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProvidersHaveValidTitles(String url) throws XPathException {
        initServiceProviderCatalogTests(url);
        // Get all entries, parse out which have embedded ServiceProviders and check the titles
        NodeList entries = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:entry/*", doc, XPathConstants.NODESET);
        for (int i = 0; i < entries.getLength(); i++) {
            Node provider = (Node) OSLCUtils.getXPath()
                    .evaluate("//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProvider", doc, XPathConstants.NODE);
            // This entry has a catalog, check that it has a title
            if (provider != null) {
                Node pTitle = (Node) OSLCUtils.getXPath()
                        .evaluate(
                                "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProvider/dc:title",
                                doc,
                                XPathConstants.NODE);
                assertNotNull(pTitle);
                // Make sure the title isn't empty
                assertFalse(pTitle.getTextContent().isEmpty());
                Node child = (Node) OSLCUtils.getXPath()
                        .evaluate(
                                "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProvider/dc:title/*",
                                doc,
                                XPathConstants.NODE);
                // Make sure the title has no child elements
                assertTrue(child == null);
            }
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderCatalogsHaveValidAboutAttribute(String url) throws XPathException, IOException {
        initServiceProviderCatalogTests(url);
        // Get all ServiceProviderCatalog elements and their rdf:about attributes, make sure that
        // each catalog has
        // an rdf:about attribute
        NodeList catalogAbouts = (NodeList) OSLCUtils.getXPath()
                .evaluate("//oslc_disc:ServiceProviderCatalog/@rdf:about", doc, XPathConstants.NODESET);
        NodeList catalogs = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_disc:ServiceProviderCatalog", doc, XPathConstants.NODESET);
        assertTrue(catalogAbouts.getLength() == catalogs.getLength());

        // Verify the rdf:about attribute links
        for (int i = 0; i < catalogAbouts.getLength(); i++) {
            String url = catalogAbouts.item(i).getNodeValue();
            assertFalse(catalogUrl.isEmpty());
            Response response = OSLCUtils.getResponseFromUrl(baseUrl, url, basicCreds, "*/*", null);
            assertFalse(response.getStatus() == 404);
            response.close();
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void entryElementsHaveSingleServiceProviderOrCatalog(String url) throws XPathException {
        initServiceProviderCatalogTests(url);
        // Get all entry elements
        NodeList entries = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:entry", doc, XPathConstants.NODESET);
        // Check for either 1 ServiceProviderCatalog or 1 ServiceProvider, but not both.
        for (int i = 0; i < entries.getLength(); i++) {
            boolean hasServiceOrCatalog = false;
            NodeList spc = (NodeList) OSLCUtils.getXPath()
                    .evaluate(
                            "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProviderCatalog",
                            doc,
                            XPathConstants.NODESET);
            if (spc.getLength() == 1) {
                hasServiceOrCatalog = true;
            }
            NodeList sp = (NodeList) OSLCUtils.getXPath()
                    .evaluate(
                            "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProvider",
                            doc,
                            XPathConstants.NODESET);
            if (sp.getLength() == 1) {
                hasServiceOrCatalog = !hasServiceOrCatalog;
            }
            assertTrue(hasServiceOrCatalog);
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void noInternalServiceProviderCatalogsHaveEntryElements(String url) throws XPathException {
        initServiceProviderCatalogTests(url);
        // Gets list of possible internal entry elements and ensures that there are none
        NodeList internEntry = (NodeList) OSLCUtils.getXPath()
                .evaluate(
                        "//oslc_disc:entry/oslc_disc:ServiceProviderCatalog" + "/oslc_disc:entry",
                        doc,
                        XPathConstants.NODESET);
        assertTrue(internEntry.getLength() == 0);
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void serviceProviderElementsHaveServicesChildElement(String url) throws XPathException {
        initServiceProviderCatalogTests(url);
        // Get all entry elements
        NodeList entries = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:entry", doc, XPathConstants.NODESET);
        for (int i = 0; i < entries.getLength(); i++) {
            Node sp = (Node) OSLCUtils.getXPath()
                    .evaluate("//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProvider", doc, XPathConstants.NODE);
            // This entry has a ServiceProvider and not a catalog
            if (sp != null) {
                // Verify the ServiceProvider has a child element services
                Node services = (Node) OSLCUtils.getXPath()
                        .evaluate(
                                "//oslc_disc:entry[" + (i + 1) + "]/oslc_disc:ServiceProvider/oslc_disc:services",
                                doc,
                                XPathConstants.NODE);
                assertNotNull(services);
            }
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void servicesChildElementHasValidResourceAttribute(String url) throws XPathException, IOException {
        initServiceProviderCatalogTests(url);
        // Get all services elements
        NodeList services =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:services", doc, XPathConstants.NODESET);
        // Get all resource attributes from services
        NodeList resources = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_disc:services/@rdf:resource", doc, XPathConstants.NODESET);
        // Make sure each services element has a resource attribute
        assertTrue(services.getLength() == resources.getLength());
        // Verify that the resource urls are valid
        for (int i = 0; i < resources.getLength(); i++) {
            String resourceUrl = resources.item(i).getNodeValue();
            assertNotNull(resourceUrl);
            Response resp = OSLCUtils.getResponseFromUrl(baseUrl, resourceUrl, basicCreds, "*/*", null);
            assertFalse(resp.getStatus() == 404);
            resp.close();
        }
    }

    @MethodSource("getAllServiceProviderCatalogUrls")
    @ParameterizedTest
    public void detailsElementsHaveValidResourceAttribute(String url) throws IOException, XPathException {
        initServiceProviderCatalogTests(url);
        // Get all details elements
        NodeList detailsElements =
                (NodeList) OSLCUtils.getXPath().evaluate("//oslc_disc:details", doc, XPathConstants.NODESET);
        // Get all resource attributes of the details elements
        NodeList resources = (NodeList)
                OSLCUtils.getXPath().evaluate("//oslc_disc:details/@rdf:resource", doc, XPathConstants.NODESET);
        // Make sure they match up 1-to-1
        assertTrue(detailsElements.getLength() == resources.getLength());
        // Verify that the resource has a url
        for (int i = 0; i < resources.getLength(); i++) {
            String detailUrl = resources.item(i).getNodeValue();
            assertNotNull(detailUrl);
        }
    }
}
