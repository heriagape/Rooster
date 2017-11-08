package com.blikoon.rooster;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.blikoon.rooster.model.Chats;
import com.blikoon.rooster.model.ChatsModel;

import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";

    private RecyclerView contactsRecyclerView;
    private ChatListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        //ContactModel model = ContactModel.get(getBaseContext());
        //List<Contact> contacts = model.getContacts();

        mAdapter = new ChatListAdapter(this);
        contactsRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.rooster_logout)
        {
            //Disconnect from server
            Log.d(TAG,"Initiating the log out process");
            Intent i1 = new Intent(this,RoosterConnectionService.class);
            stopService(i1);

            //Finish this activity
            finish();

            //Start login activity for user to login
            Intent loginIntent = new Intent(this,LoginActivity.class);
            startActivity(loginIntent);

        }else if(item.getItemId() == R.id.rooster_add_contact)
        {
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
                    if(ChatsModel.get(getApplicationContext()).addChat(new Chats(inputText, Chats.ContactType.ONE_ON_ONE)))
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

        return super.onOptionsItemSelected(item);
    }

    private class ChatHolder extends RecyclerView.ViewHolder
    {
        private TextView contactTextView;
        private TextView messageAbstractTextView;
        private TextView timestampTextView;
        private Chats mChat;
        public ChatHolder(View itemView)
        {
            super(itemView);

            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);
            messageAbstractTextView = (TextView) itemView.findViewById(R.id.message_abstract);
            timestampTextView = (TextView) itemView.findViewById(R.id.text_message_timestamp);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Inside here we start the chat activity
                    Intent intent = new Intent(ChatListActivity.this
                            ,ChatActivity.class);
                    intent.putExtra("EXTRA_CONTACT_JID",mChat.getJid());
                    startActivity(intent);


                }
            });
        }


        public void bindChat(Chats chat)
        {
           mChat  = chat;
            if (mChat == null)
            {
                Log.d(TAG,"Trying to work on a null Chats object ,returning.");
                return;
            }
            contactTextView.setText(mChat.getJid());
            messageAbstractTextView.setText("This is the message we exchanged the last time we talked.");
            timestampTextView.setText("12:10 AM");

        }
    }


    private class ChatListAdapter extends RecyclerView.Adapter<ChatHolder>
    {
        private List<Chats> mChats;
        private Context mContext;

        public ChatListAdapter(Context context)
        {
            mChats = ChatsModel.get(context).getChats();
            mContext = context;
            Log.d(TAG,"Constructor of ChatListAdapter , the size of the backing list is :" +mChats.size());
        }

        public void notifyForUiUpdate()
        {
            mChats = ChatsModel.get(mContext).getChats();
            notifyDataSetChanged();

        }

        @Override
        public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater
                    .inflate(R.layout.list_item_chat, parent,
                            false);
            return new ChatHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatHolder holder, int position) {
            Chats chat = mChats.get(position);
            holder.bindChat(chat);

        }

        @Override
        public int getItemCount() {
            return mChats.size();
        }
    }
}
