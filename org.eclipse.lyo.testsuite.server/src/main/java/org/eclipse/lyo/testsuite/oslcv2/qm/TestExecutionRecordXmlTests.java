package org.eclipse.lyo.testsuite.oslcv2.qm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.lyo.testsuite.oslcv2.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestExecutionRecordXmlTests extends CoreResourceXmlTests {

	public TestExecutionRecordXmlTests(String thisUrl) 
		throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, NullPointerException {
		
		super(thisUrl);		
		setNode(ns, resource);
	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() 
		throws IOException, ParserConfigurationException, SAXException, XPathException {
				
		staticSetup();
		
		// If a particular TestExecutionRecord asset is specified, use it 
		String useThis = setupProps.getProperty("useThisTestExecutionRecord");
		if ( (useThis != null) && (useThis != "") ) {			
			ArrayList<String> results = new ArrayList<String>();
			results.add(useThis);
			return toCollection(results);
		}

		// Otherwise, run a query and pick up one
		setResourceTypeQuery(OSLCConstants.QM_TEST_EXECUTION_RECORD_QUERY);
		setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");
		return getAllDescriptionUrls(eval);
		
	}

	@Test
	public void TestExecutionRecordHasOneReportsOnTestPlan() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_qm_v2:reportsOnTestPlan";
		
		NodeList testplans = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);		
		
		int size = testplans.getLength();
		assertTrue("TestExecutionRecord has zero or one oslc_qm_v2:reportsOnTestPlan, found "+size, size <= 1);
	}

	@Test
	public void TestExecutionRecordHasOneRunsTestCase() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_qm_v2:runsTestCase";
		
		NodeList results = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);		
		
		assertEquals("oslc_qm_v2:runsTestCase"+getFailureMessage(), 1, results.getLength());
	}

	@Test
	public void TestExecutionRecordRelatedChangeRequest() throws XPathExpressionException
	{
		// TestExecutionRecord specific test
	}

	public static String ns = "oslc_qm_v2";
	public static String resource = "TestExecutionRecord"; 
	public static String eval = "//" + ns + ":" + resource + "/@rdf:about";
}