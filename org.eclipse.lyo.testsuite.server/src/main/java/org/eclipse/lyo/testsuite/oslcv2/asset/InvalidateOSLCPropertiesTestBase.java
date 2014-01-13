package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSONException;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
abstract public class InvalidateOSLCPropertiesTestBase extends AssetTestBase {

	public InvalidateOSLCPropertiesTestBase(String url, String acceptType,
			String contentType) {
		super(url, acceptType, contentType);
	}

	@Test
	public void onePropertyNotSupported() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("oslc:mutable");
	}

	@Test
	public void existingPropertyWithInvalidPerfix() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("oslb:serviceProvider");
	}

	@Test
	public void multiPropertyNotSupported() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("oslc:goat,oslc:mountain");
	}

	@Test
	public void validPropertyAndInvalidProperty() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("oslc:serviceProvider,oslc:mutable");
	}

	@Test
	public void directQueryWithFoafProperty() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("foaf:givenName");
	}

	@Test
	public void queryWithoutPrefex() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("instanceShape");
	}

	@Test
	public void queryWithStrangeCharacter() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("*$");
	}

	@Test
	public void queryWithWrongFoafProperty() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("dcterms:title,dcterms:creator{foaf:givenName,foaf:familyMame}");
	}

	@Test
	public void putUpperLevelPropertyInWrongplace() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("dcterms:creator{dcterms:title}");
	}

	@Test
	public void propertyNotSupporyFoafProperty() throws IOException, JSONException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
		queryInvalidOSLCProperties("oslc:shortTitle{foaf:givenName,foaf:familyName}");
	}
	
	protected void putInvalidateOSLCProperties(String content, String query) throws IOException{
		String invalidateQuery = assetUrl + query;
		
		HttpResponse resp = OSLCUtils.putDataToUrl(invalidateQuery,
				basicCreds, acceptType, contentType, content, headers);

		EntityUtils.consume(resp.getEntity());
		assertEquals(HttpStatus.SC_CONFLICT, resp.getStatusLine()
				.getStatusCode());
	}
	
	protected HttpResponse putAssetProperties(String properties, String content) throws IOException {
		String invalidateUrl = assetUrl + properties;
		
		HttpResponse resp = OSLCUtils.putDataToUrl(invalidateUrl,
				basicCreds, acceptType, contentType, content, headers);

		EntityUtils.consume(resp.getEntity());
		return resp;
	}
	
	/**
	 * Test cases will call this method to query with invalidate oslc.properties
	 * 
	 * @param query Invalidate oslc.properties value
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws XPathExpressionException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */
	abstract protected void queryInvalidOSLCProperties(String query) throws JSONException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException;
}
