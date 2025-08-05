/*******************************************************************************
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
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.auto;

import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.StmtIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a auto request returned by accessing the auto request URL directly.
 * It runs the equality query from the properties file and grabs the first request
 * to test against, checking the relationship of elements in the XML representation of the auto request.
 */
@RunWith(Parameterized.class)
public class AutomationRequestRdfXmlTests extends CoreResourceRdfXmlTests {

    public AutomationRequestRdfXmlTests(String thisUrl)
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

        setResourceType(OSLCConstants.AUTO_AUTOMATION_REQUEST_TYPE);

        String useThisAutoRequest = setupProps.getProperty("useThisAutoRequest");
        if (useThisAutoRequest != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAutoRequest);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.USAGE_PROP);
        setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");

        return getAllDescriptionUrls(eval);
    }

    public static String eval = OSLCConstants.RDFS_MEMBER;

    @Test
    public void autoRequestHasAtLeastOneState() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_STATE);
        int size = listStatements.toList().size();
        assertTrue("Can have 1 or more oslc_auto:state, found " + size, size >= 1);
    }

    @Test
    public void autoRequestHasAtMostOneDesiredState() {
        StmtIterator listStatements =
                getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_DESIRED_STATE);
        int size = listStatements.toList().size();
        assertTrue("Can have at most 1 oslc_auto:desiredState, found " + size, size <= 1);
    }

    @Test
    public void autoRequestHasOneExecutesLink() {
        StmtIterator listStatements =
                getStatementsForProp(OSLCConstants.AUTO_AUTOMATION_EXECUTES_AUTO_PLAN);
        int size = listStatements.toList().size();
        assertTrue("Can have 1  oslc_auto:executesAutomationPlan, found " + size, size == 1);
    }
}
