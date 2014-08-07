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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class GetAndUpdateJsonTests extends GetAndUpdateBase {
	private JSONObject hasJson;

	public GetAndUpdateJsonTests(String thisUrl) throws IOException, JSONException {
		super(thisUrl, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);
		
		assetUrl = createAsset(jsonCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
		
		String resp = getAssetAsString();
		hasJson = new JSONObject(resp);
	}

	@Test
	public void assetHasArtifactFactory() {
		assertTrue("Artifact Factory was not found", hasJson.has("oslc_asset:artifactFactory"));
	}

	@Test
	public void assetHasTitle() {
		assertTrue("Title was not found", hasJson.has(OSLCConstants.DCTERMS_TITLE));
	}
			
	@Test
	public void updateAnAssetProperty() 
			throws IOException, ParseException, ParserConfigurationException, SAXException, TransformerException, JSONException
	{
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
		
		//NodeList children = getAssetNodeChildren(document);
		String actualName = asset.getString("dcterms:title");
		assertTrue("Expected " + name + ", received " + actualName, name.equals(actualName));
	}
	
	@Test
	public void addArtifactToAsset() 
			throws IOException, ParseException, ParserConfigurationException, SAXException, IllegalStateException, JSONException
	{
		// Get the asset to add the artifact too
		String resp = getAssetAsString();

		JSONObject asset = new JSONObject(resp);	
		JSONObject factory = (JSONObject) asset.get("oslc_asset:artifactFactory");
		String artifactFactory = factory.getString("rdf:resource");
		assertTrue("There needs to be an artifact factory url",
				artifactFactory != null && artifactFactory.length() > 0);	
		
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));
		
		String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		HttpResponse response = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
					OSLCConstants.CT_JSON, OSLCConstants.CT_JSON, artifact, header);
		EntityUtils.consume(response.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + response.getStatusLine().getStatusCode(),
				response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED);
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
			throws IOException, TransformerException, ParseException, ParserConfigurationException,
			SAXException, IllegalStateException, JSONException {
		String artifactFactory = getArtifactFactory();		
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));

		String fileName = setupProps.getProperty("createTemplateArtifactJsonFile");
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		// Adds the artifact to the asset
		HttpResponse response = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
				OSLCConstants.CT_JSON, OSLCConstants.CT_JSON, artifact, header);
		EntityUtils.consume(response.getEntity());
		
		// Gets the asset with the artifact added to it
		String resp = getAssetAsString();
		JSONObject asset = new JSONObject(resp);
		asset.remove("oslc_asset:artifact");
		String content = JSONToString(asset);
		putAsset(content);

		resp = getAssetAsString();
		asset = new JSONObject(resp);
		assertTrue("The artifact was not removed", !asset.containsKey("oslc_asset:artifact"));
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
		assertTrue("There needs to be an artifact factory",
				artifactFactory != null && artifactFactory.length() > 0);
		return artifactFactory;
	}
}