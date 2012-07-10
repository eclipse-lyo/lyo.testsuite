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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class CreateAssetJsonTest extends CreateAssetBase {

	public CreateAssetJsonTest(String url) {
		super(url, OSLCConstants.CT_JSON, OSLCConstants.CT_JSON);
	}
	
	@Before
	public void setup()
		throws IOException, ParserConfigurationException, SAXException, XPathException 	{
		super.setup();
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
		assertTrue("The category was not set", asset.get("oslc_asset:categorization") != null);
	}

	@Test
	public void createAssetWithRelationship() throws IOException, JSONException {
		String otherUrl = null;
		try {
			otherUrl = createAsset(jsonCreateTemplate);
			String assetString = readFileFromProperty("createWithRelationshipTemplateJsonFile").replace("%s", otherUrl);
			assetUrl = createAsset(assetString);
			String resp = getAssetAsString();
			JSONObject asset = new JSONObject(resp);
			assertTrue("The category was not set", asset.get("dcterms:relation") != null);
		} finally {
			HttpResponse resp = OSLCUtils.deleteFromUrl(otherUrl, basicCreds, acceptType);
			EntityUtils.consume(resp.getEntity());
		}
	}
	
	@Test
	public void deletingAsset() throws IOException
	{
		deletingAsset(jsonCreateTemplate);
	}
}
