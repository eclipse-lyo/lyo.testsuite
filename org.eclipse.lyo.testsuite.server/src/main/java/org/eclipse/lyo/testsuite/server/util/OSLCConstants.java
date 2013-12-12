/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation.
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
 *    Yuhong Yin
 *    Tim Eck II     - asset management test cases
 *    Samuel Padgett - add constants for resource shapes
 *******************************************************************************/

package org.eclipse.lyo.testsuite.server.util;

public interface OSLCConstants {
	static String RFC_DATE_FORMAT = "yyyy-MM-dd'T'h:m:ss.S'Z'";

	static String DC = "http://purl.org/dc/terms/";
	static String DCTERMS ="dcterms:";
	static String EMS = "http://open-services.net/ns/ems#";
	static String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	static String ATOM = "http://www.w3.org/2005/Atom";
	static String OSLC_V2    = "http://open-services.net/ns/core#";
	static String CORE_DEFAULT = "http://open-services.net/ns/core#default";
	static String OSLC_CM_V2 = "http://open-services.net/ns/cm#";
	static String OSLC_AM_V2 = "http://open-services.net/ns/am#";
	static String OSLC_ASSET_V2 = "http://open-services.net/ns/asset#";
	static String OSLC_QM_V2 = "http://open-services.net/ns/qm#";
	static String OSLC_RM_V2 = "http://open-services.net/ns/rm#";
	static String OSLC_AUTO_V2 = "http://open-services.net/ns/auto#";
	static String OSLC_PM_V2 = "http://open-services.net/ns/pm#";
	static String OSLC_CRTV_V2 = "http://open-services.net/ns/crtv#";
	
	// Version 1.0 namespace definitions
	static String OSLC_DISC = "http://open-services.net/xmlns/discovery/1.0/";
	static String OSLC_CM   = "http://open-services.net/xmlns/cm/1.0/";
	static String OSLC_ASSET   = "http://open-services.net/xmlns/asset/1.0/";
	
	// Misc definitions
	static String RTC_CM = "http://jazz.net/xmlns/prod/jazz/rtc/cm/1.0/";
	static String JFS = "http://jazz.net/xmlns/prod/jazz/jfs/1.0/";
	static String JD = "http://jazz.net/xmlns/prod/jazz/discovery/1.0/";
	static String JP06 = "http://jazz.net/xmlns/prod/jazz/process/0.6/";
	
	//--------------------------------------------------------------------------
	// Content-types for Accept header requests
	// Standard headers:
	static String CT_XML = "application/xml";
	static String CT_RDF = "application/rdf+xml";
	static String CT_JSON = "application/json";
	static String CT_COMPACT = "application/x-oslc-compact+xml";
	static String CT_ATOM = "application/atom+xml";
	
	// Version 1 headers:
	static String CT_CR_XML = "application/x-oslc-cm-change-request+xml";
	static String CT_CR_JSON = "application/x-oslc-cm-change-request+json";
	static String CT_CR_QUERY = "application/x-oslc-cm-change-request+xml";
	static String CT_DISC_CAT_XML = "application/x-oslc-disc-service-provider-catalog+xml"; 
	static String CT_DISC_DESC_XML = "application/x-oslc-cm-service-description+xml";
	
	static String POST = "POST";
	static String SSL = "SSL";


	public static final String JENA_RDF_XML = "RDF/XML";
	
	//--------------------------------------------------------------------------
	// Property URIs
	
	// OSLC Core
	public static final String SERVICE_PROVIDER_PROP = OSLC_V2 + "serviceProvider";
	public static final String SERVICE_PROVIDER_TYPE = OSLC_V2 + "ServiceProvider";
	public static final String SERVICE_PROVIDER_CATALOG_PROP = OSLC_V2 + "serviceProviderCatalog";
	public static final String SERVICE_PROVIDER_CATALOG_TYPE = OSLC_V2 + "ServiceProviderCatalog";
	public static final String CREATION_PROP 		= OSLC_V2 + "creation";
	public static final String QUERY_CAPABILITY_PROP = OSLC_V2 + "QueryCapability";
	public static final String QUERY_BASE_PROP 		= OSLC_V2 + "queryBase";
	public static final String RESP_INFO_TYPE 		= OSLC_V2 + "ResponseInfo";
	public static final String SERVICE_PROP 		= OSLC_V2 + "service";
	public static final String DISCUSSION_PROP 		= OSLC_V2 + "discussion";
	public static final String INST_SHAPE_PROP 		= OSLC_V2 + "instanceShape";
	public static final String USAGE_PROP		 	= OSLC_V2 + "usage";
	public static final String USAGE_DEFAULT_URI 	= OSLC_V2 + "default";
	public static final String TOTAL_COUNT_PROP	 	= OSLC_V2 + "totalCount";
	public static final String RESOURCE_TYPE_PROP   = OSLC_V2 + "resourceType";
	public static final String RESOURCE_SHAPE_PROP  = OSLC_V2 + "resourceShape";
	public static final String LABEL_PROP			= OSLC_V2 + "label";
	public static final String DESCRIPTION_PROP 	= OSLC_V2 + "Description";
	
	// OSLC Core - Shapes
	public static final String INSTANCE_SHAPE       = OSLC_V2 + "instanceShape";
	public static final String DESCRIBES            = OSLC_V2 + "describes";
	public static final String PROPERTY             = OSLC_V2 + "property";
	public static final String OCCURS               = OSLC_V2 + "occurs";
	public static final String EXACTLY_ONE          = OSLC_V2 + "Exactly-one";
	public static final String ONE_OR_MANY          = OSLC_V2 + "One-or-many";
	public static final String PROPERTY_DEFINITION  = OSLC_V2 + "propertyDefinition";
	public static final String DEFAULT_VALUE        = OSLC_V2 + "defaultValue";
	public static final String ALLOWED_VALUE        = OSLC_V2 + "allowedValue";
	public static final String ALLOWED_VALUES       = OSLC_V2 + "allowedValues";
	public static final String VALUE_TYPE           = OSLC_V2 + "valueType";
	public static final String RANGE                = OSLC_V2 + "range";
	public static final String READ_ONLY            = OSLC_V2 + "readOnly";
	public static final String MAX_SIZE_PROP        = OSLC_V2 + "maxSize";
	public static final String VALUE_SHAPE_PROP     = OSLC_V2 + "valueShape";
	
	// Value types
	public static final String BOOLEAN_TYPE         = "http://www.w3.org/2001/XMLSchema#boolean";
	public static final String DATE_TIME_TYPE       = "http://www.w3.org/2001/XMLSchema#dateTime";
	public static final String DECIMAL_TYPE         = "http://www.w3.org/2001/XMLSchema#decimal";
	public static final String DOUBLE_TYPE          = "http://www.w3.org/2001/XMLSchema#double";
	public static final String FLOAT_TYPE           = "http://www.w3.org/2001/XMLSchema#float";
	public static final String INTEGER_TYPE         = "http://www.w3.org/2001/XMLSchema#integer";
	public static final String STRING_TYPE          = "http://www.w3.org/2001/XMLSchema#string";
	public static final String XML_LITERAL_TYPE     = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";
	
	// OSLC CM 2.0
	public static final String CM_CHANGE_REQUEST_TYPE = OSLC_CM_V2 + "ChangeRequest";
	public static final String CM_CLOSE_DATE_PROP 	= OSLC_CM_V2 + "closeDate";
	public static final String CM_STATUS_PROP 		= OSLC_CM_V2 + "status";
	public static final String CM_CLOSED_PROP 		= OSLC_CM_V2 + "closed";
	public static final String CM_INPROGRESS_PROP 	= OSLC_CM_V2 + "inprogress";
	public static final String CM_FIXED_PROP 		= OSLC_CM_V2 + "fixed";
	public static final String CM_APPROVED_PROP 	= OSLC_CM_V2 + "approved";
	public static final String CM_REVIEWED_PROP 	= OSLC_CM_V2 + "reviewed";
	public static final String CM_VERIFIED_PROP 	= OSLC_CM_V2 + "verified";

	
	// OSLC Asset 2.0
	public static final String ASSET_GUID_PROP		= OSLC_ASSET_V2 + "guid";
	public static final String ASSET_VERSION_PROP	= OSLC_ASSET_V2 + "version";
	public static final String ASSET_ARTIFACT_PROP	= OSLC_ASSET_V2 + "artifact";
	public static final String ASSET_ARTIFACT_FACTORY_PROP = OSLC_ASSET_V2 + "artifactFactory";
	public static final String ASSET_CATEGORIZATION_PROP = OSLC_ASSET_V2 + "categorization";
	public static final String ASSET_STATE_PROP		= OSLC_ASSET_V2 + "state";
	public static final String ASSET_MANUFACTURER_PROP = OSLC_ASSET_V2 + "manufacturer";
	public static final String ASSET_MODEL_PROP		= OSLC_ASSET_V2 + "model";
	public static final String ASSET_SERIAL_NUMBER_PROP = OSLC_ASSET_V2 + "serialNumber";

	// OSLC QM 2.0
	public static final String QM_TEST_PLAN = OSLC_QM_V2 + "testPlan";
	public static final String QM_TEST_CASE = OSLC_QM_V2 + "testCase";
	public static final String QM_TEST_SCRIPT = OSLC_QM_V2 + "testScript";
	public static final String QM_TEST_RESULT = OSLC_QM_V2 + "testResult";
	public static final String QM_TEST_EXECUTION_RECORD = OSLC_QM_V2 + "testExecutionRecord";
	
	public static final String QM_TEST_PLAN_QUERY = OSLC_QM_V2 + "TestPlanQuery";
	public static final String QM_TEST_CASE_QUERY = OSLC_QM_V2 + "TestCaseQuery";
	public static final String QM_TEST_SCRIPT_QUERY = OSLC_QM_V2 + "TestScriptQuery";
	public static final String QM_TEST_RESULT_QUERY = OSLC_QM_V2 + "TestResultQuery";
	public static final String QM_TEST_EXECUTION_RECORD_QUERY = OSLC_QM_V2 + "TestExecutionRecordQuery";

	 //OSLC RM 2.0
	 
	 public static final String RM_REQUIREMENT_TYPE = OSLC_RM_V2 + "Requirement";
	 public static final String RM_REQUIREMENT_COLLECTION_TYPE = OSLC_RM_V2 + "RequirementCollection";
	 
	 //OSLC Automation 2.0
	 public static final String AUTO_OSLC_AUTO = "oslc_auto:";
	 public static final String AUTO_AUTOMATION_PLAN_TYPE = OSLC_AUTO_V2 + "AutomationPlan";
	 public static final String AUTO_AUTOMATION_REQUEST_TYPE = OSLC_AUTO_V2 + "AutomationRequest";
	 public static final String AUTO_AUTOMATION_RESULT_TYPE = OSLC_AUTO_V2 + "AutomationResult";
	 public static final String AUTO_AUTOMATION_STATE = OSLC_AUTO_V2 + "state";
	 public static final String AUTO_OSLC_AUTO_STATE = AUTO_OSLC_AUTO + "state";
	 public static final String AUTO_AUTOMATION_DESIRED_STATE = OSLC_AUTO_V2 + "desiredState";
	 public static final String AUTO_OSLC_AUTO_DESIRED_STATE = AUTO_OSLC_AUTO + "desriedState";
	 public static final String AUTO_AUTOMATION_VERDICT = OSLC_AUTO_V2 + "verdict";
	 public static final String AUTO_OSLC_AUTO_VERDICT = AUTO_OSLC_AUTO + "verdict";
	 public static final String AUTO_AUTOMATION_EXECUTES_AUTO_PLAN = OSLC_AUTO_V2 + "executesAutomationPlan";
	 public static final String AUTO_OSLC_AUTO_EXECUTES_AUTO_PLAN = AUTO_OSLC_AUTO + "executesAutomationPlan";
	 public static final String AUTO_AUTOMATION_REPORTS_AUTO_PLAN = OSLC_AUTO_V2 + "reportsOnAutomationPlan";
	 public static final String AUTO_OSLC_AUTO_REPORTS_AUTO_PLAN = AUTO_OSLC_AUTO + "reportsOnAutomationPlan";
	 public static final String AUTO_AUTOMATION_PRODUCED_AUTO_REQUEST = OSLC_AUTO_V2 + "producedByAutomationRequest";
	 public static final String AUTO_OSLC_AUTO_PRODUCED_AUTO_REQUEST = AUTO_OSLC_AUTO + "producedByAutomationRequest";
	 
	 //OSLC Performance Monitoring 2.0
	 public static String PM_PMR_ISPARTOF = DC + "isPartOf";
	 public static final String CRTV_COMPUTERSYSTEM_TYPE = OSLC_CRTV_V2 + "ComputerSystem";
	 public static final String CRTV_PROCESS_TYPE = OSLC_CRTV_V2 + "Process";	
	 public static final String CRTV_STORAGEVOLUME_TYPE = OSLC_CRTV_V2 + "StorageVolume";
	 public static final String CRTV_AGENT_TYPE = OSLC_CRTV_V2 + "Agent";
	 public static final String CRTV_SOFTWARESERVER_TYPE = OSLC_CRTV_V2 + "SoftwareServer";
	 public static final String CRTV_SOFTWAREMODULE_TYPE = OSLC_CRTV_V2 + "SoftwareModule";	 
	 public static final String CRTV_DATABASE_TYPE = OSLC_CRTV_V2 + "Database";	 
	 public static String PM_PMR_OBSERVES = EMS + "observes";	 
	 
	// RDF
	public static final String RDF_TYPE_PROP		= RDF + "type";
	public static final String RDF_DESCRIPTION_PROP	= RDF + "Description";
	public static final String RDFS_MEMBER 			= RDFS + "member";
	
	// DCTERMS URIs
	public static final String DC_TITLE_PROP 		= DC + "title";
	public static final String DC_DESC_PROP 		= DC + "description";
	public static final String DC_TYPE_PROP 		= DC + "type";
	public static final String DC_PUBLISHER_PROP 	= DC + "publisher";
	public static final String DC_ID_PROP 			= DC + "identifier";
	public static final String DC_NAME_PROP 		= DC + "name";
	public static final String DC_CREATED_PROP		= DC + "created";
	public static final String DC_MODIFIED_PROP		= DC + "modified";
	public static final String DC_RELATION_PROP		= DC + "relation";
	public static final String DC_ABSTRACT_PROP		= DC + "abstract";
 
	// DCTERMSs
	public static final String DCTERMS_TITLE 		= DCTERMS + "title";
	public static final String DCTERMS_DESC 		= DCTERMS + "description";
	public static final String DCTERMS_TYPE 		= DCTERMS + "type";
	public static final String DCTERMS_PUBLISHER 	= DCTERMS + "publisher";
	public static final String DCTERMS_ID 			= DCTERMS + "identifier";
	public static final String DCTERMS_NAME 		= DCTERMS + "name";
	public static final String DCTERMS_CREATED		= DCTERMS + "created";
	public static final String DCTERMS_MODIFIED		= DCTERMS + "modified";
	public static final String DCTERMS_RELATION		= DCTERMS + "relation";
	public static final String DCTERMS_ABSTRACT		= DCTERMS + "abstract";
	
}
