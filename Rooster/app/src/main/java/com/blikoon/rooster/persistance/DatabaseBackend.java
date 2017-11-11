package com.blikoon.rooster.persistance;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.Chats;
import com.blikoon.rooster.model.Contact;

/**
 * Created by gakwaya on 2017/11/1.
 */

public class DatabaseBackend extends SQLiteOpenHelper {

    private static DatabaseBackend instance = null;

    private static final String DATABASE_NAME = "rooster_db";
    private static final int DATABASE_VERSION = 4;

    private static String CREATE_CHAT_LIST_STATEMENT = "create table "
            + Chats.TABLE_NAME + "("
            + Chats.Cols.contactType + " TEXT, " + Chats.Cols.jid + " TEXT"
           + ");";



    private static String CREATE_CONTACT_LIST_STATEMENT = "create table "
            + Contact.TABLE_NAME + "("
            + Contact.Cols.SubscriptionType + " TEXT, " + Contact.Cols.jid + " TEXT"
            + ");";




    private static String CREATE_CHAT_MESSAGES_STATEMENT = "create table "
            + ChatMessage.TABLE_NAME + "("
            + ChatMessage.Cols.message + " TEXT, "
            + ChatMessage.Cols.messageType + " TEXT, "
            + ChatMessage.Cols.timestamp + " NUMBER, "
            + ChatMessage.Cols.contactJid + " TEXT"
             + ");";


    public static synchronized DatabaseBackend getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseBackend(context);
        }
        return instance;
    }


    private DatabaseBackend(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CONTACT_LIST_STATEMENT);
        db.execSQL(CREATE_CHAT_LIST_STATEMENT);
        db.execSQL(CREATE_CHAT_MESSAGES_STATEMENT);
    }

    /**
     * Do better handling of db updates. We follow the approach advised here : https://thebhwgroup.com/blog/how-android-sqlite-onupgrade
     * A copy of a template for cheating purposes has been kept in our bag of tricks here : https://github.com/blikoon/Ubufundi/blob/master/Android_SQLite_template/SQLiteHelper.java
     * You can find more info on sql manipulations here : https://www.techonthenet.com/sqlite/index.php
     * */

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 4)
        {
            db.execSQL("DROP TABLE IF EXISTS " + Chats.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Contact.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ChatMessage.TABLE_NAME);
            onCreate(db);
        }


    }


}
