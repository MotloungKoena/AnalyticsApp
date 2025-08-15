package motloung.koena.analyticsapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY receivedAt DESC")
    fun all(): Flow<List<Event>>

    @Insert
    suspend fun insert(e: Event)

    @Query("DELETE FROM events")
    suspend fun clear()
}