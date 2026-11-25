#!/usr/bin/env bash
# Completes release-please's release pull request with what release-please cannot write: the versionCode, one more
# than main's, and the store changelog F-Droid shows under each processor's version code, made from the section
# release-please added to CHANGELOG.md. Run from the root of a checkout of the release branch, with origin/main
# fetched. It commits only when something changed, so running it again on a finished pull request does nothing.
#
# Usage: prepare-release-pr.sh <branch to push to>
set -euo pipefail

branch=${1:?usage: prepare-release-pr.sh <branch>}
gradle_file=app/build.gradle.kts
changelogs=fastlane/metadata/android/en-US/changelogs
# The most F-Droid shows of a version's changelog; :app:checkRelease refuses a longer one.
limit=500

version_code() { sed -n 's/^ *versionCode = \([0-9][0-9]*\)$/\1/p'; }

main_code=$(git show "origin/main:$gradle_file" | version_code)
branch_code=$(version_code < "$gradle_file")
code=$((main_code + 1))
version=$(sed -n 's/^ *versionName = "\([^"]*\)".*$/\1/p' "$gradle_file")
if [[ -z $main_code || -z $branch_code || -z $version ]]; then
  echo "Could not read versionCode and versionName from $gradle_file." >&2
  exit 1
fi

# A changelog written for another versionCode by an earlier run, before main moved on, is not this version's.
if [[ $branch_code != "$main_code" && $branch_code != "$code" ]]; then
  rm -f "$changelogs/${branch_code}"{1,2,3}.txt
fi
sed "s/^\( *versionCode = \)[0-9][0-9]*$/\1$code/" "$gradle_file" > "$gradle_file.new"
mv "$gradle_file.new" "$gradle_file"

# The bullets of the newest section, as plain text: no links to commits, pull requests as (#12), no bold scopes,
# each starting with a capital. Whole bullets are left out from the end until the text fits.
text=$(
  awk '/^## /{ if (seen) exit; seen = 1; next } seen && /^\* /' CHANGELOG.md |
    sed -E \
      -e 's/ \(\[[0-9a-f]{7,40}\]\([^)]*\)\)//g' \
      -e 's/\(\[(#[0-9]+)\]\([^)]*\)\)/(\1)/g' \
      -e 's/\[([^]]*)\]\([^)]*\)/\1/g' \
      -e 's/\*\*([^*]*)\*\*/\1/g' |
    awk -v limit="$limit" '
      { line = "* " toupper(substr($0, 3, 1)) substr($0, 4) }
      length(out) + length(line) + (out == "" ? 0 : 1) > limit { exit }
      { out = (out == "" ? line : out "\n" line) }
      END { print out }'
)
if [[ -z $text ]]; then
  text="* Fixes and improvements"
fi
for digit in 1 2 3; do
  printf '%s\n' "$text" > "$changelogs/${code}${digit}.txt"
done

git add "$gradle_file" "$changelogs"
if git diff --cached --quiet; then
  echo "The release pull request for $version already has versionCode $code and its changelog."
  exit 0
fi
git -c user.name="github-actions[bot]" \
  -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
  commit --quiet --message "chore: set versionCode $code and the store changelog for $version"
git push --quiet origin "HEAD:refs/heads/$branch"
echo "Set versionCode $code and the store changelog for $version."
