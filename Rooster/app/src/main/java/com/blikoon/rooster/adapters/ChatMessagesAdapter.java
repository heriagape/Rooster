package com.blikoon.rooster.adapters;

/**
 * Created by gakwaya on 2017/11/1.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.rooster.R;
import com.blikoon.rooster.RoosterConnection;
import com.blikoon.rooster.RoosterConnectionService;
import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.ChatMessageModel;

import java.util.List;



public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ChatMessageViewHolder>
implements ChatMessageModel.OnMessageAddListener{
    private static final String TAG = "ChatMessageAdapter";


    /* Interface implementation from ChatMessageModel when to inform the adapter when a new message is added */
    @Override
    public void onMessageAdd() {
        mChatMessageList = ChatMessageModel.get(mContext,contactJid).getMessages();
        notifyDataSetChanged();

        //Notify the recyclerview so it knows when to scroll down.
        mOnInformRecyclerViewToScrollDownListener.onInformRecyclerViewToScrollDown(mChatMessageList.size());
    }

    /*
        * Click handler interface. RecyclerView does not have
        * its own built in like AdapterViews do.
        */
    public interface OnItemClickListener {
        public void onItemClick(ChatMessageViewHolder item, int position);
    }

    /* Interface to let the view recycler view know that an item has been added so it
      * can scroll down. */
    public interface OnInformRecyclerViewToScrollDownListener {
        public void onInformRecyclerViewToScrollDown( int size);
    }


    private static final int SENT = 1;
    private static final int RECEIVED = 2;



    private List<ChatMessage> mChatMessageList;
    private OnItemClickListener mOnItemClickListener;

    private OnInformRecyclerViewToScrollDownListener mOnInformRecyclerViewToScrollDownListener;
    private LayoutInflater mLayoutInflater;
    private String contactJid;
    private Context mContext;

    public ChatMessagesAdapter(Context context, String contactJid) {
        mLayoutInflater = LayoutInflater.from(context);

        mContext = context;

        this.contactJid = contactJid;

        mChatMessageList = ChatMessageModel.get(mContext,contactJid).getMessages();
        ChatMessageModel.get(mContext,contactJid).setMessageAddListener(this);


    }

    public void notifyViewToScrollDown()
    {
        //Notify the recyclerview so it knows when to scroll down.
        mOnInformRecyclerViewToScrollDownListener.onInformRecyclerViewToScrollDown(mChatMessageList.size());

    }

    @Override
    public ChatMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;
        switch (viewType) {
            case SENT:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_view_sent, parent, false);
                return new ChatMessageViewHolder(itemView,this);
            case RECEIVED:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_view_received, parent, false);
                return new ChatMessageViewHolder(itemView,this);
            default:
                itemView = mLayoutInflater
                        .inflate(R.layout.chat_message_view_received, parent, false);
                return new ChatMessageViewHolder(itemView,this);
        }
    }

    @Override
    public void onBindViewHolder(ChatMessageViewHolder holder, int position) {
        holder.setMessageBody(mChatMessageList.get(position).getMessage());
        holder.setMessageTimestamp(mChatMessageList.get(position).getFormattedTime() );

        String jid = mChatMessageList.get(position).getContactJid();
        ChatMessage.Type type = mChatMessageList.get(position).getType();


        if( type == ChatMessage.Type.RECEIVED)
        {
            RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
            if(rc != null)
            {
                String imageAbsPath = rc.getProfileImageAbsolutePath(jid);
                if ( imageAbsPath != null)
                {
                    Drawable d = Drawable.createFromPath(imageAbsPath);
                    holder.setProfileImageDrawable(d);
                }

            }
        }

        if( type == ChatMessage.Type.SENT)
        {
            RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
            if(rc != null)
            {
                String selfJid = PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString("xmpp_jid",null);

                if ( selfJid != null)
                {
                    Log.d(TAG,"God a valid self jid : "+ selfJid);
                    String imageAbsPath = rc.getProfileImageAbsolutePath(selfJid);
                    if ( imageAbsPath != null)
                    {
                        Drawable d = Drawable.createFromPath(imageAbsPath);
                        holder.setProfileImageDrawable(d);
                    }
                }else
                {
                    Log.d(TAG,"Could not get a valid self jid ");
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mChatMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage.Type messageType = mChatMessageList.get(position).getType();

        if(messageType == ChatMessage.Type.SENT)
        {
            return SENT;
        }
        if(messageType == ChatMessage.Type.RECEIVED)
        {
            return RECEIVED;
        }
        return SENT;

    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }


    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }



    public OnInformRecyclerViewToScrollDownListener getmOnInformRecyclerViewToScrollDownListener() {
        return mOnInformRecyclerViewToScrollDownListener;
    }

    public void setmOnInformRecyclerViewToScrollDownListener(OnInformRecyclerViewToScrollDownListener mOnInformRecyclerViewToScrollDownListener) {
        this.mOnInformRecyclerViewToScrollDownListener = mOnInformRecyclerViewToScrollDownListener;
    }



    /* Required implementation of ViewHolder to wrap item view */
    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {
        private ChatMessagesAdapter mParent;
        private TextView mMessageBody, mMessageTimestamp;
        private ImageView profileImage;

        public ChatMessageViewHolder(View itemView, ChatMessagesAdapter parent) {
            super(itemView);
            itemView.setOnClickListener(this);
            mParent = parent;
            mMessageBody = (TextView) itemView.findViewById(R.id.text_message_body);
            mMessageTimestamp = (TextView) itemView.findViewById(R.id.text_message_timestamp);
            profileImage = (ImageView) itemView.findViewById(R.id.profile);
        }

        public void setMessageBody(CharSequence title) {
            mMessageBody.setText(title);
        }

        public void setProfileImageDrawable(Drawable d)
        {
            profileImage.setImageDrawable(d);
        }

        public void setMessageTimestamp(CharSequence summary) {
            mMessageTimestamp.setText(summary);
        }

        public CharSequence getMessageTimestamp() {
            return mMessageTimestamp.getText();
        }

        @Override
        public void onClick(View v) {
            final OnItemClickListener listener = mParent.getOnItemClickListener();


            if (listener != null) {
                listener.onItemClick(this, getPosition());
            }
        }



    }
}
