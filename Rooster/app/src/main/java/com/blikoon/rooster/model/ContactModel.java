package com.blikoon.rooster.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.blikoon.rooster.persistance.ChatsCursorWrapper;
import com.blikoon.rooster.persistance.ContactCursorWrapper;
import com.blikoon.rooster.persistance.DatabaseBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gakwaya on 2017/11/10.
 */

public class ContactModel {


    private static final String TAG = "ContactModel";
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

    public List<String> getContactJidStrings()
    {
        List<Contact> contacts = getContacts();
        List<String> stringJids = new ArrayList<>();

        for(Contact contact :contacts)
        {
            stringJids.add(contact.getJid());

        }
        return stringJids;

    }

    public Contact getContactByJidString(String jidString)
    {
        List<Contact> contacts = getContacts();
        List<String> stringJids = new ArrayList<>();

        Contact mContact = null;

        Log.d(TAG,"Looping around contacts============");

        for(Contact contact :contacts)
        {
            Log.d(TAG,"Contact Jid :"+contact.getJid());
            Log.d(TAG,"Subscription type :"+ contact.getTypeStringValue(contact.getSubscriptionType()));
            if(contact.getJid().equals(jidString))
            {
                mContact = contact;
            }
        }
        return mContact;
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
        //TODO: Check if contact already in db before adding.
        ContentValues values = c.getContentValues();
        if ((mDatabase.insert(Contact.TABLE_NAME, null, values)== -1))
        {
            return false;
        }else
        {
            return true;
        }
    }

    public boolean updateContactSubscription(String jidString , Contact.SubscriptionType subscrType)
    {
        Contact contact = getContactByJidString(jidString);

        Log.d(TAG,"------------------BEFORE--------------------------");

        Log.d(TAG,"Got contact :" + contact.getJid());
        Log.d(TAG,"Their subscription type is :" + contact.getTypeStringValue(contact.getSubscriptionType()));


        Log.d(TAG,"---------------------------------------------------------");


        //Update the contact
        contact.setSubscriptionType(subscrType);


        Log.d(TAG,"------------------AFTER UPDATE CONTACT--------------------------");

        Log.d(TAG,"Got contact :" + contact.getJid());
        Log.d(TAG,"Their subscription type is :" + contact.getTypeStringValue(contact.getSubscriptionType()));


        Log.d(TAG,"---------------------------------------------------------");


        //Get new contectvalues to add to db
        ContentValues values = contact.getContentValues();


        String valueJid = values.getAsString("jid");
        String valueSubType = values.getAsString("subscriptionType");

        Log.d(TAG,"---------------From content values-------------");
        Log.d(TAG," valueJid :"+valueJid);
        Log.d(TAG,"valueSubType :"+ valueSubType);

        //db.update returns the number of affected rows in the db, if this return value is not zero, we succeeded

        int rows = mDatabase.update(Contact.TABLE_NAME, values, "jid = ? ", new String[] { jidString } );
        Log.d(TAG,rows + " rows affected in db");

        if( rows != 0)
        {
            Log.d(TAG,"DB record update successful ");
            Contact contact1 = getContactByJidString(jidString);

            Log.d(TAG,"------------------AFTER UPDATE DB--------------------------");

            Log.d(TAG,"Got contact :" + contact1.getJid());
            Log.d(TAG,"Their subscription type is :" + contact1.getTypeStringValue(contact1.getSubscriptionType()));


            Log.d(TAG,"---------------------------------------------------------");


            return true;
        }
        return false;
    }

}
