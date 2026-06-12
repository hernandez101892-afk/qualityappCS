package com.example.data.local

import androidx.room.*
import com.example.model.AuditLog
import com.example.model.MaterialRecord
import com.example.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface QualityDao {

    // Record operations
    @Query("SELECT * FROM material_records ORDER BY createdAt DESC")
    fun getAllRecordsFlow(): Flow<List<MaterialRecord>>

    @Query("SELECT * FROM material_records ORDER BY createdAt DESC")
    suspend fun getAllRecordsDirect(): List<MaterialRecord>

    @Query("SELECT * FROM material_records WHERE oc = :oc LIMIT 1")
    fun getRecordByOcFlow(oc: String): Flow<MaterialRecord?>

    @Query("SELECT * FROM material_records WHERE oc = :oc LIMIT 1")
    suspend fun getRecordByOcDirect(oc: String): MaterialRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MaterialRecord)

    @Update
    suspend fun updateRecord(record: MaterialRecord)

    @Delete
    suspend fun deleteRecord(record: MaterialRecord)

    // Audit Log operations
    @Query("SELECT * FROM audit_logs WHERE oc = :oc ORDER BY timestamp DESC")
    fun getAuditLogsForRecord(oc: String): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogsDirect(): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    // User operations
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}
