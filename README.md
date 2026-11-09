# FuelTrack

FuelTrack helps drivers record odometer readings and track vehicle mileage. Add readings by taking or uploading an odometer photo for an AI suggestion, or enter mileage manually. Both paths use the same editable confirmation and validation flow; a photo is optional.

## Phase 2 features

- Manage personal vehicles and record start-of-day or end-of-day readings.
- Upload JPEG, PNG or WebP odometer photos (maximum 10 MB); image contents are verified, resized to at most 1600 pixels and stored outside the web root under generated filenames.
- Anthropic AI suggestions are editable and are not saved until confirmed. When AI is unavailable, the photo is retained and the user is directed to manual entry.
- Manually enter positive whole-kilometre readings with a Gaborone-local date and time and optional note.
- Warn on lower readings or jumps greater than 1,000 km; users must explicitly confirm an override.
- View personal reading history with source badges and optional photo thumbnails.

## Run

Use Java 21 and the Maven Wrapper:

```shell
./mvnw spring-boot:run
```

On Windows, run `mvnw.cmd spring-boot:run`. The default `dev` profile uses a local H2 database. Production uses the `prod` profile and PostgreSQL:

```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` for production. The first administrator can be seeded using `ADMIN_EMAIL` and `ADMIN_PASSWORD`. AI photo reading uses `ANTHROPIC_API_KEY`; `APP_AI_MODEL` overrides the default `claude-sonnet-5-5`. `UPLOAD_DIR` changes the default `./uploads` photo directory.

Run tests with `./mvnw test` (or `mvnw.cmd test` on Windows). Tests use H2 and mock the Anthropic endpoint.

## Reading entry flow

See [the sequence diagram](docs/sequence-reading.md) for both the photo/AI and manual paths, including AI-failure fallback.
