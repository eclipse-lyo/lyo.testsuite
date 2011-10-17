/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class OSLCUtils {
	//HttpClient used for all requests
	public static DefaultHttpClient httpclient = null;
	
	public static Document createXMLDocFromResponseBody(String respBody) throws ParserConfigurationException, IOException, SAXException
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
	
	public static HttpResponse getResponseFromUrl(String baseUrl, String url, Credentials creds, String acceptTypes) throws IOException
	{
		return getResponseFromUrl(baseUrl, url, creds, acceptTypes, null);
	}
	
	public static String absoluteUrlFromRelative(String baseUrl, String url) throws MalformedURLException
	{
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
		if (httpclient == null)
		{
			httpclient = new DefaultHttpClient();
			setupLazySSLSupport(httpclient);
			httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
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
		response.getEntity().consumeContent();
		
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

	public static String readFileByNameAsString(String fileName) 
	{
		return OSLCUtils.readFileAsString(new File(fileName));
	}
	
	public static String readFileAsString(File f)
	{
		
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
		if (httpclient == null)
		{
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
		response.getEntity().consumeContent();
	}
}
