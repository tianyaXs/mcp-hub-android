package com.app.mcp_kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ServiceListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var serviceManager: ServiceManager
    private lateinit var serviceListAdapter: ServiceListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_list)

        title = "已注册服务列表" // 设置 Activity 标题

        recyclerView = findViewById(R.id.recyclerViewServices)
        recyclerView.layoutManager = LinearLayoutManager(this)

        serviceManager = ServiceManager(this)
        val registeredServices = serviceManager.loadServices()

        serviceListAdapter = ServiceListAdapter(registeredServices)
        recyclerView.adapter = serviceListAdapter
    }
}