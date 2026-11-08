package com.anish.quickbite.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.viewmodel.AdminOrdersState
import com.anish.quickbite.viewmodel.AdminOrdersViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    viewModel: AdminOrdersViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var isVerifyingCode by remember { mutableStateOf(false) }
    var recoveryErrorMessage by remember { mutableStateOf<String?>(null) }
    var recoverySuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is AdminOrdersState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is AdminOrdersState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadOrders() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
            is AdminOrdersState.OrdersLoaded -> {
                val orders = state.orders
                val activeCount = orders.count { it.status.uppercase() in listOf("PICKUP_ACTIVE", "ACTIVE", "PLACED", "PREPARING", "READY") }
                val completedCount = orders.count { it.status.uppercase() == "COMPLETED" }
                val recoveryCount = orders.count { it.status.uppercase() in listOf("RECOVERY", "EXPIRED") || !it.recoveryCode.isNullOrBlank() }

                val filterOptions = listOf("ALL", "ACTIVE", "PICKUP_ACTIVE", "COMPLETED", "RECOVERY")
                val filteredOrders = when (selectedFilter) {
                    "ALL" -> orders
                    "ACTIVE" -> orders.filter { it.status.uppercase() in listOf("ACTIVE", "PLACED") }
                    "PICKUP_ACTIVE" -> orders.filter { it.status.uppercase() in listOf("PICKUP_ACTIVE", "PREPARING", "READY") }
                    "COMPLETED" -> orders.filter { it.status.uppercase() == "COMPLETED" }
                    "RECOVERY" -> orders.filter { it.status.uppercase() in listOf("RECOVERY", "EXPIRED") || !it.recoveryCode.isNullOrBlank() }
                    else -> orders.filter { it.status.uppercase() == selectedFilter }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Summary Metric Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Active",
                            count = activeCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Completed",
                            count = completedCount.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Recovery",
                            count = recoveryCount.toString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Filter Chips Row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterOptions) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = filter.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${selectedFilter.replace("_", " ").lowercase()} orders found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredOrders) { order ->
                                OrderCard(
                                    order = order,
                                    onUpdateStatus = { viewModel.updateOrderStatus(order.id, order.status) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button: Verify Recovery Code
        ExtendedFloatingActionButton(
            onClick = {
                recoveryCodeInput = ""
                recoveryErrorMessage = null
                recoverySuccessMessage = null
                showRecoveryDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            text = {
                Text(
                    text = "Verify Recovery",
                    fontWeight = FontWeight.Bold
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        // Verify Recovery Code Dialog
        if (showRecoveryDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isVerifyingCode) showRecoveryDialog = false
                },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Verify Student Recovery Code", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Enter the 6-character recovery code displayed on the student's expired pickup pass. Eligible orders can be reopened once for a new 10-minute window.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = recoveryCodeInput,
                            onValueChange = { recoveryCodeInput = it.uppercase() },
                            label = { Text("Recovery Code") },
                            placeholder = { Text("e.g. QB-8821") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isVerifyingCode
                        )

                        if (recoveryErrorMessage != null) {
                            Text(
                                text = recoveryErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (recoverySuccessMessage != null) {
                            Text(
                                text = recoverySuccessMessage!!,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isVerifyingCode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying with server...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isVerifyingCode && recoveryCodeInput.isNotBlank(),
                        onClick = {
                            isVerifyingCode = true
                            recoveryErrorMessage = null
                            recoverySuccessMessage = null
                            viewModel.verifyAndReopenOrder(
                                recoveryCode = recoveryCodeInput,
                                onSuccess = {
                                    isVerifyingCode = false
                                    recoverySuccessMessage = "Order verified and reopened for 10 minutes!"
                                    recoveryCodeInput = ""
                                },
                                onError = { err ->
                                    isVerifyingCode = false
                                    recoveryErrorMessage = err
                                }
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Reopen", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isVerifyingCode,
                        onClick = { showRecoveryDialog = false }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onUpdateStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Order ID, Canteen & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val shortId = order.id.take(8).uppercase()
                    Text(
                        text = "Order #$shortId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    order.canteen?.let { canteen ->
                        Text(
                            text = canteen.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    order.createdAt?.let { dateString ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatDateTime(dateString),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Status Badge Pill
                val statusColor = getStatusColor(order.status)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = order.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expiry or Recovery Notice if applicable
            if (!order.recoveryCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recovery Code: ${order.recoveryCode} (Reopens: ${order.reopenCount}/1)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Items List
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val itemName = item.menuItem?.name ?: "Food Item"
                    Text(
                        text = "${item.quantity} × $itemName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹${item.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "₹${order.totalAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Status Progress Action Button
            val actionText = getActionTextForStatus(order.status)
            if (actionText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onUpdateStatus,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

fun formatDateTime(isoString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString)

        val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault()
        if (date != null) formatter.format(date) else isoString
    } catch (e: Exception) {
        isoString
    }
}

@Composable
fun getStatusColor(status: String) = when (status.uppercase()) {
    "ACTIVE" -> MaterialTheme.colorScheme.primary
    "PICKUP_ACTIVE" -> MaterialTheme.colorScheme.secondary
    "PLACED" -> MaterialTheme.colorScheme.error
    "PREPARING" -> MaterialTheme.colorScheme.primary
    "READY" -> MaterialTheme.colorScheme.secondary
    "RECOVERY", "EXPIRED" -> MaterialTheme.colorScheme.error
    "COMPLETED" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}

fun getActionTextForStatus(status: String): String? {
    return when (status.uppercase()) {
        "PICKUP_ACTIVE" -> "Mark Collected / Completed"
        "PLACED" -> "Start Preparing"
        "PREPARING" -> "Mark Ready"
        "READY" -> "Mark Completed"
        else -> null
    }
}
