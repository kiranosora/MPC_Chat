package com.kiranosora.space.mpc_chat


import android.annotation.SuppressLint // 用于忽略 notifyDataSetChanged 警告 (稍后优化)
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 获取内部的 LinearLayout (气泡容器) 和 TextView
        val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)
        val roleTextView: TextView = view.findViewById(R.id.textViewRole) // 即使隐藏也要获取引用
        val contentTextView: TextView = view.findViewById(R.id.textViewContent)
        // 获取根布局的引用，以便设置其子视图的 gravity
        val rootLayout: LinearLayout = view as LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.roleTextView.text = message.role.replaceFirstChar { it.uppercase() }
        holder.contentTextView.text = message.content
// 根据角色设置背景和对齐
        if (message.role == "user") {
            // 用户消息 (浅绿色，靠右)
            holder.bubbleContainer.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_bubble_user)
            // 设置气泡容器在根布局内的对齐方式
            val params = holder.bubbleContainer.layoutParams as LinearLayout.LayoutParams
            params.gravity = Gravity.END // 靠右对齐
            params.leftMargin = 200
            params.rightMargin = 10
            holder.bubbleContainer.layoutParams = params // 应用修改后的参数

            // (可选) 如果需要，可以设置文字颜色
            // holder.contentTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.user_text_color))

        } else { // assistant 或 system
            // 助手或系统消息 (浅蓝色，靠左)
            holder.bubbleContainer.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_bubble_assistant)
            // 设置气泡容器在根布局内的对齐方式
            val params = holder.bubbleContainer.layoutParams as LinearLayout.LayoutParams
            params.gravity = Gravity.START // 靠左对齐
            holder.bubbleContainer.layoutParams = params // 应用修改后的参数
            params.leftMargin = 10
            params.rightMargin = 200

            // (可选) 如果需要，可以设置文字颜色
            // holder.contentTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.assistant_text_color))
        }
        // 可选: 根据 message.isStreaming 显示/隐藏 "typing" 指示器
    }

    override fun getItemCount() = messages.size



    // 添加新消息
    fun updateMessages(messages: List<ChatMessage>) {
        this.messages.clear()
        Log.d("updateMessages", "before messages: ${messages.size}")
        this.messages.addAll(messages)
        Log.d("updateMessages", "after messages: ${messages.size}")
        notifyDataSetChanged()
    }



    // 用于 notifyItemChanged 的 Payload，避免整个 item 重绘
    companion object {
        const val PAYLOAD_CONTENT_UPDATE = "content_update"
        const val PAYLOAD_STREAMING_DONE = "streaming_done"
    }

    // 优化 onBindViewHolder 以处理 Payload
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            // 完全绑定 (没有 payload)
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // 部分更新 (有 payload)
            val message = messages[position]
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_CONTENT_UPDATE -> holder.contentTextView.text = message.content
                    PAYLOAD_STREAMING_DONE -> {
                        // 在这里处理流结束时的 UI 变化，比如隐藏 typing 指示器
                        holder.contentTextView.text = message.content // 确保内容是最终的
                    }
                }
            }
        }
    }
}