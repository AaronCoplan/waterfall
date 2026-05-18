#!/usr/bin/env bash
# §5.5 golden-diff gate. Run before every backend-migration commit.
# Exits non-zero if any golden file has changed vs. the committed state.
# Usage: ./scripts/check-goldens-unchanged.sh
git diff --exit-code -- compiler/src/test/resources/golden/
