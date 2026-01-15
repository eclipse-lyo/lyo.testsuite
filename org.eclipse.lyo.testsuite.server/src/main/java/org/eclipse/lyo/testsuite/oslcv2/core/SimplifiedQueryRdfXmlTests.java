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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This class provides JUnit tests for the basic validation of query factories as specified in the OSLC version 2 spec.
 * This version of the query tests only tests the basic status code and form of the query responses, as without shapes
 * implemented it is difficult to represent the needed various templates of different change request types and to query
 * for the templates.
 */
public class SimplifiedQueryRdfXmlTests extends SimplifiedQueryBaseTests {

    public SimplifiedQueryRdfXmlTests(String thisUri) {
        super(thisUri);
    }

    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        ArrayList<String> capabilityURLsUsingRdfXml = new ArrayList<String>();

        String useThisQuery = setupProps.getProperty("useThisQuery");

        if (useThisQuery != null) {
            capabilityURLsUsingRdfXml.add(useThisQuery);
        } else {
            ArrayList<String> serviceUrls =
                    getServiceProviderURLsUsingRdfXml(setupProps.getProperty("baseUri"), onlyOnce);
            capabilityURLsUsingRdfXml = getCapabilityURLsUsingRdfXml(OSLCConstants.QUERY_BASE_PROP, serviceUrls, true);
        }

        return toCollection(capabilityURLsUsingRdfXml);
    }

    protected void validateNonEmptyResponse(String query) throws IOException {
        String queryUrl = OSLCUtils.addQueryStringToURL(currentUrl, query);
        Response response = OSLCUtils.getResponseFromUrl(setupBaseUrl, queryUrl, creds, OSLCConstants.CT_RDF, headers);

        int statusCode = response.getStatus();
        if (Response.Status.OK.getStatusCode() != statusCode) {
            throw new IOException("Response code: " + statusCode + " for " + queryUrl);
        }

        Model queryModel = ModelFactory.createDefaultModel();
        queryModel.read(
                response.readEntity(InputStream.class),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, currentUrl),
                OSLCConstants.JENA_RDF_XML);
        response.close();
        RDFUtils.validateModel(queryModel);

        Resource resultsRes = queryModel.getResource(currentUrl);

        // oslc:ResponseInfo if optional, validate it if one exists
        Resource respInfoType = queryModel.createResource(OSLCConstants.RESP_INFO_TYPE);
        ResIterator resIter = queryModel.listSubjectsWithProperty(RDF.type, respInfoType);
        while (resIter.hasNext()) {
            Resource responseInfoRes = resIter.nextResource();

            Property countMember = queryModel.getProperty(OSLCConstants.TOTAL_COUNT_PROP);
            StmtIterator stmts = responseInfoRes.listProperties(countMember);
            List<?> stmtsList = stmts.toList();
            if (!stmtsList.isEmpty()) {
                Statement stmt = (Statement) stmtsList.getFirst();
                assertTrue(stmtsList.size() == 1, "Expected oslc:totalCount property");

                Literal nodeLiteral = stmt.getObject().asLiteral();
                // If the following coercion fails, a DatatypeFormatException runtime
                // is thrown. Let it propagate
                int totalCount = nodeLiteral.getInt();

                assertTrue(totalCount > 0, "Expected oslc:totalCount > 0");
            }

            stmts = queryModel.listStatements(resultsRes, RDFS.member, (RDFNode) null);
            stmtsList = stmts.toList();
            assertNotNull(stmtsList.size() > 0, "Expected > 1 rdfs:member(s)");
        }
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validEqualsQueryContainsExpectedResource(String thisUri) throws IOException {
        String query = getQueryUrlForValidEqualsQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validNotEqualQueryContainsExpectedResource(String thisUri) throws IOException {
        String query = getQueryUrlForValidNotEqualQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validLessThanQueryContainsExpectedResources(String thisUri) throws IOException {
        String query = getQueryUrlForValidLessThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validGreaterThanQueryContainsExpectedDefects(String thisUri) throws IOException {
        String query = getQueryUrlForValidGreaterThanQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void validCompoundQueryContainsExpectedResource(String thisUri) throws IOException {
        String query = getQueryUrlForValidCompoundQueryContainsExpectedResources();
        validateNonEmptyResponse(query);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void fullTextSearchContainsExpectedResults(String thisUri) throws IOException {

        if (!getFullTextSearch()) return;

        String query = getQueryUrlForFullTextSearchContainsExpectedResults();
        validateNonEmptyResponse(query);
    }
}
