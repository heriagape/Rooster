package com.blikoon.rooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.ChatMessageModel;
import com.blikoon.rooster.model.Chats;
import com.blikoon.rooster.model.ChatsModel;
import com.blikoon.rooster.model.Contact;
import com.blikoon.rooster.model.ContactModel;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.roster.PresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.minidns.MiniDnsResolver;
import org.jivesoftware.smackx.debugger.android.AndroidDebugger;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;

import de.duenndns.ssl.MemorizingTrustManager;


/**
 * Updated by gakwaya on Oct/08/2017.
 */
public class RoosterConnection implements ConnectionListener ,PingFailedListener,RosterListener,SubscribeListener,PresenceEventListener,PresenceListener{

    private static final String TAG = "RoosterConnection";

    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private  final String mServiceName;

    public XMPPTCPConnection getmConnection() {
        return mConnection;
    }

    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.
    private PingManager pingManager;
    private VCardManager vCardManager;

    private final static int CONNECT_TIMEOUT = 1000 * 60;

    private ProxyInfo mProxyInfo = null;

    private SSLContext sslContext;

    private final static String SSLCONTEXT_TYPE = "TLS";

    private SecureRandom secureRandom;

    private MemorizingTrustManager mMemTrust;

    Roster mRoster;





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


        boolean doDnsSrv = true;
        String domain = mServiceName;
        String server = null;
        int serverPort =0;

        // If user did not specify a server, and SRV requested then lookup SRV
        if (doDnsSrv) {

            //java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            //java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");



            /** This is causing the crash reported here :
             * https://discourse.igniterealtime.org/t/ava-lang-illegalstateexception-no-dns-resolver-active-in-smack-at-org-jivesoftware-smack-util-dnsutil-resolvedomain-dnsutil-java-169/79697/1
             * COME BACK WHEN YOU HAVE A WORK AROUND.
             * */
            MiniDnsResolver.setup();/** Suggested by Flow as a workaround.*/
            Log.d(TAG, "(DNS SRV) resolving: " + domain);
            List<HostAddress> listHostsFailed = new ArrayList<>();
            List<HostAddress> listHosts = DNSUtil.resolveXMPPServiceDomain(domain, listHostsFailed, ConnectionConfiguration.DnssecMode.disabled);

            if (listHosts.size() > 0) {
                server = listHosts.get(0).getFQDN();
                serverPort = listHosts.get(0).getPort();

                Log.d(TAG, "(DNS SRV) resolved: " + domain + "=" + server + ":" + serverPort);
            }
        }

        if (serverPort == 0) //if serverPort is set to 0 then use 5222 as default
            serverPort = 5222;

        XMPPTCPConnectionConfiguration.Builder mConfig = XMPPTCPConnectionConfiguration.builder();

        mConfig.setServiceName(JidCreate.domainBareFrom(domain));
        mConfig.setPort(serverPort);

        mConfig.setCompressionEnabled(true);
        mConfig.setConnectTimeout(CONNECT_TIMEOUT);

        mProxyInfo = null;

        if (server == null)
            mConfig.setHost(domain);
        else {
            mConfig.setHost(server);

            try {


                String[] addressParts = server.split("\\.");
                if (Integer.parseInt(addressParts[0]) != -1) {
                    byte[] parts = new byte[addressParts.length];
                    for (int i = 0; i < 4; i++)
                        parts[i] = (byte)Integer.parseInt(addressParts[i]);

                    byte[] ipAddr = new byte[]{parts[0],parts[1],parts[2],parts[3]};
                    InetAddress addr = InetAddress.getByAddress(ipAddr);
                    mConfig.setHostAddress(addr);

                }
                else
                {
                    mConfig.setHostAddress(InetAddress.getByName(server));
                }
            }
            catch (Exception e){
                Log.d(TAG,"error parsing server as IP address; using as hostname instead");
                mConfig.setHostAddress(InetAddress.getByName(server));

            }

            mConfig.setHostAddress(InetAddress.getByName(server));
            mConfig.setXmppDomain(domain);
        }

        mConfig.setProxyInfo(mProxyInfo);

        mConfig.setDebuggerEnabled(true);
        SmackConfiguration.DEBUG = true;
        SmackConfiguration.setDebuggerFactory(new SmackDebuggerFactory() {
            @Override
            public SmackDebugger create(XMPPConnection xmppConnection, Writer writer, Reader reader) throws IllegalArgumentException {

                return new AndroidDebugger(xmppConnection, writer, reader);

            }
        });


        // Android has no support for Kerberos or GSSAPI, so disable completely
        SASLAuthentication.unregisterSASLMechanism("KERBEROS_V4");
        SASLAuthentication.unregisterSASLMechanism("GSSAPI");

//        if (allowPlainAuth)
            SASLAuthentication.unBlacklistSASLMechanism("PLAIN");

        SASLAuthentication.unBlacklistSASLMechanism("DIGEST-MD5");

        if (mMemTrust == null)
            mMemTrust = new MemorizingTrustManager(mApplicationContext);

        if (sslContext == null) {

            try {
                sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            secureRandom = new java.security.SecureRandom();
            try {
                sslContext.init(null, MemorizingTrustManager.getInstanceList(mApplicationContext), secureRandom);
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            while (true) {
                try {

                    if (Build.VERSION.SDK_INT >= 20) {

                        sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES_API_20);

                    } else {
                        sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
                    }
                    break;
                } catch (IllegalStateException e) {
                    Log.d(TAG,"error setting cipher suites; waiting for SSLContext to init...");
                    try { Thread.sleep(1000); } catch (Exception e2){}
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mConfig.setKeystoreType("AndroidCAStore");
                mConfig.setKeystorePath(null);
            } else {
                mConfig.setKeystoreType("BKS");
                String path = System.getProperty("javax.net.ssl.trustStore");
                if (path == null)
                    path = System.getProperty("java.home") + File.separator + "etc"
                            + File.separator + "security" + File.separator
                            + "cacerts.bks";
                mConfig.setKeystorePath(path);

            }

            //wait a second while the ssl context init's
            try { Thread.sleep(1000); } catch (Exception e) {}

        }

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= 16){
            // Enable TLS1.2 and TLS1.1 on supported versions of android
            // http://stackoverflow.com/questions/16531807/android-client-server-on-tls-v1-2

            while (true) {
                try {
                    mConfig.setEnabledSSLProtocols(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
                    sslContext.getDefaultSSLParameters().setProtocols(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
                    break;
                } catch (IllegalStateException ise) {
                    try { Thread.sleep(1000); } catch (Exception e){}
                }
            }

        }

        if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            mConfig.setEnabledSSLCiphers(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
        }

        mConfig.setCustomSSLContext(sslContext);
        mConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        mConfig.setHostnameVerifier(
                mMemTrust.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));


        mConfig.setSendPresence(true);

        XMPPTCPConnection.setUseStreamManagementDefault(true);



        ///KEEP FOR LATER
//        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
//                .setXmppDomain(mServiceName)
//                .setHost("salama.im")
//                .setResource("Rooster")
//                .setDebuggerEnabled(true)
//
//
//                //Was facing this issue
//                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
//                .setKeystoreType(null) //This line seems to get rid of the problem
//
//                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
//                .setCompressionEnabled(true).build();
//
//        Log.d(TAG, "Username : "+mUsername);
//        Log.d(TAG, "Password : "+mPassword);
//        Log.d(TAG, "Server : "+mServiceName);
        ///KEEP FOR LATER


        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(mConfig.build());

        vCardManager  = VCardManager.getInstanceFor(mConnection);




        mConnection.addConnectionListener(this);

        pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.registerPingFailedListener(this);

        mRoster = Roster.getInstanceFor(mConnection);
        mRoster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        mRoster.addRosterListener(this);
        mRoster.addSubscribeListener(this);



        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername,mPassword);
            Log.d(TAG, " login() Called ");


            updateRosterDataFromServer();

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
                    /** Add the chat to chatListModel if the this jid is not in there already */
                    List<Chats> chats = ChatsModel.get(mApplicationContext).getChatsByJid(contactJid);
                    if( chats.size() == 0)
                    {
                        Log.d(TAG, contactJid + " is a new chat, adding them.");
                        if(ChatsModel.get(mApplicationContext).addChat(new Chats(contactJid, Chats.ContactType.ONE_ON_ONE)))
                        {
                            Log.d(TAG,contactJid + "added successfully to ChatModel");
                        }else
                        {
                            Log.d(TAG,"Could not ADD " + contactJid + "to chatModel");
                        }

                    }

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



    public byte [] getSelfAvatar()
    {
        VCard vCard =  null;

        if(vCardManager != null)
        {
            try {
                vCard = vCardManager.loadVCard();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        if( vCard != null)
        {
            return vCard.getAvatar();
        }
        return null;

    }

    /** Sets the avatar for the currently connected user*/
    public boolean setSelfAvatar( byte[] image)
    {
        //Get the avatar for display
        VCard vCard = new VCard();
        vCard.setAvatar(image);

        if( vCardManager != null)
        {
            try {
                vCardManager.saveVCard(vCard);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
                return false;
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
                return false;
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            return  true;
        }else
        {
            return false;
        }
    }


    public byte[] getUserAvatar ( String user)
    {
        EntityBareJid jid = null;
        try {
            jid =JidCreate.entityBareFrom(user);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        VCard vCard =  null;

        if(vCardManager != null)
        {
            try {
                vCard = vCardManager.loadVCard(jid);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        if( vCard != null)
        {
            return vCard.getAvatar();
        }
        return null;

    }


    public Collection<RosterEntry> getRosterEntries()
    {
        Collection<RosterEntry> entries = mRoster.getEntries();

        return  entries;
    }


    public void sendPresense(Presence presence)
    {
        if(mConnection != null)
        {
            try {
                mConnection.sendStanza(presence);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage( Message message)
    {
        if(mConnection != null)
        {
            try {
                mConnection.sendStanza(message);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void subscribed(String contact)
    {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.subscribed);
        sendPresense(subscribe);

    }

    public void unsubscribed(String contact)
    {
        Jid jidTo = null;
        try {
            jidTo = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Presence subscribe = new Presence(jidTo, Presence.Type.unsubscribed);
        sendPresense(subscribe);

    }

    public boolean sendSubscriptionRequest(String contact)
    {
        //TODO : This call is failing when client is not connected. Find  out why and find a way to buffer messages sent when not connected.
        Jid jid = null;
        try {
             jid = JidCreate.from(contact);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return false;
        }

        try {
            mRoster.sendSubscriptionRequest(jid.asBareJid());
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /** Retrieves roster contacts from the server and syncs with the contact list saved in the db */
    public void updateRosterDataFromServer()
    {
        //Get roster form server
        Collection<RosterEntry> entries = RoosterConnectionService.getRoosterConnection().getRosterEntries();

        Log.d(TAG,"Retrieving roster entries from server. "+entries.size() + " contacts in his roster");

        for (RosterEntry entry : entries) {
            RosterPacket.ItemType itemType=   entry.getType();
            String stringItemType = getRosterItemTypeString(itemType);

            Log.d(TAG,"-------------------------------");
            Log.d(TAG,"Roster entry for  :"+ entry.toString());
            Log.d(TAG,"Subscription type is :" + stringItemType);
            Log.d(TAG,"getJid.toString() :" + entry.getJid().toString());

            //Update data in the db
            //Get all the contacts
            List<String> contacts = ContactModel.get(mApplicationContext).getContactJidStrings();



            if( !contacts.contains(entry.getJid().toString()))
            {
                //Add it to the db
                if(ContactModel.get(mApplicationContext).addContact(new Contact(entry.getJid().toString(),
                        rosterItemTypeToContactSubscriptionType(itemType))))
                {
                    Log.d(TAG,"Contact "+entry.getJid().toString() +"Added successfuly");
                    //mAdapter.notifyForUiUpdate();
                }else
                {
                    Log.d(TAG,"Could not add Contact "+entry.getJid().toString());
                }
            }

        }

        //For each entry

            //If not in db already

                //Add it

    }

    private String getRosterItemTypeString(RosterPacket.ItemType itemType)
    {
        if(itemType == RosterPacket.ItemType.none)
        {
            return "NONE";
        }
        else if(itemType == RosterPacket.ItemType.from)
        {
            return "FROM";
        }
        else if(itemType == RosterPacket.ItemType.to)
        {
            return "TO";
        }
        else if(itemType == RosterPacket.ItemType.both)
        {
            return  "BOTH";
        }else
        {
            return "UNKNOWN";
        }
    }

    private Contact.SubscriptionType rosterItemTypeToContactSubscriptionType(RosterPacket.ItemType itemType)
    {
        if(itemType == RosterPacket.ItemType.none)
        {
            return Contact.SubscriptionType.NONE_NONE;
        }
        else if(itemType == RosterPacket.ItemType.from)
        {
            return Contact.SubscriptionType.FROM_NONE;
        }
        else if(itemType == RosterPacket.ItemType.to)
        {
            return Contact.SubscriptionType.NONE_TO;
        }
        else
        {
            return Contact.SubscriptionType.FROM_TO;
        }

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
        if(pingTimer !=null)
        {
            pingTimer.cancel();
            pingTimer = null;
        }

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

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    /** ConnectionListener Overrides*/
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
        Log.d(TAG,"ReconnectingIn() "+ seconds +" seconds");

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

    /** PingFailedListener Overrides.
     * */

    @Override
    public void pingFailed() {
        Log.d(TAG,"Ping Failed Method called.");
    }

    /** RosterListener Overrides
     * You should find a way to update interested UI components of the
     * changes in these overrides */
    @Override
    public void entriesAdded(Collection<Jid> addresses) {
        Log.d(TAG,"Entries added to the Roster : " +addresses);

    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses) {
        Log.d(TAG,"Entries updated in the Roster : " +addresses);

    }

    @Override
    public void entriesDeleted(Collection<Jid> addresses) {
        Log.d(TAG,"Entries Deleted in the Roster : " +addresses);

    }

    @Override
    public void presenceChanged(Presence presence) {
        Log.d(TAG,"Presence changed in the Roster : " +presence);

    }

    /** SubscribeListener Overrides */
    @Override
    public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
        Log.d(TAG,"--------------------processSubscribe Called---------------------.");
        Log.d(TAG,"JID is :" +from.toString());
        Log.d(TAG,"Presence type :" +subscribeRequest.getType().toString());

        //Update the subscription status to FROM_PENDING in the database.
//        ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.FROM_PENDING);
        Contact contact = ContactModel.get(mApplicationContext)
                .getContactByJidString(from.toString());

        Contact.SubscriptionType subType = contact.getSubscriptionType();

        if(subType == Contact.SubscriptionType.NONE_NONE)
        {
            //--> PENDING_NONE
            Log.d(TAG,"Current subscription is NONE_NONE, Updating to :" + contact.getTypeStringValue(Contact.SubscriptionType.PENDING_NONE));
            ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.PENDING_NONE);

        }else if (subType == Contact.SubscriptionType.NONE_PENDING)
        {
            //--> PENDING_PENDING
            Log.d(TAG,"Current subscription is NONE_PENDING, Updating to :" + contact.getTypeStringValue(Contact.SubscriptionType.PENDING_PENDING));
            ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.PENDING_PENDING);

        }else if (subType == Contact.SubscriptionType.NONE_TO)
        {
            // -- > PENDING_TO
            Log.d(TAG,"Current subscription is NONE_TO, Updating to :" + contact.getTypeStringValue(Contact.SubscriptionType.PENDING_TO));
            ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.PENDING_TO);

        }else if (subType == Contact.SubscriptionType.PENDING_NONE)
        {
            //-->PENDING_NONE
            Log.d(TAG,"Current subscription is  already in pending state : PENDING_NONE" );
//            ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.PENDING_TO);

        }else if (subType == Contact.SubscriptionType.PENDING_PENDING)
        {
            //--> PENDING_PENDING
            Log.d(TAG,"Current subscription is already in pending state : PENDING_PENDING");
//            ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.PENDING_TO);

        }else if (subType == Contact.SubscriptionType.PENDING_TO)
        {
            //-->PENDING_TO
            Log.d(TAG,"Current subscription is already in pending state : PENDING_TO" );
//            ContactModel.get(mApplicationContext).updateContactSubscription(from.toString(), Contact.SubscriptionType.PENDING_TO);


        }else if (subType == Contact.SubscriptionType.FROM_NONE)
        {
            //-->FROM_NONE
            Log.d(TAG,"Current subscription is already accepted : FROM_NONE" );



        }else if (subType == Contact.SubscriptionType.FROM_PENDING)
        {
            //-->FROM_PENDING
            Log.d(TAG,"Current subscription is already accepted : FROM_PENDING" );


        }else if (subType == Contact.SubscriptionType.FROM_TO)
        {
            //-->FROM_TO
            Log.d(TAG,"Current subscription is already accepted : FROM_TO" );

        }

        //We do not provide an answer right away, we let the user actively accept or deny this subscription.
        return null;
    }


    /** PresenceEventListener Overrides */
    @Override
    public void presenceAvailable(FullJid address, Presence availablePresence) {

    }

    @Override
    public void presenceUnavailable(FullJid address, Presence presence) {

    }

    @Override
    public void presenceError(Jid address, Presence errorPresence) {

    }

    @Override
    public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
        Log.d(TAG,"Presence subscribed :" + address.toString());

    }

    @Override
    public void presenceUnsubscribed(BareJid address, Presence unsubscribedPresence) {
        Log.d(TAG,"Presence unsubscribed : "+address.toString());

    }


    /** PresenceListener Overrides*/
    @Override
    public void processPresence(Presence presence) {
        Log.d(TAG,"Presence from :" +presence.getFrom().toString());
        Log.d(TAG,"Presence :" + presence.toString());

    }





    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(RoosterConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG,"Sent the broadcast that we are authenticated");
    }
}
