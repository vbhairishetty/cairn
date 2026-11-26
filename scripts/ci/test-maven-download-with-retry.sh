#!/usr/bin/env bash
# Local stub test for maven-download-with-retry.sh (T-011, TM-15). Not run in CI.
# Everything happens in temporary directories: MAVEN_USER_HOME, CAIRN_MAVEN_DIST_DIR and HOME
# point there, curl and wget are stubs that only log, and the Maven zip is synthetic with its
# own SHA-256 in a temp copy of the properties. No network, no real ~/.m2, no real ~/.cache.
# Usage: scripts/ci/test-maven-download-with-retry.sh   (exits non-zero if any check fails)
set -uo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="${repo_root}/scripts/ci/maven-download-with-retry.sh"
good_url="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip"
fails=0
pass=0
top="$(mktemp -d)"
trap 'rm -rf -- "$top"' EXIT

check() {
  if eval "$2"; then
    pass=$((pass + 1))
    echo "  ok   $1"
  else
    fails=$((fails + 1))
    echo "  FAIL $1"
  fi
}

sha_of() {
  if command -v sha256sum > /dev/null; then sha256sum -- "$1" | cut -d' ' -f1; else shasum -a 256 -- "$1" | cut -d' ' -f1; fi
}

make_zip() { # make_zip <src-dir-containing apache-maven-3.9.16> <out.zip>
  if command -v zip > /dev/null; then
    (cd "$1" && zip -qr "$2" apache-maven-3.9.16)
  else
    python3 -I - "$1" "$2" << 'PY'
import sys, zipfile
src, out = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(out, "w") as z:
    for name in ("apache-maven-3.9.16/", "apache-maven-3.9.16/bin/"):
        z.writestr(zipfile.ZipInfo(name), "")
    info = zipfile.ZipInfo("apache-maven-3.9.16/bin/mvn")
    info.external_attr = 0o755 << 16
    z.writestr(info, open(src + "/apache-maven-3.9.16/bin/mvn").read())
PY
  fi
}

new_case() {
  root="$(mktemp -d "${top}/case.XXXXXX")"
  proj="${root}/proj"
  stubs="${root}/stubs"
  mhome="${root}/m2"
  dist="${root}/dist"
  calls="${root}/calls"
  mkdir -p "${proj}/.mvn/wrapper" "$stubs" "$mhome" "${root}/z/apache-maven-3.9.16/bin" "${root}/home"
  : > "$calls"
  cp "${repo_root}/mvnw" "${proj}/mvnw"
  printf '#!/bin/sh\necho "FAKE MAVEN 3.9.16 $*"\n' > "${root}/z/apache-maven-3.9.16/bin/mvn"
  chmod +x "${root}/z/apache-maven-3.9.16/bin/mvn"
  make_zip "${root}/z" "${root}/good.zip"
  sha="$(sha_of "${root}/good.zip")"
  write_props "$good_url"
  printf '#!/bin/sh\necho "wget $*" >> "%s"\nexit 1\n' "$calls" > "${stubs}/wget"
  cat > "${stubs}/curl" << STUB
#!/bin/sh
echo "curl \$*" >> "$calls"
n=\$(grep -c '^curl ' "$calls")
out=""; while [ \$# -gt 0 ]; do case "\$1" in -o|--output) out="\$2"; shift;; esac; shift; done
[ "\$n" -gt "\${CURL_FAILS:-0}" ] || exit 22
cp "\${CURL_SERVE}" "\$out"
STUB
  chmod +x "${stubs}/wget" "${stubs}/curl"
  cache="${dist}/apache-maven-3.9.16-bin.zip"
}

write_props() {
  printf 'wrapperVersion=3.3.4\ndistributionType=only-script\ndistributionUrl=%s\ndistributionSha256Sum=%s\n' "$1" "$sha" > "${proj}/.mvn/wrapper/maven-wrapper.properties"
}

run_with() { # run_with <wrapper> <unit>
  out="$(CAIRN_MAVEN_DIST_DIR="$dist" MAVEN_USER_HOME="$mhome" HOME="${root}/home" PATH="${stubs}:${PATH}" "$script" "$1" "$2" 2>&1)"
  rc=$?
}
run() { run_with "${proj}/mvnw" 0; }
no_calls='[ ! -s "$calls" ]'
curl_count() { grep -c '^curl ' "$calls" || true; }

echo "cached zip with the right SHA: no network, success line, Maven runs"
new_case; mkdir -p "$dist"; cp "${root}/good.zip" "$cache"
run
check "exit 0" '[ $rc -eq 0 ]'
check "verified line" 'grep -qF "Using cached Maven distribution (SHA-256 verified)" <<< "$out"'
check "maven ran" 'grep -qF "FAKE MAVEN 3.9.16" <<< "$out"'
check "no wget or curl call" "$no_calls"

echo "cached zip modified: fails once, no download"
new_case; mkdir -p "$dist"; cp "${root}/good.zip" "$cache"; printf x >> "$cache"
run
check "exit 1" '[ $rc -eq 1 ]'
check "mismatch line" 'grep -qF "FAIL: cached Maven distribution checksum mismatch" <<< "$out"'
check "no wget or curl call" "$no_calls"
check "no retry text" '! grep -qi "retrying" <<< "$out"'
check "maven did not run" '! grep -qF "FAKE MAVEN" <<< "$out"'

echo "cached zip is a symlink to bad bytes: fails once, no download"
new_case; mkdir -p "$dist"; printf 'bad bytes' > "${root}/bad.zip"; ln -s "${root}/bad.zip" "$cache"
run
check "exit 1" '[ $rc -eq 1 ]'
check "mismatch line" 'grep -qF "FAIL: cached Maven distribution checksum mismatch" <<< "$out"'
check "no wget or curl call" "$no_calls"

echo "cache miss, 4 failures then the real zip: ok on attempt 5, zip stored"
new_case; export CURL_FAILS=4 CURL_SERVE="${root}/good.zip"
run; unset CURL_FAILS CURL_SERVE
check "exit 0" '[ $rc -eq 0 ]'
check "attempt 5 line" 'grep -qF "Maven download ok on attempt 5" <<< "$out"'
check "5 curl calls" '[ "$(curl_count)" -eq 5 ]'
check "zip stored with the right SHA" '[ -f "$cache" ] && [ "$(sha_of "$cache")" = "$sha" ]'
check "maven ran" 'grep -qF "FAKE MAVEN 3.9.16" <<< "$out"'
check "nothing written under m2 repository" '[ ! -e "${mhome}/repository" ]'
check "no wget call" '! grep -q "^wget " "$calls"'

echo "cache miss, download serves wrong bytes: fails once, nothing stored"
new_case; printf 'not maven' > "${root}/bad.zip"; export CURL_FAILS=0 CURL_SERVE="${root}/bad.zip"
run; unset CURL_FAILS CURL_SERVE
check "exit 1" '[ $rc -eq 1 ]'
check "mismatch line" 'grep -qF "FAIL: downloaded Maven distribution checksum mismatch" <<< "$out"'
check "1 curl call" '[ "$(curl_count)" -eq 1 ]'
check "nothing stored" '[ ! -e "$cache" ]'

echo "cache miss, 5 failures: FAIL after 5 attempts"
new_case; export CURL_FAILS=99 CURL_SERVE="${root}/good.zip"
run; unset CURL_FAILS CURL_SERVE
check "exit 1" '[ $rc -eq 1 ]'
check "5 curl calls" '[ "$(curl_count)" -eq 5 ]'
check "final line" 'grep -qF "FAIL: Maven download failed 5 times" <<< "$out"'

echo "planted extracted distribution is deleted and never run"
new_case; mkdir -p "$dist"; cp "${root}/good.zip" "$cache"
run
d="$(find "${mhome}/wrapper/dists" -mindepth 2 -maxdepth 2 -type d | head -1)"
check "installed distribution found" '[ -n "$d" ]'
printf '#!/bin/sh\necho UNVERIFIED-RAN\n' > "${d}/bin/mvn"; touch "${d}/planted"
run
check "exit 0" '[ $rc -eq 0 ]'
check "planted marker never ran" '! grep -qF "UNVERIFIED-RAN" <<< "$out"'
check "planted file gone" '[ ! -e "${d}/planted" ]'
check "verified maven ran" 'grep -qF "FAKE MAVEN 3.9.16" <<< "$out"'

echo "default dist dir is HOME/.cache/cairn/maven-dist"
new_case; export CURL_FAILS=0 CURL_SERVE="${root}/good.zip"
out="$(env -u CAIRN_MAVEN_DIST_DIR HOME="${root}/home" MAVEN_USER_HOME="$mhome" PATH="${stubs}:${PATH}" "$script" "${proj}/mvnw" 0 2>&1)"; rc=$?
unset CURL_FAILS CURL_SERVE
check "exit 0" '[ $rc -eq 0 ]'
check "zip in default dir" '[ -f "${root}/home/.cache/cairn/maven-dist/apache-maven-3.9.16-bin.zip" ]'

echo "distributionUrl with odd characters is rejected before any curl call"
for bad in \
  "https://repo.maven.apache.org/maven2/org/%2e%2e/x-bin.zip" \
  "https://repo.maven.apache.org/maven2/org/apache/x-bin.zip?a=b" \
  "https://repo.maven.apache.org/maven2/org/apache/x-bin.zip#frag" \
  "https://repo.maven.apache.org/maven2/org/a@evil/x-bin.zip" \
  "https://repo.maven.apache.org/maven2/org/../x-bin.zip" \
  "http://repo.maven.apache.org/maven2/org/apache/x-bin.zip"; do
  new_case; write_props "$bad"; export CURL_FAILS=0 CURL_SERVE="${root}/good.zip"
  run; unset CURL_FAILS CURL_SERVE
  check "rejected: ${bad##*maven2/}" '[ $rc -eq 1 ] && grep -q "^FAIL: distributionUrl" <<< "$out"'
  check "  no curl call" "$no_calls"
done

echo "shim shape mismatch fails closed with a clear message"
new_case
printf '#!/bin/sh\nwget --quiet "https://example.invalid/x-bin.zip"\n' > "${proj}/fake-mvnw"; chmod +x "${proj}/fake-mvnw"
mkdir -p "$dist"; cp "${root}/good.zip" "$cache"
run_with "${proj}/fake-mvnw" 0
check "non-zero exit" '[ $rc -ne 0 ]'
check "shim message" 'grep -qF "FAIL: wget shim found no -O target" <<< "$out"'

echo "bad unit argument"
new_case
out="$(MAVEN_USER_HOME="$mhome" CAIRN_MAVEN_DIST_DIR="$dist" PATH="${stubs}:${PATH}" "$script" "${proj}/mvnw" x 2>&1)"; rc=$?
check "exit 2" '[ $rc -eq 2 ]'

echo "passed=${pass} failed=${fails}"
[ "$fails" -eq 0 ]
