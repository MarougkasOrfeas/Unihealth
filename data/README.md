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

## Medical terms

The bilingual dictionary the backend uses to turn free-text health answers into label codes is a
plain committed data file, with no generation step:

```text
backend/src/main/resources/data/labels/medical-terms.json
```

Edit it and restart the backend. The loader compares an MD5 of the file against
`t_reference_data_version` and re-seeds when it differs, so no migration or manual truncate is
needed. Malformed entries fail the boot with the offending concept named; two concepts whose terms
collapse to the same lookup key are reported as an error in the log.
