/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class UsageCaseJsonTests extends UsageCaseBase {
	private static JSONObject bestAsset = null;
	
	public UsageCaseJsonTests(String thisUrl) {
		super(thisUrl, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);
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
		
		assetUrl = bestAsset.getString("about");
		String asset = getAssetAsString();
		assetUrl = null; // This is required so that the asset is not deleted
		retrieveArtifact(asset);
	}

	@Test
	public void publishUsageCase()
		throws IOException, NullPointerException, XPathException, ParserConfigurationException, SAXException, JSONException {
		//Gets the asset creation services
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingJson(
				setupProps.getProperty("baseUri"), onlyOnce);
		ArrayList<String> creationUrls = TestsBase.getCapabilityURLsUsingRdfXml(
				OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, null);
		currentUrl = creationUrls.get(0);
		
		// Creates the asset
		assetUrl = createAsset(jsonCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
		
		// Gets the created asset
		String resp = getAssetAsString();
		JSONObject asset = new JSONObject(resp);	
		// Gets the artifact factory from the asset
		JSONObject factory = (JSONObject) asset.get("oslc_asset:artifactFactory");
		String artifactFactory = asset.getString("base") + factory.getString("resource");
		assertTrue("There needs to be an artifact factory",
				artifactFactory != null && artifactFactory.length() > 0);
		
		// Adds an artifact to the asset
		File file = new File(setupProps.getProperty("artifactContentType"));
		BasicHeader h = new BasicHeader("oslc_asset.name", file.getName());
		
		HttpResponse response = OSLCUtils.postDataToUrl(
				artifactFactory, basicCreds, acceptType, setupProps.getProperty("artifactContentType"),
				readFileFromProperty("artifactFile"), addHeader(h));
		EntityUtils.consume(response.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + response.getStatusLine().getStatusCode(),
				response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED);
		
		assertTrue("No Location header", response.getFirstHeader("Location") != null);
		
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
		HttpResponse resp = executeQuery();
		String content = EntityUtils.toString(resp.getEntity());
		EntityUtils.consume(resp.getEntity());
		JSONObject query = new JSONObject(content);
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		return query;
	}
	
	private JSONObject getBestAsset(JSONObject query) throws JSONException {
		JSONArray assets = query.getJSONArray("results");
		JSONObject bestAsset = null;
		String highestVersion = "";
		for(int i = 0; i < assets.length(); i++) {
			JSONObject asset = (JSONObject) assets.get(i);
			String version = asset.getString("oslc_asset:version");
			if(version.compareTo(highestVersion) > 0) {
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
		
		HttpResponse resp = OSLCUtils.getDataFromUrl(artifactUrl, basicCreds, acceptType, contentType, headers);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
	
	private String JSONToString(JSONObject jsonObject) throws JSONException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		jsonObject.write(output);
		return output.toString();
	}
}
