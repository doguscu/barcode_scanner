package com.example.barcodescanner

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.adapter.NotificationAdapter
import com.example.barcodescanner.databinding.ActivityNotificationsBinding
import com.example.barcodescanner.model.Notification
import com.example.barcodescanner.repository.NotificationRepository
import com.google.android.material.snackbar.Snackbar

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationRepository: NotificationRepository
    private val notificationList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationRepository = NotificationRepository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupFloatingActionButton()
        loadNotifications()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Bildirimler"
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notifications = notificationList,
            onItemClick = { notification ->
                markNotificationAsRead(notification)
            },
            onDeleteClick = { notification ->
                showDeleteConfirmationDialog(notification)
            }
        )
        
        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }
    }

    private fun setupFloatingActionButton() {
        binding.fabMarkAllAsRead.setOnClickListener {
            markAllNotificationsAsRead()
        }
    }

    private fun loadNotifications() {
        val notifications = notificationRepository.getAllNotifications()
        notificationList.clear()
        notificationList.addAll(notifications)
        notificationAdapter.notifyDataSetChanged()
        
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (notificationList.isEmpty()) {
            binding.textViewEmptyNotifications.visibility = android.view.View.VISIBLE
            binding.recyclerViewNotifications.visibility = android.view.View.GONE
            binding.fabMarkAllAsRead.hide()
        } else {
            binding.textViewEmptyNotifications.visibility = android.view.View.GONE
            binding.recyclerViewNotifications.visibility = android.view.View.VISIBLE
            binding.fabMarkAllAsRead.show()
        }
    }

    private fun markNotificationAsRead(notification: Notification) {
        if (!notification.isRead) {
            notificationRepository.markNotificationAsRead(notification.id)
            loadNotifications() // Listeyi yenile
        }
    }

    private fun markAllNotificationsAsRead() {
        val unreadCount = notificationList.count { !it.isRead }
        if (unreadCount > 0) {
            notificationRepository.markAllNotificationsAsRead()
            loadNotifications()
            Snackbar.make(binding.root, "$unreadCount bildirim okundu olarak işaretlendi", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "Tüm bildirimler zaten okundu", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Bildirimi Sil")
            .setMessage("Bu bildirimi silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                deleteNotification(notification)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteNotification(notification: Notification) {
        if (notificationRepository.deleteNotification(notification.id)) {
            notificationAdapter.removeNotification(notification)
            updateEmptyState()
            Snackbar.make(binding.root, "Bildirim silindi", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "Bildirim silinemedi", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        @Suppress("DEPRECATION")
        onBackPressed()
        return true
    }
}