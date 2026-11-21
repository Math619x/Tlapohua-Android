package com.rey.celdas;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class ContestCooldown {
    static final int WAIT_ROUNDS = 6;
    private final Map<String, Long> until = new LinkedHashMap<>();
    private long rounds;

    ContestCooldown(String saved) {
        if (saved.isEmpty()) return;
        String[] fields = saved.split("\\|", -1);
        if (fields.length != 2) return;
        try {
            rounds = Math.max(0, Long.parseLong(fields[0]));
            for (String field : fields[1].split(",")) {
                int split = field.lastIndexOf('=');
                if (split <= 0) continue;
                long release = Long.parseLong(field.substring(split + 1));
                if (release > rounds) until.put(field.substring(0, split), release);
            }
        } catch (NumberFormatException invalid) {
            rounds = 0;
            until.clear();
        }
    }

    void record(ContestRun run, int round) {
        if (run.usedTime(round) < 0) return;
        rounds++;
        Iterator<Long> entries = until.values().iterator();
        while (entries.hasNext()) if (entries.next() <= rounds) entries.remove();
        int start = round * ContestRun.PER_ROUND;
        for (int i = start; i < start + run.boardCount(round); i++) {
            if (!run.boards[i].solved(run.values[i])) until.put(run.boards[i].id, rounds + WAIT_ROUNDS);
        }
    }

    Set<String> postponed() { return Collections.unmodifiableSet(until.keySet()); }

    String encode() {
        StringBuilder saved = new StringBuilder().append(rounds).append('|');
        boolean first = true;
        for (Map.Entry<String, Long> entry : until.entrySet()) {
            if (!first) saved.append(',');
            first = false;
            saved.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return saved.toString();
    }
}
