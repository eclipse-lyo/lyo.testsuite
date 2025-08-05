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
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.cm;

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
 * This class provides JUnit tests for the validation of a change request returned by accessing the change
 * request's URL directly. It runs the equality query from the properties file and grabs the first result
 * to test against, checking the relationship of elements in the XML representation of the change request.
 */
@RunWith(Parameterized.class)
public class ChangeRequestRdfXmlTests extends CoreResourceRdfXmlTests {

    public ChangeRequestRdfXmlTests(String thisUrl)
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

    @Test
    public void changeRequestHasAtMostOneCloseDate() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_CLOSE_DATE_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:closeDate, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostOneStatus() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_STATUS_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:status, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostOneClosedElement() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_CLOSED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:closed, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostInProgressElement() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_INPROGRESS_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:inprogress, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostOneFixedElement() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_FIXED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:fixed, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostOneApprovedElement() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_APPROVED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:approved, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostOneReviewedElement() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_REVIEWED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:reviewed, found " + size, size <= 1);
    }

    @Test
    public void changeRequestHasAtMostOneVerifiedElement() {
        StmtIterator listStatements = getStatementsForProp(OSLCConstants.CM_VERIFIED_PROP);
        int size = listStatements.toList().size();
        assertTrue("Can have <=1 oslc_cm:verified, found " + size, size <= 1);
    }
}
