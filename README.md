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

The Course Service stores all session times as UTC instants in PostgreSQL `timestamptz` columns. Parent-facing APIs accept a `timezone` query parameter and return standard ISO-8601 datetime values:

- `startAtUtc` / `endAtUtc`, for example `2026-06-13T17:30:00Z`
- `localStart` / `localEnd` in the requested timezone, for example `2026-06-13T23:00:00+05:30`

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

Install Docker Desktop, start it, then run from the repository root:

```bash
docker compose up --build
```

Services:

- API Gateway: `http://localhost:8080`
- Course Service through Gateway: `http://localhost:8080/course`
- Booking Service through Gateway: `http://localhost:8080/booking`
- Course Service: `http://localhost:8081`
- Booking Service: `http://localhost:8082`
- PostgreSQL: `localhost:5432`

Useful Docker commands:

```bash
docker compose ps
docker compose logs -f booking-service
docker compose down
```

If Booking Service is not reachable, first check `docker compose ps`. It depends on Course Service and uses `COURSE_SERVICE_URL=http://course-service:8081` inside Docker.

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

All examples below use the API Gateway on port `8080`. To call services directly, remove `/course` and use port `8081` for Course Service, or remove `/booking` and use port `8082` for Booking Service.

### Teacher APIs

#### Create Offering

`POST /course/api/v1/teacher/offerings`

```bash
curl -X POST http://localhost:8080/course/api/v1/teacher/offerings \
  -H "Content-Type: application/json" \
  -d '{"teacherId":"11111111-1111-1111-1111-111111111111","courseName":"Minecraft Coding","offeringName":"Saturday Batch","teacherTimezone":"Asia/Kolkata"}'
```

```bash
curl -X POST http://localhost:8080/course/api/v1/teacher/offerings \
  -H "Content-Type: application/json" \
  -d '{"teacherId":"22222222-2222-2222-2222-222222222222","courseName":"Roblox Game Design","offeringName":"New York Weekend","teacherTimezone":"America/New_York"}'
```

```bash
curl -X POST http://localhost:8080/course/api/v1/teacher/offerings \
  -H "Content-Type: application/json" \
  -d '{"teacherId":"33333333-3333-3333-3333-333333333333","courseName":"Art Drawing","offeringName":"Tokyo Morning","teacherTimezone":"Asia/Tokyo"}'
```

#### Add Session (Copy paste the offering ID from the 'course/api/v1/teacher/offerings' reponse )

`POST /course/api/v1/teacher/offerings/{offeringId}/sessions`

```bash
curl -X POST http://localhost:8080/course/api/v1/teacher/offerings/{offeringId}/sessions \
  -H "Content-Type: application/json" \
  -d '{"localStart":"2026-06-06T18:00:00","localEnd":"2026-06-06T19:00:00"}'
```

```bash
curl -X POST http://localhost:8080/course/api/v1/teacher/offerings/{offeringId}/sessions \
  -H "Content-Type: application/json" \
  -d '{"localStart":"2026-06-13T10:00:00","localEnd":"2026-06-13T11:30:00","timezone":"America/New_York"}'
```

```bash
curl -X POST http://localhost:8080/course/api/v1/teacher/offerings/{offeringId}/sessions \
  -H "Content-Type: application/json" \
  -d '{"localStart":"2026-06-20T09:00:00","localEnd":"2026-06-20T10:00:00","timezone":"Asia/Tokyo"}'
```

Optional `timezone` can be sent if a specific session should use a timezone different from the offering timezone.

#### Get Teacher Offerings

```bash
curl "http://localhost:8080/course/api/v1/teacher/11111111-1111-1111-1111-111111111111/offerings?timezone=Asia/Kolkata"
```

```bash
curl "http://localhost:8080/course/api/v1/teacher/22222222-2222-2222-2222-222222222222/offerings?timezone=America/New_York"
```

```bash
curl "http://localhost:8080/course/api/v1/teacher/33333333-3333-3333-3333-333333333333/offerings?timezone=Europe/London"
```

### Internal Course APIs

These are used by Booking Service, but can be called while testing.

#### Get Internal Available Offerings

```bash
curl "http://localhost:8080/course/internal/v1/offerings?timezone=Asia/Kolkata"
```

```bash
curl "http://localhost:8080/course/internal/v1/offerings?timezone=America/New_York"
```

```bash
curl "http://localhost:8080/course/internal/v1/offerings?timezone=Europe/London"
```

#### Get Internal Offering By ID

```bash
curl "http://localhost:8080/course/internal/v1/offerings/{offeringId}?timezone=Asia/Kolkata"
```

```bash
curl "http://localhost:8080/course/internal/v1/offerings/{offeringId}?timezone=America/New_York"
```

```bash
curl "http://localhost:8080/course/internal/v1/offerings/{offeringId}?timezone=Asia/Tokyo"
```

### Parent APIs

#### Get Available Offerings

```bash
curl "http://localhost:8080/booking/api/v1/parent/offerings?timezone=Asia/Kolkata"
```

```bash
curl "http://localhost:8080/booking/api/v1/parent/offerings?timezone=America/New_York"
```

```bash
curl "http://localhost:8080/booking/api/v1/parent/offerings?timezone=Europe/London"
```

#### Book Offering

`POST /booking/api/v1/parent/bookings`

```bash
curl -X POST http://localhost:8080/booking/api/v1/parent/bookings \
  -H "Content-Type: application/json" \
  -d '{"parentId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","offeringId":"{offeringId}","timezone":"Asia/Kolkata"}'
```

```bash
curl -X POST http://localhost:8080/booking/api/v1/parent/bookings \
  -H "Content-Type: application/json" \
  -d '{"parentId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","offeringId":"{offeringId}","timezone":"America/New_York"}'
```

```bash
curl -X POST http://localhost:8080/booking/api/v1/parent/bookings \
  -H "Content-Type: application/json" \
  -d '{"parentId":"cccccccc-cccc-cccc-cccc-cccccccccccc","offeringId":"{offeringId}","timezone":"Europe/London"}'
```

#### Get Bookings

```bash
curl "http://localhost:8080/booking/api/v1/parent/bookings?parentId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa&timezone=Asia/Kolkata"
```

```bash
curl "http://localhost:8080/booking/api/v1/parent/bookings?parentId=bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb&timezone=America/New_York"
```

```bash
curl "http://localhost:8080/booking/api/v1/parent/bookings?parentId=cccccccc-cccc-cccc-cccc-cccccccccccc&timezone=Europe/London"
```

## Assumptions

- Authentication is outside the scope of this assignment; `teacherId` and `parentId` are supplied as UUIDs.
- Capacity limits are outside the scope; many parents may book the same offering.
- Parents book an entire offering, never individual sessions.
- An offering becomes published after its first session is added.
- The conflict rule is per parent, across every session in their confirmed bookings.

## Useful Test Flow

1. Create an offering in Course Service using `http://localhost:8080/course/api/v1/teacher/offerings`
2. Add multiple sessions to that offering, copy paste the offering id from above response.
3. View the offerings from parent api with parent timezone - POST `http://localhost:8080/booking/api/v1/parent/offerings`
3. Call Booking Service `http://localhost:8080/booking/api/v1/parent/bookings` with desired offering id,
parent id is just UUID field, random Id for parent Id works.(Further, can introduce a user with role based access for parents/teachers, then use get from parent/teacher id through client)
4. Book the offering.
5. Try booking another offering with an overlapping session for the same parent.
6. The second booking should return `409 BOOKING_CONFLICT`.
7. Concurrent booking tried and tested using powershell.
