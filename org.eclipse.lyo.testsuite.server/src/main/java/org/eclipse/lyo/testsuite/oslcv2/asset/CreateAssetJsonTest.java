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
import java.io.IOException;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CreateAssetJsonTest extends CreateAssetBase {

    public CreateAssetJsonTest(String url) {
        super(url, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);
    }

    @Test
    public void createSimpleAsset() throws IOException {
        assetUrl = createAsset(jsonCreateTemplate);
    }

    @Test
    public void createAssetWithCategory() throws IOException, JSONException {
        assetUrl = createAsset(readFileFromProperty("createWithCategoryTemplateJsonFile"));
        String resp = getAssetAsString();
        JSONObject asset = new JSONObject(resp);
        assertTrue("The category was not set", asset.containsKey("oslc_asset:categorization"));
    }

    @Test
    public void createAssetWithRelationship() throws IOException, JSONException {
        String otherUrl = null;
        try {
            otherUrl = createAsset(jsonCreateTemplate);
            String assetString = readFileFromProperty("createWithRelationshipTemplateJsonFile")
                    .replace("%s", otherUrl);
            assetUrl = createAsset(assetString);
            String resp = getAssetAsString();
            JSONObject asset = new JSONObject(resp);
            assertTrue("The category was not set", asset.get("dcterms:relation") != null);
        } finally {
            Response resp = OSLCUtils.deleteFromUrl(otherUrl, creds, acceptType);
            resp.close();
        }
    }

    @Test
    public void deletingAsset() throws IOException {
        deletingAsset(jsonCreateTemplate);
    }
}
