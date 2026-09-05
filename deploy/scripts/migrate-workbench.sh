#!/usr/bin/env bash
# Run from the deployment directory after MySQL is healthy and before updating the backend.
set -euo pipefail
umask 077

compose=(docker compose --env-file .env -f compose.prod.yml)
for migration in sql/add_article_revisions.sql sql/add_brand_profile.sql; do
  test -s "$migration"
done

# The versioned migration SQL targets ArticlePilot explicitly.
database_name="$("${compose[@]}" exec -T mysql sh -c 'printf "%s" "$MYSQL_DATABASE"')"
if [[ "$database_name" != "ArticlePilot" ]]; then
  echo 'Migration stopped: these SQL scripts require the ArticlePilot database.' >&2
  exit 1
fi

mkdir -p backups
backup="backups/before-workbench-$(date +%Y%m%d-%H%M%S)-$$.sql"
"${compose[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces ArticlePilot' > "$backup"
test -s "$backup"

# CREATE TABLE IF NOT EXISTS permits deployment after an earlier manual migration.
for migration in sql/add_article_revisions.sql sql/add_brand_profile.sql; do
  "${compose[@]}" exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4' < "$migration"
done

# Validate the columns needed by the application, including pre-existing tables.
"${compose[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot ArticlePilot' <<'SQL'
SELECT id, taskId, userId, revisionNumber, content, fullContent, revisionNote, createTime
FROM article_revision LIMIT 0;
SELECT id, userId, brandName, tone, targetAudience, preferredTerms, bannedTerms, referenceNotes, createTime, updateTime
FROM brand_profile LIMIT 0;
SQL
printf 'Workbench migration completed. Backup: %s\n' "$backup"
