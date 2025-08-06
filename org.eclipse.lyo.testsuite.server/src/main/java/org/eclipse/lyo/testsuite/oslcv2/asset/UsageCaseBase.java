/*
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
 */
package org.eclipse.lyo.testsuite.oslcv2.asset;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.runners.Parameterized.Parameters;

public class UsageCaseBase extends AssetTestBase {

    protected static String queryProperty;
    protected static String queryPropertyValue;
    protected static String queryComparisonProperty;
    protected static String queryComparisonValue;
    protected static String fullTextSearchTerm;
    protected static String additionalParameters;

    public UsageCaseBase(String thisUrl, String acceptType, String contentType) {
        super(thisUrl, acceptType, contentType);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        String useThisAsset = setupProps.getProperty("useThisAsset");
        if (useThisAsset != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAsset);
            return toCollection(results);
        }

        queryProperty = setupProps.getProperty("queryEqualityProperty");
        queryPropertyValue = setupProps.getProperty("queryEqualityValue");
        queryComparisonProperty = setupProps.getProperty("queryComparisonProperty");
        queryComparisonValue = setupProps.getProperty("queryComparisonValue");
        fullTextSearchTerm = setupProps.getProperty("fullTextSearchTerm");
        additionalParameters = setupProps.getProperty("queryAdditionalParameters");
        if (additionalParameters == null) additionalParameters = "";

        ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
        ArrayList<String> capabilityURLsUsingRdfXml =
                TestsBase.getCapabilityURLsUsingRdfXml(OSLCConstants.QUERY_BASE_PROP, serviceUrls, true);
        return toCollection(capabilityURLsUsingRdfXml);
    }

    protected Response executeQuery() throws ClientProtocolException, IOException {
        String query = "?oslc.select="
                + URLEncoder.encode("oslc_asset:version", "UTF-8")
                + "&oslc.where="
                + URLEncoder.encode(queryProperty + "=\"" + queryPropertyValue + "\"", "UTF-8");
        String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);
        return OSLCUtils.getDataFromUrl(queryUrl, creds, acceptType, contentType, headers);
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
}
