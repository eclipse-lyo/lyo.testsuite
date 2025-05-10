package org.eclipse.lyo.testsuite.server.trsutils;

public class HttpConstants {

    private HttpConstants() {}

    /**
     * General-header field name for <i>Cache-Control</i>. See <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9"
     * >RFC2616 Section 14.9</a>
     */
    public static final String CACHE_CONTROL = "Cache-Control"; // $NON-NLS-1$

    // alias

    /**
     * Request-header field name for <i>Accept</i>. See <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.3"
     * >RFC2616 Section 5.3</a> and <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1"
     * >RFC2616 Section 14.1</a>
     */
    public static final String ACCEPT = "Accept"; // $NON-NLS-1$

    /**
     * Response-header field name for <i>Location</i>. See <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.2"
     * >RFC2616 Section 6.2</a> and <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.30"
     * >RFC2616 Section 14.30</a>
     */
    public static final String LOCATION = "Location"; // $NON-NLS-1$

    /**
     * Entity-header field name for <i>Content-Type</i>. See <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.1"
     * >RFC2616 Section 7.1</a> and <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17"
     * >RFC2616 Section 14.17</a>
     */
    public static final String CONTENT_TYPE = "Content-Type"; // $NON-NLS-1$

    public static final String DEFAULT_ENCODING = "UTF-8"; // $NON-NLS-1$

    /**
     * The internet media type or MIME type or Content-Type value for an XML
     * feed.
     */
    public static final String CT_APPLICATION_RDF_XML = "application/rdf+xml"; // $NON-NLS-1$

    /**
     * The internet media type or MIME type or Content-Type value for an XML as
     * text.
     */
    public static final String CT_TEXT_XML = "text/xml"; // $NON-NLS-1$
}
