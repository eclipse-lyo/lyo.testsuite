/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
 *    Steve Speicher - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server.oslcv2tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;


import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of OSLC Service Provider
 * Catalogs, for the 2.0 version of the OSLC standard, as defined by the OSLC
 * Core Spec.
 */
@RunWith(Parameterized.class)
public abstract class ServiceProviderCatalogBaseTests extends TestsBase {

	// Base URL of the OSLC Service Provider Catalog to be tested
	protected HttpResponse response = null;
	protected String fContentType = null;

	public ServiceProviderCatalogBaseTests(String thisUrl) {
		super(thisUrl);
		currentUrl = thisUrl;
	}

	@Before
	public void setup() throws IOException, ParserConfigurationException,
			SAXException, XPathException {
		super.setup();
	}

	@Test
	public void invalidContentTypeGivesNotSupported() throws IOException {
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl,
				basicCreds, "application/svg+xml", headers);
		if (resp.getEntity() != null) {
			String respType = "";
			if (resp.getEntity().getContentType() != null) {
				respType = resp.getEntity().getContentType().getValue();
			}
			resp.getEntity().consumeContent();
			assertTrue(resp.getStatusLine().getStatusCode() == 406
					|| respType.contains("application/svg+xml"));
		}
	}

	/**
	 * Not required directly from the spec, just mentions that it should be
	 * application/rdf+xml
	 */
	@Test
	public void contentTypeIsSuggestedType() throws IOException {
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl,
				basicCreds, fContentType, headers);
		resp.getEntity().consumeContent();
		// Make sure the response to this URL was of valid type
		String ct = resp.getEntity().getContentType().getValue();
		assertTrue("Expected content-type \"" + fContentType + "\" received : "
				+ ct, ct.contains(fContentType));
	}

	@Test
	public void misplacedParametersDoNotEffectResponse() throws IOException {
		HttpResponse baseResp = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				currentUrl, basicCreds, fContentType, headers);
		String baseRespValue = EntityUtils.toString(baseResp.getEntity());

		HttpResponse parameterResp = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				currentUrl + "?oslc_cm:query", basicCreds, fContentType,
				headers);
		String parameterRespValue = EntityUtils.toString(parameterResp
				.getEntity());

		assertTrue(baseRespValue.equals(parameterRespValue));
	}
}
