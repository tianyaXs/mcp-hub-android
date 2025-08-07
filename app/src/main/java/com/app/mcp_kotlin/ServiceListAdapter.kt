package com.app.mcp_kotlin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.mcp_kotlin.config.ServiceConfig

/**
 * RecyclerView 的适配器，用于显示 ServiceConfig 列表。
 */
class ServiceListAdapter(private val services: List<ServiceConfig>) :
    RecyclerView.Adapter<ServiceListAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.tvServiceName.text = "服务名称: ${service.name}"
        holder.tvServiceAddress.text = "服务地址: ${service.sseUrl}"
    }

    override fun getItemCount(): Int {
        return services.size
    }

    // ViewHolder 类，持有单个列表项的视图引用
    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvServiceName: TextView = itemView.findViewById(R.id.tvItemServiceName)
        val tvServiceAddress: TextView = itemView.findViewById(R.id.tvItemServiceAddress)
    }
}
