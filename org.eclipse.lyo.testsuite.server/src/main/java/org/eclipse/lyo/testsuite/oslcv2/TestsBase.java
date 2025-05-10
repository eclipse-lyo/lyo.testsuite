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
 *    Steve Speicher - initial API and implementation
 *    Tim Eck II     - asset management test cases
 *    Yuhong Yin
 *    Samuel Padgett - create and update resources using shapes
 *    Samuel Padgett - don't cache query shapes for creation when the URIs are the same
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONArtifact;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.eclipse.lyo.testsuite.util.SetupProperties;
import org.eclipse.lyo.testsuite.util.oauth.OAuthConsumerPrincipal;
import org.eclipse.lyo.testsuite.util.oauth.OAuthCredentials;
import org.junit.BeforeClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class TestsBase {
    private static Logger logger = Logger.getLogger(TestsBase.class);

    public enum AuthMethods {
        BASIC,
        FORM,
        OAUTH
    };

    protected static Credentials creds;
    protected static boolean onlyOnce = true;
    protected static boolean useDefaultUsageForCreation = true;
    protected static Properties setupProps = null;
    protected static String xmlCreateTemplate;
    protected static String xmlUpdateTemplate;
    protected static String rdfXmlCreateTemplate;
    protected static String rdfXmlUpdateTemplate;
    protected static String jsonCreateTemplate;
    protected static String jsonUpdateTemplate;
    protected static String updateParams;
    protected static String useThisServiceProvider;
    protected static Header[] headers;
    protected static AuthMethods authMethod = AuthMethods.BASIC;
    protected static String implName;

    protected static String currentUrl = null; // URL of current service being tested
    protected static String setupBaseUrl =
            null; // Configuration baseUrl, think ServiceProvider or ServiceProviderCatalog
    protected static String shapeUrl = null; // Shape for the creation dialog

    protected static String testVersion = null;

    protected static Map<String, String> creationShapeMap = new HashMap<>();

    public TestsBase(String thisUrl) {
        currentUrl = thisUrl;
    }

    public static void staticSetup() {

        if (setupProps == null) {
            setupProps = SetupProperties.setup(null);
            implName = setupProps.getProperty("implName");
            updateParams = setupProps.getProperty("updateParams");
            String userId = setupProps.getProperty("userId");
            String pw = setupProps.getProperty("pw");
            if (userId != null && pw != null) {
                creds = new UsernamePasswordCredentials(userId, pw);
            } else {
                String consumerKey = setupProps.getProperty("consumerKey");
                String consumerSecret = setupProps.getProperty("consumerSecret");
                if (consumerKey != null && consumerSecret != null) {
                    creds =
                            new OAuthCredentials(
                                    new OAuthConsumerPrincipal(consumerKey), consumerSecret);
                } else {
                    logger.warn("No credentials found in setup.properties");
                }
            }

            Header h = new BasicHeader("OSLC-Core-Version", "2.0");

            headers = new Header[] {h};
            String onlyOnceStr = setupProps.getProperty("runOnlyOnce");
            if (onlyOnceStr != null && onlyOnceStr.equals("false")) {
                onlyOnce = false;
            }
            String defUsageStr = setupProps.getProperty("useDefaultUsageForCreation");
            if (defUsageStr != null && defUsageStr.equals("false")) {
                useDefaultUsageForCreation = false;
            }
            setupBaseUrl = setupProps.getProperty("baseUri");
            currentUrl = setupBaseUrl;

            String authType = setupProps.getProperty("authMethod");
            if (authType.equalsIgnoreCase("OAUTH")) {
                authMethod = AuthMethods.OAUTH;
            } else if (authType.equalsIgnoreCase("FORM")) {
                authMethod = AuthMethods.FORM;
                formLogin(userId, pw);
            }

            useThisServiceProvider = setupProps.getProperty("useThisServiceProvider");

            // First, Setup plain old XML
            String fileName = setupProps.getProperty("createTemplateXmlFile");
            if (fileName != null) xmlCreateTemplate = OSLCUtils.readFileByNameAsString(fileName);
            fileName = setupProps.getProperty("updateTemplateXmlFile");
            if (fileName != null) xmlUpdateTemplate = OSLCUtils.readFileByNameAsString(fileName);
            // Now RDF/XML
            fileName = setupProps.getProperty("createTemplateRdfXmlFile");
            if (fileName != null) rdfXmlCreateTemplate = OSLCUtils.readFileByNameAsString(fileName);
            fileName = setupProps.getProperty("updateTemplateRdfXmlFile");
            if (fileName != null) rdfXmlUpdateTemplate = OSLCUtils.readFileByNameAsString(fileName);
            // Now JSON
            fileName = setupProps.getProperty("createTemplateJsonFile");
            if (fileName != null) jsonCreateTemplate = OSLCUtils.readFileByNameAsString(fileName);
            fileName = setupProps.getProperty("updateTemplateJsonFile");
            if (fileName != null) jsonUpdateTemplate = OSLCUtils.readFileByNameAsString(fileName);
            // Now handle if RDF/XML wasn't given
            if (rdfXmlCreateTemplate == null) rdfXmlCreateTemplate = xmlCreateTemplate;
            if (rdfXmlUpdateTemplate == null) rdfXmlUpdateTemplate = xmlUpdateTemplate;

            // Acquire the test version (v1/v2)
            testVersion = setupProps.getProperty("testVersions");
        }
    }

    public static Object getProperty(String propName) {
        return setupProps.get(propName);
    }

    public static int getPropertyInt(String propName, int defaultValue) {
        Object property = getProperty(propName);
        if (property == null) {
            return defaultValue;
        } else {
            return Integer.parseInt((String) property);
        }
    }

    @BeforeClass
    public static void setup()
            throws IOException, ParserConfigurationException, SAXException, XPathException {
        staticSetup();
    }

    public static ArrayList<String> getServiceProviderURLsUsingXML(String inBaseURL)
            throws IOException, XPathException, ParserConfigurationException, SAXException {
        return getServiceProviderURLsUsingXML(inBaseURL, onlyOnce);
    }

    public static Collection<Object[]> toCollection(ArrayList<String> list) {
        Collection<Object[]> data = new ArrayList<>();
        for (String string : list) {
            data.add(new Object[] {string});
        }
        return data;
    }

    public static ArrayList<String> getServiceProviderURLsUsingXML(
            String inBaseURL, boolean dontGoDeep)
            throws IOException, XPathException, ParserConfigurationException, SAXException {

        staticSetup();

        // ArrayList to contain the urls from all SPCs
        ArrayList<String> data = new ArrayList<>();

        // If we are given a shortcut, then use it and skip the rest
        if (useThisServiceProvider != null && useThisServiceProvider.length() > 0) {
            data.add(useThisServiceProvider);
            return data;
        }

        String base = null;
        if (inBaseURL == null) base = setupBaseUrl;
        else base = inBaseURL;
        HttpResponse resp =
                OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_XML, headers);

        String responseBody = EntityUtils.toString(resp.getEntity());
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Reading service catalog <%s>", base));
            logger.debug(responseBody);
        }
        Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(responseBody);

        // Get all ServiceProvider urls from the base document in order to
        // recursively add all the capability urls from them as well.
        //   Inlined using oslc:ServiceProvider/@rdf:about
        NodeList sps =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate(
                                        "//oslc_v2:ServiceProvider/@rdf:about",
                                        baseDoc,
                                        XPathConstants.NODESET);
        for (int i = 0; i < sps.getLength(); i++) {
            if (!sps.item(i).getNodeValue().equals(base) || sps.getLength() == 1) {
                data.add(sps.item(i).getNodeValue());
                if (onlyOnce) return data;

                if (dontGoDeep) return data;
            }
        }

        // Get all ServiceProvider urls from the base document in order to
        // recursively add all the capability urls from them as well.
        //   Referenced using oslc:serviceProvider/@rdf:resource
        sps =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate(
                                        "//oslc_v2:serviceProvider/@rdf:resource",
                                        baseDoc,
                                        XPathConstants.NODESET);
        for (int i = 0; i < sps.getLength(); i++) {
            if (!sps.item(i).getNodeValue().equals(base) || sps.getLength() == 1) {
                data.add(sps.item(i).getNodeValue());
                if (dontGoDeep) return data;
            }
        }

        // Get all ServiceProviderCatalog urls from the base document in order
        // to recursively add all the capability from ServiceProviders within them as well.
        //   Inlined using oslc:ServiceProviderCatalog/@rdf:about
        NodeList spcs =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate(
                                        "//oslc_v2:ServiceProviderCatalog/@rdf:about",
                                        baseDoc,
                                        XPathConstants.NODESET);
        for (int i = 0; i < spcs.getLength(); i++) {
            if (!spcs.item(i).getNodeValue().equals(base)) {
                ArrayList<String> subCollection =
                        getServiceProviderURLsUsingXML(spcs.item(i).getNodeValue(), dontGoDeep);
                for (String subUri : subCollection) {
                    data.add(subUri);
                    if (dontGoDeep) return data;
                }
            }
        }
        // Get all ServiceProviderCatalog urls from the base document in order
        // to recursively add all the capability from ServiceProviders within them as well.
        //   Referenced using oslc:serviceProviderCatalog/@rdf:resource
        spcs =
                (NodeList)
                        OSLCUtils.getXPath()
                                .evaluate(
                                        "//oslc_v2:serviceProviderCatalog/@rdf:resource",
                                        baseDoc,
                                        XPathConstants.NODESET);
        for (int i = 0; i < spcs.getLength(); i++) {
            if (!spcs.item(i).getNodeValue().equals(base)) {
                ArrayList<String> subCollection =
                        getServiceProviderURLsUsingXML(spcs.item(i).getNodeValue(), dontGoDeep);
                for (String subUri : subCollection) {
                    data.add(subUri);
                    if (dontGoDeep) return data;
                }
            }
        }

        return data;
    }

    public static ArrayList<Node> getCapabilityDOMNodesUsingXML(
            String xpathStmt, ArrayList<String> serviceUrls)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {
        // Collection to contain the creationFactory urls from all SPs
        ArrayList<Node> data = new ArrayList<>();

        for (String base : serviceUrls) {
            HttpResponse resp =
                    OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_XML, headers);

            Document baseDoc =
                    OSLCUtils.createXMLDocFromResponseBody(EntityUtils.toString(resp.getEntity()));

            NodeList sDescs =
                    (NodeList)
                            OSLCUtils.getXPath()
                                    .evaluate(xpathStmt, baseDoc, XPathConstants.NODESET);
            for (int i = 0; i < sDescs.getLength(); i++) {
                data.add(sDescs.item(i));
                if (onlyOnce) return data;
            }
        }
        return data;
    }

    public static ArrayList<String> getCapabilityURLsUsingXML(
            String xpathStmt, ArrayList<String> serviceUrls, boolean useDefaultUsage)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {

        return getCapabilityURLsUsingXML(
                xpathStmt,
                "//oslc_v2:usage/@rdf:resource",
                OSLCConstants.USAGE_DEFAULT_URI,
                serviceUrls,
                useDefaultUsage);
    }

    public static ArrayList<String> getCapabilityURLsUsingXML(
            String xpathStmt,
            String xpathSubStmt,
            String rT,
            ArrayList<String> serviceUrls,
            boolean useDefaultUsage)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException {

        // Collection to contain the creationFactory urls from all SPs
        ArrayList<String> data = new ArrayList<>();
        String firstUrl = null;

        for (String base : serviceUrls) {
            HttpResponse resp =
                    OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_XML, headers);

            try {
                if (resp.getStatusLine().getStatusCode() > 299) {
                    logger.error(
                            "Failed to fetch a resource: "
                                    + resp.getStatusLine().getReasonPhrase());
                    throw new IllegalStateException();
                }

                String responseBody = EntityUtils.toString(resp.getEntity());
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Reading service provider document <%s>", base));
                    logger.debug(responseBody);
                }

                Document baseDoc = OSLCUtils.createXMLDocFromResponseBody(responseBody);

                NodeList sDescs =
                        (NodeList)
                                OSLCUtils.getXPath()
                                        .evaluate(xpathStmt, baseDoc, XPathConstants.NODESET);

                for (int i = 0; i < sDescs.getLength(); i++) {
                    if (firstUrl == null) firstUrl = sDescs.item(i).getNodeValue();

                    if (useDefaultUsage) {
                        NodeList usages =
                                (NodeList)
                                        OSLCUtils.getXPath()
                                                .evaluate(
                                                        xpathSubStmt,
                                                        sDescs.item(i),
                                                        XPathConstants.NODESET);

                        for (int u = 0; u < usages.getLength(); u++) {
                            String usageValue = usages.item(u).getNodeValue();
                            // if (OSLCConstants.USAGE_DEFAULT_URI.equals(usageValue)) {
                            // if (rT.equals(usageValue) && (u ==i)) {
                            if (rT.contains(usageValue) && (u == i)) {
                                data.add(sDescs.item(i).getNodeValue());
                                return data;
                            }
                        }
                    } else {
                        data.add(sDescs.item(i).getNodeValue());
                        if (onlyOnce) return data;
                    }
                }
            } finally {
                EntityUtils.consume(resp.getEntity());
            }
        }
        // If we didn't find the default, then just send back the first one we
        // found.
        if (useDefaultUsage && firstUrl != null) data.add(firstUrl);
        return data;
    }

    public static ArrayList<String> getServiceProviderURLsUsingRdfXml(
            String inBaseURL, boolean dontGoDeep) throws IOException {
        staticSetup();

        // ArrayList to contain the urls from all SPCs
        ArrayList<String> data = new ArrayList<>();

        // If we are given a shortcut, then use it and skip the rest
        if (useThisServiceProvider != null && useThisServiceProvider.length() > 0) {
            data.add(useThisServiceProvider);
            return data;
        }

        HttpResponse resp =
                OSLCUtils.getResponseFromUrl(
                        setupBaseUrl, inBaseURL, creds, OSLCConstants.CT_RDF, headers);
        assertEquals(
                "Did not successfully retrieve ServiceProviders at: " + inBaseURL,
                HttpStatus.SC_OK,
                resp.getStatusLine().getStatusCode());

        // Used to hold RDF from doing service discovery
        Model spModel = ModelFactory.createDefaultModel();
        spModel.read(
                resp.getEntity().getContent(),
                OSLCUtils.absoluteUrlFromRelative(setupBaseUrl, inBaseURL),
                OSLCConstants.JENA_RDF_XML);
        EntityUtils.consume(resp.getEntity());
        RDFUtils.validateModel(spModel);

        // Get all the "inlined" definitions for Service Providers, namely
        // all subjects whose rdf:type = oslc:ServiceProvider
        Property rdfType = spModel.createProperty(OSLCConstants.RDF_TYPE_PROP);
        Resource spTypeRes = spModel.getResource(OSLCConstants.SERVICE_PROVIDER_TYPE);
        Selector select = new SimpleSelector(null, rdfType, spTypeRes);
        StmtIterator statements = spModel.listStatements(select);
        // Since resources can have multiple types, iterate over all
        while (statements.hasNext()) {
            Statement st = statements.nextStatement();
            data.add(st.getSubject().getURI());
            if (dontGoDeep) return data;
        }

        // Get all the "referenced" definitions for Service Providers, namely
        // of form: <oslc:serviceProvider rdf:resource="url" />
        Property spProp = spModel.createProperty(OSLCConstants.SERVICE_PROVIDER_PROP);
        select = new SimpleSelector(null, spProp, (RDFNode) null);
        statements = spModel.listStatements(select);
        // Since resources can have multiple types, iterate over all
        while (statements.hasNext()) {
            Statement st = statements.nextStatement();
            data.add(st.getObject().toString());
            if (dontGoDeep) return data;
        }

        // Chase any ServiceProviderCatalogs, looking for ServiceProviders definitions.
        Property spcPredicate = spModel.createProperty(OSLCConstants.SERVICE_PROVIDER_CATALOG_PROP);
        select = new SimpleSelector(null, spcPredicate, (RDFNode) null);
        statements = spModel.listStatements(select);
        while (statements.hasNext()) {
            ArrayList<String> results =
                    getServiceProviderURLsUsingRdfXml(
                            statements.nextStatement().getObject().toString(), dontGoDeep);
            data.addAll(results);
            if (dontGoDeep) return data;
        }

        return data;
    }

    public static ArrayList<String> getCapabilityURLsUsingRdfXml(
            String propertyUri, ArrayList<String> serviceUrls, boolean useDefaultUsage)
            throws IOException {
        return getCapabilityURLsUsingRdfXml(
                propertyUri,
                serviceUrls,
                useDefaultUsage,
                null,
                OSLCConstants.USAGE_PROP,
                OSLCConstants.USAGE_DEFAULT_URI);
    }

    public static ArrayList<String> getCapabilityURLsUsingRdfXml(
            String propertyUri,
            ArrayList<String> serviceUrls,
            boolean useDefaultUsage,
            String[] types)
            throws IOException {
        return getCapabilityURLsUsingRdfXml(
                propertyUri,
                serviceUrls,
                useDefaultUsage,
                types,
                OSLCConstants.USAGE_PROP,
                OSLCConstants.USAGE_DEFAULT_URI);
    }

    public static ArrayList<String> getCapabilityURLsUsingRdfXml(
            String propertyUri,
            ArrayList<String> serviceUrls,
            boolean useDefaultUsage,
            String[] types,
            String prop,
            String eval)
            throws IOException {
        // Collection to contain the creationFactory urls from all SPs
        ArrayList<String> data = new ArrayList<>();
        String firstUrl = null;
        for (String base : serviceUrls) {
            HttpResponse resp =
                    OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_RDF, headers);

            try {
                if (resp.getStatusLine().getStatusCode() > 299) {
                    throw new IllegalStateException(
                            "Request failed: " + resp.getStatusLine().getReasonPhrase());
                }
                Model spModel = ModelFactory.createDefaultModel();
                spModel.read(resp.getEntity().getContent(), base, OSLCConstants.JENA_RDF_XML);
                RDFUtils.validateModel(spModel);

                Property capProp = spModel.createProperty(propertyUri);
                Property usageProp = spModel.createProperty(prop);
                Selector select = new SimpleSelector(null, capProp, (RDFNode) null);
                StmtIterator statements = spModel.listStatements(select);

                while (statements.hasNext()) {
                    Statement stmt = statements.nextStatement();
                    // Only cache creation factory shapes. Some providers use the same capability
                    // URI for both query and creation.
                    if (OSLCConstants.CREATION_PROP.equals(propertyUri)) {
                        Statement shape =
                                stmt.getSubject()
                                        .getProperty(
                                                spModel.createProperty(
                                                        OSLCConstants.RESOURCE_SHAPE_PROP));
                        if (shape != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        String.format(
                                                "Caching shape URI <%s> for capability URI <%s>",
                                                shape.getObject().toString(),
                                                stmt.getObject().toString()));
                            }
                            creationShapeMap.put(
                                    stmt.getObject().toString(), shape.getObject().toString());
                        }
                    }

                    if (firstUrl == null) firstUrl = stmt.getObject().toString();
                    if (useDefaultUsage) {
                        StmtIterator usages = stmt.getSubject().listProperties(usageProp);

                        while (usages.hasNext()) {

                            Statement usageStmt = usages.nextStatement();

                            if (eval.equals(usageStmt.getObject().toString())) {
                                data.add(stmt.getObject().toString());
                                return data;
                            }
                        }
                    } else {
                        // Now if we have types, we match the capability for the given types
                        if (types != null && types.length > 0) {
                            Property typeProp =
                                    spModel.getProperty(OSLCConstants.RESOURCE_TYPE_PROP);
                            StmtIterator typeIter = stmt.getSubject().listProperties(typeProp);
                            while (typeIter.hasNext()) {
                                String typeName = typeIter.nextStatement().getObject().toString();
                                for (String t : types) {
                                    if (t.equals(typeName)) {
                                        data.add(stmt.getObject().toString());
                                        if (onlyOnce) return data;
                                    }
                                }
                            }
                        } else {
                            data.add(stmt.getObject().toString());
                            if (onlyOnce) return data;
                        }
                    }
                }
            } finally {
                EntityUtils.consume(resp.getEntity());
            }
        }
        // If no default usage was found, then just return first one
        if (useDefaultUsage && firstUrl != null) data.add(firstUrl);
        return data;
    }

    /*
     * Return the cached shape URI associated with this creation factory URI. You must
     * have previously called getCapabilityURLsUsingRdfXml().
     */
    public static String getShapeUriForCreation(String creationFactoryUri) {
        return creationShapeMap.get(creationFactoryUri);
    }

    public static boolean formLogin(String userId, String pw) {
        String formUri = setupProps.getProperty("formUri");
        // Get cookies for forms login procedure (ie: get redirected to login
        // page.
        HttpResponse resp;
        try {
            resp = OSLCUtils.getResponseFromUrl(setupBaseUrl, setupBaseUrl, null, "*/*");
            if (resp.getEntity() != null) {
                EntityUtils.consume(resp.getEntity());
            }
            // Post info to forms auth page
            OSLCUtils.setupFormsAuth(formUri, userId, pw);
        } catch (ClientProtocolException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static ArrayList<String> getServiceProviderURLsUsingJson(String inBaseURL)
            throws IOException,
                    XPathException,
                    ParserConfigurationException,
                    SAXException,
                    JSONException {
        return getServiceProviderURLsUsingJson(inBaseURL, onlyOnce);
    }

    public static ArrayList<String> getServiceProviderURLsUsingJson(
            String inBaseURL, boolean dontGoDeep)
            throws IOException,
                    XPathException,
                    ParserConfigurationException,
                    SAXException,
                    JSONException {

        staticSetup();

        // ArrayList to contain the urls from all SPCs
        ArrayList<String> data = new ArrayList<>();

        String base = null;
        if (inBaseURL == null) base = setupBaseUrl;
        else base = inBaseURL;

        // If we are given a shortcut, then use it and skip the rest
        if (useThisServiceProvider != null && useThisServiceProvider.length() > 0) {
            data.add(useThisServiceProvider);
            return data;
        }

        HttpResponse resp =
                OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_JSON, headers);
        assertEquals(
                "Failed to retrieve ServiceProviders at: " + inBaseURL,
                HttpStatus.SC_OK,
                resp.getStatusLine().getStatusCode());

        String respBody = EntityUtils.toString(resp.getEntity());

        // Create mapping of JSON variables
        JSONArtifact userData = JSON.parse(respBody);
        JSONObject resultJson = null;
        if (userData instanceof JSONArtifact) {
            resultJson = (JSONObject) userData;
        }

        JSONArray results = (JSONArray) resultJson.get("oslc:serviceProvider");

        // Now walk through the array to get a list of service providers
        for (int i = 0; i < results.length(); i++) {
            JSONObject serviceProviderJSON = (JSONObject) results.get(i);
            String serviceProvider = (String) serviceProviderJSON.get("rdf:about");
            data.add(serviceProvider);
        }

        return data;
    }

    public static ArrayList<String> getCapabilityURLsUsingJson(
            String xpathStmt, ArrayList<String> serviceUrls, boolean useDefaultUsage)
            throws IOException,
                    ParserConfigurationException,
                    SAXException,
                    XPathExpressionException,
                    NullPointerException,
                    JSONException {

        // Collection to contain the creationFactory urls from all SPs
        ArrayList<String> data = new ArrayList<>();

        for (String base : serviceUrls) {
            HttpResponse resp =
                    OSLCUtils.getResponseFromUrl(base, base, creds, OSLCConstants.CT_JSON, headers);
            assertEquals(
                    "Failed to retrieve ServiceProviders at: " + base,
                    HttpStatus.SC_OK,
                    resp.getStatusLine().getStatusCode());

            String respBody = EntityUtils.toString(resp.getEntity());

            String contentType = OSLCUtils.getContentType(resp);

            assertEquals(
                    "Expected content-type "
                            + OSLCConstants.CT_JSON
                            + " but received "
                            + contentType,
                    OSLCConstants.CT_JSON,
                    contentType);

            // Create mapping of JSON variables
            JSONArtifact userData = JSON.parse(respBody);
            JSONObject resultJson = null;
            if (userData instanceof JSONArtifact) {
                resultJson = (JSONObject) userData;
            }
            JSONArray s = (JSONArray) resultJson.get("oslc:service");

            for (int i = 0; i < s.length(); i++) {
                JSONObject serviceProviderJson = (JSONObject) s.get(i);
                try {
                    JSONArray u = (JSONArray) serviceProviderJson.get("oslc:queryCapability");
                    JSONObject u1 = (JSONObject) u.get(0);

                    JSONObject q = (JSONObject) u1.get("oslc:queryBase");
                    String queryBase = q.getString("rdf:resource");

                    data.add(queryBase);
                } catch (JSONException e) {
                    // ignore
                }
            }
        }

        return data;
    }
}
