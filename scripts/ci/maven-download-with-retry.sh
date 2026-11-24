#!/usr/bin/env bash
# Downloads Maven through the wrapper, retrying when the download fails (T-010).
# The wrapper verifies the pinned SHA-256 on every download; nothing is cached here.
# Usage: maven-download-with-retry.sh [wrapper-path] [delay-seconds]
# The delay argument exists only so the local stub test can run fast.
set -euo pipefail

wrapper="${1:-./mvnw}"
delay="${2:-15}"
max_attempts=3

for attempt in $(seq 1 "$max_attempts"); do
  if "$wrapper" -B -v; then
    echo "Maven download ok on attempt ${attempt}"
    exit 0
  fi
  if [ "$attempt" -lt "$max_attempts" ]; then
    echo "Maven download attempt ${attempt} failed; retrying in ${delay}s"
    sleep "$delay"
  fi
done

echo "FAIL: Maven download failed ${max_attempts} times"
exit 1
