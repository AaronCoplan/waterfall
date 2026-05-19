#!/usr/bin/env bash
# §7.2: P11 conditional golden gate. Allows the documented set to differ;
# enforces byte-identical everywhere else.
#
# Usage:
#   ./scripts/check-goldens-unchanged-except-p11.sh [TARGET]
#
# TARGET defaults to 'master' (compares working tree against merge base) — the
# right choice for CI and manual gating on the feature branch.
# Pass a specific SHA (e.g., 8c3dbb1) to compare against a per-commit baseline
# during §4.1 development.
#
# NOTE: Full CI wiring (pre-commit hook or Gradle task) is a phase-exit
# deliverable per spec §7.6 checklist. For §4.1, only this script's correct
# default behavior is required; CI integration lands at P11 phase exit.
#
# Run at every commit. Exits non-zero if any UNDOCUMENTED golden has changed.

# Documented-to-change set (per §7.2 of PHASE-11-design.md).
# These goldens become empty strings when the verifier rejects their source programs
# (ControlFlowModule: undeclared `things` in for-collection + `doSomething` local call;
# WhileModule: undeclared `doSomething` local call). OQ-11.6=strict (Aaron, 2026-05-19).
DOCUMENTED_CHANGES=(
  "compiler/src/test/resources/golden/c/ControlFlowModule.expected"
  "compiler/src/test/resources/golden/python/ControlFlowModule.expected"
  "compiler/src/test/resources/golden/js/ControlFlowModule.expected"
  "compiler/src/test/resources/golden/c/WhileModule.expected"
  "compiler/src/test/resources/golden/python/WhileModule.expected"
  "compiler/src/test/resources/golden/js/WhileModule.expected"
)

# git diff against master (default) or a specific SHA passed as first arg:
TARGET="${1:-master}"

# All goldens NOT in the documented set: require byte-identical.
DIFF_ARGS=(--exit-code -- compiler/src/test/resources/golden/)
for f in "${DOCUMENTED_CHANGES[@]}"; do
  DIFF_ARGS+=(":(exclude)$f")
done

if ! git diff "$TARGET" "${DIFF_ARGS[@]}" >/dev/null 2>&1; then
  echo "ERROR: Undocumented golden change detected." >&2
  echo "Diff (excluding documented-to-change set):" >&2
  git diff "$TARGET" "${DIFF_ARGS[@]}"
  exit 1
fi

# Content verification of documented-to-change goldens is GoldenTests.kt's job
# (parameterized snapshot tests that compare against the .expected files).
# This script only enforces that UNDOCUMENTED goldens are unchanged.
echo "Golden gate passed: undocumented goldens are byte-identical to $TARGET."
