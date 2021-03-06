package be.hepl.benbear.boatapp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import be.hepl.benbear.iobrep.AuthenticatedPacket;
import be.hepl.benbear.iobrep.Packet;
import be.hepl.benbear.iobrep.ResponsePacket;

public class ServerCommunicationService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private Socket sock = null;
    private ObjectInputStream ois = null;
    private ObjectOutputStream oos = null;
    private SharedPreferences settings;
    private ReadPacketTask readPacketTask = null;
    private final Queue<ResponsePacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Set<PacketNotificationListener> listeners = new HashSet<>();

    public void addOnPacketReceptionListener(PacketNotificationListener listener) {
        this.listeners.add(listener);
    }

    public void removeOnPacketReceptionListener(PacketNotificationListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Creation server connection service", Toast.LENGTH_SHORT).show();
        settings = getSharedPreferences(getString(R.string.config_file), 0);
        establishConnection();
    }

    // TODO Check if connection was established
    public void establishConnection() {
        Log.d("DEBUG SOCKET CREA", "Before AsyncTask");

        onDestroy();

        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.d("DEBUG SOCKET CREA", "Before try");
                try {
                    Log.d("DEBUG SOCKET CREA", "Check for existing connection");
                    if (isEstablished()) {
                        oos.close();
                        ois.close();
                        sock.close();
                    }
                    Log.d("HUE", settings.getString("server_ip", "0.0.0.0")+ " " +settings.getInt("server_port", 30000));
                    sock = new Socket(InetAddress.getByName(settings.getString("server_ip", "0.0.0.0")), settings.getInt("server_port", 30000));
                    Log.d("DEBUG SOCKET", "SOCK = " + sock.toString() + " IS CONNECTED? = " + sock.isConnected());
                    oos = new ObjectOutputStream(sock.getOutputStream());
                    oos.flush();
                    Log.d("DEBUG SOCKET", "OOS = " + oos.toString());
                    ois = new ObjectInputStream(sock.getInputStream());
                    Log.d("DEBUG SOCKET", "OIS = " + ois.toString());

                    startReadTask();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

        Log.d("DEBUG SOCKET CREA", "After AsyncTask");
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Destruction server connection", Toast.LENGTH_LONG).show();
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if(readPacketTask != null) {
            readPacketTask.stop();
        }
    }

    public void startReadTask() {
        readPacketTask = new ReadPacketTask();
        readPacketTask.execute();
    }

    public ResponsePacket getPacket() {
        return packetQueue.poll();
    }

    public class LocalBinder extends Binder {
        ServerCommunicationService getService() {
            return ServerCommunicationService.this;
        }
    }

    public boolean isEstablished() {
        return sock != null;
    }

    private class ReadPacketTask extends AsyncTask<Void, Integer, Void> {

        private volatile boolean stopped = false;

        public void stop() {
            stopped = true;
            this.cancel(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while(!stopped) {
                Log.d("BOAT SERVICE", "============================ reading =========");
                try {
                    readPacket();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public void writePacket(Packet p) throws ProtocolException {
        try {
            Log.d("DEBUG WRITING", "Writing (" + p.getClass() + ") " + "on " + oos);
            if(p instanceof AuthenticatedPacket){
                Log.d("DEBUG WRITING", "With session (" + ((AuthenticatedPacket)p).getSession() +")");
            }
            oos.writeObject(p);
        } catch (IOException e) {
            throw new ProtocolException("Network error", e);
        }
    }

    private void readPacket() throws ProtocolException {
        ResponsePacket p = readPacket(0);
        Log.d("DEBUG READING", "Reading packet of type: " + p.getClass());
        packetQueue.add(p);
        for (PacketNotificationListener listener : listeners) {
            listener.onPacketReception();
        }
    }

    private ResponsePacket readPacket(int tries) throws ProtocolException {
        try {
            Object o = ois.readObject();

            if (o instanceof ResponsePacket) {
                return ((ResponsePacket) o);
            }
            throw new ProtocolException("Invalid packet received: " + o.getClass().getName());
        } catch (ClassNotFoundException | IOException e) {
            if(tries < 3) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tries * 500);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return readPacket(tries + 1);
            }
            throw new ProtocolException(e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public UUID getSession() {
        String session = settings.getString("session", null);
        if(session == null) {
            return null;
        }
        return UUID.fromString(session);
    }

    public void setSession(UUID session) {
        SharedPreferences.Editor editor = settings.edit();
        if(session == null) {
            editor.remove("session");
        } else {
            editor.putString("session", session.toString());
        }
        editor.commit();
    }
}
