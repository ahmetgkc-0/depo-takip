package com.example

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Stok Listesi") },
                    label = { Text("Stok Listesi") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Tarayıcı") },
                    label = { Text("Tarayıcı") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (selectedTab == 0) {
                InventoryTab(viewModel)
            } else {
                ScannerTab(viewModel)
            }
        }
    }
}

@Composable
fun ScannerTab(viewModel: MainViewModel) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isScanningPaused by viewModel.isScanningPaused.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (!formState.successMessage.isNullOrEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.resumeScanning() },
                title = { Text("İşlem Başarılı") },
                text = { Text(formState.successMessage!!) },
                confirmButton = {
                    Button(onClick = { viewModel.resumeScanning() }) {
                        Text("Yeni Tarama Yap")
                    }
                }
            )
        }

        // Camera Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            CameraPreview(
                isScanningPaused = isScanningPaused,
                onBarcodeScanned = { barcode ->
                    triggerFeedback(context)
                    viewModel.onBarcodeScanned(barcode)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isScanningPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { viewModel.resumeScanning() }) {
                        Text("Yeni Tarama Yap")
                    }
                }
            }
        }

        // Form Section
        AnimatedVisibility(visible = isScanningPaused) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!formState.errorMessage.isNullOrEmpty()) {
                    Text(
                        text = formState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.barcode,
                        onValueChange = { viewModel.updateFormState { s -> s.copy(barcode = it) } },
                        label = { Text("Barkod") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formState.stockCode,
                        onValueChange = { viewModel.updateFormState { s -> s.copy(stockCode = it) } },
                        label = { Text("Stok Kodu") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = formState.stockName,
                    onValueChange = { viewModel.updateFormState { s -> s.copy(stockName = it) } },
                    label = { Text("Stok Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.brand,
                        onValueChange = { viewModel.updateFormState { s -> s.copy(brand = it) } },
                        label = { Text("Marka") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formState.unit,
                        onValueChange = { viewModel.updateFormState { s -> s.copy(unit = it) } },
                        label = { Text("Birim (Adet, vb.)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text(
                    text = "Mevcut Stok: ${formState.currentQuantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Transaction Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransactionTypeButton(
                                text = "GELDİ (STOK EKLE)",
                                selected = formState.transactionType == "IN",
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.updateFormState { it.copy(transactionType = "IN") } }
                            )
                            TransactionTypeButton(
                                text = "GİTTİ (STOK DÜŞ)",
                                selected = formState.transactionType == "OUT",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.updateFormState { it.copy(transactionType = "OUT") } }
                            )
                        }

                        OutlinedTextField(
                            value = formState.transactionQuantity,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    viewModel.updateFormState { s -> s.copy(transactionQuantity = it) } 
                                }
                            },
                            label = { Text("İşlem Miktarı") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        )

                        Button(
                            onClick = { viewModel.saveTransaction() },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("İŞLEMİ KAYDET", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionTypeButton(
    text: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color else color.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InventoryTab(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    
    val displayList = if (searchQuery.isNotEmpty()) searchResults else allProducts
    val context = LocalContext.current
    var exportMessage by remember { mutableStateOf<String?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportToCsv(uri, context) { _, message ->
                    exportMessage = message
                }
            }
        }
    )

    if (exportMessage != null) {
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("Dışa Aktar") },
            text = { Text(exportMessage!!) },
            confirmButton = {
                Button(onClick = { exportMessage = null }) {
                    Text("Tamam")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Stok Ara (Ad veya Kod)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { exportLauncher.launch("stok_listesi.csv") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Excel (CSV) Olarak Dışa Aktar")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayList) { product ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = product.stockCode, fontWeight = FontWeight.Bold)
                            Text(text = "Miktar: ${product.currentQuantity} ${product.unit}")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = product.stockName, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Barkod: ${product.barcode}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun triggerFeedback(context: Context) {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    } catch (e: Exception) {
        // Ignore
    }
}
