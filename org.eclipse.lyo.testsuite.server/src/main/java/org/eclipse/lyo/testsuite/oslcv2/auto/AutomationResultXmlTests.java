/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of an automation result returned by accessing the automation
 * result URL directly. It runs the equality query from the properties file and grabs the first result
 * to test against, checking the relationship of elements in the XML representation of the automation result.
 */
//@RunWith(Parameterized.class)
public class AutomationResultXmlTests extends CoreResourceXmlTests {

	public AutomationResultXmlTests(String thisUrl) throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {

		super(thisUrl);
		setNode(ns, resource);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
		throws IOException, ParserConfigurationException, SAXException, XPathException {

		TestsBase.staticSetup();

		String useThisAutoResult = setupProps.getProperty("useThisAutoResult");
		if ( useThisAutoResult != null ) {
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

	@Test
	public void autoResultHasAtLeastOneState() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_auto_v2:state";

		NodeList states = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);

		assertTrue("oslc_auto_v2:state"+getFailureMessage(), (states.getLength()>=1));
	}

	@Test
	public void autoResultHasAtLeastOneVerdict() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_auto_v2:verdict";

		NodeList states = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);

		assertTrue("oslc_auto_v2:verdict"+getFailureMessage(), (states.getLength()>=1));
	}

	@Test
	public void autoResultHasAtMostOneDesiredState() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_auto_v2:desiredState";

		NodeList desiredStates = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);

		assertTrue("oslc_auto_v2:desiredState"+getFailureMessage(), (desiredStates.getLength()<=1));
	}

	@Test
	public void autoResultHasOneReportsOnLink() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_auto_v2:reportsOnAutomationPlan";

		NodeList executes = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);

		assertEquals("oslc_auto_v2:reportsOnAutomationPlan"+getFailureMessage(),1, executes.getLength());
	}

	@Test
	public void autoResultHasOneProducedByLink() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_auto_v2:producedByAutomationRequest";

		NodeList executes = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);

		assertEquals("oslc_auto_v2:producedByAutomationRequest"+getFailureMessage(),1, executes.getLength());
	}
}
