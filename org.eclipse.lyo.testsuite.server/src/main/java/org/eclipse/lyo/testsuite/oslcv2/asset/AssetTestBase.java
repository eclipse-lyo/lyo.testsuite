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
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.runners.Parameterized.Parameters;

public class AssetTestBase extends TestsBase {
    protected static String baseUrl;
    protected String assetUrl;
    protected static String acceptType;
    protected static String contentType;

    @SuppressWarnings("static-access")
    public AssetTestBase(String url, String acceptType, String contentType) {
        super(url);
        this.acceptType = acceptType;
        this.contentType = contentType;
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        baseUrl = setupProps.getProperty("baseUri");

        ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(baseUrl, onlyOnce);
        String[] types = null;
        ArrayList<String> capabilityURLsUsingRdfXml = TestsBase.getCapabilityURLsUsingRdfXml(
                OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, types);
        return toCollection(capabilityURLsUsingRdfXml);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (assetUrl == null) return;
        Response resp = OSLCUtils.deleteFromUrl(assetUrl, creds, acceptType);
        resp.close();
    }

    /**
     * Gets an asset and the returns the content of the response in as a string Guid and version must be set
     *
     * @throws IOException
     */
    protected String getAssetAsString() throws IOException {
        Response resp = OSLCUtils.getDataFromUrl(assetUrl, creds, acceptType, contentType, headers);
        String content = resp.readEntity(String.class);
        resp.close();
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
        return content;
    }

    /**
     * Get's an asset and then returns the response
     *
     * @throws IOException
     */
    protected Response getAssetResponse() throws IOException {
        Response resp = OSLCUtils.getDataFromUrl(assetUrl, creds, acceptType, contentType, headers);
        try {
            assertTrue(
                    resp.getStatus() == Response.Status.OK.getStatusCode(),
                    "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
        } catch (AssertionError e) {
            resp.close();
            throw e;
        }
        return resp;
    }

    /**
     * Creates an asset and then asserts that it was created. If all goes well the url to the created asset is returned
     */
    protected String createAsset(String content) throws IOException {
        Response resp = OSLCUtils.postDataToUrl(currentUrl, creds, acceptType, contentType, content, headers);
        resp.close();
        assertTrue(
                Status.CREATED.getStatusCode() == resp.getStatus(),
                "Expected: " + Status.CREATED.getStatusCode() + ", received: " + resp.getStatus());

        var loc = resp.getHeaderString("Location");
        return loc;
    }

    /** Given a property from the property file, the contents of the file are returned */
    protected String readFileFromProperty(String property) {
        String fileName = setupProps.getProperty(property);
        if (fileName == null) return fileName;
        return OSLCUtils.readFileByNameAsString(fileName);
    }

    protected void putAsset(String content) throws IOException {
        Response resp = OSLCUtils.putDataToUrl(assetUrl, creds, acceptType, contentType, content, headers);

        resp.close();
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
    }
}
