/*
 * Copyright (c) 2025 IBM Corporation.
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
 *    Cline - initial API and implementation
 */
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Tests the process by which consumers are able to access resources of an OSLC Provider using BASIC authentication to
 * access the provider's resources.
 */
@RunWith(Parameterized.class)
public class BasicAuthTests extends TestsBase {

    private static UserCredentials basicCreds;
    private String currentUrl;

    public BasicAuthTests(String url) {
        super(url);
        this.currentUrl = url;
    }

    @BeforeClass
    public static void setup() throws IOException, ParserConfigurationException, SAXException, XPathException {
        staticSetup();
        // Ensure we're using BASIC auth
        assertEquals("BASIC", setupProps.getProperty("authMethod"));
        String userId = setupProps.getProperty("userId");
        String pw = setupProps.getProperty("pw");
        assertNotNull("userId must be specified in setup.properties", userId);
        assertNotNull("pw must be specified in setup.properties", pw);
        basicCreds = new UserPassword(userId, pw);
    }

    @Parameters
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

    @Test
    public void testBasicAuthAccess() throws IOException {
        // Test that we can access a resource using BASIC auth
        Response response =
                OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, OSLCConstants.CT_XML, headers);
        assertEquals("BASIC auth should allow access to the resource", 200, response.getStatus());
        response.close();
    }

    @Test
    public void testBasicAuthWithWrongCredentials() throws IOException {
        // Test that BASIC auth fails with wrong credentials
        UserCredentials wrongCreds = new UserPassword("wrongUser", "wrongPassword");
        Response response =
                OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, wrongCreds, OSLCConstants.CT_XML, headers);
        // Should get 401 Unauthorized
        assertEquals("BASIC auth should fail with wrong credentials", 401, response.getStatus());
        response.close();
    }

    @Test
    public void testBasicAuthWithServiceProviderCatalog()
            throws IOException, XPathException, ParserConfigurationException, SAXException {
        // Test that we can access the ServiceProviderCatalog using BASIC auth
        Response response = OSLCUtils.getResponseFromUrl(
                setupBaseUrl, setupBaseUrl, basicCreds, OSLCConstants.CT_DISC_CAT_XML, headers);
        assertEquals("BASIC auth should allow access to the ServiceProviderCatalog", 200, response.getStatus());

        // Verify the response is a valid ServiceProviderCatalog
        String responseBody = response.readEntity(String.class);
        Document doc = OSLCUtils.createXMLDocFromResponseBody(responseBody);
        Node rootNode = (Node) OSLCUtils.getXPath().evaluate("/oslc:ServiceProviderCatalog", doc, XPathConstants.NODE);
        assertNotNull("Response should be a ServiceProviderCatalog", rootNode);
    }
}
