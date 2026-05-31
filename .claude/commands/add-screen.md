# Add New Screen

Add a complete new screen to the SuryaWorld Personal Tracker app.

## What to ask me:
> /add-screen

Then tell me:
1. **Screen name** (e.g. "Reminders", "Notes")
2. **Module path** (e.g. `reminders` → goes in `ui/screens/reminders/`)
3. **What data it stores** (fields and types)
4. **Features** (list, add, detail view, edit, delete?)
5. **Route name** (e.g. `reminders`, `reminder_detail/{id}`)

## What I will create:
- `data/database/entity/XxxEntity.kt` — Room entity
- `data/database/dao/XxxDao.kt` — DAO with Flow queries
- `domain/repository/XxxRepository.kt` — interface
- `data/repository/XxxRepositoryImpl.kt` — @Singleton implementation
- `ui/screens/xxx/XxxScreen.kt` — list screen (ViewModel + Composable in one file)
- `ui/screens/xxx/AddXxxScreen.kt` — add/edit form screen
- `ui/screens/xxx/XxxDetailScreen.kt` — detail screen (if needed)
- Update `di/AppModule.kt` — @Binds binding
- Update `di/DatabaseModule.kt` — @Provides DAO
- Update `data/database/AppDatabase.kt` — add entity, bump version + Migration
- Update `ui/navigation/NavRoutes.kt` — add route constants
- Update `ui/navigation/NavGraph.kt` — wire composable() routes
- Update `ui/screens/dashboard/DashboardScreen.kt` — add navigation card

## Key rules I follow:
- ViewModel + Screen composable in the same `.kt` file
- `PTTopBar` for all top bars
- `ConfirmDeleteDialog` for delete confirmations
- `LocalDateTime` stored via `Converters.kt`
- All data local only — no cloud
