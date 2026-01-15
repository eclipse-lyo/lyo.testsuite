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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of an automation plan returned by accessing the automation plan's
 * URL directly. It runs the equality query from the properties file and grabs the first result to test against,
 * checking the relationship of elements in the XML representation of the automation plan.
 */
//
public class AutomationPlanXmlTests extends CoreResourceXmlTests {

    public AutomationPlanXmlTests() {
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

        String useThisAutoPlan = setupProps.getProperty("useThisAutoPlan");
        if (useThisAutoPlan != null) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThisAutoPlan);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.CORE_DEFAULT);
        setxpathSubStmt("//oslc_v2:usage/@rdf:resource");
        return getAllDescriptionUrls(eval);
    }

    public static String ns = "oslc_auto_v2";
    public static String resource = "AutomationPlan";
    public static String eval = "//rdfs:member/@rdf:resource";
}
