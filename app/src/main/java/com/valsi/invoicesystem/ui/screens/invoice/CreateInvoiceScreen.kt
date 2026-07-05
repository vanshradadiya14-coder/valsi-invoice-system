package com.valsi.invoicesystem.ui.screens.invoice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valsi.invoicesystem.data.entity.Customer
import com.valsi.invoicesystem.data.entity.PaymentStatus
import com.valsi.invoicesystem.data.entity.Product
import com.valsi.invoicesystem.ui.components.EmptyState
import com.valsi.invoicesystem.ui.components.QuantityStepper
import com.valsi.invoicesystem.ui.components.ValsiSearchBar
import com.valsi.invoicesystem.ui.theme.BalanceDue
import com.valsi.invoicesystem.util.Money
import com.valsi.invoicesystem.viewmodel.CartLine
import com.valsi.invoicesystem.viewmodel.DiscountMode
import com.valsi.invoicesystem.viewmodel.InvoiceCreationState
import com.valsi.invoicesystem.viewmodel.InvoiceCreationViewModel
import com.valsi.invoicesystem.viewmodel.InvoiceStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    onClose: () -> Unit,
    onInvoiceGenerated: (Long) -> Unit,
    viewModel: InvoiceCreationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customerQuery by viewModel.customerQuery.collectAsStateWithLifecycle()
    val productQuery by viewModel.productQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.generatedInvoiceId) {
        state.generatedInvoiceId?.let(onInvoiceGenerated)
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(state.step)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.step == InvoiceStep.CUSTOMER) onClose() else viewModel.back()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.step == InvoiceStep.PRODUCTS && state.itemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.goToStep(InvoiceStep.CART) },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    text = { Text("Cart (${state.itemCount})") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        bottomBar = { InvoiceBottomBar(state, viewModel) },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (state.step) {
                InvoiceStep.CUSTOMER -> SelectCustomerStep(
                    state = state,
                    customers = customers,
                    query = customerQuery,
                    onQueryChange = viewModel::onCustomerQueryChange,
                    onSelect = viewModel::selectCustomer,
                )
                InvoiceStep.PRODUCTS -> SelectProductsStep(
                    state = state,
                    products = products,
                    query = productQuery,
                    onQueryChange = viewModel::onProductQueryChange,
                    onAdd = viewModel::addToCart,
                )
                InvoiceStep.CART -> CartStep(
                    state = state,
                    onQuantity = viewModel::setLineQuantity,
                    onUnitPrice = viewModel::setLineUnitPrice,
                    onRemove = viewModel::removeLine,
                )
                InvoiceStep.SUMMARY -> SummaryStep(
                    state = state,
                    onDiscountMode = viewModel::setDiscountMode,
                    onDiscountInput = viewModel::onDiscountInput,
                    onVatInput = viewModel::onVatInput,
                    onPaymentStatus = viewModel::setPaymentStatus,
                    onAmountPaid = viewModel::onAmountPaidInput,
                    onNotes = viewModel::onNotes,
                )
            }
        }
    }
}

private fun titleFor(step: InvoiceStep): String = when (step) {
    InvoiceStep.CUSTOMER -> "Select Customer"
    InvoiceStep.PRODUCTS -> "Add Products"
    InvoiceStep.CART -> "Invoice Cart"
    InvoiceStep.SUMMARY -> "Summary"
}

// ---------------- Shared header ----------------

@Composable
private fun CustomerHeader(customer: Customer, currencySymbol: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    customer.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    customer.ownerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (customer.outstandingBalance > 0.0) {
                Text(
                    "Due ${Money.format(customer.outstandingBalance, currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BalanceDue,
                )
            }
        }
    }
}

// ---------------- Step 1: Customer ----------------

@Composable
private fun SelectCustomerStep(
    state: InvoiceCreationState,
    customers: List<Customer>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Customer) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ValsiSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Search customer",
            modifier = Modifier.padding(16.dp),
        )
        if (customers.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ShoppingCart,
                title = "No customers",
                message = "Add a customer first from the Customers screen.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(customers, key = { it.id }) { customer ->
                    val selected = state.selectedCustomer?.id == customer.id
                    Card(
                        onClick = { onSelect(customer) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = if (selected) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            CardDefaults.cardColors()
                        },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(customer.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${customer.ownerName} · ${customer.phoneNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Step 2: Products ----------------

@Composable
private fun SelectProductsStep(
    state: InvoiceCreationState,
    products: List<Product>,
    query: String,
    onQueryChange: (String) -> Unit,
    onAdd: (Product, Int) -> Unit,
) {
    val pendingQty = remember { mutableStateMapOf<Long, Int>() }

    Column(Modifier.fillMaxSize()) {
        state.selectedCustomer?.let { CustomerHeader(it, state.currencySymbol) }
        ValsiSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Search products",
            modifier = Modifier.padding(16.dp),
        )
        if (products.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AddShoppingCart,
                title = "No products",
                message = "All products are hidden or none match your search.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(products, key = { it.id }) { product ->
                    val qty = pendingQty[product.id] ?: 1
                    ProductPickRow(
                        product = product,
                        quantity = qty,
                        currencySymbol = state.currencySymbol,
                        onDecrement = { pendingQty[product.id] = (qty - 1).coerceAtLeast(1) },
                        onIncrement = { pendingQty[product.id] = qty + 1 },
                        onAdd = {
                            onAdd(product, qty)
                            pendingQty[product.id] = 1
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductPickRow(
    product: Product,
    quantity: Int,
    currencySymbol: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(product.imageResId),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Money.format(product.currentPrice, currencySymbol)} / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(value = quantity, onDecrement = onDecrement, onIncrement = onIncrement)
                Button(onClick = onAdd) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to Cart")
                }
            }
        }
    }
}

// ---------------- Step 3: Cart ----------------

@Composable
private fun CartStep(
    state: InvoiceCreationState,
    onQuantity: (Long, Int) -> Unit,
    onUnitPrice: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        state.selectedCustomer?.let { CustomerHeader(it, state.currencySymbol) }
        if (state.cart.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ShoppingCart,
                title = "Your cart is empty",
                message = "Go back and add some products.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.cart, key = { it.productId }) { line ->
                    CartLineCard(
                        line = line,
                        currencySymbol = state.currencySymbol,
                        onQuantity = { onQuantity(line.productId, it) },
                        onUnitPrice = { onUnitPrice(line.productId, it) },
                        onRemove = { onRemove(line.productId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    currencySymbol: String,
    onQuantity: (Int) -> Unit,
    onUnitPrice: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    // Local text state so partial decimal input (e.g. "0.") isn't clobbered by the model value.
    var priceText by androidx.compose.runtime.remember(line.productId) {
        androidx.compose.runtime.mutableStateOf(trimmed(line.unitPrice))
    }
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Quantity: stepper + direct numeric entry
                QuantityStepper(
                    value = line.quantity,
                    onDecrement = { onQuantity(line.quantity - 1) },
                    onIncrement = { onQuantity(line.quantity + 1) },
                )
                OutlinedTextField(
                    value = line.quantity.toString(),
                    onValueChange = { it.toIntOrNull()?.let(onQuantity) },
                    label = { Text("Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(84.dp),
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        it.toDoubleOrNull()?.let(onUnitPrice)
                    },
                    label = { Text("Price") },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Line total: ${Money.format(line.lineTotal, currencySymbol)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

// ---------------- Step 4: Summary ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryStep(
    state: InvoiceCreationState,
    onDiscountMode: (DiscountMode) -> Unit,
    onDiscountInput: (String) -> Unit,
    onVatInput: (String) -> Unit,
    onPaymentStatus: (PaymentStatus) -> Unit,
    onAmountPaid: (String) -> Unit,
    onNotes: (String) -> Unit,
) {
    val symbol = state.currencySymbol
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { state.selectedCustomer?.let { CustomerHeader(it, symbol) } }
        item {
            Column(Modifier.padding(16.dp)) {
                TotalsRow("Subtotal", Money.format(state.subtotal, symbol))
                Spacer(Modifier.height(16.dp))

                Text("Discount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.discountMode == DiscountMode.FLAT,
                        onClick = { onDiscountMode(DiscountMode.FLAT) },
                        label = { Text("Amount ($symbol)") },
                    )
                    FilterChip(
                        selected = state.discountMode == DiscountMode.PERCENT,
                        onClick = { onDiscountMode(DiscountMode.PERCENT) },
                        label = { Text("Percent (%)") },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.discountInput,
                    onValueChange = onDiscountInput,
                    label = { Text(if (state.discountMode == DiscountMode.PERCENT) "Discount %" else "Discount $symbol") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "− ${Money.format(state.discountAmount, symbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(Modifier.height(16.dp))
                Text("VAT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.vatPercentInput,
                    onValueChange = onVatInput,
                    label = { Text("VAT %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "+ ${Money.format(state.vatAmount, symbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(Modifier.height(16.dp))
                TotalsRow("Grand Total", Money.format(state.grandTotal, symbol), emphasize = true)

                Spacer(Modifier.height(20.dp))
                Text("Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentStatus.entries.forEach { status ->
                        FilterChip(
                            selected = state.paymentStatus == status,
                            onClick = { onPaymentStatus(status) },
                            label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                if (state.paymentStatus == PaymentStatus.PARTIAL) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.amountPaidInput,
                        onValueChange = onAmountPaid,
                        label = { Text("Amount Paid") },
                        prefix = { Text(symbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotes,
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------- Bottom action bar ----------------

@Composable
private fun InvoiceBottomBar(state: InvoiceCreationState, viewModel: InvoiceCreationViewModel) {
    when (state.step) {
        InvoiceStep.CUSTOMER -> {
            if (state.selectedCustomer != null) {
                BottomBarContainer {
                    Button(
                        onClick = { viewModel.goToStep(InvoiceStep.PRODUCTS) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Continue") }
                }
            }
        }
        InvoiceStep.PRODUCTS -> {
            BottomBarContainer {
                OutlinedButton(
                    onClick = { viewModel.goToStep(InvoiceStep.CART) },
                    enabled = state.cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Review Cart (${state.itemCount} items)") }
            }
        }
        InvoiceStep.CART -> {
            BottomBarContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Money.format(state.subtotal, state.currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Button(
                        onClick = { viewModel.goToStep(InvoiceStep.SUMMARY) },
                        enabled = state.cart.isNotEmpty(),
                    ) { Text("Continue to Summary") }
                }
            }
        }
        InvoiceStep.SUMMARY -> {
            BottomBarContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Grand Total", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Money.format(state.grandTotal, state.currencySymbol),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Button(onClick = { viewModel.generateInvoice() }, enabled = !state.isSaving) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Invoice")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarContainer(content: @Composable () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) { content() }
    }
}

private fun trimmed(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
