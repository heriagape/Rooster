package com.blikoon.rooster.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.blikoon.rooster.persistance.ChatsCursorWrapper;
import com.blikoon.rooster.persistance.ContactCursorWrapper;
import com.blikoon.rooster.persistance.DatabaseBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 2017/11/10.
 */

public class ContactModel {


    private static ContactModel sContactMoel;
    private SQLiteDatabase mDatabase;
    private Context mContext;

    public static ContactModel get(Context context)
    {
        if(sContactMoel == null)
        {
            sContactMoel = new ContactModel(context);
        }
        /** If the model is already there, make sure it is used to retrieve the correct messages from the db.
         * Messages belonging to counterpartJid */
        //sChatMessageModel.setCounterpartJid(counterpartJid);
        return sContactMoel;
    }
    private ContactModel(Context context)
    {
        mContext = context.getApplicationContext();
        mDatabase = DatabaseBackend.getInstance(mContext).getWritableDatabase();
    }


    public List<Contact> getContacts()
    {
        List<Contact> contacts = new ArrayList<>();

        ContactCursorWrapper cursor = queryMessages(null,null);

        try
        {
            cursor.moveToFirst();
            while( !cursor.isAfterLast())
            {
                contacts.add(cursor.getContact());
                cursor.moveToNext();
            }

        }finally {
            cursor.close();
        }
        return contacts;
    }



    private ContactCursorWrapper queryMessages(String whereClause , String [] whereArgs)
    {
        Cursor cursor = mDatabase.query(
                Contact.TABLE_NAME,
                null ,//Columns - null selects all columns
                whereClause,
                whereArgs,
                null ,//groupBy
                null, //having
                null//orderBy
        );
        return new ContactCursorWrapper(cursor);
    }


    public boolean addContact(Contact c)
    {
        ContentValues values = c.getContentValues();
        if ((mDatabase.insert(Contact.TABLE_NAME, null, values)== -1))
        {
            return false;
        }else
        {
            return true;
        }
    }





}
