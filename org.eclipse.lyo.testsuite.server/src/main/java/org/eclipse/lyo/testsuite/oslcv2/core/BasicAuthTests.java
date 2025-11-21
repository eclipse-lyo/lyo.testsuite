/*
 * Copyright (c) 2025, 2025 IBM Corporation and others
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Tests the process by which consumers are able to access resources of an OSLC Provider using BASIC authentication to
 * access the provider's resources.
 */
public class BasicAuthTests extends TestsBase {

    private static UserCredentials basicCreds;
    private String currentUrl;

    public void initBasicAuthTests(String url) {
        super(url);
        this.currentUrl = url;
    }

    @BeforeAll
    public static void setup() throws IOException, ParserConfigurationException, SAXException, XPathException {
        staticSetup();
        // Ensure we're using BASIC auth
        assertEquals("BASIC", setupProps.getProperty("authMethod"));
        String userId = setupProps.getProperty("userId");
        String pw = setupProps.getProperty("pw");
        assertNotNull(userId, "userId must be specified in setup.properties");
        assertNotNull(pw, "pw must be specified in setup.properties");
        basicCreds = new UserPassword(userId, pw);
    }

    public static Collection<Object[]> getAllServiceProviderUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Get all ServiceProvider URLs to test BASIC auth against
        ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
        Collection<Object[]> data = new ArrayList<>();
        for (String url : serviceUrls) {
            data.add(new Object[] {url});
        }
        return data;
    }

    @MethodSource("getAllServiceProviderUrls")
    @ParameterizedTest
    public void testBasicAuthAccess(String url) throws IOException {
        initBasicAuthTests(url);
        // Test that we can access a resource using BASIC auth
        Response response =
                OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, OSLCConstants.CT_XML, headers);
        assertEquals(200, response.getStatus(), "BASIC auth should allow access to the resource");
        response.close();
    }

    @MethodSource("getAllServiceProviderUrls")
    @ParameterizedTest
    public void testBasicAuthWithWrongCredentials(String url) throws IOException {
        initBasicAuthTests(url);
        // Test that BASIC auth fails with wrong credentials
        UserCredentials wrongCreds = new UserPassword("wrongUser", "wrongPassword");
        Response response =
                OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, wrongCreds, OSLCConstants.CT_XML, headers);
        // Should get 401 Unauthorized
        assertEquals(401, response.getStatus(), "BASIC auth should fail with wrong credentials");
        response.close();
    }

    @MethodSource("getAllServiceProviderUrls")
    @ParameterizedTest
    public void testBasicAuthWithServiceProviderCatalog(String url)
            throws IOException, XPathException, ParserConfigurationException, SAXException {
        initBasicAuthTests(url);
        // Test that we can access the ServiceProviderCatalog using BASIC auth
        Response response = OSLCUtils.getResponseFromUrl(
                setupBaseUrl, setupBaseUrl, basicCreds, OSLCConstants.CT_DISC_CAT_XML, headers);
        assertEquals(200, response.getStatus(), "BASIC auth should allow access to the ServiceProviderCatalog");

        // Verify the response is a valid ServiceProviderCatalog
        String responseBody = response.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
        Node rootNode = (Node) OSLCUtils.getXPath().evaluate("/oslc:ServiceProviderCatalog", doc, XPathConstants.NODE);
        assertNotNull(rootNode, "Response should be a ServiceProviderCatalog");
    }
}
