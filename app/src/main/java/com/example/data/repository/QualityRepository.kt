package com.example.data.repository

import android.util.Log
import com.example.data.local.QualityDao
import com.example.model.AuditLog
import com.example.model.MaterialRecord
import com.example.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow

class QualityRepository(private val qualityDao: QualityDao) {

    val allRecords: Flow<List<MaterialRecord>> = qualityDao.getAllRecordsFlow()
    val allAuditLogs: Flow<List<AuditLog>> = qualityDao.getAllAuditLogsFlow()

    suspend fun getRecordsDirect(): List<MaterialRecord> = qualityDao.getAllRecordsDirect()

    fun getRecordByOc(oc: String): Flow<MaterialRecord?> = qualityDao.getRecordByOcFlow(oc)

    suspend fun getRecordByOcDirect(oc: String): MaterialRecord? = qualityDao.getRecordByOcDirect(oc)

    suspend fun insertRecord(record: MaterialRecord, user: String, actionName: String, actionDetails: String) {
        // Automatically save the record
        qualityDao.insertRecord(record)
        // Automatically create and insert audit log
        val auditLog = AuditLog(
            oc = record.oc,
            user = user,
            action = actionName,
            details = actionDetails
        )
        qualityDao.insertAuditLog(auditLog)

        // Firebase Cloud Sync
        trySyncRecordToFirestore(record)
        trySyncAuditLogToFirestore(auditLog)
    }

    suspend fun updateRecord(record: MaterialRecord, user: String, actionName: String, actionDetails: String) {
        // Automatically calculate status based on section status
        val updatedRecord = record.copy(
            status = record.computeGlobalStatus(),
            modifiedAt = System.currentTimeMillis()
        )
        qualityDao.insertRecord(updatedRecord) // Insert handles replace
        
        // Save audit log
        val auditLog = AuditLog(
            oc = updatedRecord.oc,
            user = user,
            action = actionName,
            details = actionDetails
        )
        qualityDao.insertAuditLog(auditLog)

        // Firebase Cloud Sync
        trySyncRecordToFirestore(updatedRecord)
        trySyncAuditLogToFirestore(auditLog)
    }

    suspend fun deleteRecord(record: MaterialRecord, user: String) {
        qualityDao.deleteRecord(record)
        val auditLog = AuditLog(
            oc = record.oc,
            user = user,
            action = "Eliminado",
            details = "Registro QC ${record.oc} eliminado"
        )
        qualityDao.insertAuditLog(auditLog)

        // Firebase Cloud Sync
        tryDeleteRecordFromFirestore(record.oc)
        trySyncAuditLogToFirestore(auditLog)
    }

    fun getAuditLogsForRecord(oc: String): Flow<List<AuditLog>> = qualityDao.getAuditLogsForRecord(oc)

    // Helper: auto-assign operator based on stamp
    fun getOperatorNameForStamp(stamp: String): String {
        val trimmed = stamp.trim().uppercase()
        return when (trimmed) {
            "OP-101" -> "Juan Pérez (Prensa 1)"
            "OP-202" -> "Eduardo Martínez (Ensamble)"
            "OP-303" -> "María Rodríguez (Inyección)"
            "OP-404" -> "Luis Hernández (Maquinado)"
            "OP-505" -> "Ana Gómez (Pintura)"
            else -> if (trimmed.isNotEmpty()) "Operador #$trimmed" else "No asignado"
        }
    }

    suspend fun getUserByEmail(email: String): User? = qualityDao.getUserByEmail(email)

    suspend fun insertUser(user: User) = qualityDao.insertUser(user)

    // Firebase Cloud Sync Utilities
    fun trySyncRecordToFirestore(record: MaterialRecord) {
        try {
            val db = FirebaseFirestore.getInstance()
            val map = recordToMap(record)
            db.collection("material_records")
                .document(record.oc)
                .set(map, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FirebaseSync", "Record ${record.oc} synced to Cloud Firestore automatically.")
                }
                .addOnFailureListener { e ->
                    Log.w("FirebaseSync", "Offline or missing firebase config; cached locally. Details: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Firestore not initialized or unavailable: ${e.message}")
        }
    }

    fun trySyncAuditLogToFirestore(log: AuditLog) {
        try {
            val db = FirebaseFirestore.getInstance()
            val map = auditLogToMap(log)
            val docId = if (log.id > 0) log.id.toString() else "log_${log.oc}_${log.timestamp}"
            db.collection("audit_logs")
                .document(docId)
                .set(map, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FirebaseSync", "AuditLog ${log.oc} synced to Cloud Firestore automatically.")
                }
                .addOnFailureListener { e ->
                    Log.w("FirebaseSync", "Failed to sync audit log to Cloud: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Firestore not initialized or unavailable: ${e.message}")
        }
    }

    fun tryDeleteRecordFromFirestore(oc: String) {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("material_records")
                .document(oc)
                .delete()
                .addOnSuccessListener {
                    Log.d("FirebaseSync", "Record ${oc} purged from Cloud Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.w("FirebaseSync", "Failed to delete record ${oc} in Cloud: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Firestore unavailable: ${e.message}")
        }
    }

    suspend fun syncAllLocalToCloudFirestore(onProgress: (String) -> Unit) {
        try {
            val records = getRecordsDirect()
            val logs = qualityDao.getAllAuditLogsDirect()
            val db = FirebaseFirestore.getInstance()

            onProgress("Enviando ${records.size} expedientes de calidad a Firestore...")
            for (rec in records) {
                db.collection("material_records")
                    .document(rec.oc)
                    .set(recordToMap(rec), SetOptions.merge())
            }

            onProgress("Enviando ${logs.size} bitácoras de auditoría a Firestore...")
            for (log in logs) {
                val docId = if (log.id > 0) log.id.toString() else "log_${log.oc}_${log.timestamp}"
                db.collection("audit_logs")
                    .document(docId)
                    .set(auditLogToMap(log), SetOptions.merge())
            }

            onProgress("¡Sincronización con Firebase realizada con éxito!")
        } catch (e: Exception) {
            onProgress("Error al sincronizar con Firebase: ${e.message}")
            Log.e("FirebaseSync", "Sync failed: ${e.message}")
        }
    }

    private fun recordToMap(record: MaterialRecord): Map<String, Any?> {
        return mapOf(
            "oc" to record.oc,
            "partNumber" to record.partNumber,
            "quantity" to record.quantity,
            "warehouse" to record.warehouse,
            "location" to record.location,
            "nonConformance" to record.nonConformance,
            "operatorStamp" to record.operatorStamp,
            "operatorName" to record.operatorName,
            "deliveredBy" to record.deliveredBy,
            "rejectType" to record.rejectType,
            "photos" to record.photos,
            "status" to record.status,
            "createdBy" to record.createdBy,
            "modifiedBy" to record.modifiedBy,
            "createdAt" to record.createdAt,
            "modifiedAt" to record.modifiedAt,
            "dispositionType" to record.dispositionType,
            "dispositionAreaOrSupplier" to record.dispositionAreaOrSupplier,
            "dispositionWorkedAlready" to record.dispositionWorkedAlready,
            "why1" to record.why1,
            "why2" to record.why2,
            "why3" to record.why3,
            "why4" to record.why4,
            "why5" to record.why5,
            "rootCause" to record.rootCause,
            "details" to record.details,
            "correctiveActions" to record.correctiveActions,
            "actionEffective" to record.actionEffective,
            "contentionQtyProd" to record.contentionQtyProd,
            "contentionQtyQual" to record.contentionQtyQual,
            "contentionGoodQty" to record.contentionGoodQty,
            "contentionBadQty" to record.contentionBadQty
        )
    }

    private fun auditLogToMap(log: AuditLog): Map<String, Any?> {
        return mapOf(
            "id" to log.id,
            "oc" to log.oc,
            "user" to log.user,
            "action" to log.action,
            "timestamp" to log.timestamp,
            "details" to log.details
        )
    }
}
