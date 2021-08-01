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

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;

public class GetAndUpdateBase extends AssetTestBase {


	public GetAndUpdateBase(String url, String acceptType, String contentType) {
		super(url, acceptType, contentType);
	    HttpConnectionParams.setConnectionTimeout(OSLCUtils.httpClient.getParams(), 30000);
	}

	@Test
	public void getAnAsset() throws IOException {
		getAssetAsString();
	}

	protected Header[] addHeader(Header header) {
		Header[] newHeaders = new Header[headers.length + 1];
		int i = 0;
		for(; i < headers.length; i++)
		{
			newHeaders[i] = headers[i];
		}
		newHeaders[i] = header;
		return newHeaders;
	}

	/**
	 * Uploads the artifact set in the property file
	 * @param fileName
	 * @param artifactFactory
	 * @return the url location of the artifact
	 */
	protected String uploadArtifact(String artifactFactory) throws IOException {
		File file = new File(setupProps.getProperty("artifactContentType"));
		Header h = new BasicHeader("oslc_asset.name", file.getName());

		HttpResponse resp = OSLCUtils.postDataToUrl(
				artifactFactory, creds, acceptType, setupProps.getProperty("artifactContentType"),
				readFileFromProperty("artifactFile"), addHeader(h));
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED);

		assertTrue("No Location header", resp.getFirstHeader("Location") != null);
		assertTrue("No content length header", resp.getFirstHeader("Content-Length") != null);
		return resp.getFirstHeader("Location").getValue();
	}

	protected void downloadArtifact(String artifactUrl) throws ClientProtocolException, IOException {
		HttpResponse resp = OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
}
