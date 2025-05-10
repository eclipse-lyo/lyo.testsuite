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
 *    Steve Speicher - initial API and implementation
 *    Yuhong Yin
 *    Tori Santonil  - validate oslc:domain property on oslc:Service resource
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of OSLCv2 ServiceProvider documents
 *
 */
@RunWith(Parameterized.class)
public class ServiceProviderRdfXmlTests extends TestsBase {

    protected HttpResponse response;
    protected static String fContentType = OSLCConstants.CT_RDF;
    private Model fRdfModel = ModelFactory.createDefaultModel();
    private Resource fServiceProvider = null;

    public ServiceProviderRdfXmlTests(String url)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {
        super(url);

        response =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, currentUrl, creds, fContentType, headers);
        try {
            assertEquals(
                    "Did not successfully retrieve ServiceProvider at: " + currentUrl,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

            fRdfModel.read(
                    response.getEntity().getContent(),
                    OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                    OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(fRdfModel);
            fServiceProvider = (Resource) fRdfModel.getResource(currentUrl);

            assertNotNull(fServiceProvider);
        } finally {
            EntityUtils.consume(response.getEntity());
        }
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively, in order to find the
        // URLs of all
        // service description documents of the REST service.

        // Properties setupProps = SetupProperties.setup(null);

        staticSetup();

        Collection<Object[]> coll = getReferencedUrls(setupProps.getProperty("baseUri"));

        return coll;
    }

    public static Collection<Object[]> getReferencedUrls(String base) throws IOException {
        // ArrayList to contain the urls from all SPCs
        Collection<Object[]> data = new ArrayList<Object[]>();

        ArrayList<String> serviceURLs = TestsBase.getServiceProviderURLsUsingRdfXml(base, true);

        for (String serviceURL : serviceURLs) {
            data.add(new Object[] {serviceURL});
            if (onlyOnce) return data;
        }

        return data;
    }

    @Test
    public void currentUrlIsValid() {
        assertNotNull("Could not locate a service provider document", currentUrl);

        // Get the status, make sure 200 OK
        assertTrue(
                "Expected 200-Ok but received " + response.getStatusLine().toString(),
                response.getStatusLine().getStatusCode() == 200);
    }

    @Test
    public void rootAboutElementPointsToSelf() throws XPathException, IOException {
        assertEquals(currentUrl, fServiceProvider.getURI());
    }

    @Test
    public void typeIsServiceProvider() throws XPathException {
        Property rdfType = fRdfModel.createProperty(OSLCConstants.RDF_TYPE_PROP);
        StmtIterator iter = fServiceProvider.listProperties(rdfType);
        boolean matches = false;
        // Since resources can have multiple types, iterate over all
        while (iter.hasNext() && !matches) {
            Statement st = iter.nextStatement();
            matches = OSLCConstants.SERVICE_PROVIDER_TYPE.equals(st.getObject().toString());
        }
        assertTrue("Expected rdf:type=" + OSLCConstants.SERVICE_PROVIDER_TYPE, matches);
    }

    @Test
    @Ignore(
            "Neither HTTP/1.1 nor OSLC Core 2.0 REQUIRE a 406 Not Acceptable response. "
                    + "It doesn't appear to be mentioned in the OSLC 2.0 Core specification. "
                    + "This is a SHOULD per HTTP/1.1, but not a MUST. See "
                    + "http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1")
    public void invalidContentTypeGivesNotSupportedOPTIONAL() throws IOException {
        HttpResponse resp =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, currentUrl, creds, "invalid/content-type", headers);
        String respType =
                (resp.getEntity().getContentType() == null)
                        ? ""
                        : resp.getEntity().getContentType().getValue();
        EntityUtils.consume(resp.getEntity());
        assertTrue(
                "Expected 406 but received "
                        + resp.getStatusLine()
                        + ",Content-type='invalid/content-type' but received "
                        + respType,
                resp.getStatusLine().getStatusCode() == 406
                        || respType.contains("invalid/content-type"));
    }

    @Test
    public void responseContentTypeIsXML() throws IOException {
        HttpResponse resp =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, currentUrl, creds, fContentType, headers);
        // Make sure the response to this URL was of valid type
        EntityUtils.consume(resp.getEntity());
        String contentType = resp.getEntity().getContentType().getValue();
        String contentTypeSplit[] = contentType.split(";");
        contentType = contentTypeSplit[0];
        assertTrue(
                contentType.equalsIgnoreCase("application/xml")
                        || contentType.equalsIgnoreCase("application/rdf+xml")
                        || contentType.equalsIgnoreCase("text/xml"));
    }

    @Test
    public void misplacedParametersDoNotEffectResponse() throws IOException {
        Model baseRespModel = this.fRdfModel;
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
        RDFUtils.validateModel(fRdfModel);

        assertTrue(baseRespModel.isIsomorphicWith(badParmModel));
    }

    @Test
    public void serviceProviderHasAtMostOneTitle() {
        Property dcTitle = fRdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
        assertTrue(fServiceProvider.listProperties(dcTitle).toList().size() <= 1);
    }

    @Test
    public void eachServiceHasOneDomain() throws XPathExpressionException {
        Property domainProp = fRdfModel.createProperty(OSLCConstants.OSLC_V2, "domain");
        Property serviceProp = fRdfModel.createProperty(OSLCConstants.SERVICE_PROP);
        List<Statement> statements = fServiceProvider.listProperties(serviceProp).toList();
        boolean domainFound = false;
        for (Statement statement : statements) {
            Resource service = statement.getResource();
            List<Statement> domains = service.listProperties(domainProp).toList();
            // Make sure each service has one domain
            assertEquals(1, domains.size());
            // Make sure the domain is a resource with a URI
            assertTrue(domains.get(0).getObject().isURIResource());
            // Make sure one of the domains found in the services matches the test version
            if (domains.get(0).getResource().getNameSpace().equals(testVersion)) {
                domainFound = true;
            }
        }

        assertTrue("Domain " + testVersion + " not found for any Service resource", domainFound);
    }

    @Test
    public void serviceProviderHasService() {
        Property service = fRdfModel.createProperty(OSLCConstants.SERVICE_PROP);
        List<?> lst = fServiceProvider.listProperties(service).toList();
        assertTrue(lst.size() >= 1);
    }

    @Test
    public void serviceProviderHasAtMostOnePublisher() {
        Property dcPublisher = fRdfModel.createProperty(OSLCConstants.DC_PUBLISHER_PROP);
        assertTrue(fServiceProvider.listProperties(dcPublisher).toList().size() <= 1);
    }

    /* TODO: Complete ServiceProvider RDF/XML test validation

    @Test
    public void serviceProviderHasValidDetails()
    {
    }

    @Test
    public void prefixDefinitionsAreValid()
    {
    }


    @Test
    public void publisherElementsAreValid()
    {
    }

    @Test
    public void serviceProviderHasAtMostOneOAuthElement()
    {
    }

    @Test
    public void oAuthElementsAreValid()
    {
    }

    @Test
    public void creationFactoriesAreValid()
    {
    }

    @Test
    public void queryCapabilityBlocksAreValid()
    {
    }

    @Test
    public void dialogsAreValid()
    {
    }
    */
}
