@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditLog
import com.example.model.MaterialRecord
import com.example.ui.theme.*
import com.example.viewmodel.QualityViewModel
import com.example.ui.components.RealCameraPhotoCapture
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    oc: String,
    viewModel: QualityViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val recordState = viewModel.allRecords.collectAsState()
    val record = recordState.value.find { it.oc == oc }

    val auditLogsState = viewModel.getAuditLogsForRecord(oc).collectAsState(initial = emptyList())

    // Currently logged-in operator
    val currentUser by viewModel.currentUser.collectAsState()

    // Form inputs backed by active database values
    if (record == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Buscando Folio $oc...")
            }
        }
        return
    }

    var activeTab by remember { mutableStateOf("Disposición") } // "Disposición", "5 Whys", "Acciones", "Contención", "Historial"
    var showPrintDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = IndAlertRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar Expediente de Calidad", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Estás completamente seguro de eliminar el expediente con Folio OC '${record.oc}'? Esta acción es definitiva y purgará los registros de auditoría asociados en planta.",
                    fontSize = 12.sp,
                    color = IndSteelSlate
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteRecord(record) {
                            Toast.makeText(context, "Registro de material rechazado purgado", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndAlertRed)
                ) {
                    Text("SÍ, ELIMINAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CONSERVAR", color = IndSteelSlate)
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expediente: ${record.oc}", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        com.example.util.PdfPrintHelper.generateAndPreviewIndividualPdf(context, record, doPrint = true)
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir boleta física", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        com.example.util.PdfPrintHelper.generateAndPreviewIndividualPdf(context, record, doPrint = false)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Previsualizar boleta PDF", tint = Color(0xFFE11D48))
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_record_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Lote", tint = IndAlertRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            
            // HEADER BANNER (OC details & Real-time recalculated Status)
            HeaderBanner(record = record, viewModel = viewModel, context = context)

            // WORKSPACE NAVIGATION TABS
            WorkspaceTabs(
                activeTab = activeTab,
                onTabChanged = { activeTab = it },
                record = record
            )

            // TAB WORKSPACE CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "Disposición" -> DispositionWorkspace(record, viewModel)
                    "5 Whys" -> FiveWhysWorkspace(record, viewModel)
                    "Acciones" -> ActionsWorkspace(record, viewModel)
                    "Contención" -> ContentionWorkspace(record, viewModel)
                    "Historial" -> AuditTimelineWorkspace(auditLogsState.value)
                }
            }
        }
    }

    // LABEL PRINT DIALOG MOCK UP
    if (showPrintDialog) {
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, null, tint = IndBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Etiqueta Física Impresa / PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TARJETA DE RECHAZO DE CALIDAD",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "----------------------------------",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FOLIO OC: ${record.oc}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("ESTADO: ${record.status}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("P/N: ${record.partNumber}", color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("CANT: ${record.quantity} PZS", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UBIC: ${record.warehouse}-${record.location}", color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("RECH: ${record.rejectType}", color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "FALLA DETECTADA:\n${record.nonConformance}",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Technical canvas QR code
                        CleanQrCode(
                            content = "OC:${record.oc}|PN:${record.partNumber}|QTY:${record.quantity}",
                            modifier = Modifier.size(110.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Generado por: ${record.createdBy}",
                            fontSize = 8.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Fecha impresión: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                            fontSize = 8.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.util.PdfPrintHelper.generateAndPreviewIndividualPdf(context, record, doPrint = true)
                        showPrintDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                ) {
                    Text("Imprimir Etiqueta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrintDialog = false }) {
                    Text("Fila local")
                }
            }
        )
    }
}

@Composable
fun HeaderBanner(
    record: MaterialRecord,
    viewModel: QualityViewModel,
    context: android.content.Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IndCargoGrey),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.oc,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusIndicator(record.status)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Parte: ${record.partNumber} • Cant: ${record.quantity} pzs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No Conformidad: ${record.nonConformance}",
                        fontSize = 12.sp,
                        color = IndAlertRed,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Responsable: ${record.operatorName} (${record.operatorStamp})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Canvas drawn QR displaying physical values
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CleanQrCode(
                        content = "OC:${record.oc}|Part:${record.partNumber}|Qty:${record.quantity}",
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("OC QR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // EVIDENCE GALLERY AREA WITH ADD & DELETE ATTACHMENTS CAPABILITIES (Satisfies "deleting files")
            HorizontalDivider(color = IndCargoGrey.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PhotoCamera, null, tint = IndBluePrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "ARCHIVOS FOTOGRÁFICOS DE EVIDENCIA (${record.photos.size}/5)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    if (record.photos.size < 5) {
                        var showPhotoSourceDialogDetail by remember { mutableStateOf(false) }
                        var showRealCameraCaptureDetail by remember { mutableStateOf(false) }
                        var showAddPhotoDialog by remember { mutableStateOf(false) }
                        var newPhotoDesc by remember { mutableStateOf("") }
                        
                        TextButton(
                            onClick = { showPhotoSourceDialogDetail = true },
                            modifier = Modifier.testTag("add_photo_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showPhotoSourceDialogDetail) {
                            AlertDialog(
                                onDismissRequest = { showPhotoSourceDialogDetail = false },
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
                                                showPhotoSourceDialogDetail = false
                                                showRealCameraCaptureDetail = true
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
                                                showPhotoSourceDialogDetail = false
                                                showAddPhotoDialog = true
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
                                    TextButton(onClick = { showPhotoSourceDialogDetail = false }) {
                                        Text("CANCELAR", color = IndSteelSlate)
                                    }
                                }
                            )
                        }

                        if (showRealCameraCaptureDetail) {
                            AlertDialog(
                                onDismissRequest = { showRealCameraCaptureDetail = false },
                                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                                content = {
                                    Card(
                                        modifier = Modifier.fillMaxSize().background(Color.Black),
                                        shape = RoundedCornerShape(0.dp)
                                    ) {
                                        RealCameraPhotoCapture(
                                            onDismiss = { showRealCameraCaptureDetail = false },
                                            onPhotoCaptured = { filePath ->
                                                val updatedList = record.photos.toMutableList()
                                                updatedList.add(filePath)
                                                val updatedRecord = record.copy(photos = updatedList)
                                                viewModel.updateRecordFields(
                                                    record = updatedRecord,
                                                    actionName = "Edición de Evidencia",
                                                    actionDetails = "Se cargó un nuevo soporte fotográfico real: ${filePath}",
                                                    onSuccess = {
                                                        Toast.makeText(context, "Evidencia fotográfica real capturada", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                showRealCameraCaptureDetail = false
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        
                        if (showAddPhotoDialog) {
                            AlertDialog(
                                onDismissRequest = { showAddPhotoDialog = false },
                                title = { Text("Adjuntar Archivo de Evidencia", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Describe el defecto detectado para registrar el archivo de soporte fotográfico:", fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = newPhotoDesc,
                                            onValueChange = { newPhotoDesc = it },
                                            placeholder = { Text("Ej. Fractura_por_estres.jpg") },
                                            modifier = Modifier.fillMaxWidth().testTag("new_photo_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (newPhotoDesc.isNotBlank()) {
                                                val updatedList = record.photos.toMutableList()
                                                updatedList.add(newPhotoDesc.trim())
                                                val updatedRecord = record.copy(photos = updatedList)
                                                viewModel.updateRecordFields(
                                                    record = updatedRecord,
                                                    actionName = "Edición de Evidencia",
                                                    actionDetails = "Se cargó un nuevo soporte fotográfico: ${newPhotoDesc.trim()}",
                                                    onSuccess = {
                                                        Toast.makeText(context, "Archivo de evidencia cargado", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                newPhotoDesc = ""
                                                showAddPhotoDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                                    ) {
                                        Text("GUARDAR")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddPhotoDialog = false }) {
                                        Text("CANCELAR", color = IndSteelSlate)
                                    }
                                },
                                containerColor = Color.White
                            )
                        }
                    }
                }
                
                if (record.photos.isEmpty()) {
                    Text(
                        text = "Sin evidencias fotográficas adjuntas. Presiona 'Añadir' para capturar soporte visual conforme a ISO-9001.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        record.photos.forEachIndexed { idx, pDesc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IndBlueLight, RoundedCornerShape(8.dp))
                                    .border(1.dp, IndBluePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (pDesc.startsWith("/") || pDesc.startsWith("file://") || pDesc.startsWith("content://")) {
                                        Card(
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            AsyncImage(
                                                model = File(pDesc),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Imagen_Real_${idx + 1}.jpg",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = IndDarkBg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, null, tint = IndBluePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = pDesc,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IndDarkBg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar evidencia de foto",
                                    tint = IndAlertRed,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            val updatedList = record.photos.toMutableList()
                                            updatedList.removeAt(idx)
                                            val updatedRecord = record.copy(photos = updatedList)
                                            viewModel.updateRecordFields(
                                                record = updatedRecord,
                                                actionName = "Edic. Evidencia",
                                                actionDetails = "Se eliminó el soporte de evidencia: $pDesc",
                                                onSuccess = {
                                                    Toast.makeText(context, "Archivo eliminado de forma definitiva", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                        .testTag("delete_photo_${idx}")
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
fun WorkspaceTabs(
    activeTab: String,
    onTabChanged: (String) -> Unit,
    record: MaterialRecord
) {
    val items = listOf(
        Quadruple("Disposición", "DISPO", record.isDispositionComplete(), "disposition_tab"),
        Quadruple("5 Whys", "5 WHYS", record.isFiveWhysComplete(), "five_whys_tab"),
        Quadruple("Acciones", "PLAN", record.isCorrectiveActionsComplete(), "actions_tab"),
        Quadruple("Contención", "CONT", record.isContentionComplete(), "contention_tab"),
        Quadruple("Historial", "LOGS", true, "history_tab")
    )

    ScrollableTabRow(
        selectedTabIndex = items.map { it.first }.indexOf(activeTab).coerceAtLeast(0),
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { (label, shortLabel, isDone, tag) ->
            val isSelected = activeTab == label
            Tab(
                selected = isSelected,
                onClick = { onTabChanged(label) },
                modifier = Modifier.testTag(tag)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shortLabel,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (!label.equals("Historial", ignoreCase = true)) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isDone) IndSuccessGreen else IndSteelSlate,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// 1. DISPOSITION AREA WORKSPACE
@Composable
fun DispositionWorkspace(record: MaterialRecord, viewModel: QualityViewModel) {
    val context = LocalContext.current
    val types = listOf("Retrabajo", "Scrap", "Usar así", "Retorno a proveedor")
    
    var selectedType by remember(record) { mutableStateOf(record.dispositionType) }
    var selectedAreaOrSupplier by remember(record) { mutableStateOf(record.dispositionAreaOrSupplier) } // "Área" or "Proveedor interno"
    var selectedWorkedAlready by remember(record) { mutableStateOf(record.dispositionWorkedAlready) } // Boolean

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(selectedType, selectedAreaOrSupplier, selectedWorkedAlready) {
        if (selectedType == record.dispositionType &&
            selectedAreaOrSupplier == record.dispositionAreaOrSupplier &&
            selectedWorkedAlready == record.dispositionWorkedAlready
        ) {
            return@LaunchedEffect
        }
        isSaving = true
        delay(1200)
        val updatedRecord = record.copy(
            dispositionType = selectedType,
            dispositionAreaOrSupplier = selectedAreaOrSupplier,
            dispositionWorkedAlready = selectedWorkedAlready
        )
        viewModel.updateRecordFields(
            record = updatedRecord,
            actionName = "Autoguardado Disposición",
            actionDetails = "Cambio automático de disposición a: ${selectedType}" + 
                    if (selectedType == "Retrabajo") " en $selectedAreaOrSupplier" else ""
        )
        isSaving = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, IndCargoGrey), RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("I. Disposición del Material Rechazado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isSaving) Icons.Default.Sync else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (isSaving) IndBluePrimary else IndSuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isSaving) "Guardando..." else "Autoguardado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSaving) IndBluePrimary else IndSuccessGreen
                )
            }
        }
        Text("Defina la resolución final de la calidad para este material. Se actualizará la trazabilidad.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        // Large Cards Radio selector
        types.forEach { type ->
            val isSelected = selectedType == type
            Card(
                onClick = {
                    selectedType = type
                    if (type != "Retrabajo") {
                        selectedAreaOrSupplier = null
                        selectedWorkedAlready = false
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { selectedType = type })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(type, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Sub question if retrabajo selected
        if (selectedType == "Retrabajo") {
            Spacer(modifier = Modifier.height(4.dp))
            Text("¿Dónde se realizará el retrabajo?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Área", "Proveedor interno").forEach { loc ->
                    val isLocSec = selectedAreaOrSupplier == loc
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedAreaOrSupplier = loc
                                if (loc == "Área") selectedWorkedAlready = true
                             },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLocSec) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            RadioButton(selected = isLocSec, onClick = {
                                selectedAreaOrSupplier = loc
                                if (loc == "Área") selectedWorkedAlready = true
                            })
                            Text(loc, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Supplier sub-question
            if (selectedAreaOrSupplier == "Proveedor interno") {
                Spacer(modifier = Modifier.height(4.dp))
                Text("¿El material ya fue retrabajado satisfactoriamente?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Sí" to true, "No" to false).forEach { (caption, isYes) ->
                        val isSel = selectedWorkedAlready == isYes
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedWorkedAlready = isYes },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = caption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val updatedRecord = record.copy(
                    dispositionType = selectedType,
                    dispositionAreaOrSupplier = selectedAreaOrSupplier,
                    dispositionWorkedAlready = selectedWorkedAlready
                )
                viewModel.updateRecordFields(
                    record = updatedRecord,
                    actionName = "Disposición",
                    actionDetails = "Usuario definió la disposición como ${selectedType}" + 
                            if (selectedType == "Retrabajo") " en $selectedAreaOrSupplier (Trabajado: $selectedWorkedAlready)" else ""
                )
                Toast.makeText(context, "Disposición de calidad guardada", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_disposition_button"),
            colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("GUARDAR DISPOSICIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

// 2. 5 WHYS WORKSPACE
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiveWhysWorkspace(record: MaterialRecord, viewModel: QualityViewModel) {
    val context = LocalContext.current
    var why1 by remember(record) { mutableStateOf(record.why1) }
    var why2 by remember(record) { mutableStateOf(record.why2) }
    var why3 by remember(record) { mutableStateOf(record.why3) }
    var why4 by remember(record) { mutableStateOf(record.why4) }
    var why5 by remember(record) { mutableStateOf(record.why5) }
    var rootCause by remember(record) { mutableStateOf(record.rootCause) }
    var details by remember(record) { mutableStateOf(record.details) }

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(why1, why2, why3, why4, why5, rootCause, details) {
        if (why1 == record.why1 &&
            why2 == record.why2 &&
            why3 == record.why3 &&
            why4 == record.why4 &&
            why5 == record.why5 &&
            rootCause == record.rootCause &&
            details == record.details
        ) {
            return@LaunchedEffect
        }
        isSaving = true
        delay(1500)
        val updatedRecord = record.copy(
            why1 = why1,
            why2 = why2,
            why3 = why3,
            why4 = why4,
            why5 = why5,
            rootCause = rootCause,
            details = details
        )
        viewModel.updateRecordFields(
            record = updatedRecord,
            actionName = "Autoguardado 5 Whys",
            actionDetails = "Causa raíz guardada automáticamente de forma asíncrona"
        )
        isSaving = false
    }

    val answeredCount = remember(why1, why2, why3, why4, why5) {
        var count = 0
        if (why1.isNotBlank()) count++
        if (why2.isNotBlank()) count++
        if (why3.isNotBlank()) count++
        if (why4.isNotBlank()) count++
        if (why5.isNotBlank()) count++
        count
    }

    val isValid = answeredCount >= 3 && rootCause.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, IndCargoGrey), RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("II. Análisis Causa Raíz (5 Whys)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSaving) Icons.Default.Sync else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isSaving) IndBluePrimary else IndSuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isSaving) "Guardando..." else "Autoguardado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSaving) IndBluePrimary else IndSuccessGreen
                    )
                }
            }
            Text(
                text = "Metodología Industrial. Mínimo 3 preguntas contestadas y Causa Raíz son obligatorios para validar esta sección.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isValid) IndSuccessGreen.copy(alpha = 0.1f) else IndAlertRed.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isValid) IndSuccessGreen else IndAlertRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Respuestas: $answeredCount/3 mín. • Causa Raíz: " + if (rootCause.isNotBlank()) "Lleno" else "Pendiente",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isValid) IndSuccessGreen else IndAlertRed
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            OutlinedTextField(
                value = why1,
                onValueChange = { why1 = it },
                label = { Text("1. ¿Por qué ocurrió el defecto? (Requerido)") },
                modifier = Modifier.fillMaxWidth().testTag("why1_input_field"),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = why2,
                onValueChange = { why2 = it },
                label = { Text("2. ¿Por qué se generó? (Requerido)") },
                modifier = Modifier.fillMaxWidth().testTag("why2_input_field"),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = why3,
                onValueChange = { why3 = it },
                label = { Text("3. ¿Por qué no se contuvo antes? (Requerido)") },
                modifier = Modifier.fillMaxWidth().testTag("why3_input_field"),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = why4,
                onValueChange = { why4 = it },
                label = { Text("4. ¿Por qué faltó control? (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = why5,
                onValueChange = { why5 = it },
                label = { Text("5. ¿Por qué falló el proceso raíz? (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = rootCause,
                onValueChange = { rootCause = it },
                label = { Text("Causa Raíz Final (Obligatorio)") },
                placeholder = { Text("Describa el factor técnico/operativo final...") },
                modifier = Modifier.fillMaxWidth().testTag("root_cause_input_field"),
                shape = RoundedCornerShape(6.dp),
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Detalles del Hallazgo / Observaciones") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                minLines = 2
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val updatedRecord = record.copy(
                        why1 = why1,
                        why2 = why2,
                        why3 = why3,
                        why4 = why4,
                        why5 = why5,
                        rootCause = rootCause,
                        details = details
                    )
                    viewModel.updateRecordFields(
                        record = updatedRecord,
                        actionName = "Análisis 5 Whys",
                        actionDetails = "Análisis de causa raíz guardado. Causa: $rootCause"
                    )
                    Toast.makeText(context, "Análisis de Whys registrado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_5whys_button"),
                colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("GUARDAR ANÁLISIS 5 WHYS", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

// 3. ACTION PLAN WORKSPACE
@Composable
fun ActionsWorkspace(record: MaterialRecord, viewModel: QualityViewModel) {
    val context = LocalContext.current
    var correctiveActions by remember(record) { mutableStateOf(record.correctiveActions) }
    var actionEffective by remember(record) { mutableStateOf(record.actionEffective) } // Boolean

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(correctiveActions, actionEffective) {
        if (correctiveActions == record.correctiveActions &&
            actionEffective == record.actionEffective
        ) {
            return@LaunchedEffect
        }
        isSaving = true
        delay(1200)
        val updatedRecord = record.copy(
            correctiveActions = correctiveActions,
            actionEffective = actionEffective
        )
        viewModel.updateRecordFields(
            record = updatedRecord,
            actionName = "Autoguardado Acciones",
            actionDetails = "Cambio automático de acciones correctivas"
        )
        isSaving = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, IndCargoGrey), RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("III. Plan de Acciones Correctivas", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isSaving) Icons.Default.Sync else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (isSaving) IndBluePrimary else IndSuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isSaving) "Guardando..." else "Autoguardado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSaving) IndBluePrimary else IndSuccessGreen
                )
            }
        }
        Text("Describa las acciones tomadas para evitar recurrencia de este rechazo y verifique efectividad.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        OutlinedTextField(
            value = correctiveActions,
            onValueChange = { correctiveActions = it },
            label = { Text("Lista de Acciones Correctivas Aplicadas") },
            placeholder = { Text("Ej. Re-calibrar prensa, capacitar operador en plano técnico...") },
            modifier = Modifier.fillMaxWidth().testTag("corrective_actions_input_field"),
            shape = RoundedCornerShape(6.dp),
            minLines = 4
        )

        Text("¿Fue efectiva la acción correctiva en piso?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Sí - Validado" to true, "No - En Verificación" to false).forEach { (caption, state) ->
                val isSelected = actionEffective == state
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { actionEffective = state }
                        .testTag("action_effective_${state}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val updatedRecord = record.copy(
                    correctiveActions = correctiveActions,
                    actionEffective = actionEffective
                )
                viewModel.updateRecordFields(
                    record = updatedRecord,
                    actionName = "Acción Correctiva",
                    actionDetails = "Plan de acción correctiva definido. Efectiva: $actionEffective"
                )
                Toast.makeText(context, "Plan de Acción Correctiva guardado", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_corrective_actions_button"),
            colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("GUARDAR PLAN DE ACCIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

// 4. CONTENTION AND INVENTORY WORKSPACE
@Composable
fun ContentionWorkspace(record: MaterialRecord, viewModel: QualityViewModel) {
    val context = LocalContext.current
    var qtyProdStr by remember(record) { mutableStateOf(record.contentionQtyProd.toString()) }
    var qtyQualStr by remember(record) { mutableStateOf(record.contentionQtyQual.toString()) }
    var goodQtyStr by remember(record) { mutableStateOf(record.contentionGoodQty.toString()) }
    var badQtyStr by remember(record) { mutableStateOf(record.contentionBadQty.toString()) }

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(qtyProdStr, qtyQualStr, goodQtyStr, badQtyStr) {
        val qProd = qtyProdStr.toIntOrNull() ?: 0
        val qQual = qtyQualStr.toIntOrNull() ?: 0
        val gdQty = goodQtyStr.toIntOrNull() ?: 0
        val bdQty = badQtyStr.toIntOrNull() ?: 0
        if (qProd == record.contentionQtyProd &&
            qQual == record.contentionQtyQual &&
            gdQty == record.contentionGoodQty &&
            bdQty == record.contentionBadQty
        ) {
            return@LaunchedEffect
        }
        isSaving = true
        delay(1200)
        val updatedRecord = record.copy(
            contentionQtyProd = qProd,
            contentionQtyQual = qQual,
            contentionGoodQty = gdQty,
            contentionBadQty = bdQty
        )
        viewModel.updateRecordFields(
            record = updatedRecord,
            actionName = "Autoguardado Contención",
            actionDetails = "Cambio automático de cantidades de contención"
        )
        isSaving = false
    }

    val qtyProd = qtyProdStr.toIntOrNull() ?: 0
    val qtyQual = qtyQualStr.toIntOrNull() ?: 0
    val goodQty = goodQtyStr.toIntOrNull() ?: 0
    val badQty = badQtyStr.toIntOrNull() ?: 0

    val totalContained = qtyProd + qtyQual
    val totalResolved = goodQty + badQty
    val pendingContention = (totalContained - totalResolved).coerceAtLeast(0)

    val isDone = totalContained > 0 && pendingContention == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, IndCargoGrey), RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("IV. Plan de Contención Industrial", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isSaving) Icons.Default.Sync else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (isSaving) IndBluePrimary else IndSuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isSaving) "Guardando..." else "Autoguardado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSaving) IndBluePrimary else IndSuccessGreen
                )
            }
        }
        Text("Bloquee inventarios WIP en almacenes de planta para certificar la cantidad de material afectado.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        // Grid Inputs for Contained Qty
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = qtyProdStr,
                onValueChange = { qtyProdStr = it },
                label = { Text("Contenido Prod WIP") },
                modifier = Modifier.weight(1f).testTag("contention_prod_input"),
                shape = RoundedCornerShape(6.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = qtyQualStr,
                onValueChange = { qtyQualStr = it },
                label = { Text("Contenido Calidad") },
                modifier = Modifier.weight(1f).testTag("contention_qual_input"),
                shape = RoundedCornerShape(6.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        // Output Panel Pending
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isDone) IndSuccessGreen.copy(alpha = 0.12f) else IndAlertRed.copy(alpha = 0.12f)
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Contenido Encerrado: $totalContained pzs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Total Clasificado (B+M): $totalResolved pzs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PENDIENTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDone) IndSuccessGreen else IndAlertRed)
                    Text("$pendingContention pzs", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (isDone) IndSuccessGreen else IndAlertRed)
                }
            }
        }

        // Resolving Good vs Bad
        Text("Resultado de Clasificación (Sorting)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = goodQtyStr,
                onValueChange = { goodQtyStr = it },
                label = { Text("Buenas (Aceptadas)") },
                modifier = Modifier.weight(1f).testTag("contention_good_input"),
                shape = RoundedCornerShape(6.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = badQtyStr,
                onValueChange = { badQtyStr = it },
                label = { Text("Malas (Rechazo/Scrap)") },
                modifier = Modifier.weight(1f).testTag("contention_bad_input"),
                shape = RoundedCornerShape(6.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val updatedRecord = record.copy(
                    contentionQtyProd = qtyProd,
                    contentionQtyQual = qtyQual,
                    contentionGoodQty = goodQty,
                    contentionBadQty = badQty
                )
                viewModel.updateRecordFields(
                    record = updatedRecord,
                    actionName = "Contención",
                    actionDetails = "Inventario clasificado. Buenos: $goodQty, Malos: $badQty, Pendiente: $pendingContention"
                )
                Toast.makeText(context, "Contención registrada satisfactoriamente", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_contention_button"),
            colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("GUARDAR REGISTRO CONTENCIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

// 5. TIMELINE AUDIT HISTORY
@Composable
fun AuditTimelineWorkspace(logs: List<AuditLog>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Historial de cambios vacío", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, IndCargoGrey), RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Historial de Auditoría de Planta", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Text("Trazabilidad completa de modificaciones en tiempo real conforme a ISO-9001.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(logs) { log ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Column line drawing
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                    val circleColor = when (log.action) {
                        "Creado" -> IndBluePrimary
                        "Disposición" -> IndInfoBlue
                        "Análisis 5 Whys" -> IndAlertOrange
                        "Acción Correctiva" -> IndAlertRed
                        "Contención" -> IndSuccessGreen
                        else -> IndSteelSlate
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(circleColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Detail content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.action.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val logDate = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.timestamp)) }
                        Text(
                            text = logDate,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "Por: " + log.user,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = log.details,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    val color = when (status) {
        "Abierto" -> IndSteelSlate
        "En Proceso" -> IndAlertOrange
        "Terminado" -> IndSuccessGreen
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}

// Deterministic mock technical canvas QR code drawing tool (Pure Offline Kotlin Draw)
@Composable
fun CleanQrCode(content: String, modifier: Modifier = Modifier) {
    val isSystemDark = isSystemInDarkTheme()
    val tintColor = if (isSystemDark) Color.Black else Color.Black // Keep QR codes clean black for printing on white always
    
    val matrixSize = 21 // QR Version 1 grid size
    val qrModel = remember(content) {
        // Deterministic generation seeded by content hash
        val rnd = Random(content.hashCode().toLong())
        val grid = Array(matrixSize) { BooleanArray(matrixSize) }
        
        // Fill grid randomly
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                grid[r][c] = rnd.nextBoolean()
            }
        }

        // Apply anchor squares (top-left, top-right, bottom-left) in QR standard positions
        fun applyAnchor(sr: Int, sc: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val realR = sr + r
                    val realC = sc + c
                    if (realR < matrixSize && realC < matrixSize) {
                        // Outermost ring or center pixel is dark
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isCenter = r >= 2 && r <= 4 && c >= 2 && c <= 4
                        grid[realR][realC] = isBorder || isCenter
                    }
                }
            }
        }

        applyAnchor(0, 0) // Top-Left
        applyAnchor(0, matrixSize - 7) // Top-Right
        applyAnchor(matrixSize - 7, 0) // Bottom-Left

        grid
    }

    Card(
        modifier = modifier
            .border(3.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / matrixSize
            
            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (qrModel[r][c]) {
                        drawRect(
                            color = tintColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f) // overlapping prevent white hairlines
                        )
                    }
                }
            }
        }
    }
}

// Quadruple helper data container
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
// Triple helper data container
data class Triple<A, B, C>(val first: A, val second: B, val third: C)

fun savePdfReportToDownloads(context: android.content.Context, record: MaterialRecord, allRecords: List<MaterialRecord>) {
    // Obsolete - Migrated and fully optimized in com.example.util.PdfPrintHelper
}
