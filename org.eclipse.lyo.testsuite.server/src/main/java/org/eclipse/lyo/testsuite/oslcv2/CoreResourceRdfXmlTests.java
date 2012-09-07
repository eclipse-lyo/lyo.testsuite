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
 *    Yuhong Yin - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.eclipse.lyo.testsuite.server.util.RDFUtils;
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
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This class provides JUnit tests with JSON format for the validation of an OSLC core resource.
 * 
 */
@RunWith(Parameterized.class)
public abstract class CoreResourceRdfXmlTests  extends TestsBase {
	private HttpResponse response;
	private Model fRdfModel = ModelFactory.createDefaultModel();
	private Resource fResource = null;

	protected static String resourceTypeQuery = "";
	protected static String xpathSubStmt = "";
	
	public CoreResourceRdfXmlTests(String thisUrl) 
		throws IOException,ParserConfigurationException, SAXException, XPathExpressionException, NullPointerException {
		
		super(thisUrl);
		
		// If currentUrl is null, it means that the query didn't match any
		// records. This isn't exactly a failure, but there's nothing more we
		// can test.
		assumeNotNull(currentUrl);
        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, basicCreds, OSLCConstants.CT_RDF,
        		headers);
		// Some records in the system might not be accessible to this user. This
		// isn't a failure, but there's nothing more we can test.
        int sc = response.getStatusLine().getStatusCode();
        assumeTrue(sc != HttpStatus.SC_FORBIDDEN && sc != HttpStatus.SC_UNAUTHORIZED);
        // Make sure the request succeeded before continuing.
        assertEquals(HttpStatus.SC_OK, sc);

		fRdfModel.read(response.getEntity().getContent(),
				OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
				OSLCConstants.JENA_RDF_XML);
		RDFUtils.validateModel(fRdfModel);
		
		fResource = (Resource) fRdfModel.getResource(currentUrl);
		assumeTrue(fRdfModel.contains(fResource, RDF.type,
				        				fRdfModel.createResource(OSLCConstants.CM_CHANGE_REQUEST_TYPE)));

	}

	@Parameters
	protected static Collection<Object[]> getAllDescriptionUrls(String eval) throws IOException
	{
		ArrayList<String> results = new ArrayList<String>();
	
		String useThisCR = setupProps.getProperty("useThisChangeRequest");
		if ( useThisCR != null ) {
			results.add(useThisCR);
			return toCollection(results);
		}
		
		//Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order to grab all urls
		//to other ServiceProvidersCatalogs contained within it, recursively, in order to find the URLs of all
		//query factories of the REST service.
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
		
		ArrayList<String> capabilityURLsUsingRdfXml = TestsBase.getCapabilityURLsUsingRdfXml(
				OSLCConstants.QUERY_BASE_PROP, 
				serviceUrls, 
				true,
				null, 
				getResourceTypeQuery(),
				getxpathSubStmt());
		
		String where = setupProps.getProperty("changeRequestsWhere");
		if (where == null) {
			String queryProperty = setupProps.getProperty("queryEqualityProperty");
			String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
			where = queryProperty + "=\"" + queryPropertyValue + "\"";
		}

		String additionalParameters = setupProps.getProperty("queryAdditionalParameters");
		String query = (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&"; 
		query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";

		for (String queryBaseUri : capabilityURLsUsingRdfXml) {
			String queryUrl = OSLCUtils.addQueryStringToURL(queryBaseUri, query);
			HttpResponse resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, basicCreds, 
					OSLCConstants.CT_RDF, headers);
			Model queryModel = ModelFactory.createDefaultModel();
			queryModel.read(resp.getEntity().getContent(), queryBaseUri, OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(queryModel);
            	
            Property member = queryModel.createProperty(eval);
            
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

	@Before
	public void setup() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		super.setup();
	}

	@Test
	public void CoreResourceHasOneTitle()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_TITLE_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have 1 dc:title, found "+size, size == 1); 
	}
	
	@Test
	public void CoreResourceHasAtMostOneDescription()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_DESC_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 dc:description, found "+size, size <= 1); 
	}
	
	@Test
	public void CoreResourceHasAtMostOneIdentifier()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_ID_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 dc:identifier, found "+size, size <= 1); 
	}
	
	@Test
	public void CoreResourceHasAtMostOneName()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_NAME_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 dc:name, found "+size, size <= 1); 
	}
	
	@Test
	public void CoreResourceHasAtMostOneCreatedDate()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_CREATED_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 dc:created, found "+size, size <= 1);
	}
	
	@Test
	public void CoreResourceHasAtMostOneModifiedDate()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_MODIFIED_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 dc:modified, found "+size, size <= 1);
	}
	
	@Test
	public void CoreResourceHasAtMostOneDiscussion()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.DISCUSSION_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 oslc:discussion, found "+size, size <= 1);
	}
	
	@Test
	public void CoreResourceHasAtMostOneInstanceShape()
	{
		StmtIterator listStatements = getStatementsForProp(OSLCConstants.INST_SHAPE_PROP);
		int size=listStatements.toList().size();
		assertTrue("Can have <=1 oslc:instanceShape, found "+size, size <= 1);
	}
	
	protected StmtIterator getStatementsForProp(String propUri) {
		Property prop = fRdfModel.getProperty(propUri);
		Selector select = new SimpleSelector(fResource, prop, (RDFNode)null);
		return fRdfModel.listStatements(select);
	}
	
	protected static void setResourceTypeQuery (String rT) {
		resourceTypeQuery = rT;
	}
	
	protected static String getResourceTypeQuery() {
		return resourceTypeQuery;
	}

	protected static void setxpathSubStmt (String x) {
		xpathSubStmt = x;
	}

	protected static String getxpathSubStmt() {
		return xpathSubStmt;
	}
}
