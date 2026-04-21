# No-Mistakes Integration

This repository uses [no-mistakes](https://github.com/kunchenguid/no-mistakes) as a QA gate for all code changes.

## What is no-mistakes?

`no-mistakes` puts a local git proxy in front of your real remote. Push to `no-mistakes` instead of `origin`, and it spins up a disposable worktree, runs an AI-driven validation pipeline, forwards upstream only after every check passes, and opens a clean PR automatically.

**Key features:**
- **Rebase onto latest main** and resolve conflicts automatically
- **Agentic review** to find code problems
- **Agentic testing** to make sure changes actually work
- **Update docs** when code changes require it
- **Raise a PR** automatically
- **Babysit CI**, auto-fix errors

## Installation

```bash
# Install no-mistakes CLI
curl -fsSL https://raw.githubusercontent.com/kunchenguid/no-mistakes/main/docs/install.sh | sh

# Initialize in this repo
./scripts/install-no-mistakes.sh
```

## Workflow

### Daily Development Flow

```bash
# 1. Create a feature branch
git checkout -b feat/my-feature

# 2. Make changes and commit
git add -A
git commit -m "feat: add new feature"

# 3. Push through the no-mistakes gate
./scripts/push-no-mistakes.sh

# 4. Monitor the pipeline
no-mistakes
```

### What Happens After Push

1. **Rebase** — your branch is rebased onto latest `master`
2. **Review** — AI agent reviews code for bugs, style, security issues
3. **Test** — backend tests (Spring Boot + Testcontainers), frontend build (Angular), dbt models
4. **Docs** — README, ADRs, API contracts checked and updated if needed
5. **Push** — clean branch pushed to `origin`
6. **PR** — pull request opened automatically
7. **CI** — GitHub Actions runs, failures are auto-fixed

## GitHub Actions Pipeline

The repository also runs the **No-Mistakes QA Pipeline** on every PR and push to `master`:

| Job | Purpose |
|-----|---------|
| `rebase-check` | Validates PR can be rebased cleanly |
| `backend-qa` | Spring Boot compile, unit tests, integration tests |
| `frontend-qa` | Angular lint, build, tests |
| `dbt-qa` | dbt deps, parse, tests |
| `docs-qa` | README freshness, ADR completeness, API contracts |
| `security-qa` | Trivy vulnerability scan, TruffleHog secret detection |
| `qa-gate` | Final approval gate with PR comment |

### PR Comment Example

When a PR is opened, the pipeline posts a comment:

```
## No-Mistakes QA Pipeline Results

| Check | Status |
|-------|--------|
| Rebase | ✅ |
| Backend | ✅ |
| Frontend | ✅ |
| dbt | ✅ |
| Docs | ✅ |
| Security | ✅ |

✅ All checks passed! Ready to merge.
```

## Local Development (Bypassing the Gate)

For rapid local iteration, you can still push directly to your fork or a feature branch:

```bash
# Direct push (skips no-mistakes - not recommended for final PRs)
git push origin feat/my-feature
```

**Note:** The GitHub Actions QA pipeline will still run on the PR.

## Configuration

### Environment Variables

| Variable | Purpose |
|----------|---------|
| `NM_HOME` | no-mistakes state directory (default: `~/.no-mistakes`) |
| `OPENAI_API_KEY` | Required for agentic review (if using OpenAI) |
| `ANTHROPIC_API_KEY` | Required for agentic review (if using Claude) |

### Agent Configuration

no-mistakes supports multiple AI agents:
- `claude` (Anthropic Claude)
- `codex` (OpenAI Codex)
- `opencode` (OpenCode — used in this repo)
- `rovodev` (Rovo Dev)

Configure in `~/.no-mistakes/config.yaml`:

```yaml
agent:
  provider: opencode
  model: kimi-k2.6
```

## Troubleshooting

### "no-mistakes remote not found"

Run the installation script:
```bash
./scripts/install-no-mistakes.sh
```

### Pipeline fails on rebase

Pull latest master and rebase locally:
```bash
git fetch origin
git rebase origin/master
# resolve conflicts
git push no-mistakes --force-with-lease
```

### Pipeline fails on tests

Run tests locally first:
```bash
make backend-test   # Spring Boot tests
make frontend-build # Angular build
make dbt-test       # dbt tests
```

## Rules of Engagement

1. **Always push through no-mistakes for production code**
2. **Never force-push to `master`**
3. **Wait for QA pipeline to pass before merging**
4. **Address all AI review comments** — they're usually correct
5. **Keep commits atomic** — one logical change per commit

## Resources

- [no-mistakes Documentation](https://kunchenguid.github.io/no-mistakes/)
- [no-mistakes GitHub](https://github.com/kunchenguid/no-mistakes)
- [Discord Community](https://discord.gg/Wsy2NpnZDu)
