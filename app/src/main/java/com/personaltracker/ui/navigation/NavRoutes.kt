package com.personaltracker.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val SETUP_PIN = "setup_pin"
    const val DASHBOARD = "dashboard"

    // Documents
    const val DOCUMENTS = "documents"
    const val DOCUMENT_DETAIL = "document_detail/{id}"
    const val ADD_DOCUMENT = "add_document"

    // Credentials
    const val CREDENTIALS = "credentials"
    const val CREDENTIAL_DETAIL = "credential_detail/{id}"
    const val ADD_CREDENTIAL = "add_credential"

    // Expenses
    const val EXPENSES = "expenses"
    const val ADD_EXPENSE = "add_expense"
    const val EXPENSE_REPORTS = "expense_reports"

    // Investments
    const val INVESTMENTS = "investments"
    const val ADD_INVESTMENT = "add_investment"
    const val INVESTMENT_DETAIL = "investment_detail/{id}"

    // EMI
    const val EMI = "emi"
    const val ADD_EMI = "add_emi"
    const val EMI_DETAIL = "emi_detail/{id}"

    // Gold
    const val GOLD = "gold"
    const val ADD_GOLD = "add_gold"

    // School
    const val SCHOOL = "school"
    const val ADD_SCHOOL_EXPENSE = "add_school_expense"

    // Travel
    const val TRAVEL = "travel"
    const val TRIP_DETAIL = "trip_detail/{id}"
    const val ADD_TRIP = "add_trip"
    const val ADD_TRAVEL_EXPENSE = "add_travel_expense/{tripId}"

    // Group Expenses
    const val GROUP_EXPENSES = "group_expenses"
    const val GROUP_DETAIL = "group_detail/{id}"
    const val ADD_GROUP = "add_group"

    // Settings
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val SECURITY_SETTINGS = "security_settings"

    fun documentDetail(id: Long) = "document_detail/$id"
    fun credentialDetail(id: Long) = "credential_detail/$id"
    fun investmentDetail(id: Long) = "investment_detail/$id"
    fun emiDetail(id: Long) = "emi_detail/$id"
    fun tripDetail(id: Long) = "trip_detail/$id"
    fun addTravelExpense(tripId: Long) = "add_travel_expense/$tripId"
    fun groupDetail(id: Long) = "group_detail/$id"
}
