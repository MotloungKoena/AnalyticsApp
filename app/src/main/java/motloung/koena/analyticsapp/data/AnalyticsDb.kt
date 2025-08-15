package motloung.koena.analyticsapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Event::class], version = 1, exportSchema = false)
abstract class AnalyticsDb : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile private var INSTANCE: AnalyticsDb? = null
        fun get(context: Context): AnalyticsDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDb::class.java,
                    "analytics.db"
                ).build().also { INSTANCE = it }
            }
    }
}