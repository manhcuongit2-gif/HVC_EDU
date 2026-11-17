# Repository Guidelines

## Project Structure & Modules

This is a Java 21 Spring Boot application built with Maven. Application code lives under `src/main/java/com/educenter`, organized into packages such as `controller`, `service`, `repository`, `entity`, `dto`, `config`, and `security`. Thymeleaf views are in `src/main/resources/templates` and grouped by role or feature (`admin/`, `parent/`, `teacher/`, `auth/`). Runtime settings are in `src/main/resources/application.properties`. Database schema and seed scripts are `csdl.sql` and `data.sql` at the repository root. Static assets, if added, belong in `src/main/resources/static`.

## Build, Test, and Run

- `mvn spring-boot:run` starts the app locally on port 8080.
- `mvn test` runs the Maven test suite.
- `mvn package` compiles, tests, and packages the executable application JAR under `target/`.

The repository does not currently include `src/test`; add tests there as features are introduced. Local runs require a MySQL database named `educenter_db` initialized from the SQL scripts.

## Coding Style & Naming

Follow standard Java conventions and the existing package structure: four spaces for indentation, `UpperCamelCase` for classes (for example, `StudentServiceImpl`), and `lowerCamelCase` for methods and fields. Use descriptive feature names and keep controllers, business logic, and persistence in their respective layers. Name tests after the class under test, such as `StudentServiceTest`. Keep Thymeleaf templates grouped by user role or feature.

## Testing Guidelines

The project includes Spring Boot Test and Spring Security Test dependencies, with Maven as the test runner. Add focused unit or integration tests under `src/test/java`; run `mvn test` before submitting changes. No coverage threshold or formatter/linter is configured in `pom.xml`.

## Commits and Pull Requests

The existing history has short, generic subjects (`update`, `Update`, `Initial commit`) and does not establish a consistent format. Use a concise imperative subject that explains the change, such as `Add parent registration validation`. Pull requests should describe the behavior changed, note database or configuration impacts, and include screenshots for UI changes. Link related issues when available.

## Configuration and Security

Use local MySQL and payment settings for development. Supply credentials and VNPay secrets through environment-specific configuration; never commit real passwords, API keys, or merchant secrets. Keep `target/` and IDE-generated files out of commits.
