package com.valsi.invoicesystem.ui.screens.invoice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.ui.components.EmptyState
import com.valsi.invoicesystem.ui.components.InvoiceRow
import com.valsi.invoicesystem.ui.components.ValsiSearchBar
import com.valsi.invoicesystem.viewmodel.DatePreset
import com.valsi.invoicesystem.viewmodel.InvoiceHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(
    onInvoiceClick: (Long) -> Unit,
    onCreateInvoice: () -> Unit,
    viewModel: InvoiceHistoryViewModel = hiltViewModel(),
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateInvoice,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create invoice")
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            ValsiSearchBar(
                query = filters.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Search invoice # or customer",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Date presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DatePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = filters.datePreset == preset,
                        onClick = { viewModel.onDatePreset(preset) },
                        label = { Text(preset.label()) },
                    )
                }
            }

            // Payment status + customer filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filters.paymentStatus == null,
                    onClick = { viewModel.onPaymentFilter(null) },
                    label = { Text("All") },
                )
                PaymentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = filters.paymentStatus == status,
                        onClick = { viewModel.onPaymentFilter(status) },
                        label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CustomerFilterMenu(
                    selectedName = customers.find { it.id == filters.customerId }?.storeName,
                    customers = customers,
                    onSelect = viewModel::onCustomerFilter,
                )
                TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
            }

            if (invoices.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No invoices",
                    message = "Invoices you generate will appear here.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(invoices, key = { it.invoice.id }) { row ->
                        InvoiceRow(
                            row = row,
                            currencySymbol = currencySymbol,
                            onClick = { onInvoiceClick(row.invoice.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerFilterMenu(
    selectedName: String?,
    customers: List<com.valsi.invoicesystem.data.entity.Customer>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedName ?: "All customers")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("All customers") },
                onClick = { onSelect(null); expanded = false },
            )
            customers.forEach { customer ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(customer.storeName) },
                    onClick = { onSelect(customer.id); expanded = false },
                )
            }
        }
    }
}

private fun DatePreset.label(): String = when (this) {
    DatePreset.ALL -> "All dates"
    DatePreset.TODAY -> "Today"
    DatePreset.WEEK -> "Last 7 days"
    DatePreset.MONTH -> "Last 30 days"
}
