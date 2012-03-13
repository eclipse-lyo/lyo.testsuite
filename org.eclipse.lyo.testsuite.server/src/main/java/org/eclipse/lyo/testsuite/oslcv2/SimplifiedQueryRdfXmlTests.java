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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
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
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

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
		String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);
		HttpResponse response = OSLCUtils.getResponseFromUrl(setupBaseUrl,
				queryUrl, basicCreds, OSLCConstants.CT_RDF, headers);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

		Model queryModel = ModelFactory.createDefaultModel();
		queryModel.read(response.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
				OSLCConstants.JENA_RDF_XML);
		EntityUtils.consume(response.getEntity());
		RDFUtils.validateModel(queryModel);
		
		Resource resultsRes = queryModel.getResource(currentUrl);
		assertTrue("Expected a results resource with URI: " + currentUrl,
				queryModel.contains(resultsRes, null));

		// oslc:ResponseInfo if optional, validate it if one exists
		Resource respInfoType = queryModel.createResource(OSLCConstants.RESP_INFO_TYPE);
		ResIterator resIter = queryModel.listSubjectsWithProperty(RDF.type, respInfoType);
		while (resIter.hasNext()) {
			Resource responseInfoRes = resIter.nextResource();
			assertEquals(
					"Response info URI should match the request URI (with query parameters)",
					queryUrl, responseInfoRes.getURI());

			Property countMember = queryModel
					.getProperty(OSLCConstants.TOTAL_COUNT_PROP);
			StmtIterator stmts = responseInfoRes.listProperties(countMember);
			List<?> stmtsList = stmts.toList();
			if (!stmtsList.isEmpty()) {
				Statement stmt = (Statement) stmtsList.get(0);
				assertTrue("Expected oslc:totalCount property",
						stmtsList.size() == 1);
				int totalCount = Integer.parseInt(stmt.getObject().toString());
				assertTrue("Expected oslc:totalCount > 0", totalCount > 0);
			}

			stmts = queryModel.listStatements(resultsRes, RDFS.member, (RDFNode)null);
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
