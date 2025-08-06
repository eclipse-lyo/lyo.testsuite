/*
 * Copyright (c) 2011,2013 IBM Corporation.
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
 *    Julianne Bielski - updated for OSLC Performance Monitoring V2 spec
 */

package org.eclipse.lyo.testsuite.oslcv2.pm;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a performance monitoring record returned by accessing the
 * performance monitoring record URL directly. It runs the equality query from the properties file and grabs the first
 * request to test against, checking the relationship of elements in the XML representation of the PMR request.
 */
public class PerformanceMonitoringRecordProcessRdfXmlTests extends CoreResourceRdfXmlTests {
    public PerformanceMonitoringRecordProcessRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {
        super(thisUrl);
        // TODO Auto-generated constructor stub

    }

    public static String eval = OSLCConstants.RDFS_MEMBER;

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        // If you use useThisResource, ensure that your resource type matches the type
        // of the resource
        setResourceType(OSLCConstants.CRTV_PROCESS_TYPE);

        // If a particular Resource is specified, use it
        String useThis = setupProps.getProperty("useThisResource");
        if ((useThis != null) && (useThis != "")) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThis);
            return toCollection(results);
        }
        setResourceTypeQuery(OSLCConstants.RESOURCE_TYPE_PROP);
        setxpathSubStmt(OSLCConstants.CRTV_PROCESS_TYPE);

        return getAllDescriptionUrls(eval);
    }

    @Test
    public void PerformanceMonitoringRecordHasOneisPartOf() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.PM_PMR_ISPARTOF);
        int size = listStatements.toList().size();
        assertTrue("Can have 1 dcterms:isPartOf, found " + size, size == 1);
    }

    @Test
    // OSLC: Optional
    public void PerformanceMonitoringRecordHasObservesOPTIONAL() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.PM_PMR_OBSERVES);
        int size = listStatements.toList().size();
        assertTrue("Can have zero or many  ems:observes, found " + size, size >= 0);
    }
}
