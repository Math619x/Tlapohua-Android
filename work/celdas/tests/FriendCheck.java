package com.rey.celdas;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class FriendCheck {
    public static void main(String[] args) throws Exception {
        FriendCard mine = new FriendCard(UUID.randomUUID(), "Sol", 1000, 300, 13, 24, new long[8]);
        FriendCard other = new FriendCard(UUID.randomUUID(), "Jazmín", 2000, 900, 22, 38,
            new long[]{5000, 0, 58000, 69000, 0, 0, 0, 0});
        same(other, FriendCard.read(new ByteArrayInputStream(other.encode())));
        reject(Arrays.copyOf(other.encode(), other.encode().length - 1));
        reject(new byte[FriendCard.MAX_BYTES + 1]);
        if (FriendCard.validName("A\nB") || FriendCard.validName("   ") || other.newerThan(other) || !other.newerThan(null))
            throw new AssertionError("Validación o regla de actualización incorrecta");
        FriendCard prior = new FriendCard(other.id, other.name, 1999, 920, 22, 37, other.times);
        if (!other.newerThan(prior) || prior.newerThan(other)) throw new AssertionError("La fecha debe prevalecer sobre el puntaje");
        exchange(mine, other, true);
        exchange(mine, other, false);
        System.out.println("Liga: fichas validadas e intercambio bilateral con código y consentimiento probado por flujos.");
    }

    private static void reject(byte[] bytes) throws IOException {
        try {
            FriendCard.read(new ByteArrayInputStream(bytes));
            throw new AssertionError("Se aceptó una ficha inválida.");
        } catch (IOException expected) { }
    }

    private static void same(FriendCard expected, FriendCard actual) {
        if (!expected.id.equals(actual.id) || !expected.name.equals(actual.name) || expected.updatedAt != actual.updatedAt
                || expected.points != actual.points || expected.collected != actual.collected
                || expected.rankedSolved != actual.rankedSolved || !Arrays.equals(expected.times, actual.times))
            throw new AssertionError("La ficha cambió durante el intercambio.");
    }

    private static void exchange(FriendCard mine, FriendCard other, boolean bothAccept) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        AtomicInteger serverCode = new AtomicInteger(), clientCode = new AtomicInteger();
        try (PipedInputStream atServer = new PipedInputStream();
             PipedInputStream atClient = new PipedInputStream();
             PipedOutputStream toServer = new PipedOutputStream(atServer);
             PipedOutputStream toClient = new PipedOutputStream(atClient)) {
            Future<FriendCard> server = workers.submit(() -> FriendLink.exchange(new DataInputStream(atServer),
                new DataOutputStream(toClient), true, mine, code -> { serverCode.set(code); return true; }));
            Future<FriendCard> client = workers.submit(() -> FriendLink.exchange(new DataInputStream(atClient),
                new DataOutputStream(toServer), false, other, code -> { clientCode.set(code); return bothAccept; }));
            if (bothAccept) {
                same(other, server.get(5, TimeUnit.SECONDS));
                same(mine, client.get(5, TimeUnit.SECONDS));
            } else {
                expectRejected(server);
                expectRejected(client);
            }
            if (serverCode.get() < 100000 || clientCode.get() != serverCode.get()) throw new AssertionError("Códigos distintos");
        } finally {
            workers.shutdownNow();
        }
    }

    private static void expectRejected(Future<FriendCard> endpoint) throws Exception {
        try {
            endpoint.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Se transfirió una ficha sin consentimiento.");
        } catch (ExecutionException rejected) {
            if (!(rejected.getCause() instanceof IOException)) throw rejected;
        }
    }
}
