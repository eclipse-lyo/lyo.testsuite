/*
 * Copyright (c) 2012, 2025 IBM Corporation and others
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
 *    Tim Eck II - asset management test cases and RAMConfigTool
 */
package org.eclipse.lyo.testsuite.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** This tool generates configurations to be used to run the lyo test suite against Rational Asset Manager. */
public class RAMConfigTool {
    private static Properties properties;

    private static String TEMPLATE_DIR = "templates/";
    private static String XML_DIR = TEMPLATE_DIR + "xml/";
    private static String JSON_DIR = TEMPLATE_DIR + "json/";

    private static String xmlCreateTemplate;
    private static String xmlArtifactTemplate;
    private static String xmlCategoryTemplate;
    private static String xmlRelationshipTemplate;
    private static String xmlUpdateTemplate;

    private static String jsonCreateTemplate;
    private static String jsonArtifactTemplate;
    private static String jsonCategoryTemplate;
    private static String jsonRelationshipTemplate;
    private static String jsonUpdateTemplate;

    private static String testArtifactTemplate;
    private static String setupPropertyTemplate;

    /**
     * first arg is file name, if second is -z then a zip file is created
     *
     * @param args
     */
    public static void main(String[] args) {
        // Verify the file arg might exist
        if (args.length < 1) {
            System.out.println("No file name was specified. The first argument needs to the file path to the"
                    + " properties file.");
            return;
        }

        // Attempt to read the properties
        try {
            setupProperties(args[0]);
        } catch (IOException e) {
            System.out.println("The specified file passed could no be read. Please verify that the file exists"
                    + " and is properly formatted");
            return;
        }

        // Reads all the template files
        try {
            xmlCreateTemplate = readFile(XML_DIR + "create.xml");
            xmlArtifactTemplate = readFile(XML_DIR + "createArtifact.xml");
            xmlCategoryTemplate = readFile(XML_DIR + "createCategory.xml");
            xmlRelationshipTemplate = readFile(XML_DIR + "createRelationship.xml");
            xmlUpdateTemplate = readFile(XML_DIR + "update.xml");

            jsonCreateTemplate = readFile(JSON_DIR + "create.json");
            jsonArtifactTemplate = readFile(JSON_DIR + "createArtifact.json");
            jsonCategoryTemplate = readFile(JSON_DIR + "createCategory.json");
            jsonRelationshipTemplate = readFile(JSON_DIR + "createRelationship.json");
            jsonUpdateTemplate = readFile(JSON_DIR + "update.json");

            testArtifactTemplate = readFile(TEMPLATE_DIR + "testArtifact.txt");
            setupPropertyTemplate = readFile(TEMPLATE_DIR + "setup.properties");
        } catch (IOException e) {
            System.out.println("Could not read the template files. Please verify that they are where they"
                    + " should be and can be read");
            return;
        }

        // Fill in templates
        try {
            fillInTemplates();
        } catch (NullPointerException e) {
            System.out.println("There where properties settings not found in the the properties file. Could"
                    + " not generate configuration.");
            return;
        }

        // Sets up the output location
        String outputLocation = properties.getProperty("outputLocation");
        if (outputLocation == null) outputLocation = "config/";

        boolean zip = false;
        if (args.length > 1) zip = args[1].toLowerCase().equals("-z");

        // Writes the configs to disk
        try {
            writeConfig(outputLocation, zip);
        } catch (IOException e) {
            System.out.println("Could not write the config files to disk");
            return;
        }
        System.out.println("The configuration have been created successfully");
    }

    private static void setupProperties(String propertyFile) throws IOException {
        InputStream is = new FileInputStream(propertyFile);
        properties = new Properties();
        properties.load(is);
    }

    private static String readFile(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        String line = reader.readLine();
        while (line != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
            line = reader.readLine();
        }

        String contents = stringBuilder.toString();
        reader.close();
        return contents;
    }

    private static void fillInTemplates() throws NullPointerException {
        String ramUrl = properties.getProperty("ramUrl");
        if (ramUrl == null) throw new NullPointerException();

        // Update the setup.properties template
        setupPropertyTemplate = setupPropertyTemplate.replace("*ramUrl*", ramUrl);
        String authMethod = properties.getProperty("authMethod");
        if (authMethod == null) authMethod = "BASIC";
        authMethod = authMethod.toUpperCase();
        setupPropertyTemplate = setupPropertyTemplate.replace("*authMethod*", authMethod);

        String formUri = properties.getProperty("formUri");
        if (authMethod.equals("FORM") && formUri == null) throw new NullPointerException();
        else formUri = "";
        setupPropertyTemplate = setupPropertyTemplate.replace("*formUri*", formUri);

        String userId = properties.getProperty("userId");
        String pw = properties.getProperty("pw");
        if (userId == null || pw == null) throw new NullPointerException();
        setupPropertyTemplate = setupPropertyTemplate.replace("*userId*", userId);
        setupPropertyTemplate = setupPropertyTemplate.replace("*pw*", pw);

        String outputLocation = properties.getProperty("outputLocation");
        if (outputLocation == null) outputLocation = "config/";
        setupPropertyTemplate = setupPropertyTemplate.replace("*location*", outputLocation);

        String queryAssetName = properties.getProperty("queryAssetName");
        String queryAssetModificationDate = properties.getProperty("queryAssetModificationDate");
        if (queryAssetName == null || queryAssetModificationDate == null) throw new NullPointerException();
        setupPropertyTemplate = setupPropertyTemplate.replace("*queryAssetName*", queryAssetName);
        setupPropertyTemplate =
                setupPropertyTemplate.replace("*queryAssetModificationDate*", queryAssetModificationDate);

        // Set up the property values for the xml/json file templates
        String assetType = properties.getProperty("assetType");
        String assetCommunity = properties.getProperty("assetCommunity");
        String assetCategory = properties.getProperty("assetCategory");
        String assetRelationship = properties.getProperty("assetRelationship");
        if (assetType == null || assetCommunity == null || assetCategory == null || assetRelationship == null)
            throw new NullPointerException();

        // Update the xml file templates
        xmlCreateTemplate = xmlCreateTemplate.replace("*ramUrl*", ramUrl);
        xmlCreateTemplate = xmlCreateTemplate.replace("*assetType*", assetType);
        xmlCreateTemplate = xmlCreateTemplate.replace("*assetCommunity*", assetCommunity);

        xmlCategoryTemplate = xmlCategoryTemplate.replace("*ramUrl*", ramUrl);
        xmlCategoryTemplate = xmlCategoryTemplate.replace("*assetType*", assetType);
        xmlCategoryTemplate = xmlCategoryTemplate.replace("*assetCommunity*", assetCommunity);
        xmlCategoryTemplate = xmlCategoryTemplate.replace("*assetCategory*", assetCategory);

        xmlRelationshipTemplate = xmlRelationshipTemplate.replace("*ramUrl*", ramUrl);
        xmlRelationshipTemplate = xmlRelationshipTemplate.replace("*assetType*", assetType);
        xmlRelationshipTemplate = xmlRelationshipTemplate.replace("*assetCommunity*", assetCommunity);
        xmlRelationshipTemplate = xmlRelationshipTemplate.replace("*assetRelationship*", assetRelationship);

        xmlUpdateTemplate = xmlUpdateTemplate.replace("*ramUrl*", ramUrl);
        xmlUpdateTemplate = xmlUpdateTemplate.replace("*assetType*", assetType);
        xmlUpdateTemplate = xmlUpdateTemplate.replace("*assetCommunity*", assetCommunity);

        // Update the json file templates
        jsonCreateTemplate = jsonCreateTemplate.replace("*ramUrl*", ramUrl);
        jsonCreateTemplate = jsonCreateTemplate.replace("*assetType*", assetType);
        jsonCreateTemplate = jsonCreateTemplate.replace("*assetCommunity*", assetCommunity);

        jsonCategoryTemplate = jsonCategoryTemplate.replace("*ramUrl*", ramUrl);
        jsonCategoryTemplate = jsonCategoryTemplate.replace("*assetType*", assetType);
        jsonCategoryTemplate = jsonCategoryTemplate.replace("*assetCommunity*", assetCommunity);
        jsonCategoryTemplate = jsonCategoryTemplate.replace("*assetCategory*", assetCategory);

        jsonRelationshipTemplate = jsonRelationshipTemplate.replace("*ramUrl*", ramUrl);
        jsonRelationshipTemplate = jsonRelationshipTemplate.replace("*assetType*", assetType);
        jsonRelationshipTemplate = jsonRelationshipTemplate.replace("*assetCommunity*", assetCommunity);
        jsonRelationshipTemplate = jsonRelationshipTemplate.replace("*assetRelationship", assetRelationship);

        jsonUpdateTemplate = jsonUpdateTemplate.replace("*ramUrl*", ramUrl);
        jsonUpdateTemplate = jsonUpdateTemplate.replace("*assetType*", assetType);
        jsonUpdateTemplate = jsonUpdateTemplate.replace("*assetCommunity*", assetCommunity);
    }

    private static void writeConfig(String location, boolean createZip) throws IOException {
        File directory = new File(location);
        if (!directory.exists()) directory.mkdirs();

        if (createZip) {
            String zipFileName = properties.getProperty("outputFileName");
            if (zipFileName == null) zipFileName = "config.zip";

            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(location + zipFileName));

            addToZip(zip, "xml/", "create", "xml", xmlCreateTemplate);
            addToZip(zip, "xml/", "createArtifact", "xml", xmlArtifactTemplate);
            addToZip(zip, "xml/", "createCategory", "xml", xmlCategoryTemplate);
            addToZip(zip, "xml/", "createRelationship", "xml", xmlRelationshipTemplate);
            addToZip(zip, "xml/", "update", "xml", xmlUpdateTemplate);

            addToZip(zip, "json/", "create", "json", jsonCreateTemplate);
            addToZip(zip, "json/", "createArtifact", "json", jsonArtifactTemplate);
            addToZip(zip, "json/", "createCategory", "json", jsonCategoryTemplate);
            addToZip(zip, "json/", "createRelationship", "json", jsonRelationshipTemplate);
            addToZip(zip, "json/", "update", "json", jsonUpdateTemplate);

            addToZip(zip, "", "setup", "properties", setupPropertyTemplate);
            addToZip(zip, "", "testArtifact", "properties", testArtifactTemplate);
            zip.close();

        } else {
            directory = new File(location + "xml/");
            if (!directory.exists()) directory.mkdirs();

            directory = new File(location + "json/");
            if (!directory.exists()) directory.mkdirs();

            writeFile(location + "xml/create.xml", xmlCreateTemplate);
            writeFile(location + "xml/createArtifact.xml", xmlArtifactTemplate);
            writeFile(location + "xml/createCategory.xml", xmlCategoryTemplate);
            writeFile(location + "xml/createRelationship.xml", xmlRelationshipTemplate);
            writeFile(location + "xml/update.xml", xmlUpdateTemplate);

            writeFile(location + "json/create.json", jsonCreateTemplate);
            writeFile(location + "json/createArtifact.json", jsonArtifactTemplate);
            writeFile(location + "json/createCategory.json", jsonCategoryTemplate);
            writeFile(location + "json/createRelationship.json", jsonRelationshipTemplate);
            writeFile(location + "json/update.json", jsonUpdateTemplate);

            writeFile(location + "setup.properties", setupPropertyTemplate);
            writeFile(location + "testArtifact.txt", testArtifactTemplate);
        }
    }

    private static void writeFile(String location, String content) throws IOException {
        FileWriter writer = new FileWriter(location);
        writer.append(content);
        writer.close();
    }

    private static void addToZip(
            ZipOutputStream zip, String directory, String fileName, String extention, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(directory + fileName + "." + extention));
        File temp = File.createTempFile(fileName, extention);

        FileWriter tempWriter = new FileWriter(temp);
        tempWriter.append(content);
        tempWriter.close();

        InputStream in = new FileInputStream(temp);
        byte[] b = new byte[1024];
        int count;
        while ((count = in.read(b)) > 0) {
            zip.write(b, 0, count);
        }
    }
}
