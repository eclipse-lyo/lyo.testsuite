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

import static org.junit.Assert.assertTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;

public class GetAndUpdateBase extends AssetTestBase {

    public GetAndUpdateBase(String url, String acceptType, String contentType) {
        super(url, acceptType, contentType);
        //        HttpConnectionParams.setConnectionTimeout(OSLCUtils.httpClient.getParams(), 30000);
    }

    @Test
    public void getAnAsset() throws IOException {
        getAssetAsString();
    }

    protected Map<String, String> addHeader(Map<String, String> headers, Map.Entry<String, String> header) {
        // handle immutable and mutable maps
        if (headers == null) {
            var map = new HashMap<>();
            map.put(header.getKey(), header.getValue());
            return headers;
        } else {
            headers.put(header.getKey(), header.getValue());
            return headers;
        }
    }

    /**
     * Uploads the artifact set in the property file
     *
     * @param artifactFactory
     * @return the url location of the artifact
     */
    protected String uploadArtifact(String artifactFactory) throws IOException {
        File file = new File(setupProps.getProperty("artifactContentType"));
        var h = Map.entry("oslc_asset.name", file.getName());

        Response resp = OSLCUtils.postDataToUrl(
                artifactFactory,
                creds,
                acceptType,
                setupProps.getProperty("artifactContentType"),
                readFileFromProperty("artifactFile"),
                addHeader(null, h));
        resp.close();
        assertTrue(
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus(),
                resp.getStatus() == Status.CREATED.getStatusCode());

        assertTrue("No Location header", resp.getHeaderString("Location") != null);
        assertTrue("No content length header", resp.getHeaderString("Content-Length") != null);
        return resp.getHeaderString("Location");
    }

    protected void downloadArtifact(String artifactUrl) throws ClientProtocolException, IOException {
        Response resp = OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
        resp.close();
        assertTrue(
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
    }
}
