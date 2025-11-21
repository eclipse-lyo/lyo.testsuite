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

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * This class provides JUnit tests for the validation of OSLC Service Provider Catalogs, for the 2.0 version of the OSLC
 * standard, as defined by the OSLC Core Spec.
 */
@RunWith(Parameterized.class)
public abstract class ServiceProviderCatalogBaseTests extends TestsBase {

    // Base URL of the OSLC Service Provider Catalog to be tested
    // protected Response response = null;
    protected static String fContentType = null;

    public ServiceProviderCatalogBaseTests(String thisUrl) {
        super(thisUrl);
        currentUrl = thisUrl;
    }

    @Test
    @Disabled("Neither HTTP/1.1 nor OSLC Core 2.0 REQUIRE a 406 Not Acceptable response. "
            + "It doesn't appear to be mentioned in the OSLC 2.0 Core specification. "
            + "This is a SHOULD per HTTP/1.1, but not a MUST. See "
            + "http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1")
    public void invalidContentTypeGivesNotSupportedOPTIONAL() throws IOException {
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, "invalid/content-type", headers);
        if (resp.hasEntity()) {
            String respType = "";
            if (resp.getHeaderString("Content-Type") != null) {
                respType = resp.getHeaderString("Content-Type");
            }
            resp.close();
            assertTrue(
                    resp.getStatus() == 406 || respType.contains("invalid/content-type"),
                    "Expected 406 but received "
                            + resp.getStatus()
                            + " or Content-type='invalid/content-type' but received "
                            + respType);
        }
    }

    /** Not required directly from the spec, just mentions that it should be application/rdf+xml */
    @Test
    public void contentTypeIsSuggestedType() throws IOException {
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);
        resp.close();
        // Make sure the response to this URL was of valid type
        String ct = resp.getHeaderString("Content-Type");
        assertTrue(ct.contains(fContentType), "Expected content-type \"" + fContentType + "\" received : " + ct);
    }
}
