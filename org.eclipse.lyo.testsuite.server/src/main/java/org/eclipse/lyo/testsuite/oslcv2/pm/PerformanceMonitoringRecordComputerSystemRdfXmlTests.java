/*
 * Copyright (c) 2011,2013, 2025 IBM Corporation and others
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
package org.eclipse.lyo.testsuite.oslcv2.pm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.jupiter.api.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a performance monitoring record returned by accessing the
 * performance monitoring record URL directly. It runs the equality query from the properties file and grabs the first
 * request to test against, checking the relationship of elements in the XML representation of the PMR request.
 */
public class PerformanceMonitoringRecordComputerSystemRdfXmlTests extends CoreResourceRdfXmlTests {

    
    public void initCoreResourceRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {

        super.initCoreResourceRdfXmlTests(thisUrl);
        // TODO Auto-generated constructor stub

    }

    public static String eval = OSLCConstants.RDFS_MEMBER;

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        // If you use useThisResource, ensure that your resource type matches the type
        // of the resource
        setResourceType(OSLCConstants.CRTV_COMPUTERSYSTEM_TYPE);

        // If a particular Resource is specified, use it
        String useThis = setupProps.getProperty("useThisResource");
        if ((useThis != null) && (useThis != "")) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThis);
            return toCollection(results);
        }
        setResourceTypeQuery(OSLCConstants.RESOURCE_TYPE_PROP);
        setxpathSubStmt(OSLCConstants.CRTV_COMPUTERSYSTEM_TYPE);

        return getAllDescriptionUrls(eval);
    }

    @Test
    public void PerformanceMonitoringRecordHasOneisPartOf() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.PM_PMR_ISPARTOF);
        int size = listStatements.toList().size();
        assertTrue(size == 1, "Can have 1 dcterms:isPartOf, found " + size);
    }

    @Test
    // OSLC: Optional
    public void PerformanceMonitoringRecordHasObservesOPTIONAL() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.PM_PMR_OBSERVES);
        int size = listStatements.toList().size();
        assertTrue(size >= 0, "Can have zero or many  ems:observes, found " + size);
    }
}
