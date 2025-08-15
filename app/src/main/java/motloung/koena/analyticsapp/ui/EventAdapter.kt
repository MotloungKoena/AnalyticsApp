package motloung.koena.analyticsapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import motloung.koena.analyticsapp.R
import motloung.koena.analyticsapp.data.Event
import java.text.DateFormat
import java.util.*

class EventAdapter : ListAdapter<Event, EventAdapter.VH>(DIFF) {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val type: TextView = v.findViewById(R.id.txtType)
        val payload: TextView = v.findViewById(R.id.txtPayload)
        val time: TextView = v.findViewById(R.id.txtTime)
    }
    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_event, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = getItem(pos)
        h.type.text = e.type
        h.payload.text = e.payload
        h.time.text = DateFormat.getDateTimeInstance().format(Date(e.receivedAt))
    }
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(a: Event, b: Event) = a.id == b.id
            override fun areContentsTheSame(a: Event, b: Event) = a == b
        }
    }
}
