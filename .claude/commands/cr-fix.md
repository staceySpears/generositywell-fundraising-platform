# /cr-fix

Fetch the **latest** CodeRabbit review for the current branch's open PR and apply all unresolved findings.

Use this after CodeRabbit's green check appears on GitHub. Running it too early returns stale data because the GitHub API is pull-only — CodeRabbit's review won't exist yet.

## What this does

1. Resolves the open PR number for the current branch
2. Checks whether CodeRabbit is still processing (exits early with a message if so)
3. Fetches every unresolved, non-outdated CodeRabbit review thread via GraphQL
4. For each thread: reads the relevant code, assesses validity, proposes a fix
5. Shows each proposed fix and asks for approval before applying
6. Commits all approved fixes in a single `fix: apply CodeRabbit auto-fixes` commit

## When to run

```bash
git push
# wait ~90 seconds for the CodeRabbit status check to go green on GitHub
/cr-fix
```

Or from the terminal without opening Claude Code first:

```bash
git push && ./cr-fix.sh
```

## Invoke the autofix skill

$ARGUMENTS
