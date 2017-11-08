package com.blikoon.rooster.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.blikoon.rooster.persistance.ChatMessageCursorWrapper;
import com.blikoon.rooster.persistance.DatabaseBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 2017/11/2.
 */

public class ChatMessageModel {


    public interface OnMessageAddListener {
         void onMessageAdd();
    }

    private static ChatMessageModel sChatMessageModel;
    private SQLiteDatabase mDatabase;
    private Context mContext;



    private String counterpartJid;

    public OnMessageAddListener getMessageAddListener() {
        return messageAddListener;
    }

    public void setMessageAddListener(OnMessageAddListener messageAddListener) {
        this.messageAddListener = messageAddListener;
    }

    OnMessageAddListener messageAddListener;




    public static ChatMessageModel get(Context context, String counterpartJid)
    {
        if(sChatMessageModel == null)
        {
            sChatMessageModel = new ChatMessageModel(context,counterpartJid);
        }
        /** If the model is already there, make sure it is used to retrieve the correct messages from the db.
         * Messages belonging to counterpartJid */
        sChatMessageModel.setCounterpartJid(counterpartJid);
        return sChatMessageModel;
    }

    private ChatMessageModel(Context context ,String counterpartJid)
    {
        mContext = context.getApplicationContext();
        mDatabase = DatabaseBackend.getInstance(mContext).getWritableDatabase();
        this.counterpartJid = counterpartJid;
    }

    public void setCounterpartJid(String counterpartJid) {
        this.counterpartJid = counterpartJid;
    }

    public boolean addMessage(ChatMessage c)
    {
        ContentValues values = c.getContentValues();
        if ((mDatabase.insert(ChatMessage.TABLE_NAME, null, values)== -1))
        {
            return false;
        }else
        {
            return true;
        }
//        messageAddListener.onMessageAdd();
    }


    public List<ChatMessage> getMessages()
    {
        List<ChatMessage> messages = new ArrayList<>();

        ChatMessageCursorWrapper cursor = queryMessages("contactJid= ?",new String [] {counterpartJid});

        try
        {
            cursor.moveToFirst();
            while( !cursor.isAfterLast())
            {
                messages.add(cursor.getChatMessage());
                cursor.moveToNext();
            }

        }finally {
            cursor.close();
        }
        return messages;
    }



    private ChatMessageCursorWrapper queryMessages(String whereClause , String [] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                ChatMessage.TABLE_NAME,
                null ,//Columns - null selects all columns
                whereClause,
                whereArgs,
                null ,//groupBy
                null, //having
                null//orderBy
        );
        return new ChatMessageCursorWrapper(cursor);
    }

}
