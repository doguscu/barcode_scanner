package com.example.barcodescanner.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "barcode_scanner.db"
        private const val DATABASE_VERSION = 6

        // Ana sayfa taramaları tablosu
        const val TABLE_SCAN_RESULTS = "scan_results"
        const val COLUMN_SCAN_ID = "id"
        const val COLUMN_SCAN_BARCODE = "barcode"
        const val COLUMN_SCAN_BRAND = "brand"
        const val COLUMN_SCAN_SALE_PRICE = "sale_price"
        const val COLUMN_SCAN_DATE = "scan_date"

        // Stok tablosu
        const val TABLE_STOCK_ITEMS = "stock_items"
        const val COLUMN_STOCK_ID = "id"
        const val COLUMN_STOCK_BARCODE = "barcode"
        const val COLUMN_STOCK_BRAND = "brand"
        const val COLUMN_STOCK_PURCHASE_PRICE = "purchase_price"
        const val COLUMN_STOCK_DATE = "stock_date"
        const val COLUMN_STOCK_QUANTITY = "quantity"
        const val COLUMN_STOCK_PRODUCT_TYPE = "product_type"

        // İşlemler tablosu
        const val TABLE_TRANSACTIONS = "transactions"
        const val COLUMN_TRANSACTION_ID = "id"
        const val COLUMN_TRANSACTION_BRAND = "brand"
        const val COLUMN_TRANSACTION_PRODUCT_TYPE = "product_type"
        const val COLUMN_TRANSACTION_TYPE = "transaction_type"
        const val COLUMN_TRANSACTION_AMOUNT = "amount"
        const val COLUMN_TRANSACTION_BARCODE = "barcode"
        const val COLUMN_TRANSACTION_DATE = "transaction_date"
        const val COLUMN_TRANSACTION_QUANTITY = "quantity"
        const val COLUMN_TRANSACTION_PURCHASE_PRICE = "purchase_price"

        // Bildirimler tablosu
        const val TABLE_NOTIFICATIONS = "notifications"
        const val COLUMN_NOTIFICATION_ID = "id"
        const val COLUMN_NOTIFICATION_TITLE = "title"
        const val COLUMN_NOTIFICATION_MESSAGE = "message"
        const val COLUMN_NOTIFICATION_TYPE = "type"
        const val COLUMN_NOTIFICATION_RELATED_BARCODE = "related_barcode"
        const val COLUMN_NOTIFICATION_IS_READ = "is_read"
        const val COLUMN_NOTIFICATION_CREATED_DATE = "created_date"

        // Ana sayfa taramaları tablosu oluşturma
        private const val CREATE_SCAN_RESULTS_TABLE = """
            CREATE TABLE $TABLE_SCAN_RESULTS (
                $COLUMN_SCAN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SCAN_BARCODE TEXT NOT NULL,
                $COLUMN_SCAN_BRAND TEXT NOT NULL,
                $COLUMN_SCAN_SALE_PRICE REAL NOT NULL,
                $COLUMN_SCAN_DATE INTEGER NOT NULL
            )
        """

        // Stok tablosu oluşturma
        private const val CREATE_STOCK_ITEMS_TABLE = """
            CREATE TABLE $TABLE_STOCK_ITEMS (
                $COLUMN_STOCK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_STOCK_BARCODE TEXT NOT NULL,
                $COLUMN_STOCK_BRAND TEXT NOT NULL,
                $COLUMN_STOCK_PURCHASE_PRICE REAL NOT NULL,
                $COLUMN_STOCK_DATE INTEGER NOT NULL,
                $COLUMN_STOCK_QUANTITY INTEGER NOT NULL DEFAULT 1,
                $COLUMN_STOCK_PRODUCT_TYPE TEXT NOT NULL DEFAULT ''
            )
        """

        // İşlemler tablosu oluşturma
        private const val CREATE_TRANSACTIONS_TABLE = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_TRANSACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TRANSACTION_BRAND TEXT NOT NULL,
                $COLUMN_TRANSACTION_PRODUCT_TYPE TEXT NOT NULL,
                $COLUMN_TRANSACTION_TYPE TEXT NOT NULL,
                $COLUMN_TRANSACTION_AMOUNT REAL NOT NULL,
                $COLUMN_TRANSACTION_BARCODE TEXT NOT NULL,
                $COLUMN_TRANSACTION_DATE INTEGER NOT NULL,
                $COLUMN_TRANSACTION_QUANTITY INTEGER NOT NULL DEFAULT 1,
                $COLUMN_TRANSACTION_PURCHASE_PRICE REAL NOT NULL DEFAULT 0.0
            )
        """

        // Bildirimler tablosu oluşturma
        private const val CREATE_NOTIFICATIONS_TABLE = """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COLUMN_NOTIFICATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOTIFICATION_TITLE TEXT NOT NULL,
                $COLUMN_NOTIFICATION_MESSAGE TEXT NOT NULL,
                $COLUMN_NOTIFICATION_TYPE TEXT NOT NULL,
                $COLUMN_NOTIFICATION_RELATED_BARCODE TEXT,
                $COLUMN_NOTIFICATION_IS_READ INTEGER NOT NULL DEFAULT 0,
                $COLUMN_NOTIFICATION_CREATED_DATE INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_SCAN_RESULTS_TABLE)
        db?.execSQL(CREATE_STOCK_ITEMS_TABLE)
        db?.execSQL(CREATE_TRANSACTIONS_TABLE)
        db?.execSQL(CREATE_NOTIFICATIONS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                // Version 1'den 2'ye geçiş: transactions tablosunu ekle
                db?.execSQL(CREATE_TRANSACTIONS_TABLE)
                // Eğer version 3'e de geçiyorsa, quantity sütununu ekle
                if (newVersion >= 3) {
                    db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_QUANTITY INTEGER NOT NULL DEFAULT 1")
                }
                // Eğer version 4'e de geçiyorsa, transaction tablosuna yeni sütunları ekle
                if (newVersion >= 4) {
                    db?.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANSACTION_QUANTITY INTEGER NOT NULL DEFAULT 1")
                    db?.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANSACTION_PURCHASE_PRICE REAL NOT NULL DEFAULT 0.0")
                }
                // Eğer version 5'e de geçiyorsa, notifications tablosunu ekle
                if (newVersion >= 5) {
                    db?.execSQL(CREATE_NOTIFICATIONS_TABLE)
                }
                // Eğer version 6'ya da geçiyorsa, stock_items tablosuna product_type sütunu ekle
                if (newVersion >= 6) {
                    db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_PRODUCT_TYPE TEXT NOT NULL DEFAULT ''")
                }
            }
            2 -> {
                // Version 2'den 3'e geçiş: stock_items tablosuna quantity sütunu ekle
                db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_QUANTITY INTEGER NOT NULL DEFAULT 1")
                // Eğer version 4'e de geçiyorsa, transaction tablosuna yeni sütunları ekle
                if (newVersion >= 4) {
                    db?.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANSACTION_QUANTITY INTEGER NOT NULL DEFAULT 1")
                    db?.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANSACTION_PURCHASE_PRICE REAL NOT NULL DEFAULT 0.0")
                }
                // Eğer version 5'e de geçiyorsa, notifications tablosunu ekle
                if (newVersion >= 5) {
                    db?.execSQL(CREATE_NOTIFICATIONS_TABLE)
                }
                // Eğer version 6'ya da geçiyorsa, stock_items tablosuna product_type sütunu ekle
                if (newVersion >= 6) {
                    db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_PRODUCT_TYPE TEXT NOT NULL DEFAULT ''")
                }
            }
            3 -> {
                // Version 3'den 4'e geçiş: transactions tablosuna yeni sütunları ekle
                db?.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANSACTION_QUANTITY INTEGER NOT NULL DEFAULT 1")
                db?.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANSACTION_PURCHASE_PRICE REAL NOT NULL DEFAULT 0.0")
                // Eğer version 5'e de geçiyorsa, notifications tablosunu ekle
                if (newVersion >= 5) {
                    db?.execSQL(CREATE_NOTIFICATIONS_TABLE)
                }
                // Eğer version 6'ya da geçiyorsa, stock_items tablosuna product_type sütunu ekle
                if (newVersion >= 6) {
                    db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_PRODUCT_TYPE TEXT NOT NULL DEFAULT ''")
                }
            }
            4 -> {
                // Version 4'den 5'e geçiş: notifications tablosunu ekle
                db?.execSQL(CREATE_NOTIFICATIONS_TABLE)
                // Eğer version 6'ya da geçiyorsa, stock_items tablosuna product_type sütunu ekle
                if (newVersion >= 6) {
                    db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_PRODUCT_TYPE TEXT NOT NULL DEFAULT ''")
                }
            }
            5 -> {
                // Version 5'den 6'ya geçiş: stock_items tablosuna product_type sütunu ekle
                db?.execSQL("ALTER TABLE $TABLE_STOCK_ITEMS ADD COLUMN $COLUMN_STOCK_PRODUCT_TYPE TEXT NOT NULL DEFAULT ''")
            }
            else -> {
                // Diğer durumlar için tüm tabloları yeniden oluştur
                db?.execSQL("DROP TABLE IF EXISTS $TABLE_SCAN_RESULTS")
                db?.execSQL("DROP TABLE IF EXISTS $TABLE_STOCK_ITEMS")
                db?.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
                db?.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
                onCreate(db)
            }
        }
    }
}