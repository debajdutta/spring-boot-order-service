#!/usr/bin/env bash
# Start order-service locally: run the tests, then start the app.
# Usage: ./start.sh            (tests + run)
#        ./start.sh --skip-tests
set -euo pipefail

cd "$(dirname "$0")"   # run from the project folder, wherever it is cloned

# DB credentials live in .env.local (git-ignored), not in this script.
# First time: cp .env.example .env.local and fill in your values.
if [[ -f .env.local ]]; then
  set -a; source .env.local; set +a
fi
: "${DB_USERNAME:?DB_USERNAME not set - add it to .env.local}"
: "${DB_PASSWORD:?DB_PASSWORD not set - add it to .env.local}"

if [[ "${1:-}" != "--skip-tests" ]]; then
  ./mvnw test        # stops here if any test fails (set -e)
fi
./mvnw spring-boot:run
