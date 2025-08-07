package com.app.mcp_kotlin

import android.content.Context
import com.app.mcp_kotlin.config.ServiceConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * ServiceManager 类，用于处理服务配置的 JSON 读写操作。
 */
class ServiceManager(private val context: Context) {

    private val FILE_NAME = "registered_services.json" // 存储服务配置的 JSON 文件名

    /**
     * 将服务列表保存到内部存储的 JSON 文件中。
     * @param services 要保存的服务配置列表。
     */
    fun saveServices(services: List<ServiceConfig>) {
        val jsonString = Json.encodeToString(services) // 将服务列表序列化为 JSON 字符串
        try {
            // 打开文件输出流，MODE_PRIVATE 表示只有本应用可以访问
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray()) // 将 JSON 字符串写入文件
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // 可以在这里添加更详细的错误处理，例如记录日志或通知用户
        }
    }

    /**
     * 从内部存储的 JSON 文件中加载服务列表。
     * @return 加载的服务配置列表，如果文件不存在或加载失败则返回空列表。
     */
    fun loadServices(): List<ServiceConfig> {
        return try {
            // 打开文件输入流
            context.openFileInput(FILE_NAME).use {
                val jsonString = it.bufferedReader().use { reader -> reader.readText() } // 读取文件内容
                Json.decodeFromString<List<ServiceConfig>>(jsonString) // 将 JSON 字符串反序列化为服务列表
            }
        } catch (e: FileNotFoundException) {
            // 文件不存在，返回空列表
            emptyList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList() // 读取失败，返回空列表
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // 反序列化失败，返回空列表
        }
    }
}