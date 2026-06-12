package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MaterialRecord
import com.example.ui.theme.*
import com.example.viewmodel.QualityViewModel
import com.example.ui.components.QrCodeCameraScanner
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: QualityViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val records by viewModel.filteredRecords.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val selectedWarehouse by viewModel.selectedWarehouseFilter.collectAsState()
    val selectedNonConformance by viewModel.selectedNonConformanceFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()

    var activeParetoTab by remember { mutableStateOf("Part Number") } // "Part Number", "Warehouse", "Defecto"
    var showFilterSheet by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    if (showQrScannerDialog) {
        MockQrScannerDialog(
            records = allRecords,
            onClose = { showQrScannerDialog = false },
            onQrScanned = { oc ->
                showQrScannerDialog = false
                onNavigateToDetail(oc)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Calidad Industrial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Mat. Rechazado • $currentUser",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showQrScannerDialog = true },
                        modifier = Modifier.testTag("qr_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear QR Folio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = IndBluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("add_record_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NUEVO RECHAZO", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            
            // 1. STATS SECTION (KPI Tracker)
            StatsCardsSection(allRecords)

            // 2. PARETO CHART SECTION
            ParetoChartSection(
                records = allRecords,
                activeTab = activeParetoTab,
                onTabSelected = { activeParetoTab = it }
            )

            // 3. SEARCH & FILTERS CONTROLS
            SearchAndFilterControls(
                searchQuery = searchQuery,
                onSearchChanged = { viewModel.setSearchQuery(it) },
                onFilterClick = { showFilterSheet = true },
                isAnyFilterActive = selectedStatus != null || selectedWarehouse != null || selectedNonConformance != null || selectedDateFilter != "Todos"
            )

            // 4. LIST OF RECORDS
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No se encontraron registros de rechazo",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            "Prueba ajustando los filtros o registra uno nuevo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("records_list"),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Resultados (${records.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(records, key = { it.oc }) { record ->
                        RecordCard(
                            record = record,
                            onClick = { onNavigateToDetail(record.oc) }
                        )
                    }
                }
            }
        }
    }

    // BOTTOM FILTER SHEET DIALOG (Minimalist implementation)
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            title = { Text("Filtros de Calidad", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status Selector
                    Text("Estatus:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(null, "Abierto", "En Proceso", "Terminado").forEach { statusLabel ->
                            val isSelected = selectedStatus == statusLabel
                            Card(
                                onClick = { viewModel.setStatusFilter(statusLabel) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = statusLabel ?: "Todos",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Date range selector
                    Text("Intervalo de Tiempo:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Todos", "Hoy", "Semana", "Mes").forEach { dFilter ->
                            val isSelected = selectedDateFilter == dFilter
                            Card(
                                onClick = { viewModel.setDateFilter(dFilter) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = dFilter,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Warehouse filter dropdown items
                    Text("Almacén:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    var whExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { whExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedWarehouse ?: "Seleccionar Almacén (Todos)")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = whExpanded, onDismissRequest = { whExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("(Todos)") },
                                onClick = { viewModel.setWarehouseFilter(null); whExpanded = false }
                            )
                            val warehouseSuggestions by viewModel.warehouseSuggestions.collectAsState()
                            warehouseSuggestions.forEach { wh ->
                                DropdownMenuItem(
                                    text = { Text(wh) },
                                    onClick = { viewModel.setWarehouseFilter(wh); whExpanded = false }
                                )
                            }
                        }
                    }

                    // Defect filter dropdown
                    Text("No Conformidad:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    var ncExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { ncExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedNonConformance ?: "Seleccionar Defecto (Todos)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = ncExpanded, onDismissRequest = { ncExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("(Todos)") },
                                onClick = { viewModel.setNonConformanceFilter(null); ncExpanded = false }
                            )
                            val nonConfSuggestions by viewModel.nonConformanceSuggestions.collectAsState()
                            nonConfSuggestions.forEach { nc ->
                                DropdownMenuItem(
                                    text = { Text(nc) },
                                    onClick = { viewModel.setNonConformanceFilter(nc); ncExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterSheet = false }) {
                    Text("Aplicar Filtros")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setStatusFilter(null)
                        viewModel.setDateFilter("Todos")
                        viewModel.setWarehouseFilter(null)
                        viewModel.setNonConformanceFilter(null)
                        viewModel.setSearchQuery("")
                        showFilterSheet = false
                    }
                ) {
                    Text("Limpiar Todo")
                }
            }
        )
    }
}

@Composable
fun StatsCardsSection(allRecords: List<MaterialRecord>) {
    val total = allRecords.size
    val open = allRecords.count { it.status == "Abierto" }
    val inProcess = allRecords.count { it.status == "En Proceso" }
    val completed = allRecords.count { it.status == "Terminado" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("REGISTROS", total.toString(), IndBluePrimary),
            Triple("ABIERTOS", open.toString(), IndSteelSlate),
            Triple("PROCESO", inProcess.toString(), IndAlertOrange),
            Triple("TERMINADO", completed.toString(), IndSuccessGreen)
        ).forEach { (label, count, color) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, IndCargoGrey),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = count,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
fun ParetoChartSection(
    records: List<MaterialRecord>,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, IndCargoGrey),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Análisis de Pareto & KPIs QC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                com.example.util.PdfPrintHelper.generateAndPreviewGeneralPdf(context, records, doPrint = true)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Imprimir reporte",
                                tint = IndBluePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                com.example.util.PdfPrintHelper.generateAndPreviewGeneralPdf(context, records, doPrint = false)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Previsualizar PDF",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                
                // Segments Tab bar (full width)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Part Number", "Warehouse", "Defecto").forEach { label ->
                        val isSelected = activeTab == label
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) IndBluePrimary else Color.Transparent)
                                .clickable { onTabSelected(label) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compute counts
            val groupings: Map<String, Int> = when (activeTab) {
                "Part Number" -> records.groupBy { it.partNumber }.mapValues { entry -> entry.value.sumOf { it.quantity } }
                "Warehouse" -> records.groupBy { it.warehouse }.mapValues { entry -> entry.value.sumOf { it.quantity } }
                else -> records.groupBy { it.nonConformance }.mapValues { entry -> entry.value.sumOf { it.quantity } }
            }

            val totalVolume = groupings.values.sum().coerceAtLeast(1)
            val sortedList = groupings.entries.sortedByDescending { it.value }.take(4)

            if (sortedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin datos suficientes para calcular Pareto",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                var runningPercentage = 0f
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sortedList.forEach { entry ->
                        val itemVolume = entry.value
                        val percent = (itemVolume.toFloat() / totalVolume) * 100f
                        runningPercentage += percent
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.key,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = "${itemVolume} pzs (${String.format("%.1f", percent)}% • Acum: ${String.format("%.1f", runningPercentage)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (runningPercentage <= 80f) IndAlertRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.weight(0.5f),
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                // Accumulative bar indicator (Visual Pareto concept mapping)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percent / 100f)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (runningPercentage <= 80f) IndBluePrimary else IndSteelSlate
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAndFilterControls(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onFilterClick: () -> Unit,
    isAnyFilterActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Buscar OC, Parte, Defecto...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                    }
                }
            } else null,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("dashboard_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = IndBluePrimary,
                unfocusedBorderColor = IndCargoGrey
            )
        )

        Button(
            onClick = onFilterClick,
            modifier = Modifier.height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAnyFilterActive) IndBlueLight else MaterialTheme.colorScheme.surface,
                contentColor = if (isAnyFilterActive) IndBluePrimary else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isAnyFilterActive) IndBluePrimary else IndCargoGrey),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filtros",
                modifier = Modifier.size(20.dp)
            )
            if (isAnyFilterActive) {
                Spacer(modifier = Modifier.width(4.dp))
                Badge(containerColor = IndAlertRed) { Text("!", color = Color.White) }
            }
        }
    }
}

@Composable
fun RecordCard(record: MaterialRecord, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(record.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("record_card_${record.oc}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IndCargoGrey),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .height(IntrinsicSize.Min)
        ) {
            // Left stripe color accent representing state (abierto, proceso, terminado)
            val statusColor = when (record.status) {
                "Abierto" -> IndSteelSlate
                "En Proceso" -> IndAlertOrange
                "Terminado" -> IndSuccessGreen
                else -> MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Header: Folio / Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = record.oc,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    StatusPill(status = record.status)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Body info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PART NUMBER",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.partNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CANTIDAD",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${record.quantity} pzs",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "UBICACIÓN",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${record.warehouse} • ${record.location}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Defect details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = IndAlertRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = record.nonConformance,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer meta: Reject Type & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (record.rejectType == "Proceso") Icons.Default.PrecisionManufacturing else Icons.Default.Person2,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rechazo: ${record.rejectType}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bgColor, textColor) = when (status) {
        "Abierto" -> Color(0xFFEFF6FF) to Color(0xFF0056D2)
        "En Proceso" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        "Terminado" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockQrScannerDialog(
    records: List<MaterialRecord>,
    onClose: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMockOption by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("CÁMARA") } // "CÁMARA" o "MANUAL"

    if (activeTab == "CÁMARA") {
        AlertDialog(
            onDismissRequest = onClose,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        QrCodeCameraScanner(
                            onDismiss = onClose,
                            onQrCodeDetected = { scanned ->
                                val input = scanned.trim()
                                // Extract OC
                                var extractedOc = ""
                                if (input.startsWith("OC:")) {
                                    val pieces = input.split("|")
                                    for (p in pieces) {
                                        if (p.trim().startsWith("OC:")) {
                                            extractedOc = p.substringAfter("OC:").trim()
                                        }
                                    }
                                } else if (input.contains("-")) {
                                    extractedOc = input.split("-").first().trim()
                                } else {
                                    extractedOc = input
                                }

                                val exists = records.any { it.oc.equals(extractedOc, ignoreCase = true) }
                                if (exists) {
                                    onQrScanned(extractedOc)
                                } else {
                                    errorMessage = "El código QR '$extractedOc' no coincide con ningún folio local."
                                    activeTab = "MANUAL"
                                    rawText = scanned
                                }
                            }
                        )

                        // Alternar a entrada manual
                        Button(
                            onClick = { activeTab = "MANUAL" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ENTRADA MANUAL / SIMULAR", color = Color.White)
                        }
                    }
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onClose,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = IndBluePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escáner / Trazabilidad Manual", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Apunta al código QR generado en la etiqueta de material rechazado o simula el escaneo seleccionando un registro preexistente.",
                        fontSize = 12.sp,
                        color = IndSteelSlate
                    )

                    // Visual Mock Scan Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IndDarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedMockOption.isNotBlank() || rawText.isNotBlank()) "¡Código QR Detectado!" else "Esperando Código de Conformidad...",
                                color = if (selectedMockOption.isNotBlank() || rawText.isNotBlank()) IndSuccessGreen else Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Selector for mock existing registers
                    if (records.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Simular escaneo de lote en planta:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndBluePrimary
                            )
                            ScrollableTabRow(
                                selectedTabIndex = 0,
                                edgePadding = 0.dp,
                                indicator = {},
                                divider = {}
                            ) {
                                records.take(6).forEach { rec ->
                                    val qrStr = "OC:${rec.oc}|PN:${rec.partNumber}|QTY:${rec.quantity}"
                                    FilterChip(
                                        selected = selectedMockOption == qrStr,
                                        onClick = {
                                            selectedMockOption = qrStr
                                            rawText = qrStr
                                            errorMessage = null
                                        },
                                        label = { Text("OC: ${rec.oc}") },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Text field for manual scanning raw content
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it
                            selectedMockOption = ""
                            errorMessage = null
                        },
                        label = { Text("Código QR Escaneado (Raw String)") },
                        placeholder = { Text("OC:OC-12345|PN:PARTX|QTY:100") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("qr_scanner_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndBluePrimary,
                            unfocusedBorderColor = IndCargoGrey
                        )
                    )

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { activeTab = "CÁMARA" },
                        colors = ButtonDefaults.textButtonColors(contentColor = IndBluePrimary)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("USAR CÁMARA", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val input = rawText.trim()
                            if (input.isBlank()) {
                                errorMessage = "Por favor, introduce o selecciona un código QR válido."
                                return@Button
                            }

                            // Extract OC
                            var extractedOc = ""
                            if (input.startsWith("OC:")) {
                                val pieces = input.split("|")
                                for (p in pieces) {
                                    if (p.trim().startsWith("OC:")) {
                                        extractedOc = p.substringAfter("OC:").trim()
                                    }
                                }
                            } else if (input.contains("-")) {
                                extractedOc = input.split("-").first().trim()
                            } else {
                                extractedOc = input
                            }

                            val exists = records.any { it.oc.equals(extractedOc, ignoreCase = true) }
                            if (exists) {
                                onQrScanned(extractedOc)
                            } else {
                                errorMessage = "Lote con Folio OC '$extractedOc' no existe en el sistema local offline."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("qr_scanner_submit")
                    ) {
                        Text("PROCESAR LOTE", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("CANCELAR", color = IndSteelSlate)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}
