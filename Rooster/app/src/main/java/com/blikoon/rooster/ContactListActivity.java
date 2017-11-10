package com.blikoon.rooster;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;

import com.blikoon.rooster.model.Chats;
import com.blikoon.rooster.model.ChatsModel;
import com.blikoon.rooster.model.Contact;
import com.blikoon.rooster.model.ContactModel;

import java.util.List;

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

                Log.d(TAG,"Adding Contact :"+inputText);
                if(ContactModel.get(getApplicationContext()).addContact(new Contact(inputText, Contact.SubscriptionType.BOTH)))
                {
                    Log.d(TAG,"Contact "+inputText +"Added successfuly");
                    mAdapter.notifyForUiUpdate();
                }else
                {
                    Log.d(TAG,"Could not add Contact "+inputText);
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

    private class ContactHolder extends RecyclerView.ViewHolder
    {
        private TextView jidTexView;
        private TextView subscriptionTypeTextView;
        private Contact mContact;
        public ContactHolder(View itemView)
        {
            super(itemView);

            jidTexView = (TextView) itemView.findViewById(R.id.contact_jid);
            subscriptionTypeTextView = (TextView) itemView.findViewById(R.id.suscription_type);

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
                Log.d(TAG,"Trying to work on a null Chats object ,returning.");
                return;
            }
            jidTexView.setText(mContact.getJid());
            subscriptionTypeTextView.setText("This is the message we exchanged the last time we talked.");

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
