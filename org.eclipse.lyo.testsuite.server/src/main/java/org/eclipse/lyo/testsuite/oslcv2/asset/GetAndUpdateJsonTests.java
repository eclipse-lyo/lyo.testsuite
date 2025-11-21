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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.http.ParseException;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class GetAndUpdateJsonTests extends GetAndUpdateBase {
    private JSONObject hasJson;

    public GetAndUpdateJsonTests(String thisUrl) throws IOException, JSONException {
        super(thisUrl, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);

        assetUrl = createAsset(jsonCreateTemplate);
        assertTrue(assetUrl != null, "The location of the asset after it was create was not returned");

        String resp = getAssetAsString();
        hasJson = new JSONObject(resp);
    }

    @Test
    public void assetHasArtifactFactory() {
        assertTrue(hasJson.has("oslc_asset:artifactFactory"), "Artifact Factory was not found");
    }

    @Test
    public void assetHasTitle() {
        assertTrue(hasJson.has(OSLCConstants.DCTERMS_TITLE), "Title was not found");
    }

    @Test
    public void updateAnAssetProperty()
            throws IOException, ParseException, ParserConfigurationException, SAXException, TransformerException,
                    JSONException {
        // Get the asset
        String resp = getAssetAsString();
        JSONObject asset = new JSONObject(resp);

        // Updates the title
        String name = "updated asset";
        asset.put("dcterms:title", name);
        String content = JSONToString(asset);

        // Update the asset
        putAsset(content);

        // Get the asset again to verify it the asset was updated
        resp = getAssetAsString();
        asset = new JSONObject(resp);

        // NodeList children = getAssetNodeChildren(document);
        String actualName = asset.getString("dcterms:title");
        assertTrue(name.equals(actualName), "Expected " + name + ", received " + actualName);
    }

    @Test
    public void addArtifactToAsset()
            throws IOException, ParseException, ParserConfigurationException, SAXException, IllegalStateException,
                    JSONException {
        // Get the asset to add the artifact too
        String resp = getAssetAsString();

        JSONObject asset = new JSONObject(resp);
        JSONObject factory = (JSONObject) asset.get("oslc_asset:artifactFactory");
        String artifactFactory = factory.getString("rdf:resource");
        assertTrue(
                artifactFactory != null && artifactFactory.length() > 0, "There needs to be an artifact factory url");

        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        assertTrue(fileName != null, "There needs to be an artifact template file");
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        Response response = OSLCUtils.postDataToUrl(
                artifactFactory, creds, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON, artifact, header);
        response.close();
        assertTrue(
                response.getStatus() == Status.CREATED.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + response.getStatus());
    }

    @Test
    public void uploadArtifact() throws IOException, JSONException {
        String artifactFactory = getArtifactFactory();
        uploadArtifact(artifactFactory);
    }

    @Test
    public void downloadArtifact() throws JSONException, IOException {
        String artifactFactory = getArtifactFactory();
        String location = uploadArtifact(artifactFactory);
        downloadArtifact(location);
    }

    @Test
    public void removeArtifactFromAsset()
            throws IOException, TransformerException, ParseException, ParserConfigurationException, SAXException,
                    IllegalStateException, JSONException {
        String artifactFactory = getArtifactFactory();
        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactJsonFile");
        assertTrue(fileName != null, "There needs to be an artifact template file");
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        // Adds the artifact to the asset
        Response response = OSLCUtils.postDataToUrl(
                artifactFactory, creds, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON, artifact, header);
        response.close();

        // Gets the asset with the artifact added to it
        String resp = getAssetAsString();
        JSONObject asset = new JSONObject(resp);
        asset.remove("oslc_asset:artifact");
        String content = JSONToString(asset);
        putAsset(content);

        resp = getAssetAsString();
        asset = new JSONObject(resp);
        assertTrue(!asset.containsKey("oslc_asset:artifact"), "The artifact was not removed");
    }

    private String JSONToString(JSONObject jsonObject) throws JSONException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        jsonObject.write(output);
        return output.toString();
    }

    private String getArtifactFactory() throws IOException, JSONException {
        // Gets the asset that the artifact will be added too
        String resp = getAssetAsString();

        JSONObject asset = new JSONObject(resp);
        // Gets the artifact factory from the asset
        JSONObject factory = (JSONObject) asset.get("oslc_asset:artifactFactory");
        String artifactFactory = factory.getString("rdf:resource");
        assertTrue(artifactFactory != null && artifactFactory.length() > 0, "There needs to be an artifact factory");
        return artifactFactory;
    }
}
