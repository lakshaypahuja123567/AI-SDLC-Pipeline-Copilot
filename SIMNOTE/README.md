# SimpleNote

A small notes app built with Spring Boot 3.4.1 (Java 21), an H2 file database, and static HTML/CSS/JavaScript pages.

## Prerequisites

- JDK 21 (or a verified compatible JDK), with `JAVA_HOME` set to its install directory
- Maven 3.10.0 or later on `PATH` (check with `mvn -v`)
- Node.js 22 and Google Chrome, only for the DOM check
- `SIMPLENOTE_REMEMBER_ME_KEY` environment variable, at least 32 characters

Set the key for the current PowerShell session:

```powershell
$env:SIMPLENOTE_REMEMBER_ME_KEY = "<at least 32 characters>"
```

Without this variable the app fails at startup. Do not commit the key value.

## Run

From the `SIMNOTE` folder:

```powershell
mvn spring-boot:run
```

Open http://localhost:8080. The H2 database is stored in `data/` (git-ignored). Only one instance can use the database at a time; a second process fails with a lock error.

## Test

```powershell
mvn -B test
```

## DOM check

Start the app first, then from the `SIMNOTE` folder run:

```powershell
node tests/dom/dom-check.mjs
```

The script drives Chrome against `http://localhost:8080` (override with `SIMPLENOTE_URL`). It exits with 0 when all checks pass. Chrome is found at its default install path, or set `CHROME_PATH`.

## Security notes

- The session cookie and the XSRF cookie are not marked `Secure`, so they work over plain `http://localhost`. Enable the `Secure` flag before any HTTPS deployment.
- The `simplenote.remember-me.key` default in `application.properties` is a development placeholder. Always set `SIMPLENOTE_REMEMBER_ME_KEY` outside local development.
