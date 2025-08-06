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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import jakarta.ws.rs.core.Response;
import org.apache.http.ParseException;
import java.util.HashMap;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
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
public class UsageCaseXmlTests extends UsageCaseBase {
    private static Node bestAsset = null;

    public UsageCaseXmlTests(String thisUrl) {
        super(thisUrl, OSLCConstants.CT_XML, OSLCConstants.CT_XML);
    }

    @Test
    public void queryUsageCase()
            throws IOException,
                    ParseException,
                    ParserConfigurationException,
                    SAXException,
                    TransformerException,
                    XPathExpressionException {
        // Runs a query to get a bunch of assets by their name
        Document document = runQuery();
        // Selects the asset with the best version
        bestAsset = getBestAsset(document);
        assertTrue("The asset with the highest version couldn't be found", bestAsset != null);
    }

    @Test
    public void retrieveUsageCase()
            throws IOException,
                    ParseException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {
        assertTrue("The asset with the highest version couldn't be found", bestAsset != null);

        // Once the best asset is determined then the full asset is retrieved
        NamedNodeMap attributes = bestAsset.getAttributes();
        assetUrl = attributes.getNamedItem("rdf:about").getNodeValue();
        String asset = getAssetAsString();
        assetUrl = null; // This is required so that the asset is not deleted
        retrieveArtifact(asset);
    }

    @Test
    public void publishUsageCase()
            throws IOException,
                    ParseException,
                    ParserConfigurationException,
                    SAXException,
                    TransformerException,
                    XPathException {
        // Get url
        ArrayList<String> serviceUrls =
                getServiceProviderURLsUsingXML(setupProps.getProperty("baseUri"));
        ArrayList<String> capabilityURLsUsingRdfXml =
                TestsBase.getCapabilityURLsUsingRdfXml(
                        OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, null);
        currentUrl = capabilityURLsUsingRdfXml.getFirst();

        // Create the asset
        assetUrl = createAsset(xmlCreateTemplate);
        assertTrue(
                "The location of the asset after it was create was not returned", assetUrl != null);

        // Add the artifact to the asset
        String artifactFactory = getArtifactFactory();

        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        // String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        String fileName = setupProps.getProperty("createTemplateXmlFile");
        assertTrue("There needs to be an artifact template file", fileName != null);
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        Response response =
                OSLCUtils.postDataToUrl(
                        artifactFactory,
                        creds,
                        OSLCConstants.CT_XML,
                        OSLCConstants.CT_XML,
                        artifact,
                        header);
        response.close();
        assertTrue(
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + response.getStatus(),
                response.getStatus() == Status.CREATED.getStatusCode());

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
        assertTrue("Could not find the artifact's label node", label != null);
        assertEquals("The label was not updated properly", labelValue, label.getTextContent());
    }

    private Document runQuery() throws IOException, ParserConfigurationException, SAXException {
        Response resp = executeQuery();
        Document document =
                OSLCUtils.createXMLDocFromResponseBody(resp.readEntity(String.class));
        resp.close();
        assertTrue(
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
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
            throws ParserConfigurationException,
                    IOException,
                    SAXException,
                    XPathExpressionException {
        Document document = OSLCUtils.createXMLDocFromResponseBody(asset);
        String path =
                "/rdf:RDF/oslc_asset:Asset/oslc_asset:artifact[1]/oslc_asset:Artifact/oslc_asset:content";
        XPath xpath = OSLCUtils.getXPath();
        Node content = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
        assertTrue("Could not find the artifact", content != null);

        NamedNodeMap attributes = content.getAttributes();
        String artifactUrl = attributes.getNamedItem("rdf:resource").getNodeValue();
        assertTrue("No artifact could be found in the asset", artifactUrl != null);

        Response resp =
                OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
        resp.close();
        assertTrue(
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
    }

    private String getArtifactFactory()
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {
        String resp = getAssetAsString();
        Document document = OSLCUtils.createXMLDocFromResponseBody(resp);

        // Gets the artifact factory from the asset
        NodeList nodes = getAssetNodeChildren(document);
        String artifactFactory =
                getNodeAttribute(nodes, "oslc_asset:artifactFactory", "rdf:resource");
        assertTrue(
                "There needs to be an artifact factory",
                artifactFactory != null && artifactFactory.length() > 0);
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
