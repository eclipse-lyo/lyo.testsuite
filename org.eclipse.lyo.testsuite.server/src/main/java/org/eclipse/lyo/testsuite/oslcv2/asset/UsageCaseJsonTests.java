/*
 * Copyright (c) 2012, 2014, 2025 IBM Corporation and others
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
 *    Tim Eck II - asset management test cases
 */
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;
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
        assertTrue("The asset with the highest version couldn't be found", bestAsset != null);
    }

    @Test
    public void retrieveUsageCase() throws JSONException, IOException {
        assertTrue("The asset with the highest version couldn't be found", bestAsset != null);

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
        assertTrue("The location of the asset after it was create was not returned", assetUrl != null);

        // Gets the created asset
        String resp = getAssetAsString();
        JSONObject asset = new JSONObject(resp);
        // Gets the artifact factory from the asset
        JSONObject factory = (JSONObject) asset.get("oslc_asset:artifactFactory");
        String artifactFactory = baseUrl + factory.getString("rdf:resource");
        assertTrue("There needs to be an artifact factory", artifactFactory != null && artifactFactory.length() > 0);

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
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + response.getStatus(),
                response.getStatus() == Status.CREATED.getStatusCode());

        assertTrue("No Location header", response.getHeaderString("Location") != null);

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
        assertEquals("The label value was not set", labelValue, artifact.getString("oslc:label"));
    }

    private JSONObject runQuery() throws IOException, IllegalStateException, JSONException {
        Response resp = executeQuery();
        String content = resp.readEntity(String.class);
        resp.close();
        JSONObject query = new JSONObject(content);
        assertTrue(
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
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
        assertTrue("This asset has no artifacts", artifacts.length() > 0);

        JSONObject artifact = artifacts.getJSONObject(0);
        JSONObject content = artifact.getJSONObject("oslc_asset:content");
        String artifactUrl = content.getString("rdf:resource");

        Response resp = OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
        resp.close();
        assertTrue(
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
    }

    private String JSONToString(JSONObject jsonObject) throws JSONException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        jsonObject.write(output);
        return output.toString();
    }
}
