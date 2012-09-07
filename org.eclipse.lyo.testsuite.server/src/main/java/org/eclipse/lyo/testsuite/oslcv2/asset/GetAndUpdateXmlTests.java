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
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHeader;
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
public class GetAndUpdateXmlTests extends GetAndUpdateBase {

	public GetAndUpdateXmlTests(String thisUrl) {
		super(thisUrl, OSLCConstants.CT_XML, OSLCConstants.CT_XML);
	}
	
	@Before
	public void setup() 
		throws IOException, ParserConfigurationException, SAXException, XPathException {
		super.setup();
		
		assetUrl = createAsset(xmlCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
	}
	
	@Test
	public void updateAnAssetProperty() 
			throws IOException, ParseException, ParserConfigurationException, SAXException,
			TransformerException, XPathExpressionException {
		// Get the asset
		String resp = getAssetAsString();
		Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
		
		// Updates the title
		String name = "updated asset";
		NodeList nodes = getAssetNodeChildren(document);
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeName().equals("dcterms:title")) {
				node.setTextContent(name);
			}
		}
		String content = OSLCUtils.createStringFromXMLDoc(document);
		// Update the asset
		putAsset(content);
		
		// Get the asset again to verify it the asset was updated
		resp = getAssetAsString();
		document = OSLCUtils.createXMLDocFromResponseBody(resp);
		
		NodeList children = getAssetNodeChildren(document);
		String actualName = getNodeText(children, "dcterms:title");
		assertTrue("Expected " + name + ", received " + actualName, name.equals(actualName));
	}
	
	@Test
	public void addArtifactToAsset() 
			throws IOException, ParseException, ParserConfigurationException, SAXException, XPathExpressionException {
		String artifactFactory = getArtifactFactory();
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));
		
		String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		HttpResponse response = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
					OSLCConstants.CT_XML, OSLCConstants.CT_XML, artifact, header);
		
		EntityUtils.consume(response.getEntity());
		assertTrue("Expected "+HttpStatus.SC_CREATED + ", received " + response.getStatusLine().getStatusCode(),
				response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED);
	}
	
	@Test
	public void uploadArtifact()
			throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
		String artifactFactory = getArtifactFactory();
		uploadArtifact(artifactFactory);
	}
	
	@Test
	public void downloadArtifact() throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
		String artifactFactory = getArtifactFactory();
		String location = uploadArtifact(artifactFactory);
		downloadArtifact(location);
	}
	
	@Test
	public void removeArtifactFromAsset()
			throws IOException, TransformerException, ParseException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		String artifactFactory = getArtifactFactory();
		
		Header[] header = addHeader(new BasicHeader("oslc_asset.name", "/helpFolder/help"));
		
		String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
		assertTrue("There needs to be an artifact template file", fileName != null);
		String artifact = OSLCUtils.readFileByNameAsString(fileName);

		// Adds the artifact to the asset
		HttpResponse response = OSLCUtils.postDataToUrl(artifactFactory,  basicCreds,
				OSLCConstants.CT_XML, OSLCConstants.CT_XML, artifact, header);
		EntityUtils.consume(response.getEntity());
		
		// Gets the asset with the artifact added to it
		String resp = getAssetAsString();
		Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
		
		// Removes the artifact from the asset
		NodeList nodes = getAssetNodeChildren(document);
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeName().equals("oslc_asset:artifact")) {
				node.getParentNode().removeChild(node);
			}
		}
		
		String content = OSLCUtils.createStringFromXMLDoc(document);
		putAsset(content);
		resp = getAssetAsString();
		document = OSLCUtils.createXMLDocFromResponseBody(resp);
		
		// Tests to verify that the artifact was removed
		nodes = getAssetNodeChildren(document);
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeName().equals("oslc_asset:artifact")) {
				fail("The artifact was not removed");
			}
		}
	}
	
	private NodeList getAssetNodeChildren(Document document) throws XPathExpressionException {
		String path = "/rdf:RDF/oslc_asset:Asset";
		XPath xpath = OSLCUtils.getXPath();
		Node asset = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
		return asset.getChildNodes();
	}
	
	private String getNodeText(NodeList nodes, String nodeName) {
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeName().equals(nodeName)) {
				return node.getTextContent();
			}
		}
		return null;
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
	
	private String getArtifactFactory() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		String resp = getAssetAsString();
		Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
		
		// Gets the artifact factory from the asset
		NodeList nodes = getAssetNodeChildren(document);
		String artifactFactory = getNodeAttribute(nodes, "oslc_asset:artifactFactory", "rdf:resource");
		assertTrue("There needs to be an artifact factory",
				artifactFactory != null && artifactFactory.length() > 0);
		return artifactFactory;
	}
}
