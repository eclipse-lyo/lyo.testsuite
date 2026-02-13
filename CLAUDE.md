# Contributor pre-commit checks

Before committing, run these commands from the repository root:

1. `mvn -B --settings .mvn/settings.xml validate --file org.eclipse.lyo.testsuite.build/pom.xml` (Enforcer, required)
2. `mvn -B --settings .mvn/settings.xml spotless:apply -P!enforcer --file org.eclipse.lyo.testsuite.build/pom.xml`
3. `mvn -B --settings .mvn/settings.xml org.openrewrite.maven:rewrite-maven-plugin:run spotless:apply -DskipTests -P!enforcer --file org.eclipse.lyo.testsuite.build/pom.xml`

Do not skip Enforcer validation. If any command fails, fix it before commit.
