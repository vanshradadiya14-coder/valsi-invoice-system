package com.valsi.invoicesystem.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.valsi.invoicesystem.ui.screens.customer.CustomerDetailScreen
import com.valsi.invoicesystem.ui.screens.customer.CustomerEditScreen
import com.valsi.invoicesystem.ui.screens.customer.CustomerListScreen
import com.valsi.invoicesystem.ui.screens.home.HomeScreen
import com.valsi.invoicesystem.ui.screens.invoice.CreateInvoiceScreen
import com.valsi.invoicesystem.ui.screens.invoice.InvoiceDetailScreen
import com.valsi.invoicesystem.ui.screens.invoice.InvoiceHistoryScreen
import com.valsi.invoicesystem.ui.screens.product.ProductListScreen
import com.valsi.invoicesystem.ui.screens.settings.SettingsScreen

/** Root composable: bottom-nav scaffold hosting the app's navigation graph. */
@Composable
fun ValsiApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    Scaffold(
        // Each screen owns a Scaffold/TopAppBar that handles the top inset, so the outer
        // scaffold must not consume window insets again (avoids double status-bar padding).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) ValsiBottomBar(navController)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToCustomers = { navController.navigate(Routes.CUSTOMERS) },
                    onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                    onCreateInvoice = { navController.navigate(Routes.createInvoice()) },
                    onNavigateToInvoiceHistory = { navController.navigate(Routes.INVOICES) },
                )
            }

            customerGraph(navController)

            composable(Routes.PRODUCTS) {
                ProductListScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.CREATE_INVOICE,
                arguments = listOf(
                    navArgument(Routes.ARG_DUPLICATE_FROM) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_ID
                    },
                    navArgument(Routes.ARG_EDIT_DRAFT) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_ID
                    },
                ),
            ) {
                CreateInvoiceScreen(
                    onClose = { navController.popBackStack() },
                    onInvoiceGenerated = { invoiceId ->
                        navController.navigate(Routes.invoiceDetail(invoiceId)) {
                            // Replace the create flow so Back returns to where it started.
                            popUpTo(Routes.CREATE_INVOICE) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.INVOICES) {
                InvoiceHistoryScreen(
                    onInvoiceClick = { navController.navigate(Routes.invoiceDetail(it)) },
                    onCreateInvoice = { navController.navigate(Routes.createInvoice()) },
                )
            }

            composable(
                route = Routes.INVOICE_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_INVOICE_ID) { type = NavType.LongType }),
            ) {
                InvoiceDetailScreen(
                    onBack = { navController.popBackStack() },
                    onDuplicate = { invoiceId ->
                        navController.navigate(Routes.createInvoice(duplicateFrom = invoiceId))
                    },
                    onEditDraft = { invoiceId ->
                        navController.navigate(Routes.createInvoice(editDraft = invoiceId)) {
                            // Leave the draft's detail screen; we're resuming it now.
                            popUpTo(Routes.INVOICE_DETAIL) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

private fun NavGraphBuilder.customerGraph(navController: NavHostController) {
    composable(Routes.CUSTOMERS) {
        CustomerListScreen(
            onBack = { navController.popBackStack() },
            onAddCustomer = { navController.navigate(Routes.CUSTOMER_ADD) },
            onCustomerClick = { navController.navigate(Routes.customerDetail(it)) },
            onEditCustomer = { navController.navigate(Routes.customerEdit(it)) },
        )
    }
    composable(Routes.CUSTOMER_ADD) {
        CustomerEditScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.CUSTOMER_EDIT,
        arguments = listOf(navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.LongType }),
    ) {
        CustomerEditScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.CUSTOMER_DETAIL,
        arguments = listOf(navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.LongType }),
    ) {
        CustomerDetailScreen(
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(Routes.customerEdit(it)) },
            onInvoiceClick = { navController.navigate(Routes.invoiceDetail(it)) },
        )
    }
}

