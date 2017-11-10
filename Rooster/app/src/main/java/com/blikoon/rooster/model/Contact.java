package com.blikoon.rooster.model;

import android.content.ContentValues;

/**
 * Created by gakwaya on 2017/11/10.
 */

public class Contact {

    private String jid;
    private SubscriptionType subscriptionType;

    public static final String TABLE_NAME = "contacts";

    public static final class Cols
    {
        public static final String jid = "jid";
        public static final String SubscriptionType = "subscriptionType";
    }

    public Contact(String jid, SubscriptionType subscriptionType) {
        this.jid = jid;
        this.subscriptionType = subscriptionType;
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put("jid", jid);
        values.put("subscriptionType",getTypeStringValue(subscriptionType));

        return values;

    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public enum SubscriptionType{
        NONE,//No presense subscription
        FROM, // He asked to be friends with me but I haven't accepted yet
        TO,   // I asked dto be friends with them but they haven't accepted yet
        BOTH  // We have accepted each other's friendship requests.
    }

    public String getTypeStringValue(SubscriptionType type)
    {
        if(type== SubscriptionType.NONE)
            return "NONE";
        else if(type == SubscriptionType.FROM)
            return "FROM";
        else if(type == SubscriptionType.TO)
            return "TO";
        else
            return "BOTH";
    }
}
