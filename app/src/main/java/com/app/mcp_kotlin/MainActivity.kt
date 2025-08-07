package com.app.mcp_kotlin
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.mcp_kotlin.service.Agent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport

// Agent 接口已移至单独的 Agent.kt 文件中。
// 请确保 Agent.kt 文件存在于同一包 (com.app.mcp_kotlin) 中。

class MainActivity : AppCompatActivity() {

    private lateinit var etPrompt: EditText
    private lateinit var btnSend: Button
    private lateinit var tvResponse: TextView
    private lateinit var chatModel: ChatModel
    private lateinit var agentService: Agent // 变量名已更改为 agentService
    private lateinit var btnRegisterPage: Button // 注册页面按钮
    private lateinit var btnViewServices: Button // 新增：查看服务列表按钮
    private lateinit var serviceManager: ServiceManager // ServiceManager 实例

    // 用于启动 RegistrationActivity 并处理返回结果的 Launcher
    private val registerActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 如果 RegistrationActivity 返回 RESULT_OK，则重新加载服务
                CoroutineScope(Dispatchers.IO).launch {
                    initAgentService() // 重新初始化 AgentService
                    withContext(Dispatchers.Main) {
                        tvResponse.text = "新服务注册成功，已重新加载服务。"
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 UI 元素
        etPrompt = findViewById(R.id.etPrompt)
        btnSend = findViewById(R.id.btnSend)
        tvResponse = findViewById(R.id.tvResponse)
        btnRegisterPage = findViewById(R.id.btnRegisterPage)
        btnViewServices = findViewById(R.id.btnViewServices) // 初始化查看服务列表按钮

        serviceManager = ServiceManager(this) // 初始化 ServiceManager

        // 初始化 LangChain4j ChatModel (只初始化一次)
        chatModel = OpenAiChatModel.builder()
            .apiKey(BuildConfig.apiKey)
            .baseUrl(BuildConfig.baseUrl)
            .modelName(BuildConfig.modelName)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true)
            .build()

        // 首次加载和设置 AgentService
        CoroutineScope(Dispatchers.IO).launch {
            initAgentService()
        }

        // 设置注册页面按钮的点击监听器
        btnRegisterPage.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            registerActivityResultLauncher.launch(intent) // 使用 launcher 启动 Activity
        }

        // 设置查看服务列表按钮的点击监听器
        btnViewServices.setOnClickListener {
            val intent = Intent(this, ServiceListActivity::class.java)
            startActivity(intent)
        }

        // 设置发送按钮的点击监听器
        btnSend.setOnClickListener {
            val prompt = etPrompt.text.toString()
            if (prompt.isNotBlank()) {
                tvResponse.text = "思考中..."
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 确保 agentService 已被初始化
                        if (!this@MainActivity::agentService.isInitialized) {
                            initAgentService() // 如果未初始化，则先初始化
                        }

                        val response: String? = agentService.chat(prompt)

                        withContext(Dispatchers.Main) {
                            tvResponse.text = response ?: "无响应"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            tvResponse.text = "发生错误: ${e.message}"
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                tvResponse.text = "请输入一些文本！"
            }
        }
    }

    /**
     * 负责加载已注册的服务并初始化 AgentService 的函数。
     */
    private suspend fun initAgentService() {
        val registeredServices = serviceManager.loadServices()
        val mcpClients = mutableListOf<McpClient>()

        for (serviceConfig in registeredServices) {
            val transport: McpTransport = createMcpTransport(serviceConfig.sseUrl)
            val mcpClient: McpClient = createMcpClient(serviceConfig.name, transport)
            mcpClients.add(mcpClient)
        }

        // 如果没有注册任何服务，可以创建一个空的工具提供者或默认的 AgentService
        if (mcpClients.isEmpty()) {
            withContext(Dispatchers.Main) {
                tvResponse.text = "当前未注册任何服务。请到注册页面添加服务。"
            }
            // 创建一个没有工具的 AgentService
            agentService = AiServices.builder<Agent>(Agent::class.java)
                .chatModel(chatModel)
                .build()
        } else {
            agentService = setupAgentService(chatModel, *mcpClients.toTypedArray())
        }
    }

    /**
     * 封装 McpTransport 的创建。
     */
    private fun createMcpTransport(sseUrl: String): McpTransport {
        return HttpMcpTransport.Builder()
            .sseUrl(sseUrl)
            .logRequests(true)
            .logResponses(true)
            .build()
    }

    /**
     * 封装 McpClient 的创建。
     */
    private fun createMcpClient(key: String, transport: McpTransport): McpClient {
        return DefaultMcpClient.Builder()
            .key(key)
            .transport(transport)
            .build()
    }

    /**
     * 封装 McpToolProvider 的创建。
     */
    private fun createToolProvider(vararg clients: McpClient): McpToolProvider {
        return McpToolProvider.builder()
            .mcpClients(*clients)
            .build()
    }

    /**
     * 负责注册和构建 Agent 服务的核心函数。
     */
    private fun setupAgentService(model: ChatModel, vararg mcpClients: McpClient): Agent {
        val toolProvider = createToolProvider(*mcpClients)
        return AiServices.builder<Agent>(Agent::class.java)
            .chatModel(model)
            .toolProvider(toolProvider)
            .build()
    }
}

