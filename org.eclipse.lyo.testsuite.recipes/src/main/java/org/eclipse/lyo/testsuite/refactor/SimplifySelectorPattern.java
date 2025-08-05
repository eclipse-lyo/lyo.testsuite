package org.eclipse.lyo.testsuite.refactor;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * OpenRewrite recipe that transforms SimpleSelector usage patterns.
 *
 * Transforms code like:
 * <pre>
 * a = new SimpleSelector(x, y, z);
 * b = c.listStatements(a);
 * </pre>
 *
 * Into:
 * <pre>
 * b = c.listStatements(x, y, z);
 * </pre>
 *
 * The SimpleSelector instantiation is removed and its arguments are inlined
 * into the listStatements method call.
 */
public class SimplifySelectorPattern extends Recipe {

    private static final Logger logger = LoggerFactory.getLogger(SimplifySelectorPattern.class);

    @Override
    public String getDisplayName() {
        return "Simplify SimpleSelector pattern";
    }

    @Override
    public String getDescription() {
        return "Removes SimpleSelector instantiations and inlines their arguments into listStatements calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SimplifySelectorVisitor();
    }

    private static class SimplifySelectorVisitor extends JavaIsoVisitor<ExecutionContext> {

        // Map to store SimpleSelector variable names and their constructor arguments
        private final Map<String, List<Expression>> selectorVariables = new HashMap<>();

        // Set to track statements that should be removed
        private final Set<J.VariableDeclarations> statementsToRemove = new HashSet<>();
        private final Set<J.Assignment> assignmentsToRemove = new HashSet<>();

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            // First pass: collect SimpleSelector variables and their arguments
            selectorVariables.clear();
            statementsToRemove.clear();
            assignmentsToRemove.clear();

            // Visit the compilation unit to collect information
            super.visitCompilationUnit(cu, ctx);

            // Second pass: perform transformations
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            multiVariable = super.visitVariableDeclarations(multiVariable, ctx);

            for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                if (variable.getInitializer() instanceof J.NewClass) {
                    J.NewClass newClass = (J.NewClass) variable.getInitializer();
                    if (isSimpleSelectorConstructor(newClass)) {
                        String variableName = variable.getSimpleName();
                        List<Expression> arguments = newClass.getArguments();
                        selectorVariables.put(variableName, arguments);
                        statementsToRemove.add(multiVariable);

                        logger.debug("Found SimpleSelector variable: {} with {} arguments",
                                   variableName, arguments.size());
                    }
                }
            }

            // Remove this statement if it declares a SimpleSelector
            if (statementsToRemove.contains(multiVariable)) {
                return null;
            }

            return multiVariable;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            assignment = super.visitAssignment(assignment, ctx);

            if (assignment.getAssignment() instanceof J.NewClass) {
                J.NewClass newClass = (J.NewClass) assignment.getAssignment();
                if (isSimpleSelectorConstructor(newClass)) {
                    if (assignment.getVariable() instanceof J.Identifier) {
                        String variableName = ((J.Identifier) assignment.getVariable()).getSimpleName();
                        List<Expression> arguments = newClass.getArguments();
                        selectorVariables.put(variableName, arguments);
                        assignmentsToRemove.add(assignment);

                        logger.debug("Found SimpleSelector assignment: {} with {} arguments",
                                   variableName, arguments.size());
                    }
                }
            }

            // Remove this assignment if it assigns a SimpleSelector
            if (assignmentsToRemove.contains(assignment)) {
                return null;
            }

            return assignment;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            method = super.visitMethodInvocation(method, ctx);

            // Check if this is a listStatements method call
            if ("listStatements".equals(method.getSimpleName()) &&
                method.getArguments().size() == 1) {

                Expression argument = method.getArguments().get(0);
                if (argument instanceof J.Identifier) {
                    String argumentName = ((J.Identifier) argument).getSimpleName();

                    // Check if the argument is a SimpleSelector variable
                    if (selectorVariables.containsKey(argumentName)) {
                        List<Expression> selectorArgs = selectorVariables.get(argumentName);

                        logger.debug("Transforming listStatements call with SimpleSelector: {}", argumentName);

                        // Replace the single argument with the SimpleSelector constructor arguments
                        return method.withArguments(selectorArgs);
                    }
                }
            }

            return method;
        }

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            block = super.visitBlock(block, ctx);

            // Filter out null statements (removed assignments/declarations)
            List<org.openrewrite.java.tree.Statement> filteredStatements = new ArrayList<>();
            for (org.openrewrite.java.tree.Statement statement : block.getStatements()) {
                if (statement != null) {
                    filteredStatements.add(statement);
                }
            }

            if (filteredStatements.size() != block.getStatements().size()) {
                return block.withStatements(filteredStatements);
            }

            return block;
        }

        /**
         * Checks if the given NewClass expression is a SimpleSelector constructor call
         */
        private boolean isSimpleSelectorConstructor(J.NewClass newClass) {
            if (newClass.getClazz() == null) {
                return false;
            }

            // Check if the class being instantiated is SimpleSelector
            String className = newClass.getClazz().toString();
            return "SimpleSelector".equals(className) || className.endsWith(".SimpleSelector");
        }
    }
}
