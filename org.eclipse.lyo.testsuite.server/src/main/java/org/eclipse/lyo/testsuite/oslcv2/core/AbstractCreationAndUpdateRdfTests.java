/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation.
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
 *    Samuel Padgett - create and update resources using shapes
 *    Samuel Padgett - don't cache query shapes for creation when the URIs are the same
 *    Samuel Padgett - handle oslc:AllowedValues blank nodes
 *******************************************************************************/
package org.eclipse.lyo.testsuite.oslcv2.core;

import static org.junit.Assert.*;

import org.apache.jena.datatypes.xsd.impl.XMLLiteralType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import jakarta.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.eclipse.lyo.testsuite.oslcv2.TestsBase;
import org.eclipse.lyo.testsuite.util.OSLCConstants;
import org.eclipse.lyo.testsuite.util.OSLCUtils;
import org.eclipse.lyo.testsuite.util.RDFUtils;
import org.junit.Test;

/**
 * Common class for testing creation and update using RDF media types.
 */
public abstract class AbstractCreationAndUpdateRdfTests extends CreationAndUpdateBaseTests {

    private static Logger logger = Logger.getLogger(AbstractCreationAndUpdateRdfTests.class);

    public AbstractCreationAndUpdateRdfTests(String url) {
        super(url);
    }

    public String createResourceFromShape(String shapeUri) throws IOException {
        logger.debug("Creating resource from shape <%s>".formatted(shapeUri));
        Model m = ModelFactory.createDefaultModel();
        createResourceFromShape(m, shapeUri, 1);

        return toString(m);
    }

    // For debugging.
    private String asTurtle(Model m) {
        StringWriter writer = new StringWriter();
        m.write(writer, "TURTLE");

        return writer.toString();
    }

    // Max depth is used to detect cycles.
    private static final int MAX_DEPTH =
            Integer.parseInt(
                    System.getProperty(
                            "org.eclipse.lyo.testsuite.oslcv2.createResource.maxDepth", "10"));

    protected Resource createResourceFromShape(Model requestModel, String shapeUri, int depth)
            throws IOException {
        assertTrue(
                "Detected possible circular reference in shape while creating resource.",
                depth < MAX_DEPTH);

        // Get the shape.
        Model shapeModel = getModel(shapeUri);
        if (logger.isDebugEnabled()) {
            logger.debug(asTurtle(shapeModel));
        }

        Resource toCreate = requestModel.createResource();

        Resource shapeResource = shapeModel.getResource(shapeUri);
        StmtIterator typeIter =
                shapeResource.listProperties(shapeModel.createProperty(OSLCConstants.DESCRIBES));

        // Use the first rdf:type if defined.
        if (typeIter.hasNext()) {
            Resource type = typeIter.next().getResource();
            toCreate.addProperty(RDF.type, type);
        }

        final Property propertyProp = shapeModel.createProperty(OSLCConstants.PROPERTY);

        // Try to create a resource based on the properties in the shape.
        StmtIterator propIter = shapeResource.listProperties(propertyProp);
        while (propIter.hasNext()) {
            Resource nextProperty = propIter.next().getResource();

            // Only try to fill in required properties to minimize the chance of errors.
            if (isPropertyRequired(nextProperty)) {
                fillInProperty(shapeModel, toCreate, nextProperty, depth);
            }
        }

        return toCreate;
    }

    private void fillInProperty(
            Model shapeModel, Resource toCreate, Resource propertyToFill, int depth)
            throws IOException {
        final Property propertyDefinitionProp =
                shapeModel.createProperty(OSLCConstants.PROPERTY_DEFINITION);
        final Property allowedValueProp = shapeModel.createProperty(OSLCConstants.ALLOWED_VALUE);
        final Property allowedValuesProp = shapeModel.createProperty(OSLCConstants.ALLOWED_VALUES);

        String propertyDefinition =
                propertyToFill.getRequiredProperty(propertyDefinitionProp).getResource().getURI();
        Property requestProp = toCreate.getModel().createProperty(propertyDefinition);

        /*
         * Don't attempt to use the default value. Some providers make the
         * default value something that is not allowed (for instance, Filed
         * Against: Unassigned in RTC).
         */

        //		final Property defaultValueProp =
        // shapeModel.createProperty(OSLCConstants.DEFAULT_VALUE);
        //		if (propertyToFill.hasProperty(defaultValueProp)) {
        //			RDFNode defaultValue = propertyToFill.getProperty(defaultValueProp).getObject();
        //			// Make sure it's not just the empty string.
        //			if (defaultValue.isResource() || !"".equals(defaultValue.asLiteral().getLexicalForm()))
        // {
        //				toCreate.addProperty(requestProp, defaultValue);
        //				return;
        //			}
        //		}

        // Check for a list of allowed values that we can use.
        if (propertyToFill.hasProperty(allowedValueProp)) {
            RDFNode randomAllowedValue = getAllowedValue(propertyToFill);
            toCreate.addProperty(requestProp, randomAllowedValue);
            return;
        }

        if (propertyToFill.hasProperty(allowedValuesProp)) {
            Resource allowedValuesResource =
                    propertyToFill.getProperty(allowedValuesProp).getResource();
            String allowedValuesUri = allowedValuesResource.getURI();
            RDFNode randomAllowedValue;
            if (allowedValuesUri == null) {
                // Blank node. Maybe the allowed values are inline?
                // It's not normal since in that case you can use oslc:allowedValue directly on
                // Property.
                randomAllowedValue = getAllowedValue(allowedValuesResource);
            } else {
                // Make another request to get the list.
                Model allowedValuesModel = getModel(allowedValuesUri);
                allowedValuesResource = allowedValuesModel.getResource(allowedValuesUri);
                randomAllowedValue = getAllowedValue(allowedValuesResource);
            }
            toCreate.addProperty(requestProp, randomAllowedValue);
            return;
        }

        // No allowed values. Fill some some data appropriate to the type.
        fillInPropertyFromValueType(toCreate, propertyToFill, requestProp, depth);
    }

    /*
     * Try to find an acceptable allowed value. Really we should be able to
     * select any, but some providers give an empty or unassigned value as the
     * first item in the list. Technically, order is not guaranteed from
     * listProperties(), but in practice it seems to be preserved. Thus let's
     * try to avoid the first item if possible to minimize errors.
     */
    private RDFNode getAllowedValue(Resource r) {
        final Property allowedValueProp = r.getModel().createProperty(OSLCConstants.ALLOWED_VALUE);
        List<Statement> allowedValues = r.listProperties(allowedValueProp).toList();
        if (allowedValues.isEmpty()) {
            return null;
        }

        if (allowedValues.size() == 1) {
            return allowedValues.getFirst().getObject();
        }

        if (allowedValues.size() == 2) {
            return allowedValues.get(1).getObject();
        }

        // Even the second item causes problems sometimes.
        // Use the second to last... Maybe it's safe?
        return allowedValues.get(allowedValues.size() - 2).getObject();
    }

    /*
     * Attempt to add a value for this property using its value type.
     */
    private void fillInPropertyFromValueType(
            Resource toCreate, Resource propertyResource, Property requestProp, int depth)
            throws IOException {
        Model requestModel = toCreate.getModel();
        Model shapeModel = propertyResource.getModel();
        final Property valueTypeProp = shapeModel.createProperty(OSLCConstants.VALUE_TYPE);

        if (propertyResource.hasProperty(valueTypeProp)) {
            final Property rangeProp = shapeModel.createProperty(OSLCConstants.RANGE);
            final Property valueShapeProp =
                    shapeModel.createProperty(OSLCConstants.VALUE_SHAPE_PROP);
            HashSet<String> valueTypes = new HashSet<>();
            StmtIterator valueTypeIter = propertyResource.listProperties(valueTypeProp);
            while (valueTypeIter.hasNext()) {
                String typeUri = valueTypeIter.next().getResource().getURI();
                valueTypes.add(typeUri);
            }

            /*
             * Look at each type. Try to fill in something reasonable.
             */
            if (valueTypes.contains(OSLCConstants.STRING_TYPE)) {
                String string = generateStringValue(getMaxSize(propertyResource));
                toCreate.addProperty(requestProp, string);
            } else if (valueTypes.contains(OSLCConstants.XML_LITERAL_TYPE)) {
                String string = generateStringValue(getMaxSize(propertyResource));
                Literal literal =
                        requestModel.createTypedLiteral(string, XMLLiteralType.theXMLLiteralType);
                toCreate.addLiteral(requestProp, literal);
            } else if (valueTypes.contains(OSLCConstants.BOOLEAN_TYPE)) {
                toCreate.addLiteral(requestProp, true);
            } else if (valueTypes.contains(OSLCConstants.INTEGER_TYPE)) {
                toCreate.addLiteral(requestProp, 1);
            } else if (valueTypes.contains(OSLCConstants.DOUBLE_TYPE)) {
                toCreate.addLiteral(requestProp, 1.0d);
            } else if (valueTypes.contains(OSLCConstants.FLOAT_TYPE)) {
                toCreate.addLiteral(requestProp, 1.0f);
            } else if (valueTypes.contains(OSLCConstants.DECIMAL_TYPE)) {
                Literal literal = requestModel.createTypedLiteral(1, OSLCConstants.DECIMAL_TYPE);
                toCreate.addLiteral(requestProp, literal);
            } else if (valueTypes.contains(OSLCConstants.DATE_TIME_TYPE)) {
                toCreate.addLiteral(
                        requestProp, requestModel.createTypedLiteral(Calendar.getInstance()));
            } else {
                // It appears to be a resource.
                Statement valueShapeStatement = propertyResource.getProperty(valueShapeProp);
                if (valueShapeStatement == null) {
                    // We have no shape, so this will likely fail. We can try, though.
                    // Create an empty resource. Add an rdf:type if the property has a range.
                    Resource valueResource = requestModel.createResource();
                    StmtIterator rangeIter = propertyResource.listProperties(rangeProp);
                    if (rangeIter.hasNext()) {
                        valueResource.addProperty(RDF.type, rangeIter.next().getResource());
                    }
                    toCreate.addProperty(requestProp, valueResource);
                } else {
                    Resource nested =
                            createResourceFromShape(
                                    requestModel,
                                    valueShapeStatement.getResource().getURI(),
                                    depth + 1);
                    toCreate.addProperty(requestProp, nested);
                }
            }
        } else {
            // We have no hints. Try to set a string value. This may fail.
            String string = generateStringValue(getMaxSize(propertyResource));
            toCreate.addProperty(requestProp, string);
        }
    }

    protected String toString(Model model) {
        String lang =
                (OSLCConstants.CT_XML.equals(getHeaderString("Content-Type"))) ? "RDF/XML-ABBREV" : "RDF/XML";
        StringWriter writer = new StringWriter();
        model.write(writer, lang, "");

        return writer.toString();
    }

    protected abstract String getHeaderString(String s);

    /*
     * Is this property from a resource shape required?
     */
    private boolean isPropertyRequired(Resource property) {
        Statement statement =
                property.getRequiredProperty(
                        property.getModel().createProperty(OSLCConstants.OCCURS));
        String occursValue = statement.getResource().getURI();

        return isPropertyRequired(occursValue);
    }

    /*
     * Is this property from a resource shape read only?
     */
    private boolean isPropertyReadOnly(Resource property) {
        Statement statement =
                property.getProperty(property.getModel().createProperty(OSLCConstants.READ_ONLY));
        if (statement == null) {
            return false;
        }

        return statement.getBoolean();
    }

    /*
     * Is this property from a resource shape a string?
     */
    private boolean isStringType(Resource property) {
        Property valueTypeProp = property.getModel().getProperty(OSLCConstants.VALUE_TYPE);
        if (!property.hasProperty(valueTypeProp)) {
            // We don't know, but assume it's not.
            return false;
        }

        StmtIterator iter = property.listProperties(valueTypeProp);
        while (iter.hasNext()) {
            String valueType = iter.next().getResource().getURI();
            if (OSLCConstants.STRING_TYPE.equals(valueType)) {
                return true;
            }
        }

        return false;
    }

    /*
     * Get the max size for this property if defined. Return null otherwise.
     */
    private Integer getMaxSize(Resource property) {
        Property maxSizeProp = property.getModel().createProperty(OSLCConstants.MAX_SIZE_PROP);
        Statement maxSize = property.getProperty(maxSizeProp);
        if (maxSize == null) {
            return null;
        }

        return maxSize.getInt();
    }

    private Model getModel(String uri) throws IOException {
        Response resp =
            OSLCUtils.getResponseFromUrl(uri, uri, TestsBase.creds, OSLCConstants.CT_RDF, TestsBase.headers);



        try {
            assertEquals(
                    "Failed to get resource at " + uri, 200, resp.getStatus());
            Model model = ModelFactory.createDefaultModel();
            model.read(resp.readEntity(InputStream.class), uri, OSLCConstants.JENA_RDF_XML);
            RDFUtils.validateModel(model);

            return model;
        } finally {
            resp.close();
        }
    }

    /**
     * Change a resource using its instance shape. Will only attempt to change
     * one property to limit the chance of errors.
     *
     * @param uri
     *            the resource to modify
     *
     * @throws IOException
     *             on errors requesting the instance shape
     */
    protected String updateResourceFromShape(String uri, String contentType) throws IOException {
        Model resourceModel = getModel(uri);
        Resource resource = resourceModel.getResource(uri);
        Property instanceShapeProp = resourceModel.createProperty(OSLCConstants.INSTANCE_SHAPE);
        Statement instanceShapeStatement = resource.getProperty(instanceShapeProp);
        assertNotNull("The resource does not have an instance shape.", instanceShapeStatement);

        String shapeUri = instanceShapeStatement.getResource().getURI();
        Model shapeModel = getModel(shapeUri);

        Property propertyProp = shapeModel.createProperty(OSLCConstants.PROPERTY);
        Property propertyDefinitionProp =
                shapeModel.createProperty(OSLCConstants.PROPERTY_DEFINITION);
        Resource shape = shapeModel.getResource(shapeUri);
        StmtIterator propertyIterator = shape.listProperties(propertyProp);

        // Get the list of properties.
        // List<Statement> propertyStatements = shape.listProperties(propertyProp).toList();
        while (propertyIterator.hasNext()) {
            Resource property = propertyIterator.next().getResource();

            // Skip read-only properties that we can't change.
            if (isPropertyReadOnly(property)) {
                continue;
            }

            /*
             * For now, let's keep things simple and try to find a string value
             * we can update. This will fail (incorrectly) if the resource does
             * not have any modifiable strings. We could make this more
             * sophisticated and look for any type that might be modifiable.
             */
            if (isStringType(property)) {
                String propertyDefinition =
                        property.getRequiredProperty(propertyDefinitionProp).getResource().getURI();
                Property propertyToChange = resourceModel.createProperty(propertyDefinition);
                resource.removeAll(propertyToChange);
                resource.addLiteral(
                        propertyToChange, generateStringValue(getMaxSize(propertyToChange)));

                // Updating one field should be good enough.
                break;
            }
        }

        return toString(resourceModel);
    }

    protected String getCreateContent(String template) throws IOException {
        String content;
        if (template == null) {
            String shapeUri = TestsBase.getShapeUriForCreation(TestsBase.currentUrl);
            assertNotNull("No shape for creation factory: " + TestsBase.currentUrl, shapeUri);
            content = createResourceFromShape(shapeUri);
        } else {
            content = template;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("POST content:");
            logger.debug(content);
        }

        return content;
    }

    protected String getUpdateContent(String resourceUri, String template) throws IOException {
        if (template == null) {
            return updateResourceFromShape(resourceUri, getHeaderString("Content-Type"));
        }

        if (template.contains("rdf:about=\"\"")) {
            // We need to replace the rdf:about in the template with the real url
            String replacement = "rdf:about=\"" + resourceUri + "\"";
            template = template.replace("rdf:about=\"\"", replacement);
        }

        return template;
    }
}
