package org.eclipse.lyo.testsuite.refactor;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateOpenJenaToApacheJenaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateOpenJenaToApacheJena());
        spec.parser(JavaParser.fromJavaVersion());
    }

    @Test
    void migrateBasicOpenJenaToApacheJena() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;
                import org.openjena.riot.Lang;
                import org.openjena.riot.WebContent;

                public class TestClass {
                    public void processContentType(String contentTypeHeader) {
                        if (contentTypeHeader != null) {
                            final ContentType contentType = ContentType.parse(contentTypeHeader);
                            if (contentType != null) {
                                Lang lang = WebContent.contentTypeToLang(contentType.getContentType());
                                if (lang != null) {
                                    String rdfFormat = lang.getName();
                                } else if (HttpConstants.CT_TEXT_XML.equals(contentType.getContentType())) {
                                    String rdfFormat = Lang.RDFXML.getName();
                                }
                            }
                        }
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;
                import org.apache.jena.riot.Lang;
                import org.apache.jena.riot.RDFLanguages;
                import org.apache.jena.riot.WebContent;

                public class TestClass {
                    public void processContentType(String contentTypeHeader) {
                        if (contentTypeHeader != null) {
                            ContentType contentType = ContentType.create(contentTypeHeader);
                            if (contentType != null) {
                                Lang lang = RDFLanguages.contentTypeToLang(contentType);
                                if (lang != null) {
                                    String rdfFormat = lang.getName();
                                } else if (WebContent.contentTypeXML.equals(contentType.getContentTypeStr())) {
                                    String rdfFormat = Lang.RDFXML.getName();
                                }
                            }
                        }
                    }
                }
                """
            )
        );
    }

    @Test
    void migrateImportsOnly() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;
                import org.openjena.riot.Lang;
                import org.openjena.riot.WebContent;

                public class TestClass {
                    // No actual usage, just imports
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;
                import org.apache.jena.riot.Lang;
                import org.apache.jena.riot.WebContent;

                public class TestClass {
                    // No actual usage, just imports
                }
                """
            )
        );
    }

    @Test
    void migrateContentTypeParse() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;

                public class TestClass {
                    public void test(String header) {
                        ContentType ct = ContentType.parse(header);
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;

                public class TestClass {
                    public void test(String header) {
                        ContentType ct = ContentType.create(header);
                    }
                }
                """
            )
        );
    }

    @Test
    void migrateWebContentToRDFLanguages() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.Lang;
                import org.openjena.riot.WebContent;
                import org.openjena.riot.ContentType;

                public class TestClass {
                    public void test(ContentType contentType) {
                        Lang lang = WebContent.contentTypeToLang(contentType.getContentType());
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;
                import org.apache.jena.riot.Lang;
                import org.apache.jena.riot.RDFLanguages;
                import org.apache.jena.riot.WebContent;

                public class TestClass {
                    public void test(ContentType contentType) {
                        Lang lang = RDFLanguages.contentTypeToLang(contentType);
                    }
                }
                """
            )
        );
    }

    @Test
    void migrateHttpConstantsToWebContent() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;

                public class TestClass {
                    public void test(ContentType contentType) {
                        if (HttpConstants.CT_TEXT_XML.equals(contentType.getContentType())) {
                            // do something
                        }
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;
                import org.apache.jena.riot.WebContent;

                public class TestClass {
                    public void test(ContentType contentType) {
                        if (WebContent.contentTypeXML.equals(contentType.getContentTypeStr())) {
                            // do something
                        }
                    }
                }
                """
            )
        );
    }

    @Test
    void removeFinalModifierFromContentType() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;

                public class TestClass {
                    public void test(String header) {
                        final ContentType contentType = ContentType.parse(header);
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;

                public class TestClass {
                    public void test(String header) {
                        ContentType contentType = ContentType.create(header);
                    }
                }
                """
            )
        );
    }

    @Test
    void handleComplexScenario() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;
                import org.openjena.riot.Lang;
                import org.openjena.riot.WebContent;

                public class ContentTypeProcessor {
                    private String rdfFormat;

                    public void processHeader(String contentTypeHeader) {
                        if (contentTypeHeader != null) {
                            final ContentType contentType = ContentType.parse(contentTypeHeader);
                            if (contentType != null) {
                                Lang lang = WebContent.contentTypeToLang(contentType.getContentType());
                                if (lang != null) {
                                    rdfFormat = lang.getName();
                                } else if (HttpConstants.CT_TEXT_XML.equals(contentType.getContentType())) {
                                    rdfFormat = Lang.RDFXML.getName(); // attempt RDF parsing anyway
                                }
                            }
                        }
                    }

                    public void anotherMethod(ContentType ct) {
                        String type = ct.getContentType();
                        // This should become getContentTypeStr() in equals context only
                        if (HttpConstants.CT_TEXT_XML.equals(ct.getContentType())) {
                            // handle XML
                        }
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;
                import org.apache.jena.riot.Lang;
                import org.apache.jena.riot.RDFLanguages;
                import org.apache.jena.riot.WebContent;

                public class ContentTypeProcessor {
                    private String rdfFormat;

                    public void processHeader(String contentTypeHeader) {
                        if (contentTypeHeader != null) {
                            ContentType contentType = ContentType.create(contentTypeHeader);
                            if (contentType != null) {
                                Lang lang = RDFLanguages.contentTypeToLang(contentType);
                                if (lang != null) {
                                    rdfFormat = lang.getName();
                                } else if (WebContent.contentTypeXML.equals(contentType.getContentTypeStr())) {
                                    rdfFormat = Lang.RDFXML.getName(); // attempt RDF parsing anyway
                                }
                            }
                        }
                    }

                    public void anotherMethod(ContentType ct) {
                        String type = ct.getContentType();
                        // This should become getContentTypeStr() in equals context only
                        if (WebContent.contentTypeXML.equals(ct.getContentTypeStr())) {
                            // handle XML
                        }
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotChangeWhenNoOpenJenaImports() {
        rewriteRun(
            java(
                """
                import java.util.List;
                import java.util.Map;

                public class TestClass {
                    public void test() {
                        // No OpenJena code here
                        String test = "hello";
                    }
                }
                """
            )
        );
    }

    @Test
    void handlesPartialTransformation() {
        rewriteRun(
            java(
                """
                import org.openjena.riot.ContentType;

                public class TestClass {
                    public void test() {
                        // Only has ContentType import and usage
                        ContentType ct = ContentType.parse("text/xml");
                        String type = ct.getContentType();
                    }
                }
                """,
                """
                import org.apache.jena.atlas.web.ContentType;

                public class TestClass {
                    public void test() {
                        // Only has ContentType import and usage
                        ContentType ct = ContentType.create("text/xml");
                        String type = ct.getContentType();
                    }
                }
                """
            )
        );
    }
}
