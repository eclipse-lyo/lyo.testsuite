/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
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
 *    Joseph Leong, Sujeet Mishra - Initial implementation
 *******************************************************************************/

package org.eclipse.lyo.testsuite.server.trsutils;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * An API to access the vocabulary of the TRS Protocol.
 * The following namespaces are supported: {@link #TEMPORARILY_SUPPORTED} and {@link #LATEST}
 */
public interface ITRSVocabulary {

	// Local names
	public static final String TRS_PROVIDER_LOCALNAME = "TrackedResourceSetProvider"; //$NON-NLS-1$
	public static final String TRS_RES_LOCALNAME = "TrackedResourceSet"; //$NON-NLS-1$
	public static final String TRS_PROP_LOCALNAME = "trackedResourceSet"; //$NON-NLS-1$
	public static final String BASE_LOCALNAME = "base"; //$NON-NLS-1$
	public static final String CUTOFFEVENT_LOCALNAME = "cutoffEvent"; //$NON-NLS-1$
	public static final String NEXT_LOCALNAME = "nextPage"; //$NON-NLS-1$
	public static final String CHANGELOG_LOCALNAME = "changeLog"; //$NON-NLS-1$
	public static final String CHANGES_LOCALNAME = "changes"; //$NON-NLS-1$
	public static final String CHANGED_LOCALNAME = "changed"; //$NON-NLS-1$
	public static final String ORDER_LOCALNAME = "order"; //$NON-NLS-1$
	public static final String PREVIOUS_LOCALNAME = "previous"; //$NON-NLS-1$
	public static final String TRSURI_LOCALNAME = "TrackedResourceSetUri"; //$NON-NLS-1$
	public static final String CREATION_LOCALNAME = "Creation"; //$NON-NLS-1$
	public static final String MODIFICATION_LOCALNAME = "Modification"; //$NON-NLS-1$
	public static final String DELETION_LOCALNAME = "Deletion"; //$NON-NLS-1$

	/**
	 * The namespace of the vocabulary as a string
	 */
	public static final String NS = "http://jazz.net/ns/trs#"; //$NON-NLS-1$

	/**
	 * Get a specific vocabulary within the TRS provider as a string
	 */
	public static final String TRS_PROVIDER_URI = NS + TRS_PROVIDER_LOCALNAME;

	/**
	 * Get a specific vocabulary within the TRS property as a string
	 */
	public static final String TRS_PROP_URI = NS + TRS_PROP_LOCALNAME;

	/**
	 * The resource for the TRS namespace
	 */
	public static final Resource NAMESPACE = ResourceFactory.createResource(NS);

	/**
	 * The property for the base predicate in a TRS
	 */
	public static final Property BASE_PROPERTY = ResourceFactory.createProperty(NS, BASE_LOCALNAME);

	/**
	 * The property for the cut off event predicate in a TRS
	 */
	public static final Property CUTOFFEVENT_PROPERTY = ResourceFactory.createProperty(NS,
		CUTOFFEVENT_LOCALNAME);

	/**
	 * The property for the next page predicate in a TRS or base resource
	 */
	public static final Property NEXT_PAGE_PROPERTY = ResourceFactory.createProperty(NS,
		NEXT_LOCALNAME);

	/**
	 * The property for a change log predicate within a tracked resource set
	 */
	public static final Property CHANGELOG_PROPERTY = ResourceFactory.createProperty(NS,
		CHANGELOG_LOCALNAME);

	/**
	 * The property for the changes predicate in a TRS
	 */
	public static final Property CHANGES_PROPERTY = ResourceFactory.createProperty(NS,
		CHANGES_LOCALNAME);

	/**
	 * The property for the changes predicate of a change log entry
	 */
	public static final Property CHANGED_PROPERTY = ResourceFactory.createProperty(NS,
		CHANGED_LOCALNAME);

	/**
	 * The property for the order predicate of a change log entry
	 */
	public static final Property ORDER_PROPERTY = ResourceFactory.createProperty(NS,
		ORDER_LOCALNAME);

	/**
	 * The property for the previous predicate in a TRS or base resource
	 */
	public static final Property PREVIOUS_PROPERTY = ResourceFactory.createProperty(NS,
		PREVIOUS_LOCALNAME);

	/**
	 * The resource for the set of tracked resources
	 */
	public static final Resource TRS_RESOURCE = ResourceFactory.createResource(NS
		+ TRS_RES_LOCALNAME);

	/**
	 * The resource for the creation change log type
	 */
	public static final Resource CREATION_RESOURCE = ResourceFactory.createResource(NS
		+ CREATION_LOCALNAME);

	/**
	 * The resource for the modification change log type
	 */
	public static final Resource MODIFICATION_RESOURCE = ResourceFactory.createResource(NS
		+ MODIFICATION_LOCALNAME);

	/**
	 * The resource for the deletion change log type
	 */
	public static final Resource DELETION_RESOURCE = ResourceFactory.createResource(NS
		+ DELETION_LOCALNAME);

}