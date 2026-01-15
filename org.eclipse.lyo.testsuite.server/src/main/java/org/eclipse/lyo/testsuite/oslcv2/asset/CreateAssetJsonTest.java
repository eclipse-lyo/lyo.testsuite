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
import java.io.IOException;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CreateAssetJsonTest extends CreateAssetBase {

    public CreateAssetJsonTest() {
        super(null, null, null);
    }

    protected void setup(String url) {

        currentUrl = url;
        acceptType = OSLCConstants.CT_JSON;
        contentType = OSLCConstants.CT_JSON;
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void createSimpleAsset(String thisUrl) throws IOException {
        setup(thisUrl);
        assetUrl = createAsset(jsonCreateTemplate);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void createAssetWithCategory(String thisUrl) throws IOException, JSONException {
        setup(thisUrl);
        assetUrl = createAsset(readFileFromProperty("createWithCategoryTemplateJsonFile"));
        String resp = getAssetAsString();
        JSONObject asset = new JSONObject(resp);
        assertTrue(asset.containsKey("oslc_asset:categorization"), "The category was not set");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void createAssetWithRelationship(String thisUrl) throws IOException, JSONException {
        setup(thisUrl);
        String otherUrl = null;
        try {
            otherUrl = createAsset(jsonCreateTemplate);
            String assetString = readFileFromProperty("createWithRelationshipTemplateJsonFile")
                    .replace("%s", otherUrl);
            assetUrl = createAsset(assetString);
            String resp = getAssetAsString();
            JSONObject asset = new JSONObject(resp);
            assertTrue(asset.get("dcterms:relation") != null, "The category was not set");
        } finally {
            Response resp = OSLCUtils.deleteFromUrl(otherUrl, creds, acceptType);
            resp.close();
        }
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void deletingAsset(String thisUrl) throws IOException {
        setup(thisUrl);
        deletingAsset(jsonCreateTemplate);
    }
}
