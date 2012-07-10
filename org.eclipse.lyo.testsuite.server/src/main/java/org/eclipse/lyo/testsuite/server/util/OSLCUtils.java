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
 *    Tim Eck II     - asset management test cases
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class OSLCUtils {
	//HttpClient used for all requests
	public static DefaultHttpClient httpclient = null;
	
	public static Document createXMLDocFromResponseBody(String respBody)
			throws ParserConfigurationException, IOException, SAXException
	{
	    //Create XML Doc out of response
	    DocumentBuilderFactory dbf =
            DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    	dbf.setValidating(false);
    	// Don't load external DTD
    	dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		DocumentBuilder db = dbf.newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(respBody));
	    return db.parse(is);
	}
	
	public static String createStringFromXMLDoc(Document document) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		return writer.getBuffer().toString();
	}
	
	public static HttpResponse getResponseFromUrl(String baseUrl, String url, Credentials creds, String acceptTypes)
			throws IOException
	{
		return getResponseFromUrl(baseUrl, url, creds, acceptTypes, null);
	}
	
	public static String absoluteUrlFromRelative(String baseUrl, String url) throws MalformedURLException {
		URL base = new URL(baseUrl);
		URL result = new URL(base, url);
		return result.toString();
	}
	
	public static HttpResponse getResponseFromUrl(String baseUrl, String url, Credentials creds, String acceptTypes,
			Header[] headers) throws IOException
	{

		getHttpClient(creds);
		
		url = absoluteUrlFromRelative(baseUrl, url);
		HttpGet httpget = new HttpGet(url);
		//Set headers, accept only the types passed in
		if (headers != null) {
			httpget.setHeaders(headers);
		}
		httpget.setHeader("Accept", acceptTypes);
		
		//Get the response and return it
		HttpResponse response = httpclient.execute(httpget);
        return response;
	}

	private static HttpClient getHttpClient(Credentials creds) {
		//If this is our first request, initialized our httpclient
		if (httpclient == null) {
			httpclient = new DefaultHttpClient();
			setupLazySSLSupport(httpclient);
			if (creds != null) {
				httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
			}
		}
		return httpclient;
	}
	
	public static int checkOslcVersion(String url, Credentials creds, String acceptTypes) throws ClientProtocolException, IOException
	{
		getHttpClient(creds);
		
		HttpGet httpget = new HttpGet(url);
		//Get the response and return it, accept only service provider catalogs & description documents
		httpget.setHeader("Accept", acceptTypes);
		httpget.setHeader("OSLC-Core-Version", "2.0");
		HttpResponse response = httpclient.execute(httpget);
		EntityUtils.consume(response.getEntity());
		
		if (response.containsHeader("OSLC-Core-Version") && 
				response.getFirstHeader("OSLC-Core-Version").getValue().equals("2.0"))
		{
			return 2;
		}
		return 1;
	}
	
	public static HttpResponse postDataToUrl(String url, Credentials creds, String acceptTypes, String contentType, 
			String content) throws IOException
	{
		return postDataToUrl(url, creds, acceptTypes, contentType, content, null);
	}
	
	public static HttpResponse postDataToUrl(String url, Credentials creds, String acceptTypes, String contentType, 
			String content, Header[] headers) throws IOException
	{
		getHttpClient(creds);
		
		//Create the post and add headers
		HttpPost httppost = new HttpPost(url);
		StringEntity entity = new StringEntity(content);
		
		httppost.setEntity(entity);
		if (headers != null)
			httppost.setHeaders(headers);
		if (contentType != null && !contentType.isEmpty())
			httppost.addHeader("Content-Type", contentType);
		if (acceptTypes != null && !acceptTypes.isEmpty())
			httppost.addHeader("Accept", acceptTypes);
		
		//Send the request and return the response
		HttpResponse resp = httpclient.execute(httppost);
		return resp;
	}
	
	public static HttpResponse deleteFromUrl(String url, Credentials creds, String acceptTypes) throws IOException
	{
		getHttpClient(creds);
		
		//Create the post and add headers
		HttpDelete httpdelete = new HttpDelete(url);
		
		if (acceptTypes != null && !acceptTypes.isEmpty())
			httpdelete.addHeader("Accept", acceptTypes);
		
		//Send the request and return the response
		HttpResponse resp = httpclient.execute(httpdelete, new BasicHttpContext());
		return resp;
	}
	
	public static HttpResponse putDataToUrl(String url, Credentials creds, String acceptTypes, String contentType, String content) throws IOException
	{
		return putDataToUrl(url, creds, acceptTypes, contentType, content, null);
	}
	
	public static HttpResponse putDataToUrl(String url, Credentials creds, String acceptTypes, 
			String contentType, String content, Header[] headers) throws IOException
	{
		getHttpClient(creds);
		
		//Create the post and add headers
		HttpPut httpput = new HttpPut(url);
		StringEntity entity = new StringEntity(content);
		
		httpput.setEntity(entity);
		if (headers != null) {
			httpput.setHeaders(headers);
		}
		if (contentType != null && !contentType.isEmpty())
			httpput.addHeader("Content-Type", contentType);
		if (acceptTypes != null && !acceptTypes.isEmpty())
			httpput.addHeader("Accept", acceptTypes);
		
		//Send the request and return the response
		HttpResponse resp = httpclient.execute(httpput, new BasicHttpContext());
		return resp;
	}
	
	public static HttpResponse getDataFromUrl(String url, Credentials creds, String acceptTypes, String contentType) throws ClientProtocolException, IOException
	{
		return getDataFromUrl(url, creds, acceptTypes, contentType, null);
	}
	
	public static HttpResponse getDataFromUrl(String url, Credentials creds, String acceptTypes,
			String contentType, Header[] headers) throws ClientProtocolException, IOException
	{
		getHttpClient(creds);
		
		//Create the post and add headers
		HttpGet httpget = new HttpGet(url);
		
		if (headers != null) {
			httpget.setHeaders(headers);
		}
		if (contentType != null && !contentType.isEmpty())
			httpget.addHeader("Content-Type", contentType);
		if (acceptTypes != null && !acceptTypes.isEmpty())
			httpget.addHeader("Accept", acceptTypes);
		
		//Send the request and return the response
		HttpResponse resp = httpclient.execute(httpget, new BasicHttpContext());
		return resp;
	}
	
	public static XPath getXPath()
	{
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(new OSLCNamespaceContext());
		return xpath;
	}
	
	static public void setupLazySSLSupport(HttpClient httpClient) {
		ClientConnectionManager connManager = httpClient.getConnectionManager();
		SchemeRegistry schemeRegistry = connManager.getSchemeRegistry();
		schemeRegistry.unregister("https");
		/** Create a trust manager that does not validate certificate chains */
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
				/** Ignore Method Call */
			}

			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
				/** Ignore Method Call */
			}

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL"); //$NON-NLS-1$
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			/* Fail Silently */
		} catch (KeyManagementException e) {
			/* Fail Silently */
		}

		SSLSocketFactory sf = new SSLSocketFactory(sc);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme https = new Scheme("https", sf, 443);

		schemeRegistry.register(https);
	}

	public static String readFileByNameAsString(String fileName) {
		return OSLCUtils.readFileAsString(new File(fileName));
	}
	
	public static String readFileAsString(File f) {
		
		StringBuilder stringBuilder = new StringBuilder();
	    Scanner scanner = null;
		try {
			scanner = new Scanner(f);
		} catch (FileNotFoundException e) {
			return null;
		}

	    try {
	        while(scanner.hasNextLine()) {        
	            stringBuilder.append(scanner.nextLine() + "\n");
	        }
	    } finally {
	        scanner.close();
	    }
	    return stringBuilder.toString();
	}
	
	public static void setupFormsAuth(String url, String username, String password) throws ClientProtocolException, IOException
	{
		//If this is our first request, initialized our httpclient
		if (httpclient == null) {
			httpclient = new DefaultHttpClient();
			setupLazySSLSupport(httpclient);
		}
		HttpPost httppost = new HttpPost(url);
		StringEntity entity = new StringEntity("j_username=" + username + "&j_password=" + password);
		//Set headers, accept only the types passed in
		httppost.setHeader("Accept", "*/*");
		httppost.setEntity(entity);
		httppost.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		httppost.addHeader("OSLC-Core-Version", "2.0");
		//Get the response and return it
		HttpResponse response = httpclient.execute(httppost);
		EntityUtils.consume(response.getEntity());
	}

	/**
	 * Adds a query string to the end of a URL, handling the case where the URL
	 * already has query parameters.
	 * 
	 * @param url
	 *            the URL to modify. It may or may not already have query
	 *            parameters.
	 * @param queryString
	 *            the query string, starting with a '?'. For instance,
	 *            "?oslc.properties=dcterms%3Aidentifier". Parameter values must
	 *            already be encoded.
	 * @return the new URL
	 */
	public static String addQueryStringToURL(String url, String queryString) {
		Assert.assertTrue("queryString must begin with a '?'",
				queryString.startsWith("?"));
		if (url.indexOf('?') == -1) {
			return url + queryString;
		}

		return url + '&' + queryString.substring(1);
	}

	/**
	 * Adds query parameter values to a URL.
	 * 
	 * @param url
	 *            the URL to modify
	 * @param params
	 *            a map of query parameters as name/value pairs
	 * @return the new URL
	 * @throws UnsupportedEncodingException
	 *             on errors encoding the values
	 */
	public static String addParametersToURL(String url,
			Map<String, String> queryParameters)
			throws UnsupportedEncodingException {
		StringBuffer updatedUrl = new StringBuffer(url);
		if (url.indexOf('?') == -1) {
			updatedUrl.append('?');
		} else {
			updatedUrl.append('&');
		}

		boolean first = true;
		for (Entry<String, String> next : queryParameters.entrySet()) {
			if (!first) {
				updatedUrl.append("&");
			}
			updatedUrl.append(URLEncoder.encode(next.getKey(), "UTF-8"));
			updatedUrl.append("=");
			updatedUrl.append(URLEncoder.encode(next.getValue(), "UTF-8"));
			first = false;
		}

		return updatedUrl.toString();
	}

	/**
	 * Adds a single query parameter to a URL.
	 * 
	 * @param url
	 *            the URL to modify
	 * @param name
	 *            the parameter name
	 * @param value
	 *            the parameter value
	 * @return the new URL
	 * @throws UnsupportedEncodingException
	 *             on errors encoding the values
	 */
	public static String addParameterToURL(String url, String name, String value)
			throws UnsupportedEncodingException {
		StringBuffer updatedUrl = new StringBuffer(url);
		if (url.indexOf('?') == -1) {
			updatedUrl.append('?');
		} else {
			updatedUrl.append('&');
		}

		updatedUrl.append(URLEncoder.encode(name, "UTF-8"));
		updatedUrl.append("=");
		updatedUrl.append(URLEncoder.encode(value, "UTF-8"));
		
		return updatedUrl.toString();
	}
}
