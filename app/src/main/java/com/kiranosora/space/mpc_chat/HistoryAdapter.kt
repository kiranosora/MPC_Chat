package com.kiranosora.space.mpc_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kiranosora.space.mpc_chat.db.ChatSession
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val onItemClicked: (ChatSession) -> Unit) :
    ListAdapter<ChatSession, HistoryAdapter.HistoryViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history_session, parent, false)
        return HistoryViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        itemView: View,
        val onItemClicked: (ChatSession) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewSessionTitle)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewSessionTimestamp)
        private var currentSession: ChatSession? = null
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())


        init {
            itemView.setOnClickListener {
                currentSession?.let {
                    onItemClicked(it)
                }
            }
        }

        fun bind(session: ChatSession) {
            currentSession = session
            titleTextView.text = session.title ?: "Session ${session.id}" // Use ID if title is null
            timestampTextView.text = dateFormat.format(Date(session.startTimestamp))
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<ChatSession>() {
        override fun areItemsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean {
            return oldItem == newItem
        }
    }
}