# Contributor pre-commit checks

Before committing, run these commands from the repository root:

1. `mvn -B spotless:apply -P!enforcer --file org.eclipse.lyo.testsuite.build/pom.xml`
2. `mvn -B org.openrewrite.maven:rewrite-maven-plugin:run spotless:apply -DskipTests -P!enforcer --file org.eclipse.lyo.testsuite.build/pom.xml`
3. `mvn -B validate --file org.eclipse.lyo.testsuite.build/pom.xml`

Do not skip Enforcer for the final validation step.
