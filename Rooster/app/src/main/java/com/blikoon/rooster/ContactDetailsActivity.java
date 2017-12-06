package com.blikoon.rooster;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.blikoon.rooster.model.Contact;
import com.blikoon.rooster.model.ContactModel;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class ContactDetailsActivity extends AppCompatActivity {

    private String contactJid;
    private ImageView profileImage;
    private CheckBox fromCheckBox;
    private CheckBox toCheckBox;
    private CardView requestPresenseBox;
    private Button requestPresenceButton;
    private Context mApplicationContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        //Get the contact Jid
        Intent intent = getIntent();
        contactJid = intent.getStringExtra("EXTRA_COUNTERPART_JID");
        setTitle(contactJid);

        mApplicationContext=getApplicationContext();
        //Set the profile image if available
        profileImage = (ImageView) findViewById(R.id.contact_details_user_profile);
        RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
        if (rc != null)
        {
            String imagepath = rc.getProfileImageAbsolutePath(contactJid);
            if ( imagepath != null)
            {
                Drawable d = Drawable.createFromPath(imagepath);
                profileImage.setImageDrawable(d);

            }
        }

        fromCheckBox = (CheckBox) findViewById(R.id.them_to_me);

        toCheckBox = (CheckBox) findViewById(R.id.me_to_tem);

        requestPresenseBox = (CardView) findViewById(R.id.request_presence_box);

        requestPresenceButton = (Button) findViewById(R.id.request_presence_button);
        requestPresenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send a subscribe presence to the counterpart
                Jid jidTo = null;
                try {
                    jidTo = JidCreate.from(contactJid);
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }
                Presence subscribe = new Presence(jidTo, Presence.Type.subscribe);
                RoosterConnectionService.getRoosterConnection().sendPresense(subscribe);

                //Update subscription status in database
                Contact contact = ContactModel.get(getApplicationContext()).getContactByJidString(contactJid);
                Contact.SubscriptionType subType = contact.getSubscriptionType();
                if(subType == Contact.SubscriptionType.NONE_NONE)
                {
                     ContactModel.get(mApplicationContext).updateContactSubscription(contactJid, Contact.SubscriptionType.NONE_PENDING);

                }else if (subType == Contact.SubscriptionType.NONE_PENDING)
                {
                    ContactModel.get(mApplicationContext).updateContactSubscription(contactJid, Contact.SubscriptionType.NONE_PENDING);

                }else if (subType == Contact.SubscriptionType.NONE_TO)
                {
                    //Nothing relevant to do here
                }else if (subType == Contact.SubscriptionType.PENDING_NONE)
                {
                    ContactModel.get(mApplicationContext).updateContactSubscription(contactJid, Contact.SubscriptionType.PENDING_PENDING);

                }else if (subType == Contact.SubscriptionType.PENDING_PENDING)
                {
                    //Nothing relevant to do here
                }else if (subType == Contact.SubscriptionType.PENDING_TO)
                {
                    //Nothing relevant to do here
                }else if (subType == Contact.SubscriptionType.FROM_NONE)
                {
                    ContactModel.get(mApplicationContext).updateContactSubscription(contactJid, Contact.SubscriptionType.FROM_PENDING);

                }else if (subType == Contact.SubscriptionType.FROM_PENDING)
                {
                    //Nothing relevant to do here
                }else if (subType == Contact.SubscriptionType.FROM_TO)
                {
                    //Nothing relevant to do here
                }
            }
        });

        Contact contact = ContactModel.get(getApplicationContext()).getContactByJidString(contactJid);
        Contact.SubscriptionType subType = contact.getSubscriptionType();
        //Decide which buttons to show in which status based on the current subscription status.
        if(subType == Contact.SubscriptionType.NONE_NONE)
        {
            fromCheckBox.setChecked(false);
            toCheckBox.setChecked(false);
            requestPresenseBox.setVisibility(View.VISIBLE);

        }else if (subType == Contact.SubscriptionType.NONE_PENDING)
        {
            fromCheckBox.setChecked(false);
            toCheckBox.setChecked(false);
            toCheckBox.setText("I can se their online status [PENDING]");
            requestPresenseBox.setVisibility(View.INVISIBLE);

        }else if (subType == Contact.SubscriptionType.NONE_TO)
        {
            fromCheckBox.setChecked(false);
            toCheckBox.setChecked(true);
            requestPresenseBox.setVisibility(View.INVISIBLE);

        }else if (subType == Contact.SubscriptionType.PENDING_NONE)
        {
            fromCheckBox.setChecked(false);
            fromCheckBox.setText("They can see my online status[PENDING]");
            toCheckBox.setChecked(false);
            requestPresenseBox.setVisibility(View.VISIBLE);

        }else if (subType == Contact.SubscriptionType.PENDING_PENDING)
        {
            fromCheckBox.setChecked(false);
            fromCheckBox.setText("They can see my online status[PENDING]");
            toCheckBox.setChecked(false);
            toCheckBox.setText("I can see their online status[PENDING]");
            requestPresenseBox.setVisibility(View.INVISIBLE);

        }else if (subType == Contact.SubscriptionType.PENDING_TO)
        {
            fromCheckBox.setChecked(false);
            fromCheckBox.setText("They can see my online status[PENDING]");
            toCheckBox.setChecked(true);
            requestPresenseBox.setVisibility(View.INVISIBLE);


        }else if (subType == Contact.SubscriptionType.FROM_NONE)
        {
            fromCheckBox.setChecked(true);
            toCheckBox.setChecked(false);
            requestPresenseBox.setVisibility(View.VISIBLE);


        }else if (subType == Contact.SubscriptionType.FROM_PENDING)
        {
            fromCheckBox.setChecked(true);
            toCheckBox.setChecked(false);
            toCheckBox.setText("I can see their online status[PENDING]");
            requestPresenseBox.setVisibility(View.INVISIBLE);

        }else if (subType == Contact.SubscriptionType.FROM_TO)
        {
            fromCheckBox.setChecked(true);
            toCheckBox.setChecked(true);
            requestPresenseBox.setVisibility(View.INVISIBLE);
        }
    }

}
