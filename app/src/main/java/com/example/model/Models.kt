package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "material_records")
data class MaterialRecord(
    @PrimaryKey
    val oc: String, // ID único del movimiento
    val partNumber: String,
    val quantity: Int,
    val warehouse: String,
    val location: String,
    val nonConformance: String,
    val operatorStamp: String,
    val operatorName: String,
    val deliveredBy: String,
    val rejectType: String, // "Proceso" or "Cliente interno"
    val photos: List<String> = emptyList(), // Max 5 image paths
    val status: String = "Abierto", // "Abierto", "En Proceso", "Terminado"
    val createdBy: String,
    val modifiedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),

    // DISPOSICIÓN
    val dispositionType: String? = null, // "Retrabajo", "Scrap", "Usar así", "Retorno a proveedor"
    val dispositionAreaOrSupplier: String? = null, // "Área" or "Proveedor interno"
    val dispositionWorkedAlready: Boolean = false, // True if workedAlready/Sí selected

    // 5 WHYs
    val why1: String = "",
    val why2: String = "",
    val why3: String = "",
    val why4: String = "",
    val why5: String = "",
    val rootCause: String = "",
    val details: String = "",

    // ACCIONES CORRECTIVAS
    val correctiveActions: String = "",
    val actionEffective: Boolean = false, // true = Sí / false = No

    // CONTENCIÓN
    val contentionQtyProd: Int = 0,
    val contentionQtyQual: Int = 0,
    val contentionGoodQty: Int = 0,
    val contentionBadQty: Int = 0
) : Serializable {

    // Helper to calculate total containment pending
    val contentionPending: Int
        get() {
            val total = contentionQtyProd + contentionQtyQual
            val resolved = contentionGoodQty + contentionBadQty
            return (total - resolved).coerceAtLeast(0)
        }

    // Helper status checkers
    fun isDispositionComplete(): Boolean {
        if (dispositionType == null) return false
        return if (dispositionType == "Retrabajo") {
            if (dispositionAreaOrSupplier == "Área") {
                true
            } else if (dispositionAreaOrSupplier == "Proveedor interno") {
                dispositionWorkedAlready
            } else {
                false
            }
        } else {
            true // Scrap, Usar así, Retorno a proveedor are simple completions
        }
    }

    fun isFiveWhysComplete(): Boolean {
        var answeredCount = 0
        if (why1.isNotBlank()) answeredCount++
        if (why2.isNotBlank()) answeredCount++
        if (why3.isNotBlank()) answeredCount++
        if (why4.isNotBlank()) answeredCount++
        if (why5.isNotBlank()) answeredCount++
        return answeredCount >= 3 && rootCause.isNotBlank()
    }

    fun isCorrectiveActionsComplete(): Boolean {
        return correctiveActions.isNotBlank() && actionEffective
    }

    fun isContentionComplete(): Boolean {
        // Only makes sense if contencion is configured, i.e., at least some quantity is declared in prod or qual,
        // and pending is calculated to 0. But if no qty is set yet, check if pending is 0 and they filled some info, or if total > 0.
        val total = contentionQtyProd + contentionQtyQual
        return total > 0 && contentionPending == 0
    }

    // Helper to determine active section statuses and recalculate global status
    fun hasAnySectionStarted(): Boolean {
        return dispositionType != null || 
               why1.isNotBlank() || why2.isNotBlank() || why3.isNotBlank() || rootCause.isNotBlank() ||
               correctiveActions.isNotBlank() ||
               (contentionQtyProd > 0 || contentionQtyQual > 0)
    }

    fun computeGlobalStatus(): String {
        val isComplete = isDispositionComplete() && 
                         isFiveWhysComplete() && 
                         isCorrectiveActionsComplete() && 
                         isContentionComplete()
        
        return if (isComplete) {
            "Terminado"
        } else if (hasAnySectionStarted()) {
            "En Proceso"
        } else {
            "Abierto"
        }
    }
}

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val oc: String,
    val user: String,
    val action: String, // e.g. "Creado", "Disposición", "5 Whys", "Contención", "Acciones Correctivas", "Importado", "Editado"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
) : Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val email: String,
    val name: String,
    val passwordHash: String, // SHA-256 or simple text hash for offline validation
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

