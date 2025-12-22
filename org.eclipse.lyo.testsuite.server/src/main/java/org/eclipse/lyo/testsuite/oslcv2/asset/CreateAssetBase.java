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
import org.eclipse.lyo.testsuite.util.OSLCUtils;

public class CreateAssetBase extends AssetTestBase {

    public CreateAssetBase(String url, String acceptType, String contentType) {
        super(url, acceptType, contentType);
    }

    protected void deletingAsset(String initialAsset) throws IOException {
        assetUrl = createAsset(initialAsset);
        Response resp = OSLCUtils.deleteFromUrl(assetUrl, creds, acceptType);
        resp.close();
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
    }
}
