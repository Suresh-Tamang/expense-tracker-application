package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.data.Expense
import com.example.pdf.PdfExporter
import com.example.security.BiometricHelper
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

object ThemeConfig {
    var isDarkMode: Boolean = true
}

typealias Color = ComposeColor

// Dynamic theme-aware factory for hardcoded color literals
fun Color(colorVal: Long): ComposeColor {
    val isDark = ThemeConfig.isDarkMode
    return when (colorVal) {
        0xFF1C1B1FL -> if (isDark) ComposeColor(0xFF1C1B1FL) else ComposeColor(0xFFFFFFFFL) // Pure white background
        0xFF2B2930L -> if (isDark) ComposeColor(0xFF2B2930L) else ComposeColor(0xFFFFFFFFL) // Pure white surfaces
        0xFF49454FL -> if (isDark) ComposeColor(0xFF49454FL) else ComposeColor(0xFFE0E0E0L) // Subtle light gray border
        0xFFE6E1E5L -> if (isDark) ComposeColor(0xFFE6E1E5L) else ComposeColor(0xFF000000L) // Text is pure black
        0xFFCAC4D0L -> if (isDark) ComposeColor(0xFFCAC4D0L) else ComposeColor(0xFF212121L) // Sub-text is black/charcoal
        0xFF938F99L -> if (isDark) ComposeColor(0xFF938F99L) else ComposeColor(0xFF424242L) // Muted text is dark gray
        0xFFD0BCFFL -> if (isDark) ComposeColor(0xFFD0BCFFL) else ComposeColor(0xFF000000L) // Highlights / major text are black
        0xFF381E72L -> if (isDark) ComposeColor(0xFF381E72L) else ComposeColor(0xFFFFFFFFL) // High contrast on-primary white
        0xFF332D41L -> if (isDark) ComposeColor(0xFF332D41L) else ComposeColor(0xFFF5F5F5L) // Light containers
        0xFF4F378BL -> if (isDark) ComposeColor(0xFF4F378BL) else ComposeColor(0xFFEEEEEEL) // Light container backgrounds
        0xFFEADDFFL -> if (isDark) ComposeColor(0xFFEADDFFL) else ComposeColor(0xFF000000L) // Container content is black
        else -> ComposeColor(colorVal)
    }
}

@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel, activity: FragmentActivity) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    ThemeConfig.isDarkMode = isDarkTheme
    var isUnlocked by remember { mutableStateFlowOf(false) }

    // Biometric authentication state and preferences load
    LaunchedEffect(Unit) {
        if (BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = { isUnlocked = true },
                onError = { error ->
                    Toast.makeText(context, "Authentication: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // Fallback for devices without enrolled biometrics or JVM Emulator
            isUnlocked = true
        }
    }

    AnimatedVisibility(
        visible = !isUnlocked,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        SecurityLockScreen(
            onUnlockMock = { isUnlocked = true },
            activity = activity,
            context = context
        )
    }

    AnimatedVisibility(
        visible = isUnlocked,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut()
    ) {
        MainDashboard(viewModel = viewModel)
    }
}

@Composable
fun SecurityLockScreen(
    onUnlockMock: () -> Unit,
    activity: FragmentActivity,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FinFlow Secure Lock",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "All your financial data remains fully encrypted on this device.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (BiometricHelper.isBiometricAvailable(context)) {
                        BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            onSuccess = onUnlockMock,
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        onUnlockMock()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Security", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onUnlockMock) {
                Text("Use Quick Pin (Bypass Mock)", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    ThemeConfig.isDarkMode = isDarkTheme

    var selectedTab by remember { mutableStateFlowOf(0) }
    val currentMonth by viewModel.currentMonth.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val currentBudget by viewModel.currentBudget.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrency(context)
        viewModel.loadTheme(context)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Charts") },
                    label = { Text("Charts") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Manage") },
                    label = { Text("Manage") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    viewModel = viewModel,
                    expenses = monthlyExpenses,
                    budget = currentBudget,
                    monthKey = currentMonth
                )
                1 -> ChartsTab(
                    viewModel = viewModel,
                    expenses = monthlyExpenses,
                    budget = currentBudget
                )
                2 -> ManageTab(
                    viewModel = viewModel,
                    expenses = monthlyExpenses,
                    budget = currentBudget,
                    monthKey = currentMonth
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: ExpenseViewModel,
    expenses: List<Expense>,
    budget: Double,
    monthKey: String
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is androidx.fragment.app.FragmentActivity) {
                break
            }
            currentContext = currentContext.baseContext
        }
        currentContext as? androidx.fragment.app.FragmentActivity
    }
    var showAddDialog by remember { mutableStateFlowOf(false) }
    var showEditProfileDialog by remember { mutableStateFlowOf(false) }
    val userNameState by viewModel.userName.collectAsState()
    val profilePicturePathState by viewModel.profilePicturePath.collectAsState()
    val currencyState by viewModel.selectedCurrency.collectAsState()

    // Load persisted username, profile picture, and currency on active composition
    LaunchedEffect(Unit) {
        viewModel.loadUserName(context)
        viewModel.loadProfilePicture(context)
        viewModel.loadCurrency(context)
    }

    // Calculate spent totals
    val totalSpent = expenses.sumOf { it.amount }
    val remaining = budget - totalSpent
    val percentage = if (budget > 0) (totalSpent / budget).coerceIn(0.0, 1.0) else 0.0

    val profileBitmap = remember(profilePicturePathState) {
        if (profilePicturePathState != null) {
            try {
                android.graphics.BitmapFactory.decodeFile(profilePicturePathState)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activity?.let {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        it,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Elegant "Welcome Back" Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEditProfileDialog = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD0BCFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap.asImageBitmap(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text("👤", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "WELCOME BACK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCAC4D0),
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userNameState,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1E5)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Theme Toggle Button
                        val isDarkTheme by viewModel.isDarkMode.collectAsState()
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF332D41), CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.updateTheme(context, !isDarkTheme)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                                contentDescription = "Toggle Theme",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Shield Quick Status Badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF332D41), CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Biometric data encryption is active.", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                        }
                    }
                }
            }

            item {
                // Monthly Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2B2930)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    "Monthly Spending", 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD0BCFF)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${currencyState.symbol}${String.format("%.2f", totalSpent)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    color = if (totalSpent > budget) MaterialTheme.colorScheme.error else Color(0xFFE6E1E5)
                                )
                            }
                            
                            // June 2024 badge
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4F378B), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = formatMonthDisplay(monthKey).uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEADDFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Budget: ${currencyState.symbol}${String.format("%.2f", budget)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCAC4D0)
                                )
                                Text(
                                    text = "${(percentage * 100).toInt()}% used",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE6E1E5)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { percentage.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(CircleShape),
                                color = if (totalSpent > budget) MaterialTheme.colorScheme.error else Color(0xFFD0BCFF),
                                trackColor = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Recent Transactions header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCAC4D0)
                    )
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "All history shown in settings reports.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No expenses logged for this month.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(expenses) { expense ->
                    ExpenseRow(expense = expense, currencySymbol = currencyState.symbol, onDelete = {
                        viewModel.deleteExpense(expense.id)
                    })
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFFD0BCFF),
            contentColor = Color(0xFF381E72)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
        }

        if (showAddDialog) {
            AddExpenseDialog(
                currencySymbol = currencyState.symbol,
                onDismiss = { showAddDialog = false },
                onAdd = { amt, cat, desc, date ->
                    viewModel.addExpense(context, amt, cat, desc, date)
                    showAddDialog = false
                }
            )
        }

        if (showEditProfileDialog) {
            EditProfileDialog(
                currentName = userNameState,
                currentPhotoPath = profilePicturePathState,
                onDismiss = { showEditProfileDialog = false },
                onSaveName = { newName ->
                    viewModel.updateUserName(context, newName)
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                },
                onSavePhoto = { newPhotoPath ->
                    viewModel.updateProfilePicture(context, newPhotoPath)
                }
            )
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, currencySymbol: String = "$", onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with Circle Background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF332D41), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifEmpty { expense.category },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE6E1E5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(expense.timestamp))} • ${expense.category}",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "-$currencySymbol${String.format("%.2f", expense.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF938F99),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartsTab(viewModel: ExpenseViewModel, expenses: List<Expense>, budget: Double) {
    val currencyState by viewModel.selectedCurrency.collectAsState()
    val categoryTotals = expenses.groupBy { it.category }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }
    val grandTotal = categoryTotals.values.sum()
    var selectedCategoryForDetails by remember { mutableStateOf<String?>(null) }
    var chartType by remember { mutableStateOf("pie") } // "pie" or "bar"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Category Breakdown",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E1E5),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
        }

        if (expenses.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color(0xFF2B2930), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (chartType == "pie") Color(0xFFD0BCFF) else Color.Transparent)
                            .clickable { chartType = "pie" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = if (chartType == "pie") Color(0xFF381E72) else Color(0xFFCAC4D0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Donut Chart",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartType == "pie") Color(0xFF381E72) else Color(0xFFCAC4D0)
                            )
                        }
                    }

                    // Bar Chart Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (chartType == "bar") Color(0xFFD0BCFF) else Color.Transparent)
                            .clickable { chartType = "bar" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = if (chartType == "bar") Color(0xFF381E72) else Color(0xFFCAC4D0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bar Chart",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartType == "bar") Color(0xFF381E72) else Color(0xFFCAC4D0)
                            )
                        }
                    }
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF49454F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No chart data available for this month.",
                            color = Color(0xFF938F99),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            if (chartType == "pie") {
                // Elegant Donut Canvas Chart Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(150.dp)) {
                                    var startAngle = -90f
                                    categoryTotals.forEach { (cat, amt) ->
                                        val sweep = (amt / grandTotal).toFloat() * 360f
                                        drawArc(
                                            color = getCategoryColor(cat),
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        startAngle += sweep
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Summary", 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCAC4D0)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${currencyState.symbol}${String.format("%.2f", grandTotal)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFE6E1E5)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Elegant Vertical Bar Chart Card
                item {
                    val maxVal = categoryTotals.values.maxOrNull() ?: 1.0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Category Trends",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCAC4D0),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                categoryTotals.toList().sortedByDescending { it.second }.forEach { (category, total) ->
                                    val percentageHeight = (total / maxVal).toFloat()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${currencyState.symbol}${String.format("%.0f", total)}",
                                            fontSize = 9.sp,
                                            color = Color(0xFFE6E1E5),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.6f)
                                                .fillMaxHeight(percentageHeight.coerceAtLeast(0.08f))
                                                .background(
                                                    color = getCategoryColor(category),
                                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = category.take(5),
                                            fontSize = 10.sp,
                                            color = Color(0xFFCAC4D0),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories List
            item {
                Text(
                    text = "Distribution",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCAC4D0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }

            items(categoryTotals.toList().sortedByDescending { it.second }) { (cat, amt) ->
                val percentage = if (grandTotal > 0) (amt / grandTotal) * 100 else 0.0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedCategoryForDetails = cat },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(getCategoryColor(cat), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = cat,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE6E1E5),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${currencyState.symbol}${String.format("%.2f", amt)} (${String.format("%.1f", percentage)}%)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1E5)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (selectedCategoryForDetails != null) {
        CategoryExpensesDialog(
            category = selectedCategoryForDetails!!,
            expenses = expenses.filter { it.category == selectedCategoryForDetails },
            onDismiss = { selectedCategoryForDetails = null },
            viewModel = viewModel
        )
    }
}

@Composable
fun ManageTab(
    viewModel: ExpenseViewModel,
    expenses: List<Expense>,
    budget: Double,
    monthKey: String
) {
    val context = LocalContext.current
    var showBudgetDialog by remember { mutableStateFlowOf(false) }
    var previewPdfFile by remember { mutableStateOf<java.io.File?>(null) }

    val sharePdf = { file: java.io.File ->
        val authority = "${context.packageName}.fileprovider"
        try {
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Expense Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val userNameState by viewModel.userName.collectAsState()
    val profilePicturePathState by viewModel.profilePicturePath.collectAsState()
    val currencyState by viewModel.selectedCurrency.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserName(context)
        viewModel.loadProfilePicture(context)
        viewModel.loadCurrency(context)
    }

    val profileBitmap = remember(profilePicturePathState) {
        if (profilePicturePathState != null) {
            try {
                android.graphics.BitmapFactory.decodeFile(profilePicturePathState)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Management & Reports",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6E1E5)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Profile Card (Interactive)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEditProfileDialog = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text("👤", fontSize = 28.sp)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userNameState,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Secure Profile Settings",
                        fontSize = 11.sp,
                        color = Color(0xFFCAC4D0)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit Profile",
                    tint = Color(0xFFD0BCFF)
                )
            }
        }

        if (showEditProfileDialog) {
            EditProfileDialog(
                currentName = userNameState,
                currentPhotoPath = profilePicturePathState,
                onDismiss = { showEditProfileDialog = false },
                onSaveName = { newName ->
                    viewModel.updateUserName(context, newName)
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                },
                onSavePhoto = { newPhotoPath ->
                    viewModel.updateProfilePicture(context, newPhotoPath)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Month Selector Component (Surgical Left-Right arrow buttons)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    viewModel.selectMonth(getAdjacentMonthKey(monthKey, -1))
                }) {
                    Icon(
                        Icons.Default.ArrowBackIosNew, 
                        contentDescription = "Prev Month",
                        tint = Color(0xFFD0BCFF)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Selected Period", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCAC4D0)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatMonthDisplay(monthKey),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5)
                    )
                }

                IconButton(onClick = {
                    viewModel.selectMonth(getAdjacentMonthKey(monthKey, 1))
                }) {
                    Icon(
                        Icons.Default.ArrowForwardIos, 
                        contentDescription = "Next Month",
                        tint = Color(0xFFD0BCFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Budget Modifier Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBudgetDialog = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Limit Budget", 
                            fontSize = 12.sp,
                            color = Color(0xFFCAC4D0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${currencyState.symbol}${String.format("%.2f", budget)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF)
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Edit Budget",
                    tint = Color(0xFFD0BCFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Currency Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCurrencyDialog = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Preferred Currency", 
                            fontSize = 12.sp,
                            color = Color(0xFFCAC4D0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${currencyState.displayName}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF)
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Change Currency",
                    tint = Color(0xFFD0BCFF)
                )
            }
        }

        if (showCurrencyDialog) {
            CurrencySelectorDialog(
                currentCurrency = currencyState,
                onDismiss = { showCurrencyDialog = false },
                onSelectCurrency = { currency ->
                    viewModel.updateCurrency(context, currency.code)
                    showCurrencyDialog = false
                    Toast.makeText(context, "Currency changed to ${currency.code}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Toggle Card (Light / Dark mode selection)
        val isDarkTheme by viewModel.isDarkMode.collectAsState()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.updateTheme(context, !isDarkTheme) },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Theme Mode",
                            fontSize = 12.sp,
                            color = Color(0xFFCAC4D0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDarkTheme) "Sleek Dark Theme" else "Polished Light Theme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF)
                        )
                    }
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.updateTheme(context, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF381E72),
                        checkedTrackColor = Color(0xFFD0BCFF),
                        uncheckedThumbColor = Color(0xFF49454F),
                        uncheckedTrackColor = Color(0xFFCAC4D0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PDF Generation Trigger Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Export PDF Report",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Compile and export a comprehensive, print-ready document summarizing your budget limits, categorical spending, and full transactions logs.",
                    fontSize = 13.sp,
                    color = Color(0xFFCAC4D0),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val pdfFile = PdfExporter.generateExpenseReport(context, monthKey, expenses, budget, currencyState.symbol, userNameState)
                        if (pdfFile != null) {
                            Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                            previewPdfFile = pdfFile
                        } else {
                            Toast.makeText(context, "Failed to generate report.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Preview PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showBudgetDialog) {
            EditBudgetDialog(
                currentBudget = budget,
                currencySymbol = currencyState.symbol,
                onDismiss = { showBudgetDialog = false },
                onSave = { newBudget ->
                    viewModel.updateBudget(newBudget)
                    showBudgetDialog = false
                    Toast.makeText(context, "Budget limit updated successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (previewPdfFile != null) {
            PdfPreviewDialog(
                pdfFile = previewPdfFile!!,
                onDismiss = { previewPdfFile = null },
                onShare = { sharePdf(previewPdfFile!!) }
            )
        }
    }
}

@Composable
fun EditBudgetDialog(
    currentBudget: Double,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetInput by remember { mutableStateFlowOf(if (currentBudget > 0) currentBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Monthly Budget") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Set your desired expenditure limit for this period. We'll trigger notifications when spending approaches this threshold.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Budget Limit ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = budgetInput.toDoubleOrNull() ?: 0.0
                onSave(amt)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhotoPath: String?,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSavePhoto: (String?) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateFlowOf(currentName) }
    var photoPathState by remember { mutableStateOf(currentPhotoPath) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val savedPath = saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                photoPathState = savedPath
                Toast.makeText(context, "Photo loaded. Click Save to apply.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to load photo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val profileBitmap = remember(photoPathState) {
        if (photoPathState != null) {
            try {
                android.graphics.BitmapFactory.decodeFile(photoPathState)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo Display
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0BCFF), CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text("👤", fontSize = 48.sp)
                    }
                    
                    // Small overlay camera edit icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            "EDIT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Choose Photo")
                    }

                    if (photoPathState != null) {
                        TextButton(
                            onClick = { photoPathState = null },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.trim().isNotEmpty()) {
                        onSaveName(nameInput.trim())
                        onSavePhoto(photoPathState)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun saveUriToInternalStorage(context: Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, "profile_pic.jpg")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AddExpenseDialog(
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onAdd: (Double, String, String, Date) -> Unit
) {
    var amountInput by remember { mutableStateFlowOf("") }
    var descriptionInput by remember { mutableStateFlowOf("") }
    var selectedCategory by remember { mutableStateFlowOf("Food") }

    val categories = listOf("Food", "Transport", "Entertainment", "Shopping", "Bills", "Health", "Education", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New Expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector header
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = cat },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder(isSelected)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(cat),
                                            contentDescription = cat,
                                            tint = getCategoryColor(cat),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onAdd(amt, selectedCategory, descriptionInput, Date())
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility icon & colors helpers
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Food" -> Icons.Default.Fastfood
        "Transport" -> Icons.Default.DirectionsCar
        "Entertainment" -> Icons.Default.LocalPlay
        "Shopping" -> Icons.Default.ShoppingBag
        "Bills" -> Icons.Default.Receipt
        "Health" -> Icons.Default.MedicalServices
        "Education" -> Icons.Default.School
        else -> Icons.Default.Category
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food" -> Color(0xFFE57373)        // Red-Orange
        "Transport" -> Color(0xFF64B5F6)   // Blue
        "Entertainment" -> Color(0xFFBA68C8) // Purple
        "Shopping" -> Color(0xFFFFB74D)    // Orange
        "Bills" -> Color(0xFF4DB6AC)       // Teal
        "Health" -> Color(0xFFFF8A65)      // Peach-Orange
        "Education" -> Color(0xFF90A4AE)   // Blue-Grey
        else -> Color(0xFFA1887F)          // Brown
    }
}

fun formatMonthDisplay(monthKey: String): String {
    return try {
        val parts = monthKey.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
        formatter.format(cal.time)
    } catch (e: Exception) {
        monthKey
    }
}

fun getAdjacentMonthKey(monthKey: String, offset: Int): String {
    return try {
        val parts = monthKey.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, offset)
        }
        String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    } catch (e: Exception) {
        monthKey
    }
}

// Helper implementation because stateIn or collectAsState needs simple builders
inline fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)

@Composable
fun PdfPreviewDialog(
    pdfFile: java.io.File,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(context, "Report saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var pdfBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(pdfFile) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val bitmaps = PdfExporter.renderPdfToBitmaps(context, pdfFile)
            withContext(Dispatchers.Main) {
                pdfBitmaps = bitmaps
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Report Preview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            createDocumentLauncher.launch(pdfFile.name)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFFD0BCFF))
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFFD0BCFF))
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFD0BCFF))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Rendering PDF pages...", color = Color(0xFFCAC4D0), fontSize = 14.sp)
                        }
                    }
                } else if (pdfBitmaps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Unable to load PDF preview.", color = Color.Red, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(pdfBitmaps) { bitmap ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "PDF Page",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                )
                            }
                        }
                    }
                }

                // Bottom Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD0BCFF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            createDocumentLauncher.launch(pdfFile.name)
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF004D40),
                            contentColor = Color(0xFF80CBC4)
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}



@Composable
fun CategoryExpensesDialog(
    category: String,
    expenses: List<Expense>,
    onDismiss: () -> Unit,
    viewModel: ExpenseViewModel
) {
    val currencyState by viewModel.selectedCurrency.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(getCategoryColor(category), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$category Expenses",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val total = expenses.sumOf { it.amount }
                    Text(
                        text = "${currencyState.symbol}${String.format("%.2f", total)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD0BCFF)
                    )
                }

                if (expenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions in this category.", color = Color(0xFFCAC4D0), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(expenses) { expense ->
                            ExpenseRow(expense = expense, currencySymbol = currencyState.symbol, onDelete = {
                                viewModel.deleteExpense(expense.id)
                            })
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF381E72),
                        contentColor = Color(0xFFD0BCFF)
                    )
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun CurrencySelectorDialog(
    currentCurrency: com.example.ui.viewmodel.CurrencyInfo,
    onDismiss: () -> Unit,
    onSelectCurrency: (com.example.ui.viewmodel.CurrencyInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.ui.viewmodel.SUPPORTED_CURRENCIES.forEach { currency ->
                    val isSelected = currency.code == currentCurrency.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF4F378B).copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { onSelectCurrency(currency) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF332D41), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currency.symbol,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD0BCFF)
                                )
                            }
                            Column {
                                Text(
                                    text = currency.code,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1E5)
                                )
                                Text(
                                    text = currency.displayName,
                                    fontSize = 12.sp,
                                    color = Color(0xFFCAC4D0)
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFFD0BCFF)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

