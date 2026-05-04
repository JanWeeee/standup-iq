# StandupIQ Project Learning Guide

This document is a complete learning and interview-preparation guide for StandupIQ. It explains what the project does, how the code is organized, how each request flows through the system, which part of the code belongs to which technology, and what concepts you should be ready to explain in interviews.

Do not put real API keys or tokens in this document. Secrets belong only in `.env` or secure deployment environment variables.

## Table Of Contents

1. Project Summary
2. Problem Statement
3. Current Feature Set
4. Architecture Overview
5. End-To-End Flow
6. Project Structure
7. Tech Stack And Code Mapping
8. Backend Concepts
9. Database And Persistence
10. GitHub Integration
11. Gemini AI Integration
11A. Slack Delivery
11B. Scheduled Generation
12. Frontend UI Flow
13. Error Handling
14. Configuration And Secrets
15. Build, Test, Docker, And Deployment
16. API Reference
17. Resume Talking Points
18. Limitations And Future Improvements
19. Interview Questions And Answers
20. Study Roadmap

## 1. Project Summary

StandupIQ is an AI-powered daily standup generator for software developers.

The application reads a developer's real GitHub activity, such as commits and pull requests, then uses Gemini to generate a natural-language standup update. It saves generated standups to PostgreSQL so the user can view history and reuse previous updates.

In one sentence:

> StandupIQ turns GitHub activity into professional standup updates automatically.

## 2. Problem Statement

Developers often spend 10 to 15 minutes before standup trying to remember what they did yesterday. They check GitHub commits, pull requests, Jira tickets, Slack messages, and sometimes still miss details.

StandupIQ solves this by using actual activity from GitHub as the source of truth. Instead of relying on memory, it collects commits and pull requests, summarizes them with AI, and stores the generated standup.

Why this is useful:

- Saves daily preparation time.
- Reduces forgotten work in standup.
- Creates a consistent format: Yesterday, Today, Blockers.
- Works across repositories.
- Can be extended to Jira, Slack, email, and team dashboards.

## 3. Current Feature Set

Currently implemented:

- Spring Boot backend running on port `8080`.
- PostgreSQL database integration.
- Flyway database migrations.
- JPA entities for users and standups.
- GitHub activity collection using WebClient.
- GitHub search API plus direct repository APIs for better public/private repository coverage.
- Gemini AI standup generation.
- Fallback standup generation if Gemini fails or API key is missing.
- Generated standup persistence.
- Standup history endpoint.
- Global error handling.
- Static browser UI served from Spring Boot.
- Swagger/OpenAPI documentation at `/swagger-ui.html`.
- Optional Slack webhook delivery.
- Scheduled weekday standup generation.
- Service-level unit tests with JUnit 5 and Mockito.
- Maven wrapper configuration.
- Multi-stage Dockerfile.
- Railway deployment configuration.
- Professional README.

Important endpoints:

- `GET /api/health`
- `GET /api/github/activity/{username}`
- `GET /api/standup/generate/{username}`
- `GET /api/standup/history/{username}`
- `GET /swagger-ui.html`
- Browser UI: `GET /`

## 4. Architecture Overview

High-level architecture:

```text
Browser UI
   |
   | HTTP requests
   v
Spring Boot Controllers
   |
   | call services
   v
Business Services
   |        |
   |        +--> GitHub REST API
   |        |
   |        +--> Gemini API
   |
   v
Spring Data JPA Repositories
   |
   v
PostgreSQL
```

Layered architecture:

```text
Controller Layer
    Accepts HTTP requests and returns HTTP responses.

Service Layer
    Contains business logic such as fetching GitHub activity and generating standups.

Repository Layer
    Handles database access through Spring Data JPA.

Entity Layer
    Maps Java objects to database tables.

DTO Layer
    Defines API request/response shapes separate from database entities.

Static UI Layer
    HTML, CSS, and JavaScript served from src/main/resources/static.
```

## 5. End-To-End Flow

### 5.1 Health Check Flow

URL:

```text
GET /api/health
```

Flow:

```text
Browser
  -> HealthController.health()
  -> returns JSON status
```

Purpose:

- Confirms backend is running.
- Useful for local testing and deployment health checks.

### 5.2 Fetch GitHub Activity Flow

URL:

```text
GET /api/github/activity/JanWeeee?days=7
```

Flow:

```text
Browser or UI
  -> GitHubController.getActivity()
  -> GitHubService.getActivity()
  -> GitHub search API
  -> direct GitHub repository commits API
  -> direct GitHub repository PR API
  -> deduplicate commits by SHA
  -> sort results
  -> return GitHubActivityResponse
```

Why this matters:

- GitHub search can miss private repository commits or delayed indexed commits.
- Direct repository API gives stronger coverage for repos accessible by your token.

### 5.3 Generate Standup Flow

URL:

```text
GET /api/standup/generate/JanWeeee?days=7
```

Flow:

```text
Browser or UI
  -> StandupController.generateStandup()
  -> GitHubService.getActivity()
  -> StandupService.generateAndSaveStandup()
  -> Gemini generateContent API
  -> fallback standup if Gemini fails
  -> save Standup entity through StandupRepository
  -> return StandupResponse
```

Response contains:

- Generated standup ID.
- Username.
- Time range in days.
- Generated timestamp.
- Raw GitHub activity.
- Generated standup text.

### 5.4 History Flow

URL:

```text
GET /api/standup/history/JanWeeee
```

Flow:

```text
Browser or UI
  -> StandupController.getHistory()
  -> StandupService.getHistory()
  -> StandupRepository.findByUsernameOrderByGeneratedAtDesc()
  -> return list of StandupHistoryResponse
```

Purpose:

- Shows previous generated standups.
- Demonstrates persistence and analytics readiness.

### 5.5 UI Flow

URL:

```text
http://localhost:8080/
```

Flow:

```text
index.html loads
  -> styles.css styles page
  -> app.js checks /api/health
  -> user enters GitHub username and time range
  -> Fetch Activity calls /api/github/activity
  -> Generate Standup calls /api/standup/generate
  -> History calls /api/standup/history
```

## 6. Project Structure

Current important structure:

```text
com.standupiq.standup_iq
├── controller
│   ├── GitHubController.java
│   ├── HealthController.java
│   └── StandupController.java
├── config
│   └── OpenApiConfig.java
├── dto
│   ├── ApiErrorResponse.java
│   ├── CommitSummary.java
│   ├── DebugTimeResponse.java
│   ├── GitHubActivityResponse.java
│   ├── HealthResponse.java
│   ├── PullRequestSummary.java
│   ├── StandupHistoryResponse.java
│   └── StandupResponse.java
├── entity
│   ├── Standup.java
│   └── User.java
├── exception
│   ├── ExternalServiceException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository
│   ├── StandupRepository.java
│   └── UserRepository.java
├── service
│   ├── GitHubService.java
│   ├── ScheduledStandupJob.java
│   ├── SlackService.java
│   └── StandupService.java
└── StandupIqApplication.java
```

Resources:

```text
src/main/resources
├── application.properties
├── db/migration/V1__init.sql
└── static
    ├── app.js
    ├── index.html
    └── styles.css
```

Root-level infrastructure:

```text
Dockerfile
.dockerignore
railway.json
.mvn/wrapper/maven-wrapper.properties
pom.xml
README.md
```

## 7. Tech Stack And Code Mapping

This section maps every major technology to the files where it is used.

| Technology | Purpose | Main Files |
| --- | --- | --- |
| Java 21 | Programming language | All `.java` files |
| Spring Boot 3.5.14 | Application runtime and auto-configuration | `StandupIqApplication.java`, `pom.xml` |
| Spring Web MVC | REST API controllers | `controller/*.java` |
| Spring WebFlux WebClient | Calling GitHub and Gemini APIs | `GitHubService.java`, `StandupService.java` |
| Spring Data JPA | Repository abstraction | `UserRepository.java`, `StandupRepository.java` |
| Hibernate | JPA implementation and ORM | `entity/*.java`, `application.properties` |
| PostgreSQL | Relational database | `application.properties`, `V1__init.sql` |
| Flyway | Database migrations | `pom.xml`, `V1__init.sql`, Flyway properties |
| Lombok | Reduces boilerplate | `@Data`, `@Slf4j` in entities/services |
| Gemini API | AI standup generation | `StandupService.java` |
| Slack incoming webhooks | Optional standup delivery | `SlackService.java` |
| Spring Scheduler | Automated daily generation | `ScheduledStandupJob.java`, `StandupIqApplication.java` |
| Springdoc OpenAPI | Swagger API docs | `OpenApiConfig.java`, controller annotations |
| JUnit 5 and Mockito | Unit testing | `src/test/java/...` |
| Maven | Build and dependency management | `pom.xml`, `.mvn/wrapper/...`, `mvnw` |
| Docker | Container packaging | `Dockerfile`, `.dockerignore` |
| Railway | Deployment target config | `railway.json`, Dockerfile |
| HTML/CSS/JS | Browser UI | `src/main/resources/static/*` |

## 8. Backend Concepts

### 8.1 Spring Boot

Spring Boot is a framework that simplifies creating Spring applications. It provides:

- Embedded Tomcat server.
- Auto-configuration.
- Dependency injection.
- Easy REST API creation.
- Integration with databases and external services.

Entry point:

```java
@SpringBootApplication
@EnableScheduling
public class StandupIqApplication {
    public static void main(String[] args) {
        SpringApplication.run(StandupIqApplication.class, args);
    }
}
```

`@SpringBootApplication` combines:

- `@Configuration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

`@EnableScheduling` activates Spring's scheduled task support so `ScheduledStandupJob` can run from a cron expression.

Interview explanation:

> Spring Boot starts an embedded server, scans my package for Spring components, creates beans, wires dependencies, and starts the REST application.

### 8.2 REST Controllers

Controllers expose HTTP endpoints.

Example files:

- `HealthController.java`
- `GitHubController.java`
- `StandupController.java`

Common annotations:

- `@RestController`: class returns JSON/text directly.
- `@RequestMapping`: base route for controller.
- `@GetMapping`: GET route.
- `@PathVariable`: reads value from URL path.
- `@RequestParam`: reads query parameters.
- `ResponseEntity`: lets you control HTTP status and response body.

Example:

```java
@GetMapping("/activity/{username}")
public ResponseEntity<GitHubActivityResponse> getActivity(...)
```

### 8.3 Service Layer

Services contain business logic.

Files:

- `GitHubService.java`
- `StandupService.java`
- `SlackService.java`
- `ScheduledStandupJob.java`

Why services exist:

- Keeps controllers thin.
- Makes business logic reusable.
- Makes testing easier.
- Separates HTTP routing from application behavior.

In this project:

- `GitHubService` fetches commits and pull requests.
- `StandupService` creates prompt, calls Gemini, saves standup.
- `SlackService` posts generated standups to Slack via webhook.
- `ScheduledStandupJob` automates generation on a configurable cron schedule.

### 8.4 Dependency Injection

Dependency injection means Spring creates objects and gives them to classes that need them.

Example:

```java
public StandupController(GitHubService gitHubService, StandupService standupService) {
    this.gitHubService = gitHubService;
    this.standupService = standupService;
}
```

This is constructor injection. It is generally preferred because dependencies are explicit and easier to test.

### 8.5 DTOs

DTO means Data Transfer Object.

DTOs define what the API sends or receives. They are separate from database entities.

Files:

- `CommitSummary.java`
- `PullRequestSummary.java`
- `GitHubActivityResponse.java`
- `StandupResponse.java`
- `StandupHistoryResponse.java`
- `ApiErrorResponse.java`

Why DTOs matter:

- Prevent exposing database internals.
- Keep API responses clean.
- Make JSON output predictable.
- Avoid returning raw third-party API responses.

Example:

```java
public record CommitSummary(
        String sha,
        String message,
        String url,
        Instant date,
        String repository
) {}
```

Records are concise immutable data classes introduced in modern Java.

## 9. Database And Persistence

### 9.1 PostgreSQL

PostgreSQL stores:

- Users.
- Generated standups.
- Standup metadata such as commit count and PR count.

Database config:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/standupiq
spring.datasource.username=standupuser
spring.datasource.password=standup123
```

### 9.2 JPA And Hibernate

JPA is the Java Persistence API. Hibernate is the implementation used by Spring Boot.

Entities:

- `User.java`
- `Standup.java`

Example:

```java
@Entity
@Table(name = "standups")
public class Standup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

Important annotations:

- `@Entity`: Java class maps to database table.
- `@Table`: custom table name.
- `@Id`: primary key.
- `@GeneratedValue`: database generates ID.
- `@Column`: column settings.
- `@PrePersist`: lifecycle hook before insert.

### 9.3 Repositories

Repositories provide database access without writing SQL manually.

Files:

- `UserRepository.java`
- `StandupRepository.java`

Example:

```java
public interface StandupRepository extends JpaRepository<Standup, Long> {
    List<Standup> findByUsernameOrderByGeneratedAtDesc(String username);
}
```

Spring Data JPA reads the method name and creates the query automatically.

### 9.4 Flyway

Flyway manages database schema changes through versioned migration files.

Migration file:

```text
src/main/resources/db/migration/V1__init.sql
```

Current migration creates:

- `users` table.
- `standups` table.
- index for standup history lookup.

Why Flyway is better than only `ddl-auto=update`:

- Migrations are versioned.
- Schema changes are reproducible.
- Production deployments are safer.
- Interviewers expect migration tools in serious backend projects.

Current setting:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Hibernate validates schema instead of modifying it automatically. Flyway owns schema creation.

## 10. GitHub Integration

File:

```text
GitHubService.java
```

Purpose:

- Fetch developer commits.
- Fetch pull requests.
- Support public and private repo activity.
- Support configurable time range.
- Deduplicate and sort results.

### 10.1 GitHub APIs Used

Search commits:

```text
GET /search/commits
```

Public repos:

```text
GET /users/{username}/repos
```

Authenticated accessible repos:

```text
GET /user/repos
```

Repo commits:

```text
GET /repos/{owner}/{repo}/commits
```

Pull request search:

```text
GET /search/issues
```

Repo pull requests:

```text
GET /repos/{owner}/{repo}/pulls
```

### 10.2 Why Two Commit Approaches Exist

GitHub search is useful but can miss:

- Private repository commits.
- Recently pushed commits due to indexing delay.
- Non-default branch commits depending on search behavior.

Direct repository commit API is more reliable when you know the repository and branch.

StandupIQ combines both:

```text
Search commits
  + direct repo commits
  -> merge
  -> deduplicate by SHA
```

### 10.3 Time Range

The API accepts:

```text
?days=1
?days=7
```

Implementation:

```java
Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
```

Validation:

```text
days must be between 1 and 30
```

### 10.4 Deduplication

Commits are deduped using SHA:

```java
Map<String, CommitSummary> commitsBySha = new LinkedHashMap<>();
```

Why SHA?

- Every Git commit has a unique SHA.
- Same commit can appear from search API and repo API.
- Dedup prevents double counting.

### 10.5 Private Repository Support

Private repo support depends on:

- GitHub token has correct scopes.
- Token user has access to repo.
- Direct repo APIs are used.

This project reads the token from:

```properties
github.token=${GITHUB_TOKEN:}
```

The token must be stored in `.env`, not committed.

## 11. Gemini AI Integration

File:

```text
StandupService.java
```

Purpose:

- Build a prompt from GitHub activity.
- Send the prompt to Gemini.
- Parse the model response.
- Save the result.
- Use fallback text if Gemini fails.

### 11.1 Gemini Config

```properties
gemini.api.key=${GEMINI_API_KEY:}
gemini.api.base-url=https://generativelanguage.googleapis.com
gemini.model=gemini-2.5-flash
```

The API key is read from `.env`:

```properties
GEMINI_API_KEY=your_key_here
```

### 11.2 Gemini API Call

The service calls:

```text
POST /v1beta/models/{model}:generateContent
```

It sends:

- System instruction.
- User prompt.
- Generation config.

### 11.3 Prompt Design

The prompt includes:

- Developer username.
- Number of days.
- Commit messages.
- Repository names.
- Commit dates.
- PR titles.
- PR states.

It asks Gemini to return exactly:

```text
Yesterday:
...

Today:
...

Blockers:
...
```

Prompt engineering concept:

- The more structured the input, the more reliable the output.
- The model should receive compact, relevant data instead of raw huge JSON.
- Clear output format improves frontend display and user trust.

### 11.4 Fallback Behavior

If Gemini key is missing or Gemini fails:

```text
Yesterday:
Worked on X commits and Y pull requests across GitHub activity.

Today:
Continue the in-progress development work and follow up on any related repository changes.

Blockers:
None.
```

Why fallback matters:

- Better user experience.
- Demonstrates resilient design.
- Avoids total failure when external AI API is down.

## 11A. Slack Delivery

File:

```text
SlackService.java
```

Purpose:

- Sends generated standups to a Slack channel.
- Uses an incoming webhook URL from environment variables.
- Returns `false` instead of crashing if Slack is disabled or misconfigured.

Config:

```properties
SLACK_ENABLED=true
SLACK_WEBHOOK_URL=your_slack_webhook_url
```

Endpoint usage:

```text
GET /api/standup/generate/{username}?days=1&sendToSlack=true
```

Interview explanation:

> I added Slack delivery as an optional side effect after standup generation. The generated standup is still saved even if Slack delivery fails, so an external notification failure does not break the core workflow.

## 11B. Scheduled Generation

File:

```text
ScheduledStandupJob.java
```

Purpose:

- Automatically generates a standup on a cron schedule.
- Can optionally send the generated standup to Slack.
- Is disabled by default so local development does not accidentally call external APIs.

Config:

```properties
STANDUP_SCHEDULER_ENABLED=true
STANDUP_SCHEDULER_CRON=0 45 8 * * MON-FRI
STANDUP_SCHEDULER_ZONE=Asia/Kolkata
STANDUP_SCHEDULER_USERNAME=JanWeeee
STANDUP_SCHEDULER_OWNER=JanWeeee
STANDUP_SCHEDULER_REPO=standup-iq
STANDUP_SCHEDULER_BRANCH=main
STANDUP_SCHEDULER_DAYS=1
STANDUP_SCHEDULER_SEND_TO_SLACK=true
```

Concept:

- `@Scheduled` runs a method based on a cron expression.
- The job reuses `GitHubService`, `StandupService`, and `SlackService`.
- Reusing services avoids duplicating business logic between manual and automated flows.

## 12. Frontend UI Flow

Files:

- `src/main/resources/static/index.html`
- `src/main/resources/static/styles.css`
- `src/main/resources/static/app.js`

The UI is served directly by Spring Boot at:

```text
http://localhost:8080/
```

### 12.1 UI Sections

Activity Source:

- GitHub username.
- Days.
- Fetch Activity button.
- Generate Standup button.

Activity Preview:

- Commits tab.
- Pull Requests tab.
- Commit count.
- PR count.

Generated Standup:

- Generated standup text.
- Copy button.

History:

- Previous standups.
- Commit and PR counts.

### 12.2 Frontend Concepts Used

HTML:

- Defines structure.
- Forms and semantic sections.

CSS:

- Responsive grid layout.
- Panels.
- Buttons.
- Tabs.
- Loading/status messages.

JavaScript:

- Fetch API.
- DOM manipulation.
- State management using a simple object.
- Error handling.
- Copy to clipboard.

### 12.3 Why Static UI Is Good For This Stage

Using static HTML/CSS/JS is good for a resume demo because:

- No separate React build needed yet.
- Same Spring Boot app serves UI and API.
- No CORS issues.
- Easy to deploy as one app.

Future upgrade:

- React + Vite frontend.
- OAuth login.
- Better routing and component structure.

## 13. Error Handling

Files:

- `GlobalExceptionHandler.java`
- `ApiErrorResponse.java`
- `ExternalServiceException.java`
- `ResourceNotFoundException.java`

### 13.1 Global Exception Handler

`@RestControllerAdvice` catches exceptions across controllers.

It converts Java exceptions into clean JSON:

```json
{
  "timestamp": "...",
  "status": 500,
  "error": "Internal Server Error",
  "message": "GitHub API failed during search commits",
  "path": "/api/github/activity/JanWeeee"
}
```

### 13.2 Why This Matters

Without global handling:

- Users may see stack traces.
- API responses are inconsistent.
- Debug details may leak.

With global handling:

- Responses are predictable.
- UI can show meaningful error messages.
- Production app looks professional.

## 14. Configuration And Secrets

Main config file:

```text
src/main/resources/application.properties
```

Local secrets file:

```text
.env
```

Important:

- `.env` is ignored by Git.
- Real tokens must never be committed.
- `application.properties` should contain placeholders, not real secrets.

Current placeholders:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/standupiq}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:standupuser}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:standup123}
github.token=${GITHUB_TOKEN:}
gemini.api.key=${GEMINI_API_KEY:}
slack.enabled=${SLACK_ENABLED:false}
slack.webhook.url=${SLACK_WEBHOOK_URL:}
```

Why the `:` exists:

```text
${GITHUB_TOKEN:}
```

It means:

- Use `GITHUB_TOKEN` if available.
- Otherwise use empty string.

This prevents the app from failing during startup when the variable is missing.

Scheduler config is also environment-driven:

```properties
standup.scheduler.enabled=${STANDUP_SCHEDULER_ENABLED:false}
standup.scheduler.cron=${STANDUP_SCHEDULER_CRON:0 45 8 * * MON-FRI}
standup.scheduler.username=${STANDUP_SCHEDULER_USERNAME:}
```

This is production-friendly because deployment platforms such as Railway provide configuration through environment variables instead of committed files.

## 15. Build, Test, Docker, And Deployment

### 15.1 Maven

Maven manages:

- Dependencies.
- Compile.
- Tests.
- Packaging jar.

Commands:

```bash
./mvnw clean test
./mvnw -DskipTests package
./mvnw spring-boot:run
```

### 15.2 Maven Wrapper

Files:

```text
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
```

Why wrapper matters:

- Anyone can build project without installing Maven manually.
- CI/CD can use the same command.
- More reproducible builds.

### 15.3 Docker

File:

```text
Dockerfile
```

Purpose:

- Package the app into a container.
- Run same app in local, staging, production.

Current Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/standup-iq-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build:

```bash
docker build -t standup-iq .
docker run --env-file .env -p 8080:8080 standup-iq
```

### 15.4 Unit Tests

Tests live under:

```text
src/test/java/com/standupiq/standup_iq/service
```

Current test coverage:

- `StandupServiceTest`: fallback generation, persistence mapping, history behavior.
- `GitHubServiceTest`: missing token and invalid day validation.
- `SlackServiceTest`: disabled and missing webhook behavior.
- `ScheduledStandupJobTest`: scheduler skip/run paths.

### 15.5 Railway Deployment

File:

```text
railway.json
```

Railway uses the Dockerfile to build and run the app. Required variables:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://host:port/database
SPRING_DATASOURCE_USERNAME=your_database_user
SPRING_DATASOURCE_PASSWORD=your_database_password
GITHUB_TOKEN=your_github_token
GEMINI_API_KEY=your_gemini_key
```

GitHub Actions is not included yet because pushing workflow files requires a GitHub token with `workflow` permission. This can be added later after token scopes are updated.

## 16. API Reference

### Health

```text
GET /api/health
```

Returns:

```json
{
  "status": "UP",
  "app": "StandupIQ",
  "message": "Welcome to StandupIQ - Your AI Standup Generator"
}
```

### GitHub Debug

```text
GET /api/github/debug/{username}?days=1
```

Returns current UTC time and calculated `sinceDate`.

### GitHub Activity

```text
GET /api/github/activity/{username}?days=7
```

Returns:

- username
- days
- totalCommits
- totalPRs
- commits
- pullRequests

Optional API parameters:

- `owner`
- `repo`
- `branch`

### Generate Standup

```text
GET /api/standup/generate/{username}?days=7&sendToSlack=false
```

Does:

- Fetches activity.
- Generates Gemini standup.
- Saves to database.
- Optionally posts the result to Slack.
- Returns activity + standup text.

### Standup History

```text
GET /api/standup/history/{username}
```

Returns saved standups newest first.

### Swagger UI

```text
GET /swagger-ui.html
```

Opens interactive API documentation generated from the Spring controllers.

## 17. Resume Talking Points

Use this explanation:

> StandupIQ is a Java 21 Spring Boot application that generates AI-powered daily standups from real GitHub activity. I built REST APIs that fetch commits and pull requests through GitHub's REST API, including a fallback strategy using direct repository endpoints to handle private repos and search indexing limitations. I use Gemini for natural-language generation, PostgreSQL for persistence, Flyway for schema migrations, Swagger for API documentation, Slack webhooks and Spring Scheduler for delivery automation, Docker for containerization, and Railway-ready deployment config. The app also includes a browser UI for fetching activity, previewing commits, generating standups, and viewing history.

Key strengths:

- Real-world problem.
- External API integration.
- AI integration.
- Database persistence.
- Clean layered architecture.
- Error handling.
- Configurable time range.
- Docker and Railway deployment readiness.
- Swagger API docs.
- Unit-tested service behavior.
- Slack and scheduled delivery.
- Browser demo.

What to highlight:

- You did not just call AI directly.
- You used GitHub as the source of truth.
- You handled private repo/search limitations.
- You persisted generated standups.
- You designed for future team workflows.

## 18. Limitations And Future Improvements

Current limitations:

- Uses one GitHub token from environment.
- No OAuth login yet.
- No Jira integration yet.
- UI is static HTML/CSS/JS, not React.
- No pagination controls in UI.
- No integration tests using Testcontainers yet.
- No team dashboard yet.

Best next improvements:

1. GitHub OAuth so each user connects their own account.
2. Jira integration for ticket context.
3. React frontend.
4. Integration tests using Testcontainers.
5. Team dashboard.
6. GitHub Actions after adding token `workflow` scope.
7. Email delivery for users who do not use Slack.

## 19. Interview Questions And Answers

### Project-Level Questions

#### Q1. What problem does StandupIQ solve?

It saves developers from manually remembering and collecting their work before standup. It reads actual GitHub activity, generates a professional standup using AI, and stores the generated standup for history.

#### Q2. Why is this better than asking an AI chatbot what I did?

Because StandupIQ reads from GitHub, which is the source of truth. It does not depend on chat history, local memory, or a single coding session.

#### Q3. What are the main modules?

Controllers expose REST APIs, services contain business logic, repositories handle database access, entities map to database tables, DTOs define API responses, and static resources provide the UI.

#### Q4. Explain the complete generate standup flow.

The browser calls `/api/standup/generate/{username}`. `StandupController` calls `GitHubService` to fetch commits and PRs. It then passes that activity to `StandupService`, which builds a prompt and calls Gemini. The generated text is saved as a `Standup` entity through `StandupRepository`, then returned as a `StandupResponse`.

### Spring Boot Questions

#### Q5. What is Spring Boot?

Spring Boot is a framework that simplifies Spring application development with auto-configuration, embedded servers, dependency management, and production-ready defaults.

#### Q6. What does `@SpringBootApplication` do?

It combines configuration, auto-configuration, and component scanning. It tells Spring Boot where to start and which package tree to scan for beans.

#### Q7. What is dependency injection?

Dependency injection means objects receive their dependencies from the framework instead of creating them manually. In this project, controllers receive services and services receive repositories through constructors.

#### Q8. Why use service classes?

Service classes keep business logic separate from HTTP routing. This makes the code cleaner, easier to test, and easier to extend.

### REST API Questions

#### Q9. What is REST?

REST is an architectural style for building APIs around resources using HTTP methods such as GET, POST, PUT, and DELETE.

#### Q10. Why are your generate endpoints GET instead of POST?

For a production system, generating and saving a standup changes server state, so POST would be more REST-correct. GET works for a simple browser demo, but a production version should use `POST /api/standup/generate`.

This is a good interview answer because it shows you understand the tradeoff.

#### Q11. What is `ResponseEntity`?

`ResponseEntity` lets us return both response body and HTTP status code from a controller.

#### Q12. What status codes should this project return?

- `200 OK` for successful reads/generation.
- `400 Bad Request` for invalid query parameters.
- `404 Not Found` when no history or repositories are found.
- `500 Internal Server Error` for external service failures with clean error messages.

### DTO And Entity Questions

#### Q13. What is the difference between DTO and Entity?

An entity maps to a database table. A DTO defines data sent through the API. Entities are persistence models; DTOs are API models.

#### Q14. Why not return raw GitHub JSON?

Raw GitHub JSON is huge, unstable, and exposes fields the frontend does not need. DTOs keep responses small, clean, and controlled.

#### Q15. Why use Java records for DTOs?

Records are concise immutable data carriers. They reduce boilerplate and are ideal for response objects.

### JPA And Database Questions

#### Q16. What is JPA?

JPA is a Java specification for mapping Java objects to relational database tables.

#### Q17. What is Hibernate?

Hibernate is an implementation of JPA. Spring Boot uses it to perform ORM operations.

#### Q18. What does `JpaRepository` provide?

It provides common CRUD methods such as `save`, `findById`, `findAll`, and `delete`, plus query derivation from method names.

#### Q19. Explain `findByUsernameOrderByGeneratedAtDesc`.

Spring Data JPA parses the method name and creates a query that finds standups by username ordered by generated timestamp descending.

#### Q20. Why use Flyway?

Flyway gives version-controlled database migrations. It makes database schema changes reproducible across local, staging, and production environments.

#### Q21. Why change `ddl-auto` from `update` to `validate`?

`update` lets Hibernate change the schema automatically, which is risky in production. `validate` ensures the schema matches entities while Flyway controls actual schema changes.

### GitHub API Questions

#### Q22. How does StandupIQ fetch commits?

It uses both GitHub search API and direct repository commits API. It merges both sources and deduplicates by commit SHA.

#### Q23. Why not only use GitHub search API?

GitHub search can miss private repo commits, recently pushed commits, or branch-specific commits due to indexing behavior. Direct repository APIs are more reliable when repo access is available.

#### Q24. What is a commit SHA?

A SHA is a unique hash identifying a Git commit. It is used for deduplication.

#### Q25. How does private repo access work?

The GitHub token must have permission to access private repositories. The app calls authenticated endpoints like `/user/repos` and direct repo endpoints with Bearer token auth.

#### Q26. What happens if GitHub fails?

The service throws `ExternalServiceException`, and `GlobalExceptionHandler` returns a clean JSON error instead of exposing a stack trace.

### WebClient Questions

#### Q27. What is WebClient?

WebClient is Spring's modern HTTP client. It is part of Spring WebFlux and supports reactive and blocking HTTP calls.

#### Q28. Are you using WebClient reactively?

The project uses WebClient but calls `.block()` to keep the service flow simple and synchronous. A future advanced version could return `Mono`/`Flux` for non-blocking behavior.

#### Q29. Why use WebClient instead of RestTemplate?

RestTemplate is older and in maintenance mode. WebClient is the recommended modern client for external HTTP calls.

### AI Integration Questions

#### Q30. How is Gemini used?

`StandupService` builds a prompt from commits and PRs, calls Gemini's `generateContent` API, extracts the generated text, and saves it to PostgreSQL.

#### Q31. What is prompt engineering in this project?

Prompt engineering is structuring the input so Gemini knows exactly what to do. The prompt includes commit messages, repository names, PR titles, states, and required output format.

#### Q32. Why include a fallback standup?

External AI APIs can fail due to network, quota, or invalid key issues. Fallback keeps the product usable and improves reliability.

#### Q33. How do you know Gemini is working?

The output mentions actual commit work instead of generic fallback text like "Worked on X commits". The logs also do not show a Gemini failure warning.

### Error Handling Questions

#### Q34. What is `@RestControllerAdvice`?

It is a Spring annotation that applies exception handling across all REST controllers.

#### Q35. Why not return stack traces?

Stack traces can expose internal implementation details and confuse users. Production APIs should return clean, structured errors.

### Security Questions

#### Q36. Where are secrets stored?

Secrets are stored in `.env` locally and environment variables in deployment. They are not committed to Git.

#### Q37. Why is committing tokens dangerous?

Anyone with repo access can steal the token. GitHub may block pushes using secret scanning. Tokens should be rotated immediately if exposed.

#### Q38. Is the current app multi-user safe?

Not fully. It currently uses one backend GitHub token. A production multi-user version should use GitHub OAuth so each developer connects their own account securely.

### Docker And Deployment Questions

#### Q39. What does the Dockerfile do?

It uses a Java 21 build stage to package the Spring Boot jar, then copies that jar into a smaller Java 21 runtime image, exposes port 8080, and runs it with `java -jar`.

#### Q40. Why use Docker?

Docker packages the app and runtime environment consistently, making deployment more reliable.

#### Q41. How is this ready for Railway deployment?

The repo has `railway.json`, a Dockerfile that builds the jar from source, and environment-variable-based configuration for database, GitHub, Gemini, Slack, and scheduler settings.

### Frontend Questions

#### Q42. Why serve static UI from Spring Boot?

It keeps the demo simple. The backend and frontend run from one app, avoiding CORS and separate deployment complexity.

#### Q43. What does `app.js` do?

It calls backend APIs using Fetch, renders commits and PRs, generates standups, copies text to clipboard, and displays history.

#### Q44. How would you improve the UI?

Use React + Vite, add routing, loading skeletons, better history filtering, authentication, and deployment as a separate frontend if needed.

### Advanced Design Questions

#### Q45. How would you scale this project?

Add OAuth, store encrypted tokens per user, use background jobs for scheduled generation, cache GitHub responses, add retry/rate-limit handling, move AI generation to async jobs, and deploy with managed PostgreSQL.

#### Q46. How would you handle GitHub rate limits?

Read rate-limit headers, reduce API calls, cache repository lists, use conditional requests, and return clear errors when limits are exceeded.

#### Q47. How would you test this project better?

Add unit tests for services, mock GitHub/Gemini WebClient calls, use Testcontainers for PostgreSQL integration tests, and add controller tests with MockMvc.

#### Q48. What design pattern does this architecture use?

It mainly uses layered architecture: controller, service, repository, entity, DTO.

#### Q49. What are the biggest production gaps?

OAuth, encrypted token storage, deployment environment configuration, better test coverage, rate-limit handling, logging/monitoring, and POST-based generation endpoint.

#### Q50. How would you explain this project in one minute?

StandupIQ is a Spring Boot and Java 21 backend with a lightweight browser UI that turns GitHub commits and pull requests into AI-generated daily standups. It uses GitHub REST APIs for source-of-truth activity, Gemini for natural-language generation, PostgreSQL and Flyway for persistence, Swagger for API docs, Slack and scheduler support for delivery automation, and Docker/Railway config for deployment readiness. I designed it with DTOs, service separation, error handling, private repository support, unit tests, and standup history so it looks like a real production-oriented backend project.

## 20. Study Roadmap

Use this order to learn the project deeply:

1. Run the app and test all endpoints.
2. Read `HealthController` to understand a simple controller.
3. Read `User` and `Standup` entities to understand JPA mapping.
4. Read `V1__init.sql` to understand database schema.
5. Read repositories to understand Spring Data JPA.
6. Read DTOs to understand API response shapes.
7. Read `GitHubController`, then `GitHubService`.
8. Trace one activity request from browser to GitHub and back.
9. Read `StandupController`, then `StandupService`.
10. Trace one standup generation request from browser to Gemini to database.
11. Read `GlobalExceptionHandler`.
12. Read frontend `index.html`, `styles.css`, and `app.js`.
13. Read `Dockerfile`, `railway.json`, `SlackService`, and `ScheduledStandupJob`.
14. Practice interview questions from this guide.
15. Add one improvement yourself, such as GitHub OAuth or Testcontainers.

## Quick Demo Script

Use this during a resume walkthrough:

1. Open `http://localhost:8080/`.
2. Show the form with username and time range.
3. Click Fetch Activity.
4. Explain that the backend calls GitHub search plus direct repo APIs.
5. Show commits and PRs preview.
6. Click Generate Standup.
7. Explain that the backend sends compact activity to Gemini.
8. Show the generated Yesterday, Today, Blockers text.
9. Show the optional Slack delivery checkbox and Swagger UI.
10. Show History.
11. Explain that generated standups are stored in PostgreSQL through JPA and Flyway-managed schema.

## Final Mental Model

Think of StandupIQ as four connected parts:

```text
GitHub activity collector
    gets facts

AI standup generator
    turns facts into language

Database history
    stores generated output

Slack and scheduler
    deliver generated standups automatically

Browser UI
    makes it easy to use and demo
```

That is the core story to remember.
