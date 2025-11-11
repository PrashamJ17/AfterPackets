package com.packethunter.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [PacketInfo::class, Alert::class, DetectionRule::class, FilterPreset::class, com.packethunter.mobile.interception.InterceptItem::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PacketDatabase : RoomDatabase() {
    abstract fun packetDao(): PacketDao
    abstract fun alertDao(): AlertDao
    abstract fun ruleDao(): RuleDao
    abstract fun filterPresetDao(): FilterPresetDao

    companion object {
        @Volatile
        private var INSTANCE: PacketDatabase? = null

        fun getDatabase(context: Context): PacketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PacketDatabase::class.java,
                    "packet_hunter_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromByteArray(bytes: ByteArray?): String? {
        return bytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
    }

    @TypeConverter
    fun toByteArray(encoded: String?): ByteArray? {
        return encoded?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
    }
}
