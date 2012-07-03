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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.junit.After;
import org.junit.runners.Parameterized.Parameters;

public class AssetTestBase  extends TestsBase {
	protected String assetUrl;
	protected static String acceptType;
	protected static String contentType;
	
	@SuppressWarnings("static-access")
	public AssetTestBase(String url, String acceptType, String contentType)
	{
		super(url);
		this.acceptType = acceptType;
		this.contentType = contentType;
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() throws IOException {
		Properties setupProps = SetupProperties.setup(null);
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(
				setupProps.getProperty("baseUri"), onlyOnce);
		String [] types = null;
		ArrayList<String> capabilityURLsUsingRdfXml = TestsBase.getCapabilityURLsUsingRdfXml(
				OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, types);
		return toCollection(capabilityURLsUsingRdfXml);
	}
	
	@After
	public void tearDown() throws IOException {
		if(assetUrl == null)
			return;
		HttpResponse resp =OSLCUtils.deleteFromUrl(assetUrl, basicCreds, acceptType);
		EntityUtils.consume(resp.getEntity());
	}
	
	/**
	 * Gets an asset and the returns the content of the response in as a string
	 * Guid and version must be set
	 * @throws IOException 
	 */
	protected String getAssetAsString() throws IOException
	{
		HttpResponse resp = OSLCUtils.getDataFromUrl(assetUrl, basicCreds, acceptType, contentType, headers);
		String content = EntityUtils.toString(resp.getEntity());
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		return content;
	}
	
	/**
	 * Get's an asset and then returns the response
	 * @throws IOException 
	 */
	protected HttpResponse getAssetResponse() throws IOException
	{
		HttpResponse resp = OSLCUtils.getDataFromUrl(assetUrl, basicCreds, acceptType, contentType, headers);
		try {
			assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
					resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		} catch (AssertionError e) {
			EntityUtils.consume(resp.getEntity());
			throw e;
		}
		return resp;
	}
	
	/**
	 * Creates an asset and then asserts that it was created. If all goes well the url to
	 * the created asset is returned
	 */
	protected String createAsset(String content) throws IOException
	{
		HttpResponse resp = 
			OSLCUtils.postDataToUrl(currentUrl, basicCreds, acceptType, contentType, content, headers);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected: " + HttpStatus.SC_CREATED + ", received: " + resp.getStatusLine().getStatusCode(),
				HttpStatus.SC_CREATED == resp.getStatusLine().getStatusCode());
		
		Header loc = resp.getFirstHeader("Location");
		return loc.getValue();
	}
	
	/**
	 * Given a property from the property file, the contents of the file are returned
	 */
	protected String readFileFromProperty(String property) {
		String fileName = setupProps.getProperty(property);
		if(fileName == null)
			return fileName;
		return OSLCUtils.readFileByNameAsString(fileName);
	}
	
	protected void putAsset(String content) throws IOException {
		HttpResponse resp = OSLCUtils.putDataToUrl(assetUrl,
				basicCreds, acceptType, contentType, content, headers);

		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
}
