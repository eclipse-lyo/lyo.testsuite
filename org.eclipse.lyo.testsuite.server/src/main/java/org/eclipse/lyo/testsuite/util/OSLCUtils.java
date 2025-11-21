/*
 * Copyright (c) 2011, 2014, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lyo.testsuite.util;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
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
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import org.apache.log4j.Logger;
import org.eclipse.lyo.client.OslcClient;
import org.eclipse.lyo.client.OslcClientFactory;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.jupiter.api.Assertions;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OSLCUtils {
    private static Logger logger = Logger.getLogger(OSLCUtils.class);
    // Lyo Client used for all requests
    private static OslcClient oslcClient = null;
    private static final String JAZZ_AUTH_MESSAGE_HEADER = "X-com-ibm-team-repository-web-auth-msg";
    private static final String JAZZ_AUTH_FAILED = "authfailed";

    private static OslcClient getOslcClient(TestsBase.UserCredentials creds) {
        if (oslcClient == null) {
            // Create a new ClientBuilder
            ClientBuilder clientBuilder = ClientBuilder.newBuilder();

            if (creds instanceof TestsBase.Oauth1UserCredentials oauth1UserCredentials) {
                var builder = OslcClientFactory.oslcOAuthClientBuilder();
                // FIXME callback
                builder.setOAuthConsumer(
                        null, oauth1UserCredentials.principal().getName(), oauth1UserCredentials.secret());
                builder.setClientBuilder(clientBuilder);
            } else {
                if (creds instanceof TestsBase.UserPassword(String login, String password)) {
                    var authFeature = HttpAuthenticationFeature.basic(login, password);
                    clientBuilder.register(authFeature);
                }
                var builder = OslcClientFactory.oslcClientBuilder();
                builder.setClientBuilder(clientBuilder);
            }

            // Create the OSLC client
            oslcClient = new OslcClient(clientBuilder);
        }
        return oslcClient;
    }

    public static Document createXMLDocFromResponseBody(String respBody)
            throws ParserConfigurationException, IOException, SAXException {
        // Create XML Doc out of response
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        // Don't load external DTD
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(respBody));
        try {
            return db.parse(is);
        } catch (SAXException e) {
            logger.error("Exception parsing XML response body:%n%s%n".formatted(respBody), e);
            throw e;
        }
    }

    public static String createStringFromXMLDoc(Document document) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    public static Response getResponseFromUrl(
            String baseUrl, String url, TestsBase.Oauth1UserCredentials creds, String acceptTypes) throws IOException {
        return getResponseFromUrl(baseUrl, url, creds, acceptTypes, null);
    }

    public static Response getResponseFromUrl(
            String baseUrl, String url, TestsBase.UserPassword userCredentials, String acceptTypes) throws IOException {
        return getResponseFromUrl(baseUrl, url, userCredentials, acceptTypes, null);
    }

    public static Response getResponseFromUrl(
            String baseUrl,
            String url,
            TestsBase.UserPassword userCredentials,
            String acceptTypes,
            Map<String, String> headers)
            throws IOException {
        OslcClient client = getOslcClient(userCredentials);

        url = absoluteUrlFromRelative(baseUrl, url);

        // Prepare request headers
        Map<String, String> requestHeaders = new HashMap<>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestHeaders.put(entry.getKey(), entry.getValue());
            }
        }
        if (acceptTypes != null && !acceptTypes.isEmpty()) {
            requestHeaders.put("Accept", acceptTypes);
        }

        // Execute the request
        Response response = client.getResource(url, requestHeaders);
        return response;
    }

    public static String absoluteUrlFromRelative(String baseUrl, String url) throws MalformedURLException {
        URL base = new URL(baseUrl);
        URL result = base;
        if (url != null) result = new URL(base, url);

        return result.toString();
    }

    public static Response getResponseFromUrl(
            String baseUrl,
            String url,
            TestsBase.Oauth1UserCredentials creds,
            String acceptTypes,
            Map<String, String> headers)
            throws IOException {

        OslcClient client = getOslcClient(creds);

        url = absoluteUrlFromRelative(baseUrl, url);

        // Prepare request headers
        Map<String, String> requestHeaders = new HashMap<>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestHeaders.put(entry.getKey(), entry.getValue());
            }
        }
        if (acceptTypes != null && !acceptTypes.isEmpty()) {
            requestHeaders.put("Accept", acceptTypes);
        }

        // Handle OAuth signing if needed
        if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
            oAuthSignLyo(client, url, "GET", creds, requestHeaders);
        }

        // Execute the request
        Response response = client.getResource(url, requestHeaders);
        return response;
    }

    // Sign the request using 2-legged OAuth if necessary for Lyo Client.
    private static void oAuthSignLyo(
            OslcClient client,
            String url,
            String method,
            TestsBase.Oauth1UserCredentials creds,
            Map<String, String> headers) {
        if (creds != null) {
            OAuthServiceProvider provider = new OAuthServiceProvider(null, null, null);
            OAuthConsumer consumer = new OAuthConsumer("", creds.getPrincipal().getName(), creds.getSecret(), provider);
            OAuthAccessor accessor = new OAuthAccessor(consumer);
            accessor.accessToken = "";
            OAuthMessage message;
            try {
                message = accessor.newRequestMessage(method, url, null);
                String authHeader = message.getAuthorizationHeader(null);
                headers.put("Authorization", authHeader);
            } catch (Exception e) {
                logger.error("Could not OAuth sign request", e);
                throw new RuntimeException(e);
            }
        }
    }

    //    public static int checkOslcVersion(String url, TestsBase.UserCredentials creds, String acceptTypes)
    //            throws IOException {
    //        OslcClient client = getOslcClient(creds);
    //
    //        // Prepare request headers
    //        Map<String, String> requestHeaders = new HashMap<>();
    //        if (acceptTypes != null && !acceptTypes.isEmpty()) {
    //            requestHeaders.put("Accept", acceptTypes);
    //        }
    //        requestHeaders.put("OSLC-Core-Version", "2.0");
    //
    //        // Handle OAuth signing if needed
    //        if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
    //            oAuthSignLyo(client, url, "GET", creds, requestHeaders);
    //        }
    //
    //        // Execute the request
    //        Response response = client.getResource(url, requestHeaders);
    //
    //        // Consume the entity (we don't need the body)
    //        response.readEntity(String.class);
    //
    //        // Check for OSLC-Core-Version header
    //        if (response.getHeaders().containsKey("OSLC-Core-Version")) {
    //            String version = response.getHeaderString("OSLC-Core-Version");
    //            if ("2.0".equals(version)) {
    //                return 2;
    //            }
    //        }
    //        return 1;
    //    }

    public static Response postDataToUrl(
            String url, TestsBase.UserCredentials creds, String acceptTypes, String contentType, String content)
            throws IOException {
        return postDataToUrl(url, creds, acceptTypes, contentType, content, null);
    }

    public static Response postDataToUrl(
            String url,
            TestsBase.UserCredentials creds,
            String acceptTypes,
            String contentType,
            String content,
            Map<String, String> headers)
            throws IOException {
        OslcClient client = getOslcClient(creds);

        // Prepare request headers
        Map<String, String> requestHeaders = new HashMap<>();
        if (headers != null) {
            requestHeaders.putAll(headers);
        }
        if (contentType != null && !contentType.isEmpty()) {
            requestHeaders.put("Content-Type", contentType);
        }
        if (acceptTypes != null && !acceptTypes.isEmpty()) {
            requestHeaders.put("Accept", acceptTypes);
        }

        // Handle OAuth signing if needed
        if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
            oAuthSignLyo(client, url, "POST", oAuthCredentials, requestHeaders);
        }

        // Execute the request
        Response response = client.createResource(url, content, contentType, acceptTypes);
        return response;
    }

    public static Response deleteFromUrl(String url, TestsBase.UserCredentials creds, String acceptTypes)
            throws IOException {
        OslcClient client = getOslcClient(creds);

        // Prepare request headers
        Map<String, String> requestHeaders = new HashMap<>();
        if (acceptTypes != null && !acceptTypes.isEmpty()) {
            requestHeaders.put("Accept", acceptTypes);
        }

        // Handle OAuth signing if needed
        if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
            oAuthSignLyo(client, url, "DELETE", oAuthCredentials, requestHeaders);
        }

        // Execute the request
        Response response = client.deleteResource(url);
        return response;
    }

    public static Response putDataToUrl(
            String url, TestsBase.UserCredentials creds, String acceptTypes, String contentType, String content)
            throws IOException {
        return putDataToUrl(url, creds, acceptTypes, contentType, content, null);
    }

    public static Response putDataToUrl(
            String url,
            TestsBase.UserCredentials creds,
            String acceptTypes,
            String contentType,
            String content,
            Map<String, String> headers)
            throws IOException {
        OslcClient client = getOslcClient(creds);

        // Prepare request headers
        Map<String, String> requestHeaders = new HashMap<>();
        if (headers != null) {
            requestHeaders.putAll(headers);
        }
        if (contentType != null && !contentType.isEmpty()) {
            requestHeaders.put("Content-Type", contentType);
        }
        if (acceptTypes != null && !acceptTypes.isEmpty()) {
            requestHeaders.put("Accept", acceptTypes);
        }

        // Handle OAuth signing if needed
        if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
            oAuthSignLyo(client, url, "PUT", oAuthCredentials, requestHeaders);
        }

        // Execute the request
        Response response = client.updateResource(url, content, contentType, acceptTypes);
        return response;
    }

    public static Response getDataFromUrl(
            String url, TestsBase.UserCredentials creds, String acceptTypes, String contentType) throws IOException {
        return getDataFromUrl(url, creds, acceptTypes, contentType, null);
    }

    public static Response getDataFromUrl(
            String url,
            TestsBase.UserCredentials creds,
            String acceptTypes,
            String contentType,
            Map<String, String> headers)
            throws IOException {
        OslcClient client = getOslcClient(creds);

        // Prepare request headers
        Map<String, String> requestHeaders = new HashMap<>();
        if (headers != null) {
            requestHeaders.putAll(headers);
        }
        if (contentType != null && !contentType.isEmpty()) {
            requestHeaders.put("Content-Type", contentType);
        }
        if (acceptTypes != null && !acceptTypes.isEmpty()) {
            requestHeaders.put("Accept", acceptTypes);
        }

        // Handle OAuth signing if needed
        if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
            oAuthSignLyo(client, url, "GET", oAuthCredentials, requestHeaders);
        }

        // Execute the request
        Response response = client.getResource(url, requestHeaders);
        return response;
    }

    public static XPath getXPath() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new OSLCNamespaceContext());
        return xpath;
    }

    public static void setupLazySSLSupport() {
        // This method is no longer needed as SSL is configured in the Lyo Client setup
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
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }

    public static void setupFormsAuth(String url, String username, String password) throws IOException {
        // parse the old style of properties file baseUrl so users don't have to modify properties
        // files
        String[] tmpUrls = url.split("/authenticated");
        if (tmpUrls != null && tmpUrls.length > 0) {
            url = tmpUrls[0];
        } // else assume (!) it is the style of URL we want - just scheme://hostname:port/context

        OslcClient client = getOslcClient(null);

        // Prepare request headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        headers.put("OSLC-Core-Version", "2.0");

        // First request to get authrequired
        Response resp = client.getResource(url + "/auth/authrequired", headers);
        int statusCode = resp.getStatus();
        String location = getHeader(resp, "Location");
        resp.readEntity(String.class);
        followRedirects(client, statusCode, location);

        // Second request to get identity
        resp = client.getResource(url + "/authenticated/identity", headers);
        statusCode = resp.getStatus();
        location = getHeader(resp, "Location");
        resp.readEntity(String.class);
        followRedirects(client, statusCode, location);

        // POST to j_security_check
        String formData = "j_username=" + username + "&j_password=" + password;
        resp = client.createResource(url + "/j_security_check", formData, "application/x-www-form-urlencoded", "*/*");
        statusCode = resp.getStatus();

        String jazzAuthMessage = getHeader(resp, JAZZ_AUTH_MESSAGE_HEADER);

        if (jazzAuthMessage != null && jazzAuthMessage.equalsIgnoreCase(JAZZ_AUTH_FAILED)) {
            resp.readEntity(String.class);
            Assertions.fail(
                    "Could not login to Jazz server.  URL: " + url + " user: " + username + "password: " + password);
        } else if (statusCode != 200 && statusCode != 302) {
            resp.readEntity(String.class);
            Assertions.fail("Unknown error logging in to Jazz Server.  Status code: " + statusCode);
        } else // success
        {
            location = getHeader(resp, "Location");
            resp.readEntity(String.class);
            followRedirects(client, statusCode, location);

            // Final request to get initialization data
            resp = client.getResource(
                    url
                            + "/service/com.ibm.team.repository.service.internal.webuiInitializer.IWebUIInitializerRestService/initializationData",
                    headers);
            statusCode = resp.getStatus();
            location = getHeader(resp, "Location");
            resp.readEntity(String.class);
            followRedirects(client, statusCode, location);
        }
        resp.readEntity(String.class);
    }

    private static void followRedirects(OslcClient client, int statusCode, String location) {
        while ((statusCode == 302) && (location != null)) {
            try {
                Response resp = client.getResource(location);
                statusCode = resp.getStatus();
                location = getHeader(resp, "Location");
                resp.readEntity(String.class);
            } catch (Exception e) {
                Assertions.fail(e.getMessage());
            }
        }
    }

    private static String getHeader(Response resp, String headerName) {
        return resp.getHeaderString(headerName);
    }

    /**
     * Adds a query string to the end of a URL, handling the case where the URL already has query parameters.
     *
     * @param url the URL to modify. It may or may not already have query parameters.
     * @param queryString the query string, starting with a '?'. For instance, "?oslc.properties=dcterms%3Aidentifier".
     *     Parameter values must already be encoded.
     * @return the new URL
     */
    public static String addQueryStringToURL(String url, String queryString) {
        Assertions.assertTrue(queryString.startsWith("?"), "queryString must begin with a '?'");
        if (url.indexOf('?') == -1) {
            return url + queryString;
        }

        return url + '&' + queryString.substring(1);
    }

    /**
     * Adds query parameter values to a URL.
     *
     * @param url the URL to modify
     * @param queryParameters a map of query parameters as name/value pairs
     * @return the new URL
     * @throws UnsupportedEncodingException on errors encoding the values
     */
    public static String addParametersToURL(String url, Map<String, String> queryParameters)
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
     * @param url the URL to modify
     * @param name the parameter name
     * @param value the parameter value
     * @return the new URL
     * @throws UnsupportedEncodingException on errors encoding the values
     */
    public static String addParameterToURL(String url, String name, String value) throws UnsupportedEncodingException {
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

    public static String getContentType(Response resp) {
        Assertions.assertNotNull(resp);
        String contentType = resp.getHeaderString("Content-Type");
        if (contentType == null) {
            return null;
        }

        String contentTypeSplit[] = contentType.split(";");
        return contentTypeSplit[0];
    }

    public static Response getResponseFromUrl(
            String base, String url, TestsBase.UserCredentials creds, String contentType) throws IOException {
        return getResponseFromUrl(base, url, creds, contentType, null);
    }

    public static Response getResponseFromUrl(
            String base, String url, TestsBase.UserCredentials creds, String contentType, Map<String, String> headers)
            throws IOException {
        if (creds instanceof TestsBase.UserPassword userPassword) {
            return getResponseFromUrl(base, url, userPassword, contentType, headers);
        } else if (creds instanceof TestsBase.Oauth1UserCredentials oAuthCredentials) {
            return getResponseFromUrl(base, url, oAuthCredentials, contentType, headers);
        } else {
            throw new IllegalArgumentException(
                    "Unknown credentials type: " + creds.getClass().getName());
        }
    }
}
