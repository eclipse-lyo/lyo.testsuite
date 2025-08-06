/*
 * Copyright (c) 2011, 2012, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lyo.testsuite.util;

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
        else if ("oslc_cm_v2".equals(prefix)) return OSLCConstants.OSLC_CM_V2;
        else if ("oslc_qm_v2".equals(prefix)) return OSLCConstants.OSLC_QM_V2;
        else if ("oslc_rm_v2".equals(prefix)) return OSLCConstants.OSLC_RM_V2;
        else if ("oslc_auto_v2".equals(prefix)) return OSLCConstants.OSLC_AUTO_V2;
        return XMLConstants.NULL_NS_URI;
    }

    public String getPrefix(String uri) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Iterator getPrefixes(String uri) {
        return null;
    }
}
