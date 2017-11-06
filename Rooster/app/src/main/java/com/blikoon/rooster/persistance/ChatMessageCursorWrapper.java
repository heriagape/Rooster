package com.blikoon.rooster.persistance;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

import com.blikoon.rooster.model.ChatMessage;


/**
 * Created by gakwaya on 2017/11/2.
 */

public class ChatMessageCursorWrapper extends CursorWrapper {

    public ChatMessageCursorWrapper(Cursor cursor)
    {
        super(cursor);
    }

    public ChatMessage getChatMessage()
    {

        String message = getString(getColumnIndex(ChatMessage.Cols.message));
        long timestamp = getLong(getColumnIndex(ChatMessage.Cols.timestamp));
        String messageType = getString(getColumnIndex(ChatMessage.Cols.messageType));
        String counterpartJid = getString(getColumnIndex(ChatMessage.Cols.contactJid));

        ChatMessage.Type chatMessageType = null;

        if( messageType.equals("SENT"))
        {
            chatMessageType = ChatMessage.Type.SENT;
        }

        else if(messageType.equals("RECEIVED"))
        {
            chatMessageType = ChatMessage.Type.RECEIVED;
        }
        ChatMessage chatMessage = new ChatMessage(message,timestamp,chatMessageType,counterpartJid);

        return  chatMessage;
    }

}