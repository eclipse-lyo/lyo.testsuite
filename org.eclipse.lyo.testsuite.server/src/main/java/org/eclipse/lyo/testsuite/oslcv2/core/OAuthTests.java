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
 *    Steve Speicher
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient3.HttpClient3;
import net.oauth.client.httpclient3.HttpClientPool;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.SSLProtocolSocketFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Tests the process by which consumers are able to access resources of an OSLC
 * Provider using OAuth authentication to access the provider's resources.
 *
 */
@RunWith(Parameterized.class)
public class OAuthTests extends TestsBase {

    private HttpClient c;
    private OAuthServiceProvider provider;
    private OAuthConsumer consumer;
    private String requestUrl;
    private String authorizeUrl;
    private String accessUrl;
    private String postParameters;
    private String consumerSecret;
    private String consumerToken;

    public OAuthTests(String baseUrl, String requestUrl, String authorizeUrl, String accessUrl)
            throws GeneralSecurityException {
        super(baseUrl);
        // Setup the HTTP client to properly handle SSL requests
        Protocol.registerProtocol(
                "https", new Protocol("https", SSLProtocolSocketFactory.INSTANCE, 443));
        c = new HttpClient();
        this.requestUrl = requestUrl;
        this.authorizeUrl = authorizeUrl;
        this.accessUrl = accessUrl;
    }

    @BeforeClass
    public void mysetup()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        postParameters = setupProps.getProperty("OAuthAuthorizationParameters");
        // Setup the OAuth provider
        provider = new OAuthServiceProvider(requestUrl, authorizeUrl, accessUrl);
        // Setup the OAuth consumer
        consumerSecret = setupProps.getProperty("OAuthConsumerSecret");
        consumerToken = setupProps.getProperty("OAuthConsumerToken");
        consumer = new OAuthConsumer("", consumerToken, consumerSecret, provider);
    }

    @Parameters
    public static Collection<Object[]> getAllDescriptionUrls()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        // Checks the ServiceProviderCatalog at the specified baseUrl of the REST service in order
        // to grab all urls
        // to other creation factories contained within it, recursively, in order to find the URLs
        // of all
        // creation factories of the REST service.
        String v = "//oslc_v2:OAuthConfiguration";
        ArrayList<String> serviceUrls = getServiceProviderURLsUsingXML(null);
        return getReferencedUrls(TestsBase.getCapabilityDOMNodesUsingXML(v, serviceUrls), null);
    }

    public static Collection<Object[]> getReferencedUrls(
            ArrayList<Node> capabilityDOMNodesUsingXML, String base)
            throws IOException, XPathException, ParserConfigurationException, SAXException {
        // ArrayList to contain the urls from all SPCs
        Collection<Object[]> data = new ArrayList<Object[]>();

        String requestTokenUri = "";
        String authorizationUri = "";
        String accessTokenUri = "";
        for (Node node : capabilityDOMNodesUsingXML) {
            NodeList oAuthChildren = node.getChildNodes();
            requestTokenUri = null;
            authorizationUri = null;
            accessTokenUri = null;
            for (int j = 0; j < oAuthChildren.getLength(); j++) {
                Node oAuthNode = oAuthChildren.item(j);
                if (oAuthNode.getLocalName() == null) continue;
                NamedNodeMap attribs = oAuthNode.getAttributes();
                if (oAuthNode.getNamespaceURI().equals(OSLCConstants.OSLC_V2)) {
                    if (oAuthNode.getLocalName().equals("oauthRequestTokenURI")) {
                        requestTokenUri =
                                attribs.getNamedItemNS(OSLCConstants.RDF, "resource")
                                        .getNodeValue();
                    } else if (oAuthNode.getLocalName().equals("authorizationURI")) {
                        authorizationUri =
                                attribs.getNamedItemNS(OSLCConstants.RDF, "resource")
                                        .getNodeValue();
                    } else if (oAuthNode.getLocalName().equals("oauthAccessTokenURI")) {
                        accessTokenUri =
                                attribs.getNamedItemNS(OSLCConstants.RDF, "resource")
                                        .getNodeValue();
                    }
                }
            }
            if (requestTokenUri != null && authorizationUri != null && accessTokenUri != null)
                data.add(new Object[] {base, requestTokenUri, authorizationUri, accessTokenUri});
        }

        // If service provider didn't provide OAuth parameters, see if they
        // were provided in test configuration parameters.
        if (data.isEmpty()) {
            requestTokenUri = setupProps.getProperty("OAuthRequestTokenUrl");
            authorizationUri = setupProps.getProperty("OAuthAuthorizationUrl");
            accessTokenUri = setupProps.getProperty("OAuthAccessTokenUrl");
            if (requestTokenUri != null && authorizationUri != null && accessTokenUri != null)
                data.add(new Object[] {base, requestTokenUri, authorizationUri, accessTokenUri});
        }
        return data;
    }

    @Test
    public void oAuthRequestTokenProperlyRecieved()
            throws IOException, OAuthException, URISyntaxException {
        // Setup the client using our HttpClient that is set up with SSL.
        OAuthClient client = new OAuthClient(new HttpClient3(new TestHttpClientPool()));
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        // Attempt to get a request token from the provider
        client.getRequestToken(accessor);
        // Make sure we got the request token
        assertNotNull(accessor.requestToken);
        assertFalse(accessor.requestToken.isEmpty());
    }

    @Test
    public void oAuthAuthorizationHandled() throws IOException, OAuthException, URISyntaxException {
        // Setup the client using our HttpClient that is set up with SSL.
        OAuthClient client = new OAuthClient(new HttpClient3(new TestHttpClientPool()));
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        client.getRequestToken(accessor);
        // Get a response from the base URL to setup cookies (to prevent form redirection)
        HttpResponse resp =
                OSLCUtils.getResponseFromUrl("", setupBaseUrl, creds, OSLCConstants.CT_XML);
        EntityUtils.consume(resp.getEntity());
        // Post authorization using user credentials provided.
        resp =
                OSLCUtils.postDataToUrl(
                        provider.userAuthorizationURL + "?oauth_token=" + accessor.requestToken,
                        creds,
                        "",
                        "application/x-www-form-urlencoded",
                        postParameters + "&oauth_token=" + accessor.requestToken,
                        headers);
        EntityUtils.consume(resp.getEntity());
        int statusCode = resp.getStatusLine().getStatusCode();
        assertTrue("Request failed with status code: " + statusCode, statusCode < 400);
    }

    @Test
    public void oAuthAccessTokenReceived() throws OAuthException, IOException, URISyntaxException {
        OAuthClient client = new OAuthClient(new HttpClient3(new TestHttpClientPool()));
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        client.getRequestToken(accessor);

        HttpResponse resp =
                OSLCUtils.getResponseFromUrl("", setupBaseUrl, creds, OSLCConstants.CT_XML);
        EntityUtils.consume(resp.getEntity());

        resp =
                OSLCUtils.postDataToUrl(
                        provider.userAuthorizationURL + "?oauth_token=" + accessor.requestToken,
                        creds,
                        "",
                        "application/x-www-form-urlencoded",
                        postParameters + "&oauth_token=" + accessor.requestToken,
                        headers);
        EntityUtils.consume(resp.getEntity());
        int sc = resp.getStatusLine().getStatusCode();
        assertTrue(sc == HttpStatus.SC_OK || sc == HttpStatus.SC_CREATED);
        try {
            // Trade the request token for an access token.
            client.getAccessToken(accessor, OAuthMessage.POST, null);
        } catch (OAuthProblemException e) {
            Assert.fail("Exception while requesting access token: " + e.getMessage());
        }
        // Make sure we got an access token that is not empty.
        assertNotNull(accessor.accessToken);
        assertFalse(accessor.accessToken.isEmpty());
    }

    private class TestHttpClientPool implements HttpClientPool {
        @Override
        public org.apache.commons.httpclient.HttpClient getHttpClient(URL arg0) {
            return c;
        }
    }
}
