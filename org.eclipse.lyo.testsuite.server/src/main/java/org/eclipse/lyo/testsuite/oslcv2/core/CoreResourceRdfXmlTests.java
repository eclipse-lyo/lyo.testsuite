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
 *    Yuhong Yin - initial API and implementation
 *    Samuel Padgett - don't fail if queryAdditionalParameters is not defined
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests with JSON format for the validation of an OSLC core resource.
 *
 */
@RunWith(Parameterized.class)
public abstract class CoreResourceRdfXmlTests extends TestsBase {
    private static Logger logger = Logger.getLogger(CoreResourceRdfXmlTests.class);

    private Response response;
    private Model fRdfModel = ModelFactory.createDefaultModel();
    private Resource fResource = null;

    protected static String resourceTypeQuery = "";
    protected static String xpathSubStmt = "";
    protected static String resourceType = "";

    public CoreResourceRdfXmlTests(String thisUrl)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException,
                    NullPointerException {

        super(thisUrl);

        // If currentUrl is null, it means that the query didn't match any
        // records. This isn't exactly a failure, but there's nothing more we
        // can test.
        assumeNotNull(currentUrl);
        response =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, currentUrl, creds, OSLCConstants.CT_RDF, headers);
        // Some records in the system might not be accessible to this user. This
        // isn't a failure, but there's nothing more we can test.
        int sc = response.getStatus();
        assumeTrue(sc != Status.FORBIDDEN.getStatusCode() && sc != Status.UNAUTHORIZED.getStatusCode());
        // Make sure the request succeeded before continuing.
        assertEquals(Response.Status.OK.getStatusCode(), sc);

        fRdfModel.read(
                response.readEntity(InputStream.class),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                OSLCConstants.JENA_RDF_XML);
        RDFUtils.validateModel(fRdfModel);

        fResource = (Resource) fRdfModel.getResource(currentUrl);
        if (logger.isDebugEnabled()) {
            StringWriter w = new StringWriter();
            fRdfModel.write(w, "TURTLE");
            logger.debug(
                    "Testing Resource <%s> with type <%s>".formatted(currentUrl, getResourceType()));
            logger.debug(w.toString());
        }

        String resourceType = getResourceType();
        if (resourceType != null && !"".equals(resourceType)) {
            assumeTrue(
                    fRdfModel.contains(
                            fResource, RDF.type, fRdfModel.createResource(getResourceType())));
        }
    }

    @Parameters
    protected static Collection<Object[]> getAllDescriptionUrls(String eval) throws IOException {
        ArrayList<String> results = new ArrayList<String>();

        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other ServiceProvidersCatalogs contained within it, recursively, in order to find the
        // URLs of all
        // query factories of the REST service.
        ArrayList<String> serviceUrls =
                getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);

        ArrayList<String> capabilityURLsUsingRdfXml =
                TestsBase.getCapabilityURLsUsingRdfXml(
                        OSLCConstants.QUERY_BASE_PROP,
                        serviceUrls,
                        true,
                        null,
                        getResourceTypeQuery(),
                        getxpathSubStmt());

        String where = setupProps.getProperty("changeRequestsWhere");
        if (where == null) {
            String queryProperty = setupProps.getProperty("queryEqualityProperty");
            String queryPropertyValue = setupProps.getProperty("queryEqualityValue");
            where = queryProperty + "=\"" + queryPropertyValue + "\"";
        }

        String additionalParameters = setupProps.getProperty("queryAdditionalParameters", "");
        String query =
                (additionalParameters.length() == 0) ? "?" : "?" + additionalParameters + "&";
        query = query + "oslc.where=" + URLEncoder.encode(where, "UTF-8") + "&oslc.pageSize=1";

        for (String queryBaseUri : capabilityURLsUsingRdfXml) {
            String queryUrl = OSLCUtils.addQueryStringToURL(queryBaseUri, query);
            Response resp =
                    OSLCUtils.getResponseFromUrl(
                            setupBaseUrl, queryUrl, creds, OSLCConstants.CT_RDF, headers);
            Model queryModel = ModelFactory.createDefaultModel();
            queryModel.read(
                    resp.readEntity(InputStream.class), queryBaseUri, OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(queryModel);

            Property member = queryModel.createProperty(eval);

            Resource queryBase = queryModel.getResource(queryBaseUri);
            StmtIterator statements = queryModel.listStatements(queryBase, member, (RDFNode) null);

            while (statements.hasNext()) {
                results.add(statements.nextStatement().getObject().toString());
                if (onlyOnce) return toCollection(results);
            }
            if (!results.isEmpty() && onlyOnce) break;
        }
        return toCollection(results);
    }

    @Test
    public void CoreResourceHasOneTitle() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_TITLE_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have 1 dc:title, found " + size, size == 1);
    }

    @Test
    public void CoreResourceHasAtMostOneDescription() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_DESC_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 dc:description, found " + size, size <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneIdentifier() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_ID_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 dc:identifier, found " + size, size <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneName() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_NAME_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 dc:name, found " + size, size <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneCreatedDate() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_CREATED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 dc:created, found " + size, size <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneModifiedDate() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DC_MODIFIED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 dc:modified, found " + size, size <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneDiscussion() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.DISCUSSION_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc:discussion, found " + size, size <= 1);
    }

    @Test
    public void CoreResourceHasAtMostOneInstanceShape() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.INST_SHAPE_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc:instanceShape, found " + size, size <= 1);
    }

    protected StmtIterator getStatementsForProp(String propUri) {
        Property prop = fRdfModel.getProperty(propUri);
        return fRdfModel.listStatements(fResource, prop, (RDFNode) null);
    }

    protected static void setResourceTypeQuery(String rT) {
        resourceTypeQuery = rT;
    }

    protected static String getResourceTypeQuery() {
        return resourceTypeQuery;
    }

    protected static void setxpathSubStmt(String x) {
        xpathSubStmt = x;
    }

    protected static String getxpathSubStmt() {
        return xpathSubStmt;
    }

    public static String getResourceType() {
        return resourceType;
    }

    public static void setResourceType(String resourceType) {
        CoreResourceRdfXmlTests.resourceType = resourceType;
    }
}
