#!/usr/bin/env bash
set -euo pipefail

echo "=== Installing no-mistakes CLI ==="

# Install no-mistakes if not already installed
if ! command -v no-mistakes &> /dev/null; then
    echo "no-mistakes not found. Installing..."
    curl -fsSL https://raw.githubusercontent.com/kunchenguid/no-mistakes/main/docs/install.sh | sh
else
    echo "no-mistakes already installed: $(no-mistakes --version 2>/dev/null || echo 'version unknown')"
fi

echo ""
echo "=== Initializing no-mistakes for ServiceHomes ==="

# Initialize no-mistakes in the current repo
no-mistakes init

echo ""
echo "=== Setup complete ==="
echo ""
echo "Usage:"
echo "  git checkout -b my-feature"
echo "  # make changes"
echo "  git commit -am 'feat: my feature'"
echo "  git push no-mistakes"
echo ""
echo "The no-mistakes pipeline will:"
echo "  1. Rebase onto latest main"
echo "  2. Run agentic code review"
echo "  3. Run tests"
echo "  4. Update docs"
echo "  5. Push to origin"
echo "  6. Open a PR"
echo "  7. Watch CI and auto-fix failures"
echo ""
