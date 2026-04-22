#!/usr/bin/env bash
# Push helper for no-mistakes gate
# Usage: ./scripts/push-no-mistakes.sh [branch-name]

set -euo pipefail

BRANCH=${1:-$(git rev-parse --abbrev-ref HEAD)}

echo "=== ServiceHomes No-Mistakes Push ==="
echo "Branch: $BRANCH"
echo ""

# Check if no-mistakes remote exists
if ! git remote | grep -q "no-mistakes"; then
    echo "no-mistakes remote not found."
    echo "Run: ./scripts/install-no-mistakes.sh"
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "You have uncommitted changes."
    read -p "Commit them now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -p "Commit message: " msg
        git add -A
        git commit -m "$msg"
    else
        echo "Please commit or stash your changes before pushing."
        exit 1
    fi
fi

echo "Pushing to no-mistakes gate..."
git push no-mistakes "$BRANCH"

echo ""
echo "Push initiated."
echo "Run 'no-mistakes' to monitor the pipeline."
