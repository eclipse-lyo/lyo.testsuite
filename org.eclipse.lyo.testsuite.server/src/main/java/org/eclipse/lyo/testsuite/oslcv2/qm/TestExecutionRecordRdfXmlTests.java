/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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
 *    Yuhong Yin - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.qm;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.lyo.testsuite.oslcv2.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.StmtIterator;

public class TestExecutionRecordRdfXmlTests extends CoreResourceRdfXmlTests {

	public TestExecutionRecordRdfXmlTests(String thisUrl) 
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, NullPointerException {
		
		super(thisUrl);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() throws IOException {
		
		staticSetup();
		
		setResourceType(OSLCConstants.QM_TEST_EXECUTION_RECORD);

		// If a particular TestExecutionRecord asset is specified, use it 
		String useThis = setupProps.getProperty("useThisTestExecutionRecord");
		if ( (useThis != null) && (useThis != "") ) {			
			ArrayList<String> results = new ArrayList<String>();
			results.add(useThis);
			return toCollection(results);
		}
		
		// Otherwise, run a query and pick up one
		setResourceTypeQuery(OSLCConstants.RESOURCE_TYPE_PROP);
		setxpathSubStmt(OSLCConstants.QM_TEST_EXECUTION_RECORD_QUERY);
		return getAllDescriptionUrls(eval);
	}
	
	@Test
	public void TestExecutionRecordHasOneReportsOnTestPlan() throws XPathExpressionException
	{
		StmtIterator listStatements = getStatementsForProp("oslc_qm_v2:reportsOnTestPlan");
		int size=listStatements.toList().size();
		assertTrue("TestExecutionRecord has zero or one oslc_qm_v2:reportsOnTestPlan, found "+size, size <= 1);
	}

	@Test
	public void TestExecutionRecordHasOneRunsTestCase() throws XPathExpressionException
	{
		StmtIterator listStatements = getStatementsForProp("oslc_qm_v2:runsTestCase");
		int size=listStatements.toList().size();
		assertTrue("TestExecutionRecord has one oslc_qm_v2:runsTestCase, found "+size, size == 1);
	}

	@Test
	public void TestExecutionRecordRelatedChangeRequest() throws XPathExpressionException
	{
		// TestExecutionRecord specific test
		// zero or many
	}

	public static String ns = "oslc_qm_v2";
	public static String resource = "TestExecutionRecord"; 
	public static String eval = OSLCConstants.QM_TEST_EXECUTION_RECORD; 
}
