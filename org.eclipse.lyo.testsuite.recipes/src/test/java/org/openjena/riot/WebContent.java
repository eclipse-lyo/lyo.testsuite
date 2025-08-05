package org.openjena.riot;

public class WebContent {
    public static final String contentTypeXML = "text/xml";
    public static org.openjena.riot.Lang contentTypeToLang(String contentType) {
        return org.openjena.riot.Lang.RDFXML;
    }
}
