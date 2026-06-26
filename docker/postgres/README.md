# Local PostgreSQL with pgvector

This compose file starts a local PostgreSQL database with the pgvector extension enabled for the `egg` database.

## Start

```powershell
docker compose -f compose.postgres.yml up -d
```

## Check Status

```powershell
docker compose -f compose.postgres.yml ps
```

## Connect With psql

```powershell
docker exec -it egg-postgres psql -U egg -d egg
```

Then verify pgvector:

```sql
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

## Stop

```powershell
docker compose -f compose.postgres.yml down
```

## Reset Local Data

This deletes the local database volume. Use it only when you want a clean local database.

```powershell
docker compose -f compose.postgres.yml down -v
```

## Local Connection

```text
Host: localhost
Port: 5432
Database: egg
Username: egg
Password: egg_dev_password
JDBC URL: jdbc:postgresql://localhost:5432/egg
```

The backend still needs its MySQL schema and MyBatis SQL migrated before switching the application datasource to PostgreSQL.
