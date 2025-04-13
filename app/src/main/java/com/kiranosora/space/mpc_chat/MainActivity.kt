package com.kiranosora.space.mpc_chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.util.copy
import com.google.gson.Gson // 用于解析 SSE JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response // OkHttp Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.io.IOException
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    // --- ViewModel ---
    // Use the activity-ktx delegate to get the ViewModel instance
    private val chatViewModel: ChatViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    // Activity Result Launcher for History Activity
    private val historyActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val sessionId = result.data?.getLongExtra(HistoryActivity.EXTRA_SESSION_ID, -1L) ?: -1L
            if (sessionId != -1L) {
                Log.d("MainActivity", "Loading session from history: $sessionId")
                chatViewModel.loadSession(sessionId)
            }
        }
    }
    //private val apiKey = "lm_studio"
    //private val bearerToken = "Bearer $apiKey"
    //private val modelName = "triangle104/qwq-32b" // <--- 替换!

    private val gson = Gson() // 用于解析 JSON 块
    private var currentEventSource: EventSource? = null // 持有 EventSource 引用以便取消


    private lateinit var apiConfigSpinner: Spinner // Spinner 引用

    // --- API Configurations ---
    // 在这里定义你的 API 配置列表
    private val apiConfigs = listOf(
        ApiConfig(
            name = "qwq-32b@4bit", // 显示在下拉菜单的名字
            baseUrl = "https://kiranosora.space:12345/v1/", // 你的原始 Base URL
            modelName = "lmstudio-community/qwq-32b", // 替换成你的模型名!
            apiKey = "lm_studio" // 你的原始 Key
        ),
        ApiConfig(
            name = "qwq-32b@8bit", // 显示在下拉菜单的名字
            baseUrl = "https://kiranosora.space:12345/v1/", // 你的原始 Base URL
            modelName = "triangle104/qwq-32b", // 替换成你的模型名!
            apiKey = "lm_studio" // 你的原始 Key
        ),
        ApiConfig(
            name = "gemini 2.5 pro",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai", // 示例: OpenAI 官方 URL
            modelName = "models/gemini-2.5-pro-exp-03-25",             // 示例: OpenAI 模型
            apiKey = "AIzaSyCr4L4sPz5h7tMWqOttqNKLKaHWaxfmMUw"           // 示例: 替换成你的 OpenAI Key
        ),
    )
    companion object{
        // SharedPreferences 常量
        private const val PREFS_NAME = "ApiChatPrefs"
        private const val KEY_LAST_SELECTED_INDEX = "lastSelectedApiIndex"
    }
    // 当前选中的 API 配置
    private lateinit var currentApiConfig: ApiConfig
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar) // 或者 MaterialToolbar
        setSupportActionBar(toolbar) // 将 Toolbar 设置为 Activity 的 ActionBar
        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageInput = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)

        setupRecyclerView()
        // 设置 Spinner
        setupApiConfigSpinner() // 调用新的设置方法
        sendButton.setOnClickListener {
            val inputText = messageInput.text.toString().trim()
            val currentSessionId = chatViewModel.currentSessionId.value // Get current session ID
            if (inputText.isNotEmpty() && currentSessionId != null) {
                currentEventSource?.cancel()
                // Add user message via ViewModel FIRST
                //chatViewModel.buildApiHistory()
                Log.d("sendButton", "before addMessage")
                chatViewModel.addMessage("user", inputText, currentSessionId)
                messageInput.text.clear() // Clear input after adding
                // Then trigger API call
                sendMessageToApi(currentSessionId)
            } else if (currentSessionId == null) {
                showError("No active session. Try starting a new one.")
            }
        }

        // Observe ViewModel State
        observeViewModel()
    }

    // --- Inflate Options Menu ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu) // Create res/menu/main_menu.xml
        return true
    }

    // --- Handle Menu Item Clicks ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_session -> {
                currentEventSource?.cancel() // Cancel any ongoing stream
                chatViewModel.startNewSession()
                Toast.makeText(this, "New session started", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_view_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                historyActivityResultLauncher.launch(intent) // Use launcher
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // --- 新增：设置 Spinner ---
    private fun setupApiConfigSpinner() {
        apiConfigSpinner = findViewById(R.id.spinnerApiConfig)
        // --- 加载上次选择的索引 ---
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var lastSelectedIndex = prefs.getInt(KEY_LAST_SELECTED_INDEX, 0) // 默认选第0个

        // 验证索引是否有效 (防止列表变动导致索引越界)
        if (lastSelectedIndex !in apiConfigs.indices) {
            lastSelectedIndex = 0 // 如果无效，重置为0
        }
        // 提取配置名称用于显示在 Spinner 中
        val configNames = apiConfigs.map { it.name }
        // --- 设置 Spinner 初始选项 ---
        if (apiConfigs.isNotEmpty()) {
            // 设置 Spinner 显示的初始项，false 表示不要触发 onItemSelected 监听器
            apiConfigSpinner.setSelection(lastSelectedIndex, false)
            // 初始化 currentApiConfig 为加载的配置
            currentApiConfig = apiConfigs[lastSelectedIndex]
            Log.d("API_CONFIG", "Restored API Config: ${currentApiConfig.name}")
        } else {
            // 处理没有配置的情况
            sendButton.isEnabled = false
            apiConfigSpinner.isEnabled = false
            Toast.makeText(this, "错误：没有可用的 API 配置", Toast.LENGTH_LONG).show()
        }
        // 创建 ArrayAdapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, configNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 设置适配器
        apiConfigSpinner.adapter = adapter

        // 设置选择监听器
        apiConfigSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 更新当前选中的配置
                currentApiConfig = apiConfigs[position]
                Log.d("API_CONFIG", "Selected API Config: ${currentApiConfig.name}")

                // --- 保存当前选择的索引 ---
                // 使用 edit() 获取 Editor 对象，放入数据，然后 apply() 保存
                prefs.edit() { putInt(KEY_LAST_SELECTED_INDEX, position) }
                Log.d("API_CONFIG", "Saved selected index: $position")                // (可选) 清空当前聊天记录，因为模型或端点可能已改变
                // messageList.clear()
                // messageAdapter.notifyDataSetChanged()
                Toast.makeText(this@MainActivity, "切换到: ${currentApiConfig.name}", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 通常不需要处理
            }
        }

        // (可选) 如果你使用 SharedPreferences 加载了上次的选择，在这里设置 Spinner 的初始位置
         if (lastSelectedIndex in apiConfigs.indices) {
            apiConfigSpinner.setSelection(lastSelectedIndex)
            currentApiConfig = apiConfigs[lastSelectedIndex]
         } else if (apiConfigs.isNotEmpty()) {
            currentApiConfig = apiConfigs[0] // 默认选第一个
         }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity 销毁时确保取消 EventSource
        currentEventSource?.cancel()
    }


    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList)
        recyclerView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
    }

    // --- Observe ViewModel ---
    private fun observeViewModel() {
        // Observe changes in the current messages list
        lifecycleScope.launch {
            Log.d("observeViewModel", "launch")
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Observe only when started
                chatViewModel.currentMessages.collectLatest { messages ->
                    // Update the adapter's data
                    Log.d("observeViewModel", "${messages.size}, messages.last: ${messages.lastOrNull()?.role}, ${messages.lastOrNull()?.content}")
                    messageAdapter.updateMessages(messages)
                    // Scroll to bottom if new messages arrive
                    if (messages.isNotEmpty()) {
                        recyclerView.post { // Use post to ensure layout is complete
                            Log.d("observeViewModel", "scrollToPosition: ${messages.size - 1}")
                            recyclerView.scrollToPosition(messages.size - 1)
                        }
                    }
                }
            }
        }

        // (Optional) Observe session ID changes if you need to update UI title etc.
        // lifecycleScope.launch {
        //     repeatOnLifecycle(Lifecycle.State.STARTED) {
        //         chatViewModel.currentSessionId.collect { sessionId ->
        //             // Update Activity title or some indicator
        //             title = "Chat Session ${sessionId ?: "N/A"}"
        //         }
        //     }
        // }
    }
    // --- Modified API Call Trigger ---
    // Renamed to avoid confusion, now takes sessionId
    val debug = true
    private fun sendMessageToApi(sessionId: Long) {
        // Get history specifically for the API call from ViewModel
        // Check if API config is ready
        if (!::currentApiConfig.isInitialized) {
            showError("Please select an API configuration.")
            return
        }

        // 添加助手的初始空消息 (或带 "typing..." 标记)
        chatViewModel.addMessage("assistant", "", sessionId)


        // --- Prepare Request ---
        val requestPayload = ChatCompletionRequest(
            currentApiConfig.modelName,
            chatViewModel.currentMessages.value, // Map to API model if needed
            true
        )
        // ... (rest of request building using currentApiConfig - same as before)
        val requestBodyJson = gson.toJson(requestPayload)
        Log.d("sendMessageToApi", "requestBodyJson: $requestBodyJson")
        val requestBody = requestBodyJson.toRequestBody("application/json".toMediaType())
        val chatUrl = currentApiConfig.baseUrl.removeSuffix("/") + "/chat/completions"
        val apiKeyHeader = "Bearer ${currentApiConfig.apiKey}"
        val request = Request.Builder()
            .url(chatUrl)
            .header("Authorization", apiKeyHeader)
            .header("Accept", "text/event-stream")
            .post(requestBody)
            .build()


        // --- EventSource Listener ---
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d("SSE_${currentApiConfig.name}", "Connection Opened")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                // ... (parse data - same as before) ...
                if (data == "[DONE]") {
                    // Mark completion in ViewModel
                    lifecycleScope.launch { chatViewModel.markStreamingComplete() }
                    runOnUiThread {
                        // messageAdapter.markStreamingComplete() // Handled by observing ViewModel state now
                        enableControls()
                    }
                    return
                }
                try {
                    Log.d("gson","parseJson data: $data")
                    val chunk = gson.fromJson(data, StreamingChatCompletionChunk::class.java)
                    val contentDelta = chunk?.choices?.firstOrNull()?.delta?.content
                    Log.d("MainActivity", "contentDelta: $contentDelta")
                    if (contentDelta != null) {
                        // Append content via ViewModel
                        lifecycleScope.launch { chatViewModel.appendStreamingContent(contentDelta) }
                        // UI update happens via observing chatViewModel.currentMessages
                    }
                    val finishReason = chunk?.choices?.firstOrNull()?.finishReason
                    if (finishReason != null) {
                        lifecycleScope.launch { chatViewModel.markStreamingComplete() }
                        runOnUiThread { enableControls() }
                        eventSource.cancel()
                    }
                } catch (e: Exception) {
                    // ... (error handling - mark complete in VM, enable controls) ...
                    lifecycleScope.launch { chatViewModel.markStreamingComplete() } // Still mark complete on error
                    runOnUiThread {
                        e.printStackTrace()
                        showError("Error parsing SSE: ${e.message}")
                        enableControls()
                    }
                    eventSource.cancel()
                }
            }

            override fun onClosed(eventSource: EventSource) {
                // Ensure completion is marked if closed unexpectedly
                lifecycleScope.launch { chatViewModel.markStreamingComplete() }
                runOnUiThread { enableControls() }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // ... (error handling - mark complete in VM, enable controls) ...
                lifecycleScope.launch { chatViewModel.markStreamingComplete() }
                runOnUiThread {
                    showError("SSE Failure: ${t?.message ?: response?.message}")
                    enableControls()
                }
            }
        }

        // Disable controls and start SSE
        disableControls()
        currentEventSource = ApiClient.eventSourceFactory.newEventSource(request, listener)
    }

    private fun disableControls() {
        runOnUiThread {
            sendButton.isEnabled = false
            apiConfigSpinner.isEnabled = false
            messageInput.isEnabled = false // Also disable input field
        }
    }

    private fun enableControls() {
        runOnUiThread {
            sendButton.isEnabled = true
            apiConfigSpinner.isEnabled = true
            messageInput.isEnabled = true
        }
    }
//    private fun sendMessageStream(text: String) {
//        // 1. 添加用户消息
//        val userMessage = ChatMessage("user", text, true)
//        addMessageToList(userMessage)
//        messageInput.text.clear()
//
//        // 2. 添加助手的初始空消息 (或带 "typing..." 标记)
//        val assistantMessagePlaceholder = ChatMessage("assistant", "", true)
//        addMessageToList(assistantMessagePlaceholder) // 添加占位符
//
//        // 3. 准备请求体
//        val messagesToSend = buildMessageHistory() // 获取历史记录
//        val requestPayload = ChatCompletionRequest(
//            currentApiConfig.modelName,
//            messagesToSend,
//            true // <-- 启用流式传输
//        )
//        val requestBodyJson = gson.toJson(requestPayload)
//        val requestBody = requestBodyJson.toRequestBody("application/json".toMediaType())
//        val chatCompletionsUrl =  currentApiConfig.baseUrl + "chat/completions"
//        Log.d("sendMessageStream: ", "chatCompletionsUrl: $chatCompletionsUrl modelName: ${currentApiConfig.modelName}")
//        // 4. 构建 OkHttp Request
//        val request = Request.Builder()
//            .url(chatCompletionsUrl) // 使用完整的 URL
//            .header("Authorization", "Bearer ${currentApiConfig.apiKey}")
//            .header("Accept", "text/event-stream") // 告诉服务器期望 SSE
//            .post(requestBody)
//            .build()
//
//        // 5. 创建 EventSourceListener
//        val listener = object : EventSourceListener() {
//            override fun onOpen(eventSource: EventSource, response: Response) {
//                Log.d("SSE", "Connection Opened + ${System.currentTimeMillis()}")
//                // 连接已打开，可以更新 UI 显示连接成功 (可选)
//            }
//
//            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
//                Log.d("SSE", "Received data: $data  time: ${System.currentTimeMillis()}")
//                if (data == "[DONE]") {
//                    Log.d("SSE", "Stream finished with [DONE]")
//                    // 流结束信号
//                    runOnUiThread {
//                        messageAdapter.markStreamingComplete() // 标记 UI 状态
//                        sendButton.isEnabled = true // 重新启用发送按钮
//                    }
//                    return // 处理完毕
//                }
//
//                try {
//                    // 解析收到的 JSON 数据块
//                    val chunk = gson.fromJson(data, StreamingChatCompletionChunk::class.java)
//                    val contentDelta = chunk?.choices?.firstOrNull()?.delta?.content
//                    Log.d("MainActivity", "contentDelta: ${contentDelta}")
//                    if (contentDelta != null) {
//                        // 在 UI 线程上追加内容到 RecyclerView
//                        runOnUiThread {
//                            messageAdapter.appendToLastMessage(contentDelta)
//                            // 滚动到底部
//                            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
//                        }
//                    }
//
//                    // 检查是否有结束原因
//                    val finishReason = chunk?.choices?.firstOrNull()?.finishReason
//                    if (finishReason != null) {
//                        Log.d("SSE", "Stream finished with reason: $finishReason")
//                        runOnUiThread {
//                            messageAdapter.markStreamingComplete() // 标记 UI 状态
//                            sendButton.isEnabled = true // 重新启用发送按钮
//                        }
//                        eventSource.cancel() // 主动关闭连接，因为我们收到了结束信号
//                    }
//
//                } catch (e: Exception) {
//                    Log.e("SSE", "Error parsing SSE data: $data", e)
//                    // 可以在 UI 上显示解析错误
//                    runOnUiThread {
//                        messageAdapter.appendToLastMessage("\n[Error parsing response chunk]")
//                        messageAdapter.markStreamingComplete()
//                        sendButton.isEnabled = true
//                    }
//                    eventSource.cancel() // 解析错误，也关闭连接
//                }
//            }
//
//            override fun onClosed(eventSource: EventSource) {
//                Log.d("SSE", "Connection Closed")
//                runOnUiThread {
//                    // 确保最终状态是完成
//                    if (messageList.isNotEmpty() && messageList.last().isStreaming) {
//                        messageAdapter.markStreamingComplete()
//                    }
//                    sendButton.isEnabled = true // 重新启用发送按钮
//                }
//            }
//
//            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
//                Log.e("SSE", "Connection Failure: ${response?.message}", t)
//                runOnUiThread {
//                    showError("SSE Error: ${t?.message ?: response?.message ?: "Unknown error"}")
//                    // 出错时也标记完成状态并启用按钮
//                    if (messageList.isNotEmpty() && messageList.last().isStreaming) {
//                        messageAdapter.appendToLastMessage("\n[Connection Error]")
//                        messageAdapter.markStreamingComplete()
//                    }
//                    sendButton.isEnabled = true
//                }
//                // 不需要手动 cancel，因为已经 failure 了
//            }
//        }
//
//        // 6. 启动 EventSource
//        sendButton.isEnabled = false // 禁用发送按钮，防止重复发送
//        currentEventSource = ApiClient.eventSourceFactory.newEventSource(request, listener)
//
//        // 注意：newEventSource 是异步执行的，这里不需要 lifecycleScope.launch
//        apiConfigSpinner.isEnabled = false // 禁用 Spinner，防止在请求期间切换
//    }


    private fun buildMessageHistory(): List<ChatMessage> {
        // 只发送 user 和 assistant 的消息，并且移除 isStreaming 标记
        return messageList
            .filter { it.role == "user" || it.role == "assistant" }
            .map { it.copy(false) } // 创建副本，不修改原始列表中的状态
            .toList()
        // .takeLast(10) // 限制历史记录长度 (可选)
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}