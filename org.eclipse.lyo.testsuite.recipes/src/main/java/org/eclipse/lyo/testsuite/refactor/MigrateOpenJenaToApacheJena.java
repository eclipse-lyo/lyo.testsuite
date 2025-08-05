package org.eclipse.lyo.testsuite.refactor;

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.openrewrite.Tree.*;

/**
 * OpenRewrite recipe that migrates from OpenJena to Apache Jena.
 *
 * Transforms:
 * - Import statements from org.openjena.* to org.apache.jena.*
 * - ContentType.parse() to ContentType.create()
 * - WebContent.contentTypeToLang() to RDFLanguages.contentTypeToLang()
 * - contentType.getContentType() to contentType
 * - HttpConstants.CT_TEXT_XML to WebContent.contentTypeXML
 * - contentType.getContentType() to contentType.getContentTypeStr()
 */
public class MigrateOpenJenaToApacheJena extends Recipe {

    private static final Logger logger = LoggerFactory.getLogger(MigrateOpenJenaToApacheJena.class);

    @Override
    public String getDisplayName() {
        return "Migrate OpenJena to Apache Jena";
    }

    @Override
    public String getDescription() {
        return "Migrates OpenJena imports and API calls to Apache Jena equivalents.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new OpenJenaToApacheJenaVisitor();
    }

    private static class OpenJenaToApacheJenaVisitor extends JavaIsoVisitor<ExecutionContext> {

        // Template for ContentType.create() method call
        private final JavaTemplate contentTypeCreateTemplate = JavaTemplate.builder(
            "ContentType.create(#{any(java.lang.String)})"
        ).build();

        // Template for RDFLanguages.contentTypeToLang() method call
        private final JavaTemplate rdfLanguagesTemplate = JavaTemplate.builder(
            "RDFLanguages.contentTypeToLang(#{any()})"
        ).build();

        // Template for WebContent.contentTypeXML
        private final JavaTemplate webContentXmlTemplate = JavaTemplate.builder(
            "WebContent.contentTypeXML"
        ).build();

        // Template for contentType.getContentTypeStr()
        private final JavaTemplate getContentTypeStrTemplate = JavaTemplate.builder(
            "#{any()}.getContentTypeStr()"
        ).build();

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
            _import = super.visitImport(_import, ctx);

            String importPath = _import.getPackageName();

            // Map of OpenJena imports to Apache Jena imports
            Map<String, String> importMappings = new HashMap<>();
            importMappings.put("org.openjena.riot.ContentType", "org.apache.jena.atlas.web.ContentType");
            importMappings.put("org.openjena.riot.Lang", "org.apache.jena.riot.Lang");
            importMappings.put("org.openjena.riot.WebContent", "org.apache.jena.riot.WebContent");

            if (importMappings.containsKey(importPath)) {
                String newImportPath = importMappings.get(importPath);
                logger.debug("Migrating import from {} to {}", importPath, newImportPath);

                // Use JavaTemplate to replace the import
                JavaTemplate importTemplate = JavaTemplate.builder("import " + newImportPath + ";").build();
                return importTemplate.apply(getCursor(), _import.getCoordinates().replace());
            }

            return _import;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            cu = super.visitCompilationUnit(cu, ctx);

            // Add RDFLanguages import if WebContent.contentTypeToLang is used
            if (cu.printAll().contains("contentTypeToLang")) {
                boolean hasRdfLanguagesImport = cu.getImports().stream()
                    .anyMatch(imp -> "org.apache.jena.riot.RDFLanguages".equals(imp.getPackageName()));

                if (!hasRdfLanguagesImport) {
                    // Use maybeAddImport to add the import
                    maybeAddImport("org.apache.jena.riot.RDFLanguages");
                    logger.debug("Adding RDFLanguages import");
                }
            }

            return cu;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            method = super.visitMethodInvocation(method, ctx);

            // Transform ContentType.parse() to ContentType.create()
            if ("parse".equals(method.getSimpleName()) &&
                method.getSelect() instanceof J.Identifier &&
                "ContentType".equals(((J.Identifier) method.getSelect()).getSimpleName())) {

                logger.debug("Transforming ContentType.parse() to ContentType.create()");
                return contentTypeCreateTemplate.apply(
                    getCursor(),
                    method.getCoordinates().replace(),
                    method.getArguments().get(0)
                );
            }

            // Transform WebContent.contentTypeToLang() to RDFLanguages.contentTypeToLang()
            if ("contentTypeToLang".equals(method.getSimpleName()) &&
                method.getSelect() instanceof J.Identifier &&
                "WebContent".equals(((J.Identifier) method.getSelect()).getSimpleName())) {

                logger.debug("Transforming WebContent.contentTypeToLang() to RDFLanguages.contentTypeToLang()");
                return rdfLanguagesTemplate.apply(
                    getCursor(),
                    method.getCoordinates().replace(),
                    method.getArguments().get(0)
                );
            }

            // Transform contentType.getContentType() calls
            if ("getContentType".equals(method.getSimpleName())) {
                // Check the context to determine the appropriate transformation
                Tree parent = getCursor().getParent().getValue();

                // If parent is RDFLanguages.contentTypeToLang(), remove .getContentType()
                if (parent instanceof J.MethodInvocation) {
                    J.MethodInvocation parentMethod = (J.MethodInvocation) parent;
                    if ("contentTypeToLang".equals(parentMethod.getSimpleName()) &&
                        parentMethod.getSelect() instanceof J.Identifier &&
                        "RDFLanguages".equals(((J.Identifier) parentMethod.getSelect()).getSimpleName())) {

                        logger.debug("Removing .getContentType() in RDFLanguages.contentTypeToLang context");
                        // Return the select expression (the variable before .getContentType())
                        return method.withName(method.getName().withSimpleName(""))
                                    .withSelect(null)
                                    .withArguments(Collections.emptyList());
                    }
                }

                // If parent is a comparison with equals(), transform to getContentTypeStr()
                if (parent instanceof J.MethodInvocation) {
                    J.MethodInvocation parentMethod = (J.MethodInvocation) parent;
                    if ("equals".equals(parentMethod.getSimpleName())) {
                        logger.debug("Transforming .getContentType() to .getContentTypeStr() in equals context");
                        return getContentTypeStrTemplate.apply(
                            getCursor(),
                            method.getCoordinates().replace(),
                            method.getSelect()
                        );
                    }
                }
            }

            return method;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            fieldAccess = super.visitFieldAccess(fieldAccess, ctx);

            // Transform HttpConstants.CT_TEXT_XML to WebContent.contentTypeXML
            if ("CT_TEXT_XML".equals(fieldAccess.getSimpleName()) &&
                fieldAccess.getTarget() instanceof J.Identifier &&
                "HttpConstants".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName())) {

                logger.debug("Transforming HttpConstants.CT_TEXT_XML to WebContent.contentTypeXML");
                return webContentXmlTemplate.apply(
                    getCursor(),
                    fieldAccess.getCoordinates().replace()
                );
            }

            return fieldAccess;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            multiVariable = super.visitVariableDeclarations(multiVariable, ctx);

            // Remove 'final' modifier from ContentType declarations
            if (multiVariable.getTypeExpression() != null &&
                multiVariable.getTypeExpression().toString().contains("ContentType")) {

                List<J.Modifier> modifiers = multiVariable.getModifiers();
                List<J.Modifier> newModifiers = new ArrayList<>();

                for (J.Modifier modifier : modifiers) {
                    if (modifier.getType() != J.Modifier.Type.Final) {
                        newModifiers.add(modifier);
                    } else {
                        logger.debug("Removing 'final' modifier from ContentType declaration");
                    }
                }

                if (newModifiers.size() != modifiers.size()) {
                    return multiVariable.withModifiers(newModifiers);
                }
            }

            return multiVariable;
        }

        private boolean isContentTypeMethod(J.MethodInvocation method) {
            return method.getSelect() instanceof J.Identifier &&
                   "ContentType".equals(((J.Identifier) method.getSelect()).getSimpleName());
        }

        private boolean isWebContentMethod(J.MethodInvocation method) {
            return method.getSelect() instanceof J.Identifier &&
                   "WebContent".equals(((J.Identifier) method.getSelect()).getSimpleName());
        }

        private boolean isHttpConstantsField(J.FieldAccess fieldAccess) {
            return fieldAccess.getTarget() instanceof J.Identifier &&
                   "HttpConstants".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName());
        }

        private boolean shouldAddImport(J.CompilationUnit cu, String importPath) {
            // Simple heuristic: check if the class name appears in the source code
            String className = importPath.substring(importPath.lastIndexOf('.') + 1);
            String sourceCode = cu.printAll();

            // Special cases for imports that are always needed based on the transformation
            if (importPath.equals("org.apache.jena.riot.RDFLanguages") &&
                sourceCode.contains("contentTypeToLang")) {
                return true;
            }

            return sourceCode.contains(className);
        }
    }
}
