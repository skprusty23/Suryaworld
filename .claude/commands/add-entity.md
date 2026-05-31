# Add New Database Entity

Add a new Room entity with full repository wiring to the Personal Tracker app.

## What to ask me:
> /add-entity

Then tell me:
1. **Entity name** (e.g. `Reminder`)
2. **Fields** (name, type, nullable?)
3. **Table name** (e.g. `reminders`)
4. **Queries needed** (getAll, getById, getByDate, search?)

## What I will create / update:
- `data/database/entity/XxxEntity.kt`
- `data/database/dao/XxxDao.kt`
- `domain/repository/XxxRepository.kt` (interface)
- `data/repository/XxxRepositoryImpl.kt` (@Singleton impl)
- Update `di/AppModule.kt` → add @Binds
- Update `di/DatabaseModule.kt` → add @Provides DAO
- Update `data/database/AppDatabase.kt` → add to @Database entities list, bump version, add Migration

## Migration template I use:
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `my_table` (...)")
    }
}
```
