package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.QualityViewModel
import com.example.ui.components.RealCameraPhotoCapture
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecordScreen(
    viewModel: QualityViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Autocomplete sources from viewmodel and database
    val partSuggestions by viewModel.partNumberSuggestions.collectAsState()
    val warehouseSuggestions by viewModel.warehouseSuggestions.collectAsState()
    val locationSuggestions by viewModel.locationSuggestions.collectAsState()
    val nonConformanceSuggestions by viewModel.nonConformanceSuggestions.collectAsState()

    // Form states
    var oc by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var warehouse by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var nonConformance by remember { mutableStateOf("") }
    var operatorStamp by remember { mutableStateOf("") }
    var deliveredBy by remember { mutableStateOf("") }
    var rejectType by remember { mutableStateOf("Proceso") } // "Proceso" or "Cliente interno"
    val photos = remember { mutableStateListOf<String>() } // Max 5 image descriptions/mock paths

    // Dynamic Operator assignment
    val assignedOperatorName = remember(operatorStamp) {
        viewModel.getOperatorNameForStamp(operatorStamp)
    }

    // Camera and Photo Selections State
    var showCameraSimDialog by remember { mutableStateOf(false) }
    var showRealCameraCapture by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Rechazo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // SECTION 1: UNIQUE IDENTIFIER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, IndCargoGrey),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "FOLIO E IDENTIFICACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = oc,
                            onValueChange = { oc = it },
                            label = { Text("Orden de Compra / Folio OC") },
                            placeholder = { Text("Ej. OC-90812") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("oc_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = IndBluePrimary,
                                unfocusedBorderColor = IndCargoGrey
                            ),
                            leadingIcon = { Icon(Icons.Default.Pin, null) }
                        )
                    }
                }
            }

            // SECTION 2: MATERIAL DESCRIPTION & QUANTITY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, IndCargoGrey),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "DETALLE DEL MATERIAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Part Number AutoComplete
                        AutocompleteField(
                            value = partNumber,
                            onValueChange = { partNumber = it },
                            label = "Número de Parte (Part Number)",
                            suggestions = partSuggestions,
                            modifier = Modifier.testTag("part_number_input_field"),
                            leadingIcon = Icons.Default.Inventory2
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quantity Input
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Cantidad Rechazada (pzs)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quantity_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = IndBluePrimary,
                                unfocusedBorderColor = IndCargoGrey
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Numbers, null) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Warehouse AutoComplete
                        AutocompleteField(
                            value = warehouse,
                            onValueChange = { warehouse = it },
                            label = "Almacén de Ubicación",
                            suggestions = warehouseSuggestions,
                            modifier = Modifier.testTag("warehouse_input_field"),
                            leadingIcon = Icons.Default.HomeWork
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Location AutoComplete
                        AutocompleteField(
                            value = location,
                            onValueChange = { location = it },
                            label = "Localización / Rack",
                            suggestions = locationSuggestions,
                            modifier = Modifier.testTag("location_input_field"),
                            leadingIcon = Icons.Default.GridOn
                        )
                    }
                }
            }

            // SECTION 3: REJECTION ROOT INFO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, IndCargoGrey),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "INFO DE NO CONFORMIDAD QC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Reject Type Selector
                        Text("Tipo de Rechazo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Proceso", "Cliente interno").forEach { type ->
                                val isSelected = rejectType == type
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { rejectType = type },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = isSelected, onClick = { rejectType = type })
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = type, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // NonConformance AutoComplete
                        AutocompleteField(
                            value = nonConformance,
                            onValueChange = { nonConformance = it },
                            label = "No Conformidad / Defecto Detectado",
                            suggestions = nonConformanceSuggestions,
                            modifier = Modifier.testTag("non_conformance_input_field"),
                            leadingIcon = Icons.Default.ReportProblem
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Delivered By
                        OutlinedTextField(
                            value = deliveredBy,
                            onValueChange = { deliveredBy = it },
                            label = { Text("Quien Entrega el Material") },
                            placeholder = { Text("Ej. Supervisor Ensamble A") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = IndBluePrimary,
                                unfocusedBorderColor = IndCargoGrey
                            ),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                    }
                }
            }

            // SECTION 4: OPERATOR STAMP MAPPER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, IndCargoGrey),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "RESPONSABLE DE OPERACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = operatorStamp,
                            onValueChange = { operatorStamp = it },
                            label = { Text("Estampa del Operador (Código)") },
                            placeholder = { Text("Escriba OP-101, OP-202...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("operator_stamp_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = IndBluePrimary,
                                unfocusedBorderColor = IndCargoGrey
                            ),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Badge, null) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Immediate Operator Assignment FeedBack CARD
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContactPage, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Operador Auto-Asignado:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    Text(
                                        text = assignedOperatorName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: CAPTURE PHOTOS (MAX 5)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, IndCargoGrey),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "REGISTRO FOTOGRÁFICO DEFECTOS (${photos.size}/5)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )

                            if (photos.size < 5) {
                                Button(
                                    onClick = { showPhotoSourceDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CÁMARA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (photos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.NoPhotography, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Sin fotos capturadas. Agregue evidencia técnica.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(photos) { index, item ->
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(IndBluePrimary)
                                    ) {
                                        if (item.startsWith("/") || item.startsWith("file://") || item.startsWith("content://")) {
                                            AsyncImage(
                                                model = File(item),
                                                contentDescription = "Evidencia foto real",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.15f))
                                            )
                                        } else {
                                            // Draw technical mockup visual inside box representing photo
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.8f))
                                                    .padding(4.dp)
                                            ) {
                                                // Mock grid overlay
                                                Column {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("[#${index+1} DEV-CAM]", color = Color.White.copy(alpha = 0.4f), fontSize = 7.sp)
                                                        Text("EV-QC", color = IndAlertOrange, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = item,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                                        maxLines = 3
                                                    )
                                                }

                                                // Crosshair badge center
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .align(Alignment.Center)
                                                ) {
                                                    Icon(
                                                        Icons.Default.CenterFocusStrong,
                                                        null,
                                                        tint = IndAlertRed.copy(alpha = 0.7f),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }

                                        // Delete icon
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 4.dp, end = 4.dp)
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Red)
                                                .clickable { photos.removeAt(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BUTTON SAVE
            item {
                Button(
                    onClick = {
                        val qty = quantityStr.toIntOrNull() ?: 0
                        viewModel.createRecord(
                            oc = oc,
                            partNumber = partNumber,
                            quantity = qty,
                            warehouse = warehouse,
                            location = location,
                            nonConformance = nonConformance,
                            operatorStamp = operatorStamp,
                            deliveredBy = deliveredBy,
                            rejectType = rejectType,
                            photos = photos.toList(),
                            onSuccess = {
                                Toast.makeText(context, "Folio QC registrado exitosamente", Toast.LENGTH_LONG).show()
                                onNavigateBack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_record_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDAR Y CONFIRMAR RECHAZO", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.5.sp)
                }
            }
        }
    }

    // Camera defect selector simulator dialog
    if (showCameraSimDialog) {
        val defects = listOf(
            "Fisura Mecánica Externa",
            "Gubia en Rosca Interna",
            "Porosidad Exceso Fundido",
            "Golpe Estructural Esquina",
            "Terminado Rugosidad fuera Plano",
            "Etiqueta de Número de Parte Errónea"
        )
        AlertDialog(
            onDismissRequest = { showCameraSimDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, null, tint = IndBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulador de Cámara Calidad", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Seleccione la falla visual detectada en el material:", fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
                    defects.forEach { defectItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    photos.add(defectItem)
                                    showCameraSimDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = IndAlertRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(defectItem, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCameraSimDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Choose between simulated and live photos
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, null, tint = IndBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Soporte Fotográfico", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Seleccione el método de registro para evidenciar los defectos conforme a la norma ISO-9001:", fontSize = 13.sp)
                    
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            showRealCameraCapture = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("USAR CÁMARA REAL (MÓVIL)")
                    }

                    OutlinedButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            showCameraSimDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IndBluePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoMode, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMULAR DEFECTO OFFLINE")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("CANCELAR", color = IndSteelSlate)
                }
            }
        )
    }

    if (showRealCameraCapture) {
        AlertDialog(
            onDismissRequest = { showRealCameraCapture = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    RealCameraPhotoCapture(
                        onDismiss = { showRealCameraCapture = false },
                        onPhotoCaptured = { filePath ->
                            photos.add(filePath)
                            showRealCameraCapture = false
                        }
                    )
                }
            }
        )
    }
}

// Custom autocomplete selection dropdown text fields helper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) suggestions else suggestions.filter { it.contains(value, ignoreCase = true) }
    }

        Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = IndBluePrimary,
                unfocusedBorderColor = IndCargoGrey
            ),
            leadingIcon = { Icon(leadingIcon, null) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null)
                }
            }
        )

        if (expanded && filteredSuggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                filteredSuggestions.take(5).forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            onValueChange(selection)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
