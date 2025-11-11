package com.packethunter.mobile.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PacketDao {
    @Query("SELECT * FROM packets ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPackets(limit: Int = 1000): Flow<List<PacketInfo>>

    @Query("SELECT * FROM packets WHERE protocol = :protocol ORDER BY timestamp DESC")
    fun getPacketsByProtocol(protocol: String): Flow<List<PacketInfo>>

    @Query("SELECT * FROM packets WHERE sourceIp = :ip OR destIp = :ip ORDER BY timestamp DESC")
    fun getPacketsByIp(ip: String): Flow<List<PacketInfo>>
    
    @Query("SELECT * FROM packets WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPacketsBySession(sessionId: String): List<PacketInfo>
    
    @Query("""
        SELECT * FROM packets 
        WHERE (sourceIp = :sourceIp AND sourcePort = :sourcePort AND destIp = :destIp AND destPort = :destPort)
           OR (sourceIp = :destIp AND sourcePort = :destPort AND destIp = :sourceIp AND destPort = :sourcePort)
        ORDER BY timestamp ASC
    """)
    suspend fun getPacketsByConnection(
        sourceIp: String,
        sourcePort: Int,
        destIp: String,
        destPort: Int
    ): List<PacketInfo>

    @Query("SELECT * FROM packets WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getPacketsInTimeRange(startTime: Long, endTime: Long): Flow<List<PacketInfo>>

    @Query("SELECT * FROM packets WHERE id IN (:ids)")
    suspend fun getPacketsByIds(ids: List<Long>): List<PacketInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacket(packet: PacketInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackets(packets: List<PacketInfo>)

    @Query("SELECT COUNT(*) FROM packets")
    suspend fun getPacketCount(): Long

    @Query("SELECT COUNT(*) FROM packets WHERE timestamp >= :startTime")
    suspend fun getPacketCountSince(startTime: Long): Long

    @Query("SELECT SUM(length) FROM packets")
    suspend fun getTotalBytes(): Long?

    @Query("SELECT SUM(length) FROM packets WHERE timestamp >= :startTime")
    suspend fun getTotalBytesSince(startTime: Long): Long?

    @Query("SELECT protocol, COUNT(*) as count FROM packets GROUP BY protocol")
    suspend fun getProtocolDistribution(): List<ProtocolCount>

    @Query("""
        SELECT destIp as ip, COUNT(*) as count, SUM(length) as bytes 
        FROM packets 
        GROUP BY destIp 
        ORDER BY bytes DESC 
        LIMIT :limit
    """)
    suspend fun getTopTalkers(limit: Int = 10): List<TalkerStats>

    @Query("DELETE FROM packets WHERE timestamp < :beforeTime")
    suspend fun deleteOldPackets(beforeTime: Long): Int

    @Query("DELETE FROM packets")
    suspend fun deleteAllPackets()
}

data class ProtocolCount(
    val protocol: String,
    val count: Int
)

data class TalkerStats(
    val ip: String,
    val count: Int,
    val bytes: Long
)

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE acknowledged = 0 ORDER BY timestamp DESC")
    fun getUnacknowledgedAlerts(): Flow<List<Alert>>

    @Insert
    suspend fun insertAlert(alert: Alert): Long

    @Update
    suspend fun updateAlert(alert: Alert)

    @Query("UPDATE alerts SET acknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Long)

    @Query("DELETE FROM alerts WHERE timestamp < :beforeTime")
    suspend fun deleteOldAlerts(beforeTime: Long)

    @Query("DELETE FROM alerts")
    suspend fun deleteAllAlerts()
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY name ASC")
    fun getAllRules(): Flow<List<DetectionRule>>

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<DetectionRule>

    @Query("SELECT * FROM rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: Long): DetectionRule?

    @Insert
    suspend fun insertRule(rule: DetectionRule): Long

    @Update
    suspend fun updateRule(rule: DetectionRule)

    @Delete
    suspend fun deleteRule(rule: DetectionRule)

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)

    @Query("DELETE FROM rules")
    suspend fun deleteAllRules()
}
