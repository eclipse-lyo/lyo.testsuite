/*
 * Copyright (c) 2012, 2014 IBM Corporation.
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
 */
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.apache.http.ParseException;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
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
    private Document hasDocument;

    public GetAndUpdateXmlTests(String thisUrl) throws IOException, ParserConfigurationException, SAXException {
        super(thisUrl, OSLCConstants.CT_XML, OSLCConstants.CT_XML);

        assetUrl = createAsset(xmlCreateTemplate);
        assertTrue("The location of the asset after it was create was not returned", assetUrl != null);

        String resp = getAssetAsString();
        hasDocument = OSLCUtils.createXMLDocFromResponseBody(resp);
    }

    @Test
    public void assetHasAtMostOneModel() {
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:model"));
    }

    @Test
    public void assetHasAtMostOneSerialNumber() {
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:serialNumber"));
    }

    @Test
    public void assetHasArtifactFactory() {
        assertTrue("Artifact Factory was not found", hasNode(hasDocument, "oslc_asset:artifactFactory"));
    }

    @Test
    public void assetHasAtMostOneGuid() {
        assertTrue("Multiple guids returned", isOneOrNone(hasDocument, "oslc_asset:guid"));
    }

    @Test
    public void assetHasAtMostOneVersion() {
        assertTrue("Multiple versions returned", isOneOrNone(hasDocument, "oslc_asset:version"));
    }

    @Test
    public void assetHasAtMostOneAbstract() {
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_ABSTRACT));
    }

    @Test
    public void assetHasAtMostOneType() {
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_TYPE));
    }

    @Test
    public void assetHasAtMostOneState() {
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:state"));
    }

    @Test
    public void assetHasAtMostOneManufacturer() {
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:manufacturer"));
    }

    @Test
    public void assetHasAtMostOneIdentifier() {
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_ID));
    }

    @Test
    public void assetHasAtMostOneDescription() {
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_DESC));
    }

    @Test
    public void assetHasTitle() {
        assertTrue("Title was not found", hasNode(hasDocument, OSLCConstants.DCTERMS_TITLE));
    }

    @Test
    public void assetHasAtMostOneCreatedDate() {
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_CREATED));
    }

    @Test
    public void assetHasAtMostOneModifiedDate() {
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_MODIFIED));
    }

    @Test
    public void assetHasAtMostOneInstanceShape() {
        assertTrue(isOneOrNone(hasDocument, "oslc:instanceShape"));
    }

    @Test
    public void updateAnAssetProperty()
            throws IOException, ParseException, ParserConfigurationException, SAXException, TransformerException,
                    XPathExpressionException {
        // Get the asset
        String resp = getAssetAsString();
        Document document = OSLCUtils.createXMLDocFromResponseBody(resp);

        // Updates the title
        String name = "updated asset";
        NodeList nodes = getAssetNodeChildren(document);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("dcterms:title")) {
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
        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        assertTrue("There needs to be an artifact template file", fileName != null);
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        Response response = OSLCUtils.postDataToUrl(
                artifactFactory, creds, OSLCConstants.CT_XML, OSLCConstants.CT_XML, artifact, header);
        response.close();
        assertTrue(
                "Expected " + Status.CREATED.getStatusCode() + ", received " + response.getStatus(),
                response.getStatus() == Status.CREATED.getStatusCode());
    }

    @Test
    public void uploadArtifact()
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        String artifactFactory = getArtifactFactory();
        uploadArtifact(artifactFactory);
    }

    @Test
    public void downloadArtifact()
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        String artifactFactory = getArtifactFactory();
        String location = uploadArtifact(artifactFactory);
        downloadArtifact(location);
    }

    @Test
    public void removeArtifactFromAsset()
            throws IOException, TransformerException, ParseException, ParserConfigurationException, SAXException,
                    XPathExpressionException {
        String artifactFactory = getArtifactFactory();

        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        assertTrue("There needs to be an artifact template file", fileName != null);
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        // Adds the artifact to the asset
        Response response = OSLCUtils.postDataToUrl(
                artifactFactory, creds, OSLCConstants.CT_XML, OSLCConstants.CT_XML, artifact, header);
        response.close();

        // Gets the asset with the artifact added to it
        String resp = getAssetAsString();
        Document document = OSLCUtils.createXMLDocFromResponseBody(resp);

        // Removes the artifact from the asset
        NodeList nodes = getAssetNodeChildren(document);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("oslc_asset:artifact")) {
                node.getParentNode().removeChild(node);
            }
        }

        String content = OSLCUtils.createStringFromXMLDoc(document);
        putAsset(content);
        resp = getAssetAsString();
        document = OSLCUtils.createXMLDocFromResponseBody(resp);

        // Tests to verify that the artifact was removed
        nodes = getAssetNodeChildren(document);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("oslc_asset:artifact")) {
                fail("The artifact was not removed");
            }
        }
    }

    private boolean hasNode(Document document, String tagName) {
        return document.getElementsByTagName(tagName).getLength() == 1;
    }

    private boolean isOneOrNone(Document document, String tagName) {
        return document.getElementsByTagName(tagName).getLength() <= 1;
    }

    private NodeList getAssetNodeChildren(Document document) throws XPathExpressionException {
        String path = "/rdf:RDF/oslc_asset:Asset";
        XPath xpath = OSLCUtils.getXPath();
        Node asset = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
        return asset.getChildNodes();
    }

    private String getNodeText(NodeList nodes, String nodeName) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(nodeName)) {
                return node.getTextContent();
            }
        }
        return null;
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

    private String getArtifactFactory()
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String resp = getAssetAsString();
        Document document = OSLCUtils.createXMLDocFromResponseBody(resp);

        // Gets the artifact factory from the asset
        NodeList nodes = getAssetNodeChildren(document);
        String artifactFactory = getNodeAttribute(nodes, "oslc_asset:artifactFactory", "rdf:resource");
        assertTrue("There needs to be an artifact factory", artifactFactory != null && artifactFactory.length() > 0);
        return artifactFactory;
    }
}
