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
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.cm;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.lyo.testsuite.oslcv2.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of a change request returned by accessing the change
 * request's URL directly. It runs the equality query from the properties file and grabs the first result
 * to test against, checking the relationship of elements in the XML representation of the change request.
 */
//@RunWith(Parameterized.class)
public class ChangeRequestXmlTests extends CoreResourceXmlTests {
	 
	public ChangeRequestXmlTests(String thisUrl) throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {
		
		super(thisUrl);		
		setNode(ns, resource);
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() 
		throws IOException, ParserConfigurationException, SAXException, XPathException {
		
		TestsBase.staticSetup();
		
		String useThisCR = setupProps.getProperty("useThisChangeRequest");
		if ( useThisCR != null ) {
			ArrayList<String> results = new ArrayList<String>();
			results.add(useThisCR);
			return toCollection(results);
		}
		
		setResourceTypeQuery(OSLCConstants.CORE_DEFAULT);
		setxpathSubStmt("//oslc_v2:usage/@rdf:resource");
		return getAllDescriptionUrls(eval);
	}
	
	public static String ns = "oslc_cm_v2";
	public static String resource = "ChangeRequest"; 
	public static String eval = "//rdfs:member/@rdf:resource";

	@Test
	public void changeRequestHasAtMostOneCloseDate() throws XPathExpressionException
	{
		NodeList closeDates = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:closeDate", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), closeDates.getLength() <= 1);
		//If there is a close date, verify the format.
		if (closeDates.getLength() > 0)
		{
			try
			{
				DatatypeConverter.parseDateTime(closeDates.item(0).getTextContent());
			}
			catch (Exception e)
			{
				fail("Modified date not in valid XSD format");
			}
		}
	}
	
	@Test
	public void changeRequestHasAtMostOneStatus() throws XPathExpressionException
	{
		NodeList statuses = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:status", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), statuses.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneClosedElement() throws XPathExpressionException
	{
		NodeList closedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:closed", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), closedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostInProgressElement() throws XPathExpressionException
	{
		NodeList inProgressEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:inprogress", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), inProgressEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneFixedElement() throws XPathExpressionException
	{
		NodeList fixedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:fixed", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), fixedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneApprovedElement() throws XPathExpressionException
	{
		NodeList approvedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:approved", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), approvedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneReviewedElement() throws XPathExpressionException
	{
		NodeList reviewedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:reviewed", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), reviewedEles.getLength() <= 1);
	}
	
	@Test
	public void changeRequestHasAtMostOneVerifiedElement() throws XPathExpressionException
	{
		NodeList verifiedEles = (NodeList) OSLCUtils.getXPath().evaluate("//oslc_cm_v2:ChangeRequest/" +
				"oslc_cm_v2:verified", doc, XPathConstants.NODESET);
		assertTrue(getFailureMessage(), verifiedEles.getLength() <= 1);
	}	
}
