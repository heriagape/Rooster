package com.blikoon.rooster.persistance;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.blikoon.rooster.model.Contact;

/**
 * Created by gakwaya on 2017/11/10.
 */

public class ContactCursorWrapper extends CursorWrapper {

    public ContactCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Contact getContact() {

        String subscriptionTypeString = getString(getColumnIndex(Contact.Cols.SubscriptionType));
        String jid = getString(getColumnIndex(Contact.Cols.jid));


        Contact.SubscriptionType subscriptionType = null;

        if (subscriptionTypeString.equals("NONE")) {
            subscriptionType = Contact.SubscriptionType.NONE;
        } else if (subscriptionTypeString.equals("FROM")) {
            subscriptionType = Contact.SubscriptionType.FROM;
        }
        else if (subscriptionTypeString.equals("TO")) {
            subscriptionType = Contact.SubscriptionType.TO;
        }
        else if (subscriptionTypeString.equals("BOTH")) {
            subscriptionType = Contact.SubscriptionType.BOTH;
        }

        Contact contact = new Contact(jid, subscriptionType);
        return contact;
    }

}
