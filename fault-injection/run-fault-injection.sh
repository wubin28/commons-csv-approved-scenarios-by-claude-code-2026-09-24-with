#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -u
cd "$(dirname "$0")/.." || exit 2
root=$(pwd)
current_patch=
work=$(mktemp -d "${TMPDIR:-/tmp}/fault-injection.XXXXXX") || exit 2
reports=target/surefire-reports
fixtures=src/test/resources/org/apache/commons/csv/approved
mkdir -p fault-injection/diffs

print_license() {
    cat <<'LICENSE'
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
LICENSE
}

print_license

cleanup_received() {
    for received in "$fixtures"/*.received.md; do
        [ -e "$received" ] || continue
        rm -f "$received"
    done
}

restore() {
    if [ -n "$current_patch" ]; then
        git apply --unidiff-zero -R "$current_patch" || echo "ERROR: failed to reverse $current_patch" >&2
        current_patch=
    fi
    cleanup_received
    rm -rf "$work"
}
trap restore EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# Only production files touched by mutations and the index must be pristine.
# Guide edits and additional test files may be in progress during an S7 rerun.
if ! git diff --quiet -- src/main/java || ! git diff --cached --quiet; then
    echo "ERROR: production source or index is dirty" >&2
    exit 2
fi
if [ -n "$(git ls-files --others --exclude-standard | grep -Ev '^(fault-injection/|docs/)' || true)" ]; then
    echo "ERROR: unrelated untracked files exist" >&2
    exit 2
fi
for received in "$fixtures"/*.received.md; do
    if [ -e "$received" ]; then
        echo "ERROR: pre-existing received file: $received" >&2
        exit 2
    fi
done

run_tests() {
    mvn -q -Dtest=ApprovedScenariosTest,RequiredHeadersContractTest test >"$work/maven.log" 2>&1
}

collect_failures() {
    perl -0777 -ne '
        while (/<testcase\b([^>]*?)(?<!\/)>(.*?)<\/testcase>/sg) {
            my ($attrs, $body) = ($1, $2);
            next unless $body =~ /<(?:failure|error)\b/;
            my ($name) = $attrs =~ /\bname="([^"]+)"/;
            next unless defined $name;
            if ($name =~ /^shouldMatchApprovedResultIfFixtureIsExecuted\(\)\[(\d+)\]$/) {
                printf "%02d\n", $1;
            } else {
                $name =~ s/\(.*//;
                print "$name\n";
            }
        }
    ' "$reports/TEST-org.apache.commons.csv.ApprovedScenariosTest.xml" \
       "$reports/TEST-org.apache.commons.csv.RequiredHeadersContractTest.xml" |
        LC_ALL=C sort -u | paste -sd, -
}

expected_for() {
    awk -F '|' -v id="$1" '$2 ~ ("^[[:space:]]*" id "[[:space:]]*$") {
        x=$5; gsub(/^[[:space:]]+|[[:space:]]+$/, "", x); print x; exit
    }' fault-injection/expected.md
}

echo "Baseline: two test classes"
if ! run_tests; then
    tail -30 "$work/maven.log" >&2
    echo "ERROR: baseline is red" >&2
    exit 2
fi
echo "Baseline: PASS"
printf '%s\t%s\t%s\t%s\n' "变异" "预期" "实际" "一致？" >"$work/summary"
mismatch=0
count=0
for patch in fault-injection/[0-9][0-9]-*.patch; do
    [ -e "$patch" ] || { echo "ERROR: no patches" >&2; exit 2; }
    id=$(basename "$patch" | cut -c1-2)
    expected=$(expected_for "$id")
    [ -n "$expected" ] || { echo "ERROR: no prediction for $id" >&2; exit 2; }
    if ! git apply --unidiff-zero --check "$patch"; then
        echo "ERROR: patch cannot apply: $patch" >&2
        exit 2
    fi
    git apply --unidiff-zero "$patch" || exit 2
    current_patch=$patch
    marker="$work/before-$id"
    touch "$marker"
    run_tests
    test_status=$?
    if [ "$reports/TEST-org.apache.commons.csv.ApprovedScenariosTest.xml" -nt "$marker" ] &&
       [ "$reports/TEST-org.apache.commons.csv.RequiredHeadersContractTest.xml" -nt "$marker" ]; then
        actual=$(collect_failures)
        if [ "$test_status" -ne 0 ] && [ -z "$actual" ]; then actual=BUILD_ERROR; fi
    else
        actual=BUILD_ERROR
    fi
    diff_file="fault-injection/diffs/$id.diff"
    {
        print_license
        echo "# $patch"
        found=0
        for received in "$fixtures"/*.received.md; do
            [ -e "$received" ] || continue
            found=1
            approved="${received%.received.md}.approved.md"
            diff -u "$approved" "$received" | sed 's/^ $/<blank context>/' | head -80 || true
        done
        [ "$found" -eq 1 ] || echo "# no received files"
    } >"$diff_file"
    if [ "$actual" = "$expected" ]; then match=YES; else match=NO; mismatch=$((mismatch + 1)); fi
    printf '%s\t%s\t%s\t%s\n' "$id" "$expected" "$actual" "$match" >>"$work/summary"
    git apply --unidiff-zero -R "$current_patch" || exit 2
    current_patch=
    cleanup_received
    count=$((count + 1))
done
cat "$work/summary"
echo "Mutations: $count; mismatches: $mismatch"
[ "$mismatch" -eq 0 ]
