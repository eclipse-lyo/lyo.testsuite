/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
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
 *    Wu Kai
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class InvalidateOSLCPropertiesXmlTest extends InvalidateOSLCPropertiesTestBase {

	public InvalidateOSLCPropertiesXmlTest(String url) throws IOException {
		super(url, OSLCConstants.CT_XML, OSLCConstants.CT_XML);
		
		assetUrl = createAsset(xmlCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
	}

	@Override
	protected void queryInvalidOSLCProperties(String properties) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
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

		// Put invalidate properties
		HttpResponse response = putAssetProperties(properties, content);
		assertEquals(HttpStatus.SC_CONFLICT, response.getStatusLine().getStatusCode());
	}

	private NodeList getAssetNodeChildren(Document document) throws XPathExpressionException {
		String path = "/rdf:RDF/oslc_asset:Asset";
		XPath xpath = OSLCUtils.getXPath();
		Node asset = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
		return asset.getChildNodes();
	}
}
