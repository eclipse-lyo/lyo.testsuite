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
import java.io.InputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CreateAssetRdfXmlTest extends CreateAssetBase {

    public CreateAssetRdfXmlTest(String url) {
        super(url, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);
    }

    @Test
    public void createSimpleAsset() throws IOException {
        assetUrl = createAsset(rdfXmlCreateTemplate);
    }

    @Test
    public void createAssetWithCategory() throws IOException {
        String file = readFileFromProperty("createWithCategoryTemplateRdfXmlFile");
        if (file == null) // Fall back to the xml if the rdf is not defined
        file = readFileFromProperty("createWithCategoryTemplateXmlFile");

        assetUrl = createAsset(file);
        Response resp = getAssetResponse();

        Model model = ModelFactory.createDefaultModel();
        model.read(resp.readEntity(InputStream.class), baseUrl);
        resp.close();

        Property property = model.getProperty(OSLCConstants.ASSET_CATEGORIZATION_PROP);
        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
        assertTrue(statements.hasNext(), "The category was not set");
    }

    @Test
    public void createAssetWithRelationship() throws IOException {
        Response resp = null;
        String otherUrl = null;
        try {
            otherUrl = createAsset(rdfXmlCreateTemplate);
            String file = readFileFromProperty("createWithRelationshipTemplateRdfXmlFile");
            if (file == null) // Fall back to the xml if the rdf is not defined
            file = readFileFromProperty("createWithRelationshipTemplateXmlFile");

            String asset = file.replace("%s", otherUrl);
            assetUrl = createAsset(asset);
            resp = getAssetResponse();

            Model model = ModelFactory.createDefaultModel();
            model.read(resp.readEntity(InputStream.class), baseUrl);
            resp.close();

            Property property = model.getProperty(OSLCConstants.DC_RELATION_PROP);
            StmtIterator statements = model.listStatements(null, property, (RDFNode) null);

            assertTrue(statements.hasNext(), "The relation was not created");
        } finally {
            resp = OSLCUtils.deleteFromUrl(otherUrl, creds, acceptType);
            resp.close();
        }
    }

    @Test
    public void deletingAsset() throws IOException {
        deletingAsset(rdfXmlCreateTemplate);
    }
}
