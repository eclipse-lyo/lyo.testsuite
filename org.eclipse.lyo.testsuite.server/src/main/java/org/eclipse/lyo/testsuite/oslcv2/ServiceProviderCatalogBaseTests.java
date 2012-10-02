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
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * This class provides JUnit tests for the validation of OSLC Service Provider
 * Catalogs, for the 2.0 version of the OSLC standard, as defined by the OSLC
 * Core Spec.
 */
@RunWith(Parameterized.class)
public abstract class ServiceProviderCatalogBaseTests extends TestsBase {

	// Base URL of the OSLC Service Provider Catalog to be tested
	//protected HttpResponse response = null;
	protected static String fContentType = null;

	public ServiceProviderCatalogBaseTests(String thisUrl) {
		super(thisUrl);
		currentUrl = thisUrl;
	}

	@Test
	public void invalidContentTypeGivesNotSupportedOPTIONAL() throws IOException {
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl,
				basicCreds, "invalid/content-type", headers);
		if (resp.getEntity() != null) {
			String respType = "";
			if (resp.getEntity().getContentType() != null) {
				respType = resp.getEntity().getContentType().getValue();
			}
			EntityUtils.consume(resp.getEntity());
			assertTrue("Expected 406 but received " + resp.getStatusLine()
					+ " or Content-type='invalid/content-type' but received "
					+ respType, resp.getStatusLine().getStatusCode() == 406
					|| respType.contains("invalid/content-type"));
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
		EntityUtils.consume(resp.getEntity());
		// Make sure the response to this URL was of valid type
		String ct = resp.getEntity().getContentType().getValue();
		assertTrue("Expected content-type \"" + fContentType + "\" received : "
				+ ct, ct.contains(fContentType));
	}
}
