package com.example.barcodescanner.repository

import android.content.ContentValues
import android.content.Context
import com.example.barcodescanner.database.DatabaseHelper
import com.example.barcodescanner.model.Notification

class NotificationRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insertNotification(notification: Notification): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NOTIFICATION_TITLE, notification.title)
            put(DatabaseHelper.COLUMN_NOTIFICATION_MESSAGE, notification.message)
            put(DatabaseHelper.COLUMN_NOTIFICATION_TYPE, notification.type)
            put(DatabaseHelper.COLUMN_NOTIFICATION_RELATED_BARCODE, notification.relatedBarcode)
            put(DatabaseHelper.COLUMN_NOTIFICATION_IS_READ, if (notification.isRead) 1 else 0)
            put(DatabaseHelper.COLUMN_NOTIFICATION_CREATED_DATE, notification.createdDate)
        }
        
        val result = db.insert(DatabaseHelper.TABLE_NOTIFICATIONS, null, values)
        db.close()
        return result
    }

    fun getAllNotifications(): List<Notification> {
        val notifications = mutableListOf<Notification>()
        val db = dbHelper.readableDatabase
        
        val cursor = db.query(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_NOTIFICATION_CREATED_DATE} DESC"
        )
        
        cursor?.use {
            while (it.moveToNext()) {
                val notification = Notification(
                    id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_TITLE)),
                    message = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_MESSAGE)),
                    type = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_TYPE)),
                    relatedBarcode = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_RELATED_BARCODE)),
                    isRead = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_IS_READ)) == 1,
                    createdDate = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTIFICATION_CREATED_DATE))
                )
                notifications.add(notification)
            }
        }
        
        db.close()
        return notifications
    }

    fun getUnreadNotificationCount(): Int {
        val db = dbHelper.readableDatabase
        var count = 0
        
        val cursor = db.query(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            arrayOf("COUNT(*)"),
            "${DatabaseHelper.COLUMN_NOTIFICATION_IS_READ} = ?",
            arrayOf("0"),
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        
        db.close()
        return count
    }

    fun markNotificationAsRead(notificationId: Long): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NOTIFICATION_IS_READ, 1)
        }
        
        val result = db.update(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            "${DatabaseHelper.COLUMN_NOTIFICATION_ID} = ?",
            arrayOf(notificationId.toString())
        )
        
        db.close()
        return result > 0
    }

    fun markAllNotificationsAsRead(): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NOTIFICATION_IS_READ, 1)
        }
        
        val result = db.update(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            "${DatabaseHelper.COLUMN_NOTIFICATION_IS_READ} = ?",
            arrayOf("0")
        )
        
        db.close()
        return result > 0
    }

    fun deleteNotification(notificationId: Long): Boolean {
        val db = dbHelper.writableDatabase
        val result = db.delete(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            "${DatabaseHelper.COLUMN_NOTIFICATION_ID} = ?",
            arrayOf(notificationId.toString())
        )
        
        db.close()
        return result > 0
    }

    fun createLowStockNotification(brand: String, barcode: String, currentQuantity: Int) {
        // Aynı barkod için zaten bildirim var mı kontrol et
        val existingNotification = checkExistingLowStockNotification(barcode)
        if (existingNotification) {
            return // Zaten bildirim var, yenisini oluşturma
        }

        val notification = Notification(
            title = "Düşük Stok Uyarısı",
            message = "$brand markasının stoku düştü (Kalan: $currentQuantity adet)",
            type = Notification.TYPE_LOW_STOCK,
            relatedBarcode = barcode
        )
        
        insertNotification(notification)
    }

    private fun checkExistingLowStockNotification(barcode: String): Boolean {
        val db = dbHelper.readableDatabase
        var exists = false
        
        val cursor = db.query(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            arrayOf(DatabaseHelper.COLUMN_NOTIFICATION_ID),
            "${DatabaseHelper.COLUMN_NOTIFICATION_TYPE} = ? AND ${DatabaseHelper.COLUMN_NOTIFICATION_RELATED_BARCODE} = ? AND ${DatabaseHelper.COLUMN_NOTIFICATION_IS_READ} = ?",
            arrayOf(Notification.TYPE_LOW_STOCK, barcode, "0"),
            null,
            null,
            null
        )
        
        cursor?.use {
            exists = it.count > 0
        }
        
        db.close()
        return exists
    }
}