/*******************************************************************************
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
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.core.Response;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;
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
        assertTrue("The category was not set", statements.hasNext());
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

            assertTrue("The relation was not created", statements.hasNext());
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
