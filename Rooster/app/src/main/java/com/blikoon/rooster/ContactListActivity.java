package com.blikoon.rooster;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.rooster.model.Chats;
import com.blikoon.rooster.model.ChatsModel;
import com.blikoon.rooster.model.Contact;
import com.blikoon.rooster.model.ContactModel;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterEntries;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;
import java.util.List;

import static android.R.attr.value;

public class ContactListActivity extends AppCompatActivity {

    private RecyclerView contactListRecyclerView;
    private ContactListAdapter mAdapter;
    private final String TAG = "ContactListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton addNewContactFloatingButton = (FloatingActionButton) findViewById(R.id.add_new_contact_floating_button);
        addNewContactFloatingButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));

        addNewContactFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addContact();
            }
        });

        contactListRecyclerView = (RecyclerView) findViewById(R.id.contact_list_recycler_view);
        contactListRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        mAdapter = new ContactListAdapter(this);
        contactListRecyclerView.setAdapter(mAdapter);


    }

    private void addContact() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type in JID");
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //User has clicked the OK button.
                String inputText = input.getText().toString();

                if(RoosterConnectionService.getRoosterConnection().sendSubscriptionRequest(inputText))
                {
                    Log.d(TAG,"Adding Contact :"+inputText);
                    if(ContactModel.get(getApplicationContext()).addContact(new Contact(inputText, Contact.SubscriptionType.FROM_TO)))
                    {
                        Log.d(TAG,"Contact "+inputText +"Added successfuly");
                        mAdapter.notifyForUiUpdate();
                        //Start the chat activity for the added contact
                        //Inside here we start the chat activity
                        Intent intent = new Intent(ContactListActivity.this
                                ,ChatActivity.class);
                        intent.putExtra("EXTRA_CONTACT_JID",inputText);
                        startActivity(intent);


                        ///
                    }else
                    {
                        Log.d(TAG,"Could not add Contact "+inputText);
                    }

                }else
                {
                    Log.d(TAG,"Could not send subscription request");
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void sendPresenceAndMessage(String inputText) {
        Presence subscrPresense = new Presence(Presence.Type.subscribed,"Could you be my friend ?",
                10, Presence.Mode.available);

        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(inputText);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        subscrPresense.setTo(jidTo);

        RoosterConnectionService.getRoosterConnection().sendPresense(subscrPresense);


        Message messageTo = new Message(jidTo,"Hello bro");

        //Send them a message
        RoosterConnectionService.getRoosterConnection().sendMessage(messageTo);
    }

    private void retrieveUserRoster() {
        Collection<RosterEntry> entries = RoosterConnectionService.getRoosterConnection().getRosterEntries();

        Log.d(TAG,"The current contact has "+entries.size() + " contacts in his roster");

        for (RosterEntry entry : entries) {
            RosterPacket.ItemType itemType=   entry.getType();

            Log.d(TAG,"-------------------------------");
            Log.d(TAG,"Roster entry for  :"+ entry.toString());

            if(itemType == RosterPacket.ItemType.none)
            {
                Log.d(TAG,"Subscription is none");
            }
            if(itemType == RosterPacket.ItemType.from)
            {
                Log.d(TAG,"Subscription is from");
            }
            if(itemType == RosterPacket.ItemType.to)
            {
                Log.d(TAG,"Subscription is to");
            }
            if(itemType == RosterPacket.ItemType.both)
            {
                Log.d(TAG,"Subscription is both");
            }
        }
    }

    private class ContactHolder extends RecyclerView.ViewHolder
    {
        private TextView jidTexView;
        private TextView subscriptionTypeTextView;
        private Contact mContact;
        private ImageView profile_image;
        public ContactHolder(View itemView)
        {
            super(itemView);

            jidTexView = (TextView) itemView.findViewById(R.id.contact_jid);
            subscriptionTypeTextView = (TextView) itemView.findViewById(R.id.suscription_type);
            profile_image = (ImageView) itemView.findViewById(R.id.profile_contact);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.d(TAG,"Clicked on a contact item. JID is :" +jidTexView.getText().toString());

                    /*
                        If a chat is new
                        {
                            .add them to chat_list db and open a chat window with them
                        }else
                        {
                            .add them to chat_list db and open a chat window with them

                        }
                     */

                    List<Chats> chats = ChatsModel.get(getApplicationContext()).getChatsByJid(jidTexView.getText().toString());
                    if( chats.size() == 0)
                    {
                        Log.d(TAG, jidTexView.getText().toString() + " is a new chat, adding them.");
                        ChatsModel.get(getApplicationContext()).addChat(new Chats(jidTexView.getText().toString(), Chats.ContactType.ONE_ON_ONE));

                        //Inside here we start the chat activity
                        Intent intent = new Intent(ContactListActivity.this
                                ,ChatActivity.class);
                        intent.putExtra("EXTRA_CONTACT_JID",jidTexView.getText().toString());
                        startActivity(intent);

                        finish();

                    }else
                    {
                        Log.d(TAG, jidTexView.getText().toString() + " is ALREADY in chat db.Just opening conversation");
                        //Inside here we start the chat activity
                        Intent intent = new Intent(ContactListActivity.this
                                ,ChatActivity.class);
                        intent.putExtra("EXTRA_CONTACT_JID",jidTexView.getText().toString());
                        startActivity(intent);

                        finish();

                    }

                }
            });
        }


        public void bindContact(Contact contact)
        {
            mContact  = contact;
            if (mContact == null)
            {
                Log.d(TAG,"Trying to work on a null Contact object ,returning.");
                return;
            }
            jidTexView.setText(mContact.getJid());

            //Profile image
            RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
//            if( rc != null)
//            {
//                byte[] image_byte_array = rc.getUserAvatar(contact.getJid());
//                if( image_byte_array != null)
//                {
//                    // Retrieve the avatar and put it in the profile image view
//                    Drawable image = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(image_byte_array, 0, image_byte_array.length));
//                    profile_image.setImageDrawable(image);
//                }
//
//            }
            if(rc != null)
            {
                String imageAbsPath = rc.getProfileImageAbsolutePath(mContact.getJid());
                if ( imageAbsPath != null)
                {
                    Drawable d = Drawable.createFromPath(imageAbsPath);
                    profile_image.setImageDrawable(d);
                }

            }



            /**
             * Code for subscription status display
             *
             *          [FROM - TO]
             *          00---------->NONE_NONE
             *          0#---------->NONE_PENDING
             *          01---------->NONE_TO
             *
             *          #0---------->PENDING_NONE
             *          ##---------->PENDING_PENDING
             *          #1---------->PENDING_TO
             *
             *          10---------->FROM_NONE
             *          1#---------->FROM_PENDING
             *          11---------->FROM_TO                */

            if(contact.getSubscriptionType() == Contact.SubscriptionType.NONE_NONE)
            {
                subscriptionTypeTextView.setText("00");
            }else if(contact.getSubscriptionType() == Contact.SubscriptionType.NONE_PENDING)
            {
                subscriptionTypeTextView.setText("0#");
            }else if(contact.getSubscriptionType() == Contact.SubscriptionType.NONE_TO)
            {
                subscriptionTypeTextView.setText("01");
            }else if(contact.getSubscriptionType() == Contact.SubscriptionType.PENDING_NONE)
            {
                subscriptionTypeTextView.setText("#0");
            }else if(contact.getSubscriptionType() == Contact.SubscriptionType.PENDING_PENDING)
            {
                subscriptionTypeTextView.setText("##");
            }else if(contact.getSubscriptionType() == Contact.SubscriptionType.PENDING_TO)
            {
                subscriptionTypeTextView.setText("#1");
            }


            else if(contact.getSubscriptionType() == Contact.SubscriptionType.FROM_NONE)
            {
                subscriptionTypeTextView.setText("10");
            }
            else if(contact.getSubscriptionType() == Contact.SubscriptionType.FROM_PENDING)
            {
                subscriptionTypeTextView.setText("1#");
            }
            else if(contact.getSubscriptionType() == Contact.SubscriptionType.FROM_TO)
            {
                subscriptionTypeTextView.setText("11");
            }
            else
            {
                subscriptionTypeTextView.setText("INDETERMINATE");
            }

        }
    }


    private class ContactListAdapter extends RecyclerView.Adapter<ContactListActivity.ContactHolder>
    {
        private List<Contact> mContacts;
        private Context mContext;

        public ContactListAdapter(Context context)
        {
            mContacts = ContactModel.get(context).getContacts();
            mContext = context;
            Log.d(TAG,"Constructor of ChatListAdapter , the size of the backing list is :" +mContacts.size());
        }

        public void notifyForUiUpdate()
        {
            mContacts = ContactModel.get(mContext).getContacts();
            notifyDataSetChanged();

        }

        @Override
        public ContactListActivity.ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater
                    .inflate(R.layout.list_item_contact, parent,
                            false);
            return new ContactListActivity.ContactHolder(view);
        }

        @Override
        public void onBindViewHolder(ContactListActivity.ContactHolder holder, int position) {
            Contact contact = mContacts.get(position);
            holder.bindContact(contact);

        }

        @Override
        public int getItemCount() {
            return mContacts.size();
        }
    }
}
