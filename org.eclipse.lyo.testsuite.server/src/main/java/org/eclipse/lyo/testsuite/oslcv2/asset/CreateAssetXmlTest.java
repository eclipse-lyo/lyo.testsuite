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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class CreateAssetXmlTest extends CreateAssetBase {

	public CreateAssetXmlTest(String url) {
		super(url, OSLCConstants.CT_XML, OSLCConstants.CT_XML);
	}
		
	@Before
	public void setup()
		throws IOException, ParserConfigurationException, SAXException, XPathException 	{
		super.setup();
	}
	
	@Test
	public void createSimpleAsset() throws IOException {
		assetUrl = createAsset(xmlCreateTemplate);
	}
	
	@Test
	public void createAssetWithCategory()
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		assetUrl = createAsset(readFileFromProperty("createWithCategoryTemplateXmlFile"));
		String resp = getAssetAsString();
		Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
		NodeList children = getAssetNodeChildren(document);
		String cat = getNodeAttribute(children, "oslc_asset:categorization", "rdf:resource");
		assertTrue("Category was not set", cat != null);
	}

	@Test
	public void createAssetWithRelationship()
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		String otherUrl = null;
		try {
			otherUrl = createAsset(xmlCreateTemplate);
			String asset = readFileFromProperty("createWithRelationshipTemplateXmlFile").replace("%s", otherUrl);
			assetUrl = createAsset(asset);
			String resp = getAssetAsString();
			Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
			NodeList children = getAssetNodeChildren(document);
			String cat = getNodeAttribute(children, "dcterms:relation", "rdf:resource");
			assertTrue("Relationship was not set", cat != null);
		} finally {
			HttpResponse resp = OSLCUtils.deleteFromUrl(otherUrl, basicCreds, acceptType);
			EntityUtils.consume(resp.getEntity());
		}
	}
	
	@Test
	public void deletingAsset() throws IOException
	{
		deletingAsset(xmlCreateTemplate);
	}
	
	private NodeList getAssetNodeChildren(Document document) throws XPathExpressionException {
		String path = "/rdf:RDF/oslc_asset:Asset";
		XPath xpath = OSLCUtils.getXPath();
		Node asset = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
		return asset.getChildNodes();
	}
	
	private String getNodeAttribute(NodeList nodes, String nodeName, String attr) {
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeName().equals(nodeName)) {
				NamedNodeMap attributes = node.getAttributes();
				return attributes.getNamedItem(attr).getNodeValue();
			}
		}
		return null;
	}
}
