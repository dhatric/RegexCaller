package com.regexcaller.callblocker.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [BlockRule::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callblocker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
