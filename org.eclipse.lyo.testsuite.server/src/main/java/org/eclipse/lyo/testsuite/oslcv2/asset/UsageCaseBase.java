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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

public class UsageCaseBase extends AssetTestBase {

	protected String queryProperty;
	protected String queryPropertyValue;
	protected String queryComparisonProperty;
	protected String queryComparisonValue;
	protected String fullTextSearchTerm;
	protected String additionalParameters;

	public UsageCaseBase(String thisUrl, String acceptType, String contentType) {
		super(thisUrl, acceptType, contentType);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
			throws IOException {
		Properties setupProps = SetupProperties.setup(null);
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"),
				onlyOnce);
		ArrayList<String> capabilityURLsUsingRdfXml = TestsBase
				.getCapabilityURLsUsingRdfXml(OSLCConstants.QUERY_BASE_PROP,
						serviceUrls, true);
		return toCollection(capabilityURLsUsingRdfXml);		
	}

	@Before
	public void setup() throws IOException, ParserConfigurationException,
			SAXException, XPathException {
		super.setup();
		queryProperty = setupProps.getProperty("queryEqualityProperty");
		queryPropertyValue = setupProps.getProperty("queryEqualityValue");
		queryComparisonProperty = setupProps
				.getProperty("queryComparisonProperty");
		queryComparisonValue = setupProps.getProperty("queryComparisonValue");
		fullTextSearchTerm = setupProps.getProperty("fullTextSearchTerm");
		additionalParameters = setupProps
				.getProperty("queryAdditionalParameters");
		if (additionalParameters == null)
			additionalParameters = "";
	}
	
	protected HttpResponse executeQuery() throws ClientProtocolException, IOException {
		String query =  "?oslc.select=" + URLEncoder.encode("oslc_asset:version", "UTF-8") +
						"&oslc.where=" +
						URLEncoder.encode(queryProperty + "=\"" + queryPropertyValue + "\"", "UTF-8");
		String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);
		return OSLCUtils.getDataFromUrl(queryUrl, basicCreds, acceptType, contentType, headers);
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
}
