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
package org.eclipse.lyo.testsuite.oslcv2.auto;

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
 * This class provides JUnit tests for the validation of a auto result returned by accessing the auto result URL
 * directly. It runs the equality query from the properties file and grabs the first result to test against, checking
 * the relationship of elements in the XML representation of the auto result.
 */
public class AutomationResultRdfXmlTests extends CoreResourceRdfXmlTests {

    public void initAutomationResultRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {

        setup(thisUrl);
    }

    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        setResourceType(OSLCConstants.AUTO_AUTOMATION_RESULT_TYPE);

        String useThisAutoResult = setupProps.getProperty("useThisAutoResult");
        if (useThisAutoResult != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAutoResult);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.USAGE_PROP);
        setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");

        return getAllDescriptionUrls(eval);
    }

    public static String eval = OSLCConstants.RDFS_MEMBER;

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void autoResulttHasAtLeastOneState(String thisUrl) {
        initAutomationResultRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_STATE);
        int size = listStatements.toList().size();
        assertTrue(size >= 1, "Can have 1 or more oslc_auto:state, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void autoResulttHasAtLeastOneVerdict(String thisUrl) {
        initAutomationResultRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_VERDICT);
        int size = listStatements.toList().size();
        assertTrue(size >= 1, "Can have 1 or more oslc_auto:verdict, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void autoResultHasAtMostOneDesiredState(String thisUrl) {
        initAutomationResultRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_DESIRED_STATE);
        int size = listStatements.toList().size();
        assertTrue(size <= 1, "Can have at most 1 oslc_auto:desiredState, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void autoResultHasOneReportsOnLink(String thisUrl) {
        initAutomationResultRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_REPORTS_AUTO_PLAN);
        int size = listStatements.toList().size();
        assertTrue(size == 1, "Can have 1  oslc_auto:reportsOnAutomationPlan, found " + size);
    }

    @MethodSource("getAllDescriptionUrls")
    @ParameterizedTest
    public void autoResultHasOneProducedByLink(String thisUrl) {
        initAutomationResultRdfXmlTests(thisUrl);
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_PRODUCED_AUTO_REQUEST);
        int size = listStatements.toList().size();
        assertTrue(size == 1, "Can have 1  oslc_auto:producedByAutomationRequest, found " + size);
    }
}
