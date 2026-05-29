# Global Class Offering Booking System

Backend service for a live-learning platform where teachers create course offerings in their own timezone and parents book complete offerings from their local timezone.

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Cloud OpenFeign
- Spring Cloud Gateway
- PostgreSQL 16
- Flyway migrations
- Spring Data JPA
- Springdoc Swagger UI / OpenAPI
- Docker Compose

## Project Structure

Each service follows the same layered architecture:

- `entity`: JPA entities mapped to database tables
- `dto`: request and response records used by the API layer
- `controller`: REST endpoints
- `service`: business rules, validation, timezone conversion, booking orchestration
- `repository`: Spring Data database access
- `exception`: custom exceptions and global error responses
- `config`: Swagger/OpenAPI configuration

Booking Service also has:

- `client`: OpenFeign client for Course Service communication

API Gateway is intentionally thin. It only routes requests and exposes a combined Swagger UI.

## Services

### Course Service

Runs on port `8081`.

Owns:

- Courses
- Offerings
- Sessions
- Teacher APIs
- Internal offering APIs used by Booking Service

### Booking Service

Runs on port `8082`.

Owns:

- Parent bookings
- Parent-level booking locks
- Parent APIs
- Conflict detection

Booking Service calls Course Service through OpenFeign.

### API Gateway

Runs on port `8080`.

Routes:

- `/course/**` -> Course Service
- `/booking/**` -> Booking Service

The gateway Swagger UI lists both downstream OpenAPI documents.

## Database Design

### course_db

- `courses`
- `offerings`
- `course_sessions`

Sessions store `start_at` and `end_at` as `timestamptz`. Teacher-provided local times are converted to UTC instants before persistence.

### booking_db

- `bookings`
- `parent_booking_locks`

`parent_booking_locks` has one row per parent. Booking Service locks that row with `PESSIMISTIC_WRITE` while it checks and creates a booking.

## Concurrency Approach

Booking is handled inside a `SERIALIZABLE` transaction.

For each booking request, Booking Service:

1. Ensures a parent lock row exists.
2. Takes a PostgreSQL pessimistic write lock on that parent row.
3. Checks duplicate booking for the same offering.
4. Fetches the offering and sessions from Course Service using Feign.
5. Fetches the parent's existing booked offerings.
6. Compares all requested sessions against already booked sessions.
7. Creates the booking only when no session overlaps.

This handles:

- Two requests from the same parent trying to book overlapping offerings
- Duplicate booking attempts for the same parent and offering
- Concurrent bookings by many different parents

Multiple parents can book the same offering because the conflict rule is parent-specific.

## Timezone Approach

Teacher APIs accept local date-time values and an IANA timezone such as `Asia/Kolkata` or `America/New_York`.

The Course Service stores all session times as UTC instants in PostgreSQL `timestamptz` columns. Parent-facing APIs accept a `timezone` query parameter and return both:

- `startAtUtc` / `endAtUtc`
- `localStart` / `localEnd` in the requested timezone

Use IANA timezone names, not abbreviations like `IST` or `PST`.

## Environment Variables

### Course Service

| Name | Default |
| --- | --- |
| `COURSE_SERVICE_PORT` | `8081` |
| `COURSE_DB_URL` | `jdbc:postgresql://localhost:5432/course_db` |
| `COURSE_DB_USERNAME` | `postgres` |
| `COURSE_DB_PASSWORD` | `postgres` |

### Booking Service

| Name | Default |
| --- | --- |
| `BOOKING_SERVICE_PORT` | `8082` |
| `BOOKING_DB_URL` | `jdbc:postgresql://localhost:5432/booking_db` |
| `BOOKING_DB_USERNAME` | `postgres` |
| `BOOKING_DB_PASSWORD` | `postgres` |
| `COURSE_SERVICE_URL` | `http://localhost:8081` |

## Run Locally With Docker

```bash
docker compose up --build
```

Services:

- API Gateway: `http://localhost:8080`
- Course Service: `http://localhost:8081`
- Booking Service: `http://localhost:8082`
- PostgreSQL: `localhost:5432`

## Run Locally Without Docker

Install Java 21, Maven 3.9+, and PostgreSQL.

Create databases:

```sql
create database course_db;
create database booking_db;
```

Start Course Service:

```bash
mvn -pl course-service spring-boot:run
```

Start Booking Service in another terminal:

```bash
mvn -pl booking-service spring-boot:run
```

Flyway runs migrations automatically on startup.

## Swagger / OpenAPI Documentation

Open these URLs after the services are running:

- Gateway Swagger UI: `http://localhost:8080/swagger-ui.html`
- Course OpenAPI through Gateway: `http://localhost:8080/course/v3/api-docs`
- Booking OpenAPI through Gateway: `http://localhost:8080/booking/v3/api-docs`
- Course Service Swagger UI: `http://localhost:8081/swagger-ui.html`
- Course Service OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Booking Service Swagger UI: `http://localhost:8082/swagger-ui.html`
- Booking Service OpenAPI JSON: `http://localhost:8082/v3/api-docs`

To submit Swagger documentation, open `/v3/api-docs` in the browser and save the JSON, or run:

```bash
curl http://localhost:8080/course/v3/api-docs -o course-service-openapi.json
curl http://localhost:8080/booking/v3/api-docs -o booking-service-openapi.json
```

## API Documentation

### Teacher APIs

#### Create Offering

`POST /api/v1/teacher/offerings`

```json
{
  "teacherId": "11111111-1111-1111-1111-111111111111",
  "courseName": "Minecraft Coding",
  "offeringName": "Saturday Batch",
  "teacherTimezone": "Asia/Kolkata"
}
```

#### Add Session

`POST /api/v1/teacher/offerings/{offeringId}/sessions`

```json
{
  "localStart": "2026-06-06T18:00:00",
  "localEnd": "2026-06-06T19:00:00"
}
```

Optional `timezone` can be sent if a specific session should use a timezone different from the offering timezone.

#### Get Teacher Offerings

`GET /api/v1/teacher/{teacherId}/offerings?timezone=Asia/Kolkata`

### Parent APIs

#### Get Available Offerings

`GET /api/v1/parent/offerings?timezone=America/New_York`

#### Book Offering

`POST /api/v1/parent/bookings`

```json
{
  "parentId": "22222222-2222-2222-2222-222222222222",
  "offeringId": "33333333-3333-3333-3333-333333333333",
  "timezone": "America/New_York"
}
```

#### Get Bookings

`GET /api/v1/parent/bookings?parentId=22222222-2222-2222-2222-222222222222&timezone=America/New_York`

## Assumptions

- Authentication is outside the scope of this assignment; `teacherId` and `parentId` are supplied as UUIDs.
- Capacity limits are outside the scope; many parents may book the same offering.
- Parents book an entire offering, never individual sessions.
- An offering becomes published after its first session is added.
- The conflict rule is per parent, across every session in their confirmed bookings.

## Useful Test Flow

1. Create an offering in Course Service.
2. Add multiple sessions to that offering.
3. Call Booking Service `GET /api/v1/parent/offerings` with a parent timezone.
4. Book the offering.
5. Try booking another offering with an overlapping session for the same parent.
6. The second booking should return `409 BOOKING_CONFLICT`.
