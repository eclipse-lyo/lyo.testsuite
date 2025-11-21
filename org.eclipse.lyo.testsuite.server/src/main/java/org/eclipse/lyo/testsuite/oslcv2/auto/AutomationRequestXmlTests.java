/*
 * Copyright (c) 2011, 2012, 2025 IBM Corporation and others
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.jupiter.api.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of an automation request returned by accessing the automation
 * request URL directly. It runs the equality query from the properties file and grabs the first result to test against,
 * checking the relationship of elements in the XML representation of the automation request.
 */
// @RunWith(Parameterized.class)
public class AutomationRequestXmlTests extends CoreResourceXmlTests {

    public AutomationRequestXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        super(thisUrl);
        setNode(ns, resource);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {

        TestsBase.staticSetup();

        String useThisAutoRequest = setupProps.getProperty("useThisAutoRequest");
        if (useThisAutoRequest != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAutoRequest);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.CORE_DEFAULT);
        setxpathSubStmt("//oslc_v2:usage/@rdf:resource");
        return getAllDescriptionUrls(eval);
    }

    public static String ns = "oslc_auto_v2";
    public static String resource = "AutomationRequest";
    public static String eval = "//rdfs:member/@rdf:resource";

    @Test
    public void autoRequestHasAtLeastOneState() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:state";

        NodeList states = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue((states.getLength() >= 1), "oslc_auto_v2:state" + getFailureMessage());
    }

    @Test
    public void autoRequestHasAtMostOneDesiredState() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:desiredState";

        NodeList desiredStates = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue((desiredStates.getLength() <= 1), "oslc_auto_v2:desiredState" + getFailureMessage());
    }

    @Test
    public void autoRequestHasOneExecutesLink() throws XPathExpressionException {
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:executesAutomationPlan";

        NodeList executes = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertEquals(1, executes.getLength(), "oslc_auto_v2:executesAutomationPlan" + getFailureMessage());
    }
}
