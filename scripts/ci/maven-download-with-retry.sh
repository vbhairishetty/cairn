#!/usr/bin/env bash
# Downloads Maven through the wrapper, retrying when the download fails (T-010).
# setup-java with cache: maven also restores ~/.m2/wrapper/dists, and the wrapper runs a
# distribution that is already there without checking its SHA-256 again. So this script
# first deletes that folder, which forces a fresh download that the wrapper verifies.
# A checksum mismatch is never retried. Other failures are retried up to 3 times.
# Usage: maven-download-with-retry.sh [wrapper-path] [delay-seconds]
# The delay argument exists only so the local stub test can run fast.
# For local runs, set MAVEN_USER_HOME to a temporary directory first; otherwise this deletes
# your own ~/.m2/wrapper/dists (a re-download, not data loss).
set -euo pipefail

wrapper="${1:-./mvnw}"
delay="${2:-15}"
max_attempts=3
mismatch_text="Failed to validate Maven distribution SHA-256"

case "$delay" in
  '' | *[!0-9]*)
    echo "FAIL: delay must be a non-negative integer" >&2
    exit 2
    ;;
esac

maven_home="${MAVEN_USER_HOME:-${HOME:?HOME is not set}/.m2}"
rm -rf -- "${maven_home}/wrapper/dists"
echo "Deleted cached Maven distributions under ${maven_home}/wrapper/dists"

log_file="$(mktemp)"
trap 'rm -f -- "$log_file"' EXIT

for attempt in $(seq 1 "$max_attempts"); do
  if "$wrapper" -B -v 2>&1 | tee "$log_file"; then
    echo "Maven download ok on attempt ${attempt}"
    exit 0
  fi
  if grep -qF -- "$mismatch_text" "$log_file"; then
    echo "FAIL: Maven distribution checksum mismatch; not retrying"
    exit 1
  fi
  if [ "$attempt" -lt "$max_attempts" ]; then
    echo "Maven download attempt ${attempt} failed; retrying in ${delay}s"
    sleep "$delay"
  fi
done

echo "FAIL: Maven download failed ${max_attempts} times"
exit 1
