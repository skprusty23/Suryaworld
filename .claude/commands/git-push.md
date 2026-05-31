# Git Commit and Push

Commit all pending changes and push to origin/main.

## What to ask me:
> /git-push

Then optionally tell me a commit message. If you don't, I'll write one based on the changed files.

## What I will do:
1. Run `git status` to see what changed
2. Run `git diff` to understand the changes
3. Stage the relevant files (excluding secrets like `.env`, `local.properties`, `*.keystore`)
4. Write a meaningful commit message
5. Commit and push to `origin main`

## Commit message format I follow:
```
feat: add reminders module with Room entity and list screen
fix: resolve SQLCipher key decryption crash on fresh install
refactor: move PIN verification to PinManager
docs: update CLAUDE.md with new module
```
