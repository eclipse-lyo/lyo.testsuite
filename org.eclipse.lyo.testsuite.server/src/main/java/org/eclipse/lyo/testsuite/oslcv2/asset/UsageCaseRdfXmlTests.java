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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@RunWith(Parameterized.class)
public class UsageCaseRdfXmlTests extends UsageCaseBase {
	private String baseUrl;
	private static Resource bestAsset = null;
	
	public UsageCaseRdfXmlTests(String thisUrl) {
		super(thisUrl, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);
	}
	
	@Before
	public void setUp() {
		baseUrl = setupProps.getProperty("baseUrl");
	}
	
	@Test
	public void queryUsageTest() throws IOException, ParserConfigurationException, SAXException {
		Model model = runQuery();
		bestAsset = getBestAsset(model);
		assertTrue("The asset with the highest version couldn't be found", bestAsset != null);
	}
	
	@Test
	public void retrieveUsageCase() throws IOException, ParserConfigurationException, SAXException {
		assertTrue("The asset with the highest version couldn't be found", bestAsset != null);
		
		// Once the best asset is determined then the full asset is retrieved
		Model model = ModelFactory.createDefaultModel();
		Property versionProperty = model.getProperty(OSLCConstants.ASSET_VERSION_PROP);
		Statement urlStatement = bestAsset.getProperty(versionProperty);	
		assetUrl = urlStatement.getSubject().toString();
		HttpResponse resp = getAssetResponse();
		assetUrl = null; // This is required so that the asset is not deleted
		retrieveArtifact(resp);
	}
	
	@Test
	public void publishUsageCase() throws IllegalStateException, IOException {
		// Get url
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingRdfXml(
						setupProps.getProperty("baseUri"), onlyOnce);
		ArrayList<String> capabilityURLsUsingRdfXml = TestsBase.getCapabilityURLsUsingRdfXml(
						OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, null);
		currentUrl = capabilityURLsUsingRdfXml.get(0);
		
		// Creates the asset
		assetUrl = createAsset(rdfXmlCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
		baseUrl = setupProps.getProperty("baseUrl");
		
		HttpResponse resp = getAssetResponse();
		
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		// Gets the artifact factory from the asset
		String artifactFactory = getPropertyValue(model, OSLCConstants.ASSET_ARTIFACT_FACTORY_PROP);
		assertTrue("There needs to be an artifact factory",
				artifactFactory != null && artifactFactory.length() > 0);
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));
		
		// Creates the artifact
		String fileName = setupProps.getProperty("createTemplateArtifactRdfXmlFile");
		if (fileName == null) // Fall back to the xml if the rdf is not defined
			fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		resp = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
					OSLCConstants.CT_RDF, OSLCConstants.CT_RDF, artifact, header);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected: " + HttpStatus.SC_CREATED + ", received: " + resp.getStatusLine().getStatusCode(),
				HttpStatus.SC_CREATED == resp.getStatusLine().getStatusCode());
		
		// Get and updates the artifacts subject
		resp = getAssetResponse();
		
		model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		// TODO make this so that if the label is not there it is added
		Property artifactProp = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
		String labelValue = "this subject has been changed";
		Selector selectArtifact = new SimpleSelector(null, artifactProp, (RDFNode)null);
		StmtIterator artifactStatements = model.listStatements(selectArtifact);
		List<Statement> statementList = artifactStatements.toList();
		for(int i = 0; i < statementList.size(); i++) {
			Statement statement = statementList.get(i);
			Selector selectChildren = new SimpleSelector(statement.getObject().asResource() , null, (RDFNode)null);
			StmtIterator childrenStatements = model.listStatements(selectChildren);
			Property prop = model.createProperty(OSLCConstants.LABEL_PROP);
			setPropertyValue(childrenStatements.nextStatement(), prop, labelValue);
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		model.write(output);
		String content = output.toString();
		putAsset(content);

		// Checks the validity of the put
		resp = getAssetResponse();
		model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		selectArtifact = new SimpleSelector(null, artifactProp, (RDFNode)null);
		artifactStatements = model.listStatements(selectArtifact);
		statementList = artifactStatements.toList();
		for(int i = 0; i < statementList.size(); i++) {
			Statement statement = statementList.get(i);
			Selector selectChildren = new SimpleSelector(statement.getObject().asResource() , null, (RDFNode)null);
			StmtIterator childrenStatements = model.listStatements(selectChildren);
			Statement childStatement = childrenStatements.nextStatement();
			Property prop = model.createProperty(OSLCConstants.LABEL_PROP);
			StmtIterator statements = childStatement.getResource().listProperties(prop);
			assertTrue("No label was found", statements.hasNext());
			Statement label = statements.nextStatement();
			assertEquals("Label was not set", labelValue, label.getObject().toString());
		}
	}
	
	private Model runQuery() throws IOException, ParserConfigurationException, SAXException {
		HttpResponse resp = executeQuery();
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		return model;
	}
	
	private Resource getBestAsset(Model model) {
		Property versionProperty = model.getProperty(OSLCConstants.ASSET_VERSION_PROP);
		ResIterator iterator = model.listResourcesWithProperty(versionProperty);
		String highestVersion = "";
		Resource bestAsset = null;
		while(iterator.hasNext()) {
			Resource resource = iterator.nextResource();
			Statement version = resource.getProperty(versionProperty);
			String value = version.getObject().toString();
			if(value.compareTo(highestVersion) > 0) {
				bestAsset = resource;
				highestVersion = value;
			}
		}
		return bestAsset;
	}

	private void retrieveArtifact(HttpResponse resp) throws IllegalStateException, IOException {
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);

		Property property = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
		Selector select = new SimpleSelector(null, property, (RDFNode)null);
		StmtIterator statements = model.listStatements(select);
		String artifactUrl = null;
		while(statements.hasNext()){
			Statement artifactStatement = statements.nextStatement();
			Property content = model.createProperty(OSLCConstants.OSLC_ASSET_V2, "content");
			Selector selectContent = new SimpleSelector(artifactStatement.getObject().asResource() , content, (RDFNode)null);
			StmtIterator contentStatements = model.listStatements(selectContent);
			while(contentStatements.hasNext()) {
				Statement contentStatement = contentStatements.nextStatement();
				artifactUrl = contentStatement.getObject().toString();
				break;
			}
			break;
		}
		assertTrue("No artifact could be found in the asset", artifactUrl != null);

		resp = OSLCUtils.getDataFromUrl(artifactUrl, basicCreds, acceptType, contentType, headers);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected "+HttpStatus.SC_OK + ", received " + resp.getStatusLine().getStatusCode(),
				resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
	
	private String getPropertyValue(Model model, String uri) {
		Property property = model.getProperty(uri);
		Selector select = new SimpleSelector(null, property, (RDFNode)null);
		StmtIterator statements = model.listStatements(select);
		while(statements.hasNext()) {
			Statement statement = statements.next();
			return statement.getObject().toString();
		}
		return null;
	}
	
	private void setPropertyValue(Statement statement, Property property, String newValue) {
		StmtIterator statements = statement.getResource().listProperties(property);
		ArrayList<Statement> statementList = new ArrayList<Statement>();
		// Converts the iterator into an array list so that the statement(s) can be modified
		while(statements.hasNext()) {
			statementList.add(statements.nextStatement());
		}
		
		for(int i = 0; i < statementList.size(); i++) {
			statement = statementList.get(i);
			statement.changeObject(newValue);
		}
	}
}
