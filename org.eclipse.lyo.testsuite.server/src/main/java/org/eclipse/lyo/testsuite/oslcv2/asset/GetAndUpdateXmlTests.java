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
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GetAndUpdateXmlTests extends GetAndUpdateBase {
    private Document hasDocument;

    public GetAndUpdateXmlTests() {
        super(null, null, null);
    }

    protected void setup(String thisUrl) throws IOException, ParserConfigurationException, SAXException {

        currentUrl = thisUrl;
        acceptType = OSLCConstants.CT_XML;
        contentType = OSLCConstants.CT_XML;

        assetUrl = createAsset(xmlCreateTemplate);
        assertTrue(assetUrl != null, "The location of the asset after it was create was not returned");

        String resp = getAssetAsString();
        hasDocument = OSLCUtils.createXMLDocFromResponseBody(resp);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneModel(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:model"));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneSerialNumber(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:serialNumber"));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasArtifactFactory(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(hasNode(hasDocument, "oslc_asset:artifactFactory"), "Artifact Factory was not found");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneGuid(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:guid"), "Multiple guids returned");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneVersion(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:version"), "Multiple versions returned");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneAbstract(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_ABSTRACT));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneType(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_TYPE));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneState(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:state"));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneManufacturer(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, "oslc_asset:manufacturer"));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneIdentifier(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_ID));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneDescription(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_DESC));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasTitle(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(hasNode(hasDocument, OSLCConstants.DCTERMS_TITLE), "Title was not found");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneCreatedDate(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_CREATED));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneModifiedDate(String thisUrl) throws Exception {
        setup(thisUrl);
        assertTrue(isOneOrNone(hasDocument, OSLCConstants.DCTERMS_MODIFIED));
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void assetHasAtMostOneInstanceShape(String thisUrl) throws Exception {
        setup(thisUrl);
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
        assertTrue(name.equals(actualName), "Expected " + name + ", received " + actualName);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void addArtifactToAsset(String thisUrl)
            throws IOException, ParseException, ParserConfigurationException, SAXException, XPathExpressionException {
        setup(thisUrl);
        String artifactFactory = getArtifactFactory();
        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        assertTrue(fileName != null, "There needs to be an artifact template file");
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        Response response = OSLCUtils.postDataToUrl(
                artifactFactory, creds, OSLCConstants.CT_XML, OSLCConstants.CT_XML, artifact, header);
        response.close();
        assertTrue(
                response.getStatus() == Status.CREATED.getStatusCode(),
                "Expected " + Status.CREATED.getStatusCode() + ", received " + response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void uploadArtifactTest(String thisUrl)
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        setup(thisUrl);
        String artifactFactory = getArtifactFactory();
        uploadArtifact(artifactFactory);
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void downloadArtifactTest(String thisUrl)
            throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        setup(thisUrl);
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
        assertTrue(fileName != null, "There needs to be an artifact template file");
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
        assertTrue(artifactFactory != null && artifactFactory.length() > 0, "There needs to be an artifact factory");
        return artifactFactory;
    }
}
