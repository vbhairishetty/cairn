#!/usr/bin/env bash
# Installs Maven for the wrapper from a zip whose SHA-256 is checked on every use (T-010, T-011).
# setup-java with cache: maven restores ~/.m2/wrapper/dists, and the wrapper runs a
# distribution that is already there without checking its SHA-256 again. So this script
# always deletes that folder first and never runs anything that was there.
# The verified zip lives in its own directory, ~/.cache/cairn/maven-dist by default. That
# default must equal the `path:` of the workflow's actions/cache step; CAIRN_MAVEN_DIST_DIR
# exists for local tests. The workflow saves that directory keyed on the wrapper properties.
# It is outside ~/.m2 so the setup-java cache is unaffected. Then:
#   - cached zip present: its SHA-256 must equal distributionSha256Sum from
#     .mvn/wrapper/maven-wrapper.properties. A mismatch fails at once: no retry, no download.
#   - no cached zip: download it (5 attempts, waiting unit, 2*unit, 4*unit, 8*unit seconds, so
#     15, 30, 60, 120 by default), verify the SHA-256 (a mismatch fails at once), and store it.
# Either way the wrapper then installs from a private copy of the verified zip. The wrapper
# asks wget for the zip, so a one-file wget on PATH for that one command hands it the local
# copy; the wrapper checks the SHA-256 again before it unpacks. Its log line "downloading and
# installing" therefore does not mean a network request.
# The pinned properties file is the single source of truth for the URL and the hash.
# Usage: maven-download-with-retry.sh [wrapper-path] [unit-seconds]
# The unit argument exists only so the local stub test can run fast.
# For local runs, set MAVEN_USER_HOME to a temporary directory first; otherwise this deletes
# your own ~/.m2/wrapper/dists (a re-download, not data loss); also set CAIRN_MAVEN_DIST_DIR.
set -euo pipefail

wrapper="${1:-./mvnw}"
unit="${2:-15}"
max_attempts=5
central_prefix="https://repo.maven.apache.org/maven2/"

case "$unit" in
  '' | *[!0-9]*)
    echo "FAIL: unit must be a non-negative integer" >&2
    exit 2
    ;;
esac

trim() { printf '%s' "$1" | tr -d '[:space:]'; }

sha256_of() {
  if command -v sha256sum > /dev/null; then
    sha256sum -- "$1" | cut -d' ' -f1
  elif command -v shasum > /dev/null; then
    shasum -a 256 -- "$1" | cut -d' ' -f1
  else
    echo "FAIL: neither sha256sum nor shasum is available" >&2
    exit 1
  fi
}

properties="$(dirname "$wrapper")/.mvn/wrapper/maven-wrapper.properties"
distribution_url=""
expected_sha=""
while IFS="=" read -r key value; do
  case "${key-}" in
    distributionUrl) distribution_url="$(trim "${value-}")" ;;
    distributionSha256Sum) expected_sha="$(trim "${value-}" | tr 'A-F' 'a-f')" ;;
  esac
done < "$properties"

case "$distribution_url" in
  "${central_prefix}"*-bin.zip) ;;
  *)
    echo "FAIL: distributionUrl must be a ${central_prefix}...-bin.zip URL" >&2
    exit 1
    ;;
esac
case "$distribution_url" in
  *..* | *%* | *\?* | *\#* | *@*)
    echo "FAIL: distributionUrl must not contain '..', '%', '?', '#' or '@'" >&2
    exit 1
    ;;
esac
case "$expected_sha" in
  '' | *[!0-9a-f]*)
    echo "FAIL: distributionSha256Sum is missing or not hexadecimal" >&2
    exit 1
    ;;
esac
if [ "${#expected_sha}" -ne 64 ]; then
  echo "FAIL: distributionSha256Sum must be 64 hexadecimal characters" >&2
  exit 1
fi

maven_home="${MAVEN_USER_HOME:-${HOME:?HOME is not set}/.m2}"
dist_dir="${CAIRN_MAVEN_DIST_DIR:-${HOME:?HOME is not set}/.cache/cairn/maven-dist}"
cached_zip="${dist_dir}/${distribution_url##*/}"

rm -rf -- "${maven_home}/wrapper/dists"
echo "Deleted cached Maven distributions under ${maven_home}/wrapper/dists"

work_dir="$(mktemp -d)"
trap 'rm -rf -- "$work_dir"' EXIT
verified_zip="${work_dir}/maven-distribution.zip"

if [ -f "$cached_zip" ]; then
  cp -- "$cached_zip" "$verified_zip"
  if [ "$(sha256_of "$verified_zip")" != "$expected_sha" ]; then
    echo "FAIL: cached Maven distribution checksum mismatch" >&2
    exit 1
  fi
  echo "Using cached Maven distribution (SHA-256 verified)"
else
  downloaded=false
  for attempt in $(seq 1 "$max_attempts"); do
    rm -f -- "$verified_zip"
    if curl --fail --location --silent --show-error \
      --proto '=https' --proto-redir '=https' \
      --connect-timeout 30 --max-time 300 \
      --output "$verified_zip" "$distribution_url"; then
      if [ "$(sha256_of "$verified_zip")" != "$expected_sha" ]; then
        echo "FAIL: downloaded Maven distribution checksum mismatch; not retrying" >&2
        exit 1
      fi
      echo "Maven download ok on attempt ${attempt}"
      downloaded=true
      break
    fi
    if [ "$attempt" -lt "$max_attempts" ]; then
      wait_seconds=$((10#$unit * (1 << (attempt - 1))))
      echo "Maven download attempt ${attempt} failed; retrying in ${wait_seconds}s"
      sleep "$wait_seconds"
    fi
  done
  if [ "$downloaded" != true ]; then
    echo "FAIL: Maven download failed ${max_attempts} times"
    exit 1
  fi
  mkdir -p -- "$(dirname "$cached_zip")"
  cp -- "$verified_zip" "${cached_zip}.tmp.$$"
  mv -- "${cached_zip}.tmp.$$" "$cached_zip"
fi

shim_dir="${work_dir}/shim"
mkdir -p -- "$shim_dir"
cat > "${shim_dir}/wget" << 'SHIM'
#!/bin/sh
# Serves the verified local zip to the wrapper. Called as: wget [--quiet] URL -O DEST
dest=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    -O) dest="$2"; shift ;;
  esac
  shift
done
[ -n "$dest" ] || { echo "FAIL: wget shim found no -O target" >&2; exit 2; }
cp -- "$CAIRN_VERIFIED_MAVEN_ZIP" "$dest"
SHIM
chmod +x "${shim_dir}/wget"

CAIRN_VERIFIED_MAVEN_ZIP="$verified_zip" PATH="${shim_dir}:${PATH}" "$wrapper" -B -v
