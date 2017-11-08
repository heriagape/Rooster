package com.blikoon.rooster.model;

import android.content.ContentValues;

/**
 * Created by gakwaya on 2017/11/1.
 */

/** Chats encapsulates the data shown in each single item in the ChatListActivity. It also wraps around
 * the schema of the table to store chats information in the database.*/

public class Chats {

    private String jid;
    private ContactType contactType;



    private String lastMessageTimeStamp;


    public static final String TABLE_NAME = "chats";

    public static final class Cols
    {
        public static final String jid = "jid";
        public static final String contactType = "contactType";
    }


    public Chats( String jid, ContactType contactType) {
        this.jid = jid;
        this.contactType = contactType;
    }

    public String getLastMessageTimeStamp() {
        return lastMessageTimeStamp;
    }

    public void setLastMessageTimeStamp(String lastMessageTimeStamp) {
        this.lastMessageTimeStamp = lastMessageTimeStamp;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public ContactType getContactType() {
        return contactType;
    }

    public void setContactType(ContactType contactType) {
        this.contactType = contactType;
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put("jid", jid);
        values.put("contactType",getTypeStringValue(contactType));

        return values;

    }

    public String getTypeStringValue(ContactType type)
    {
        if(type== ContactType.ONE_ON_ONE)
            return "ONE_ON_ONE";
        else
            return "GROUP";
    }

    public enum ContactType{
        ONE_ON_ONE,GROUP
    }
}
