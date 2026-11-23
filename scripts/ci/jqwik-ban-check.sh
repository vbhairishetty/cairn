#!/usr/bin/env bash
# Behavioral check for the jqwik ban (ADR 0006, TM-14).
#   (a) A copy of HEAD with a direct jqwik test dependency must fail the build
#       with the ban message, not just any failure.
#   (b) The real checkout must pass ./mvnw -B test, and its output must not
#       mention jqwik except inside the class name JqwikBanSecurityTest.
# Run from anywhere inside the repository. Extra Maven settings, if any, come
# from the MAVEN_ARGS environment variable (used only for local runs).
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

ban_message="net.jqwik is banned"

echo "== (a) copy of HEAD with a direct net.jqwik:jqwik dependency must fail =="
copy_dir="$work_dir/copy"
mkdir "$copy_dir"
git archive HEAD | tar -x -C "$copy_dir"

# Insert the dependency before the last closing dependencies tag, which
# closes the project-level dependencies block.
last_line="$(grep -n '</dependencies>' "$copy_dir/pom.xml" | tail -n 1 | cut -d: -f1)"
if [ -z "$last_line" ]; then
  echo "FAIL: no </dependencies> found in pom.xml" >&2
  exit 1
fi
awk -v n="$last_line" '
  NR == n {
    print "    <dependency>"
    print "      <groupId>net.jqwik</groupId>"
    print "      <artifactId>jqwik</artifactId>"
    print "      <version>1.10.2</version>"
    print "      <scope>test</scope>"
    print "    </dependency>"
  }
  { print }
' "$copy_dir/pom.xml" > "$work_dir/pom.xml.new"
mv "$work_dir/pom.xml.new" "$copy_dir/pom.xml"

if ! grep -q '<groupId>net.jqwik</groupId>' "$copy_dir/pom.xml"; then
  echo "FAIL: the test dependency was not added to the copy" >&2
  exit 1
fi

a_status=0
(cd "$copy_dir" && env MAVEN_OPTS= ./mvnw -B validate) > "$work_dir/a.log" 2>&1 || a_status=$?

if [ "$a_status" -eq 0 ]; then
  echo "FAIL (a): build passed with a direct jqwik dependency" >&2
  tail -n 40 "$work_dir/a.log" >&2
  exit 1
fi
if ! grep -q "$ban_message" "$work_dir/a.log"; then
  echo "FAIL (a): build failed (exit $a_status) but not with the ban message" >&2
  tail -n 40 "$work_dir/a.log" >&2
  exit 1
fi
echo "OK (a): build failed with exit $a_status and the ban message"

echo "== (b) real checkout: ./mvnw -B test must pass without jqwik output =="
b_status=0
env MAVEN_OPTS= ./mvnw -B test > "$work_dir/b.log" 2>&1 || b_status=$?

if [ "$b_status" -ne 0 ]; then
  echo "FAIL (b): ./mvnw -B test exited with $b_status" >&2
  tail -n 60 "$work_dir/b.log" >&2
  exit 1
fi

# Remove the allowed class name, then look for any other mention.
sed 's/JqwikBanSecurityTest//g' "$work_dir/b.log" > "$work_dir/b.filtered.log"
if grep -n -i 'jqwik' "$work_dir/b.filtered.log"; then
  echo "FAIL (b): test output mentions jqwik outside JqwikBanSecurityTest" >&2
  exit 1
fi
echo "OK (b): tests passed and no jqwik text in the output"
