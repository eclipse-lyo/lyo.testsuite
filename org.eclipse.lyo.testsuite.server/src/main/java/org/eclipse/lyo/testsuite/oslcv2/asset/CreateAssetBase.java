/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation.
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
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
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
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
    }
}
