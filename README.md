# Hibernate Gym CRM

Spring Core + Hibernate implementation of the Gym CRM task. The project keeps the previous module's service/facade style, but stores data through JPA entities and an H2 database.

## Implemented Requirements

- Trainer and trainee profile creation with generated usernames and passwords.
- Trainer/trainee authentication by username and password.
- Profile lookup, password change, update, activation and deactivation.
- Hard trainee deletion with cascade removal of related trainings.
- Trainee and trainer training-list filtering.
- Training creation with trainee, trainer, and training type foreign keys.
- Trainer assignment queries and trainee trainer-list replacement.
- Transaction management for create, update, delete, password, status, training, and assignment operations.
- Unit/integration tests with an 80% JaCoCo line coverage gate.

## Run

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd exec:java "-Dexec.args=--demo"
```

The default database is H2 in-memory:

```text
jdbc:h2:mem:gymcrm;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
```

You can override it with JVM properties such as `-Dgym.db.url=...`, `-Dgym.db.username=...`, and `-Dgym.db.password=...`.
