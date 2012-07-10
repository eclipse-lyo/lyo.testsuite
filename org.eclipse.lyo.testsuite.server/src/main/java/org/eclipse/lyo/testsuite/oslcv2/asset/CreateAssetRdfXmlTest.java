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

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
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
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@RunWith(Parameterized.class)
public class CreateAssetRdfXmlTest extends CreateAssetBase {
	private String baseUrl;

	public CreateAssetRdfXmlTest(String url) {
		super(url, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);
	}
		
	@Before
	public void setup()
		throws IOException, ParserConfigurationException, SAXException, XPathException 	{
		super.setup();
		baseUrl = setupProps.getProperty("baseUrl");
	}
	
	@Test
	public void createSimpleAsset() throws IOException {
		assetUrl = createAsset(rdfXmlCreateTemplate);
	}
	
	@Test
	public void createAssetWithCategory() throws IOException {
		String file = readFileFromProperty("createWithCategoryTemplateRdfXmlFile");
		if (file == null) // Fall back to the xml if the rdf is not defined
			file = readFileFromProperty("createWithCategoryTemplateXmlFile");
		
		assetUrl = createAsset(file);
		HttpResponse resp = getAssetResponse();
		
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		Property property = model.getProperty(OSLCConstants.ASSET_CATEGORIZATION_PROP);
		Selector select = new SimpleSelector(null, property, (RDFNode)null);
		StmtIterator statements = model.listStatements(select);
		assertTrue("The category was not set", statements.hasNext());
	}
	
	@Test
	public void createAssetWithRelationship() throws IOException {
		HttpResponse resp = null;
		String otherUrl = null;
		try{
			otherUrl = createAsset(rdfXmlCreateTemplate);
			String file = readFileFromProperty("createWithRelationshipTemplateRdfXmlFile");
			if (file == null) // Fall back to the xml if the rdf is not defined
				file = readFileFromProperty("createWithRelationshipTemplateXmlFile");
			
			String asset = file.replace("%s", otherUrl);
			assetUrl = createAsset(asset);
			resp = getAssetResponse();
			
			Model model = ModelFactory.createDefaultModel();
			model.read(resp.getEntity().getContent(), baseUrl);
			EntityUtils.consume(resp.getEntity());
			
			Property property = model.getProperty(OSLCConstants.DC_RELATION_PROP);
			Selector select = new SimpleSelector(null, property, (RDFNode)null);
			StmtIterator statements = model.listStatements(select);
			
			assertTrue("The relation was not created", statements.hasNext());
		} finally {
			resp = OSLCUtils.deleteFromUrl(otherUrl, basicCreds, acceptType);
			EntityUtils.consume(resp.getEntity());
		}
	}
	
	@Test
	public void deletingAsset() throws IOException
	{
		deletingAsset(rdfXmlCreateTemplate);
	}
}
