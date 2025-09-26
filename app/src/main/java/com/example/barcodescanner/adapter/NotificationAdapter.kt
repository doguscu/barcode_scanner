package com.example.barcodescanner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemNotificationBinding
import com.example.barcodescanner.model.Notification
import java.text.DateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onItemClick: (Notification) -> Unit,
    private val onDeleteClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.textViewTitle.text = notification.title
            binding.textViewMessage.text = notification.message
            binding.textViewDate.text = dateFormat.format(Date(notification.createdDate))
            
            // Okunmamış bildirimleri farklı göster
            if (!notification.isRead) {
                binding.root.alpha = 1.0f
                binding.imageViewUnreadIndicator.visibility = android.view.View.VISIBLE
            } else {
                binding.root.alpha = 0.7f
                binding.imageViewUnreadIndicator.visibility = android.view.View.GONE
            }
            
            // Bildirim tipine göre ikon
            when (notification.type) {
                Notification.TYPE_LOW_STOCK -> {
                    binding.imageViewIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                }
                Notification.TYPE_INFO -> {
                    binding.imageViewIcon.setImageResource(android.R.drawable.ic_dialog_info)
                }
                Notification.TYPE_WARNING -> {
                    binding.imageViewIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                }
                else -> {
                    binding.imageViewIcon.setImageResource(android.R.drawable.ic_dialog_info)
                }
            }

            binding.root.setOnClickListener {
                onItemClick(notification)
            }
            
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(notification)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    fun removeNotification(notification: Notification) {
        val position = notifications.indexOf(notification)
        if (position != -1) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}