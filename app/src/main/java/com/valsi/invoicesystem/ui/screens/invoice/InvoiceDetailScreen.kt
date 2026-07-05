package com.valsi.invoicesystem.ui.screens.invoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valsi.invoicesystem.data.entity.InvoiceDetail
import com.valsi.invoicesystem.data.entity.InvoiceStatus
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.pdf.PdfActions
import com.valsi.invoicesystem.ui.components.LoadingBox
import com.valsi.invoicesystem.ui.components.PaymentStatusChip
import com.valsi.invoicesystem.util.DateUtils
import com.valsi.invoicesystem.util.Money
import com.valsi.invoicesystem.viewmodel.DetailEvent
import com.valsi.invoicesystem.viewmodel.InvoiceDetailViewModel
import com.valsi.invoicesystem.viewmodel.PdfAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    onDuplicate: (Long) -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var menuExpanded by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var confirmVoid by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is DetailEvent.PdfReady -> when (event.action) {
                    PdfAction.SHARE -> PdfActions.share(context, event.file, "Invoice ${event.invoiceNumber}")
                    PdfAction.PRINT -> PdfActions.print(context, event.file, event.invoiceNumber)
                    PdfAction.VIEW -> PdfActions.view(context, event.file)
                    PdfAction.SAVE -> {
                        val location = PdfActions.saveToDownloads(context, event.file)
                        snackbarHostState.showSnackbar(
                            if (location != null) "Saved to $location" else "Couldn't save to Downloads",
                        )
                    }
                }
            }
        }
    }

    val detail = state.detail
    val invoice = detail?.invoice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(invoice?.invoiceNumber ?: "Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = { menuExpanded = false; onDuplicate(viewModel.invoiceId) },
                        )
                        if (invoice?.status == InvoiceStatus.FINALIZED) {
                            DropdownMenuItem(
                                text = { Text("Void invoice") },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                onClick = { menuExpanded = false; confirmVoid = true },
                            )
                        }
                        if (invoice?.status == InvoiceStatus.DRAFT) {
                            DropdownMenuItem(
                                text = { Text("Delete draft") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menuExpanded = false; confirmDelete = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(innerPadding))
            detail == null || invoice == null -> Text(
                "Invoice not found.",
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { StatusHeader(detail, state.settings.currencySymbol) }
                item { PdfActionsRow(onAction = viewModel::preparePdf) }
                item { LineItemsCard(detail, state.settings.currencySymbol) }
                item { TotalsCard(detail, state.settings.currencySymbol) }
                if (invoice.status == InvoiceStatus.FINALIZED) {
                    item {
                        OutlinedButton(
                            onClick = { showPaymentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Update Payment Status")
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog && invoice != null) {
        UpdatePaymentDialog(
            currentStatus = invoice.paymentStatus,
            grandTotal = invoice.grandTotal,
            currencySymbol = state.settings.currencySymbol,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { status, amount ->
                viewModel.updatePaymentStatus(status, amount)
                showPaymentDialog = false
            },
        )
    }

    if (confirmVoid) {
        AlertDialog(
            onDismissRequest = { confirmVoid = false },
            title = { Text("Void this invoice?") },
            text = { Text("Voiding keeps the record and invoice number but marks it cancelled. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.voidInvoice(); confirmVoid = false }) { Text("Void") }
            },
            dismissButton = { TextButton(onClick = { confirmVoid = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete draft?") },
            text = { Text("This draft invoice will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDraft(onDeleted = onBack)
                    confirmDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatusHeader(detail: InvoiceDetail, currencySymbol: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(detail.invoice.invoiceNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        DateUtils.formatDateTime(detail.invoice.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    PaymentStatusChip(detail.invoice.paymentStatus)
                    if (detail.invoice.status == InvoiceStatus.VOID) {
                        Text("VOID", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Bill to", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail.customer.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail.customer.ownerName, style = MaterialTheme.typography.bodyMedium)
            if (detail.customer.address.isNotBlank()) {
                Text(detail.customer.address, style = MaterialTheme.typography.bodyMedium)
            }
            Text(detail.customer.phoneNumber, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PdfActionsRow(onAction: (PdfAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PdfActionButton("Share", Icons.Filled.Share, Modifier.weight(1f)) { onAction(PdfAction.SHARE) }
        PdfActionButton("Print", Icons.Filled.Print, Modifier.weight(1f)) { onAction(PdfAction.PRINT) }
        PdfActionButton("Save", Icons.Filled.Save, Modifier.weight(1f)) { onAction(PdfAction.SAVE) }
        PdfActionButton("Open", Icons.Filled.PictureAsPdf, Modifier.weight(1f)) { onAction(PdfAction.VIEW) }
    }
}

@Composable
private fun PdfActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LineItemsCard(detail: InvoiceDetail, currencySymbol: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Item", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("Qty", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp))
                Text("Total", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Spacer(Modifier.height(8.dp))
            detail.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.productNameSnapshot, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${Money.format(item.unitPriceSnapshot, currencySymbol)} each",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(item.quantity.toString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(40.dp))
                    Text(
                        Money.format(item.lineTotal, currencySymbol),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(detail: InvoiceDetail, currencySymbol: String) {
    val inv = detail.invoice
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SummaryLine("Subtotal", Money.format(inv.subtotal, currencySymbol))
            if (inv.discountAmount > 0.0) SummaryLine("Discount", "- ${Money.format(inv.discountAmount, currencySymbol)}")
            SummaryLine("VAT", Money.format(inv.vatAmount, currencySymbol))
            Spacer(Modifier.height(6.dp))
            SummaryLine("Grand Total", Money.format(inv.grandTotal, currencySymbol), emphasize = true)
            if (inv.paymentStatus == PaymentStatus.PARTIAL) {
                Spacer(Modifier.height(6.dp))
                SummaryLine("Amount Paid", Money.format(inv.amountPaid, currencySymbol))
                SummaryLine("Balance", Money.format(inv.grandTotal - inv.amountPaid, currencySymbol))
            }
            inv.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text("Notes: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePaymentDialog(
    currentStatus: PaymentStatus,
    grandTotal: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (PaymentStatus, Double) -> Unit,
) {
    var status by remember { mutableStateOf(currentStatus) }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update payment") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                if (status == PaymentStatus.PARTIAL) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount paid (of ${Money.format(grandTotal, currencySymbol)})") },
                        prefix = { Text(currencySymbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(status, amount) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
