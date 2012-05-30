package org.eclipse.lyo.testsuite.oslcv2.qm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
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
				
		setResourceTypeQuery(OSLCConstants.QM_TEST_EXECUTION_RECORD_QUERY);
		setxpathSubStmt("//oslc_v2:QueryCapability/oslc:resourceType/@rdf:resource");
		return getAllDescriptionUrls(eval);
		
	}

	@Test
	public void TestExecutionRecordHasOneReportsOnTestPlan() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_qm_v2:reportsOnTestPlan";
		
		NodeList results = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);		
		
		assertEquals(getFailureMessage(), 1, results.getLength());
	}

	@Test
	public void TestExecutionRecordHasOneRunsTestCase() throws XPathExpressionException
	{
		String eval = "//" + getNode() + "/" + "oslc_qm_v2:runsTestCase";
		
		NodeList results = (NodeList) OSLCUtils.getXPath().evaluate(eval,
	    		doc, XPathConstants.NODESET);		
		
		assertEquals(getFailureMessage(), 1, results.getLength());
	}

	@Test
	public void TestExecutionRecordRelatedChangeRequest() throws XPathExpressionException
	{
		// TestCase specific test
	}

	public static String ns = "oslc_qm_v2";
	public static String resource = "TestExecutionRecord"; 
	public static String eval = "//" + ns + ":" + resource + "/@rdf:about";
}