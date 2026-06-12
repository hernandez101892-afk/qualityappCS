package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.model.AuditLog
import com.example.model.MaterialRecord
import com.example.model.User
import com.example.data.repository.QualityRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QualityViewModel(private val repository: QualityRepository) : ViewModel() {

    // Authenticated User State
    private val _currentUser = MutableStateFlow("Supervisor QC")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("supervisor@qc.com")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false) // Start as false to force Login screen
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repository.getUserByEmail("supervisor@qc.com")
            if (existing == null) {
                repository.insertUser(User("supervisor@qc.com", "Supervisor QC", "123"))
            }
        }
    }

    // Database Flows
    val allRecords: StateFlow<List<MaterialRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter and UI State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String?>(null) // null = Todos
    val selectedStatusFilter: StateFlow<String?> = _selectedStatusFilter.asStateFlow()

    private val _selectedWarehouseFilter = MutableStateFlow<String?>(null)
    val selectedWarehouseFilter: StateFlow<String?> = _selectedWarehouseFilter.asStateFlow()

    private val _selectedNonConformanceFilter = MutableStateFlow<String?>(null)
    val selectedNonConformanceFilter: StateFlow<String?> = _selectedNonConformanceFilter.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow<String?>("Todos") // "Hoy", "Semana", "Mes", "Todos"
    val selectedDateFilter: StateFlow<String?> = _selectedDateFilter.asStateFlow()

    // Filtered Records flow for Dashboard & Lists
    @Suppress("UNCHECKED_CAST")
    val filteredRecords: StateFlow<List<MaterialRecord>> = combine(
        allRecords,
        _searchQuery,
        _selectedStatusFilter,
        _selectedWarehouseFilter,
        _selectedNonConformanceFilter,
        _selectedDateFilter
    ) { array ->
        val records = array[0] as List<MaterialRecord>
        val query = array[1] as String
        val status = array[2] as String?
        val warehouse = array[3] as String?
        val nonConf = array[4] as String?
        val dateRange = array[5] as String?

        records.filter { record ->
            val matchQuery = query.isBlank() || 
                    record.oc.contains(query, ignoreCase = true) || 
                    record.partNumber.contains(query, ignoreCase = true) || 
                    record.nonConformance.contains(query, ignoreCase = true)
            
            val matchStatus = status == null || record.status == status
            val matchWarehouse = warehouse == null || record.warehouse == warehouse
            val matchNonConf = nonConf == null || record.nonConformance == nonConf

            val matchDate = when (dateRange) {
                "Hoy" -> {
                    val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                    record.createdAt >= oneDayAgo
                }
                "Semana" -> {
                    val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                    record.createdAt >= oneWeekAgo
                }
                "Mes" -> {
                    val oneMonthAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000
                    record.createdAt >= oneMonthAgo
                }
                else -> true
            }

            matchQuery && matchStatus && matchWarehouse && matchNonConf && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAuditLogsForRecord(oc: String): Flow<List<AuditLog>> {
        return repository.getAuditLogsForRecord(oc)
    }

    // Setters for parameters
    private val _firebaseSyncStatus = MutableStateFlow("Listo para sincronizar")
    val firebaseSyncStatus: StateFlow<String> = _firebaseSyncStatus.asStateFlow()

    fun triggerFirebaseSync() {
        viewModelScope.launch {
            repository.syncAllLocalToCloudFirestore { progress ->
                _firebaseSyncStatus.value = progress
            }
        }
    }

    fun signUpUser(
        email: String,
        name: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || name.isBlank() || password.isBlank()) {
                onError("Todos los campos (email, nombre, contraseña) son obligatorios.")
                return@launch
            }
            if (!email.contains("@")) {
                onError("Por favor, introduce un correo electrónico válido.")
                return@launch
            }

            val existing = repository.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                onError("El correo electrónico ya está registrado localmente.")
                return@launch
            }

            val newUser = User(
                email = email.trim().lowercase(),
                name = name.trim(),
                passwordHash = password
            )
            repository.insertUser(newUser)

            // Dynamic Firebase Auth registration in background / online
            try {
                val auth = FirebaseAuth.getInstance()
                auth.createUserWithEmailAndPassword(newUser.email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val userMap = mapOf(
                                "email" to newUser.email,
                                "name" to newUser.name,
                                "createdAt" to newUser.createdAt
                            )
                            db.collection("users").document(newUser.email).set(userMap)
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.w("FirebaseSync", "Cloud authentication record deferred (Offline). Detail: ${e.message}")
            }
            
            // Log in automatically locally first (offline resilience)
            _currentUser.value = newUser.name
            _currentUserEmail.value = newUser.email
            _isUserLoggedIn.value = true
            onSuccess()
        }
    }

    fun signInUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                onError("Email y contraseña son obligatorios.")
                return@launch
            }

            val cleanEmail = email.trim().lowercase()

            // 1. Core Local validation
            val user = repository.getUserByEmail(cleanEmail)
            if (user != null && user.passwordHash == password) {
                _currentUser.value = user.name
                _currentUserEmail.value = user.email
                _isUserLoggedIn.value = true
                
                // Sync to Firebase Auth in background
                try {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(cleanEmail, password)
                } catch (e: Exception) { /* Silent */ }
                
                onSuccess()
                return@launch
            } else if (cleanEmail == "supervisor@qc.com" && password == "123") {
                val preU = User("supervisor@qc.com", "Supervisor QC", "123")
                repository.insertUser(preU)
                _currentUser.value = preU.name
                _currentUserEmail.value = preU.email
                _isUserLoggedIn.value = true
                
                try {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword("supervisor@qc.com", "123")
                } catch (e: Exception) { /* Silent */ }
                
                onSuccess()
                return@launch
            }

            // 2. Fallback to Remote Firebase Auth as authority
            try {
                val auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener { authResult ->
                        val onlineEmail = authResult.user?.email ?: cleanEmail
                        val onlineName = authResult.user?.displayName ?: onlineEmail.substringBefore("@")
                        
                        viewModelScope.launch {
                            val localU = User(onlineEmail, onlineName, password)
                            repository.insertUser(localU)
                            
                            _currentUser.value = localU.name
                            _currentUserEmail.value = localU.email
                            _isUserLoggedIn.value = true
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        onError("Credenciales incorrectas locales o error de Firebase Cloud: ${e.localizedMessage}")
                    }
            } catch (e: Exception) {
                onError("No es posible iniciar sesión remotamente/localmente: ${e.message}")
            }
        }
    }

    fun logout() {
        _currentUser.value = "Invitado"
        _currentUserEmail.value = ""
        _isUserLoggedIn.value = false
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) { /* Silent */ }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String?) {
        _selectedStatusFilter.value = status
    }

    fun setWarehouseFilter(warehouse: String?) {
        _selectedWarehouseFilter.value = warehouse
    }

    fun setNonConformanceFilter(nonConf: String?) {
        _selectedNonConformanceFilter.value = nonConf
    }

    fun setDateFilter(dateFilter: String?) {
        _selectedDateFilter.value = dateFilter ?: "Todos"
    }

    // Auto-complete suggestion generators from existing database records
    val partNumberSuggestions: StateFlow<List<String>> = allRecords.map { list ->
        list.map { it.partNumber }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouseSuggestions: StateFlow<List<String>> = allRecords.map { list ->
        val defaults = listOf("Almacén Recepción", "Almacén WIP", "Almacén PT", "Almacén Scrap", "Piso de Producción")
        (list.map { it.warehouse } + defaults).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationSuggestions: StateFlow<List<String>> = allRecords.map { list ->
        val defaults = listOf("Rack-A1", "Rack-B2", "Rack-C3", "Bodega-3", "Área Ensamble")
        (list.map { it.location } + defaults).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nonConformanceSuggestions: StateFlow<List<String>> = allRecords.map { list ->
        val defaults = listOf("Defecto Dimensional", "Porosidad en Fundición", "Contaminación", "Gubia / Rayón", "Falta Ensamblar", "Etiquetado Incorrecto")
        (list.map { it.nonConformance } + defaults).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto-maps Operator Name when stamp changes
    fun getOperatorNameForStamp(stamp: String): String {
        return repository.getOperatorNameForStamp(stamp)
    }

    // CREAR REGISTRO
    fun createRecord(
        oc: String,
        partNumber: String,
        quantity: Int,
        warehouse: String,
        location: String,
        nonConformance: String,
        operatorStamp: String,
        deliveredBy: String,
        rejectType: String,
        photos: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (oc.isBlank() || partNumber.isBlank() || quantity <= 0) {
                onError("OC, Part Number y Cantidad > 0 son campos requeridos")
                return@launch
            }

            val existing = repository.getRecordByOcDirect(oc)
            if (existing != null) {
                onError("El folio OC ($oc) ya se encuentra registrado")
                return@launch
            }

            val operatorName = getOperatorNameForStamp(operatorStamp)
            val timestamp = System.currentTimeMillis()
            val newRecord = MaterialRecord(
                oc = oc.trim().uppercase(),
                partNumber = partNumber.trim().uppercase(),
                quantity = quantity,
                warehouse = warehouse.trim(),
                location = location.trim(),
                nonConformance = nonConformance.trim(),
                operatorStamp = operatorStamp.trim().uppercase(),
                operatorName = operatorName,
                deliveredBy = deliveredBy.trim(),
                rejectType = rejectType,
                photos = photos,
                status = "Abierto",
                createdBy = currentUser.value,
                modifiedBy = currentUser.value,
                createdAt = timestamp,
                modifiedAt = timestamp
            )

            repository.insertRecord(
                record = newRecord,
                user = currentUser.value,
                actionName = "Creado",
                actionDetails = "Creación de registro para material rechazado con status Abierto"
            )
            onSuccess()
        }
    }

    // ACTUALIZAR REGISTRO COMPLETO (o parte de él)
    fun updateRecordFields(
        record: MaterialRecord,
        actionName: String,
        actionDetails: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = record.copy(
                modifiedBy = currentUser.value,
                modifiedAt = System.currentTimeMillis()
            )
            repository.updateRecord(updated, currentUser.value, actionName, actionDetails)
            onSuccess()
        }
    }

    // ELIMINAR REGISTRO
    fun deleteRecord(record: MaterialRecord, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteRecord(record, currentUser.value)
            onSuccess()
        }
    }

    // EXPORT TO TSV/CSV format compatible with Excel
    fun getCsvContentOfRecords(): String {
        val records = allRecords.value
        val header = listOf(
            "OC", "Part Number", "Quantity", "Warehouse", "Location", "Non Conformance", 
            "Operator Stamp", "Operator Name", "Delivered By", "Reject Type", "Status", 
            "Created By", "Modified By", "Created At", "Modified At",
            "Disp Type", "Disp Area/Supplier", "Disp Worked Already",
            "Why 1", "Why 2", "Why 3", "Why 4", "Why 5", "Root Cause", "Details",
            "Corrective Actions", "Effective", "Cont Qty Prod", "Cont Qty Qual", "Cont Good Qty", "Cont Bad Qty", "Cont Pending"
        ).joinToString(",")

        val rows = records.map { r ->
            listOf(
                escapeCsv(r.oc),
                escapeCsv(r.partNumber),
                r.quantity.toString(),
                escapeCsv(r.warehouse),
                escapeCsv(r.location),
                escapeCsv(r.nonConformance),
                escapeCsv(r.operatorStamp),
                escapeCsv(r.operatorName),
                escapeCsv(r.deliveredBy),
                escapeCsv(r.rejectType),
                escapeCsv(r.status),
                escapeCsv(r.createdBy),
                escapeCsv(r.modifiedBy),
                r.createdAt.toString(),
                r.modifiedAt.toString(),
                escapeCsv(r.dispositionType ?: ""),
                escapeCsv(r.dispositionAreaOrSupplier ?: ""),
                r.dispositionWorkedAlready.toString(),
                escapeCsv(r.why1),
                escapeCsv(r.why2),
                escapeCsv(r.why3),
                escapeCsv(r.why4),
                escapeCsv(r.why5),
                escapeCsv(r.rootCause),
                escapeCsv(r.details),
                escapeCsv(r.correctiveActions),
                r.actionEffective.toString(),
                r.contentionQtyProd.toString(),
                r.contentionQtyQual.toString(),
                r.contentionGoodQty.toString(),
                r.contentionBadQty.toString(),
                r.contentionPending.toString()
            ).joinToString(",")
        }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    // IMPORT FROM CSV
    fun importFromCsv(csvText: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvText.lines().filter { it.isNotBlank() }
                if (lines.size <= 1) {
                    onError("El archivo CSV no contiene registros para importar")
                    return@launch
                }

                // Custom parsing of simple comma separated string with quotation handling
                var importedCount = 0
                for (i in 1 until lines.size) {
                    val row = parseCsvRow(lines[i])
                    if (row.size < 11) continue // Must have basic fields
                    
                    val ocStr = row[0].trim().uppercase()
                    if (ocStr.isBlank()) continue

                    val part = row[1].trim().uppercase()
                    val qty = row[2].toIntOrNull() ?: 1
                    val wh = row[3]
                    val loc = row[4]
                    val nonC = row[5]
                    val stamp = row[6]
                    val opName = if (row[7].isNotBlank()) row[7] else getOperatorNameForStamp(stamp)
                    val delBy = row[8]
                    val rejType = if (row[9] == "Cliente interno") "Cliente interno" else "Proceso"
                    val statusText = row[10]

                    val remainingFieldsSize = row.size
                    
                    val dispType = if (remainingFieldsSize > 15 && row[15].isNotBlank()) row[15] else null
                    val dispArea = if (remainingFieldsSize > 16 && row[16].isNotBlank()) row[16] else null
                    val dispWorked = if (remainingFieldsSize > 17) row[17].toBoolean() else false
                    
                    val w1 = if (remainingFieldsSize > 18) row[18] else ""
                    val w2 = if (remainingFieldsSize > 19) row[19] else ""
                    val w3 = if (remainingFieldsSize > 20) row[20] else ""
                    val w4 = if (remainingFieldsSize > 21) row[21] else ""
                    val w5 = if (remainingFieldsSize > 22) row[22] else ""
                    val root = if (remainingFieldsSize > 23) row[23] else ""
                    val detail = if (remainingFieldsSize > 24) row[24] else ""
                    
                    val corrAct = if (remainingFieldsSize > 25) row[25] else ""
                    val eff = if (remainingFieldsSize > 26) row[26].toBoolean() else false
                    
                    val qP = if (remainingFieldsSize > 27) row[27].toIntOrNull() ?: 0 else 0
                    val qQ = if (remainingFieldsSize > 28) row[28].toIntOrNull() ?: 0 else 0
                    val qG = if (remainingFieldsSize > 29) row[29].toIntOrNull() ?: 0 else 0
                    val qB = if (remainingFieldsSize > 30) row[30].toIntOrNull() ?: 0 else 0

                    val record = MaterialRecord(
                        oc = ocStr,
                        partNumber = part,
                        quantity = qty,
                        warehouse = wh,
                        location = loc,
                        nonConformance = nonC,
                        operatorStamp = stamp,
                        operatorName = opName,
                        deliveredBy = delBy,
                        rejectType = rejType,
                        photos = emptyList(), // Photos cannot easily be recovered from raw CSV
                        status = statusText,
                        createdBy = currentUser.value,
                        modifiedBy = currentUser.value,
                        createdAt = System.currentTimeMillis() - (lines.size - i) * 60000,
                        modifiedAt = System.currentTimeMillis(),

                        dispositionType = dispType,
                        dispositionAreaOrSupplier = dispArea,
                        dispositionWorkedAlready = dispWorked,

                        why1 = w1,
                        why2 = w2,
                        why3 = w3,
                        why4 = w4,
                        why5 = w5,
                        rootCause = root,
                        details = detail,

                        correctiveActions = corrAct,
                        actionEffective = eff,

                        contentionQtyProd = qP,
                        contentionQtyQual = qQ,
                        contentionGoodQty = qG,
                        contentionBadQty = qB
                    )

                    // Compute global status based on values inside
                    val finalRecord = record.copy(status = record.computeGlobalStatus())

                    repository.insertRecord(
                        record = finalRecord,
                        user = currentUser.value,
                        actionName = "Importado",
                        actionDetails = "Registro QC importado desde archivo externo en lote"
                    )
                    importedCount++
                }
                
                onSuccess(importedCount)
            } catch (e: Exception) {
                onError("Error parsing CSV: ${e.message}")
            }
        }
    }

    private fun parseCsvRow(rowText: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val currentField = StringBuilder()
        
        var i = 0
        while (i < rowText.length) {
            val c = rowText[i]
            if (c == '\"') {
                if (i + 1 < rowText.length && rowText[i + 1] == '\"') {
                    // Escaped double quote
                    currentField.append('\"')
                    i++
                } else {
                    // Quotes toggle
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString())
                currentField.setLength(0)
            } else {
                currentField.append(c)
            }
            i++
        }
        result.add(currentField.toString())
        return result
    }
}

class QualityViewModelFactory(private val repository: QualityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QualityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QualityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
