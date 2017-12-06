package com.blikoon.rooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.blikoon.rooster.adapters.ChatMessagesAdapter;
import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.ChatMessageModel;
import com.blikoon.rooster.model.Contact;
import com.blikoon.rooster.model.ContactModel;
import com.blikoon.rooster.ui.InsetDecoration;
import com.blikoon.rooster.ui.KeyboardUtil;

public class ChatActivity extends ActionBarActivity implements
        ChatMessagesAdapter.OnItemClickListener,ChatMessagesAdapter.OnInformRecyclerViewToScrollDownListener,
        KeyboardUtil.KeyboardVisibilityListener {

    private static final String TAG ="ChatActivity";

    private BroadcastReceiver mBroadcastReceiver;


    private RecyclerView mRecyclerView;
    private ChatMessagesAdapter mAdapter;
    private LinearLayoutManager mVerticalManager;
    private EditText textInputTextEdit;
    private ImageButton textSendButton;
    private String counterpartJid;

    private View snackbar;
    private TextView snackBarActionAccept;
    private TextView snackBarActionDeny;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);



        //Get the counterpart Jid
        Intent intent = getIntent();
        counterpartJid = intent.getStringExtra("EXTRA_CONTACT_JID");
        setTitle(counterpartJid);


        mRecyclerView = (RecyclerView) findViewById(R.id.chat_message_recycler_view);

        mVerticalManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        mAdapter = new ChatMessagesAdapter(this,counterpartJid);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setmOnInformRecyclerViewToScrollDownListener(this);
        mRecyclerView.setAdapter(mAdapter);

        //Apply margins decoration to all collections
        mRecyclerView.addItemDecoration(new InsetDecoration(this));

        //Default to vertical layout
        mRecyclerView.setLayoutManager(mVerticalManager);


        //Send text EditText and Send ImageButton
        textInputTextEdit = (EditText) findViewById(R.id.textinput);

        textSendButton = (ImageButton)findViewById(R.id.textSendButton);
        textSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                if (RoosterConnectionService.getState().equals(RoosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sending Message");
                    //Send the message to the server

                    Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
                    intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,
                            textInputTextEdit.getText().toString());
                    intent.putExtra(RoosterConnectionService.BUNDLE_TO, counterpartJid);

                    sendBroadcast(intent);
                /* Here you should have the logic to send the message with whatever it is you are using [xmpp|angularJs|...]
                * after that you update your model with the new message. Note that we update the model here, not the view. The view picks up the data from the model when it needs to.*/
                    ChatMessageModel.get(getApplicationContext(),counterpartJid).addMessage(new ChatMessage(textInputTextEdit.getText().toString(),System.currentTimeMillis(), ChatMessage.Type.SENT,counterpartJid));

                    //Notify the recyclerView on the message Add
                    mAdapter.onMessageAdd();
                    //Clear the text from the edit text
                    textInputTextEdit.getText().clear();

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });



        snackbar = findViewById(R.id.snackbar);
        //Get the subscription data and decide whether or not to show the snackbar.
        Log.d(TAG,"Getting contact data for :" + counterpartJid);
        Contact contact = ContactModel.get(this).getContactByJidString(counterpartJid);
        Log.d(TAG,"We got a contact with JID :" + contact.getJid());

        if( contact.getSubscriptionType() == Contact.SubscriptionType.PENDING_NONE ||
                contact.getSubscriptionType() == Contact.SubscriptionType.PENDING_PENDING ||
                contact.getSubscriptionType() == Contact.SubscriptionType.PENDING_TO)
        {
            Log.d(TAG," Your subscription to "+ contact.getJid() + " is in the FROM direction is in pending state. Should show the snackbar");
            snackbar.setVisibility(View.VISIBLE);
        }else
        {
            Log.d(TAG,"Your subscription is not FROM_PENDING");
        }

        snackBarActionAccept = (TextView) findViewById(R.id.snackbar_action_accept);
        snackBarActionAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //User accepts presence subscription
                Log.d(TAG," Accept presence subscription from :" + counterpartJid);
                RoosterConnectionService.getRoosterConnection().subscribed(counterpartJid);
                //Update the user subscription status

                Contact contact = ContactModel.get(getApplicationContext())
                        .getContactByJidString(counterpartJid);

                Contact.SubscriptionType subType = contact.getSubscriptionType();

                if(subType == Contact.SubscriptionType.NONE_NONE)
                {
                    //--> FROM_NONE
                    Log.d(TAG,"NONE_NONE :The contact sub type is :" + contact.getTypeStringValue(subType));
                    ContactModel.get(getApplicationContext()).updateContactSubscription(counterpartJid, Contact.SubscriptionType.FROM_NONE);


                }else if (subType == Contact.SubscriptionType.NONE_PENDING)
                {
                    //--> FROM_PENDING
                    Log.d(TAG,"NONE_PENDING :The contact sub type is :" + contact.getTypeStringValue(subType));
                    ContactModel.get(getApplicationContext()).updateContactSubscription(counterpartJid, Contact.SubscriptionType.FROM_PENDING);


                }else if (subType == Contact.SubscriptionType.NONE_TO)
                {
                    // -- > FROM_TO
                    Log.d(TAG,"NONE_TO :The contact sub type is :" + contact.getTypeStringValue(subType));
                    ContactModel.get(getApplicationContext()).updateContactSubscription(counterpartJid, Contact.SubscriptionType.FROM_TO);


                }else if (subType == Contact.SubscriptionType.PENDING_NONE)
                {
                    //-->FROM_NONE
                    Log.d(TAG,"PENDING_NONE :The contact sub type is :" + contact.getTypeStringValue(subType));
                    ContactModel.get(getApplicationContext()).updateContactSubscription(counterpartJid, Contact.SubscriptionType.FROM_NONE);


                }else if (subType == Contact.SubscriptionType.PENDING_PENDING)
                {
                    //--> FROM_PENDING
                    Log.d(TAG,"PENDING_PENDING :The contact sub type is :" + contact.getTypeStringValue(subType));
                    ContactModel.get(getApplicationContext()).updateContactSubscription(counterpartJid, Contact.SubscriptionType.FROM_PENDING);


                }else if (subType == Contact.SubscriptionType.PENDING_TO)
                {
                    //-->FROM_TO
                    Log.d(TAG,"PENDING_TO :The contact sub type is :" + contact.getTypeStringValue(subType));
                    ContactModel.get(getApplicationContext()).updateContactSubscription(counterpartJid, Contact.SubscriptionType.FROM_TO);



                }else if (subType == Contact.SubscriptionType.FROM_NONE)
                {
                    //-->FROM_NONE
                    Log.d(TAG,"FROM_NONE :The contact sub type is :" + contact.getTypeStringValue(subType));
                    Log.d(TAG,"Contact subscription already accepted :FROM_NONE");


                }else if (subType == Contact.SubscriptionType.FROM_PENDING)
                {
                    //-->FROM_PENDING
                    Log.d(TAG,"FROM_PENDING :The contact sub type is :" + contact.getTypeStringValue(subType));
                    Log.d(TAG,"Contact subscription already accepted :FROM_PENDING");



                }else if (subType == Contact.SubscriptionType.FROM_TO)
                {
                    //-->FROM_TO
                    Log.d(TAG,"FROM_TO :The contact sub type is :" + contact.getTypeStringValue(subType));
                    Log.d(TAG,"Contact subscription already accepted :FROM_TO");
                }
            }
        });

        snackBarActionDeny = (TextView) findViewById(R.id.snackbar_action_deny);
        snackBarActionDeny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //User denies presence subscription
                Log.d(TAG," Deny presence subscription from :" + counterpartJid);
                RoosterConnectionService.getRoosterConnection().unsubscribed(counterpartJid);

            }
        });

        KeyboardUtil.setKeyboardVisibilityListener(this,this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.contact_details)
        {

            Intent i = new Intent(this,ContactDetailsActivity.class);
            i.putExtra("EXTRA_COUNTERPART_JID",counterpartJid);
            startActivity(i);
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();


        /** Make the RecyclerView scroll down when the activity is fully loaded. **/
        scrollRecycerViewDown();



        //No need for such a receiver anymore, the view picks up the messages from the database.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action)
                {
                    case RoosterConnectionService.NEW_MESSAGE:
                        mAdapter.onMessageAdd();
                        return;
                }

            }
        };

        IntentFilter filter = new IntentFilter(RoosterConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver,filter);

    }

    private void scrollRecycerViewDown() {
        ChatMessagesAdapter adapter = (ChatMessagesAdapter)  mRecyclerView.getAdapter();
        adapter.notifyViewToScrollDown();
    }


    /** KeyboardVisibilityListener Methods */
    @Override
    public void onKeyboardVisibilityChanged(boolean keyboardVisible) {
//        if(keyboardVisible)
//            Log.d("RecyclerView","Keyboard visible");
//        else
//            Log.d("RecyclerView","Keyboard not visible");
        scrollRecycerViewDown();
    }

    /** OnItemClickListener Methods */
    @Override
    public void onItemClick(ChatMessagesAdapter.ChatMessageViewHolder item, int position) {
        Toast.makeText(this, item.getMessageTimestamp(), Toast.LENGTH_SHORT).show();
    }

    /** OnInformRecyclerViewToScrollDownListener Methods */
    @Override
    public void onInformRecyclerViewToScrollDown(int size) {
        mRecyclerView.scrollToPosition(size-1);
    }
}
