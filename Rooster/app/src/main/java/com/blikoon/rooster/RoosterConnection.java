package com.blikoon.rooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blikoon.rooster.model.ChatMessage;
import com.blikoon.rooster.model.ChatMessageModel;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
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
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;
import org.jivesoftware.smackx.debugger.android.AndroidDebugger;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;

import de.duenndns.ssl.MemorizingTrustManager;


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

    private final static int CONNECT_TIMEOUT = 1000 * 60;

    private ProxyInfo mProxyInfo = null;

    private SSLContext sslContext;

    private final static String SSLCONTEXT_TYPE = "TLS";

    private SecureRandom secureRandom;

    private MemorizingTrustManager mMemTrust;





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
//            Log.d(TAG, "(DNS SRV) resolving: " + domain);
//            List<HostAddress> listHostsFailed = new ArrayList<>();
//            List<HostAddress> listHosts = DNSUtil.resolveXMPPServiceDomain(domain, listHostsFailed, ConnectionConfiguration.DnssecMode.disabled);
//
//            if (listHosts.size() > 0) {
//                server = listHosts.get(0).getFQDN();
//                serverPort = listHosts.get(0).getPort();
//
//                Log.d(TAG, "(DNS SRV) resolved: " + domain + "=" + server + ":" + serverPort);
//            }
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

        mConnection.addConnectionListener(this);

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

    /** PingFailedListener Overrides */

    @Override
    public void pingFailed() {
        Log.d(TAG,"Ping Failed Method called.");
    }





    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(RoosterConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG,"Sent the broadcast that we are authenticated");
    }
}
