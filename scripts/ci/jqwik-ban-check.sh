#!/usr/bin/env bash
# Behavioral check for the jqwik ban (ADR 0006, TM-14).
#   (a) A checkout of HEAD with a direct jqwik test dependency must fail the
#       build with the Enforcer ban failure, not just any failure. The copy
#       gets its own local Maven repository so jqwik never enters the shared one.
#   (b) The real checkout must pass ./mvnw -B test. The local Maven repository
#       must hold no net/jqwik before or after, the named guard tests must have
#       run, and nothing under target/ may mention jqwik except inside the class
#       name JqwikBanSecurityTest. These checks do not depend on how the pom is
#       written: any route that puts jqwik on a classpath trips them.
# The working tree must be clean: (a) builds HEAD and (b) builds the tree, so
# they must be the same content. Locally, commit first.
# Run from anywhere inside the repository. For local runs, MAVEN_ARGS may set
# -Dmaven.repo.local (the repository (b) uses; it must not contain net/jqwik) and
# -Dmaven.repo.local.tail. MAVEN_LOCAL_REPO names the shared repository that (a)
# reads from (default $HOME/.m2/repository). CI must set none of them.
# If the script is killed with SIGKILL the trap cannot run. Clean up with:
#   git worktree prune
# (and delete the leftover temporary directory by hand).
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

echo "MAVEN_ARGS=[${MAVEN_ARGS:-}]"
if [ "${CI:-}" = "true" ] && [ -n "${MAVEN_ARGS:-}" ]; then
  echo "FAIL: MAVEN_ARGS must be empty when CI=true" >&2
  exit 1
fi

if [ -n "$(git status --porcelain --ignored=no)" ]; then
  echo "FAIL: working tree is not clean; (a) builds HEAD and (b) builds the tree, so commit or stash first" >&2
  git status --short >&2
  exit 1
fi

shared_repo="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"

# The local repository (b) uses: -Dmaven.repo.local from MAVEN_ARGS if given,
# else the default.
used_repo="$HOME/.m2/repository"
read -r -a maven_arg_words <<< "${MAVEN_ARGS:-}"
for word in "${maven_arg_words[@]+"${maven_arg_words[@]}"}"; do
  case "$word" in
    -Dmaven.repo.local=*) used_repo="${word#-Dmaven.repo.local=}" ;;
  esac
done

work_dir="$(mktemp -d)"
copy_dir="$work_dir/copy"
cleanup() {
  git -C "$repo_root" worktree remove --force "$copy_dir" > /dev/null 2>&1 || true
  rm -rf "$work_dir"
  git -C "$repo_root" worktree prune > /dev/null 2>&1 || true
}
trap cleanup EXIT

ban_message="net.jqwik is banned"

echo "== (a) HEAD with a direct net.jqwik:jqwik dependency must fail =="
# A worktree applies .gitattributes like a normal checkout (git archive would
# drop export-ignore files, so the copy could differ from the real build).
git worktree add --detach "$copy_dir" HEAD > /dev/null 2>&1

# Insert the dependency before the last closing dependencies tag, which
# closes the project-level dependencies block.
last_line="$(grep -n '</dependencies>' "$copy_dir/pom.xml" | tail -n 1 | cut -d: -f1 || true)"
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
# The copy has its own local repository; the shared one is read only (tail).
(cd "$copy_dir" && env MAVEN_OPTS= ./mvnw -B \
  -Dmaven.repo.local="$work_dir/m2-a" \
  -Dmaven.repo.local.tail="$shared_repo" \
  validate) > "$work_dir/a.log" 2>&1 || a_status=$?

if [ "$a_status" -eq 0 ]; then
  echo "FAIL (a): build passed with a direct jqwik dependency" >&2
  tail -n 40 "$work_dir/a.log" >&2
  exit 1
fi
# Require the Enforcer's own failure lines. A bare message match could also
# come from a configuration dump in a build that failed for another reason.
for expected in \
  "BannedDependencies failed with message" \
  "$ban_message" \
  "net.jqwik:jqwik:jar:1.10.2 <--- banned"; do
  if ! grep -q -F -- "$expected" "$work_dir/a.log"; then
    echo "FAIL (a): build failed (exit $a_status) but the log lacks: $expected" >&2
    tail -n 40 "$work_dir/a.log" >&2
    exit 1
  fi
done
echo "OK (a): build failed with exit $a_status and the Enforcer ban failure"

echo "== (b) real checkout: ./mvnw -B test must pass without jqwik output =="
# Maven puts every artifact it resolves in the local repository. jqwik must
# never be resolved, so the directory must be absent before and after.
echo "checking local repository: $used_repo"
if [ -e "$used_repo/net/jqwik" ]; then
  echo "FAIL (b): $used_repo/net/jqwik exists before the run" >&2
  exit 1
fi
if [ -e .jqwik-database ]; then
  echo "FAIL (b): .jqwik-database exists before the run" >&2
  exit 1
fi

# Remove old build output so only this run's files are checked.
rm -rf target

b_status=0
env MAVEN_OPTS= ./mvnw -B test > "$work_dir/b.log" 2>&1 || b_status=$?

if [ "$b_status" -ne 0 ]; then
  echo "FAIL (b): ./mvnw -B test exited with $b_status" >&2
  tail -n 60 "$work_dir/b.log" >&2
  exit 1
fi

if [ -e "$used_repo/net/jqwik" ]; then
  echo "FAIL (b): $used_repo/net/jqwik exists after the run; jqwik was resolved" >&2
  exit 1
fi
if [ -e .jqwik-database ]; then
  echo "FAIL (b): .jqwik-database exists after the run; jqwik ran" >&2
  exit 1
fi

# Console output, then every file under target/ (reports, redirected test
# output, extra report directories). The allowed class name is removed first.
# Compiled .class files are skipped: the guard's own classes name the library
# on purpose. Other files are scanned with NUL bytes turned into line breaks.
scan_for_jqwik() {
  LC_ALL=C tr '\000' '\n' | LC_ALL=C sed 's/JqwikBanSecurityTest//g' \
    | LC_ALL=C grep -a -i -n 'jqwik' | cut -c1-200 || true
}

console_hits="$(scan_for_jqwik < "$work_dir/b.log")"
if [ -n "$console_hits" ]; then
  echo "FAIL (b): console output mentions jqwik outside JqwikBanSecurityTest:" >&2
  echo "$console_hits" >&2
  exit 1
fi

if [ ! -d target ]; then
  echo "FAIL (b): no target directory after the run" >&2
  exit 1
fi
while IFS= read -r -d '' file; do
  case "$file" in
    *.class) continue ;;
  esac
  file_hits="$(scan_for_jqwik < "$file")"
  if [ -n "$file_hits" ]; then
    echo "FAIL (b): $file mentions jqwik outside JqwikBanSecurityTest:" >&2
    echo "$file_hits" >&2
    exit 1
  fi
done < <(find target -type f -print0)

# The guard tests must have run, with at least one test and no failures,
# errors or skips (a build change could otherwise hide them).
require_suite() {
  local file="target/surefire-reports/TEST-$1.xml"
  if [ ! -f "$file" ]; then
    echo "FAIL (b): missing test report $file; the suite did not run" >&2
    exit 1
  fi
  local tag tests errors failures skipped
  tag="$(tr '\n' ' ' < "$file" | grep -o '<testsuite [^>]*>' | head -n 1 || true)"
  tests="$(printf '%s' "$tag" | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' || true)"
  errors="$(printf '%s' "$tag" | grep -o ' errors="[0-9]*"' | grep -o '[0-9]*' || true)"
  failures="$(printf '%s' "$tag" | grep -o ' failures="[0-9]*"' | grep -o '[0-9]*' || true)"
  skipped="$(printf '%s' "$tag" | grep -o ' skipped="[0-9]*"' | grep -o '[0-9]*' || true)"
  if [ -z "$tests" ] || [ -z "$errors" ] || [ -z "$failures" ] || [ -z "$skipped" ]; then
    echo "FAIL (b): could not read the counts in $file" >&2
    exit 1
  fi
  if [ "$tests" -le 0 ] || [ "$errors" -ne 0 ] || [ "$failures" -ne 0 ] || [ "$skipped" -ne 0 ]; then
    echo "FAIL (b): $1 tests=$tests errors=$errors failures=$failures skipped=$skipped" >&2
    exit 1
  fi
  echo "  $1: tests=$tests errors=$errors failures=$failures skipped=$skipped"
}

# One named test case: present exactly once, with no failure, error or skip.
require_test_case() {
  local class="$1" method="$2"
  local file="target/surefire-reports/TEST-$class.xml"
  local chunk count
  if [ ! -f "$file" ]; then
    echo "FAIL (b): missing test report $file" >&2
    exit 1
  fi
  chunk="$(tr '\n' ' ' < "$file" | sed 's#<testcase #\n<testcase #g' \
    | grep -F "<testcase name=\"$method\" classname=\"$class\"" || true)"
  count="$(printf '%s' "$chunk" | grep -c . || true)"
  if [ "$count" -ne 1 ]; then
    echo "FAIL (b): expected exactly one test case $class#$method, found $count" >&2
    exit 1
  fi
  if printf '%s' "$chunk" | grep -q -E '<(failure|error|skipped|flakyFailure|rerunFailure|flakyError|rerunError)'; then
    echo "FAIL (b): test case $class#$method did not pass" >&2
    exit 1
  fi
  echo "  $class#$method: passed"
}

require_suite app.cairn.JqwikBanSecurityTest
require_suite app.cairn.BuildConfigurationTest
require_test_case app.cairn.JqwikBanSecurityTest unchangedLiveBuildFilesAreNotReported
require_test_case app.cairn.JqwikBanSecurityTest blankMavenConfigIsNotReported
require_test_case app.cairn.BuildConfigurationTest buildGuardFindsNoViolationsInTheRealBuildFiles
require_test_case app.cairn.BuildConfigurationTest errorProneTreatsReferenceAndBigDecimalEqualityAsErrors

echo "OK (b): tests passed, named guard tests ran, no net/jqwik in $used_repo, no jqwik text under target/"
