/*
 * Copyright (c) 2011, 2014, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

/** This class provides JUnit tests for the validation of OSLCv2 ServiceProvider documents */
public class ServiceProviderRdfXmlTests extends TestsBase {

    protected Response response;
    protected static String fContentType = OSLCConstants.CT_RDF;
    private Model fRdfModel = ModelFactory.createDefaultModel();
    private Resource fServiceProvider = null;

    public void initServiceProviderRdfXmlTests(String url)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        setup(url);

        response = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);
        try {
            assertEquals(
                    Response.Status.OK.getStatusCode(),
                    response.getStatus(),
                    "Did not successfully retrieve ServiceProvider at: " + currentUrl);

            fRdfModel.read(
                    response.readEntity(InputStream.class),
                    OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                    OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(fRdfModel);
            fServiceProvider = (Resource) fRdfModel.getResource(currentUrl);

            assertNotNull(fServiceProvider);
        } finally {
            response.close();
        }
    }

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

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void currentUrlIsValid(String url) {
        initServiceProviderRdfXmlTests(url);
        assertNotNull(currentUrl, "Could not locate a service provider document");

        // Get the status, make sure 200 OK
        assertTrue(
                response.getStatus() == 200,
                "Expected 200-Ok but received " + response.getStatusInfo().getStatusCode());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void rootAboutElementPointsToSelf(String url) throws XPathException, IOException {
        initServiceProviderRdfXmlTests(url);
        assertEquals(currentUrl, fServiceProvider.getURI());
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void typeIsServiceProvider(String url) throws XPathException {
        initServiceProviderRdfXmlTests(url);
        Property rdfType = fRdfModel.createProperty(OSLCConstants.RDF_TYPE_PROP);
        StmtIterator iter = fServiceProvider.listProperties(rdfType);
        boolean matches = false;
        // Since resources can have multiple types, iterate over all
        while (iter.hasNext() && !matches) {
            Statement st = iter.nextStatement();
            matches = OSLCConstants.SERVICE_PROVIDER_TYPE.equals(st.getObject().toString());
        }
        assertTrue(matches, "Expected rdf:type=" + OSLCConstants.SERVICE_PROVIDER_TYPE);
    }

    @ParameterizedTest
    @Disabled("Neither HTTP/1.1 nor OSLC Core 2.0 REQUIRE a 406 Not Acceptable response. "
            + "It doesn't appear to be mentioned in the OSLC 2.0 Core specification. "
            + "This is a SHOULD per HTTP/1.1, but not a MUST. See "
            + "http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1")
    @MethodSource("getAllDescriptionUrls")
    public void invalidContentTypeGivesNotSupportedOPTIONAL(String url) throws IOException {
        initServiceProviderRdfXmlTests(url);
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, "invalid/content-type", headers);
        String respType = (resp.getHeaderString("Content-Type") == null) ? "" : resp.getHeaderString("Content-Type");
        resp.close();
        assertTrue(
                resp.getStatus() == 406 || respType.contains("invalid/content-type"),
                "Expected 406 but received "
                        + resp.getStatus()
                        + ",Content-type='invalid/content-type' but received "
                        + respType);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void responseContentTypeIsXML(String url) throws IOException {
        initServiceProviderRdfXmlTests(url);
        Response resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, currentUrl, creds, fContentType, headers);
        // Make sure the response to this URL was of valid type
        resp.close();
        String contentType = resp.getHeaderString("Content-Type");
        String contentTypeSplit[] = contentType.split(";");
        contentType = contentTypeSplit[0];
        assertTrue(contentType.equalsIgnoreCase("application/xml")
                || contentType.equalsIgnoreCase("application/rdf+xml")
                || contentType.equalsIgnoreCase("text/xml"));
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void misplacedParametersDoNotEffectResponse(String url) throws IOException {
        initServiceProviderRdfXmlTests(url);
        Model baseRespModel = this.fRdfModel;
        String badParmUrl = currentUrl + "?oslc_cm:query";

        var parameterResp = OSLCUtils.getResponseFromUrl(setupBaseUrl, badParmUrl, creds, fContentType, headers);
        assertEquals(
                Response.Status.OK.getStatusCode(),
                response.getStatus(),
                "Did not successfully retrieve catalog at: " + badParmUrl);

        Model badParmModel = ModelFactory.createDefaultModel();
        badParmModel.read(
                parameterResp.readEntity(InputStream.class),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, badParmUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(fRdfModel);

        assertTrue(baseRespModel.isIsomorphicWith(badParmModel));
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasAtMostOneTitle(String url) {
        initServiceProviderRdfXmlTests(url);
        Property dcTitle = fRdfModel.createProperty(OSLCConstants.DC_TITLE_PROP);
        assertTrue(fServiceProvider.listProperties(dcTitle).toList().size() <= 1);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void eachServiceHasOneDomain(String url) throws XPathExpressionException {
        initServiceProviderRdfXmlTests(url);
        Property domainProp = fRdfModel.createProperty(OSLCConstants.OSLC_V2, "domain");
        Property serviceProp = fRdfModel.createProperty(OSLCConstants.SERVICE_PROP);
        List<Statement> statements =
                fServiceProvider.listProperties(serviceProp).toList();
        boolean domainFound = false;
        for (Statement statement : statements) {
            Resource service = statement.getResource();
            List<Statement> domains = service.listProperties(domainProp).toList();
            // Make sure each service has one domain
            assertEquals(1, domains.size());
            // Make sure the domain is a resource with a URI
            assertTrue(domains.getFirst().getObject().isURIResource());
            // Make sure one of the domains found in the services matches the test version
            if (domains.getFirst().getResource().getNameSpace().equals(testVersion)) {
                domainFound = true;
            }
        }

        assertTrue(domainFound, "Domain " + testVersion + " not found for any Service resource");
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasService(String url) {
        initServiceProviderRdfXmlTests(url);
        Property service = fRdfModel.createProperty(OSLCConstants.SERVICE_PROP);
        List<?> lst = fServiceProvider.listProperties(service).toList();
        assertTrue(lst.size() >= 1);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void serviceProviderHasAtMostOnePublisher(String url) {
        initServiceProviderRdfXmlTests(url);
        Property dcPublisher = fRdfModel.createProperty(OSLCConstants.DC_PUBLISHER_PROP);
        assertTrue(fServiceProvider.listProperties(dcPublisher).toList().size() <= 1);
    }

    /* TODO: Complete ServiceProvider RDF/XML test validation

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void serviceProviderHasValidDetails(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void prefixDefinitionsAreValid(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }


        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void publisherElementsAreValid(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void serviceProviderHasAtMostOneOAuthElement(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void oAuthElementsAreValid(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void creationFactoriesAreValid(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void queryCapabilityBlocksAreValid(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }

        @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void dialogsAreValid(String thisUrl) throws Exception {
        initServiceProviderRdfXmlTests(thisUrl);
    }
    */
}
