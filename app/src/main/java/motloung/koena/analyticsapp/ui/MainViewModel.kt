package motloung.koena.analyticsapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import motloung.koena.analyticsapp.data.AnalyticsDb

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AnalyticsDb.get(app).eventDao()

    val events = dao.all().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    suspend fun clear() = dao.clear()
}
