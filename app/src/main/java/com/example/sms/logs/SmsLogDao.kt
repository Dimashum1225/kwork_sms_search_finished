package com.example.sms.logs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmsLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLogEntity)

    @Query("SELECT * FROM sms_logs")
    suspend fun getLastLogs(): List<SmsLogEntity>
    @Query("SELECT * FROM sms_logs WHERE sender = :sender AND text = :text LIMIT 1")
    fun getLogBySenderAndText(sender: String, text: String): SmsLogEntity?


}
