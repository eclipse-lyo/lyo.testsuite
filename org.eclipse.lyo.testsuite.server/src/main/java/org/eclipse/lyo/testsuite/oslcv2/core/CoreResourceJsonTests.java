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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

/** This class provides JUnit tests with JSON format for the validation of an OSLC core resource. */
@RunWith(Parameterized.class)
public abstract class CoreResourceJsonTests extends TestsBase {
    private Response response;
    private String responseBody;
    protected JSONObject doc;

    public CoreResourceJsonTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException, JSONException {
        super(thisUrl);

        // If currentUrl is null, it means that the query didn't match any
        // records. This isn't exactly a failure, but there's nothing more we
        // can test.
        assumeNotNull(currentUrl);
        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, OSLCConstants.CT_JSON, headers);
        responseBody = response.readEntity(String.class);
        int sc = response.getStatus();

        // Some records in the system might not be accessible to this user. This
        // isn't a failure, but there's nothing more we can test.
        assumeTrue(sc != Status.FORBIDDEN.getStatusCode() && sc != Status.UNAUTHORIZED.getStatusCode());

        // Make sure the request succeeded before continuing.
        assertEquals(Response.Status.OK.getStatusCode(), sc);

        // Get JSON doc from response
        JSONArtifact userData = JSON.parse(responseBody);

        if (userData instanceof JSONArtifact) {
            doc = (JSONObject) userData;
        }
    }

    @Test
    //
    // Verify that the OSLC Core Resource has one and only one dcterms:title
    //
    public void CoreResourceHasOneTitle() throws JSONException {
        assertTrue(doc.get(OSLCConstants.DCTERMS_TITLE) instanceof String);
    }

    @Test
    public void CoreResourceHasAtMostOneDescription() throws JSONException {
        if (doc.containsKey(OSLCConstants.DCTERMS_DESC)) {
            assertTrue(doc.get(OSLCConstants.DCTERMS_DESC) instanceof String);
        }
    }

    @Test
    public void CoreResourceHasAtMostOneIdentifier() throws JSONException {
        if (doc.containsKey(OSLCConstants.DCTERMS_ID)) {
            assertTrue((doc.get(OSLCConstants.DCTERMS_ID) instanceof String)
                    || (doc.get(OSLCConstants.DCTERMS_ID) instanceof Integer));
        }
    }

    @Test
    public void CoreResourceHasAtMostOneName() throws JSONException {
        if (doc.containsKey(OSLCConstants.DCTERMS_NAME)) {
            assertTrue(doc.get(OSLCConstants.DCTERMS_NAME) instanceof String);
        }
    }

    @Test
    public void CoreResourceHasAtMostOneCreatedDate() throws JSONException {
        if (doc.containsKey(OSLCConstants.DCTERMS_CREATED)) {
            assertTrue(doc.get(OSLCConstants.DCTERMS_CREATED) instanceof String);
        }
    }

    @Test
    public void CoreResourceHasAtMostOneModifiedDate() throws JSONException {
        if (doc.containsKey(OSLCConstants.DCTERMS_MODIFIED)) {
            assertTrue(doc.get(OSLCConstants.DCTERMS_MODIFIED) instanceof String);
        }
    }

    @Test
    public void CoreResourceHasAtMostOneDiscussion() throws JSONException {
        if (doc.containsKey("oslc:discussion")) {
            assertTrue(doc.get("oslc:discussion") instanceof JSONObject);
        }
    }

    @Test
    public void CoreResourceHasAtMostOneInstanceShape() throws JSONException {
        if (doc.containsKey("oslc:instanceShape")) {
            assertTrue(doc.get("oslc:instanceShape") instanceof JSONObject);
        }
    }
}
