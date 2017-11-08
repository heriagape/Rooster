package com.blikoon.rooster.persistance;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.Chats;

/**
 * Created by gakwaya on 2017/11/8.
 */

public class ChatsCursorWrapper extends CursorWrapper {

    public ChatsCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Chats getChat() {

        String contactType = getString(getColumnIndex(Chats.Cols.contactType));
        String jid = getString(getColumnIndex(Chats.Cols.jid));


        Chats.ContactType chatType = null;

        if (contactType.equals("GROUP")) {
            chatType = Chats.ContactType.GROUP;
        } else if (contactType.equals("ONE_ON_ONE")) {
            chatType = Chats.ContactType.ONE_ON_ONE;
        }
//        ChatMessage chatMessage = new ChatMessage(message,timestamp,chatMessageType,counterpartJid);

        Chats chat = new Chats(jid, chatType);
        return chat;
    }

}

