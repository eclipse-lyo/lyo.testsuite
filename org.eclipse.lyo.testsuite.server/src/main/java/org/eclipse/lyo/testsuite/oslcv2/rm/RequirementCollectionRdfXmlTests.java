package org.eclipse.lyo.testsuite.oslcv2.rm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.eclipse.lyo.testsuite.oslcv2.core.CoreResourceRdfXmlTests;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class RequirementCollectionRdfXmlTests extends CoreResourceRdfXmlTests {

    public static String eval = OSLCConstants.RDFS_MEMBER;

    public RequirementCollectionRdfXmlTests(String thisUrl)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException,
                    NullPointerException {

        super(thisUrl);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls() throws IOException {

        staticSetup();

        setResourceType(OSLCConstants.RM_REQUIREMENT_COLLECTION_TYPE);

        // If a particular RequirementCollection is specified, use it
        String useThis = setupProps.getProperty("useThisRequirementCollection");
        if ((useThis != null) && (useThis != "")) {
            ArrayList<String> results = new ArrayList<String>();
            results.add(useThis);
            return toCollection(results);
        }

        setResourceTypeQuery(OSLCConstants.RESOURCE_TYPE_PROP);
        setxpathSubStmt(OSLCConstants.RM_REQUIREMENT_COLLECTION_TYPE);

        return getAllDescriptionUrls(eval);
    }
}
