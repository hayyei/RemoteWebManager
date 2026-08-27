package com.gimo.remotewebmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gimo.remotewebmanager.databinding.ItemDeviceBinding
import java.text.DateFormat
import java.util.Date

class DeviceAdapter(private val onClick:(Device)->Unit, private val onLong:(Device)->Unit): RecyclerView.Adapter<DeviceAdapter.VH>() {
    private var items: List<Device> = emptyList()
    fun submit(data: List<Device>) { items = data; notifyDataSetChanged() }
    class VH(val b: ItemDeviceBinding): RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)= VH(ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount()=items.size
    override fun onBindViewHolder(h: VH, p: Int) {
        val d=items[p]
        h.b.name.text=d.name
        h.b.url.text=d.url
        h.b.time.text=if(d.lastOpenedAt>0) "最近打开：${DateFormat.getDateTimeInstance().format(Date(d.lastOpenedAt))}" else "尚未打开"
        h.b.root.setOnClickListener{ onClick(d) }
        h.b.root.setOnLongClickListener{ onLong(d); true }
    }
}
