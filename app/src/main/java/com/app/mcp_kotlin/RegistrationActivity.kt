package com.app.mcp_kotlin

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.mcp_kotlin.config.ServiceConfig

class RegistrationActivity : AppCompatActivity() {

    private lateinit var etServiceName: EditText
    private lateinit var etServiceAddress: EditText
    private lateinit var btnRegisterService: Button
    private lateinit var serviceManager: ServiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // 初始化 UI 元素
        etServiceName = findViewById(R.id.etServiceName)
        etServiceAddress = findViewById(R.id.etServiceAddress)
        btnRegisterService = findViewById(R.id.btnRegisterService)

        serviceManager = ServiceManager(this)

        btnRegisterService.setOnClickListener {
            val serviceName = etServiceName.text.toString().trim()
            val serviceAddress = etServiceAddress.text.toString().trim()

            if (serviceName.isNotBlank() && serviceAddress.isNotBlank()) {
                val existingServices = serviceManager.loadServices().toMutableList()

                if (existingServices.any { it.name == serviceName }) {
                    Toast.makeText(this, "服务名称已存在，请使用其他名称", Toast.LENGTH_SHORT).show()
                } else {
                    val newService = ServiceConfig(serviceName, serviceAddress)
                    existingServices.add(newService)

                    serviceManager.saveServices(existingServices)

                    Toast.makeText(this, "服务 '${serviceName}' 注册成功", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // <--- 设置成功结果
                    finish()
                }
            } else {
                Toast.makeText(this, "服务名称和服务地址不能为空", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
