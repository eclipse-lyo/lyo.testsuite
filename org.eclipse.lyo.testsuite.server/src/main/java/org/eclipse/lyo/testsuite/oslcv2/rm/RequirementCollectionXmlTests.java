package org.eclipse.lyo.testsuite.oslcv2.rm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.xml.sax.SAXException;

public class RequirementCollectionXmlTests extends CoreResourceXmlTests {

    public void initRequirementCollectionXmlTests(String thisUrl)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        super(thisUrl);
        setNode(ns, resource);
    }

    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, javax.xml.xpath.XPathException {

        staticSetup();

        // If a particular RequirementCollection is specified, use it
        String useThis = setupProps.getProperty("useThisRequirementCollection");
        if ((useThis != null) && (useThis != "")) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThis);
            return toCollection(results);
        }
        setResourceTypeQuery(OSLCConstants.CORE_DEFAULT);
        setxpathSubStmt("//oslc_v2:usage/@rdf:resource");
        return getAllDescriptionUrls(eval);
    }

    public static String eval = "//rdfs:member/@rdf:resource";
    public static String ns = "oslc_rm_v2";
    public static String resource = "RequirementCollection";
}
