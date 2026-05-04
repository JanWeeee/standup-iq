# StandupIQ

AI-powered daily standup generator for software developers.

StandupIQ eliminates the morning context-switching developers do before standup. Instead of manually checking commits, pull requests, and memory, it reads GitHub activity directly from the source of truth and generates a professional standup with AI. The backend supports public and private repository activity, configurable time ranges, generated standup history, Swagger API docs, scheduled generation, and optional Slack delivery.

## Architecture

Client or API consumer -> Spring Boot REST controllers -> GitHubService for repository activity -> StandupService for Gemini generation -> PostgreSQL for users and generated standup history. Optional delivery flows send generated standups to Slack immediately or from the scheduled job.

The GitHub layer combines search API results with direct repository commit and pull request calls. This improves coverage for private repositories where GitHub search indexing can be delayed or incomplete. The AI layer sends a compact activity summary to Gemini and stores the generated result for analytics and history.

## Tech Stack

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring WebFlux WebClient
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Lombok
- Google Gemini API
- Slack incoming webhooks
- Spring Scheduler
- Springdoc OpenAPI / Swagger UI
- JUnit 5 and Mockito
- Maven
- Docker
- Railway deployment target

## Run Locally

1. Start PostgreSQL and create the local database:

```bash
createdb standupiq
```

2. Create a database user if needed:

```sql
CREATE USER standupuser WITH PASSWORD 'standup123';
GRANT ALL PRIVILEGES ON DATABASE standupiq TO standupuser;
```

3. Create a local `.env` file in the project root:

```properties
GITHUB_TOKEN=your_github_token
GEMINI_API_KEY=your_gemini_key
SLACK_ENABLED=false
SLACK_WEBHOOK_URL=
```

4. Build and test:

```bash
./mvnw clean test
```

5. Run the app:

```bash
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

The browser demo runs on:

```text
http://localhost:8080/
```

Swagger UI runs on:

```text
http://localhost:8080/swagger-ui.html
```

## API Endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/health` | Returns application health status. |
| GET | `/api/users/test` | Saves a sample user to PostgreSQL. |
| GET | `/api/github/activity/{username}` | Fetches GitHub commits and PRs for a user. Supports `days`, `owner`, `repo`, and `branch` query parameters. |
| GET | `/api/standup/generate/{username}` | Fetches GitHub activity, generates a Gemini standup, saves it, and returns the result. Supports `days`, `owner`, `repo`, `branch`, and `sendToSlack`. |
| GET | `/api/standup/history/{username}` | Returns previously generated standups for a user. |
| GET | `/swagger-ui.html` | Interactive Swagger API documentation. |

## Example Activity Response

```json
{
  "username": "JanWeeee",
  "owner": "JanWeeee",
  "repo": "standup-iq",
  "branch": "feature/phase-1-2-3-foundation",
  "days": 1,
  "totalCommits": 2,
  "totalPRs": 0,
  "commits": [
    {
      "sha": "d5db476...",
      "message": "Build StandupIQ foundation",
      "url": "https://github.com/JanWeeee/standup-iq/commit/d5db476...",
      "date": "2026-05-02T00:11:22Z",
      "repository": "JanWeeee/standup-iq"
    }
  ],
  "pullRequests": [],
  "message": "GitHub activity fetched successfully"
}
```

## Example Standup Response

```json
{
  "id": 1,
  "username": "JanWeeee",
  "days": 1,
  "generatedAt": "2026-05-02T01:30:00",
  "standupText": "Yesterday:\nWorked on the StandupIQ backend foundation...\n\nToday:\nContinue refining GitHub activity aggregation...\n\nBlockers:\nNone.",
  "slackDelivered": true
}
```

## Docker

Build and run the image:

```bash
docker build -t standup-iq .
docker run --env-file .env -p 8080:8080 standup-iq
```

The Dockerfile uses a Java 21 build stage and a smaller Java 21 runtime stage, so it can be built directly by Railway.

## Scheduled Standups

Scheduled generation is disabled by default. Enable it with environment variables:

```properties
STANDUP_SCHEDULER_ENABLED=true
STANDUP_SCHEDULER_CRON=0 45 8 * * MON-FRI
STANDUP_SCHEDULER_ZONE=Asia/Kolkata
STANDUP_SCHEDULER_USERNAME=JanWeeee
STANDUP_SCHEDULER_OWNER=JanWeeee
STANDUP_SCHEDULER_REPO=standup-iq
STANDUP_SCHEDULER_BRANCH=feature/phase-1-2-3-foundation
STANDUP_SCHEDULER_DAYS=1
STANDUP_SCHEDULER_SEND_TO_SLACK=true
SLACK_ENABLED=true
SLACK_WEBHOOK_URL=your_slack_webhook_url
```

## Railway Deployment

This repo includes `railway.json` and a Dockerfile that Railway can build.

Required Railway variables:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://host:port/database
SPRING_DATASOURCE_USERNAME=your_database_user
SPRING_DATASOURCE_PASSWORD=your_database_password
GITHUB_TOKEN=your_github_token
GEMINI_API_KEY=your_gemini_key
```

Railway provides `PORT` automatically; locally the app defaults to `8080`.

Optional Railway variables:

```properties
SLACK_ENABLED=true
SLACK_WEBHOOK_URL=your_slack_webhook_url
STANDUP_SCHEDULER_ENABLED=true
STANDUP_SCHEDULER_USERNAME=your_github_username
STANDUP_SCHEDULER_OWNER=repo_owner
STANDUP_SCHEDULER_REPO=repo_name
STANDUP_SCHEDULER_BRANCH=main
STANDUP_SCHEDULER_DAYS=1
STANDUP_SCHEDULER_SEND_TO_SLACK=true
```

GitHub Actions is intentionally not included yet because workflow-file pushes require a GitHub token with `workflow` scope. Add CI/CD after the token scope is updated.

## Future Scope

- Jira integration for ticket transitions and issue context.
- Team dashboard for managers to view all generated standups.
- GitHub OAuth so each developer can connect their own repositories securely.

## Learning Guide

For a complete project walkthrough, code-to-tech-stack mapping, core concepts, and interview preparation questions, see [docs/PROJECT_LEARNING_GUIDE.md](docs/PROJECT_LEARNING_GUIDE.md).
