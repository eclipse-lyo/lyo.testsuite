/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation.
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
 *    Samuel Padgett - don't fail if queryAdditionalParameters is not defined
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
 * This class provides JUnit tests for the validation of the OSLCv2 fetching
 * of resources.
 *
 */
@RunWith(Parameterized.class)
public class FetchResourceTests extends TestsBase {

	public FetchResourceTests(String url) {
		super(url);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
			throws IOException {
		staticSetup();
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//query capability of the REST service, then fetch the resource using the query service.

		ArrayList<String> capabilityURLsUsingRdfXml = null;
        String useThisQuery = setupProps.getProperty("useThisQuery");

		if ( useThisQuery != null ) {
			capabilityURLsUsingRdfXml = new ArrayList<String>();
			capabilityURLsUsingRdfXml.add(useThisQuery);
		} else {
			ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
		    capabilityURLsUsingRdfXml = TestsBase.getCapabilityURLsUsingRdfXml(OSLCConstants.QUERY_BASE_PROP, serviceUrls, true);
		}
		String where = setupProps.getProperty("changeRequestWhere");
		if (where == null) {
			String queryProperty = setupProps.getProperty("queryEqualityProperty");
			String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
			where = queryProperty + "=\"" + queryPropertyValue + "\"";
		}

		String additionalParameters = setupProps.getProperty("queryAdditionalParameters", "");
		String query = (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&";
		query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";

		ArrayList<String> results = new ArrayList<String>();
		for (String queryBaseUri : capabilityURLsUsingRdfXml) {
			String queryUrl = OSLCUtils.addQueryStringToURL(queryBaseUri, query);
			HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds,
					OSLCConstants.CT_RDF, headers);
			Model queryModel = ModelFactory.createDefaultModel();
			queryModel.read(resp.getEntity().getContent(), queryBaseUri, OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(queryModel);

			Property member = queryModel.createProperty(OSLCConstants.RDFS_MEMBER);
			Resource queryBase = queryModel.getResource(queryBaseUri);
			Selector select = new SimpleSelector(queryBase, member, (RDFNode)null);
			StmtIterator statements = queryModel.listStatements(select);
			while (statements.hasNext()) {
				results.add(statements.nextStatement().getObject().toString());
				if (onlyOnce) return toCollection(results);
			}
			if (!results.isEmpty() && onlyOnce)
				break;
		}
		return toCollection(results);
	}

	protected String getValidResourceUsingContentType(String requestType) throws IOException {
		HttpResponse resp = OSLCUtils.getResponseFromUrl(currentUrl, currentUrl, creds,
				requestType, headers);

		String responseBody = EntityUtils.toString(resp.getEntity());
		EntityUtils.consume(resp.getEntity());
		assertEquals("Expected response code 200 but received " + resp.getStatusLine(), HttpStatus.SC_OK, resp.getStatusLine()
				.getStatusCode());

		String contentType = OSLCUtils.getContentType(resp);

		assertEquals("Expected content-type "+requestType+" but received "+contentType, requestType, contentType);

		return responseBody;
	}

	@Test
	public void getValidResourceUsingRdfXml() throws IOException {
		String body = getValidResourceUsingContentType(OSLCConstants.CT_RDF);

		Model rdfModel = ModelFactory.createDefaultModel();
		rdfModel.read(new ByteArrayInputStream( body.getBytes() ),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
				OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(rdfModel);
	}

	@Test
	public void getValidResourceUsingXml() throws IOException, ParserConfigurationException, SAXException {
		String body = getValidResourceUsingContentType(OSLCConstants.CT_XML);

		Document doc = OSLCUtils.createXMLDocFromResponseBody(body);
		assertNotNull("XML document did not parse successfully", doc);
	}

	// TODO: JSON is not required by all tests, consider remove test annotation here, then
	// subclassing by domain tests to add it back in.
	@Test
	public void getValidResourceUsingJSON() throws IOException, NullPointerException, JSONException {
		String body = getValidResourceUsingContentType(OSLCConstants.CT_JSON);

		JSONArtifact userData = JSON.parse(body);
		assertNotNull("Received JSON content but did not parse properly", userData);
	}

	@Test
	public void getValidResourceUsingCOMPACT() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		String body = getValidResourceUsingContentType(OSLCConstants.CT_COMPACT);

		Document doc = OSLCUtils.createXMLDocFromResponseBody(body);

		Node compactNode = (Node) OSLCUtils.getXPath().evaluate(
				"/*/oslc_v2:Compact", doc,
				XPathConstants.NODE);
		assertNotNull(compactNode);

		// Everything is optional in the oslc:Compact representation.

		NodeList nodeList = (NodeList) OSLCUtils.getXPath().evaluate(
				"./dc:title", compactNode,
				XPathConstants.NODESET);
		int numNodes = nodeList.getLength();
		assertTrue("Expected number of dcterms:titles to be <=1 but was: "+numNodes, numNodes <= 1);

		nodeList = (NodeList) OSLCUtils.getXPath().evaluate(
				"./oslc_v2:shortTitle", compactNode,
				XPathConstants.NODESET);
		numNodes = nodeList.getLength();
		assertTrue("Expected number of oslc:shortTitles to be <=1 but was: "+numNodes, numNodes <= 1);

		nodeList = (NodeList) OSLCUtils.getXPath().evaluate(
				"./oslc_v2:icon", compactNode,
				XPathConstants.NODESET);
		numNodes = nodeList.getLength();
		assertTrue("Expected number of oslc:icon to be <=1 but was: "+numNodes, numNodes <= 1);

		String iconUrl = null;
		if (numNodes == 1) {
			Node rdfAbout = nodeList.item(0).getAttributes().getNamedItemNS(OSLCConstants.RDF, "resource");
			assertNotNull("oslc:icon in oslc:Compact missing rdf:about attribute", rdfAbout);
			iconUrl = rdfAbout.getTextContent();

			HttpResponse response = OSLCUtils.getResponseFromUrl(iconUrl, iconUrl, creds,
	        		"*/*", headers);
	        int statusCode = response.getStatusLine().getStatusCode();
	        EntityUtils.consume(response.getEntity());
	        assertTrue("Fetching icon from "+iconUrl+" did not respond with expected code, received "+statusCode, 200<=statusCode && statusCode<400);
		}

		nodeList = (NodeList) OSLCUtils.getXPath().evaluate(
				"./oslc_v2:smallPreview", compactNode,
				XPathConstants.NODESET);
		numNodes = nodeList.getLength();
		assertTrue("Expected number of oslc:smallPreview is 0 or 1 but was: "+numNodes, numNodes <= 1);
		if (numNodes == 1)
			validateCompactPreview(nodeList);

		nodeList = (NodeList) OSLCUtils.getXPath().evaluate(
				"./oslc_v2:largePreview", compactNode,
				XPathConstants.NODESET);
		numNodes = nodeList.getLength();
		assertTrue("Expected number of oslc:largePreview is 0 or 1 but was: "+numNodes, numNodes <= 1);
		if (numNodes == 1)
			validateCompactPreview(nodeList);
	}

	/**
	 * Assume that nodeList.getLength()==1
	 */
	protected void validateCompactPreview(NodeList nodeList) throws IOException, XPathExpressionException {
		Node node = (Node) OSLCUtils.getXPath().evaluate(
				"./oslc_v2:Preview/oslc_v2:document/@rdf:resource", nodeList.item(0),
				XPathConstants.NODE);
		assertNotNull("Expected number of oslc:Preview/oslc:document/@rdf:resource", node);
		String previewUrl = node.getTextContent();
		HttpResponse response = OSLCUtils.getResponseFromUrl(previewUrl, previewUrl, creds,
        		"*/*", headers);

        int statusCode = response.getStatusLine().getStatusCode();
        String contentType = response.getEntity().getContentType().getValue();
        EntityUtils.consume(response.getEntity());
        assertTrue("Fetching document preview from "+previewUrl+" did not respond with expected code, received "+statusCode, 200<=statusCode && statusCode<400);
        assertTrue("Expected HTML content type from preview document but received "+contentType, contentType.startsWith("text/html"));
	}


	@Test
	public void getResourceUsingInvalidContentType() throws IOException {
		HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, "invalid/content-type",
				headers);
		String respType =  OSLCUtils.getContentType(resp);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected 406 but received "+resp.getStatusLine()+", requested Content-type='invalid/content-type' but received "+respType, resp.getStatusLine().getStatusCode() == 406 || respType.contains("invalid/content-type"));
	}

}
