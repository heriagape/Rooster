package com.blikoon.rooster;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.rooster.model.Chats;
import com.blikoon.rooster.model.ChatsModel;
import com.blikoon.rooster.model.Contact;
import com.blikoon.rooster.model.ContactModel;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";

    private RecyclerView chatsRecyclerView;
    private ChatListAdapter mAdapter;
    private FloatingActionButton newConversationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatsRecyclerView = (RecyclerView) findViewById(R.id.chat_list_recycler_view);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        //ContactModel model = ContactModel.get(getBaseContext());
        //List<Contact> contacts = model.getContacts();

        mAdapter = new ChatListAdapter(this);
        chatsRecyclerView.setAdapter(mAdapter);

        newConversationButton = (FloatingActionButton) findViewById(R.id.new_conversation_floating_button);
        newConversationButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        newConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                addContact();
                    // Inside here we start the chat ContactListActivity
                    Intent intent = new Intent(ChatListActivity.this
                            ,ContactListActivity.class);
                    //intent.putExtra("EXTRA_CONTACT_JID",mChat.getJid());
                    startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_list, menu);
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

//            //Crash the app. This is for just testing ACRA reporting
//            String crashString = null;
//            crashString.length();


        }else if(item.getItemId() == R.id.rooster_add_contact)
        {
           RoosterConnectionService.getRoosterConnection().getRosterEntries();
//            addContact();
//            ContactModel.get(this).updateContactSubscription("musimbate@salama.im", Contact.SubscriptionType.NONE_NONE);
        }else if(item.getItemId() == R.id.rooster_presence_subscribe)
        {
            subscribe();
            //ContactModel.get(this).updateContactSubscription("gakwaya@salama.im", Contact.SubscriptionType.TO_PENDING);
        }else if(item.getItemId() == R.id.rooster_presence_unsubscribe)
        {
            unsubscribe();
            //ContactModel.get(this).updateContactSubscription("gakwaya@salama.im", Contact.SubscriptionType.TO);
        }else if(item.getItemId() == R.id.rooster_presence_subscribed)
        {
            subscribed();
//            ContactModel.get(this).updateContactSubscription("gakwaya@salama.im", Contact.SubscriptionType.FROM_PENDING);
        }else if(item.getItemId() == R.id.rooster_presence_unsubscribed)
        {
            unsubscribed();
//            ContactModel.get(this).updateContactSubscription("gakwaya@salama.im", Contact.SubscriptionType.FROM);
        }else if(item.getItemId() == R.id.rooster_send_message)
        {
            sendMessage();
//            ContactModel.get(this).updateContactSubscription("gakwaya@salama.im", Contact.SubscriptionType.BOTH);
        }else if( item.getItemId() == R.id.rooster_me)
        {
            //Start the MeActivity
            Intent i = new Intent(this,MeActivity.class);
            startActivity(i);
        }


        return super.onOptionsItemSelected(item);
    }

    private void sendMessage() {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from("musimbate@salama.im");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Message messageTo = new Message(jidTo,"Hello bro");

        //Send them a message
        RoosterConnectionService.getRoosterConnection().sendMessage(messageTo);
    }

    private void unsubscribed() {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from("musimbate@salama.im");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.unsubscribed);
        RoosterConnectionService.getRoosterConnection().sendPresense(subscribe);

    }

    private void subscribed() {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from("musimbate@salama.im");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.subscribed);
        RoosterConnectionService.getRoosterConnection().sendPresense(subscribe);

    }

    private void unsubscribe() {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from("musimbate@salama.im");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.unsubscribe);
        RoosterConnectionService.getRoosterConnection().sendPresense(subscribe);

    }

    private void subscribe() {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from("musimbate@salama.im");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.subscribe);
        RoosterConnectionService.getRoosterConnection().sendPresense(subscribe);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mAdapter.notifyForUiUpdate();
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

    private class ChatHolder extends RecyclerView.ViewHolder
    {
        private TextView contactTextView;
        private TextView messageAbstractTextView;
        private TextView timestampTextView;
        private ImageView profileImage;
        private Chats mChat;
        public ChatHolder(View itemView)
        {
            super(itemView);

            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);
            messageAbstractTextView = (TextView) itemView.findViewById(R.id.message_abstract);
            timestampTextView = (TextView) itemView.findViewById(R.id.text_message_timestamp);
            profileImage = (ImageView) itemView.findViewById(R.id.profile);

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

            RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
//            if( rc != null)
//            {
//                byte[] profile_image_byte_array = rc.getUserAvatar(mChat.getJid());
//                if( profile_image_byte_array  != null)
//                {
//                    // If there is a profile image in the avatar, set it to the view
//                    Drawable image = new BitmapDrawable(getResources(),
//                            BitmapFactory.decodeByteArray(profile_image_byte_array, 0, profile_image_byte_array.length));
//                    profileImage.setImageDrawable(image);
//                }
//            }

            if(rc != null)
            {
                String imageAbsPath = rc.getProfileImageAbsolutePath(mChat.getJid());
                if ( imageAbsPath != null)
                {
                    Drawable d = Drawable.createFromPath(imageAbsPath);
                    profileImage.setImageDrawable(d);
                }

            }





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
