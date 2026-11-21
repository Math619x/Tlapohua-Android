package com.rey.celdas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

final class BluetoothFriends {
    private static final UUID SERVICE = UUID.fromString("cded204c-50b7-47db-9429-ae2f425d18e4");

    interface Listener {
        void confirm(BluetoothFriends exchange, String deviceName, int code);
        void completed(BluetoothFriends exchange, FriendCard received);
        void failed(BluetoothFriends exchange);
    }

    private final CountDownLatch approval = new CountDownLatch(1);
    private boolean approved;
    private volatile boolean closed;
    private BluetoothServerSocket server;
    private BluetoothSocket socket;

    void start(BluetoothAdapter adapter, BluetoothDevice device, FriendCard local, Listener listener) {
        new Thread(() -> {
            FriendCard received = null;
            try {
                boolean hosting = device == null;
                BluetoothSocket connection;
                if (hosting) {
                    BluetoothServerSocket waiting = adapter.listenUsingRfcommWithServiceRecord("Tlapohua Amigos", SERVICE);
                    register(waiting, null);
                    connection = waiting.accept(60_000);
                    register(null, connection);
                    waiting.close();
                } else {
                    connection = device.createRfcommSocketToServiceRecord(SERVICE);
                    register(null, connection);
                    connection.connect();
                }
                DataInputStream in = new DataInputStream(connection.getInputStream());
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                received = FriendLink.exchange(in, out, hosting, local, code -> {
                    listener.confirm(this, connection.getRemoteDevice().getName(), code);
                    approval.await();
                    return !closed && approved;
                });
            } catch (IOException | InterruptedException | RuntimeException error) {
                if (!closed) listener.failed(this);
                return;
            } finally {
                release();
            }
            if (!closed) listener.completed(this, received);
        }, "tlapohua-bluetooth").start();
    }

    void approve(boolean accept) {
        approved = accept;
        approval.countDown();
    }

    private synchronized void register(BluetoothServerSocket waiting, BluetoothSocket connection) throws IOException {
        if (closed) {
            closeQuietly(waiting);
            closeQuietly(connection);
            throw new IOException("Intercambio cancelado.");
        }
        if (waiting != null) server = waiting;
        if (connection != null) socket = connection;
    }

    void close() {
        closed = true;
        approval.countDown();
        release();
    }

    private synchronized void release() {
        closeQuietly(server);
        closeQuietly(socket);
        server = null;
        socket = null;
    }

    private static void closeQuietly(Closeable resource) {
        if (resource == null) return;
        try { resource.close(); } catch (IOException ignored) { }
    }
}
