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
        //Subscription type should catter for the from and to channels. We should simultaneously know the FROM and TO subscription information
        //FROM  - TO
        NONE_NONE,//No presence subscription
        NONE_PENDING,
        NONE_TO,

        PENDING_NONE,
        PENDING_PENDING,
        PENDING_TO,

        FROM_NONE,
        FROM_PENDING,
        FROM_TO


//        FROM, // They are subscribed to my presence
//        FROM_PENDING,// They asked to be subscribed to my presence, by I haven't approved or denied yet
//        TO,   // I am subscribed to their presence
//        TO_PENDING , // I asked to be subscribed to their presence, but they haven't approved or denied yet
//        BOTH  // We are subscribed to each others presence
    }

    public String getTypeStringValue(SubscriptionType type)
    {
        if(type== SubscriptionType.NONE_NONE)
            return "NONE_NONE";
        else if(type == SubscriptionType.NONE_PENDING)
            return "NONE_PENDING";
        else if(type == SubscriptionType.NONE_TO)
            return "NONE_TO";
        else if(type == SubscriptionType.PENDING_NONE)
            return "PENDING_NONE";
        else if(type == SubscriptionType.PENDING_PENDING)
            return "PENDING_PENDING";
        else if(type == SubscriptionType.PENDING_TO)
            return "PENDING_TO";
        else if(type == SubscriptionType.FROM_NONE)
            return "FROM_NONE";
        else if(type == SubscriptionType.FROM_PENDING)
            return "FROM_PENDING";
        else if(type == SubscriptionType.FROM_TO)
            return "FROM_TO";
        else
            return "INDETERMINATE";
    }
}
