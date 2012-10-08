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
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Feed;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.SimplifiedQueryBaseTests;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the basic validation of query factories
 * as specified in the OSLC version 2 spec. This version of the query tests only
 * tests the basic status code and form of the query responses, as without
 * shapes implemented it is difficult to represent the needed various templates
 * of different change request types and to query for the templates.
 */
@RunWith(Parameterized.class)
public class SimplifiedQueryAtomTests extends SimplifiedQueryBaseTests {

	public SimplifiedQueryAtomTests(String thisUri) {
		super(thisUri);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
			throws IOException, ParserConfigurationException, SAXException,
			XPathException {
		// Checks the ServiceProviderCatalog at the specified baseUrl of the
		// REST service in order to grab all urls
		// to other ServiceProvidersCatalogs contained within it, recursively,
		// in order to find the URLs of all
		// query factories of the REST service.
		String v = "//oslc_v2:QueryCapability/oslc_v2:queryBase/@rdf:resource";
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
		ArrayList<String> capabilityURLsUsingXML = TestsBase
				.getCapabilityURLsUsingXML(v, serviceUrls, true);
		return toCollection(capabilityURLsUsingXML);
	}

	protected void validateNonEmptyResponse(String query)
			throws XPathExpressionException, IOException,
			ParserConfigurationException, SAXException {
		String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);
		
		// Send ATOM feed request
		HttpResponse response = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				queryUrl, basicCreds, OSLCConstants.CT_ATOM, headers);

		int statusCode = response.getStatusLine().getStatusCode();
		if (HttpStatus.SC_OK != statusCode)
		{
			EntityUtils.consume(response.getEntity());
			throw new IOException("Response code: " + statusCode + " for " + queryUrl);
		}

		String responseBody = EntityUtils.toString(response.getEntity());
		//
		// Validate ATOM feed response
		//
		InputStream bais = new ByteArrayInputStream(responseBody.getBytes());  
		
		// Create mapping of ATOM variables
		Abdera abdera = new Abdera();		
		org.apache.abdera.model.Document<Feed> model = abdera.getParser().parse(bais);
		
		assertNotNull(model);
		Feed feed = model.getRoot();

		// Validate the ATOS feeds
		assertNotNull("Atom feeds must have a title", feed.getTitle());
		assertNotNull("Atom feeds must have an ID", feed.getId());
		assertNotNull("Atom feeds must have an updated element", feed.getUpdated());
		
	}

	@Test
	public void validEqualsQueryContainsExpectedResource() throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {
		String query = getQueryUrlForValidEqualsQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validNotEqualQueryContainsExpectedResource()
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		String query = getQueryUrlForValidNotEqualQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validLessThanQueryContainsExpectedResources()
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException, ParseException {
		String query = getQueryUrlForValidLessThanQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validGreaterThanQueryContainsExpectedDefects()
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException, ParseException {
		String query = getQueryUrlForValidGreaterThanQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validCompoundQueryContainsExpectedResource()
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		String query = getQueryUrlForValidCompoundQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void fullTextSearchContainsExpectedResults() throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {
		if ( !getFullTextSearch() ) return;
		
		String query = getQueryUrlForFullTextSearchContainsExpectedResults();
		validateNonEmptyResponse(query);
	}
}