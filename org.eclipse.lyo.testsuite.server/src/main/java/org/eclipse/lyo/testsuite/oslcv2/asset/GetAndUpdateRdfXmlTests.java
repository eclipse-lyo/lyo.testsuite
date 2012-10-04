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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@RunWith(Parameterized.class)
public class GetAndUpdateRdfXmlTests extends GetAndUpdateBase {
	private String baseUrl;
	
	public GetAndUpdateRdfXmlTests(String thisUrl) throws IOException {
		super(thisUrl, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);
		
		assetUrl = createAsset(rdfXmlCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
		baseUrl = setupProps.getProperty("baseUrl");
	}

	@Test
	public void updateAnAssetProperty() throws IOException {
		HttpResponse resp = getAssetResponse();
		
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		// Updates the title
		String name = "updated asset";
		setPropertyValue(model, OSLCConstants.DC_TITLE_PROP, name);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		model.write(output);
		String content = output.toString();
		putAsset(content);
		
		resp = getAssetResponse();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());

		String actualName = getPropertyValue(model, OSLCConstants.DC_TITLE_PROP);
		assertTrue("Expected " + name + ", received " + actualName, name.equals(actualName));
	}
	
	@Test
	public void addArtifactToAsset() throws IOException {
		String artifactFactory = getArtifactFactory();
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));
		
		String fileName = setupProps.getProperty("createTemplateArtifactRdfXmlFile");
		if (fileName == null) // Fall back to the xml if the rdf is not defined
			fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		HttpResponse resp = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
					OSLCConstants.CT_RDF, null, artifact, header);
		EntityUtils.consume(resp.getEntity());
		assertTrue("Expected: " + HttpStatus.SC_CREATED + ", received: " + resp.getStatusLine().getStatusCode(),
				HttpStatus.SC_CREATED == resp.getStatusLine().getStatusCode());
	}
	
	@Test
	public void uploadArtifact() throws IOException {
		String artifactFactory = getArtifactFactory();
		uploadArtifact(artifactFactory);
	}
	
	@Test
	public void downloadArtifact() throws IOException {
		String artifactFactory = getArtifactFactory();
		String location = uploadArtifact(artifactFactory);
		downloadArtifact(location);
	}
	
	@Test
	public void removeArtifactFromAsset() throws IOException {
		// Gets the artifact factory from the asset
		String artifactFactory = getArtifactFactory();		
		
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));
		
		String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		if (fileName == null) // Fall back to the xml if the rdf is not defined
			fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		// Adds the artifact to the asset
		HttpResponse resp = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
				OSLCConstants.CT_RDF, "text/xml", artifact, header);
		EntityUtils.consume(resp.getEntity());
		
		// Gets the asset with the artifact added to it
		resp = getAssetResponse();
		
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		// Removes the artifact from the asset
		Property property = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
		Selector select = new SimpleSelector(null, property, (RDFNode)null);
		StmtIterator statements = model.listStatements(select);
		List<Statement> statementList = statements.toList();
		for(int i = 0; i < statementList.size(); i++) {
			Statement statement = statementList.get(i);
			Selector selectChildren = new SimpleSelector(statement.getObject().asResource() , null, (RDFNode)null);
			StmtIterator childrenStatements = model.listStatements(selectChildren);
			model.remove(childrenStatements);
		}
		model.remove(statementList);
				
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		model.write(output);
		String content = output.toString();
		putAsset(content);
		
		resp = getAssetResponse();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		property = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
		select = new SimpleSelector(null, property, (RDFNode)null);
		statements = model.listStatements(select);
		assertFalse("The artifact was no removed", statements.hasNext());
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
	
	private void setPropertyValue(Model model, String uri, String newValue) {
		Property property = model.getProperty(uri);
		Selector select = new SimpleSelector(null, property, (RDFNode)null);
		StmtIterator statements = model.listStatements(select);
		ArrayList<Statement> statementList = new ArrayList<Statement>();
		// Converts the iterator into an array list so that the statement(s) can be modified
		while(statements.hasNext()) {
			statementList.add(statements.nextStatement());
		}
		
		for(int i = 0; i < statementList.size(); i++) {
			Statement statement = statementList.get(i);
			statement.changeObject(newValue);
		}
	}
	
	private String getArtifactFactory() throws IOException {
		// Gets the asset that the artifact will be added too
		HttpResponse resp = getAssetResponse();
		
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		// Gets the artifact factory from the asset
		String artifactFactory = getPropertyValue(model, OSLCConstants.ASSET_ARTIFACT_FACTORY_PROP);
		assertTrue("There needs to be an artifact factory",
				artifactFactory != null && artifactFactory.length() > 0);
		return artifactFactory;
	}
}
