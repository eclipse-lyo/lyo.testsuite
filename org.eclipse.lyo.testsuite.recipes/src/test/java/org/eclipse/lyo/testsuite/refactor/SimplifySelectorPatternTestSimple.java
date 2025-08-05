package org.eclipse.lyo.testsuite.refactor;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class SimplifySelectorPatternTestSimple implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifySelectorPattern());
    }

    @Test
    public void transformsSimpleSelectorPattern() {
        rewriteRun(
            java(
                """
                package org.example;

                import org.apache.jena.rdf.model.Model;
                import org.apache.jena.rdf.model.Property;
                import org.apache.jena.rdf.model.RDFNode;
                import org.apache.jena.rdf.model.Selector;
                import org.apache.jena.rdf.model.SimpleSelector;
                import org.apache.jena.rdf.model.StmtIterator;

                public class TestClass {
                    public void testMethod(Model model, Property property) {
                        Selector select = new SimpleSelector(null, property, (RDFNode) null);
                        StmtIterator statements = model.listStatements(select);
                    }
                }
                """,
                """
                package org.example;

                import org.apache.jena.rdf.model.Model;
                import org.apache.jena.rdf.model.Property;
                import org.apache.jena.rdf.model.RDFNode;
                import org.apache.jena.rdf.model.StmtIterator;

                public class TestClass {
                    public void testMethod(Model model, Property property) {
                        StmtIterator statements = model.listStatements(null, property, (RDFNode) null);
                    }
                }
                """
            )
        );
    }
}
