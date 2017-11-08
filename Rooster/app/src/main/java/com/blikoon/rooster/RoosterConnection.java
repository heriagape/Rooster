package com.blikoon.rooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.ChatMessageModel;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Updated by gakwaya on Oct/08/2017.
 */
public class RoosterConnection implements ConnectionListener ,PingFailedListener {

    private static final String TAG = "RoosterConnection";

    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private  final String mServiceName;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.

    private PingManager pingManager;


    /** Due to connection timeout enforced by xmpp servers, if the client is idle for X seconds[configurable on server],
     * the server closes the connection and you get a closed on Error exception. Smack usually reconnects after that but a given
     * interval of time it fails to reconnect. We introduce a periodic ping function to ping the server, just to trick
     * it into thinking that the client is not idle. This keeps the client alive. But at the expense of more battery to
     * keep the connection to the server.
     *
     * Another threat our app have is the doze mode of android, that kills the app when it is not used for some time.
     * We have no way around that, the only way that is GCM independent is to let the user exempt the app from battery
     * optimizations.*/
    private Timer pingTimer;
    private TimerTask pingTimerTask = new TimerTask() {

        @Override
        public void run() {
            try {
                if(pingManager.pingMyServer())
                {
                    Log.d(TAG,"Server still accessible");
                }else {
                    Log.d(TAG,"Server NOT Accessible");
                }
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };

    @Override
    public void pingFailed() {
        Log.d(TAG,"Ping Failed Method called.");
    }


    public static enum ConnectionState
    {
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public static enum LoggedInState
    {
        LOGGED_IN , LOGGED_OUT;
    }


    public RoosterConnection( Context context)
    {
        Log.d(TAG,"RoosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid",null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password",null);

        if( jid != null)
        {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        }else
        {
            mUsername ="";
            mServiceName="";
        }
    }


    public void connect() throws IOException,XMPPException,SmackException
    {
        Log.d(TAG, "Connecting to server " + mServiceName);

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(mServiceName)
                .setHost("salama.im")
                .setResource("Rooster")


                //Was facing this issue
                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
                .setKeystoreType(null) //This line seems to get rid of the problem

                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setCompressionEnabled(true).build();

        Log.d(TAG, "Username : "+mUsername);
        Log.d(TAG, "Password : "+mPassword);
        Log.d(TAG, "Server : "+mServiceName);


        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();

        //Enable debugging for connections.
        SmackConfiguration.DEBUG = true;

        mConnection = new XMPPTCPConnection(conf);

        mConnection.addConnectionListener(this);

//        PingManager.setDefaultPingInterval(10); //Ping every 2 minutes
        pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.registerPingFailedListener(this);

        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername,mPassword);
            Log.d(TAG, " login() Called ");

            //Start the ping repeat timer here.
            startPinging();




        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
                ///ADDED
                Log.d(TAG,"message.getBody() :"+message.getBody());
                Log.d(TAG,"message.getFrom() :"+message.getFrom());

                String from = message.getFrom().toString();

                String contactJid="";
                if ( from.contains("/"))
                {
                    contactJid = from.split("/")[0];
                    Log.d(TAG,"The real jid is :" +contactJid);
                    Log.d(TAG,"The message is from :" +from);
                }else
                {
                    contactJid=from;
                }

                //Add the Received message to the database

                ///ADDED

                if(ChatMessageModel.get(mApplicationContext,contactJid).addMessage(
                        new ChatMessage(message.getBody(),System.currentTimeMillis(),
                                ChatMessage.Type.RECEIVED,contactJid)))
                {
                    //Bundle up the intent and send the broadcast.
                Intent intent = new Intent(RoosterConnectionService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(RoosterConnectionService.BUNDLE_FROM_JID,contactJid);
                intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,message.getBody());
                mApplicationContext.sendBroadcast(intent);
                Log.d(TAG,"Received message from :"+contactJid+" broadcast sent.");

                }

            }
        });


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }



    public void startPinging() {
        Log.d(TAG,"Pinging process started...");
        if(pingTimer != null) {
            return;
        }
        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(pingTimerTask, 0, 1*60*1000);//Ping every one minute
    }

    public void stopPinging() {
        pingTimer.cancel();
        pingTimer = null;
    }




    private void setupUiThreadBroadCastMessageReceiver()
    {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if( action.equals(RoosterConnectionService.SEND_MESSAGE))
                {
                    //Send the message.
                    sendMessage(intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(RoosterConnectionService.BUNDLE_TO));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(RoosterConnectionService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);

    }

    private void sendMessage ( String body ,String toJid)
    {
        Log.d(TAG,"Sending message to :"+ toJid);

        EntityBareJid jid = null;


        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);

        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(body);
            chat.send(message);

        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    public void disconnect()
    {
        Log.d(TAG,"Disconnecting from serser "+ mServiceName);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit().putBoolean("xmpp_logged_in",false).commit();

        //Stop pinging the server
        stopPinging();


        if (mConnection != null)
        {
            mConnection.disconnect();
        }

        mConnection = null;
        // Unregister the message broadcast receiver.
        if( uiThreadMessageReceiver != null)
        {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        RoosterConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Connected Successfully");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        RoosterConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Authenticated Successfully");
        showContactListActivityWhenAuthenticated();
    }


    @Override
    public void connectionClosed() {
        RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"ConnectionClosedOnError, error "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTING;
        Log.d(TAG,"ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG,"ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        RoosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG,"ReconnectionFailed()");

    }

    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(RoosterConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG,"Sent the broadcast that we are authenticated");
    }
}
