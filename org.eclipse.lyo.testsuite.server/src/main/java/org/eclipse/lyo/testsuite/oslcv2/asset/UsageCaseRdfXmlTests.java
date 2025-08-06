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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class UsageCaseRdfXmlTests extends UsageCaseBase {
    private String baseUrl;
    private static Resource bestAsset = null;

    public UsageCaseRdfXmlTests(String thisUrl) {
        super(thisUrl, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);
    }

    @Before
    public void setUp() {
        baseUrl = setupProps.getProperty("baseUrl");
    }

    @Test
    public void queryUsageTest() throws IOException, ParserConfigurationException, SAXException {
        Model model = runQuery();
        bestAsset = getBestAsset(model);
        assertTrue("The asset with the highest version couldn't be found", bestAsset != null);
    }

    @Test
    public void retrieveUsageCase() throws IOException, ParserConfigurationException, SAXException {
        assertTrue("The asset with the highest version couldn't be found", bestAsset != null);

        // Once the best asset is determined then the full asset is retrieved
        Model model = ModelFactory.createDefaultModel();
        Property versionProperty = model.getProperty(OSLCConstants.ASSET_VERSION_PROP);
        Statement urlStatement = bestAsset.getProperty(versionProperty);
        assetUrl = urlStatement.getSubject().toString();
        Response resp = getAssetResponse();
        assetUrl = null; // This is required so that the asset is not deleted
        retrieveArtifact(resp);
    }

    @Test
    public void publishUsageCase() throws IllegalStateException, IOException {
        // Get url
        ArrayList<String> serviceUrls =
                getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
        ArrayList<String> capabilityURLsUsingRdfXml =
                TestsBase.getCapabilityURLsUsingRdfXml(
                        OSLCConstants.CREATION_PROP, serviceUrls, useDefaultUsageForCreation, null);
        currentUrl = capabilityURLsUsingRdfXml.getFirst();

        // Creates the asset
        assetUrl = createAsset(rdfXmlCreateTemplate);
        assertTrue(
                "The location of the asset after it was create was not returned", assetUrl != null);
        baseUrl = setupProps.getProperty("baseUrl");

        Response resp = getAssetResponse();

        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        // Gets the artifact factory from the asset
        String artifactFactory = getPropertyValue(model, OSLCConstants.ASSET_ARTIFACT_FACTORY_PROP);
        assertTrue(
                "There needs to be an artifact factory",
                artifactFactory != null && artifactFactory.length() > 0);
        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        // Creates the artifact
        String fileName = setupProps.getProperty("createTemplateArtifactRdfXmlFile");
        if (fileName == null) // Fall back to the xml if the rdf is not defined
            // fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
            fileName = setupProps.getProperty("createTemplateXmlFile");

        assertTrue("There needs to be an artifact template file", fileName != null);
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        resp =
                OSLCUtils.postDataToUrl(
                        artifactFactory, creds, OSLCConstants.CT_RDF, null, artifact, header);
        resp.close();
        assertTrue(
                "Expected: "
                        + Status.CREATED.getStatusCode()
                        + ", received: "
                        + resp.getStatus(),
                Status.CREATED.getStatusCode() == resp.getStatus());

        // Get and updates the artifacts subject
        resp = getAssetResponse();

        model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        // TODO make this so that if the label is not there it is added
        Property artifactProp = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
        String labelValue = "this subject has been changed";
        StmtIterator artifactStatements = model.listStatements(null, artifactProp, (RDFNode) null);
        List<Statement> statementList = artifactStatements.toList();
        for (int i = 0; i < statementList.size(); i++) {
            Statement artifactStatement = statementList.get(i);
            Property prop = model.createProperty(OSLCConstants.LABEL_PROP);
            setPropertyValue(artifactStatement, prop, labelValue);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        model.write(output);
        String content = output.toString();
        putAsset(content);

        // Checks the validity of the put
        resp = getAssetResponse();
        model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();
        artifactStatements = model.listStatements(null, artifactProp, (RDFNode) null);
        statementList = artifactStatements.toList();
        for (int i = 0; i < statementList.size(); i++) {
            Statement artifactStatement = statementList.get(i);
            Property prop = model.createProperty(OSLCConstants.LABEL_PROP);
            setPropertyValue(artifactStatement, prop, labelValue);
            StmtIterator statements = artifactStatement.getResource().listProperties(prop);
            assertTrue("No label property was found", statements.hasNext());
            assertEquals(labelValue, statements.next().getObject().toString());
        }
    }

    private Model runQuery() throws IOException, ParserConfigurationException, SAXException {
        Response resp = executeQuery();
        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();
        assertTrue(
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
        return model;
    }

    private Resource getBestAsset(Model model) {
        Property versionProperty = model.getProperty(OSLCConstants.ASSET_VERSION_PROP);
        ResIterator iterator = model.listResourcesWithProperty(versionProperty);
        String highestVersion = "";
        Resource bestAsset = null;
        while (iterator.hasNext()) {
            Resource resource = iterator.nextResource();
            Statement version = resource.getProperty(versionProperty);
            String value = version.getObject().toString();
            if (value.compareTo(highestVersion) > 0) {
                bestAsset = resource;
                highestVersion = value;
            }
        }
        return bestAsset;
    }

    private void retrieveArtifact(Response resp) throws IllegalStateException, IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();
        assertTrue(
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());

        Property property = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
        String artifactUrl = null;
        while (statements.hasNext()) {
            Statement artifactStatement = statements.nextStatement();
            Property content = model.createProperty(OSLCConstants.OSLC_ASSET_V2, "content");
            StmtIterator contentStatements = model.listStatements(
                            artifactStatement.getObject().asResource(), content, (RDFNode) null);
            while (contentStatements.hasNext()) {
                Statement contentStatement = contentStatements.nextStatement();
                artifactUrl = contentStatement.getObject().toString();
                break;
            }
            break;
        }
        assertTrue("No artifact could be found in the asset", artifactUrl != null);

        resp = OSLCUtils.getDataFromUrl(artifactUrl, creds, acceptType, contentType, headers);
        resp.close();
        assertTrue(
                "Expected "
                        + Response.Status.OK.getStatusCode()
                        + ", received "
                        + resp.getStatus(),
                resp.getStatus() == Response.Status.OK.getStatusCode());
    }

    private String getPropertyValue(Model model, String uri) {
        Property property = model.getProperty(uri);
        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
        while (statements.hasNext()) {
            Statement statement = statements.next();
            return statement.getObject().toString();
        }
        return null;
    }

    private void setPropertyValue(Statement statement, Property property, String newValue) {
        StmtIterator statements = statement.getResource().listProperties(property);
        ArrayList<Statement> statementList = new ArrayList<Statement>();
        // Converts the iterator into an array list so that the statement(s) can be modified
        while (statements.hasNext()) {
            statementList.add(statements.nextStatement());
        }

        for (int i = 0; i < statementList.size(); i++) {
            statement = statementList.get(i);
            statement.changeObject(newValue);
        }
    }
}
