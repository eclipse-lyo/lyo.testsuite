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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class GetAndUpdateRdfXmlTests extends GetAndUpdateBase {
    private String baseUrl;
    private Model hasModel;

    public GetAndUpdateRdfXmlTests(String thisUrl) throws IOException {
        super(thisUrl, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);

        assetUrl = createAsset(rdfXmlCreateTemplate);
        assertTrue(assetUrl != null, "The location of the asset after it was create was not returned");
        baseUrl = setupProps.getProperty("baseUrl");

        Response resp = getAssetResponse();

        hasModel = ModelFactory.createDefaultModel();
        hasModel.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();
    }

    @Test
    public void assetHasAtMostOneModel() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.ASSET_MODEL_PROP));
    }

    @Test
    public void assetHasAtMostOneSerialNumber() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.ASSET_SERIAL_NUMBER_PROP));
    }

    @Test
    public void assetHasArtifactFactory() {
        assertTrue(
                getPropertyValue(hasModel, OSLCConstants.ASSET_ARTIFACT_FACTORY_PROP) != null,
                "Artifact Factory was not found");
    }

    @Test
    public void assetHasAtMostOneGuid() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.ASSET_GUID_PROP), "Multiple guids returned");
    }

    @Test
    public void assetHasAtMostOneVersion() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.ASSET_VERSION_PROP), "Multiple versions returned");
    }

    @Test
    public void assetHasAtMostOneAbstract() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.DC_ABSTRACT_PROP));
    }

    @Test
    public void assetHasAtMostOneType() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.DC_TYPE_PROP));
    }

    @Test
    public void assetHasAtMostOneState() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.ASSET_STATE_PROP));
    }

    @Test
    public void assetHasAtMostOneManufacturer() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.ASSET_MANUFACTURER_PROP));
    }

    @Test
    public void assetHasAtMostOneIdentifier() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.DC_ID_PROP));
    }

    @Test
    public void assetHasAtMostOneDescription() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.DC_DESC_PROP));
    }

    @Test
    public void assetHasTitle() {
        assertTrue(getPropertyValue(hasModel, OSLCConstants.DC_TITLE_PROP) != null, "Title was not found");
    }

    @Test
    public void assetHasAtMostOneCreatedDate() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.DC_CREATED_PROP));
    }

    @Test
    public void assetHasAtMostOneModifiedDate() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.DC_MODIFIED_PROP));
    }

    @Test
    public void assetHasAtMostOneInstanceShape() {
        assertTrue(isOneOrNone(hasModel, OSLCConstants.INST_SHAPE_PROP));
    }

    @Test
    public void updateAnAssetProperty() throws IOException {
        Response resp = getAssetResponse();

        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        // Updates the title
        String name = "updated asset";
        setPropertyValue(model, OSLCConstants.DC_TITLE_PROP, name);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        model.write(output);
        String content = output.toString();
        putAsset(content);

        resp = getAssetResponse();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        String actualName = getPropertyValue(model, OSLCConstants.DC_TITLE_PROP);
        assertTrue(name.equals(actualName), "Expected " + name + ", received " + actualName);
    }

    @Test
    public void addArtifactToAsset() throws IOException {
        String artifactFactory = getArtifactFactory();
        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactRdfXmlFile");
        if (fileName == null) // Fall back to the xml if the rdf is not defined
        fileName = setupProps.getProperty("createTemplateArtifactXmlFile");

        assertTrue(fileName != null, "There needs to be an artifact template file");
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        Response resp = OSLCUtils.postDataToUrl(artifactFactory, creds, OSLCConstants.CT_RDF, null, artifact, header);
        resp.close();
        assertTrue(
                Status.CREATED.getStatusCode() == resp.getStatus(),
                "Expected: " + Status.CREATED.getStatusCode() + ", received: " + resp.getStatus());
    }

    @Test
    public void uploadArtifact() throws IOException {
        String artifactFactory = getArtifactFactory();
        uploadArtifact(artifactFactory);
    }

    @Test
    public void downloadArtifact() throws IOException {
        String artifactFactory = getArtifactFactory();
        String location = uploadArtifact(artifactFactory);
        downloadArtifact(location);
    }

    @Test
    public void removeArtifactFromAsset() throws IOException {
        // Gets the artifact factory from the asset
        String artifactFactory = getArtifactFactory();

        var header = addHeader(null, Map.entry("oslc_asset.name", "/helpFolder/help"));

        String fileName = setupProps.getProperty("createTemplateArtifactXmlFile");
        if (fileName == null) // Fall back to the xml if the rdf is not defined
        fileName = setupProps.getProperty("createTemplateArtifactXmlFile");

        assertTrue(fileName != null, "There needs to be an artifact template file");
        String artifact = OSLCUtils.readFileByNameAsString(fileName);

        // Adds the artifact to the asset
        Response resp =
                OSLCUtils.postDataToUrl(artifactFactory, creds, OSLCConstants.CT_RDF, "text/xml", artifact, header);
        resp.close();

        // Gets the asset with the artifact added to it
        resp = getAssetResponse();

        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        // Removes the artifact from the asset
        Property property = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
        List<Statement> statementList = statements.toList();
        for (int i = 0; i < statementList.size(); i++) {
            Statement statement = statementList.get(i);
            StmtIterator childrenStatements =
                    model.listStatements(statement.getObject().asResource(), null, (RDFNode) null);
            model.remove(childrenStatements);
        }
        model.remove(statementList);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        model.write(output);
        String content = output.toString();
        putAsset(content);

        resp = getAssetResponse();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();
        property = model.getProperty(OSLCConstants.ASSET_ARTIFACT_PROP);
        statements = model.listStatements(null, property, (RDFNode) null);
        assertFalse(statements.hasNext(), "The artifact was no removed");
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

    /**
     * Determines if there is at most one of the properties found
     *
     * @return true is properties count <= 1, else false
     */
    private boolean isOneOrNone(Model model, String uri) {
        Property property = model.getProperty(uri);
        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
        return statements.toList().size() <= 1;
    }

    private void setPropertyValue(Model model, String uri, String newValue) {
        Property property = model.getProperty(uri);
        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
        ArrayList<Statement> statementList = new ArrayList<Statement>();
        // Converts the iterator into an array list so that the statement(s) can be modified
        while (statements.hasNext()) {
            statementList.add(statements.nextStatement());
        }

        for (int i = 0; i < statementList.size(); i++) {
            Statement statement = statementList.get(i);
            statement.changeObject(newValue);
        }
    }

    private String getArtifactFactory() throws IOException {
        // Gets the asset that the artifact will be added too
        Response resp = getAssetResponse();

        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        // Gets the artifact factory from the asset
        String artifactFactory = getPropertyValue(model, OSLCConstants.ASSET_ARTIFACT_FACTORY_PROP);
        assertTrue(artifactFactory != null && artifactFactory.length() > 0, "There needs to be an artifact factory");
        return artifactFactory;
    }
}
