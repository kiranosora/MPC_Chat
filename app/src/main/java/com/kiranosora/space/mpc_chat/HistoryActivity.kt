package com.kiranosora.space.mpc_chat

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels // Use viewModels delegate if needed, or get ViewModel differently
import androidx.lifecycle.ViewModelProvider // Alternative way to get ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    // Get ViewModel - ensure it's the same instance or accesses the same repository
    // Using ViewModelProvider requires a Factory if ViewModel has constructor args like Application
    // For simplicity here, let's assume ChatViewModel doesn't strictly need Application context
    // OR access the DB directly (less ideal) or pass repository instance.
    // Best: Use dependency injection (Hilt/Dagger) or a simple Application subclass provider.
    // Let's use the standard ViewModelProvider with a simple factory for now.
    private val chatViewModel: ChatViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        title = "Chat History" // Set activity title

        recyclerView = findViewById(R.id.recyclerViewHistory)
        setupRecyclerView()

        // Observe the list of all sessions from the ViewModel
        chatViewModel.allSessions.observe(this) { sessions ->
            sessions?.let {
                historyAdapter.submitList(it)
            }
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { selectedSession ->
            // When an item is clicked, finish this activity and return the session ID
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_SESSION_ID, selectedSession.id)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Close the history activity
        }
        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    companion object {
        const val EXTRA_SESSION_ID = "com.yourcompany.yourapp.EXTRA_SESSION_ID"
    }
}