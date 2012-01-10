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

public interface OSLCConstants {
	static String RFC_DATE_FORMAT = "yyyy-MM-dd'T'h:m:ss.S'Z'";

	static String DC = "http://purl.org/dc/terms/";
	static String DCTERMS ="dcterms:";
	static String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	static String ATOM = "http://www.w3.org/2005/Atom";
	static String OSLC_V2    = "http://open-services.net/ns/core#";
	static String OSLC_CM_V2 = "http://open-services.net/ns/cm#";
	static String OSLC_AM_V2 = "http://open-services.net/ns/am#";
	static String OSLC_QM_V2 = "http://open-services.net/ns/qm#";
	static String OSLC_RM_V2 = "http://open-services.net/ns/rm#";
	// Version 1.0 namespace definitions
	static String OSLC_DISC = "http://open-services.net/xmlns/discovery/1.0/";
	static String OSLC_CM   = "http://open-services.net/xmlns/cm/1.0/";
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

	// RDF
	public static final String RDF_TYPE_PROP		= RDF + "type";
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

	// DCTERMSs
	public static final String DCTERMS_TITLE 		= DCTERMS + "title";
	public static final String DCTERMS_DESC 		= DCTERMS + "description";
	public static final String DCTERMS_TYPE 		= DCTERMS + "type";
	public static final String DCTERMS_PUBLISHER 	= DCTERMS + "publisher";
	public static final String DCTERMS_ID 			= DCTERMS + "identifier";
	public static final String DCTERMS_NAME 		= DCTERMS + "name";
	public static final String DCTERMS_CREATED		= DCTERMS + "created";
	public static final String DCTERMS_MODIFIED		= DCTERMS + "modified";
	
}
