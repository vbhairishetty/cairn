#!/usr/bin/env bash
# Downloads Maven through the wrapper, retrying when the download fails (T-010).
# setup-java with cache: maven also restores ~/.m2/wrapper/dists, and the wrapper runs a
# distribution that is already there without checking its SHA-256 again. So this script
# first deletes that folder, which forces a fresh download that the wrapper verifies.
# A checksum mismatch is never retried. Other failures are retried, up to 5 attempts, waiting
# unit, 2*unit, 4*unit and 8*unit seconds between them (15, 30, 60, 120 by default).
# The wrapper runs with MVNW_VERBOSE=true for that one command only, so wget is not quiet and
# the real download error reaches the log. The wrapper never prints MVNW_USERNAME or
# MVNW_PASSWORD, which are unset in CI. Verbose mode does print the download URL, so
# credentials must never be put in MVNW_REPOURL or distributionUrl.
# Usage: maven-download-with-retry.sh [wrapper-path] [unit-seconds]
# The unit argument exists only so the local stub test can run fast.
# For local runs, set MAVEN_USER_HOME to a temporary directory first; otherwise this deletes
# your own ~/.m2/wrapper/dists (a re-download, not data loss).
set -euo pipefail

wrapper="${1:-./mvnw}"
unit="${2:-15}"
max_attempts=5
mismatch_text="Failed to validate Maven distribution SHA-256"

case "$unit" in
  '' | *[!0-9]*)
    echo "FAIL: unit must be a non-negative integer" >&2
    exit 2
    ;;
esac

maven_home="${MAVEN_USER_HOME:-${HOME:?HOME is not set}/.m2}"
rm -rf -- "${maven_home}/wrapper/dists"
echo "Deleted cached Maven distributions under ${maven_home}/wrapper/dists"

log_file="$(mktemp)"
trap 'rm -f -- "$log_file"' EXIT

for attempt in $(seq 1 "$max_attempts"); do
  if MVNW_VERBOSE=true "$wrapper" -B -v 2>&1 | tee "$log_file"; then
    echo "Maven download ok on attempt ${attempt}"
    exit 0
  fi
  if grep -qF -- "$mismatch_text" "$log_file"; then
    echo "FAIL: Maven distribution checksum mismatch; not retrying"
    exit 1
  fi
  if [ "$attempt" -lt "$max_attempts" ]; then
    wait_seconds=$((unit * (1 << (attempt - 1))))
    echo "Maven download attempt ${attempt} failed; retrying in ${wait_seconds}s"
    sleep "$wait_seconds"
  fi
done

echo "FAIL: Maven download failed ${max_attempts} times"
exit 1
