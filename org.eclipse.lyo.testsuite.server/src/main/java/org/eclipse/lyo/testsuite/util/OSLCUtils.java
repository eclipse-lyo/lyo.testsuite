/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation.
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
 *    Samuel Padgett - add null guards in getContentType()
 *    Samuel Padgett - add support for two-legged OAuth authentication
 *******************************************************************************/
package org.eclipse.lyo.testsuite.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
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
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.oauth.OAuthCredentials;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OSLCUtils {
    private static Logger logger = Logger.getLogger(OSLCUtils.class);
    // HttpClient used for all requests
    public static DefaultHttpClient httpClient = null;
    private static final String JAZZ_AUTH_MESSAGE_HEADER = "X-com-ibm-team-repository-web-auth-msg";
    private static final String JAZZ_AUTH_FAILED = "authfailed";

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
            logger.error(String.format("Exception parsing XML response body:%n%s%n", respBody), e);
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

    public static HttpResponse getResponseFromUrl(
            String baseUrl, String url, Credentials creds, String acceptTypes) throws IOException {
        return getResponseFromUrl(baseUrl, url, creds, acceptTypes, null);
    }

    public static String absoluteUrlFromRelative(String baseUrl, String url)
            throws MalformedURLException {
        URL base = new URL(baseUrl);
        URL result = base;
        if (url != null) result = new URL(base, url);

        return result.toString();
    }

    public static HttpResponse getResponseFromUrl(
            String baseUrl, String url, Credentials creds, String acceptTypes, Header[] headers)
            throws IOException {

        getHttpClient(creds);

        url = absoluteUrlFromRelative(baseUrl, url);
        HttpGet httpget = new HttpGet(url);
        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setStaleConnectionCheckEnabled(true)
                        .setConnectionRequestTimeout(
                                TestsBase.getPropertyInt("timeoutRequest", 1000))
                        .setConnectTimeout(TestsBase.getPropertyInt("timeoutConnect", 500))
                        .setSocketTimeout(TestsBase.getPropertyInt("timeoutSocket", 5000))
                        .build();
        httpget.setConfig(requestConfig);
        // Set headers, accept only the types passed in
        if (headers != null) {
            httpget.setHeaders(headers);
        }
        httpget.setHeader("Accept", acceptTypes);
        oAuthSign(httpget, creds);

        // Get the response and return it
        HttpResponse response = httpClient.execute(httpget);
        return response;
    }

    private static HttpClient getHttpClient(Credentials creds) {
        // If this is our first request, initialized our httpclient
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
            setupLazySSLSupport(httpClient);
            if (creds != null && !(creds instanceof OAuthCredentials)) {
                httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
            } else {
                throw new IllegalArgumentException();
            }

            httpClient.setRedirectStrategy(
                    new DefaultRedirectStrategy() {
                        @Override
                        public boolean isRedirected(
                                HttpRequest request,
                                HttpResponse response,
                                org.apache.http.protocol.HttpContext context) {
                            boolean isRedirect = false;
                            try {
                                isRedirect = super.isRedirected(request, response, context);
                            } catch (ProtocolException e) {
                                e.printStackTrace();
                            }
                            if (!isRedirect) {
                                int responseCode = response.getStatusLine().getStatusCode();
                                if (responseCode == 301 || responseCode == 302) {
                                    return true;
                                }
                            }
                            return isRedirect;
                        }
                    });

            // workaround from
            // https://stackoverflow.com/questions/21800495/invalid-use-of-singleclientconnmanager-connection-still-allocated
            ClientConnectionManager mgr = httpClient.getConnectionManager();
            HttpParams params = httpClient.getParams();

            mgr.closeIdleConnections(3, TimeUnit.SECONDS);
            ThreadSafeClientConnManager conman =
                    new ThreadSafeClientConnManager(mgr.getSchemeRegistry());
            conman.setDefaultMaxPerRoute(6);
            conman.setMaxTotal(256);
            // TODO: switch to this
            //            PoolingHttpClientConnectionManager conman = new
            // PoolingHttpClientConnectionManager();

            httpClient = new DefaultHttpClient(conman, params);
        }
        return httpClient;
    }

    // Sign the request using 2-legged OAuth if necessary.
    private static void oAuthSign(HttpRequestBase request, Credentials creds) {
        if (creds instanceof OAuthCredentials) {
            OAuthServiceProvider provider = new OAuthServiceProvider(null, null, null);
            OAuthConsumer consumer =
                    new OAuthConsumer(
                            "", creds.getUserPrincipal().getName(), creds.getPassword(), provider);
            OAuthAccessor accessor = new OAuthAccessor(consumer);
            accessor.accessToken = "";
            OAuthMessage message;
            try {
                message =
                        accessor.newRequestMessage(
                                request.getMethod(), request.getURI().toString(), null);
                String authHeader = message.getAuthorizationHeader(null);
                request.addHeader("Authorization", authHeader);
            } catch (Exception e) {
                logger.error("Could not OAuth sign request", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static int checkOslcVersion(String url, Credentials creds, String acceptTypes)
            throws ClientProtocolException, IOException {
        getHttpClient(creds);

        HttpGet httpget = new HttpGet(url);
        // Get the response and return it, accept only service provider catalogs & description
        // documents
        httpget.setHeader("Accept", acceptTypes);
        httpget.setHeader("OSLC-Core-Version", "2.0");
        oAuthSign(httpget, creds);
        HttpResponse response = httpClient.execute(httpget);
        EntityUtils.consume(response.getEntity());

        if (response.containsHeader("OSLC-Core-Version")
                && response.getFirstHeader("OSLC-Core-Version").getValue().equals("2.0")) {
            return 2;
        }
        return 1;
    }

    public static HttpResponse postDataToUrl(
            String url, Credentials creds, String acceptTypes, String contentType, String content)
            throws IOException {
        return postDataToUrl(url, creds, acceptTypes, contentType, content, null);
    }

    public static HttpResponse postDataToUrl(
            String url,
            Credentials creds,
            String acceptTypes,
            String contentType,
            String content,
            Header[] headers)
            throws IOException {
        getHttpClient(creds);

        // Create the post and add headers
        HttpPost httppost = new HttpPost(url);
        StringEntity entity = new StringEntity(content);

        httppost.setEntity(entity);
        if (headers != null) httppost.setHeaders(headers);
        if (contentType != null && !contentType.isEmpty())
            httppost.addHeader("Content-Type", contentType);
        if (acceptTypes != null && !acceptTypes.isEmpty())
            httppost.addHeader("Accept", acceptTypes);

        oAuthSign(httppost, creds);

        // Send the request and return the response
        HttpResponse resp = httpClient.execute(httppost);
        return resp;
    }

    public static HttpResponse deleteFromUrl(String url, Credentials creds, String acceptTypes)
            throws IOException {
        getHttpClient(creds);

        // Create the post and add headers
        HttpDelete httpdelete = new HttpDelete(url);

        if (acceptTypes != null && !acceptTypes.isEmpty())
            httpdelete.addHeader("Accept", acceptTypes);

        oAuthSign(httpdelete, creds);
        // Send the request and return the response
        HttpResponse resp = httpClient.execute(httpdelete, new BasicHttpContext());
        return resp;
    }

    public static HttpResponse putDataToUrl(
            String url, Credentials creds, String acceptTypes, String contentType, String content)
            throws IOException {
        return putDataToUrl(url, creds, acceptTypes, contentType, content, null);
    }

    public static HttpResponse putDataToUrl(
            String url,
            Credentials creds,
            String acceptTypes,
            String contentType,
            String content,
            Header[] headers)
            throws IOException {
        getHttpClient(creds);

        // Create the post and add headers
        HttpPut httpput = new HttpPut(url);
        StringEntity entity = new StringEntity(content);

        httpput.setEntity(entity);
        if (headers != null) {
            httpput.setHeaders(headers);
        }
        if (contentType != null && !contentType.isEmpty())
            httpput.addHeader("Content-Type", contentType);
        if (acceptTypes != null && !acceptTypes.isEmpty()) httpput.addHeader("Accept", acceptTypes);

        oAuthSign(httpput, creds);

        // Send the request and return the response
        HttpResponse resp = httpClient.execute(httpput, new BasicHttpContext());
        return resp;
    }

    public static HttpResponse getDataFromUrl(
            String url, Credentials creds, String acceptTypes, String contentType)
            throws ClientProtocolException, IOException {
        return getDataFromUrl(url, creds, acceptTypes, contentType, null);
    }

    public static HttpResponse getDataFromUrl(
            String url, Credentials creds, String acceptTypes, String contentType, Header[] headers)
            throws ClientProtocolException, IOException {
        getHttpClient(creds);

        // Create the post and add headers
        HttpGet httpget = new HttpGet(url);

        if (headers != null) {
            httpget.setHeaders(headers);
        }
        if (contentType != null && !contentType.isEmpty())
            httpget.addHeader("Content-Type", contentType);
        if (acceptTypes != null && !acceptTypes.isEmpty()) httpget.addHeader("Accept", acceptTypes);

        oAuthSign(httpget, creds);

        // Send the request and return the response
        HttpResponse resp = httpClient.execute(httpget, new BasicHttpContext());
        return resp;
    }

    public static XPath getXPath() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new OSLCNamespaceContext());
        return xpath;
    }

    public static void setupLazySSLSupport(HttpClient httpClient) {
        ClientConnectionManager connManager = httpClient.getConnectionManager();
        SchemeRegistry schemeRegistry = connManager.getSchemeRegistry();
        schemeRegistry.unregister("https");
        /** Create a trust manager that does not validate certificate chains */
        TrustManager[] trustAllCerts =
                new TrustManager[] {
                    new X509TrustManager() {
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
                    }
                };

        X509HostnameVerifier verifier =
                new X509HostnameVerifier() {

                    @Override
                    public void verify(String string, SSLSocket ssls) throws IOException {}

                    @Override
                    public void verify(String string, X509Certificate xc) throws SSLException {}

                    @Override
                    public void verify(String string, String[] strings, String[] strings1)
                            throws SSLException {}

                    @Override
                    public boolean verify(String string, SSLSession ssls) {
                        return true;
                    }
                };

        class TLSSocketFactory extends SSLSocketFactory {
            private final javax.net.ssl.SSLSocketFactory socketfactory;

            public TLSSocketFactory(SSLContext sslContext) {
                super(sslContext);
                this.socketfactory = sslContext.getSocketFactory();
            }

            public Socket createSocket() throws IOException {
                SSLSocket socket = (SSLSocket) super.createSocket();
                socket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1"});
                return socket;
            }

            public Socket createSocket(
                    final Socket socket, final String host, final int port, final boolean autoClose)
                    throws IOException, UnknownHostException {
                SSLSocket sslSocket =
                        (SSLSocket) this.socketfactory.createSocket(socket, host, port, autoClose);
                sslSocket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1"});
                getHostnameVerifier().verify(host, sslSocket);
                return sslSocket;
            }
        }

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS"); // $NON-NLS-1$
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            /* Fail Silently */
        } catch (KeyManagementException e) {
            /* Fail Silently */
        }

        SSLSocketFactory sf = new TLSSocketFactory(sc);
        sf.setHostnameVerifier(verifier);
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
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }

    public static void setupFormsAuth(String url, String username, String password)
            throws ClientProtocolException, IOException {
        // If this is our first request, initialized our httpclient
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
            setupLazySSLSupport(httpClient);
        }
        // parse the old style of properties file baseUrl so users don't have to modify properties
        // files
        String[] tmpUrls = url.split("/authenticated");
        if (tmpUrls != null && tmpUrls.length > 0) {
            url = tmpUrls[0];
        } // else assume (!) it is the style of URL we want - just scheme://hostname:port/context

        HttpResponse resp;
        int statusCode = -1;
        String location = null;
        HttpGet get1 = new HttpGet(url + "/auth/authrequired");
        resp = httpClient.execute(get1);
        statusCode = resp.getStatusLine().getStatusCode();
        location = getHeader(resp, "Location");
        EntityUtils.consume(resp.getEntity());
        followRedirects(statusCode, location, httpClient);

        HttpGet get2 = new HttpGet(url + "/authenticated/identity");

        resp = httpClient.execute(get2);
        statusCode = resp.getStatusLine().getStatusCode();
        location = getHeader(resp, "Location");
        EntityUtils.consume(resp.getEntity());
        followRedirects(statusCode, location, httpClient);

        HttpPost httppost = new HttpPost(url + "/j_security_check");
        StringEntity entity =
                new StringEntity("j_username=" + username + "&j_password=" + password);
        // Set headers, accept only the types passed in
        httppost.setHeader("Accept", "*/*");
        httppost.setHeader("X-Requested-With", "XMLHttpRequest");
        httppost.setEntity(entity);
        httppost.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        httppost.addHeader("OSLC-Core-Version", "2.0");
        // Get the response and return it
        resp = httpClient.execute(httppost);
        statusCode = resp.getStatusLine().getStatusCode();

        String jazzAuthMessage = null;
        Header jazzAuthMessageHeader = resp.getLastHeader(JAZZ_AUTH_MESSAGE_HEADER);
        if (jazzAuthMessageHeader != null) {
            jazzAuthMessage = jazzAuthMessageHeader.getValue();
        }

        if (jazzAuthMessage != null && jazzAuthMessage.equalsIgnoreCase(JAZZ_AUTH_FAILED)) {
            EntityUtils.consume(resp.getEntity());
            Assert.fail(
                    "Could not login to Jazz server.  URL: "
                            + url
                            + " user: "
                            + username
                            + "password: "
                            + password);
        } else if (statusCode != HttpStatus.SC_OK
                && statusCode != HttpStatus.SC_MOVED_TEMPORARILY) {
            EntityUtils.consume(resp.getEntity());
            Assert.fail("Unknown error logging in to Jazz Server.  Status code: " + statusCode);
        } else // success
        {
            location = getHeader(resp, "Location");
            EntityUtils.consume(resp.getEntity());
            followRedirects(statusCode, location, httpClient);
            HttpGet get3 =
                    new HttpGet(
                            url
                                    + "/service/com.ibm.team.repository.service.internal.webuiInitializer.IWebUIInitializerRestService/initializationData");
            resp = httpClient.execute(get3);
            statusCode = resp.getStatusLine().getStatusCode();
            location = getHeader(resp, "Location");
            EntityUtils.consume(resp.getEntity());
            followRedirects(statusCode, location, httpClient);
        }
        EntityUtils.consume(resp.getEntity());
    }

    private static void followRedirects(int statusCode, String location, HttpClient httpClient) {

        while ((statusCode == HttpStatus.SC_MOVED_TEMPORARILY) && (location != null)) {
            HttpGet get = new HttpGet(location);
            try {
                HttpResponse newResp = httpClient.execute(get);
                statusCode = newResp.getStatusLine().getStatusCode();
                location = getHeader(newResp, "Location");
                EntityUtils.consume(newResp.getEntity());
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    private static String getHeader(HttpResponse resp, String headerName) {
        String retval = null;
        Header header = resp.getFirstHeader(headerName);
        if (header != null) retval = header.getValue();
        return retval;
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
        Assert.assertTrue("queryString must begin with a '?'", queryString.startsWith("?"));
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
     * @param queryParameters
     *            a map of query parameters as name/value pairs
     * @return the new URL
     * @throws UnsupportedEncodingException
     *             on errors encoding the values
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

    public static String getContentType(HttpResponse resp) {
        Assert.assertNotNull(resp);
        HttpEntity entity = resp.getEntity();
        if (entity == null) {
            return null;
        }

        Header h = entity.getContentType();
        if (h == null) {
            return null;
        }

        String contentType = h.getValue();
        String contentTypeSplit[] = contentType.split(";");
        return contentTypeSplit[0];
    }
}
