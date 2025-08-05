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
 *    Yuhong Yin    - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.qm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.apache.jena.rdf.model.StmtIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

public class TestResultRdfXmlTests extends CoreResourceRdfXmlTests {

    public TestResultRdfXmlTests(String thisUrl)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException,
                    NullPointerException {

        super(thisUrl);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        // If a particular TestResult asset is specified, use it
        String useThis = setupProps.getProperty("useThisTestResult");
        assumeTrue(useThis != null && !"".equals(useThis));
        ArrayList<String> results = new ArrayList<String>();
        results.add(useThis);

        return toCollection(results);
    }

    @Test
    public void TestResultHasOneReportsOnTestCase() throws XPathExpressionException {
        StmtIterator listStatements =
                getStatementsForProp(OSLCConstants.OSLC_QM_V2 + "reportsOnTestCase");
        int size = listStatements.toList().size();
        assertEquals("TestResult has exactly one oslc_qm:reportsOnTestCase", 1, size);
    }

    @Test
    public void TestResultHasAtMostOneStatus() throws XPathExpressionException {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.OSLC_QM_V2 + "status");
        int size = listStatements.toList().size();
        assertTrue("TestResult has zero or one oslc_qm:status, found " + size, size <= 1);
    }

    public static String eval = OSLCConstants.QM_TEST_RESULT;
}
