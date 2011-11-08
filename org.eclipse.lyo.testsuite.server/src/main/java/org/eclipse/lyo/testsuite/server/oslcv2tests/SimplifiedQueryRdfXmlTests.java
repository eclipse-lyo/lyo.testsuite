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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.SetupProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * This class provides JUnit tests for the basic validation of query factories
 * as specified in the OSLC version 2 spec. This version of the query tests only
 * tests the basic status code and form of the query responses, as without
 * shapes implemented it is difficult to represent the needed various templates
 * of different change request types and to query for the templates.
 */
@RunWith(Parameterized.class)
public class SimplifiedQueryRdfXmlTests extends SimplifiedQueryBaseTests {

	public SimplifiedQueryRdfXmlTests(String thisUri) {
		super(thisUri);
	}

	@Before
	public void setup() throws IOException, ParserConfigurationException,
			SAXException, XPathException {
		super.setup();
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

	protected void validateNonEmptyResponse(String query)
			throws IOException {
		HttpResponse response = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				currentUrl + query, basicCreds, OSLCConstants.CT_RDF, headers);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

		Model queryModel = ModelFactory.createDefaultModel();
		queryModel.read(response.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
				OSLCConstants.JENA_RDF_XML);
		EntityUtils.consume(response.getEntity());
		Resource responseInfoRes = (Resource) queryModel.getResource(currentUrl + query);
		assumeNotNull("Expended ResponseInfo/@rdf:about to equal request URL", responseInfoRes);
		Resource resultsRes = (Resource) queryModel.getResource(currentUrl);
		assumeNotNull(resultsRes);

		// oslc:ResponseInfo if optional, validate it if one exists
		Resource respInfoType = queryModel.createResource(OSLCConstants.RESP_INFO_TYPE);
		Property rdfType = queryModel.getProperty(OSLCConstants.RDF_TYPE_PROP);
		StmtIterator stmts = queryModel.listStatements(responseInfoRes, rdfType, respInfoType);
		List<?> stmtsList = stmts.toList();
		if (stmtsList.size() > 0) {
			assertTrue("Expected ResponseInfo type for request URL",
					stmtsList.size() > 0);

			Property countMember = queryModel.getProperty(OSLCConstants.TOTAL_COUNT_PROP);
			stmts = queryModel.listStatements(responseInfoRes, countMember, (RDFNode)null);
			stmtsList = stmts.toList();
			Statement stmt = (Statement) stmtsList.get(0);
			assertTrue("Expected oslc:totalCount property", stmtsList.size() == 1);
			int totalCount = Integer.parseInt(stmt.getObject().toString());
			assertTrue("Expected oslc:totalCount > 0",
					totalCount > 0);

			Property rdfsMember = queryModel.getProperty(OSLCConstants.RDFS_MEMBER);
			stmts = queryModel.listStatements(resultsRes, rdfsMember, (RDFNode)null);
			stmtsList = stmts.toList();
			assertNotNull("Expected > 1 rdfs:member(s)", stmtsList.size() > 0);
		}
	}

	@Test
	public void validEqualsQueryContainsExpectedResource() throws IOException {
		String query = getQueryUrlForalidEqualsQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validNotEqualQueryContainsExpectedResource()
			throws IOException {
		String query = getQueryUrlForValidNotEqualQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validLessThanQueryContainsExpectedResources()
			throws IOException  {
		String query = getQueryUrlForValidLessThanQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validGreaterThanQueryContainsExpectedDefects()
			throws IOException {
		String query = getQueryUrlForValidGreaterThanQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void validCompoundQueryContainsExpectedResource()
			throws IOException {
		String query = getQueryUrlForValidCompoundQueryContainsExpectedResources();
		validateNonEmptyResponse(query);
	}

	@Test
	public void fullTextSearchContainsExpectedResults() throws IOException {
		String query = getQueryUrlForFullTextSearchContainsExpectedResults();
		validateNonEmptyResponse(query);
	}
}