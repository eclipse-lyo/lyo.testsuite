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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import org.apache.http.client.ClientProtocolException;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class UsageCaseJsonTests extends UsageCaseBase {
    private static JSONObject bestAsset = null;
    private String baseUrl;

    public UsageCaseJsonTests(String thisUrl) {
        super(thisUrl, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);
        baseUrl = setupProps.getProperty("baseUrl");
    }

    @Test
    public void queryUsageTest() throws IllegalStateException, IOException, JSONException {
        JSONObject query = runQuery();
        bestAsset = getBestAsset(query);
        assertTrue(bestAsset != null, "The asset with the highest version couldn't be found");
    }

    @Test
    public void retrieveUsageCase() throws JSONException, IOException {
        assertTrue(bestAsset != null, "The asset with the highest version couldn't be found");

        assetUrl = bestAsset.getString("rdf:about");
        String asset = getAssetAsString();
        assetUrl = null; // This is required so that the asset is not deleted
        retrieveArtifact(asset);
    }

    @Test
    public void publishUsageCase()
            throws IOException, NullPointerException, XPathException, ParserConfigurationException, SAXException,
                    JSONException {
        // Gets the asset creation services
        ArrayList<String> serviceUrls = getServiceProviderURLsUsingJson(setupProps.getProperty("baseUri"), onlyOnce);
        ArrayList<String> creationUrls = TestsBase.getCapabilityURLsUsingRdfXml(
                OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, null);
        currentUrl = creationUrls.getFirst();

        // Creates the asset
        assetUrl = createAsset(jsonCreateTemplate);
        assertTrue(assetUrl != null, "The location of the asset after it was create was not returned");

        // Gets the created asset
        String resp = getAssetAsString();
        JSONObject asset = new JSONObject(resp);
        // Gets the artifact factory from the asset
        JSONObject factory = (JSONObject) asset.get("oslc_asset:artifactFactory");
        String artifactFactory = baseUrl + factory.getString("rdf:resource");
        assertTrue(artifactFactory != null && artifactFactory.length() > 0, "There needs to be an artifact factory");

        // Adds an artifact to the asset
        File file = new File(setupProps.getProperty("artifactContentType"));
        var h = Map.entry("oslc_asset.name", file.getName());

        Response response = OSLCUtils.postDataToUrl(
                artifactFactory,
                creds,
                acceptType,
                setupProps.getProperty("artifactContentType"),
                readFileFromProperty("artifactFile"),
                addHeader(null, h));
        response.close();
        assertTrue(
                response.getStatus() == Status.CREATED.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + response.getStatus());

        assertTrue(response.getHeaderString("Location") != null, "No Location header");

        // Updates the artifacts subject
        resp = getAssetAsString();
        asset = new JSONObject(resp);
        JSONArray artifacts = asset.getJSONArray("oslc_asset:artifact");
        JSONObject artifact = artifacts.getJSONObject(0);
        String labelValue = "updated subject";
        artifact.put("oslc:label", labelValue);
        String content = JSONToString(asset);

        // Update the asset
        putAsset(content);
        resp = getAssetAsString();
        asset = new JSONObject(resp);
        artifacts = asset.getJSONArray("oslc_asset:artifact");
        artifact = artifacts.getJSONObject(0);
        assertEquals(labelValue, artifact.getString("oslc:label"), "The label value was not set");
    }

    private JSONObject runQuery() throws IOException, IllegalStateException, JSONException {
        Response resp = executeQuery();
        String content = resp.readEntity(String.class);
        resp.close();
        JSONObject query = new JSONObject(content);
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
        return query;
    }

    private JSONObject getBestAsset(JSONObject query) throws JSONException {
        JSONArray assets = query.getJSONArray("results");
        JSONObject bestAsset = null;
        String highestVersion = "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = (JSONObject) assets.get(i);
            String version = asset.getString("oslc_asset:version");
            if (version.compareTo(highestVersion) > 0) {
                highestVersion = version;
                bestAsset = asset;
            }
        }
        return bestAsset;
    }

    private void retrieveArtifact(String rawAsset) throws JSONException, ClientProtocolException, IOException {
        JSONObject asset = new JSONObject(rawAsset);
        JSONArray artifacts = asset.getJSONArray("oslc_asset:artifact");
        assertTrue(artifacts.length() > 0, "This asset has no artifacts");

        JSONObject artifact = artifacts.getJSONObject(0);
        JSONObject content = artifact.getJSONObject("oslc_asset:content");
        String artifactUrl = content.getString("rdf:resource");

        Response resp = OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
        resp.close();
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
    }

    private String JSONToString(JSONObject jsonObject) throws JSONException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        jsonObject.write(output);
        return output.toString();
    }
}
