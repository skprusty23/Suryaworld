package com.personaltracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personaltracker.ui.screens.auth.AuthScreen
import com.personaltracker.ui.screens.auth.SetupPinScreen
import com.personaltracker.ui.screens.backup.BackupScreen
import com.personaltracker.ui.screens.credentials.AddCredentialScreen
import com.personaltracker.ui.screens.credentials.CredentialDetailScreen
import com.personaltracker.ui.screens.credentials.CredentialsScreen
import com.personaltracker.ui.screens.dashboard.DashboardScreen
import com.personaltracker.ui.screens.documents.AddDocumentScreen
import com.personaltracker.ui.screens.documents.DocumentDetailScreen
import com.personaltracker.ui.screens.documents.DocumentsScreen
import com.personaltracker.ui.screens.documents.EditDocumentScreen
import com.personaltracker.ui.screens.emi.AddEmiScreen
import com.personaltracker.ui.screens.emi.EmiDetailScreen
import com.personaltracker.ui.screens.emi.EmiScreen
import com.personaltracker.ui.screens.expenses.AddExpenseScreen
import com.personaltracker.ui.screens.expenses.ExpenseReportsScreen
import com.personaltracker.ui.screens.expenses.ExpensesScreen
import com.personaltracker.ui.screens.gold.AddGoldScreen
import com.personaltracker.ui.screens.gold.GoldScreen
import com.personaltracker.ui.screens.groups.AddGroupScreen
import com.personaltracker.ui.screens.groups.GroupDetailScreen
import com.personaltracker.ui.screens.groups.GroupExpensesScreen
import com.personaltracker.ui.screens.investments.AddInvestmentScreen
import com.personaltracker.ui.screens.investments.InvestmentDetailScreen
import com.personaltracker.ui.screens.investments.InvestmentsScreen
import com.personaltracker.ui.screens.school.AddSchoolExpenseScreen
import com.personaltracker.ui.screens.school.SchoolScreen
import com.personaltracker.ui.screens.events.AddEventScreen
import com.personaltracker.ui.screens.events.EventDetailScreen
import com.personaltracker.ui.screens.events.EventsScreen
import com.personaltracker.ui.screens.notes.AddNoteScreen
import com.personaltracker.ui.screens.notes.NoteDetailScreen
import com.personaltracker.ui.screens.notes.NotesScreen
import com.personaltracker.ui.screens.settings.AboutScreen
import com.personaltracker.ui.screens.settings.AppInfoScreen
import com.personaltracker.ui.screens.settings.PrivacyPolicyScreen
import com.personaltracker.ui.screens.settings.SecuritySettingsScreen
import com.personaltracker.ui.screens.settings.SettingsScreen
import com.personaltracker.ui.screens.splash.SplashScreen
import com.personaltracker.ui.screens.travel.AddTravelExpenseScreen
import com.personaltracker.ui.screens.travel.AddTripScreen
import com.personaltracker.ui.screens.travel.TravelScreen
import com.personaltracker.ui.screens.travel.TripDetailScreen

@Composable
fun WealthHubNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {

        // ── Auth flow ──────────────────────────────────────────────────────────
        composable(NavRoutes.SPLASH) {
            SplashScreen(onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(NavRoutes.AUTH) {
            AuthScreen(onAuthSuccess = {
                navController.navigate(NavRoutes.DASHBOARD) {
                    popUpTo(NavRoutes.AUTH) { inclusive = true }
                }
            })
        }
        composable(NavRoutes.SETUP_PIN) {
            SetupPinScreen(onPinConfigured = {
                navController.navigate(NavRoutes.DASHBOARD) {
                    popUpTo(NavRoutes.SETUP_PIN) { inclusive = true }
                }
            })
        }

        // ── Dashboard ──────────────────────────────────────────────────────────
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Documents ─────────────────────────────────────────────────────────
        composable(NavRoutes.DOCUMENTS) {
            DocumentsScreen(
                onNavigateToAdd = { navController.navigate(NavRoutes.ADD_DOCUMENT) },
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.documentDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_DOCUMENT) {
            AddDocumentScreen(onBack = { navController.popBackStack() })
        }
        composable(
            NavRoutes.DOCUMENT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            DocumentDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.editDocument(id)) }
            )
        }
        composable(
            NavRoutes.EDIT_DOCUMENT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            EditDocumentScreen(onBack = { navController.popBackStack() })
        }

        // ── Credentials ───────────────────────────────────────────────────────
        composable(NavRoutes.CREDENTIALS) {
            CredentialsScreen(
                onNavigateToAdd = { navController.navigate(NavRoutes.ADD_CREDENTIAL) },
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.credentialDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_CREDENTIAL) {
            AddCredentialScreen(onBack = { navController.popBackStack() })
        }
        composable(
            NavRoutes.CREDENTIAL_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            CredentialDetailScreen(onBack = { navController.popBackStack() })
        }

        // ── Expenses ──────────────────────────────────────────────────────────
        composable(NavRoutes.EXPENSES) {
            ExpensesScreen(
                onNavigateToAddExpense = { navController.navigate(NavRoutes.ADD_EXPENSE) },
                onNavigateToReports = { navController.navigate(NavRoutes.EXPENSE_REPORTS) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_EXPENSE) {
            AddExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.EXPENSE_REPORTS) {
            ExpenseReportsScreen(onBack = { navController.popBackStack() })
        }

        // ── Investments ───────────────────────────────────────────────────────
        composable(NavRoutes.INVESTMENTS) {
            InvestmentsScreen(
                onNavigateToAddInvestment = { navController.navigate(NavRoutes.ADD_INVESTMENT) },
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.investmentDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_INVESTMENT) {
            AddInvestmentScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.INVESTMENT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            InvestmentDetailScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }

        // ── EMI ───────────────────────────────────────────────────────────────
        composable(NavRoutes.EMI) {
            EmiScreen(
                onNavigateToAddEmi = { navController.navigate(NavRoutes.ADD_EMI) },
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.emiDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_EMI) {
            AddEmiScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.EMI_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            EmiDetailScreen(
                navController = navController,
                emiId = backStack.arguments!!.getLong("id")
            )
        }

        // ── Gold ──────────────────────────────────────────────────────────────
        composable(NavRoutes.GOLD) {
            GoldScreen(
                onNavigateToAddGold = { navController.navigate(NavRoutes.ADD_GOLD) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_GOLD) {
            AddGoldScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ── School ────────────────────────────────────────────────────────────
        composable(NavRoutes.SCHOOL) {
            SchoolScreen(
                onNavigateToAddSchoolExpense = { navController.navigate(NavRoutes.ADD_SCHOOL_EXPENSE) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_SCHOOL_EXPENSE) {
            AddSchoolExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ── Travel ────────────────────────────────────────────────────────────
        composable(NavRoutes.TRAVEL) {
            TravelScreen(
                onNavigateToAddTrip = { navController.navigate(NavRoutes.ADD_TRIP) },
                onNavigateToTripDetail = { id -> navController.navigate(NavRoutes.tripDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_TRIP) {
            AddTripScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.TRIP_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            TripDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAddExpense = { tripId -> navController.navigate(NavRoutes.addTravelExpense(tripId)) }
            )
        }
        composable(
            NavRoutes.ADD_TRAVEL_EXPENSE,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) {
            AddTravelExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ── Group Expenses ────────────────────────────────────────────────────
        composable(NavRoutes.GROUP_EXPENSES) {
            GroupExpensesScreen(
                onNavigateToAddGroup = { navController.navigate(NavRoutes.ADD_GROUP) },
                onNavigateToGroupDetail = { id -> navController.navigate(NavRoutes.groupDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_GROUP) {
            AddGroupScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.GROUP_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            GroupDetailScreen(onBack = { navController.popBackStack() })
        }

        // ── Notes ─────────────────────────────────────────────────────────────
        composable(NavRoutes.NOTES) {
            NotesScreen(
                onNavigateToAdd = { navController.navigate(NavRoutes.ADD_NOTE) },
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.noteDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_NOTE) {
            AddNoteScreen(onBack = { navController.popBackStack() })
        }
        composable(
            NavRoutes.NOTE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val noteId = backStack.arguments!!.getLong("id")
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.editNote(id)) }
            )
        }
        composable(
            NavRoutes.EDIT_NOTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val noteId = backStack.arguments!!.getLong("id")
            AddNoteScreen(noteId = noteId, onBack = { navController.popBackStack() })
        }

        // ── Events ─────────────────────────────────────────────────────────────
        composable(NavRoutes.EVENTS) {
            EventsScreen(
                onNavigateToAdd = { navController.navigate(NavRoutes.ADD_EVENT) },
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.eventDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ADD_EVENT) {
            AddEventScreen(onBack = { navController.popBackStack() })
        }
        composable(
            NavRoutes.EVENT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val eventId = backStack.arguments!!.getLong("id")
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.editEvent(id)) }
            )
        }
        composable(
            NavRoutes.EDIT_EVENT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val eventId = backStack.arguments!!.getLong("id")
            AddEventScreen(eventId = eventId, onBack = { navController.popBackStack() })
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateToSecurity = { navController.navigate(NavRoutes.SECURITY_SETTINGS) },
                onNavigateToBackup = { navController.navigate(NavRoutes.BACKUP) },
                onNavigateToAbout = { navController.navigate(NavRoutes.ABOUT) },
                onNavigateToPrivacyPolicy = { navController.navigate(NavRoutes.PRIVACY_POLICY) },
                onNavigateToAppInfo = { navController.navigate(NavRoutes.APP_INFO) },
                onLogout = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.SECURITY_SETTINGS) {
            SecuritySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.APP_INFO) {
            AppInfoScreen(onBack = { navController.popBackStack() })
        }
    }
}
