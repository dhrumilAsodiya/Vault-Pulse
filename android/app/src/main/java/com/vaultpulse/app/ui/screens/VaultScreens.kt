package com.vaultpulse.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

enum class VaultTab(val label: String) {
    HOME("Home"),
    LEDGER("Ledger"),
    ANALYTICS("Analytics"),
    SETTINGS("Settings")
}

@Composable
fun VaultPulseRoot(viewModel: VaultViewModel) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(VaultTab.HOME) }
    var showAddSheet by remember { mutableStateOf(false) }
    var reassignTarget by remember { mutableStateOf<TransactionEntity?>(null) }

    state.syncAlertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSyncAlert() },
            containerColor = ObsidianColors.SurfaceContainer,
            title = { Text("Bank Sync Checkpoint", color = ObsidianColors.OnSurface) },
            text = { Text(msg, color = ObsidianColors.OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSyncAlert() }) {
                    Text("OK", color = ObsidianColors.PrimaryIndigo)
                }
            }
        )
    }

    Scaffold(
        containerColor = ObsidianColors.Canvas,
        floatingActionButton = {
            if (activeTab == VaultTab.HOME || activeTab == VaultTab.LEDGER) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = ObsidianColors.PositiveMint,
                    contentColor = Color(0xFF00210C),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
                        Text("Add Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = ObsidianColors.Surface) {
                VaultTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        VaultTab.HOME -> Icons.Rounded.AccountBalanceWallet
                        VaultTab.LEDGER -> Icons.Rounded.ReceiptLong
                        VaultTab.ANALYTICS -> Icons.Rounded.BarChart
                        VaultTab.SETTINGS -> Icons.Rounded.Settings
                    }
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianColors.SecondaryTeal,
                            selectedTextColor = ObsidianColors.SecondaryTeal,
                            indicatorColor = ObsidianColors.SecondaryTeal.copy(alpha = 0.2f),
                            unselectedIconColor = ObsidianColors.OnSurfaceVariant,
                            unselectedTextColor = ObsidianColors.OnSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (activeTab) {
            VaultTab.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onCallBank = { viewModel.callBank(it) },
                onSyncSms = { viewModel.syncBankSms(it) }
            )
            VaultTab.LEDGER -> LedgerScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onTapAccountBadge = { reassignTarget = it }
            )
            VaultTab.ANALYTICS -> AnalyticsScreen(
                modifier = Modifier.padding(padding),
                state = state
            )
            VaultTab.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onSelectPrimary = { viewModel.setPrimaryUpiProfile(it) },
                onAddRule = { kw, cat -> viewModel.saveKeywordRule(kw, cat) },
                onDeleteRule = { viewModel.deleteKeywordRule(it) }
            )
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            accounts = state.accounts,
            onDismiss = { showAddSheet = false },
            onSave = { type, amount, merchant, cat, fromId, toId, tax ->
                viewModel.addTransaction(type, amount, merchant, cat, fromId, toId, tax)
                showAddSheet = false
            }
        )
    }

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
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: VaultUiState,
    onCallBank: (AccountProfileEntity) -> Unit,
    onSyncSms: (AccountProfileEntity) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TOTAL NET LIQUIDITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹%,.2f".format(state.netLiquidity),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        items(state.accounts, key = { it.id }) { account ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = account.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (account.maskedDigits.isNotBlank()) "•••• ${account.maskedDigits} · ${account.tagLabel}" else account.tagLabel,
                        fontSize = 12.sp,
                        color = ObsidianColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "₹%,.2f".format(account.balance),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (account.accountKind == AccountKind.BANK) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { onCallBank(account) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null)
                                Spacer(Modifier.padding(4.dp))
                                Text("Call Bank")
                            }
                            OutlinedButton(
                                onClick = { onSyncSms(account) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null)
                                Spacer(Modifier.padding(4.dp))
                                Text("Sync SMS")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerScreen(
    modifier: Modifier = Modifier,
    state: VaultUiState,
    onTapAccountBadge: (TransactionEntity) -> Unit
) {
    var filter by remember { mutableStateOf<TransactionType?>(null) }
    val filtered = remember(state.transactions, filter) {
        if (filter == null) state.transactions else state.transactions.filter { it.type == filter }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                FilterChip(selected = filter == TransactionType.DEBIT, onClick = { filter = TransactionType.DEBIT }, label = { Text("Debit") })
                FilterChip(selected = filter == TransactionType.CREDIT, onClick = { filter = TransactionType.CREDIT }, label = { Text("Credit") })
                FilterChip(selected = filter == TransactionType.SELF_TRANSFER, onClick = { filter = TransactionType.SELF_TRANSFER }, label = { Text("Self-Transfer") })
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Spent", fontSize = 12.sp, color = ObsidianColors.OnSurfaceVariant)
                        Text("₹%,.0f".format(state.totalSpent), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Income", fontSize = 12.sp, color = ObsidianColors.OnSurfaceVariant)
                        Text("+₹%,.0f".format(state.totalIncome), fontWeight = FontWeight.Bold, color = ObsidianColors.PositiveMint)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saved", fontSize = 12.sp, color = ObsidianColors.OnSurfaceVariant)
                        Text("${state.savedPercent}%", fontWeight = FontWeight.Bold, color = ObsidianColors.SecondaryTeal)
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { tx ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.merchant, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ObsidianColors.SurfaceContainerHighest,
                            modifier = Modifier.clickable { onTapAccountBadge(tx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tx.category, fontSize = 11.sp, color = ObsidianColors.OnSurface)
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Reassign account", tint = ObsidianColors.PrimaryIndigo)
                            }
                        }
                    }
                    val amountColor = when (tx.type) {
                        TransactionType.CREDIT -> ObsidianColors.PositiveMint
                        TransactionType.DEBIT -> ObsidianColors.ExpenseRose
                        TransactionType.SELF_TRANSFER -> ObsidianColors.OnSurface
                    }
                    val prefix = when (tx.type) {
                        TransactionType.CREDIT -> "+"
                        TransactionType.DEBIT -> "-"
                        TransactionType.SELF_TRANSFER -> ""
                    }
                    Text(
                        text = "$prefix₹%,.2f".format(tx.amount),
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier, state: VaultUiState) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOTAL MONTHLY SPEND", fontSize = 11.sp, color = ObsidianColors.OnSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("₹%,.2f".format(state.totalSpent), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Excludes Self-Transfers · ${state.savedPercent}% net savings rate", fontSize = 12.sp, color = ObsidianColors.SecondaryTeal)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: VaultUiState,
    onSelectPrimary: (String) -> Unit,
    onAddRule: (String, String) -> Unit,
    onDeleteRule: (String) -> Unit
) {
    var kwDraft by remember { mutableStateOf("") }
    var catDraft by remember { mutableStateOf("Food & Dining") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings & Automation", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Default Primary UPI Profile", fontWeight = FontWeight.SemiBold, color = Color.White)
                    state.accounts.filter { it.accountKind == AccountKind.BANK }.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPrimary(acc.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${acc.name} (•••• ${acc.maskedDigits})", color = ObsidianColors.OnSurface)
                            Text(if (acc.isPrimaryUpi) "Primary" else "Select", color = ObsidianColors.SecondaryTeal)
                        }
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianColors.SurfaceContainer,
                border = BorderStroke(1.dp, ObsidianColors.HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Auto-Categorization Keyword Rules", fontWeight = FontWeight.SemiBold, color = Color.White)
                    state.keywordRules.forEach { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rule.keywordsCsv, color = Color.White, fontSize = 14.sp)
                                Text("Maps to ${rule.category}", color = ObsidianColors.SecondaryTeal, fontSize = 12.sp)
                            }
                            TextButton(onClick = { onDeleteRule(rule.id) }) {
                                Text("Delete", color = ObsidianColors.ExpenseRose)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = kwDraft,
                        onValueChange = { kwDraft = it },
                        label = { Text("Keywords (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (kwDraft.isNotBlank()) {
                                onAddRule(kwDraft, catDraft)
                                kwDraft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PrimaryIndigo)
                    ) {
                        Text("Add Keyword Rule", color = ObsidianColors.Canvas, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionType, Double, String, String, String, String?, Boolean) -> Unit
) {
    var txType by remember { mutableStateOf(TransactionType.DEBIT) }
    var amountStr by remember { mutableStateOf("1450") }
    var merchant by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var taxDeductible by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianColors.SurfaceContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add New Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = txType == TransactionType.DEBIT, onClick = { txType = TransactionType.DEBIT }, label = { Text("Expense") })
                FilterChip(selected = txType == TransactionType.CREDIT, onClick = { txType = TransactionType.CREDIT }, label = { Text("Income") })
                FilterChip(selected = txType == TransactionType.SELF_TRANSFER, onClick = { txType = TransactionType.SELF_TRANSFER }, label = { Text("Transfer") })
            }
            OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("Merchant / Note") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Mark as tax deductible", color = Color.White)
                Switch(checked = taxDeductible, onCheckedChange = { taxDeductible = it })
            }
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedAccountId.isNotBlank()) {
                        val dest = if (txType == TransactionType.SELF_TRANSFER) accounts.firstOrNull { it.id != selectedAccountId }?.id else null
                        onSave(txType, amt, merchant.ifBlank { "Manual Entry" }, "", selectedAccountId, dest, taxDeductible)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PositiveFixed)
            ) {
                Text("Save Transaction", color = Color(0xFF00210C), fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianColors.SurfaceContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reassign Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            accounts.forEach { acc ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (acc.id == selectedId) ObsidianColors.SurfaceContainerHigh else ObsidianColors.Surface,
                    modifier = Modifier.fillMaxWidth().clickable { selectedId = acc.id }
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${acc.shortName} •••• ${acc.maskedDigits}", color = Color.White)
                        Text("₹%,.2f".format(acc.balance), color = ObsidianColors.PositiveMint)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Auto-reassign future matches", color = Color.White)
                Switch(checked = autoFuture, onCheckedChange = { autoFuture = it })
            }
            Button(
                onClick = { onConfirm(selectedId, autoFuture) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianColors.PositiveFixed)
            ) {
                Text("Confirm", color = Color(0xFF00210C), fontWeight = FontWeight.Bold)
            }
        }
    }
}
