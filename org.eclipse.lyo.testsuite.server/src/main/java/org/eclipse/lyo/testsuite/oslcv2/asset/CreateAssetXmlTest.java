/*
 * Copyright (c) 2012, 2014, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CreateAssetXmlTest extends CreateAssetBase {

    public CreateAssetXmlTest() {
        super(null, null, null);
    }

    protected void setup(String url) {

        currentUrl = url;
        acceptType = OSLCConstants.CT_XML;
        contentType = OSLCConstants.CT_XML;
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void createSimpleAsset(String thisUrl) throws IOException {
        setup(thisUrl);
        assetUrl = createAsset(xmlCreateTemplate);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void createAssetWithCategory(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        setup(thisUrl);
        assetUrl = createAsset(readFileFromProperty("createWithCategoryTemplateXmlFile"));
        String resp = getAssetAsString();
        Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
        NodeList children = getAssetNodeChildren(document);
        String cat = getNodeAttribute(children, "oslc_asset:categorization", "rdf:resource");
        assertTrue(cat != null, "Category was not set");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void createAssetWithRelationship(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        setup(thisUrl);
        String otherUrl = null;
        try {
            otherUrl = createAsset(xmlCreateTemplate);
            String asset = readFileFromProperty("createWithRelationshipTemplateXmlFile")
                    .replace("%s", otherUrl);
            assetUrl = createAsset(asset);
            String resp = getAssetAsString();
            Document document = OSLCUtils.createXMLDocFromResponseBody(resp);
            NodeList children = getAssetNodeChildren(document);
            String cat = getNodeAttribute(children, "dcterms:relation", "rdf:resource");
            assertTrue(cat != null, "Relationship was not set");
        } finally {
            Response resp = OSLCUtils.deleteFromUrl(otherUrl, creds, acceptType);
            resp.close();
        }
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void deletingAsset(String thisUrl) throws IOException {
        setup(thisUrl);
        deletingAsset(xmlCreateTemplate);
    }

    private NodeList getAssetNodeChildren(Document document) throws XPathExpressionException {
        String path = "/rdf:RDF/oslc_asset:Asset";
        XPath xpath = OSLCUtils.getXPath();
        Node asset = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
        return asset.getChildNodes();
    }

    private String getNodeAttribute(NodeList nodes, String nodeName, String attr) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(nodeName)) {
                NamedNodeMap attributes = node.getAttributes();
                return attributes.getNamedItem(attr).getNodeValue();
            }
        }
        return null;
    }
}
