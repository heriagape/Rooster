package com.blikoon.rooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


import com.blikoon.rooster.adapters.ChatMessagesAdapter;
import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.ChatMessageModel;
import com.blikoon.rooster.ui.InsetDecoration;
import com.blikoon.rooster.ui.KeyboardUtil;

//import co.intentservice.chatui.ChatView;
//import co.intentservice.chatui.models.ChatMessage;


public class ChatActivity extends ActionBarActivity implements
        ChatMessagesAdapter.OnItemClickListener,ChatMessagesAdapter.OnInformRecyclerViewToScrollDownListener,
        KeyboardUtil.KeyboardVisibilityListener {

    private static final String TAG ="ChatActivity";

    private String contactJid;
//    private ChatView mChatView;
    private BroadcastReceiver mBroadcastReceiver;


    private RecyclerView mRecyclerView;
    private ChatMessagesAdapter mAdapter;
    private LinearLayoutManager mVerticalManager;
    private EditText textInputTextEdit;
    private ImageButton textSendButton;
    private String counterpartJid;







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

        KeyboardUtil.setKeyboardVisibilityListener(this,this);


//        mChatView =(ChatView) findViewById(R.id.rooster_chat_view);
//
//        mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener(){
//            @Override
//            public boolean sendMessage(ChatMessage chatMessage){
//                // perform actual message sending
//                if (RoosterConnectionService.getState().equals(RoosterConnection.ConnectionState.CONNECTED)) {
//                    Log.d(TAG, "The client is connected to the server,Sending Message");
//                    //Send the message to the server
//
//                    Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
//                    intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,
//                            mChatView.getTypedMessage());
//                    intent.putExtra(RoosterConnectionService.BUNDLE_TO, contactJid);
//
//                    sendBroadcast(intent);
//
//                } else {
//                    Toast.makeText(getApplicationContext(),
//                            "Client not connected to server ,Message not sent!",
//                            Toast.LENGTH_LONG).show();
//                }
//                //message sending ends here
//                return true;
//            }
//        });















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
//                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
//                        String body = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);
//
//                        if ( from.equals(contactJid))
//                        {
////                            ChatMessage chatMessage = new ChatMessage(body,System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
////                            mChatView.addMessage(chatMessage);
//
//                        }else
//                        {
//                            Log.d(TAG,"Got a message from jid :"+from);
//                        }

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
        Toast.makeText(this, item.getSummary(), Toast.LENGTH_SHORT).show();
    }

    /** OnInformRecyclerViewToScrollDownListener Methods */
    @Override
    public void onInformRecyclerViewToScrollDown(int size) {
        mRecyclerView.scrollToPosition(size-1);
    }
}
