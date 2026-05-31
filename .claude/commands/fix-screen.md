# Fix a Screen

Debug and fix an issue in an existing screen.

## What to ask me:
> /fix-screen

Then tell me:
1. **Which screen** (e.g. "Document Detail", "Add Expense")
2. **What's broken** (e.g. "edit button does nothing", "crashes on open", "data not showing")
3. Any error message you see (logcat output if available)

## What I will do:
1. Read the relevant screen file(s)
2. Read the ViewModel and repository
3. Check NavGraph wiring
4. Identify the root cause
5. Fix it and explain what was wrong

## Common issues I check:
- Empty lambda defaults `onEdit: () -> Unit = {}` in NavGraph
- Missing `LaunchedEffect` for navigation after save/delete
- Wrong `savedStateHandle["id"]` key
- Flow not collected (missing `.collect {}` or `.collectAsState()`)
- Missing `@Singleton` on repository causing multiple instances
- Room query returning wrong type (suspend vs Flow)
