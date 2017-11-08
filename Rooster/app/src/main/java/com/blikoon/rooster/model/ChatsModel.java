package com.blikoon.rooster.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.blikoon.rooster.persistance.ChatMessageCursorWrapper;
import com.blikoon.rooster.persistance.ChatsCursorWrapper;
import com.blikoon.rooster.persistance.DatabaseBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 2017/11/8.
 */

public class ChatsModel {

    private static ChatsModel sChatsModel;
    private SQLiteDatabase mDatabase;
    private Context mContext;

    public static ChatsModel get(Context context)
    {
        if(sChatsModel == null)
        {
            sChatsModel = new ChatsModel(context);
        }
        /** If the model is already there, make sure it is used to retrieve the correct messages from the db.
         * Messages belonging to counterpartJid */
        //sChatMessageModel.setCounterpartJid(counterpartJid);
        return sChatsModel;
    }
    private ChatsModel(Context context)
    {
        mContext = context.getApplicationContext();
        mDatabase = DatabaseBackend.getInstance(mContext).getWritableDatabase();
    }


    public List<Chats> getChats()
    {
        List<Chats> chats = new ArrayList<>();

        ChatsCursorWrapper cursor = queryMessages(null,null);

        try
        {
            cursor.moveToFirst();
            while( !cursor.isAfterLast())
            {
                chats.add(cursor.getChat());
                cursor.moveToNext();
            }

        }finally {
            cursor.close();
        }
        return chats;
    }

    private ChatsCursorWrapper queryMessages(String whereClause , String [] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                Chats.TABLE_NAME,
                null ,//Columns - null selects all columns
                whereClause,
                whereArgs,
                null ,//groupBy
                null, //having
                null//orderBy
        );
        return new ChatsCursorWrapper(cursor);
    }


    public boolean addChat(Chats c)
    {
        ContentValues values = c.getContentValues();
        if ((mDatabase.insert(Chats.TABLE_NAME, null, values)== -1))
        {
            return false;
        }else
        {
            return true;
        }
    }




}
