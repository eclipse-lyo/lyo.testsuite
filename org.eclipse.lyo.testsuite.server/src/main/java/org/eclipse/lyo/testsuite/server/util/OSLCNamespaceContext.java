/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation1
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
 *    Yuhong Yin
 *******************************************************************************/
package org.eclipse.lyo.testsuite.server.util;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;


public class OSLCNamespaceContext implements NamespaceContext {
	public String getNamespaceURI(String prefix) {
		if (prefix == null) throw new NullPointerException("Null prefix");
        else if ("jfs".equals(prefix)) return OSLCConstants.JFS;
        else if ("rdfs".equals(prefix)) return OSLCConstants.RDFS;
        else if ("rdf".equals(prefix)) return OSLCConstants.RDF;
        else if ("dc".equals(prefix)) return OSLCConstants.DC;
        else if ("jd".equals(prefix)) return OSLCConstants.JD;
        else if ("jp06".equals(prefix)) return OSLCConstants.JP06;
        else if ("oslc_disc".equals(prefix)) return OSLCConstants.OSLC_DISC;
        else if ("oslc".equals(prefix)) return OSLCConstants.OSLC_V2;
        else if ("oslc_core".equals(prefix)) return OSLCConstants.OSLC_V2;
        else if ("oslc_v2".equals(prefix)) return OSLCConstants.OSLC_V2;
        else if ("oslc_cm".equals(prefix)) return OSLCConstants.OSLC_CM;
        else if ("oslc_asset".equals(prefix)) return OSLCConstants.OSLC_ASSET_V2;
        else if ("rtc_cm".equals(prefix)) return OSLCConstants.RTC_CM;
        else if ("atom".equals(prefix)) return OSLCConstants.ATOM;
        else if ("oslc_cm_v2".equals(prefix)) return OSLCConstants.OSLC_CM_V2;
        else if ("oslc_qm_v2".equals(prefix)) return OSLCConstants.OSLC_QM_V2;
        return XMLConstants.NULL_NS_URI;
	}
    public String getPrefix(String uri) { return null; }

	@SuppressWarnings("rawtypes")
	public Iterator getPrefixes(String uri) { return null; }
}
