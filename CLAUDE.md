# SuryaWorld — Personal Tracker

## Project Location
`E:\poc\persosnaltracker`

## What This App Does
Offline-first Android app to track personal documents, credentials, expenses, investments, EMIs, gold, school fees, and travel. All data is stored **locally only** — no cloud sync ever.

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room + SQLCipher (AES-256 encrypted) |
| Prefs | DataStore Preferences |
| Auth | PIN (SHA-256 + salt) + Biometric |
| Images | Coil |
| Nav | Navigation Compose |
| Min SDK | 26 (Android 8) |

## Package
`com.personaltracker`

## Architecture (MVVM + Clean)
```
app/src/main/java/com/personaltracker/
├── data/
│   ├── database/
│   │   ├── entity/          ← Room @Entity classes
│   │   ├── dao/             ← @Dao interfaces
│   │   └── AppDatabase.kt   ← @Database, SQLCipher setup
│   └── repository/          ← Repository implementations
├── domain/
│   └── repository/          ← Repository interfaces
├── security/
│   ├── SecurityManager.kt   ← AES/GCM encryption, DB key
│   ├── PinManager.kt        ← PIN hash, biometric toggle
│   └── BiometricHelper.kt
├── di/
│   ├── DatabaseModule.kt    ← Hilt provides DB, DAOs
│   └── AppModule.kt         ← Hilt binds repositories
└── ui/
    ├── components/          ← PTTopBar, ConfirmDeleteDialog, etc.
    ├── navigation/
    │   ├── NavRoutes.kt     ← All route constants + helper fns
    │   └── NavGraph.kt      ← All composable() route wiring
    ├── screens/
    │   ├── auth/            ← AuthScreen, SetupPinScreen
    │   ├── dashboard/       ← DashboardScreen
    │   ├── documents/       ← DocumentsScreen, AddDocumentScreen, DocumentDetailScreen, EditDocumentScreen
    │   ├── credentials/     ← CredentialsScreen, AddCredentialScreen, CredentialDetailScreen
    │   ├── expenses/        ← ExpensesScreen, AddExpenseScreen, ExpenseReportsScreen
    │   ├── investments/     ← InvestmentsScreen, AddInvestmentScreen, InvestmentDetailScreen
    │   ├── emi/             ← EmiScreen, AddEmiScreen, EmiDetailScreen
    │   ├── gold/            ← GoldScreen, AddGoldScreen
    │   ├── school/          ← SchoolScreen, AddSchoolExpenseScreen
    │   ├── travel/          ← TravelScreen, AddTripScreen, TripDetailScreen, AddTravelExpenseScreen
    │   ├── groups/          ← GroupExpensesScreen, AddGroupScreen, GroupDetailScreen
    │   ├── settings/        ← SettingsScreen, SecuritySettingsScreen
    │   ├── backup/          ← BackupScreen
    │   └── splash/          ← SplashScreen
    └── theme/               ← Color.kt, Type.kt, Theme.kt
```

## Key Patterns

### Adding a New Screen
Every screen file contains **both** ViewModel and Composable in the same file.

```kotlin
// 1. State data class
data class MyFeatureState(val items: List<MyEntity> = emptyList(), val isLoading: Boolean = true)

// 2. HiltViewModel
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MyFeatureState())
    val state: StateFlow<MyFeatureState> = _state.asStateFlow()
    init { loadData() }
    private fun loadData() {
        viewModelScope.launch {
            repository.getAll().collect { items ->
                _state.value = MyFeatureState(items = items, isLoading = false)
            }
        }
    }
}

// 3. Composable screen
@Composable
fun MyFeatureScreen(onBack: () -> Unit, viewModel: MyFeatureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { PTTopBar(title = "My Feature", onBack = onBack) }) { padding ->
        // ...
    }
}
```

### Adding a New Entity
```kotlin
@Entity(tableName = "my_items")
data class MyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```
Then: create DAO → create repository interface in `domain/` → implement in `data/repository/` → add `@Binds` in AppModule → add `@Provides` DAO in DatabaseModule → increment database `version` and add a Migration.

### Adding a Route
```kotlin
// NavRoutes.kt
const val MY_SCREEN = "my_screen"
fun myScreen(id: Long) = "my_screen/$id"   // if parameterised

// NavGraph.kt
composable(NavRoutes.MY_SCREEN) {
    MyScreen(onBack = { navController.popBackStack() })
}
```

### Logout / Lock Pattern
Navigate to AUTH and clear the entire back stack — PIN is NOT cleared, data stays safe:
```kotlin
navController.navigate(NavRoutes.AUTH) {
    popUpTo(0) { inclusive = true }
}
```

## Security Rules (DO NOT CHANGE)
- Database key is a 32-byte random key stored encrypted in SharedPreferences via AndroidKeyStore + AES/GCM.
- The IV is always stored alongside the ciphertext (first 12 bytes of the blob).
- **No cloud storage, no Firebase, no Google Drive sync.** All data lives on-device only.

## Database
- **File:** `AppDatabase.kt` — SQLCipher `SupportFactory` is used.
- **Converters:** `Converters.kt` handles `LocalDate`, `LocalDateTime`.
- Bump `version` and add a `Migration` object every time you change an entity.

## Common Components
| Component | Usage |
|---|---|
| `PTTopBar` | Top app bar with back button + optional actions slot |
| `ConfirmDeleteDialog` | Reusable delete confirmation dialog |

## Git Workflow
Branch: `main` | Remote: already configured
```
git add <files>
git commit -m "feat: describe what changed"
git push
```

## Build
Open in **Android Studio**. Gradle sync will download all deps. Run on device/emulator (minSdk 26).
No CI/CD configured.

## Modules List
Documents · Credentials · Expenses · Investments · EMI · Gold · School Fees · Travel · Group Expenses · Settings · Backup · Auth (PIN + Biometric)
