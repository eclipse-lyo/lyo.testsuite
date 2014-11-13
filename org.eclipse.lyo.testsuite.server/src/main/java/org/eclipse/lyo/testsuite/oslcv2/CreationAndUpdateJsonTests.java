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
 *    Samuel Padgett - add support for creating and updating resource using shapes
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.eclipse.lyo.testsuite.server.util.OSLCUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

/**
 * This class provides JUnit tests for the validation of the OSLCv2 creation and
 * updating of change requests. It uses the template files specified in
 * setup.properties as the entity to be POST or PUT, for creation and updating
 * respectively. If these files are not defined, it uses the resource shapes for
 * the creation factory.
 * 
 * After each test, it attempts to perform a DELETE call on the resource that
 * was presumably created, but this DELETE call is not technically required in
 * the OSLC spec, so the created change request may still exist for some service
 * providers.
 */
@RunWith(Parameterized.class)
public class CreationAndUpdateJsonTests extends CreationAndUpdateBaseTests {

	private HashMap<String, String> namespacePrefixMap = new HashMap<String, String>();
	private static final HashMap<String, String> KNOWN_PREFIXES = new HashMap<String, String>();
	static {
		KNOWN_PREFIXES.put(OSLCConstants.DC, "dcterms");
		KNOWN_PREFIXES.put(OSLCConstants.RDF, "rdf");
		KNOWN_PREFIXES.put(OSLCConstants.RDFS, "rdfs");
		KNOWN_PREFIXES.put(OSLCConstants.OSLC_V2, "oslc");
		KNOWN_PREFIXES.put(OSLCConstants.OSLC_CM_V2, "oslc_cm");
	}

	public CreationAndUpdateJsonTests(String url) {
		super(url);
	}
	
	@Parameters
	public static Collection<Object[]> getAllDescriptionUrls()
			throws IOException, ParserConfigurationException, SAXException,
			XPathException, JSONException {
		ArrayList<String> serviceUrls = getServiceProviderURLsUsingJson(null);

		// TODO: We should use JSON for this.
		return toCollection(getCapabilityURLsUsingRdfXml(OSLCConstants.CREATION_PROP,
				serviceUrls, useDefaultUsageForCreation, null));
	}

	@Override
    public String getContentType() {
	    return OSLCConstants.CT_JSON;
    }

	@Override
    public String getCreateContent() throws IOException, JSONException {
		if (jsonCreateTemplate == null) {
			String shapeUri = getShapeUriForCreation(currentUrl);
			return createResourceFromShape(shapeUri);
		}

	    return jsonCreateTemplate;
    }

	@Override
    public String getUpdateContent(String resourceUri) throws JSONException, IOException {
		if (jsonUpdateTemplate == null) {
			return updateResourceFromShape(resourceUri);
		}

	    return jsonUpdateTemplate;
    }
	
	private String createResourceFromShape(String shapeUri) throws IOException, JSONException {
		JSONObject toCreate = new JSONObject();
		JSONObject shape = getResource(shapeUri);

		fillType(toCreate, shape);
		fillRequiredProperties(toCreate, shape);
		fillPrefixes(toCreate);

		return toCreate.write();
	}

	/*
	 * Fill in the first type from the oslc:describes property in the shape.
	 */
	private void fillType(JSONObject toCreate, JSONObject shape)
            throws JSONException {
	    if (shape.has("oslc:describes")) {
	    	JSONArray describes = shape.getJSONArray("oslc:describes");
	    	if (!describes.isEmpty()) {
	    		JSONObject firstType = describes.getJSONObject(0);
	    		toCreate.put("rdf:type", firstType);
	    	}
	    }
    }

	/*
	 * Add reasonable values for any required properties.
	 */
	private void fillRequiredProperties(JSONObject toCreate, JSONObject shape) throws JSONException, IOException {
	    // Look at each property.
	    JSONArray array = shape.getJSONArray("oslc:property");
	    for (int i = 0; i < array.length(); ++i) {
	    	JSONObject property = array.getJSONObject(i);

			// Only try to fill in required properties to minimize the chance of errors.
	    	if (isPropertyRequired(property)) {
	    		fillInProperty(toCreate, property);
	    	}
	    }
    }

	private void fillInProperty(JSONObject toCreate, JSONObject property) throws JSONException, IOException {
	    // Try to use an allowed value.
	    JSONArray allowedValueArray = getAllowedValues(property);
	    if (allowedValueArray != null && !allowedValueArray.isEmpty()) {
	    	int index = (allowedValueArray.length() > 1) ? 1 : 0;
	    	toCreate.put(getQName(property), allowedValueArray.get(index));
	    } else {
	    	// No allowed values. Do our best to fill in a value from the value type.
	    	fillInUsingValueType(toCreate, property);
	    }
    }

	/*
	 * Tries to fill in a reasonable property value based on the value type.
	 */
	private void fillInUsingValueType(JSONObject toCreate, JSONObject property)
            throws JSONException {
	    Object valueTypeObject = property.get("oslc:valueType");
	    String key = getQName(property);
	    if (valueTypeObject != null) {
	    	HashSet<String> valueTypes = getValueTypes(property);

	    	/*
	    	 * Look at each type. Try to fill in something reasonable.
	    	 */
	    	if (valueTypes.contains(OSLCConstants.STRING_TYPE) || valueTypes.contains(OSLCConstants.XML_LITERAL_TYPE)) {
	    		String string = generateStringValue(getMaxSize(property));
	    		toCreate.put(key, string);
	    	} else if (valueTypes.contains(OSLCConstants.BOOLEAN_TYPE)) {
	    		toCreate.put(key, true);
	    	} else if (valueTypes.contains(OSLCConstants.INTEGER_TYPE)) {
	    		toCreate.put(key, 1);
	    	} else if (valueTypes.contains(OSLCConstants.DOUBLE_TYPE)) {
	    		toCreate.put(key, 1.0d);
	    	} else if (valueTypes.contains(OSLCConstants.FLOAT_TYPE)) {
	    		toCreate.put(key, 1.0f);
	    	}

	    	// TODO: Support decimal, date/time, and resource.
	    	
	    } else {
	    	// We have no hints. Try to set a string value. This may fail.
	    	String string = generateStringValue(getMaxSize(property));
	    	toCreate.put(key, string);
	    }
    }

	private String getQName(JSONObject property) throws JSONException {
	    String propertyDefinition = property.getJSONObject("oslc:propertyDefinition").getString("rdf:resource");
	    String name = property.getString("oslc:name");
	    String namespace = propertyDefinition.substring(0, propertyDefinition.length() - name.length());
	    String prefix = getPrefix(namespace);
	    assertNotNull("No prefix for namespace: " + namespace, prefix);

	    return prefix + ":" + name;
    }

	private JSONArray getAllowedValues(JSONObject property)
            throws JSONException, IOException {
	    if (property.has("oslc:allowedValue")) {
	    	return property.getJSONArray("oslc:allowedValue");
	    }
	    
	    if (property.has("oslc:allowedValues")) {
	    	JSONObject allowedValuesObj = property.getJSONObject("oslc:allowedValues");
	    	// Strangely, RTC inlines oslc:allowedValues.
	    	if (allowedValuesObj.has("oslc:allowedValue")) {
	    		return allowedValuesObj.getJSONArray("oslc:allowedValue");
	    	}
	
	    	if (allowedValuesObj.has("rdf:resource")) {
	    		String allowedValuesUri = allowedValuesObj.getString("rdf:resource");
	    		JSONObject allowedValues = getResource(allowedValuesUri);
	    		return allowedValues.getJSONArray("oslc:allowedValue");
	    	}
	    }

	    return null;
    }

	/*
	 * Determine the value types for this property, a hash set of URIs (as strings).
	 */
	private HashSet<String> getValueTypes(JSONObject property)
            throws JSONException {
	    HashSet<String> valueTypes = new HashSet<String>();

	    // Some providers represent value type as an array, some as an object.
	    Object valueTypeValue = property.get("oslc:valueType");
	    if (valueTypeValue instanceof JSONObject) {
	    	// TODO: Warn? This should be an array.
	    	JSONObject o = (JSONObject) valueTypeValue;
	    	valueTypes.add(o.getString("rdf:resource"));
	    } else if (valueTypeValue instanceof JSONArray) {
	    	JSONArray a = (JSONArray) valueTypeValue;
	    	Iterator<?> i = a.iterator();
	    	while (i.hasNext()) {
	    		JSONObject next = (JSONObject) i.next();
	    		valueTypes.add(next.getString("rdf:resource"));
	    	}
	    } else {
	    	fail("Incorrect type for oslc:valueType for property " + property.getString("name"));
	    }

	    return valueTypes;
    }

	private boolean isPropertyRequired(JSONObject property) throws JSONException {
		if (!property.has("oslc:occurs")) {
			return false;
		}

		JSONObject occurs = property.getJSONObject("oslc:occurs");
		String occursValue = occurs.getString("rdf:resource");
		
		return isPropertyRequired(occursValue);
	}
	
	private Integer getMaxSize(JSONObject property) throws JSONException {
		if (property.has("oslc:maxSize")) {
			return property.getInt("oslc:maxSize");
		}
		
		return null;
	}
	
	private boolean isPropertyReadOnly(JSONObject property) throws JSONException {
		if (!property.has("oslc:readOnly")) {
			return false;
		}
		
		return property.getBoolean("oslc:readOnly");
	}
	
	private String getPrefix(String namespace) {
		String prefix = namespacePrefixMap.get(namespace);
		if (prefix == null) {
			prefix = KNOWN_PREFIXES.get(namespace);
			if (prefix == null) {
				prefix = "j" + (namespacePrefixMap.size() + 1);
			}
			namespacePrefixMap.put(namespace, prefix);
		}
		
		return prefix;
	}
	
	private void fillPrefixes(JSONObject resource) throws JSONException {
		namespacePrefixMap.put(OSLCConstants.RDF, "rdf");
		JSONObject prefixes = new JSONObject();
		for (Map.Entry<String, String> next : namespacePrefixMap.entrySet()) {
			prefixes.put(next.getValue(), next.getKey());
		}
		
		resource.put("prefixes", prefixes);
	}
	
	private String updateResourceFromShape(String uri) throws JSONException, IOException {
		JSONObject toUpdate = getResource(uri);
		JSONObject instanceShapeResource = getInstanceShape(toUpdate);
	    modifySomeProperty(toUpdate, instanceShapeResource);
	    
	   return toUpdate.write(); 
	}

	private JSONObject getResource(String uri) throws IOException,
            JSONException {
	    HttpResponse resp = OSLCUtils.getResponseFromUrl(uri, null, creds, OSLCConstants.CT_JSON, headers);
		try {
			assertEquals("Failed to get resource at " + uri, 200, resp.getStatusLine().getStatusCode());
			return (JSONObject) JSON.parse(resp.getEntity().getContent());
		} finally {
			EntityUtils.consume(resp.getEntity());
		}
    }

	private JSONObject getInstanceShape(JSONObject toUpdate)
            throws JSONException, IOException {
		assertTrue("No instance shape for resource: " + toUpdate.write(), toUpdate.has("oslc:instanceShape"));
	    String instanceShapeUri = toUpdate.getJSONObject("oslc:instanceShape").getString("rdf:resource");
		JSONObject instanceShapeResource = getResource(instanceShapeUri);

	    return instanceShapeResource;
    }

	private void modifySomeProperty(JSONObject toUpdate,
            JSONObject instanceShapeResource) throws JSONException {
	    JSONArray array = instanceShapeResource.getJSONArray("oslc:property");
	    for (int i = 0; i < array.length(); ++i) {
	    	JSONObject property = array.getJSONObject(i);

	    	if (isPropertyReadOnly(property)) {
	    		continue;
	    	}

			// Like AbstractCreationAndUpdateRdfTests, let's try to keep things
			// simple and find a string property to modify. This test will fail
			// if there isn't an editable string property for the resource.
	    	HashSet<String> valueTypes = getValueTypes(property);

	    	if (valueTypes.contains(OSLCConstants.STRING_TYPE) || valueTypes.contains(OSLCConstants.XML_LITERAL_TYPE)) {
	    		String string = generateStringValue(getMaxSize(property));
	    		toUpdate.put(getQName(property), string);
	    		return;
	    	}
	    }
    }
}
