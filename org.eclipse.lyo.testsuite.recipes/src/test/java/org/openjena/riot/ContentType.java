package org.openjena.riot;

public class ContentType {
    public static ContentType parse(String header) {
        return new ContentType();
    }
    public String getContentType() {
        return "text/xml";
    }
}
