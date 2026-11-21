package com.rey.celdas;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

final class FriendLink {
    private static final int PROTOCOL = 0x544c4201;

    interface Confirmation {
        boolean confirm(int code) throws InterruptedException;
    }

    static FriendCard exchange(DataInputStream in, DataOutputStream out, boolean hosting, FriendCard local,
            Confirmation confirmation) throws IOException, InterruptedException {
        out.writeInt(PROTOCOL);
        out.flush();
        if (in.readInt() != PROTOCOL) throw new IOException("Protocolo incompatible.");
        int code;
        if (hosting) {
            code = new SecureRandom().nextInt(900000) + 100000;
            out.writeInt(code);
            out.flush();
        } else {
            code = in.readInt();
            if (code < 100000 || code > 999999) throw new IOException("Código inválido.");
        }
        boolean approved = confirmation.confirm(code);
        out.writeBoolean(approved);
        out.flush();
        if (!in.readBoolean() || !approved) throw new IOException("Intercambio rechazado.");
        if (hosting) {
            FriendCard received = read(in);
            send(out, local);
            if (!in.readBoolean()) throw new IOException("Sin confirmación.");
            return received;
        }
        send(out, local);
        FriendCard received = read(in);
        out.writeBoolean(true);
        out.flush();
        return received;
    }

    private static void send(DataOutputStream out, FriendCard card) throws IOException {
        byte[] data = card.encode();
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    private static FriendCard read(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size <= 0 || size > FriendCard.MAX_BYTES) throw new IOException("Tamaño de ficha inválido.");
        byte[] data = new byte[size];
        in.readFully(data);
        return FriendCard.decode(data);
    }
}
