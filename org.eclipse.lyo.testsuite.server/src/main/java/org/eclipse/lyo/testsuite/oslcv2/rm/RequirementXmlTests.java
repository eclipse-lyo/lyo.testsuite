package org.eclipse.lyo.testsuite.oslcv2.rm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.abdera.xpath.XPathException;
import org.eclipse.lyo.testsuite.oslcv2.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
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
//@RunWith(Parameterized.class)
public class RequirementXmlTests extends CoreResourceXmlTests {
	
	public static String eval = "//rdfs:member/@rdf:resource";
	public static String ns = "oslc_rm_v2";
	public static String resource = "Requirement";
	
	public RequirementXmlTests(String thisUrl) throws IOException,
			ParserConfigurationException, SAXException,
			XPathExpressionException {

			super(thisUrl);	
			setNode(ns, resource);

	}

	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls() 
		throws IOException, ParserConfigurationException, SAXException, XPathException, javax.xml.xpath.XPathException {

		 staticSetup();
		 
		 
		// If a particular Requirement is specified, use it
		String useThis = setupProps.getProperty("useThisRequirement");
		if ((useThis != null) && (useThis != "")) {
			ArrayList<String> results = new ArrayList<String>();
			results.add(useThis);
			return toCollection(results);
		}
		
		setResourceTypeQuery(OSLCConstants.CORE_DEFAULT);
		setxpathSubStmt("//oslc_v2:usage/@rdf:resource");
		return getAllDescriptionUrls(eval);
	}
	
	




}
