package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.QualityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: QualityViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val globalLogs by viewModel.allAuditLogs.collectAsState()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsState()

    var userNameInput by remember { mutableStateOf(currentUser) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    var rawCsvPaste by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesa de Control y Ajustes", fontWeight = FontWeight.Bold) },
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
            
            // SECTION 1: AUTHENTICATED USER (SESIÓN DE AUDITOR)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = IndBluePrimary
                            )
                            Text(
                                "PERFIL DE INGENIERÍA / CALIDAD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Nombre: $currentUser",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Correo: $currentUserEmail",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Salió voluntariamente del sistema de auditoría", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("logout_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = IndAlertRed)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CERRAR SESIÓN (SIGN OUT)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 2: IMPORT & EXPORT EXCEL (CSV)
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
                            "INTERCAMBIO DE DATOS DE CALIDAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Compatibilidad con MS Excel mediante archivos formateados CSV conforme a ISO-9001.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // EXPORT EXCEL
                            Button(
                                onClick = { showExportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("export_excel_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                            ) {
                                Icon(Icons.Default.UploadFile, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar CSV", fontWeight = FontWeight.Bold)
                            }

                            // IMPORT EXCEL
                            Button(
                                onClick = { showImportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("import_excel_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Icon(Icons.Default.DownloadForOffline, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importar CSV", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 2.5: FIREBASE INTEGRATION & REMOTE SYNCHRONIZATION
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = IndBluePrimary
                            )
                            Text(
                                "RESPALDO Y COMPARTICIÓN EN LA NUBE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Sincroniza expedientes de inspección locales con Google Firebase Cloud Firestore para acceso compartido en tiempo real por otros supervisores y gerentes de planta.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IndBlueLight, RoundedCornerShape(8.dp))
                                .border(1.dp, IndBluePrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = IndBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ESTADO DE COPIAS DE SEGURIDAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IndDarkBg.copy(alpha = 0.6f))
                                Text(
                                    text = firebaseSyncStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IndBluePrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = {
                                viewModel.triggerFirebaseSync()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("firebase_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                        ) {
                            Icon(Icons.Default.Sync, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FORZAR RESPALDO A FIRESTORE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // SECTION 3: SYSTEM AUDIT LOGS TIMELINE
            item {
                Text(
                    text = "HISTORIAL AUDITORÍA GLOBAL PLANTA (${globalLogs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            if (globalLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se han registrado auditorías de calidad todavía.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(globalLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("log_timeline_card"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, IndCargoGrey),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            // Circle action indicator
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
                                    .padding(top = 2.dp)
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(circleColor)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.action.uppercase()} (Folio: ${log.oc})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val formattedTime = remember {
                                        SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                    }
                                    Text(
                                        text = formattedTime,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    text = "Operador: ${log.user}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = log.details,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // EXPORT EXCEL DIALOG SHOWCASE
    if (showExportDialog) {
        val csvText = viewModel.getCsvContentOfRecords()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar Registros a Excel") },
            text = {
                Column {
                    Text(
                        "Se compilaron un total de ${allRecords.size} registros del sistema a formato CSV (Excel). Puede copiarlo al portapapeles:",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = csvText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxSize(),
                            maxLines = 15,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(csvText))
                        Toast.makeText(context, "Archivo CSV copiado al portapapeles. Listo para pegar en Excel.", Toast.LENGTH_LONG).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // IMPORT EXCEL DIALOG SHOWCASE
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Registros desde CSV / Excel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pega el texto con formato CSV o haz clic en 'Ejemplos de Planta' para simular una carga masiva desde un servidor de manufactura:",
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = rawCsvPaste,
                        onValueChange = { rawCsvPaste = it },
                        placeholder = { Text("Escriba o pegue filas CSV de registros de calidad...", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(4.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    )

                    Button(
                        onClick = {
                            // Preload realistic industrial data of 5 samples
                            rawCsvPaste = """
OC,Part Number,Quantity,Warehouse,Location,Non Conformance,Operator Stamp,Operator Name,Delivered By,Reject Type,Status,Created By,Modified By,Created At,Modified At,Disp Type,Disp Area/Supplier,Disp Worked Already,Why 1,Why 2,Why 3,Why 4,Why 5,Root Cause,Details,Corrective Actions,Effective,Cont Qty Prod,Cont Qty Qual,Cont Good Qty,Cont Bad Qty
OC-70014,M-900223,380,Almacén Recepción,Rack-D2,Porosidad en Fundición,OP-202,Eduardo Martínez,Supervisor Fundición,Proceso,Terminado,Inspector_QC,Inspector_QC,1781151603218,1781151662170,Scrap,null,false,¿Por qué falló?,¿Por qué porosidad?,¿Por qué alta temp?,,,"Falla en termotemplado",,"Re-ajustar enfriadores térmicos",true,380,0,10,370
OC-10928,E-112044,15,Piso de Producción,Rack-A1,Defecto Dimensional,OP-101,Juan Pérez,Calidad Torno,Proceso,Abierto,Supervisor_QC,Supervisor_QC,1781151603218,1781151603218
OC-82193,P-456012,120,Almacén WIP,Rack-C1,Gubia / Rayón Profundo,OP-303,María Rodríguez,Línea Ensamble 2,Cliente interno,En Proceso,Inspector_QC,Inspector_QC,1781151603218,1781151680190,Retrabajo,Proveedor interno,false,¿Por qué rayado?,¿Por qué fricción?,¿Por qué sin empaque?,,,"Falta de cartón protector",,"Agregar espuma protectora",false,0,120,0,0
                            """.trimIndent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Precargar Ejemplos de Planta")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawCsvPaste.isBlank()) {
                            Toast.makeText(context, "Ingrese texto CSV válido primero", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        viewModel.importFromCsv(
                            csvText = rawCsvPaste,
                            onSuccess = { count ->
                                Toast.makeText(context, "Sincronizado: Se importaron $count registros correctamente", Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                rawCsvPaste = ""
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error de parsing: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndBluePrimary)
                ) {
                    Text("Sincronizar Lote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
