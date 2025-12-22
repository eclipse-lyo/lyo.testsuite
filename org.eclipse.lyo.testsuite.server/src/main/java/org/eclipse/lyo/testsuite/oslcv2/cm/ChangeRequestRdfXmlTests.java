/*
 * Copyright (c) 2011, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite.oslcv2.cm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a change request returned by accessing the change request's URL
 * directly. It runs the equality query from the properties file and grabs the first result to test against, checking
 * the relationship of elements in the XML representation of the change request.
 */
public class ChangeRequestRdfXmlTests extends CoreResourceRdfXmlTests {

    public void initChangeRequestRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {
        super(thisUrl);
    }

    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        setResourceType(OSLCConstants.CM_CHANGE_REQUEST_TYPE);

        String useThisCR = setupProps.getProperty("useThisChangeRequest");
        if (useThisCR != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisCR);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.USAGE_PROP);
        setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");

        return getAllDescriptionUrls(eval);
    }

    public static String eval = OSLCConstants.RDFS_MEMBER;

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneCloseDate(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_CLOSE_DATE_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:closeDate, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneStatus(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_STATUS_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:status, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneClosedElement(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_CLOSED_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:closed, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostInProgressElement(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_INPROGRESS_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:inprogress, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneFixedElement(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_FIXED_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:fixed, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneApprovedElement(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_APPROVED_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:approved, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneReviewedElement(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_REVIEWED_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:reviewed, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void changeRequestHasAtMostOneVerifiedElement(String thisUrl) {
        initChangeRequestRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_VERIFIED_PROP);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have <=1 oslc_cm:verified, found " + size);
    }
}
