package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InvalidateOSLCPropertiesJasonTest extends InvalidateOSLCPropertiesTestBase {

	public InvalidateOSLCPropertiesJasonTest(String url) throws IOException, JSONException {
		super(url, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);
		
		assetUrl = createAsset(jsonCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
	}

	@Override
	protected void queryInvalidOSLCProperties(String properties) throws JSONException, IOException {
		String resp = getAssetAsString();
		JSONObject asset = new JSONObject(resp);
		
		// Updates the title
		String name = "updated asset";
		asset.put("dcterms:title", name);
		String content = JSONToString(asset);
		
		// Put invalidate properties
		HttpResponse response = putAssetProperties(properties, content);
		
		assertEquals(HttpStatus.SC_CONFLICT, response.getStatusLine().getStatusCode());
	}

	private String JSONToString(JSONObject jsonObject) throws JSONException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		jsonObject.write(output);
		return output.toString();
	}
}
