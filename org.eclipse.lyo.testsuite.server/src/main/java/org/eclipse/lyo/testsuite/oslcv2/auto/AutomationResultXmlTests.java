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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of an automation result returned by accessing the automation
 * result URL directly. It runs the equality query from the properties file and grabs the first result to test against,
 * checking the relationship of elements in the XML representation of the automation result.
 */
//
public class AutomationResultXmlTests extends CoreResourceXmlTests {

    public AutomationResultXmlTests() {
        super(null);
    }

    protected void setup(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        currentUrl = thisUrl;
        setNode(ns, resource);
    }

    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {

        TestsBase.staticSetup();

        String useThisAutoResult = setupProps.getProperty("useThisAutoResult");
        if (useThisAutoResult != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAutoResult);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.CORE_DEFAULT);
        setxpathSubStmt("//oslc_v2:usage/@rdf:resource");
        return getAllDescriptionUrls(eval);
    }

    public static String ns = "oslc_auto_v2";
    public static String resource = "AutomationResult";
    public static String eval = "//rdfs:member/@rdf:resource";

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void autoResultHasAtLeastOneState(String thisUrl) throws XPathExpressionException {
        setup(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:state";

        NodeList states = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue((states.getLength() >= 1), "oslc_auto_v2:state" + getFailureMessage());
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void autoResultHasAtLeastOneVerdict(String thisUrl) throws XPathExpressionException {
        setup(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:verdict";

        NodeList states = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue((states.getLength() >= 1), "oslc_auto_v2:verdict" + getFailureMessage());
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void autoResultHasAtMostOneDesiredState(String thisUrl) throws XPathExpressionException {
        setup(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:desiredState";

        NodeList desiredStates = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertTrue((desiredStates.getLength() <= 1), "oslc_auto_v2:desiredState" + getFailureMessage());
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void autoResultHasOneReportsOnLink(String thisUrl) throws XPathExpressionException {
        setup(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:reportsOnAutomationPlan";

        NodeList executes = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertEquals(1, executes.getLength(), "oslc_auto_v2:reportsOnAutomationPlan" + getFailureMessage());
    }

    @ParameterizedTest
    @MethodSource("getAllDescriptionUrls")
    public void autoResultHasOneProducedByLink(String thisUrl) throws XPathExpressionException {
        setup(thisUrl);
        String eval = "//" + getNode() + "/" + "oslc_auto_v2:producedByAutomationRequest";

        NodeList executes = (NodeList) OSLCUtils.getXPath().evaluate(eval, doc, XPathConstants.NODESET);

        assertEquals(1, executes.getLength(), "oslc_auto_v2:producedByAutomationRequest" + getFailureMessage());
    }
}
