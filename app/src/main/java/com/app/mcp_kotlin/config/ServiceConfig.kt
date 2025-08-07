package com.app.mcp_kotlin.config

import kotlinx.serialization.Serializable

/**
 * 数据类，用于存储服务配置信息。
 * 使用 @Serializable 注解使其可以被 kotlinx.serialization 库进行序列化和反序列化为 JSON。
 */
@Serializable
data class ServiceConfig(
    val name: String,
    val sseUrl: String
)
