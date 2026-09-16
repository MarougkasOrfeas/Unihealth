# UniHealth Data

This directory contains data preparation tooling and generated/reference datasets that are not part
of the normal frontend or backend runtime.

## NHS ingest

`nhs-ingest` is a standalone Java utility that refreshes the committed NHS symptoms snapshot used by
the backend bootstrap process.

Run it from the repository root:

```powershell
mvn -f data/nhs-ingest/pom.xml exec:java
```

Useful overrides:

```powershell
mvn -f data/nhs-ingest/pom.xml "-Dnhs.ingest.max-pages=10" exec:java
mvn -f data/nhs-ingest/pom.xml "-Dnhs.ingest.output=../../backend/src/main/resources/data/nhs/nhs-symptoms.json" exec:java
```

The backend reads the generated snapshot from:

```text
backend/src/main/resources/data/nhs/nhs-symptoms.json
```

Commit the regenerated snapshot after a deliberate refresh.
