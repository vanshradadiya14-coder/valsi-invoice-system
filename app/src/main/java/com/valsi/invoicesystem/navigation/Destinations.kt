package com.valsi.invoicesystem.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes in the app. Parameterised routes expose builder helpers. */
object Routes {
    // Bottom-nav (top-level) destinations
    const val HOME = "home"
    const val INVOICES = "invoices"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"

    // Secondary destinations
    const val PRODUCTS = "products"

    const val CUSTOMER_ADD = "customer_add"
    const val CUSTOMER_EDIT = "customer_edit/{customerId}"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"

    const val INVOICE_DETAIL = "invoice_detail/{invoiceId}"

    // Create-invoice flow. Optional duplicateFrom pre-fills from an existing invoice.
    const val CREATE_INVOICE = "create_invoice?duplicateFrom={duplicateFrom}"

    fun customerEdit(customerId: Long) = "customer_edit/$customerId"
    fun customerDetail(customerId: Long) = "customer_detail/$customerId"
    fun invoiceDetail(invoiceId: Long) = "invoice_detail/$invoiceId"
    fun createInvoice(duplicateFrom: Long? = null) =
        if (duplicateFrom != null) "create_invoice?duplicateFrom=$duplicateFrom" else "create_invoice"

    // Argument keys
    const val ARG_CUSTOMER_ID = "customerId"
    const val ARG_INVOICE_ID = "invoiceId"
    const val ARG_DUPLICATE_FROM = "duplicateFrom"
    const val NO_DUPLICATE = -1L
}

/** The four bottom-navigation tabs. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    INVOICES(Routes.INVOICES, "Invoices", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    CUSTOMERS(Routes.CUSTOMERS, "Customers", Icons.Filled.People, Icons.Outlined.People),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}
