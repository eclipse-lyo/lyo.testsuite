package org.eclipse.lyo.testsuite.oslcv2.core;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class FetchResourceJsonTests extends FetchResourceTests {
    public FetchResourceJsonTests(String url) {
        super(url);
    }

    // TODO: JSON is not required by all tests, consider remove test annotation here, then
    // subclassing by domain tests to add it back in.
    @Test
    public void getValidResourceUsingJSON() throws IOException, NullPointerException, JSONException {
        String body = getValidResourceUsingContentType(OSLCConstants.CT_JSON);

        JSONArtifact userData = JSON.parse(body);
        assertNotNull("Received JSON content but did not parse properly", userData);
    }
}
