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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.apache.http.ParseException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
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

public class UsageCaseXmlTests extends UsageCaseBase {
    private static Node bestAsset = null;

    public UsageCaseXmlTests() {
        super(null, null, null);
    }

    protected void setup(String thisUrl) {

        currentUrl = thisUrl;
        acceptType = OSLCConstants.CT_XML;
        contentType = OSLCConstants.CT_XML;
    }

    @Test
    public void queryUsageCase()
            throws IOException, ParseException, ParserConfigurationException, SAXException, TransformerException,
                    XPathExpressionException {
        // Runs a query to get a bunch of assets by their name
        Document document = runQuery();
        // Selects the asset with the best version
        bestAsset = getBestAsset(document);
        assertTrue(bestAsset != null, "The asset with the highest version couldn't be found");
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void retrieveUsageCase(String thisUrl)
            throws IOException, ParseException, ParserConfigurationException, SAXException, XPathExpressionException {
        setup(thisUrl);
        assertTrue(bestAsset != null, "The asset with the highest version couldn't be found");

        // Once the best asset is determined then the full asset is retrieved
        NamedNodeMap attributes = bestAsset.getAttributes();
        assetUrl = attributes.getNamedItem("rdf:about").getNodeValue();
        String asset = getAssetAsString();
        assetUrl = null; // This is required so that the asset is not deleted
        retrieveArtifact(asset);
    }

    @Test
    public void publishUsageCase()
            throws IOException, ParseException, ParserConfigurationException, SAXException, TransformerException,
                    XPathException {
        // Get url
        ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(setupProps.getProperty("baseUri"));
        ArrayList<String> capabilityURLsUsingRdfXml = TestsBase.getCapabilityURLsUsingRdfXml(
                OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, null);
        currentUrl = capabilityURLsUsingRdfXml.getFirst();

        // Create the asset
        assetUrl = createAsset(xmlCreateTemplate);
        assertTrue(assetUrl != null, "The location of the asset after it was create was not returned");

        // Add the artifact to the asset
        String artifactFactory = getArtifactFactory();

        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        // String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        String fileName = setupProps.getProperty("createTemplateXmlFile");
        assertTrue(fileName != null, "There needs to be an artifact template file");
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        Response response = OSLCUtils.postDataToUrl(
                artifactFactory, creds, OSLCConstants.CT_XML, OSLCConstants.CT_XML, artifact, header);
        response.close();
        assertTrue(
                response.getStatus() == Status.CREATED.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + response.getStatus());

        // Get updated asset and update the artifact
        Response resp = getAssetResponse();
        String content = resp.readEntity(String.class);
        Document document = OSLCUtils.createXMLDocFromResponseBody(content);
        resp.close();
        String path = "/rdf:RDF/oslc_asset:Asset/oslc_asset:artifact[1]/oslc_asset:Artifact";
        XPath xpath = OSLCUtils.getXPath();
        Node artifactNode = (Node) xpath.evaluate(path, document, XPathConstants.NODE);

        NodeList artifactKids = artifactNode.getChildNodes();
        Node label = null;
        for (int i = 0; i < artifactKids.getLength(); i++) {
            if (artifactKids.item(i).getNodeName().equals("oslc:label")) {
                label = artifactKids.item(i);
                break;
            }
        }

        String labelValue = "this value was changed";
        if (label == null) {
            label = document.createElement("oslc:label");
            label.setTextContent(labelValue);
            artifactNode.appendChild(label);
        } else {
            label.setTextContent(labelValue);
        }

        // Update asset
        content = OSLCUtils.createStringFromXMLDoc(document);
        putAsset(content);

        // Check to see if the label was updated
        resp = getAssetResponse();
        content = resp.readEntity(String.class);
        document = OSLCUtils.createXMLDocFromResponseBody(content);
        resp.close();
        path = "/rdf:RDF/oslc_asset:Asset/oslc_asset:artifact[1]/oslc_asset:Artifact/oslc:label";
        xpath = OSLCUtils.getXPath();
        label = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
        assertTrue(label != null, "Could not find the artifact's label node");
        assertEquals(labelValue, label.getTextContent(), "The label was not updated properly");
    }

    private Document runQuery() throws IOException, ParserConfigurationException, SAXException {
        Response resp = executeQuery();
        Document document = OSLCUtils.createXMLDocFromResponseBody(resp.readEntity(String.class));
        resp.close();
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
        return document;
    }

    private Node getBestAsset(Document document) throws XPathExpressionException {

        String getAssets = "/rdf:RDF/oslc_asset:Asset";
        XPath xPath = OSLCUtils.getXPath();
        XPathExpression assetsExpr = xPath.compile(getAssets);
        NodeList assets = (NodeList) assetsExpr.evaluate(document, XPathConstants.NODESET);

        Node bestAsset = null;
        String highestVersion = "";
        for (int i = 0; i < assets.getLength(); i++) {
            NodeList nodeKids = assets.item(i).getChildNodes();
            for (int j = 0; j < nodeKids.getLength(); j++) {
                Node node = nodeKids.item(j);
                if (node.getNodeName().equals("oslc_asset:version")) {
                    String version = node.getTextContent();
                    if (version.compareTo(highestVersion) > 0) {
                        highestVersion = version;
                        bestAsset = assets.item(i);
                    }
                    break;
                }
            }
        }
        return bestAsset;
    }

    private void retrieveArtifact(String asset)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document document = OSLCUtils.createXMLDocFromResponseBody(asset);
        String path = "/rdf:RDF/oslc_asset:Asset/oslc_asset:artifact[1]/oslc_asset:Artifact/oslc_asset:content";
        XPath xpath = OSLCUtils.getXPath();
        Node content = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
        assertTrue(content != null, "Could not find the artifact");

        NamedNodeMap attributes = content.getAttributes();
        String artifactUrl = attributes.getNamedItem("rdf:resource").getNodeValue();
        assertTrue(artifactUrl != null, "No artifact could be found in the asset");

        Response resp = OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
        resp.close();
        assertTrue(
                resp.getStatus() == Response.Status.OK.getStatusCode(),
                "Expected " + Response.Status.OK.getStatusCode() + ", received " + resp.getStatus());
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
