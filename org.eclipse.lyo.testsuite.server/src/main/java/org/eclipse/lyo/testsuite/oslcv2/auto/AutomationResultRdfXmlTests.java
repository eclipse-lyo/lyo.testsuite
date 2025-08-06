/*
 * Copyright (c) 2011 IBM Corporation.
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
 *    Michael Fiedler - updated for OSLC Automation V2 spec
 */
package org.eclipse.lyo.testsuite.oslcv2.auto;

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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a auto result returned by accessing the auto result URL
 * directly. It runs the equality query from the properties file and grabs the first result to test against, checking
 * the relationship of elements in the XML representation of the auto result.
 */
@RunWith(Parameterized.class)
public class AutomationResultRdfXmlTests extends CoreResourceRdfXmlTests {

    public AutomationResultRdfXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException,
                    NullPointerException {
        super(thisUrl);
    }

    @Parameters
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

    @Test
    public void autoResulttHasAtLeastOneState() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_STATE);
        int size = listStatements.toList().size();
        assertTrue("Can have 1 or more oslc_auto:state, found " + size, size >= 1);
    }

    @Test
    public void autoResulttHasAtLeastOneVerdict() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_VERDICT);
        int size = listStatements.toList().size();
        assertTrue("Can have 1 or more oslc_auto:verdict, found " + size, size >= 1);
    }

    @Test
    public void autoResultHasAtMostOneDesiredState() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_DESIRED_STATE);
        int size = listStatements.toList().size();
        assertTrue("Can have at most 1 oslc_auto:desiredState, found " + size, size <= 1);
    }

    @Test
    public void autoResultHasOneReportsOnLink() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_REPORTS_AUTO_PLAN);
        int size = listStatements.toList().size();
        assertTrue("Can have 1  oslc_auto:reportsOnAutomationPlan, found " + size, size == 1);
    }

    @Test
    public void autoResultHasOneProducedByLink() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_PRODUCED_AUTO_REQUEST);
        int size = listStatements.toList().size();
        assertTrue("Can have 1  oslc_auto:producedByAutomationRequest, found " + size, size == 1);
    }
}
