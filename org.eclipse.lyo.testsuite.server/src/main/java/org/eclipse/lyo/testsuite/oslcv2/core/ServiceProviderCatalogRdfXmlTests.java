/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation.
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
 *    Steve Speicher
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

public class ServiceProviderCatalogRdfXmlTests extends ServiceProviderCatalogBaseTests {

    private Logger logger = Logger.getLogger(ServiceProviderCatalogRdfXmlTests.class);
    private Model rdfModel = ModelFactory.createDefaultModel();
    private Resource catalog = null;
    private HttpResponse response = null;

    public ServiceProviderCatalogRdfXmlTests(String thisUrl) throws IOException {
        super(thisUrl);

        fContentType = OSLCConstants.CT_RDF;

        response =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, currentUrl, creds, fContentType, headers);

        assertEquals(
                "Did not successfully retrieve catalog at: " + currentUrl,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

        rdfModel.read(
                response.getEntity().getContent(),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(rdfModel);
        catalog = (Resource) rdfModel.getResource(currentUrl);

        assertNotNull("Failed to read Catalog resource at URI: " + currentUrl, catalog);
    }

    @Parameters
    public static Collection<Object[]> getAllServiceProviderCatalogUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProviders contained within it, recursively.
        staticSetup();

        Collection<Object[]> coll =
                getReferencedCatalogUrlsUsingRdfXml(setupProps.getProperty("baseUri"));
        return coll;
    }

    public static Collection<Object[]> getReferencedCatalogUrlsUsingRdfXml(String base)
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        staticSetup();

        // ArrayList to contain the urls from all SPCs
        Collection<Object[]> data = new ArrayList<Object[]>();

        HttpResponse resp =
                OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_RDF, headers);

        try {
            assertEquals(
                    "Did not successfully retrieve catalog at: " + base,
                    HttpStatus.SC_OK,
                    resp.getStatusLine().getStatusCode());

            // Add ourself (base)
            data.add(new Object[] {base});

            Model rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(resp.getEntity().getContent(), base, OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(rdfModel);

            Property catPredicate =
                    rdfModel.createProperty(OSLCConstants.SERVICE_PROVIDER_CATALOG_PROP);
            StmtIterator listStatements = rdfModel.listStatements(null, catPredicate, (RDFNode) null);
            while (listStatements.hasNext()) {
                data.add(new Object[] {listStatements.nextStatement().getObject().toString()});
            }

            return data;
        } finally {
            EntityUtils.consume(resp.getEntity());
        }
    }

    @Test
    public void baseUrlIsValid() throws IOException {
        // Get the status, make sure 200 OK
        assertTrue(
                response.getStatusLine().toString(),
                response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);

        // Verify we got a response
        assertNotNull("Failed to locate Catalog resource at URI: " + setupBaseUrl, catalog);
    }

    @Test
    public void catalogRootIsRdfNamespaceDeclaration() throws XPathException {
        Property rdfType = rdfModel.getProperty(OSLCConstants.RDF_TYPE_PROP);
        assertNotNull(rdfType);
        Statement property = catalog.getProperty(rdfType);
        assertNotNull(property);
        assertEquals(OSLCConstants.SERVICE_PROVIDER_CATALOG_TYPE, property.getObject().toString());
    }

    @Test
    public void catalogRootAboutElementPointsToSelf() throws XPathException, IOException {
        assertEquals(setupBaseUrl, catalog.getURI());
    }

    @Test
    public void serviceProviderCatalogsHaveAtMostOneTitle() throws XPathException {
        Property dcTitle = rdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
        Property catPredicate =
                rdfModel.createProperty(OSLCConstants.SERVICE_PROVIDER_CATALOG_PROP);
        StmtIterator listStatements = rdfModel.listStatements(null, catPredicate, (RDFNode) null);
        if (!listStatements.hasNext()) logger.debug("Catalog does not contain other catalogs");
        while (listStatements.hasNext()) {
            Resource cat = (Resource) listStatements.nextStatement().getObject();
            assertTrue(cat.listProperties(dcTitle).toList().size() <= 1);
        }
    }

    @Test
    public void serviceProviderCatalogHaveAtMostOneTitle() throws XPathException {
        Property dcTitle = rdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
        StmtIterator listStatements = rdfModel.listStatements(catalog, dcTitle, (RDFNode) null);
        assertTrue(listStatements.toList().size() <= 1);
    }

    /** Look for all oslc:ServiceProviders resources in catalog
     */
    protected StmtIterator getServiceProvidersFromModel() {
        Property spPredicate = rdfModel.createProperty(OSLCConstants.SERVICE_PROVIDER_PROP);
        return rdfModel.listStatements(null, spPredicate, (RDFNode) null);
    }

    @Test
    public void serviceProvidersHaveAtMostOneTitle() throws XPathException {
        Property dcTitle = rdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
        StmtIterator listStatements = getServiceProvidersFromModel();
        if (!listStatements.hasNext()) logger.debug("Catalog does not contain other catalogs");
        while (listStatements.hasNext()) {
            Resource cat = (Resource) listStatements.nextStatement().getObject();
            assertTrue(
                    "ServiceProviders have at most 1 dc:title",
                    cat.listProperties(dcTitle).toList().size() <= 1);
        }
    }

    @Test
    public void serviceProviderCatalogsHaveAtMostOnePublisher() throws XPathExpressionException {
        Property dcPublisher = rdfModel.createProperty(OSLCConstants.DC_PUBLISHER_PROP);
        StmtIterator listStatements = rdfModel.listStatements(catalog, dcPublisher, (RDFNode) null);
        assertTrue(
                "ServiceProviderCatalogs have at most 1 oslc:publisher",
                listStatements.toList().size() <= 1);
    }

    @Test
    public void serviceProvidersHaveAtMostOnePublisher() throws XPathExpressionException {
        Property dcPublisher = rdfModel.createProperty(OSLCConstants.DC_PUBLISHER_PROP);
        StmtIterator listStatements = getServiceProvidersFromModel();
        if (!listStatements.hasNext()) logger.debug("Catalog does not contain other catalogs");
        while (listStatements.hasNext()) {
            Resource cat = (Resource) listStatements.nextStatement().getObject();
            assertTrue(
                    "Service providers have at most 1 oslc:publisher",
                    cat.listProperties(dcPublisher).toList().size() <= 1);
        }
    }

    @Test
    public void publisherElementsAreValid() throws XPathExpressionException {
        Property dcPublisher = rdfModel.createProperty(OSLCConstants.DC_PUBLISHER_PROP);
        Property dcTitle = rdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
        StmtIterator listStatements = rdfModel.listStatements(null, dcPublisher, (RDFNode) null);
        if (!listStatements.hasNext())
            logger.debug("Catalog does not contain any Publishers resources");

        while (listStatements.hasNext()) {
            Resource pub = (Resource) listStatements.nextStatement().getObject();
            rdfModel.listObjectsOfProperty(pub, dcTitle);
        }
    }

    @Test
    public void misplacedParametersDoNotEffectResponse() throws IOException {
        Model baseRespModel = this.rdfModel;

        String badParmUrl = currentUrl + "?oslc_cm:query";

        HttpResponse parameterResp =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, badParmUrl, creds, fContentType, headers);
        assertEquals(
                "Did not successfully retrieve catalog at: " + badParmUrl,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

        Model badParmModel = ModelFactory.createDefaultModel();
        badParmModel.read(
                parameterResp.getEntity().getContent(),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, badParmUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(badParmModel);

        assertTrue(baseRespModel.isIsomorphicWith(badParmModel));
    }

    /* TODO: Complete ServiceProviderCatalog validation tests for RDF/XML

    @Test
    public void serviceProviderCatalogsHaveAtMostOneOAuthElement() throws XPathExpressionException
    {
    }

    @Test
    public void serviceProvidersHaveAtMostOneOAuthElement() throws XPathExpressionException
    {
    }

    @Test
    public void oAuthElementsAreValid() throws XPathExpressionException
    {
    }

    @Test
    public void serviceProviderCatalogsHaveValidResourceUrl() throws XPathException, IOException
    {
    }

    @Test
    public void servicesProvidersHaveValidResourceUrl() throws XPathException, IOException
    {
    }

    @Test
    public void detailsElementsHaveValidResourceAttribute() throws IOException, XPathException
    {
    }

    */
}
