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

        if (subscriptionTypeString.equals("NONE_NONE")) {
            subscriptionType = Contact.SubscriptionType.NONE_NONE;
        } else if (subscriptionTypeString.equals("NONE_PENDING")) {
            subscriptionType = Contact.SubscriptionType.NONE_PENDING;
        } else if (subscriptionTypeString.equals("NONE_TO")) {
            subscriptionType = Contact.SubscriptionType.NONE_TO;
        } else if (subscriptionTypeString.equals("PENDING_NONE")) {
            subscriptionType = Contact.SubscriptionType.PENDING_NONE;
        }else if (subscriptionTypeString.equals("PENDING_PENDING")) {
            subscriptionType = Contact.SubscriptionType.PENDING_PENDING;
        } else if (subscriptionTypeString.equals("PENDING_TO")) {
            subscriptionType = Contact.SubscriptionType.PENDING_TO;
        }

        else if (subscriptionTypeString.equals("FROM_NONE")) {
            subscriptionType = Contact.SubscriptionType.FROM_NONE;
        }else if (subscriptionTypeString.equals("FROM_PENDING")) {
            subscriptionType = Contact.SubscriptionType.FROM_PENDING;
        } else if (subscriptionTypeString.equals("FROM_TO")) {
            subscriptionType = Contact.SubscriptionType.FROM_TO;
        }

        Contact contact = new Contact(jid, subscriptionType);
        return contact;
    }

}
