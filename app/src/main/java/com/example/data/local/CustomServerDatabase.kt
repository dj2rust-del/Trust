package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ServerRegion
import com.example.data.model.UdpProtocolMode
import com.example.data.model.VpnServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "custom_servers")
data class CustomServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val city: String,
    val host: String,
    val port: Int,
    val protocolName: String,
    val pingMs: Int,
    val loadPercent: Int,
    val authCredential: String,
    val mtu: Int,
    val obfuscationPayload: String,
    val createdAt: Long
) {
    fun toDomain(): VpnServer = VpnServer(
        id = id,
        name = name,
        countryName = countryName,
        countryCode = countryCode,
        flagEmoji = flagEmoji,
        city = city,
        region = ServerRegion.GENERATED,
        host = host,
        port = port,
        protocolMode = runCatching { UdpProtocolMode.valueOf(protocolName) }
            .getOrDefault(UdpProtocolMode.RAW_UDP),
        pingMs = pingMs,
        loadPercent = loadPercent,
        authCredential = authCredential,
        mtu = mtu,
        obfuscationPayload = obfuscationPayload,
        isCustomGenerated = true,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(server: VpnServer): CustomServerEntity = CustomServerEntity(
            id = server.id,
            name = server.name,
            countryName = server.countryName,
            countryCode = server.countryCode,
            flagEmoji = server.flagEmoji,
            city = server.city,
            host = server.host,
            port = server.port,
            protocolName = server.protocolMode.name,
            pingMs = server.pingMs,
            loadPercent = server.loadPercent,
            authCredential = server.authCredential,
            mtu = server.mtu,
            obfuscationPayload = server.obfuscationPayload,
            createdAt = server.createdAt
        )
    }
}

@Dao
interface CustomServerDao {
    @Query("SELECT * FROM custom_servers ORDER BY createdAt DESC")
    fun observeAllServers(): Flow<List<CustomServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: CustomServerEntity)

    @Query("DELETE FROM custom_servers WHERE id = :id")
    suspend fun deleteServer(id: String)

    @Query("UPDATE custom_servers SET pingMs = :pingMs WHERE id = :id")
    suspend fun updatePing(id: String, pingMs: Int)
}

@Database(entities = [CustomServerEntity::class], version = 1, exportSchema = false)
abstract class MegaVpnDatabase : RoomDatabase() {
    abstract fun customServerDao(): CustomServerDao

    companion object {
        @Volatile
        private var INSTANCE: MegaVpnDatabase? = null

        fun getInstance(context: Context): MegaVpnDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MegaVpnDatabase::class.java,
                    "dj_trust_mega_vpn.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class ServerRepository(private val dao: CustomServerDao) {
    val generatedServers: Flow<List<VpnServer>> = dao.observeAllServers()
        .map { list -> list.map { it.toDomain() } }

    suspend fun saveGeneratedServer(server: VpnServer) {
        dao.insertServer(CustomServerEntity.fromDomain(server))
    }

    suspend fun deleteGeneratedServer(id: String) {
        dao.deleteServer(id)
    }
}
