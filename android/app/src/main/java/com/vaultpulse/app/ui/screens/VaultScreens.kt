package com.vaultpulse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.vaultpulse.app.domain.BankSyncManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultpulse.app.data.local.AccountKind
import com.vaultpulse.app.data.local.AccountProfileEntity
import com.vaultpulse.app.data.local.TransactionEntity
import com.vaultpulse.app.data.local.TransactionType
import com.vaultpulse.app.ui.theme.ObsidianColors
import com.vaultpulse.app.ui.viewmodel.VaultUiState
import com.vaultpulse.app.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---- Relative date formatter ----
private fun relativeTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
            "Today $timeStr"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
            "Yesterday $timeStr"
        else ->
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(ts))
    }
}

// ---- Date key for grouping ----
private fun dayKey(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(ts))
    }
}

enum class VaultTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.AccountBalanceWallet),
    LEDGER("Ledger", Icons.Rounded.ReceiptLong),
    ANALYTICS("Analytics", Icons.Rounded.BarChart),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun VaultPulseRoot(viewModel: VaultViewModel) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(VaultTab.HOME) }

    // Modal dialogs and sheets state
    var showAddTxSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountProfileEntity?>(null) }
    var showCreateAccountSheet by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountProfileEntity?>(null) }

    var reassignTarget by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Sync checkpoint alert
    state.syncAlertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSyncAlert() },
            containerColor = ObsidianColors.SurfaceContainerHigh,
            title = { Text("Bank Checkpoint", color = ObsidianColors.OnSurface, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = ObsidianColors.OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSyncAlert() }) {
                    Text("OK", color = ObsidianColors.PrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Account Delete Confirmation Dialog
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = ObsidianColors.SurfaceContainerHigh,
            title = { Text("Delete Account?", color = ObsidianColors.OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${acc.name}\"? All transactions associated with this account will also be permanently deleted.",
                    color = ObsidianColors.OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(acc.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.ExpenseRose)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = ObsidianColors.OnSurfaceVariant)
                }
            }
        )
    }

    // Transaction Delete Confirmation Dialog
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            containerColor = ObsidianColors.SurfaceContainerHigh,
            title = { Text("Delete Transaction?", color = ObsidianColors.OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete \"${tx.merchant}\" (₹%,.2f)? Its balance impact will be automatically reversed from your account balance.".format(tx.amount),
                    color = ObsidianColors.OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.ExpenseRose)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Cancel", color = ObsidianColors.OnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        containerColor = ObsidianColors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            VaultPulseHeader(activeTab = activeTab)
        },
        floatingActionButton = {
            if (activeTab == VaultTab.HOME || activeTab == VaultTab.LEDGER) {
                FloatingActionButton(
                    onClick = {
                        if (state.accounts.isEmpty()) {
                            showCreateAccountSheet = true
                        } else {
                            showAddTxSheet = true
                        }
                    },
                    containerColor = ObsidianColors.Tertiary,
                    contentColor = ObsidianColors.Canvas,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add", modifier = Modifier.size(22.dp))
                        Text(
                            text = if (state.accounts.isEmpty()) "Add Account" else "Add Transaction",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianColors.Surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                VaultTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianColors.Secondary,
                            selectedTextColor = ObsidianColors.Secondary,
                            indicatorColor = ObsidianColors.SecondaryContainer.copy(alpha = 0.22f),
                            unselectedIconColor = ObsidianColors.OnSurfaceVariant.copy(alpha = 0.65f),
                            unselectedTextColor = ObsidianColors.OnSurfaceVariant.copy(alpha = 0.65f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (activeTab) {
                VaultTab.HOME -> HomeScreen(
                    state = state,
                    onCallBank = { viewModel.callBank(it) },
                    onSyncSms = { viewModel.syncBankSms(it) },
                    onAddNewAccount = { showCreateAccountSheet = true },
                    onEditAccount = { accountToEdit = it },
                    onDeleteAccount = { accountToDelete = it },
                    onSetPrimaryUpi = { viewModel.setPrimaryUpiProfile(it.id) }
                )
                VaultTab.LEDGER -> LedgerScreen(
                    state = state,
                    onTapAccountBadge = { reassignTarget = it },
                    onEditTx = { txToEdit = it },
                    onDeleteTx = { txToDelete = it }
                )
                VaultTab.ANALYTICS -> AnalyticsScreen(
                    state = state
                )
                VaultTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onAddNewAccount = { showCreateAccountSheet = true },
                    onEditAccount = { accountToEdit = it },
                    onDeleteAccount = { accountToDelete = it },
                    onSelectPrimary = { viewModel.setPrimaryUpiProfile(it) },
                    onAddRule = { kw, cat -> viewModel.saveKeywordRule(null, kw, cat) },
                    onDeleteRule = { viewModel.deleteKeywordRule(it) }
                )
            }
        }
    }

    // Modal Sheet: Add Transaction
    if (showAddTxSheet) {
        AddTransactionSheet(
            accounts = state.accounts,
            onDismiss = { showAddTxSheet = false },
            onSave = { type, amount, merchant, cat, fromId, toId, tax ->
                viewModel.addTransaction(type, amount, merchant, cat, fromId, toId, tax)
                showAddTxSheet = false
            }
        )
    }

    // Modal Sheet: Edit Transaction
    txToEdit?.let { tx ->
        EditTransactionSheet(
            transaction = tx,
            accounts = state.accounts,
            onDismiss = { txToEdit = null },
            onSave = { newType, newAmt, newMerchant, newCat, newAccId, newToId, tax ->
                viewModel.updateTransaction(tx, newType, newAmt, newMerchant, newCat, newAccId, newToId, tax)
                txToEdit = null
            }
        )
    }

    // Modal Sheet: Reassign Transaction Account
    reassignTarget?.let { tx ->
        ReassignAccountSheet(
            transaction = tx,
            accounts = state.accounts,
            onDismiss = { reassignTarget = null },
            onConfirm = { newAccId, autoFuture ->
                viewModel.reassignTransactionAccount(tx, newAccId, autoFuture)
                reassignTarget = null
            }
        )
    }

    // Modal Sheet: Create or Edit Account
    if (showCreateAccountSheet || accountToEdit != null) {
        EditAccountSheet(
            existing = accountToEdit,
            onDismiss = {
                showCreateAccountSheet = false
                accountToEdit = null
            },
            onSave = { id, name, shortName, kind, digits, tag, balance, isPrimary, upi, sender, dial ->
                viewModel.saveAccount(id, name, shortName, kind, digits, tag, balance, isPrimary, upi, sender, dial)
                showCreateAccountSheet = false
                accountToEdit = null
            }
        )
    }
}

@Composable
fun VaultPulseHeader(activeTab: VaultTab) {
    Surface(
        color = ObsidianColors.Surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, ObsidianColors.HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianColors.PrimaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = "VaultPulse Logo",
                        tint = ObsidianColors.PrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "VaultPulse",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.OnSurface,
                        letterSpacing = (-0.02).sp
                    )
                    Text(
                        text = activeTab.label,
                        fontSize = 11.sp,
                        color = ObsidianColors.OnSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ObsidianColors.SurfaceContainer)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = ObsidianColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .clip(CircleShape)
                            .background(ObsidianColors.ExpenseRose)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ObsidianColors.SurfaceContainerHighest)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = "Profile",
                        tint = ObsidianColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN (Profiles & Liquidity)
// ==========================================

@Composable
fun HomeScreen(
    state: VaultUiState,
    onCallBank: (AccountProfileEntity) -> Unit,
    onSyncSms: (AccountProfileEntity) -> Unit,
    onAddNewAccount: () -> Unit,
    onEditAccount: (AccountProfileEntity) -> Unit,
    onDeleteAccount: (AccountProfileEntity) -> Unit,
    onSetPrimaryUpi: (AccountProfileEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Liquidity Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                ObsidianColors.SurfaceContainerHigh,
                                ObsidianColors.SurfaceContainer,
                                ObsidianColors.SurfaceContainerLow
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Row: Title + SMS Sync status pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = ObsidianColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "TOTAL NET LIQUIDITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ObsidianColors.OnSurfaceVariant,
                                letterSpacing = 0.05.sp
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = ObsidianColors.SurfaceContainerHighest.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianColors.Tertiary)
                                )
                                Text(
                                    text = if (state.accounts.isNotEmpty()) "Offline Encrypted Vault" else "Awaiting Setup",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ObsidianColors.SecondaryFixed
                                )
                            }
                        }
                    }

                    // Main Balance Display
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "₹%,.2f".format(state.netLiquidity),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianColors.OnSurface,
                            letterSpacing = (-0.03).sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.totalIncome > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = ObsidianColors.TertiaryContainer.copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.TrendingUp,
                                            contentDescription = null,
                                            tint = ObsidianColors.Tertiary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${state.savedPercent}% saved",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ObsidianColors.Tertiary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${state.accounts.size} active accounts configured",
                                fontSize = 11.sp,
                                color = ObsidianColors.OnSurfaceVariant
                            )
                        }
                    }

                    // Quick Telemetry Mini-Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianColors.SurfaceContainerHighest.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianColors.Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    val bankCount = state.accounts.count { it.accountKind == AccountKind.BANK }
                                    Text("$bankCount Bank Accounts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                                    val names = state.accounts.filter { it.accountKind == AccountKind.BANK }.take(3).joinToString { it.shortName }
                                    Text(names.ifBlank { "None added" }, fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianColors.SurfaceContainerHighest.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianColors.Tertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = ObsidianColors.Tertiary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    val cashBal = state.accounts.filter { it.accountKind == AccountKind.CASH }.sumOf { it.balance }
                                    Text("Cash Wallets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                                    Text("₹%,.0f in cash".format(cashBal), fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Your Accounts & Profiles + Add Account Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your Accounts & Profiles",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.OnSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = ObsidianColors.SurfaceContainerHighest
                    ) {
                        Text(
                            text = "${state.accounts.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianColors.OnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = ObsidianColors.PrimaryContainer.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { onAddNewAccount() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(15.dp))
                        Text("Add Account", fontSize = 12.sp, color = ObsidianColors.PrimaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Clean Onboarding Empty State if 0 Accounts
        if (state.accounts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ObsidianColors.SurfaceContainerHigh,
                    border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(ObsidianColors.PrimaryContainer.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(28.dp))
                        }

                        Text("Welcome to VaultPulse", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Your personal finance ledger operates 100% on-device with zero internet permission. Add your bank accounts or physical cash wallet to start tracking balances and transactions.",
                            fontSize = 13.sp,
                            color = ObsidianColors.OnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = onAddNewAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.Tertiary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = ObsidianColors.Canvas, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Set Up Your First Account", color = ObsidianColors.Canvas, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Render actual accounts
            items(state.accounts, key = { it.id }) { acc ->
                AccountProfileCard(
                    account = acc,
                    onCallBank = { onCallBank(acc) },
                    onSyncSms = { onSyncSms(acc) },
                    onEdit = { onEditAccount(acc) },
                    onDelete = { onDeleteAccount(acc) },
                    onSetPrimary = { onSetPrimaryUpi(acc) }
                )
            }
        }
    }
}

@Composable
fun AccountProfileCard(
    account: AccountProfileEntity,
    onCallBank: () -> Unit,
    onSyncSms: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianColors.SurfaceContainerHigh,
        border = BorderStroke(1.dp, ObsidianColors.HairlineBorder),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Icon + Name + Tag + Overflow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, bgTint, fgTint) = when (account.accountKind) {
                        AccountKind.BANK -> if (account.isPrimaryUpi) {
                            Triple(Icons.Rounded.AccountBalance, ObsidianColors.SecondaryContainer.copy(alpha = 0.2f), ObsidianColors.Secondary)
                        } else {
                            Triple(Icons.Rounded.AccountBalance, ObsidianColors.PrimaryContainer.copy(alpha = 0.2f), ObsidianColors.Primary)
                        }
                        AccountKind.CASH -> Triple(Icons.Rounded.Wallet, ObsidianColors.Tertiary.copy(alpha = 0.15f), ObsidianColors.Tertiary)
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = fgTint, modifier = Modifier.size(24.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = account.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianColors.OnSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (account.maskedDigits.isNotBlank()) {
                                Text(
                                    text = "•••• ${account.maskedDigits}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ObsidianColors.OnSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianColors.OutlineVariant)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (account.isPrimaryUpi) ObsidianColors.SecondaryContainer.copy(alpha = 0.2f) else ObsidianColors.SurfaceContainerHighest
                            ) {
                                Text(
                                    text = if (account.isPrimaryUpi) "Primary UPI" else account.tagLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (account.isPrimaryUpi) ObsidianColors.Secondary else ObsidianColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = ObsidianColors.OnSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = ObsidianColors.SurfaceContainerHighest
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Account", color = ObsidianColors.OnSurface) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        if (account.accountKind == AccountKind.BANK && !account.isPrimaryUpi) {
                            DropdownMenuItem(
                                text = { Text("Set as Primary UPI", color = ObsidianColors.OnSurface) },
                                leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onSetPrimary()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete Account", color = ObsidianColors.ExpenseRose) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = ObsidianColors.ExpenseRose, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Available Balance + Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (account.accountKind == AccountKind.CASH) "CASH IN HAND" else "AVAILABLE BALANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianColors.OnSurfaceVariant,
                        letterSpacing = 0.04.sp
                    )
                    Text(
                        text = "₹%,.2f".format(account.balance),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.OnSurface,
                        letterSpacing = (-0.02).sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (account.accountKind == AccountKind.CASH) ObsidianColors.Outline else ObsidianColors.Tertiary)
                    )
                    Text(
                        text = if (account.accountKind == AccountKind.CASH) "Manual Tracking" else "SMS Synced",
                        fontSize = 11.sp,
                        color = ObsidianColors.OnSurfaceVariant
                    )
                }
            }

            // 2-Step Compact Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (account.accountKind == AccountKind.BANK) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onCallBank() },
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianColors.SurfaceContainerHighest
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Call Bank", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onSyncSms() },
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianColors.SurfaceContainerHighest
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, tint = ObsidianColors.Primary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync SMS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onEdit() },
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianColors.SurfaceContainerHighest
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Savings, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reconcile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. LEDGER SCREEN (Chronological Activity Feed)
// ==========================================

@Composable
fun LedgerScreen(
    state: VaultUiState,
    onTapAccountBadge: (TransactionEntity) -> Unit,
    onEditTx: (TransactionEntity) -> Unit,
    onDeleteTx: (TransactionEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    // Use derivedStateOf to avoid recomputing on every recomposition
    val filteredList by remember(state.transactions, selectedFilter, searchQuery) {
        derivedStateOf {
            state.transactions.filter { tx ->
                val matchFilter = selectedFilter == null || tx.type == selectedFilter
                val matchSearch = searchQuery.isBlank() ||
                    tx.merchant.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true)
                matchFilter && matchSearch
            }
        }
    }

    // Group by day for section headers
    val groupedTxs by remember(filteredList) {
        derivedStateOf {
            filteredList.groupBy { dayKey(it.timestamp) }
                .entries.toList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sticky Search Bar
        item {
            Surface(
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerHighest,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = ObsidianColors.Outline, modifier = Modifier.size(20.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search transactions, merchants...", color = ObsidianColors.Outline.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = ObsidianColors.OnSurface, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = ObsidianColors.Outline, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerFilterPill(
                    label = "All",
                    icon = if (selectedFilter == null) Icons.Rounded.Check else null,
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                LedgerFilterPill(
                    label = "Debit",
                    icon = Icons.Rounded.ArrowDownward,
                    iconTint = ObsidianColors.ExpenseRose,
                    selected = selectedFilter == TransactionType.DEBIT,
                    onClick = { selectedFilter = TransactionType.DEBIT }
                )
                LedgerFilterPill(
                    label = "Credit",
                    icon = Icons.Rounded.ArrowUpward,
                    iconTint = ObsidianColors.Tertiary,
                    selected = selectedFilter == TransactionType.CREDIT,
                    onClick = { selectedFilter = TransactionType.CREDIT }
                )
                LedgerFilterPill(
                    label = "Transfer",
                    icon = Icons.Rounded.SwapHoriz,
                    iconTint = ObsidianColors.Secondary,
                    selected = selectedFilter == TransactionType.SELF_TRANSFER,
                    onClick = { selectedFilter = TransactionType.SELF_TRANSFER }
                )
            }
        }

        // Monthly Summary Bar
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianColors.SurfaceContainerHigh,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.ExpenseRose))
                            Text("Spent", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                        }
                        Text("₹%,.0f".format(state.totalSpent), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(ObsidianColors.SurfaceContainerHighest))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ObsidianColors.Tertiary))
                            Text("Income", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                        }
                        Text("+₹%,.0f".format(state.totalIncome), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Tertiary)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(ObsidianColors.SurfaceContainerHighest))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(13.dp))
                            Text("Saved", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                        }
                        Text("${state.savedPercent}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Secondary)
                    }
                }
            }
        }

        // Transactions Feed — grouped by date
        if (groupedTxs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianColors.SurfaceContainer,
                    border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.accounts.isEmpty()) "Add an account first to start logging transactions." else "No transactions recorded yet. Tap + Add Transaction below.",
                            color = ObsidianColors.OnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            groupedTxs.forEach { (dayLabel, txsForDay) ->
                // Day header
                item(key = "header_$dayLabel") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianColors.OnSurfaceVariant,
                            letterSpacing = 0.03.sp
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(ObsidianColors.SurfaceContainerHighest))
                    }
                }
                // Transactions for this day
                item(key = "group_$dayLabel") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianColors.SurfaceContainer,
                        border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
                    ) {
                        Column {
                            txsForDay.forEachIndexed { index, tx ->
                                LedgerItemRow(
                                    transaction = tx,
                                    accounts = state.accounts,
                                    onReassign = { onTapAccountBadge(tx) },
                                    onEdit = { onEditTx(tx) },
                                    onDelete = { onDeleteTx(tx) }
                                )
                                if (index < txsForDay.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(ObsidianColors.SurfaceContainerHighest.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerFilterPill(
    label: String,
    icon: ImageVector? = null,
    iconTint: Color = ObsidianColors.OnSurface,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) ObsidianColors.PrimaryContainer else ObsidianColors.SurfaceContainerHigh,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected) ObsidianColors.OnPrimary else iconTint, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) ObsidianColors.OnPrimary else ObsidianColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
fun LedgerItemRow(
    transaction: TransactionEntity,
    accounts: List<AccountProfileEntity>,
    onReassign: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val acc = accounts.firstOrNull { it.id == transaction.accountId }
    val accLabel = if (transaction.type == TransactionType.SELF_TRANSFER && transaction.toAccountId != null) {
        val destAcc = accounts.firstOrNull { it.id == transaction.toAccountId }
        "${acc?.shortName ?: "Acc"} ➔ ${destAcc?.shortName ?: "Acc"}"
    } else {
        "${acc?.shortName ?: "Acc"} •••• ${acc?.maskedDigits.orEmpty().ifBlank { "Cash" }}"
    }

    val (catIcon, catBg, catFg) = when (transaction.category.lowercase()) {
        "food & dining", "food", "dining" -> Triple(Icons.Rounded.Restaurant, Color(0xFFF97316).copy(alpha = 0.18f), Color(0xFFFB923C))
        "income", "salary" -> Triple(Icons.Rounded.Payments, ObsidianColors.TertiaryContainer.copy(alpha = 0.2f), ObsidianColors.Tertiary)
        "fuel & transport", "transportation" -> Triple(Icons.Rounded.LocalGasStation, Color(0xFF38BDF8).copy(alpha = 0.18f), Color(0xFF38BDF8))
        "entertainment" -> Triple(Icons.Rounded.Movie, Color(0xFFA855F7).copy(alpha = 0.18f), Color(0xFFC084FC))
        "groceries", "grocery" -> Triple(Icons.Rounded.LocalGroceryStore, Color(0xFFF59E0B).copy(alpha = 0.18f), Color(0xFFFBBF24))
        else -> Triple(Icons.Rounded.SwapHoriz, ObsidianColors.PrimaryContainer.copy(alpha = 0.2f), ObsidianColors.Primary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { menuExpanded = true }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(catBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(catIcon, contentDescription = null, tint = catFg, modifier = Modifier.size(22.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.merchant,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianColors.OnSurface
                )
                val (amtColor, prefix) = when (transaction.type) {
                    TransactionType.CREDIT -> Pair(ObsidianColors.Tertiary, "+")
                    TransactionType.DEBIT -> Pair(ObsidianColors.ExpenseRose, "-")
                    TransactionType.SELF_TRANSFER -> Pair(ObsidianColors.OnSurfaceVariant, "")
                }
                Text(
                    text = "$prefix₹%,.2f".format(transaction.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amtColor
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianColors.SurfaceContainerHighest,
                    modifier = Modifier.clickable { onReassign() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(accLabel, fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(13.dp))
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = ObsidianColors.SecondaryContainer.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (transaction.source == "SMS_AUTO") "SMS Auto" else "Manual",
                        fontSize = 10.sp,
                        color = ObsidianColors.Secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            val dateStr = relativeTimestamp(transaction.timestamp)
            Text(
                text = "$dateStr • ${transaction.category}",
                fontSize = 11.sp,
                color = ObsidianColors.Outline
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = ObsidianColors.Outline, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = ObsidianColors.SurfaceContainerHighest
            ) {
                DropdownMenuItem(
                    text = { Text("Reassign Account", color = ObsidianColors.OnSurface) },
                    leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        menuExpanded = false
                        onReassign()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Transaction", color = ObsidianColors.OnSurface) },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = ObsidianColors.ExpenseRose) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = ObsidianColors.ExpenseRose, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

// ==========================================
// 3. ANALYTICS SCREEN (Spending Trends & Insights)
// ==========================================

@Composable
fun AnalyticsScreen(state: VaultUiState) {
    var selectedInterval by remember { mutableStateOf("Month") }

    // Dynamic current month/year
    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Real daily spend aggregation for bar chart (last 14 days)
    val dailySpend = remember(state.transactions) {
        val cal = Calendar.getInstance()
        (0 until 14).map { daysAgo ->
            cal.time
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dayTotal = state.transactions.filter {
                it.type == TransactionType.DEBIT &&
                Calendar.getInstance().apply { timeInMillis = it.timestamp }.let { txCal ->
                    txCal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                    txCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                }
            }.sumOf { it.amount }.toFloat()
            dayTotal
        }.reversed()
    }
    val maxDailySpend = dailySpend.maxOrNull()?.takeIf { it > 0f } ?: 1f

    // Category breakdown
    val categoryBreakdown = remember(state.transactions) {
        state.transactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category.ifBlank { "General" } }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Interval Selector & Date Navigator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(currentMonthYear, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                }
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = ObsidianColors.OnSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Week", "Month", "Quarter", "Year").forEach { interval ->
                        val isSel = selectedInterval == interval
                        Surface(
                            shape = CircleShape,
                            color = if (isSel) ObsidianColors.PrimaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedInterval = interval }
                        ) {
                            Text(
                                text = interval,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) ObsidianColors.OnPrimary else ObsidianColors.OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Hero Monthly Spend Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainerHigh,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL SPEND", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurfaceVariant, letterSpacing = 0.05.sp)
                        if (state.totalSpent > 0) {
                            Surface(shape = CircleShape, color = ObsidianColors.TertiaryContainer.copy(alpha = 0.2f)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = ObsidianColors.Tertiary, modifier = Modifier.size(13.dp))
                                    Text("Active Tracker", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Tertiary)
                                }
                            }
                        }
                    }

                    Column {
                        Text("₹%,.2f".format(state.totalSpent), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                        Text("Total income recorded: ₹%,.2f".format(state.totalIncome), fontSize = 12.sp, color = ObsidianColors.OnSurfaceVariant)
                    }

                    // Budget Progress Bar
                    val budget = if (state.totalSpent > 0) state.totalSpent * 1.4 else 50000.0
                    val pacingPct = if (budget > 0) (state.totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pacing (%d%%)".format((pacingPct * 100).toInt()), fontSize = 11.sp, color = ObsidianColors.Tertiary, fontWeight = FontWeight.SemiBold)
                            Text("₹%,.0f left".format((budget - state.totalSpent).coerceAtLeast(0.0)), fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                        }
                        LinearProgressIndicator(
                            progress = { pacingPct },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = ObsidianColors.Secondary,
                            trackColor = ObsidianColors.SurfaceContainerLowest,
                        )
                    }

                    // Metric Boxes Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianColors.SurfaceContainer
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Speed, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Daily Average", fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                                    val dailyAvg = if (state.totalSpent > 0) state.totalSpent / 30.0 else 0.0
                                    Text("₹%,.0f/day".format(dailyAvg), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianColors.SurfaceContainer
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = ObsidianColors.Primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Savings Rate", fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                                    Text("${state.savedPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Real Spending Trends Bar Chart (last 14 days)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Spending Trends", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                            Text("Daily spend — last 14 days", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                        }
                        Surface(shape = CircleShape, color = ObsidianColors.SurfaceContainerHigh) {
                            Text("Current Cycle", fontSize = 10.sp, color = ObsidianColors.Secondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    if (state.transactions.none { it.type == TransactionType.DEBIT }) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("Add transactions to see spending trends", fontSize = 12.sp, color = ObsidianColors.OnSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 14.dp.toPx()
                                val spacing = if (dailySpend.size > 1)
                                    (size.width - (dailySpend.size * barWidth)) / (dailySpend.size - 1)
                                else 0f

                                val avgY = size.height * 0.5f
                                drawLine(
                                    color = ObsidianColors.OutlineVariant,
                                    start = Offset(0f, avgY),
                                    end = Offset(size.width, avgY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                )

                                dailySpend.forEachIndexed { i, spend ->
                                    val x = i * (barWidth + spacing)
                                    val barH = (spend / maxDailySpend) * (size.height * 0.85f)
                                    val y = size.height - barH
                                    val isToday = i == dailySpend.size - 1
                                    drawRoundRect(
                                        color = if (isToday) ObsidianColors.Secondary else ObsidianColors.SurfaceContainerHighest,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barH.coerceAtLeast(4f)),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Breakdown
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianColors.SurfaceContainer,
                    border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Spending by Category", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                        val catColors = listOf(
                            Color(0xFFFB923C), ObsidianColors.Secondary, ObsidianColors.Tertiary,
                            ObsidianColors.PrimaryContainer, ObsidianColors.ExpenseRose
                        )
                        categoryBreakdown.forEachIndexed { i, (cat, total) ->
                            val barFraction = (total / (categoryBreakdown.firstOrNull()?.value ?: 1.0)).toFloat().coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat, fontSize = 12.sp, color = ObsidianColors.OnSurface, fontWeight = FontWeight.Medium)
                                    Text("₹%,.0f".format(total), fontSize = 12.sp, color = catColors.getOrElse(i) { ObsidianColors.Primary }, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { barFraction },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = catColors.getOrElse(i) { ObsidianColors.Primary },
                                    trackColor = ObsidianColors.SurfaceContainerHighest
                                )
                            }
                        }
                    }
                }
            }
        }

        // Privacy Telemetry Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianColors.PrimaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("VAULTPULSE PRIVACY TELEMETRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.PrimaryContainer, letterSpacing = 0.05.sp)
                        Text(
                            text = "All database operations and balance snapshots are encrypted via AES-256 SQLCipher on this device. No telemetry or transaction logs ever leave your phone.",
                            fontSize = 12.sp,
                            color = ObsidianColors.OnSurface,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SETTINGS SCREEN (Configuration Suite)
// ==========================================

@Composable
fun SettingsScreen(
    state: VaultUiState,
    onAddNewAccount: () -> Unit,
    onEditAccount: (AccountProfileEntity) -> Unit,
    onDeleteAccount: (AccountProfileEntity) -> Unit,
    onSelectPrimary: (String) -> Unit,
    onAddRule: (String, String) -> Unit,
    onDeleteRule: (String) -> Unit
) {
    var kwDraft by remember { mutableStateOf("") }
    var catDraft by remember { mutableStateOf("Food & Dining") }
    var catPickerOpen by remember { mutableStateOf(false) }
    var bankDropdownOpen by remember { mutableStateOf(false) }
    var autoParseSms by remember { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current

    val primaryBank = state.accounts.firstOrNull { it.isPrimaryUpi }
        ?: state.accounts.firstOrNull { it.accountKind == AccountKind.BANK }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("CONFIGURATION SUITE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.PrimaryContainer, letterSpacing = 0.05.sp)
                Text("Settings & Automation", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                Text("Account profiles, routing & offline security", fontSize = 13.sp, color = ObsidianColors.OnSurfaceVariant)
            }
        }

        // Section 1: Default UPI & Routing
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(ObsidianColors.PrimaryContainer.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Default UPI & Routing", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                Text("Prioritize account for auto-payments", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                            }
                        }
                        Surface(shape = CircleShape, color = ObsidianColors.SecondaryContainer.copy(alpha = 0.2f)) {
                            Text("Active", fontSize = 10.sp, color = ObsidianColors.Secondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }

                    // Active Bank Card
                    if (primaryBank != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianColors.SurfaceContainerHigh
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(ObsidianColors.SurfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = ObsidianColors.Primary, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(primaryBank.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                            Text("Primary Account •••• ${primaryBank.maskedDigits}", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = ObsidianColors.SurfaceContainerHighest,
                                        modifier = Modifier.clickable { bankDropdownOpen = !bankDropdownOpen }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Switch", fontSize = 12.sp, color = ObsidianColors.PrimaryContainer, fontWeight = FontWeight.SemiBold)
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                // Bank Switcher Options
                                AnimatedVisibility(visible = bankDropdownOpen) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                                        state.accounts.filter { it.accountKind == AccountKind.BANK }.forEach { bank ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (bank.id == primaryBank.id) ObsidianColors.SurfaceContainerHighest else ObsidianColors.SurfaceContainer,
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    onSelectPrimary(bank.id)
                                                    bankDropdownOpen = false
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${bank.name} (•••• ${bank.maskedDigits})", fontSize = 12.sp, color = ObsidianColors.OnSurface)
                                                    if (bank.id == primaryBank.id) {
                                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddNewAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PrimaryContainer),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = ObsidianColors.Canvas)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Primary Bank Account", color = ObsidianColors.Canvas, fontWeight = FontWeight.Bold)
                        }
                    }

                    // UPI Alias Row
                    primaryBank?.upiAlias?.let { alias ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianColors.SurfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ObsidianColors.SurfaceContainerHighest), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.AlternateEmail, contentDescription = null, tint = ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                    Column {
                                        Text("UPI ID Alias", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                                        Text(alias, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                                    }
                                }
                                IconButton(onClick = { clipboard.setText(AnnotatedString(alias)) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Auto Parse SMS Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianColors.SurfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ObsidianColors.TertiaryContainer.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Sms, contentDescription = null, tint = ObsidianColors.Tertiary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Auto-parse UPI Payment SMS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                    Text("Extract amount & merchants on arrival", fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = autoParseSms,
                                onCheckedChange = { autoParseSms = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianColors.Canvas,
                                    checkedTrackColor = ObsidianColors.Tertiary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Manage Accounts
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Manage Account Profiles", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                        Button(
                            onClick = onAddNewAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.Secondary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = ObsidianColors.Canvas, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Account", color = ObsidianColors.Canvas, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    state.accounts.forEach { acc ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianColors.SurfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                        if (acc.isPrimaryUpi) {
                                            Surface(shape = CircleShape, color = ObsidianColors.SecondaryContainer.copy(alpha = 0.2f)) {
                                                Text("Primary", fontSize = 9.sp, color = ObsidianColors.Secondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    Text("•••• ${acc.maskedDigits.ifBlank { "Cash" }} · ₹%,.2f".format(acc.balance), fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onEditAccount(acc) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = ObsidianColors.PrimaryContainer, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteAccount(acc) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = ObsidianColors.ExpenseRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Keyword Rules
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Auto-Categorization Rules", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)

                    state.keywordRules.forEach { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.keywordsCsv, fontSize = 13.sp, color = ObsidianColors.OnSurface)
                                Text("Maps to ${rule.category}", fontSize = 11.sp, color = ObsidianColors.Secondary)
                            }
                            IconButton(onClick = { onDeleteRule(rule.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Delete", tint = ObsidianColors.ExpenseRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = kwDraft,
                        onValueChange = { kwDraft = it },
                        label = { Text("Keywords (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                            unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                        )
                    )

                    // Category picker for rule
                    Text("Maps to category: $catDraft", fontSize = 11.sp, color = ObsidianColors.Secondary, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Food & Dining", "Fuel & Transport", "Groceries", "Shopping & Retail", "Entertainment", "Income", "Medical", "Utilities", "General").forEach { cat ->
                            val isSel = catDraft == cat
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) ObsidianColors.SecondaryContainer.copy(alpha = 0.25f) else ObsidianColors.SurfaceContainerHighest,
                                border = if (isSel) BorderStroke(1.dp, ObsidianColors.Secondary) else null,
                                modifier = Modifier.clickable { catDraft = cat }
                            ) {
                                Text(cat, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) ObsidianColors.Secondary else ObsidianColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (kwDraft.isNotBlank()) {
                                onAddRule(kwDraft, catDraft)
                                kwDraft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PrimaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add Rule", color = ObsidianColors.Canvas, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ACCOUNT CREATE / EDIT SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountSheet(
    existing: AccountProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        shortName: String,
        kind: AccountKind,
        maskedDigits: String,
        tagLabel: String,
        balance: Double,
        isPrimaryUpi: Boolean,
        upiAlias: String?,
        senderId: String?,
        missedCallNumber: String?
    ) -> Unit
) {
    var kind by remember { mutableStateOf(existing?.accountKind ?: AccountKind.BANK) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var shortName by remember { mutableStateOf(existing?.shortName.orEmpty()) }
    var maskedDigits by remember { mutableStateOf(existing?.maskedDigits.orEmpty()) }
    var tagLabel by remember { mutableStateOf(existing?.tagLabel.orEmpty()) }
    var balanceStr by remember { mutableStateOf(existing?.let { "%.2f".format(it.balance) } ?: "0.00") }
    var isPrimaryUpi by remember { mutableStateOf(existing?.isPrimaryUpi ?: false) }
    var upiAlias by remember { mutableStateOf(existing?.upiAlias.orEmpty()) }
    var senderId by remember { mutableStateOf(existing?.senderId.orEmpty()) }
    var missedCallNumber by remember { mutableStateOf(existing?.missedCallNumber.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.SurfaceContainerLow,
        dragHandle = {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp, 5.dp).clip(CircleShape).background(ObsidianColors.OutlineVariant.copy(alpha = 0.5f)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Set Up New Account" else "Edit Account Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianColors.OnSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ObsidianColors.OnSurfaceVariant)
                }
            }

            // Account Kind Selector
            Surface(
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    val isBank = kind == AccountKind.BANK
                    Surface(
                        shape = CircleShape,
                        color = if (isBank) ObsidianColors.PrimaryContainer else Color.Transparent,
                        modifier = Modifier.weight(1f).clickable { kind = AccountKind.BANK }
                    ) {
                        Text(
                            text = "Bank Account",
                            fontSize = 12.sp,
                            fontWeight = if (isBank) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBank) ObsidianColors.Canvas else ObsidianColors.OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    val isCash = kind == AccountKind.CASH
                    Surface(
                        shape = CircleShape,
                        color = if (isCash) ObsidianColors.Tertiary else Color.Transparent,
                        modifier = Modifier.weight(1f).clickable { kind = AccountKind.CASH }
                    ) {
                        Text(
                            text = "Cash Wallet",
                            fontSize = 12.sp,
                            fontWeight = if (isCash) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCash) ObsidianColors.Canvas else ObsidianColors.OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (shortName.isBlank() && it.length <= 6) shortName = it
                },
                label = { Text(if (kind == AccountKind.BANK) "Bank Name (e.g. State Bank of India)" else "Wallet Name (e.g. Cash Wallet)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                    unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                )
            )

            // Short Name & Digits Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Short Code (SBI)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                        unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                    )
                )

                if (kind == AccountKind.BANK) {
                    OutlinedTextField(
                        value = maskedDigits,
                        onValueChange = { maskedDigits = it.take(4) },
                        label = { Text("Last 4 Digits") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                            unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                        )
                    )
                }
            }

            // Tag Label & Balance Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = tagLabel,
                    onValueChange = { tagLabel = it },
                    label = { Text("Tag (e.g. Salary)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                        unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                    )
                )

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Opening Balance (₹)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                        unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                    )
                )
            }

            // Bank-specific details
            if (kind == AccountKind.BANK) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUICK BANK PRESETS (1-TAP AUTOFILL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianColors.PrimaryContainer,
                        letterSpacing = 0.05.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(BankSyncManager.POPULAR_BANKS) { preset ->
                            val isSelected = senderId.equals(preset.keyword, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ObsidianColors.PrimaryContainer.copy(alpha = 0.25f)
                                else ObsidianColors.SurfaceContainerHigh,
                                border = if (isSelected) BorderStroke(1.dp, ObsidianColors.PrimaryContainer)
                                else BorderStroke(1.dp, ObsidianColors.HairlineBorder),
                                modifier = Modifier.clickable {
                                    if (name.isBlank() || BankSyncManager.POPULAR_BANKS.any { it.name == name }) {
                                        name = preset.name
                                    }
                                    shortName = preset.keyword
                                    senderId = preset.keyword
                                    missedCallNumber = preset.missedCallNumber
                                    if (tagLabel.isBlank()) tagLabel = "Savings"
                                }
                            ) {
                                Text(
                                    text = preset.keyword,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) ObsidianColors.PrimaryContainer else ObsidianColors.OnSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = upiAlias,
                    onValueChange = { upiAlias = it },
                    label = { Text("UPI Alias (optional, e.g. user@okhdfcbank)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                        unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = senderId,
                        onValueChange = { senderId = it },
                        label = { Text("Bank Keyword (e.g. SBI)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                            unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                        )
                    )

                    OutlinedTextField(
                        value = missedCallNumber,
                        onValueChange = { missedCallNumber = it },
                        label = { Text("Missed Call Number") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                            unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as Primary UPI Profile", fontSize = 13.sp, color = ObsidianColors.OnSurface)
                    Switch(
                        checked = isPrimaryUpi,
                        onCheckedChange = { isPrimaryUpi = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianColors.Canvas,
                            checkedTrackColor = ObsidianColors.Secondary
                        )
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val bal = balanceStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        existing?.id,
                        name,
                        shortName,
                        kind,
                        maskedDigits,
                        tagLabel,
                        bal,
                        isPrimaryUpi,
                        upiAlias,
                        senderId,
                        missedCallNumber
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.Tertiary),
                enabled = name.isNotBlank()
            ) {
                Text(if (existing == null) "Create Profile" else "Save Changes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Canvas)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

// ==========================================
// 6. ADD TRANSACTION MODAL (BottomSheet)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionType, Double, String, String, String, String?, Boolean) -> Unit
) {
    var txType by remember { mutableStateOf(TransactionType.DEBIT) }
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Food & Dining") }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var selectedToAccountId by remember(accounts) { mutableStateOf(accounts.drop(1).firstOrNull()?.id) }
    var taxDeductible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.SurfaceContainerLow,
        dragHandle = {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp, 5.dp).clip(CircleShape).background(ObsidianColors.OutlineVariant.copy(alpha = 0.5f)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ObsidianColors.Tertiary))
                    Text("Add New Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ObsidianColors.OnSurfaceVariant)
                }
            }

            // Segmented Type Selector
            Surface(
                shape = CircleShape,
                color = ObsidianColors.SurfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val types = listOf(
                        Triple(TransactionType.DEBIT, "Expense", ObsidianColors.ExpenseRose),
                        Triple(TransactionType.CREDIT, "Income", ObsidianColors.Tertiary),
                        Triple(TransactionType.SELF_TRANSFER, "Transfer", ObsidianColors.Secondary)
                    )
                    types.forEach { (type, label, accentColor) ->
                        val isSel = txType == type
                        Surface(
                            shape = CircleShape,
                            color = if (isSel) accentColor else Color.Transparent,
                            modifier = Modifier.weight(1f).clickable { txType = type }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) ObsidianColors.Canvas else ObsidianColors.OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Hero Amount Display Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (txType) {
                            TransactionType.DEBIT -> "DEBIT AMOUNT"
                            TransactionType.CREDIT -> "CREDIT AMOUNT"
                            TransactionType.SELF_TRANSFER -> "TRANSFER AMOUNT"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianColors.OnSurfaceVariant,
                        letterSpacing = 0.05.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹ ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurfaceVariant)
                        androidx.compose.foundation.text.BasicTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = ObsidianColors.OnSurface,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }

                    // Quick Increment Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100, 500, 1000, 5000).forEach { inc ->
                            Surface(
                                shape = CircleShape,
                                color = ObsidianColors.SurfaceContainerHighest,
                                modifier = Modifier.clickable {
                                    val current = amountStr.toDoubleOrNull() ?: 0.0
                                    amountStr = "%.2f".format(current + inc)
                                }
                            ) {
                                Text("+ ₹$inc", fontSize = 11.sp, color = ObsidianColors.OnSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
            }

            // Paid From Account Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (txType == TransactionType.SELF_TRANSFER) "Transfer From" else "Paid From Account",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSel = (selectedAccountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() } == acc.id)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) ObsidianColors.PrimaryContainer else ObsidianColors.SurfaceContainerHigh,
                            modifier = Modifier.clickable { selectedAccountId = acc.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = if (isSel) ObsidianColors.OnPrimary else ObsidianColors.Primary, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(acc.shortName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) ObsidianColors.OnPrimary else ObsidianColors.OnSurface)
                                    Text("₹%,.0f".format(acc.balance), fontSize = 10.sp, color = if (isSel) ObsidianColors.OnPrimary.copy(alpha = 0.8f) else ObsidianColors.OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Transfer To Account Selector (shown only for SELF_TRANSFER)
            if (txType == TransactionType.SELF_TRANSFER && accounts.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Transfer To", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.Secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            val isSel = selectedToAccountId == acc.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) ObsidianColors.SecondaryContainer.copy(alpha = 0.3f) else ObsidianColors.SurfaceContainerHigh,
                                border = if (isSel) BorderStroke(1.dp, ObsidianColors.Secondary) else null,
                                modifier = Modifier.clickable { selectedToAccountId = acc.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = if (isSel) ObsidianColors.Secondary else ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(acc.shortName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                                        Text("₹%,.0f".format(acc.balance), fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Selector (hide for transfers)
            if (txType != TransactionType.SELF_TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category: $selectedCat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.Secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cats = listOf("Food & Dining", "Fuel & Transport", "Groceries", "Shopping & Retail", "Entertainment", "Income", "Medical", "Utilities", "General")
                        cats.forEach { cat ->
                            val isSel = selectedCat == cat
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) ObsidianColors.SecondaryContainer.copy(alpha = 0.25f) else ObsidianColors.SurfaceContainerHighest,
                                border = if (isSel) BorderStroke(1.dp, ObsidianColors.Secondary) else null,
                                modifier = Modifier.clickable { selectedCat = cat }
                            ) {
                                Text(
                                    cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) ObsidianColors.Secondary else ObsidianColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Merchant / Note Input
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Note (e.g. Swiggy, Uber)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                    unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                )
            )

            // Tax Deductible Switch
            if (txType == TransactionType.DEBIT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as tax deductible", fontSize = 13.sp, color = ObsidianColors.OnSurface)
                    Switch(
                        checked = taxDeductible,
                        onCheckedChange = { taxDeductible = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianColors.Canvas,
                            checkedTrackColor = ObsidianColors.Tertiary
                        )
                    )
                }
            }

            // Save Transaction Button
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val accId = selectedAccountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() }
                    if (amt > 0 && accId.isNotBlank()) {
                        val dest = if (txType == TransactionType.SELF_TRANSFER) selectedToAccountId else null
                        onSave(txType, amt, merchant.ifBlank { "Manual Entry" }, selectedCat, accId, dest, taxDeductible)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.Tertiary),
                enabled = accounts.isNotEmpty() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save Transaction", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Canvas)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ==========================================
// 7. EDIT TRANSACTION MODAL
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: TransactionEntity,
    accounts: List<AccountProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionType, Double, String, String, String, String?, Boolean) -> Unit
) {
    var txType by remember { mutableStateOf(transaction.type) }
    var amountStr by remember { mutableStateOf("%.2f".format(transaction.amount)) }
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var category by remember { mutableStateOf(transaction.category) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var toAccountId by remember { mutableStateOf(transaction.toAccountId) }
    var taxDeductible by remember { mutableStateOf(transaction.taxDeductible) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.SurfaceContainerLow,
        dragHandle = {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp, 5.dp).clip(CircleShape).background(ObsidianColors.OutlineVariant.copy(alpha = 0.5f)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ObsidianColors.OnSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Note") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                    unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                )
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                    unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                )
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianColors.SurfaceContainerHigh,
                    unfocusedContainerColor = ObsidianColors.SurfaceContainerHigh
                )
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(txType, amt, merchant.trim(), category.trim(), selectedAccountId, toAccountId, taxDeductible)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.Tertiary)
            ) {
                Text("Save Changes", color = ObsidianColors.Canvas, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

// ==========================================
// 8. REASSIGN ACCOUNT MODAL
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReassignAccountSheet(
    transaction: TransactionEntity,
    accounts: List<AccountProfileEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var selectedId by remember { mutableStateOf(transaction.accountId) }
    var autoFuture by remember { mutableStateOf(true) }

    val originAcc = accounts.firstOrNull { it.id == transaction.accountId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianColors.SurfaceContainerHigh,
        dragHandle = {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp, 5.dp).clip(CircleShape).background(ObsidianColors.OutlineVariant.copy(alpha = 0.5f)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(ObsidianColors.SecondaryContainer.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ObsidianColors.Secondary, modifier = Modifier.size(18.dp))
                    }
                    Text("Reassign Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ObsidianColors.OnSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianColors.SurfaceContainerLowest,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(transaction.merchant, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)
                        val amtColor = if (transaction.type == TransactionType.CREDIT) ObsidianColors.Tertiary else ObsidianColors.ExpenseRose
                        Text("₹%,.2f".format(transaction.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = amtColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Origin", fontSize = 11.sp, color = ObsidianColors.Outline)
                        Surface(shape = CircleShape, color = ObsidianColors.SurfaceContainerHighest) {
                            Text("${originAcc?.name ?: "Unknown"} (•••• ${originAcc?.maskedDigits.orEmpty()})", fontSize = 11.sp, color = ObsidianColors.OnSurface, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Destination Account", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.OnSurface)

                accounts.forEach { acc ->
                    val isSel = acc.id == selectedId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) ObsidianColors.SurfaceContainerHighest else ObsidianColors.SurfaceContainer,
                        border = if (isSel) BorderStroke(1.5.dp, ObsidianColors.Secondary) else null,
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = acc.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = if (isSel) ObsidianColors.Secondary else ObsidianColors.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ObsidianColors.OnSurface)
                                        if (acc.id == transaction.accountId) {
                                            Surface(shape = CircleShape, color = ObsidianColors.SurfaceContainerHighest) {
                                                Text("Current", fontSize = 9.sp, color = ObsidianColors.OnSurfaceVariant, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    Text("•••• ${acc.maskedDigits.ifBlank { "Cash" }} • ₹%,.2f".format(acc.balance), fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-reassign future matches", fontSize = 13.sp, color = ObsidianColors.OnSurface, fontWeight = FontWeight.Medium)
                    Text("Routes future matching SMS to this account", fontSize = 10.sp, color = ObsidianColors.OnSurfaceVariant)
                }
                Switch(
                    checked = autoFuture,
                    onCheckedChange = { autoFuture = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianColors.Canvas,
                        checkedTrackColor = ObsidianColors.Tertiary
                    )
                )
            }

            Button(
                onClick = { onConfirm(selectedId, autoFuture) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.Secondary)
            ) {
                Text("Confirm Reassignment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ObsidianColors.Canvas)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
