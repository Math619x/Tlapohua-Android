package com.rey.celdas;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class MainActivity extends Activity {
    private static final int SAVE_COLLECTION = 100;
    private static final int SAVE_CONTEST = 101;
    private static final int SAVE_CONTEST_KEY = 102;
    private static final int CHOOSE_INTRO_LOGO = 103;
    private static final int SAVE_STUDY_REPORT = 104;
    private static final int EXPORT_FRIEND = 105, IMPORT_FRIEND = 106, FRIENDS_BLUETOOTH_PERMISSION = 107;
    private static final int EXPORT_PROGRESS = 108, IMPORT_PROGRESS = 109;
    private static final String BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    private static final String FRIEND_ID = "friends-local-id", FRIEND_NAME = "friends-local-name", FRIEND_PREFIX = "friend-card-";
    private static final String TIMED_ACTIVE = "timed-practice-active";
    private static final long INTRO_PANEL_MS = 1500L;
    private static final String CONTEST_ACTIVE = "concurso-activo";
    private static final String CONTEST_HISTORY = "concurso-historial-";
    private static final String CONTEST_COOLDOWN = "concurso-espera";
    private static final String RANKED_ACTIVE = "torneo-activo";
    private static final String RANKED_HISTORY = "torneo-historial-";
    private static final String RANKED_COOLDOWN = "torneo-espera";
    private static final String RANKED_POINTS = "torneo-puntos-rango";
    private static final String RANKED_START_POINTS = "torneo-rango-previo-";
    private static final String RANKED_CHANGE = "torneo-rango-cambio-";
    private static final String RANKED_TITLE = "Cumbre de Turquesa";
    private static final String SKIP_TOURNAMENT_GUIDE = "skip-tournament-guide";
    private static final String DAILY_CARVING = "daily-carving-pending";
    private static final String LARGE_NUMBERS = "display-large-numbers";
    private static final String ZOOM_BOARD = "display-zoom-board";
    private static final String HIGH_CONTRAST = "display-high-contrast";
    private static final String REDUCED_MOTION = "display-reduced-motion";
    private static final String BEST_TIME = "record-size-";
    private static final String CASUAL_FOREST = "forest-casual";
    private static final char[] PRACTICE_OPERATIONS = {'+', '-', '*', '/'};
    private static final String ELAPSED = "-elapsed-ms";
    private static final String ATTEMPT = "-attempt";
    private static final int BACKGROUND = TlapohuaArt.PAPER;
    private static final int GREEN = 0xff17463d;
    private static final int INK = 0xff243d34;
    private static final int MUTED = 0xff506256;
    private static final int SOFT = TlapohuaArt.SAND;
    private static final int WHITE = 0xffffffff;
    private static final int COMPLETED = 0xffc6e6cb;
    private static final int COMPLETED_INK = 0xff205936;
    private static final int AT_LIMIT = 0xfff2dfb9;
    private static final int[] CAGE_COLORS = {0xffd9eadc, 0xfff7e6c5, 0xffdce9e6, 0xfff2dacc, 0xffe1e7cf,
        0xffeed8b7, 0xffd5e7df, 0xfff2e9d8, 0xffdedce9, 0xffead7ca};

    private SharedPreferences prefs;
    private List<Puzzle> catalog, dailyPool;
    private enum Mode { FREE, LESSON, FOREST, DAILY, CONTEST, PRACTICE }
    private enum Screen { INTRO, HOME, PLAY, ROUTE, DAILY_CALENDAR, LESSONS, ZONES, SIZES, BOARDS, GAME, COLLECTION, PDF_PREVIEW,
        CONTEST, CONTEST_SETUP, CONTEST_ROUND, CONTEST_RESULTS, CONTEST_RECORD, PRACTICE, RECOMMENDED, RECORDS, DISPLAY, FRIENDS, BACKUP, PRIVACY, LEARNING }
    private Mode mode = Mode.FREE;
    private Screen screen = Screen.HOME;
    private View visiblePage;
    private boolean returning;
    private String selectedFamily;
    private int selectedSize;
    private int previewPage;
    private Journey journey;
    private final Map<String, Puzzle> contestBoards = new LinkedHashMap<>();
    private ContestRun contest;
    private ContestCooldown contestCooldown;
    private boolean contestPro;
    private boolean rankedContest;
    private boolean notebookHistory;
    private ValueAnimator rankAnimation;
    private ValueAnimator introAnimation;
    private boolean savingIntroLogo;
    private TempleMotion templeMotion;
    private long contestRecordId;
    private char practiceOperation = '+';
    private int practiceSize;
    private String practiceExcluded = "", practiceBoard = "";
    private BluetoothFriends friendExchange;
    private AlertDialog friendDialog;
    private final Runnable friendTimeout = () -> {
        stopFriendExchange();
        Toast.makeText(this, "Se agotó el tiempo de intercambio.", Toast.LENGTH_SHORT).show();
    };
    private TimedPractice timedPractice;
    private TextView practiceCountdown;
    private final Runnable practiceTick = this::updatePracticeCountdown;
    private TextView contestClock;
    private AlertDialog contestConfirmation;
    private AlertDialog tournamentGuide;
    private int tournamentGuideStep;
    private boolean tournamentGuideEnrollment;
    private final Handler contestHandler = new Handler(android.os.Looper.getMainLooper());
    private final Runnable contestTick = this::updateContestClock;
    private String contestPdfState = "";
    private final ForestBoards forestBoards = new ForestBoards();
    private ForestTravel forestTravel;
    private boolean accepted, reviewing;
    private int mistakes;
    private int lessonIndex;
    private String dailyDate;
    private Calendar dailyMonth;
    private Puzzle puzzle;
    private int[] values, notes;
    private int selected;
    private boolean pencil, provisionalMode, wasSolved;
    private final ArrayDeque<Move> undo = new ArrayDeque<>();
    private Board board;
    private Chronometer clock;
    private String clockKey;
    private long elapsed, startedAt = -1;
    private boolean resumed;
    private BoardAttempt attempt;
    private TextView status, routeStatus;
    private LinearLayout editRow, provisionalRow;
    private Button noteButton, provisionalButton, confirmProvisionalButton, undoButton, nextBoardButton, submitButton, restartButton;
    private Button[] digitButtons;

    private static final class Move {
        final int cell, value, notes;
        final int position;
        final BoardAttempt.Entry entry;

        Move(int cell, int value, int notes, BoardAttempt attempt) {
            this.cell = cell;
            this.value = value;
            this.notes = notes;
            position = attempt == null ? -1 : attempt.indexOf(cell);
            entry = attempt == null ? null : attempt.at(position);
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                this::onBackPressed);
        }
        prefs = getSharedPreferences("partidas", MODE_PRIVATE);
        try {
            if (ProgressRestore.recover(progressStore(), new ProgressJournal(this)))
                Toast.makeText(this, "Se recuperó el progreso anterior a una restauración interrumpida.", Toast.LENGTH_LONG).show();
        } catch (java.io.IOException error) {
            new AlertDialog.Builder(this).setTitle("Recuperación pendiente")
                .setMessage("No se pudo recuperar el progreso. Libera espacio y vuelve a abrir Tlapohua.")
                .setPositiveButton("Cerrar", (dialog, which) -> finish()).setCancelable(false).show();
            return;
        }
        journey = new Journey(prefs.getString("forest-route", ""));
        getWindow().setBackgroundDrawable(new ColorDrawable(BACKGROUND));
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        try {
            catalog = PuzzleCatalog.load(this);
        } catch (java.io.IOException | android.database.sqlite.SQLiteException | IllegalArgumentException error) {
            android.util.Log.e("Tlapohua", "No se pudo abrir el catálogo", error);
            new AlertDialog.Builder(this).setTitle("No se pudo abrir el catálogo")
                .setMessage("Vuelve a instalar esta versión para recuperar los tableros incluidos.")
                .setPositiveButton("Cerrar", (dialog, which) -> finish()).show();
            return;
        }
        dailyPool = DailyDates.pool(catalog);
        importCompletedBoards();
        if (state == null) {
            showIntro();
            return;
        }
        contestPro = state.getBoolean("contest-pro");
        rankedContest = state.getBoolean("ranked-contest");
        notebookHistory = state.getBoolean("notebook-history");
        contestPdfState = state.getString("contest-pdf", "");
        practiceOperation = (char) state.getInt("practice-operation", '+');
        practiceSize = state.getInt("practice-size");
        practiceExcluded = state.getString("practice-excluded", "");
        practiceBoard = state.getString("practice-board", "");
        switch (Screen.valueOf(state.getString("screen", Screen.HOME.name()))) {
            case PLAY: showPlay(); break;
            case PRACTICE: showPractice(); break;
            case RECOMMENDED: showRecommendedPractice(); break;
            case LEARNING: showLearningProgress(); break;
            case RECORDS: showPersonalRecords(); break;
            case FRIENDS: showFriends(); break;
            case DISPLAY: showDisplaySettings(); break;
            case BACKUP: showBackup(); break;
            case PRIVACY: showPrivacy(); break;
            case ROUTE: showJourney(); break;
            case DAILY_CALENDAR: showDailyCalendar(restoredDailyDate(state, "daily-month")); break;
            case LESSONS: chooseLesson(); break;
            case COLLECTION: showCollection(); break;
            case PDF_PREVIEW: showPdfPreview(state.getInt("preview-page")); break;
            case ZONES: chooseLevel(); break;
            case SIZES: chooseSize(state.getString("family")); break;
            case BOARDS: chooseBoard(state.getString("family"), state.getInt("size")); break;
            case CONTEST: showContest(); break;
            case CONTEST_SETUP: showContestLevels(contestPro); break;
            case CONTEST_ROUND: showContestRound(); break;
            case CONTEST_RESULTS: showContestResults(); break;
            case CONTEST_RECORD: showContestRecord(state.getLong("contest-record")); break;
            case GAME: restoreGame(state); break;
            default: showHome();
        }
        if (state.getBoolean("tournament-guide-open") && rankedContest && screen == Screen.CONTEST)
            showTournamentGuide(state.getInt("tournament-guide-step"), state.getBoolean("tournament-guide-enrollment"));
    }

    private void restoreGame(Bundle state) {
        switch (Mode.valueOf(state.getString("mode", Mode.FREE.name()))) {
            case FOREST:
                Puzzle current = findPuzzle(journey.active);
                if (current == null || journey.finished) showJourney();
                else openForestBoard(current);
                break;
            case DAILY: openDaily(restoredDailyDate(state, "daily-date")); break;
            case LESSON:
                openLesson(state.getInt("lesson", 0));
                int[] lessonNotes = state.getIntArray("lesson-notes");
                if (lessonNotes != null && lessonNotes.length == notes.length) {
                    for (int i = 0; i < notes.length; i++) {
                        int note = lessonNotes[i];
                        if (note >= 0 && note < ((puzzle.size + 1) << puzzle.size)) notes[i] = note;
                    }
                    refresh(null);
                }
                break;
            case PRACTICE:
                TimedPractice restored = TimedPractice.decode(state.getString("timed-practice", ""));
                if (restored != null && findPuzzle(restored.boardId) != null) {
                    openTimedPractice(restored);
                    break;
                }
                Puzzle drill = practiceBoard.isEmpty() ? null : findPuzzle(practiceBoard);
                if (drill == null) showPractice();
                else openPractice(drill);
                break;
            case CONTEST:
                if (loadContest() && contest != null && contest.running()) openContestBoard(contest.board);
                else showContestRound();
                break;
            default: openFree();
        }
    }

    private void openFree() {
        String active = prefs.getString("active-board", "");
        if (active.isEmpty() && prefs.contains("level")) {
            int oldIndex = prefs.getInt("level", 0);
            if (oldIndex == 0) active = "tutorial";
            else if (oldIndex == 1) active = "reto";
            else if (oldIndex == 2) active = prefs.contains("senderos-values") ? "senderos" : "senderos-7";
        }
        Puzzle initial = catalog.get(0);
        for (Puzzle candidate : catalog) if (candidate.id.equals(active)) initial = candidate;
        openLevel(initial);
    }

    private void openLevel(Puzzle level) {
        mode = Mode.FREE;
        prefs.edit().putString("active-board", level.id).apply();
        loadBoard(level);
    }

    private void loadBoard(Puzzle level) {
        pauseClock();
        puzzle = level;
        clockKey = mode == Mode.LESSON ? null : storageKey() + ELAPSED;
        elapsed = clockKey == null ? 0 : prefs.getLong(clockKey, 0);
        values = readCells("values", puzzle.size);
        notes = readCells("notes", ((puzzle.size + 1) << puzzle.size) - 1);
        String key = storageKey();
        attempt = mode == Mode.LESSON ? null : new BoardAttempt(values, prefs.getString(key + ATTEMPT, ""));
        mistakes = mode == Mode.FOREST ? journey.mistakes : prefs.getInt(key + "-mistakes", 0);
        undo.clear();
        pencil = provisionalMode = false;
        accepted = mode != Mode.LESSON && prefs.getBoolean(key + "-accepted",
            !prefs.contains(key + ATTEMPT) && puzzle.solved(values));
        selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) { selected = i; break; }
        }
        if (mode == Mode.LESSON) {
            Lesson lesson = Lesson.ALL[lessonIndex];
            for (int cell : lesson.givens) values[cell] = puzzle.solution[cell];
            selected = lesson.cell;
        }
        if (accepted && !prefs.contains(key + "-accepted")) {
            SharedPreferences.Editor editor = prefs.edit().putBoolean(key + "-accepted", true);
            if (mode != Mode.PRACTICE) editor.putBoolean(collectionKey(puzzle.id), true);
            editor.apply();
        }
        if (accepted && mode == Mode.FOREST) recordForestWin();
        wasSolved = isComplete();
        showGame();
        refresh(null);
    }

    private int[] readCells(String key, int max) {
        int[] cells = new int[puzzle.cage.length];
        if (mode == Mode.LESSON) return cells;
        String[] pieces = prefs.getString(storageKey() + "-" + key, "").split(",");
        if (pieces.length != cells.length) return cells;
        for (int i = 0; i < cells.length; i++) {
            try {
                int value = Integer.parseInt(pieces[i]);
                if (value >= 0 && value <= max) cells[i] = value;
            } catch (NumberFormatException ignored) { }
        }
        return cells;
    }

    private String encode(int[] cells) {
        StringBuilder data = new StringBuilder();
        for (int value : cells) {
            if (data.length() > 0) data.append(',');
            data.append(value);
        }
        return data.toString();
    }

    private void save() {
        if (mode == Mode.LESSON) return;
        if (mode == Mode.CONTEST) { saveContest(); return; }
        SharedPreferences.Editor editor = prefs.edit();
        save(editor);
        editor.apply();
    }

    private void save(SharedPreferences.Editor editor) {
        String key = storageKey();
        editor.putString(key + "-values", encode(values)).putString(key + "-notes", encode(notes))
            .putString(key + ATTEMPT, attempt.encode()).putBoolean(key + "-accepted", accepted);
        if (mode == Mode.FOREST) editor.putString("forest-route", journey.encode());
        else editor.putInt(key + "-mistakes", mistakes);
        editor.putLong(clockKey, clockTime());
        if (accepted) {
            if (mode != Mode.PRACTICE) editor.putBoolean(collectionKey(puzzle.id), true);
            if (mode == Mode.DAILY && !prefs.getBoolean(dailyCompletedKey(dailyDate), false)) {
                editor.putBoolean(dailyCompletedKey(dailyDate), true).putString(DAILY_CARVING, dailyDate);
            }
        }
    }

    private long clockTime() {
        return elapsed + (startedAt < 0 ? 0 : SystemClock.elapsedRealtime() - startedAt);
    }

    private void pauseClock() {
        if (startedAt >= 0) {
            elapsed = clockTime();
            startedAt = -1;
            prefs.edit().putLong(clockKey, elapsed).apply();
        }
        if (clock != null) clock.stop();
    }

    private void updateClock(boolean solved) {
        if (clock == null) return;
        if (solved) pauseClock();
        if (solved && !prefs.contains(clockKey)) {
            clock.setText("Tiempo · sin registro");
            return;
        }
        if (!solved && !reviewing && resumed && startedAt < 0) startedAt = SystemClock.elapsedRealtime();
        clock.setBase(SystemClock.elapsedRealtime() - clockTime());
        if (startedAt >= 0) clock.start();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (introAnimation != null && introAnimation.isPaused()) introAnimation.resume();
        if (forestTravel != null) forestTravel.resume();
        if (screen == Screen.CONTEST_ROUND || (screen == Screen.GAME && mode == Mode.CONTEST)) updateContestClock();
        else if (screen == Screen.GAME) updateClock(wasSolved);
        if (screen == Screen.GAME) updatePracticeCountdown();
    }

    @Override protected void onPause() {
        resumed = false;
        stopFriendExchange();
        if (introAnimation != null) introAnimation.pause();
        if (templeMotion != null) templeMotion.dismiss();
        contestHandler.removeCallbacks(contestTick);
        contestHandler.removeCallbacks(practiceTick);
        if (forestTravel != null) forestTravel.pause();
        pauseClock();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (tournamentGuide != null) tournamentGuide.dismiss();
        stopFriendExchange();
        stopIntro();
        if (templeMotion != null) templeMotion.dismiss();
        if (rankAnimation != null) rankAnimation.cancel();
        contestHandler.removeCallbacks(contestTick);
        contestHandler.removeCallbacks(practiceTick);
        if (contestConfirmation != null) contestConfirmation.dismiss();
        if (forestTravel != null) forestTravel.dispose();
        super.onDestroy();
    }

    private String storageKey() {
        if (mode == Mode.PRACTICE) return (timedPractice == null ? "practice-" : "timed-practice-") + puzzle.id;
        if (mode == Mode.DAILY) return dailyStorageKey(dailyDate, puzzle.id);
        if (mode == Mode.FOREST) return forestKey(journey.stage, puzzle.id);
        return puzzle.id;
    }

    private String forestKey(int stage, String boardId) {
        return "forest-" + stage + "-" + boardId;
    }

    private String dailyStorageKey(String date, String boardId) {
        return "daily-" + date + "-" + boardId;
    }

    private String dailyCompletedKey(String date) {
        return "daily-completed-" + date;
    }

    private Puzzle findPuzzle(String id) {
        for (Puzzle entry : catalog) if (entry.id.equals(id)) return entry;
        return forestBoards.find(id);
    }

    private boolean isComplete() {
        if (mode == Mode.LESSON) {
            int cell = Lesson.ALL[lessonIndex].cell;
            return values[cell] == puzzle.solution[cell];
        }
        return accepted;
    }

    private String collectionKey(String id) {
        return "collection-board-" + id;
    }

    private void importCompletedBoards() {
        if (prefs.getBoolean("collection-imported", false)) return;
        Map<String, ?> previous = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();
        for (Puzzle entry : catalog) {
            if (encode(entry.solution).equals(previous.get(entry.id + "-values"))) {
                editor.putBoolean(collectionKey(entry.id), true);
            }
        }
        for (Map.Entry<String, ?> saved : previous.entrySet()) {
            String key = saved.getKey();
            if (key.startsWith("daily-board-") && saved.getValue() instanceof String) {
                Puzzle entry = findPuzzle((String) saved.getValue());
                String date = key.substring("daily-board-".length());
                if (entry != null && encode(entry.solution).equals(previous.get("daily-" + date + "-" + entry.id + "-values"))) {
                    editor.putBoolean(collectionKey(entry.id), true);
                }
            } else if (key.startsWith("forest-") && key.endsWith("-accepted") && Boolean.TRUE.equals(saved.getValue())) {
                int start = key.indexOf('-', "forest-".length()) + 1;
                int end = key.length() - "-accepted".length();
                if (start > 0 && start < end && findPuzzle(key.substring(start, end)) != null) {
                    editor.putBoolean(collectionKey(key.substring(start, end)), true);
                }
            }
        }
        editor.putBoolean("collection-imported", true).apply();
    }

    private List<Puzzle> collectedBoards() {
        List<Puzzle> collected = new ArrayList<>();
        for (Puzzle entry : catalog) {
            if (prefs.getBoolean(collectionKey(entry.id), false)) collected.add(entry);
        }
        for (Map.Entry<String, ?> saved : prefs.getAll().entrySet()) {
            String key = saved.getKey();
            if (!key.startsWith(collectionKey(ForestBoards.PREFIX)) || !Boolean.TRUE.equals(saved.getValue())) continue;
            Puzzle entry = findPuzzle(key.substring(collectionKey("").length()));
            if (entry != null) collected.add(entry);
        }
        java.util.Collections.sort(collected, (left, right) -> {
            int sizeOrder = Integer.compare(left.size, right.size);
            return sizeOrder != 0 ? sizeOrder : left.id.compareTo(right.id);
        });
        return collected;
    }

    private void showIntro() {
        if (!motionEnabled()) { showHome(); return; }
        Bitmap customLogo = IntroLogo.load(this);
        int creatorLogo = getResources().getIdentifier("logo_atr", "drawable", getPackageName());
        FrameLayout stage = new FrameLayout(this);
        stage.setContentDescription("Presentación de Tlapohua. Toca para omitir.");
        stage.setOnClickListener(view -> finishIntro());
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER);
        stage.addView(brand, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        ImageView emblem = new ImageView(this);
        emblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(emblem, new LinearLayout.LayoutParams(dp(136), dp(136)));
        gap(brand, 12);
        TextView title = addText(brand, "", 32, TlapohuaArt.CREAM, true);
        title.setGravity(Gravity.CENTER);
        int panels = 2;
        int[] displayed = {-1};
        brand.setAlpha(0f);
        present(stage, Screen.INTRO);
        introAnimation = ValueAnimator.ofFloat(0f, panels);
        introAnimation.setDuration(INTRO_PANEL_MS * panels);
        introAnimation.setInterpolator(new LinearInterpolator());
        introAnimation.addUpdateListener(animation -> {
            float position = (float) animation.getAnimatedValue();
            int panel = Math.min(panels - 1, (int) position);
            if (panel != displayed[0]) {
                boolean creator = panel == 1;
                stage.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    creator ? new int[]{0xff090e0c, 0xff161f1a} : new int[]{0xff122d28, 0xff276253}));
                if (creator && customLogo != null) emblem.setImageBitmap(customLogo);
                else emblem.setImageResource(creator ? creatorLogo : getApplicationInfo().icon);
                int size = dp(creator ? 196 : 136);
                emblem.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                title.setText("Tlapohua");
                title.setVisibility(creator ? View.GONE : View.VISIBLE);
                displayed[0] = panel;
            }
            float fraction = position - panel;
            brand.setAlpha(Math.min(1f, 5f * Math.min(fraction, 1f - fraction)));
            float scale = .93f + .07f * Math.min(1f, fraction * 2f);
            brand.setScaleX(scale);
            brand.setScaleY(scale);
        });
        introAnimation.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { finishIntro(); }
        });
        introAnimation.start();
    }

    private void stopIntro() {
        if (introAnimation == null) return;
        introAnimation.removeAllListeners();
        introAnimation.removeAllUpdateListeners();
        introAnimation.cancel();
        introAnimation = null;
    }

    private void finishIntro() {
        stopIntro();
        if (screen == Screen.INTRO) showHome();
    }

    private void showHome() {
        LinearLayout choices = menuPage(Screen.HOME, "Menú principal", "Juega y reúne tableros para imprimir.");
        choiceCard(choices, TlapohuaArt.Mark.GRID, "Jugar", "Elige un modo de juego.", this::showPlay);
        choiceCard(choices, TlapohuaArt.Mark.LESSON, "Tutorial", "Practica las operaciones paso a paso.", this::chooseLesson);
        choiceCard(choices, TlapohuaArt.Mark.COLLECTION, "Mi colección",
            "Reúne e imprime los tableros que resuelvas.", this::showCollection);
        choiceCard(choices, TlapohuaArt.Mark.EXIT, "Salir", "Cierra Tlapohua.", this::finishAndRemoveTask);
    }

    private void showPlay() {
        LinearLayout choices = menuPage(Screen.PLAY, "Jugar", "Elige un modo para empezar o continuar.");
        if (prefs.contains("continue-mode") || prefs.contains("active-board")) {
            choiceCard(choices, TlapohuaArt.Mark.GRID, "Continuar jugando", "Retoma tu última partida.", this::continuePlaying);
        } else {
            choiceCard(choices, TlapohuaArt.Mark.LESSON, "Empieza aquí", "Aprende las reglas con un tablero guiado.", this::chooseLesson);
        }
        addText(choices, "Aprender y practicar", 18, GREEN, true);
        action(row(choices), "Tutorial por casos", false, this::chooseLesson);
        action(row(choices), "Practicar por operación", false, () -> {
            practiceExcluded = "";
            showPractice();
        });
        action(row(choices), "Progreso de aprendizaje y próxima práctica", false, this::showLearningProgress);
        gap(choices, 10);
        addText(choices, "Explorar y jugar", 18, GREEN, true);
        choiceCard(choices, TlapohuaArt.Mark.GRID, "Juego libre",
            "Elige la zona, el tamaño y el tablero.", this::chooseLevel);
        choiceCard(choices, TlapohuaArt.Mark.TRAIL, "Ruta del bosque",
            "Supera siete niveles hasta la Cumbre de jade.", this::showJourney);
        gap(choices, 10);
        addText(choices, "Retos y torneos", 18, GREEN, true);
        choiceCard(choices, TlapohuaArt.Mark.SUN, "Reto diario",
            "Juega el reto de hoy o abre una fecha anterior.", () -> showDailyCalendar(DailyDates.today()));
        choiceCard(choices, TlapohuaArt.Mark.GRID, "Entrenamiento de concurso",
            "Tres rondas de práctica con reloj compartido.", () -> openContest(false));
        choiceCard(choices, TlapohuaArt.Mark.SUN, "Torneo", RANKED_TITLE, () -> {
            openContest(true);
            if (screen == Screen.CONTEST)
                playTemple(TempleMotion.Scene.GATE, RANKED_TITLE, "Rango " + ContestRun.rankName(currentRank()), () -> {});
        });
        gap(choices, 14);
        action(row(choices), "Marcas personales y cuaderno de errores", false, this::showPersonalRecords);
        action(row(choices), "Liga de amigos", false, this::showFriends);
        action(row(choices), "Ajustes, respaldo y privacidad", false, this::showDisplaySettings);
    }

    private void continuePlaying() {
        String savedMode = prefs.getString("continue-mode", Mode.FREE.name());
        if (savedMode.equals(Mode.CONTEST.name())) {
            openContest(prefs.getBoolean("continue-ranked", false));
            if (contest != null && !contest.finished()) showContestRound();
        } else if (savedMode.equals(Mode.FOREST.name())) {
            Puzzle entry = findPuzzle(journey.active);
            if (entry == null || journey.finished) showJourney();
            else openForestBoard(entry);
        } else if (savedMode.equals(Mode.DAILY.name())) {
            Calendar day = DailyDates.parse(prefs.getString("continue-date", ""));
            openDaily(day == null ? DailyDates.today() : day);
        } else if (savedMode.equals(Mode.PRACTICE.name())) {
            TimedPractice pending = TimedPractice.decode(prefs.getString(TIMED_ACTIVE, ""));
            Puzzle entry = findPuzzle(prefs.getString("continue-board", ""));
            if (pending != null) openTimedPractice(pending);
            else if (entry != null) {
                char operation = (char) prefs.getInt("continue-operation", '+');
                practiceOperation = "+-*/".indexOf(operation) >= 0 ? operation : '+';
                openPractice(entry);
            }
            else showPractice();
        } else openFree();
    }

    private void rememberCurrentGame() {
        if (mode == Mode.LESSON) return;
        prefs.edit().putString("continue-mode", mode.name()).putString("continue-board", puzzle.id)
            .putString("continue-date", dailyDate == null ? "" : dailyDate)
            .putInt("continue-operation", practiceOperation).putBoolean("continue-ranked", rankedContest).apply();
    }

    private boolean motionEnabled() {
        return !prefs.getBoolean(REDUCED_MOTION, false) && ForestTravel.motionEnabled();
    }

    private void showDisplaySettings() {
        LinearLayout menu = menuPage(Screen.DISPLAY, "Ajustes y datos", "Estos cambios se guardan en este teléfono.");
        addDisplaySwitch(menu, "Números y pistas grandes", LARGE_NUMBERS);
        addDisplaySwitch(menu, "Ampliar casillas del tablero", ZOOM_BOARD);
        addText(menu, "Al ampliar, desliza el tablero de lado a lado para recorrer las casillas.", 14, MUTED, false);
        addDisplaySwitch(menu, "Tablero de alto contraste", HIGH_CONTRAST);
        addDisplaySwitch(menu, "Reducir movimiento", REDUCED_MOTION);
        addText(menu, "Reduce movimiento para saltar la presentación, la cuenta regresiva visual y las transiciones decorativas.",
            14, MUTED, false);
        action(row(menu), "Respaldar o recuperar mi progreso", false, this::showBackup);
        action(row(menu), "Privacidad y datos", false, this::showPrivacy);
    }

    private void showBackup() {
        LinearLayout menu = menuPage(Screen.BACKUP, "Respaldo de progreso",
            "Guarda una copia antes de cambiar de teléfono o desinstalar Tlapohua.");
        addText(menu, "Incluye partidas, notas, provisionales, ruta, retos diarios, rangos, historial, marcas, apodo y fichas de amigos.",
            16, INK, false);
        addText(menu, "El logo personal se elige de nuevo en el otro teléfono. Los relojes de concurso siguen su plazo original.",
            14, MUTED, false);
        gap(menu, 8);
        action(row(menu), "Guardar respaldo", true,
            () -> openExportPicker("Tlapohua - progreso.tlaprogreso", "application/octet-stream", EXPORT_PROGRESS));
        action(row(menu), "Restaurar respaldo", false, () -> {
            try {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*"), IMPORT_PROGRESS);
            } catch (ActivityNotFoundException error) {
                Toast.makeText(this, "No hay un selector de archivos disponible.", Toast.LENGTH_LONG).show();
            }
        });
        addText(menu, "El archivo contiene tus datos locales. Guárdalo en un lugar de confianza.", 14, MUTED, false);
    }

    private void showPrivacy() {
        LinearLayout menu = menuPage(Screen.PRIVACY, "Privacidad y datos", "Información de esta versión de prueba · ATR / Tlapohua");
        addText(menu, "Datos en el teléfono", 19, INK, true);
        addText(menu, "Tlapohua guarda partidas, tiempos, notas, rangos, ajustes, el apodo que elijas y las fichas que recibas. "
            + "El logo personal se guarda cuando eliges una imagen. Puedes borrar los datos de la app desde los ajustes de Android.", 16, INK, false);
        gap(menu, 10);
        addText(menu, "Cuando compartes", 19, INK, true);
        addText(menu, "La ficha contiene apodo, identificador de jugador, rango, puntos, tableros resueltos, mejores tiempos y fecha. "
            + "Bluetooth envía esta ficha al teléfono que confirmas. El selector de Android te permite exportar fichas, respaldos, reportes y PDFs "
            + "al proveedor o aplicación que elijas. Esos destinatarios gestionan la copia que reciben.", 16, INK, false);
        gap(menu, 10);
        addText(menu, "Permisos y eliminación", 19, INK, true);
        addText(menu, "El permiso Dispositivos cercanos se solicita para Bluetooth. La selección de documentos e imágenes se realiza con Android. "
            + "Esta versión funciona sin permiso de Internet, anuncios ni analítica. Puedes quitar una ficha desde Liga de amigos "
            + "y quitar el logo manteniendo pulsado el título del menú. Las copias exportadas se eliminan desde el lugar donde las guardaste.", 16, INK, false);
        gap(menu, 10);
        addText(menu, "Antes de publicar", 19, INK, true);
        addText(menu, "El responsable debe incorporar su contacto de privacidad y publicar esta información en una dirección web accesible "
            + "para la ficha de Google Play.", 14, MUTED, false);
    }

    private void transferProgress(android.net.Uri uri, boolean exporting) {
        Map<String, ?> snapshot = exporting ? new LinkedHashMap<>(prefs.getAll()) : null;
        new Thread(() -> {
            try {
                if (exporting) {
                    try (java.io.OutputStream out = getApplicationContext().getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new java.io.IOException("Sin destino.");
                        ProgressBackup.write(out, snapshot);
                    }
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Respaldo guardado.", Toast.LENGTH_LONG).show());
                } else {
                    Map<String, Object> recovered;
                    try (java.io.InputStream in = getApplicationContext().getContentResolver().openInputStream(uri)) {
                        if (in == null) throw new java.io.IOException("Sin archivo.");
                        recovered = ProgressBackup.read(in);
                    }
                    validateBackup(recovered);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed() || screen != Screen.BACKUP) return;
                        new AlertDialog.Builder(this).setTitle("¿Restaurar este progreso?")
                            .setMessage("Se reemplazarán las partidas, el rango, las fichas y los ajustes de este teléfono por los del respaldo. "
                                + "Puedes cancelar y guardar primero una copia de tu progreso actual.")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Restaurar", (dialog, which) -> restoreProgress(recovered)).show();
                    });
                }
            } catch (java.io.IOException | RuntimeException error) {
                android.util.Log.w("Tlapohua", "No se pudo procesar el respaldo", error);
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), exporting ? "No se pudo guardar el respaldo."
                    : "El respaldo no es válido o pertenece a una versión incompatible.", Toast.LENGTH_LONG).show());
            }
        }, "tlapohua-respaldo").start();
    }

    private void validateBackup(Map<String, Object> snapshot) throws java.io.IOException {
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            Class<?> expected = null;
            if (key.endsWith("-accepted") || key.startsWith("collection-board-") || key.startsWith("daily-completed-")
                    || key.equals("collection-imported") || key.equals(CASUAL_FOREST) || key.equals(LARGE_NUMBERS)
                    || key.equals(HIGH_CONTRAST) || key.equals(ZOOM_BOARD) || key.equals(REDUCED_MOTION) || key.equals(SKIP_TOURNAMENT_GUIDE)
                    || key.equals("continue-ranked")) expected = Boolean.class;
            else if (key.endsWith(ELAPSED) || key.startsWith(BEST_TIME)) expected = Long.class;
            else if (key.endsWith("-mistakes") || key.equals("level") || key.equals(RANKED_POINTS)
                    || key.startsWith(RANKED_START_POINTS) || key.startsWith(RANKED_CHANGE) || key.startsWith("practice-stat-")
                    || key.equals("continue-operation")) expected = Integer.class;
            else if (key.endsWith("-values") || key.endsWith("-notes") || key.endsWith(ATTEMPT) || key.endsWith("-score")
                    || key.startsWith(FRIEND_PREFIX) || key.startsWith(CONTEST_HISTORY) || key.startsWith(RANKED_HISTORY)
                    || key.startsWith("daily-board-") || Arrays.asList(FRIEND_ID, FRIEND_NAME, CONTEST_ACTIVE, RANKED_ACTIVE,
                        CONTEST_COOLDOWN, RANKED_COOLDOWN, TIMED_ACTIVE, DAILY_CARVING, "forest-route", "active-board",
                        "continue-mode", "continue-board", "continue-date").contains(key)) expected = String.class;
            if (expected != null && !expected.isInstance(entry.getValue())) throw new java.io.IOException("Tipo incompatible.");
        }
        Object id = snapshot.get(FRIEND_ID);
        if (id != null) {
            try { java.util.UUID.fromString((String) id); }
            catch (IllegalArgumentException invalid) { throw new java.io.IOException("Identificador inválido.", invalid); }
        }
        Object name = snapshot.get(FRIEND_NAME);
        if (name != null && !FriendCard.validName((String) name)) throw new java.io.IOException("Apodo inválido.");
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            if (entry.getValue() instanceof Integer && (Integer) entry.getValue() < 0
                    && !entry.getKey().startsWith(RANKED_CHANGE)) throw new java.io.IOException("Contador inválido.");
            if (entry.getValue() instanceof Long && (Long) entry.getValue() < 0) throw new java.io.IOException("Tiempo inválido.");
        }
        Map<String, Puzzle> pool = new LinkedHashMap<>();
        for (Puzzle entry : PuzzleCatalog.loadContest(this, "concurso.tsv")) pool.put(entry.id, entry);
        for (Puzzle entry : PuzzleCatalog.loadContest(this, "finalistas.tsv")) pool.put(entry.id, entry);
        long elapsedNow = SystemClock.elapsedRealtime(), wallNow = System.currentTimeMillis();
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            if (key.equals(CONTEST_ACTIVE) || key.equals(RANKED_ACTIVE) || key.startsWith(CONTEST_HISTORY) || key.startsWith(RANKED_HISTORY)) {
                ContestRun run = ContestRun.decode((String) entry.getValue(), pool);
                if (run == null) throw new java.io.IOException("Partida incompatible.");
                if (key.equals(CONTEST_ACTIVE) || key.equals(RANKED_ACTIVE)) {
                    run.rebaseClock(elapsedNow, wallNow);
                    entry.setValue(run.encode());
                }
            } else if (key.equals(TIMED_ACTIVE)) {
                TimedPractice run = TimedPractice.decode((String) entry.getValue());
                if (run == null) throw new java.io.IOException("Reto incompatible.");
                entry.setValue(run.rebased(elapsedNow, wallNow).encode());
            }
        }
    }

    private SharedPreferences.Editor replaceProgress(Map<String, ?> snapshot) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, ?> item : snapshot.entrySet()) {
            Object value = item.getValue();
            if (value instanceof String) editor.putString(item.getKey(), (String) value);
            else if (value instanceof Integer) editor.putInt(item.getKey(), (Integer) value);
            else if (value instanceof Long) editor.putLong(item.getKey(), (Long) value);
            else if (value instanceof Boolean) editor.putBoolean(item.getKey(), (Boolean) value);
        }
        return editor;
    }

    private ProgressRestore.Store progressStore() {
        return new ProgressRestore.Store() {
            @Override public Map<String, ?> read() { return prefs.getAll(); }
            @Override public boolean replace(Map<String, ?> values) { return replaceProgress(values).commit(); }
        };
    }

    private void restoreProgress(Map<String, Object> snapshot) {
        ProgressRestore.Store store = progressStore();
        ProgressJournal journal = new ProgressJournal(this);
        try {
            ProgressRestore.restore(store, journal, snapshot);
        } catch (java.io.IOException | RuntimeException error) {
            try {
                ProgressRestore.recover(store, journal);
            } catch (java.io.IOException recoveryError) {
                new AlertDialog.Builder(this).setTitle("Recuperación pendiente")
                    .setMessage("Libera espacio y vuelve a abrir la app para recuperar el progreso anterior.")
                    .setPositiveButton("Cerrar", (dialog, which) -> finish()).setCancelable(false).show();
                return;
            }
            Toast.makeText(this, "La restauración no se completó. Se conservó el progreso anterior.", Toast.LENGTH_LONG).show();
            return;
        }
        stopFriendExchange();
        contestHandler.removeCallbacks(contestTick);
        contestHandler.removeCallbacks(practiceTick);
        journey = new Journey(prefs.getString("forest-route", ""));
        contestBoards.clear();
        contest = null;
        timedPractice = null;
        practiceBoard = practiceExcluded = "";
        mode = Mode.FREE;
        showHome();
        Toast.makeText(this, "Progreso restaurado.", Toast.LENGTH_LONG).show();
    }

    private void addDisplaySwitch(LinearLayout parent, String label, String key) {
        android.widget.Switch toggle = new android.widget.Switch(this);
        toggle.setText(label);
        toggle.setTextColor(INK);
        toggle.setPadding(dp(10), dp(10), dp(10), dp(10));
        toggle.setChecked(prefs.getBoolean(key, false));
        toggle.setOnCheckedChangeListener((view, checked) -> prefs.edit().putBoolean(key, checked).apply());
        parent.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
    }

    private String operationName(char operation) {
        return PracticeInsights.operationName(operation);
    }

    private List<Puzzle> practiceBoards(char operation, int size) {
        List<Puzzle> choices = new ArrayList<>();
        java.util.Set<String> excluded = new java.util.HashSet<>(Arrays.asList(practiceExcluded.split(",")));
        for (Puzzle entry : catalog) {
            if (!entry.family.equals("archivo") && (size == 0 || entry.size == size)
                    && !excluded.contains(entry.id) && entry.hasOperation(operation)) choices.add(entry);
        }
        return choices;
    }

    private void showPractice() {
        LinearLayout menu = menuPage(Screen.PRACTICE, "Practicar por operación",
            "Los tableros incluyen la operación elegida. En restas y divisiones, el número mayor va a la izquierda o arriba.");
        for (int row = 0; row < 2; row++) {
            LinearLayout operations = row(menu);
            for (int slot = 0; slot < 2; slot++) {
                char operation = PRACTICE_OPERATIONS[row * 2 + slot];
                action(operations, operationName(operation), operation == practiceOperation, () -> {
                    practiceOperation = operation;
                    showPractice();
                });
            }
        }
        action(row(menu), practiceSize == 0 ? "Tamaño · Todos" : "Tamaño · " + practiceSize + "×" + practiceSize, false,
            this::choosePracticeSize);
        List<Puzzle> options = practiceBoards(practiceOperation, practiceSize);
        TimedPractice pending = TimedPractice.decode(prefs.getString(TIMED_ACTIVE, ""));
        if (pending != null && findPuzzle(pending.boardId) != null)
            action(row(menu), "Continuar reto cronometrado", true, () -> openTimedPractice(pending));
        else if (!options.isEmpty())
            action(row(menu), "Reto cronometrado de un tablero", true, () -> choosePracticeDuration(options));
        if (options.isEmpty()) {
            addText(menu, "Elige otro tamaño para encontrar tableros con esta operación.", 15, MUTED, false);
            return;
        }
        Puzzle previous = practiceBoard.isEmpty() ? null : findPuzzle(practiceBoard);
        if (previous != null && options.contains(previous) && !prefs.getBoolean("practice-" + previous.id + "-accepted", false)) {
            choiceCard(menu, TlapohuaArt.Mark.GRID, "Continuar práctica", previous.title, () -> openPractice(previous));
        }
        java.util.Collections.shuffle(options);
        java.util.Collections.sort(options, (a, b) -> Boolean.compare(prefs.getBoolean("practice-" + a.id + "-accepted", false),
            prefs.getBoolean("practice-" + b.id + "-accepted", false)));
        int shown = 0;
        for (Puzzle entry : options) {
            if (entry == previous && !prefs.getBoolean("practice-" + entry.id + "-accepted", false)) continue;
            choiceCard(menu, null, entry.title, entry.size + "×" + entry.size + " · " + entry.operationSymbols(), () -> {
                String key = "practice-" + entry.id;
                if (prefs.getBoolean(key + "-accepted", false)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    for (String suffix : new String[]{"-values", "-notes", "-mistakes", "-accepted", ATTEMPT, ELAPSED}) editor.remove(key + suffix);
                    editor.apply();
                }
                openPractice(entry);
            });
            if (++shown == 3) break;
        }
        if (options.size() > 3) action(row(menu), "Ver otros tableros", false, this::showPractice);
    }

    private void choosePracticeSize() {
        String[] sizes = new String[9];
        sizes[0] = "Todos";
        for (int i = 1; i < sizes.length; i++) sizes[i] = (i + 1) + "×" + (i + 1);
        new AlertDialog.Builder(this).setTitle("Tamaño del tablero").setItems(sizes, (dialog, index) -> {
            practiceSize = index == 0 ? 0 : index + 1;
            showPractice();
        }).show();
    }

    private void openPractice(Puzzle entry) {
        practiceBoard = entry.id;
        timedPractice = null;
        mode = Mode.PRACTICE;
        loadBoard(entry);
    }

    private void choosePracticeDuration(List<Puzzle> options) {
        int[] minutes = {5, 10, 15};
        new AlertDialog.Builder(this).setTitle("Tiempo para un tablero")
            .setItems(new String[]{"5 minutos", "10 minutos", "15 minutos"}, (dialog, which) -> {
                Puzzle entry = options.get(new java.util.Random().nextInt(options.size()));
                TimedPractice run = new TimedPractice(entry.id, practiceOperation, minutes[which],
                    SystemClock.elapsedRealtime(), System.currentTimeMillis());
                SharedPreferences.Editor editor = prefs.edit().putString(TIMED_ACTIVE, run.encode());
                String key = "timed-practice-" + entry.id;
                for (String suffix : new String[]{"-values", "-notes", "-mistakes", "-accepted", ATTEMPT, ELAPSED}) editor.remove(key + suffix);
                String count = practiceCountKey("timed-start", entry.size, run.operation);
                editor.putInt(count, prefs.getInt(count, 0) + 1).apply();
                openTimedPractice(run);
            }).show();
    }

    private void openTimedPractice(TimedPractice run) {
        Puzzle entry = findPuzzle(run.boardId);
        if (entry == null) { prefs.edit().remove(TIMED_ACTIVE).apply(); showPractice(); return; }
        timedPractice = run;
        practiceOperation = run.operation;
        mode = Mode.PRACTICE;
        loadBoard(entry);
        updatePracticeCountdown();
    }

    private boolean isTimedPractice() { return mode == Mode.PRACTICE && timedPractice != null; }

    private long practiceRemaining() {
        return timedPractice.remaining(SystemClock.elapsedRealtime(), System.currentTimeMillis());
    }

    private void updatePracticeCountdown() {
        contestHandler.removeCallbacks(practiceTick);
        if (screen != Screen.GAME || !isTimedPractice() || practiceCountdown == null) return;
        if (accepted) {
            practiceCountdown.setText("Resuelto a tiempo");
            return;
        }
        long remaining = practiceRemaining();
        practiceCountdown.setText("Tiempo restante · " + contestTime(remaining + 999));
        if (remaining == 0 && !reviewing && resumed) finishTimedPractice();
        else if (remaining > 0 && resumed) contestHandler.postDelayed(practiceTick, 200);
    }

    private void finishTimedPractice() {
        int correct = 0, total = values.length;
        for (int i = 0; i < total; i++) if (values[i] == puzzle.solution[i]) correct++;
        prefs.edit().remove(TIMED_ACTIVE).apply();
        showPractice();
        timedPractice = null;
        new AlertDialog.Builder(this).setTitle("Se acabó el tiempo")
            .setMessage("Dejaste " + correct + " de " + total + " casillas correctas.")
            .setPositiveButton("Volver a práctica", null).show();
    }

    private boolean timedCanPlay() {
        if (!isTimedPractice() || accepted || reviewing || practiceRemaining() > 0) return true;
        finishTimedPractice();
        return false;
    }

    private String practiceCountKey(String event, int size, char operation) {
        return "practice-stat-" + event + "-" + size + "-" + operation;
    }

    private void showLearningProgress() {
        LinearLayout menu = menuPage(Screen.LEARNING, "Mi aprendizaje",
            "Regiones de tus torneos y simulacros terminados, comparadas con sus soluciones.");
        List<ContestRun> records = allContestRecords();
        List<PracticeInsights.OperationProgress> totals = PracticeInsights.progress(records);
        List<PracticeInsights.OperationProgress> recent = PracticeInsights.progress(records.subList(0, Math.min(3, records.size())));
        List<PracticeInsights.OperationProgress> previous = PracticeInsights.progress(
            records.subList(Math.min(3, records.size()), Math.min(6, records.size())));
        action(row(menu), "Ver mi próxima práctica", true, this::showRecommendedPractice);
        if (records.isEmpty()) addText(menu, "Termina un torneo o simulacro para empezar a ver tu resumen.", 16, MUTED, false);
        for (int i = 0; i < totals.size(); i++) {
            PracticeInsights.OperationProgress total = totals.get(i), now = recent.get(i), before = previous.get(i);
            gap(menu, 9);
            addText(menu, operationName(total.operation), 20, GREEN, true);
            addText(menu, total.correct + " correctas · " + total.errors + " con diferencias · "
                + total.incomplete + " incompletas", 15, INK, false);
            if (now.total() > 0) addText(menu, "Últimas 3 partidas · " + now.correct + "/" + now.total() + " regiones correctas", 14, MUTED, false);
            if (before.total() > 0) addText(menu, "3 partidas anteriores · " + before.correct + "/" + before.total() + " regiones correctas", 14, MUTED, false);
            int drills = 0, timedWins = 0, timedStarts = 0;
            for (int size = 2; size <= 9; size++) {
                drills += prefs.getInt(practiceCountKey("solved", size, total.operation), 0);
                timedWins += prefs.getInt(practiceCountKey("timed-win", size, total.operation), 0);
                timedStarts += prefs.getInt(practiceCountKey("timed-start", size, total.operation), 0);
            }
            addText(menu, drills + " prácticas resueltas · " + timedWins + "/" + timedStarts + " retos a tiempo", 14, MUTED, false);
            char operation = total.operation;
            action(row(menu), "Practicar " + operationName(operation).toLowerCase(Locale.ROOT), false, () -> {
                practiceOperation = operation;
                practiceSize = 0;
                practiceExcluded = "";
                showPractice();
            });
        }
    }

    private void showRecommendedPractice() {
        LinearLayout menu = menuPage(Screen.RECOMMENDED, "Mi próxima práctica",
            "La sugerencia se basa en hasta tus tres torneos o simulacros completos más recientes.");
        List<ContestRun> records = allContestRecords();
        List<PracticeInsights.Focus> recommendations = PracticeInsights.recent(records);
        if (recommendations.isEmpty()) {
            addText(menu, "Cuando entregues respuestas incorrectas en un torneo o simulacro, aquí aparecerán reglas para repasar.",
                16, MUTED, false);
            action(row(menu), "Elegir una operación", true, this::showPractice);
            return;
        }
        java.util.Set<String> recentBoards = new java.util.HashSet<>();
        for (int i = 0; i < Math.min(3, records.size()); i++) {
            ContestRun run = records.get(i);
            for (int round = 0; round <= run.round; round++) {
                int start = round * ContestRun.PER_ROUND;
                for (int index = start; index < start + run.boardCount(round); index++)
                    recentBoards.add(run.boards[index].id);
            }
        }
        String excluded = android.text.TextUtils.join(",", recentBoards);
        for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
            PracticeInsights.Focus focus = recommendations.get(i);
            String size = focus.size + "×" + focus.size;
            int normal = prefs.getInt(practiceCountKey("solved", focus.size, focus.operation), 0);
            int started = prefs.getInt(practiceCountKey("timed-start", focus.size, focus.operation), 0);
            int won = prefs.getInt(practiceCountKey("timed-win", focus.size, focus.operation), 0);
            choiceCard(menu, TlapohuaArt.Mark.GRID, operationName(focus.operation) + " · " + size,
                focus.regions + " regiones con respuestas incorrectas · " + normal + " prácticas resueltas · "
                    + won + "/" + started + " retos a tiempo", () -> {
                        practiceOperation = focus.operation;
                        practiceSize = focus.size;
                        practiceExcluded = excluded;
                        showPractice();
                    });
        }
    }

    private void openContest(boolean ranked) {
        notebookHistory = false;
        selectContestMode(ranked);
        showContest();
    }

    private void selectContestMode(boolean ranked) {
        if (rankedContest != ranked) {
            rankedContest = ranked;
            contestBoards.clear();
            contest = null;
        }
    }

    private void openNotebook(boolean ranked) {
        notebookHistory = true;
        selectContestMode(ranked);
        showContestResults();
    }

    private String contestHistoryKey() { return rankedContest ? RANKED_HISTORY : CONTEST_HISTORY; }

    private int currentRank() { return ContestRun.rankIndex(prefs.getInt(RANKED_POINTS, 0)); }

    private void playTemple(TempleMotion.Scene scene, String title, String detail, Runnable finished) {
        if (!motionEnabled()) {
            finished.run();
            return;
        }
        if (templeMotion != null) templeMotion.dismiss();
        templeMotion = new TempleMotion(this, scene, currentRank(), title, detail, finished);
        templeMotion.show();
    }

    private boolean loadContest() {
        if (!contestBoards.isEmpty()) return true;
        try {
            for (Puzzle entry : PuzzleCatalog.loadContest(this, "concurso.tsv")) contestBoards.put(entry.id, entry);
            if (rankedContest) {
                for (Puzzle entry : PuzzleCatalog.loadContest(this, "finalistas.tsv")) contestBoards.put(entry.id, entry);
            }
            contest = ContestRun.decode(prefs.getString(rankedContest ? RANKED_ACTIVE : CONTEST_ACTIVE, ""), contestBoards);
            contestCooldown = new ContestCooldown(prefs.getString(rankedContest ? RANKED_COOLDOWN : CONTEST_COOLDOWN, ""));
            if (!rankedContest && !prefs.contains(CONTEST_COOLDOWN)) {
                List<ContestRun> records = contestRecords();
                java.util.Collections.reverse(records);
                for (ContestRun record : records) rememberContestRounds(record);
                if (contest != null && !contest.finished()) rememberContestRounds(contest);
                prefs.edit().putString(CONTEST_COOLDOWN, contestCooldown.encode()).apply();
            }
            return true;
        } catch (java.io.IOException | IllegalArgumentException error) {
            contestBoards.clear();
            Toast.makeText(this, "No se pudo abrir el catálogo de concurso.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void saveContest() {
        String saved = contest.encode();
        SharedPreferences.Editor editor = prefs.edit()
            .putString(rankedContest ? RANKED_ACTIVE : CONTEST_ACTIVE, saved)
            .putString(rankedContest ? RANKED_COOLDOWN : CONTEST_COOLDOWN, contestCooldown.encode());
        if (contest.finished()) {
            String record = contestHistoryKey() + contest.id;
            if (rankedContest && !prefs.contains(record)) {
                int previousPoints = prefs.getInt(RANKED_POINTS, 0);
                int change = contest.rankChange(previousPoints);
                editor.putInt(RANKED_START_POINTS + contest.id, previousPoints)
                    .putInt(RANKED_CHANGE + contest.id, change)
                    .putInt(RANKED_POINTS, previousPoints + change);
            }
            editor.putString(record, saved);
        }
        editor.apply();
    }

    private void rememberContestRounds(ContestRun run) {
        for (int round = 0; round < ContestRun.ROUNDS; round++) contestCooldown.record(run, round);
    }

    private void showContest() {
        LinearLayout menu = menuPage(Screen.CONTEST, rankedContest ? RANKED_TITLE : "Entrenamiento de concurso",
            rankedContest ? "Torneo · Modo con rangos"
                : "Cada simulacro tiene tres rondas de tres tableros. El reloj es compartido dentro de cada ronda.");
        if (!loadContest()) return;
        if (closeExpiredContestRound() && rankedContest && contest.finished()) {
            showContestRecord(contest.id, true);
            return;
        }
        if (rankedContest) {
            addRankProgress(menu);
        }
        if (contest != null) {
            choiceCard(menu, TlapohuaArt.Mark.SUN, contest.finished() ? "Ver último resultado"
                : rankedContest ? "Continuar torneo" : "Continuar simulacro",
                contest.label(), contest.finished() ? () -> showContestRecord(contest.id) : this::showContestRound);
        }
        if (rankedContest) {
            choiceCard(menu, TlapohuaArt.Mark.GRID, "Inscribirme", "Elige una división · Rondas de 18, 22 y 25 minutos",
                () -> {
                    if (prefs.getBoolean(SKIP_TOURNAMENT_GUIDE, false)) showContestLevels(false);
                    else showTournamentGuide(0, true);
                });
        } else {
            choiceCard(menu, null, "Estándar", "18 · 22 · 25 minutos", () -> showContestLevels(false));
            choiceCard(menu, null, "Pro", "15 · 20 · 22 minutos", () -> showContestLevels(true));
        }
        action(row(menu), "Historial", false, this::showContestResults);
        action(row(menu), rankedContest ? "Ver recorrido y reglas" : "Reglas", false, this::showContestRules);
    }

    private void showTournamentGuide(int step, boolean enrollment) {
        if (tournamentGuide != null) tournamentGuide.dismiss();
        tournamentGuideStep = Math.max(0, Math.min(3, step));
        tournamentGuideEnrollment = enrollment;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(6));
        View seal = new View(this);
        seal.setBackground(TlapohuaArt.seal(TlapohuaArt.Mark.SUN));
        seal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams sealPosition = new LinearLayout.LayoutParams(dp(44), dp(44));
        sealPosition.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(seal, sealPosition);
        ScrollView scroll = new ScrollView(this);
        TextView explanation = new TextView(this);
        explanation.setTextSize(16);
        explanation.setTextColor(INK);
        explanation.setPadding(0, dp(10), 0, dp(10));
        explanation.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        scroll.addView(explanation);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, dp(205)));
        android.widget.CheckBox skip = new android.widget.CheckBox(this);
        skip.setText("No volver a mostrar al inscribirme");
        skip.setTextColor(INK);
        skip.setChecked(prefs.getBoolean(SKIP_TOURNAMENT_GUIDE, false));
        skip.setOnCheckedChangeListener((view, checked) -> prefs.edit().putBoolean(SKIP_TOURNAMENT_GUIDE, checked).apply());
        content.addView(skip, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(this).setView(content)
            .setPositiveButton("Siguiente", null).setNeutralButton("Anterior", null)
            .setNegativeButton(enrollment ? "Saltar recorrido" : "Cerrar", null).create();
        tournamentGuide = dialog;
        dialog.setOnDismissListener(window -> {
            if (tournamentGuide == dialog) tournamentGuide = null;
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
            tournamentGuideStep--;
            updateTournamentGuide(dialog, explanation, scroll);
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
            dialog.dismiss();
            if (enrollment && screen == Screen.CONTEST) showContestLevels(false);
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (tournamentGuideStep < 3) {
                tournamentGuideStep++;
                updateTournamentGuide(dialog, explanation, scroll);
            } else {
                dialog.dismiss();
                if (enrollment && screen == Screen.CONTEST) showContestLevels(false);
            }
        });
        updateTournamentGuide(dialog, explanation, scroll);
    }

    private void updateTournamentGuide(AlertDialog dialog, TextView explanation, ScrollView scroll) {
        String[] titles = {"Prepara tu torneo", "Reglas del tablero", "Entrega y clasifica", "Haz crecer tu rango"};
        String[] instructions = {
            "Elige una división y juega 3 rondas de 3 tableros.\n\n"
                + "Cada ronda comparte un reloj de " + ContestRun.minutes(false, 0) + ", " + ContestRun.minutes(false, 1)
                + " o " + ContestRun.minutes(false, 2) + " minutos. Puedes cambiar entre sus tableros.\n\n"
                + "Las rondas y la final simulan el formato internacional de 2024. La clasificación y los rangos son reglas de Tlapohua.",
            "Usa del 1 al tamaño del tablero, sin repetir en filas ni columnas.\n\n"
                + "Cada región cumple su pista. Puede repetir números en filas y columnas distintas. Las restas y divisiones aceptan ambos órdenes.\n\n"
                + "Puedes tomar notas y probar provisionales. Confirma estos últimos antes de entregar.",
            "Toca Entregar ronda o espera a que se agote el tiempo. El reloj continúa si sales de la app.\n\n"
                + "Cada tablero correcto vale 1000 puntos. Si completas la ronda, sumas 5 por cada 10 segundos que sobren.\n\n"
                + "Con al menos " + ContestRun.QUALIFY + " de 9 tableros correctos pasas a la final de 8×8, con "
                + ContestRun.FINAL_MINUTES + " minutos.",
            "Si clasificas, ganas " + ContestRun.RANK_BOARD_POINTS + " puntos de rango por tablero clasificatorio correcto y "
                + ContestRun.RANK_FINAL_POINTS + " más si resuelves la final. Se aplican al terminar el torneo.\n\n"
                + rankPenaltyRule() + "\n\nMadera → Cobre → Obsidiana → Jade → Turquesa. Tu progreso queda guardado en este teléfono."
        };
        dialog.setTitle((tournamentGuideStep + 1) + " de 4 · " + titles[tournamentGuideStep]);
        explanation.animate().cancel();
        explanation.setText(instructions[tournamentGuideStep]);
        scroll.scrollTo(0, 0);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(tournamentGuideStep > 0);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(tournamentGuideStep < 3 ? "Siguiente"
            : tournamentGuideEnrollment ? "Elegir división" : "Entendido");
        if (motionEnabled()) {
            explanation.setAlpha(.4f);
            explanation.animate().alpha(1f).setDuration(160).start();
        } else explanation.setAlpha(1f);
    }

    private String rankPenaltyRule() {
        return "Si quedas fuera de la final, pierdes " + ContestRun.RANK_LOSS_PER_MISSING
            + " puntos por cada acierto que te faltó para clasificar, con un máximo de " + ContestRun.RANK_MAX_LOSS
            + " por torneo. Tu saldo mínimo es cero.";
    }

    private void addRankProgress(LinearLayout menu) {
        int points = prefs.getInt(RANKED_POINTS, 0);
        int rank = ContestRun.rankIndex(points);
        LinearLayout banner = row(menu);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        addRankEmblem(banner, rank);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        banner.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        addText(copy, "Rango " + ContestRun.rankName(rank), 19, GREEN, true);
        addText(copy, points + " puntos de rango", 15, INK, false);
        if (ContestRun.hasNextRank(rank)) {
            int next = ContestRun.rankThreshold(rank + 1);
            addText(menu, "Te faltan " + (next - points) + " para llegar a " + ContestRun.rankName(rank + 1), 15, MUTED, false);
            android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(next - ContestRun.rankThreshold(rank));
            bar.setProgress(points - ContestRun.rankThreshold(rank));
            menu.addView(bar, new LinearLayout.LayoutParams(-1, dp(12)));
        } else {
            addText(menu, "Rango máximo alcanzado", 15, MUTED, false);
        }
        gap(menu, 10);
    }

    private View addRankEmblem(LinearLayout row, int rank) {
        View emblem = new View(this);
        emblem.setBackground(TlapohuaArt.rankSeal(rank));
        emblem.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams position = new LinearLayout.LayoutParams(dp(60), dp(60));
        position.rightMargin = dp(12);
        row.addView(emblem, position);
        return emblem;
    }

    private void showContestLevels(boolean pro) {
        if (!loadContest()) { showContest(); return; }
        contestPro = pro;
        LinearLayout menu = menuPage(Screen.CONTEST_SETUP, rankedContest ? "Elige tu división" : pro ? "Simulacro Pro" : "Simulacro estándar",
            rankedContest ? "Resuelve al menos " + ContestRun.QUALIFY + " de 9 tableros para clasificar a la final."
                : pro ? "Rondas de 15, 20 y 22 minutos." : "Rondas de 18, 22 y 25 minutos.");
        String[] detail = {"4×4 y 5×5 · Suma y resta", "4×4 a 6×6 · Cuatro operaciones", "5×5 a 7×7 · Cuatro operaciones"};
        for (int i = 0; i < ContestRun.LEVELS.length; i++) {
            int level = i;
            choiceCard(menu, null, ContestRun.LEVELS[i], detail[i], () -> chooseContest(pro, level));
        }
    }

    private void chooseContest(boolean pro, int level) {
        if (contest != null && !contest.finished()) {
            new AlertDialog.Builder(this).setTitle(rankedContest ? "¿Iniciar otro torneo?" : "¿Iniciar otro simulacro?")
                .setMessage(rankedContest ? "Se descartará el torneo en curso y sus puntos pendientes." : "Se descartará el simulacro en curso.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar otro", (dialog, which) -> newContest(pro, level)).show();
        } else newContest(pro, level);
    }

    private void newContest(boolean pro, int level) {
        closeExpiredContestRound();
        List<Puzzle> boards = new ArrayList<>(contestBoards.values());
        java.util.Random random = new java.util.Random();
        long id = System.currentTimeMillis();
        if (rankedContest) {
            while (prefs.contains(RANKED_HISTORY + id)) id++;
            contest = ContestRun.createRanked(level, id, boards, contestCooldown.postponed(), random);
        } else {
            contest = ContestRun.create(pro, level, id, boards, contestCooldown.postponed(), random);
        }
        saveContest();
        showContestRound();
    }

    private void showContestRules() {
        if (rankedContest) {
            showTournamentGuide(0, false);
            return;
        }
        new AlertDialog.Builder(this).setTitle("Reglas del simulacro")
            .setMessage("Cada fila y columna usa los números del 1 al tamaño del tablero. Un número puede repetirse en la región si está en filas y columnas distintas. "
                + "Las restas y divisiones admiten ambos órdenes.\n\n"
                + "Puedes cambiar de tablero, tomar notas y entregar antes del límite. El reloj sigue corriendo al salir de la app. La corrección aparece al cerrar la ronda.\n\n"
                + "Los tableros que queden sin resolver se reservan hasta que cierres " + ContestCooldown.WAIT_ROUNDS + " rondas más.\n\n"
                + "Cada tablero resuelto vale 1000 puntos de la partida. Si completas la ronda, sumas 5 puntos por cada 10 segundos completos que sobren.")
            .setPositiveButton("Entendido", null).show();
    }

    private void showContestRound() {
        if (!loadContest() || contest == null) { showContest(); return; }
        if (contest.running() && contestRemaining() == 0) { finishContestRound(); return; }
        if (rankedContest && contest.finished()) { showContestRecord(contest.id); return; }
        int round = contest.round;
        LinearLayout menu = menuPage(Screen.CONTEST_ROUND, contest.roundTitle(round),
            contest.label() + " · " + contest.roundMinutes(round) + " minutos");
        if (contest.roundFinished()) {
            showContestRoundResult(menu, contest, round);
            if (contest.finished()) {
                addText(menu, "Simulacro completo · " + contest.score() + " puntos", 20, GREEN, true);
                action(row(menu), "Ver historial", true, this::showContestResults);
            } else {
                boolean nextIsFinal = rankedContest && round == ContestRun.ROUNDS - 1;
                if (nextIsFinal) addText(menu, "¡Clasificaste a la final con " + contest.qualifierSolved() + " de 9 tableros!", 19, GREEN, true);
                action(row(menu), nextIsFinal ? "Preparar final" : "Preparar siguiente ronda", true, () -> {
                    contest.nextRound();
                    saveContest();
                    showContestRound();
                    if (nextIsFinal)
                        playTemple(TempleMotion.Scene.FINAL, contest.roundTitle(contest.round), "Un tablero de 8×8 te espera", () -> {});
                });
            }
        } else {
            for (int i = 0; i < contest.boardCount(round); i++) {
                Puzzle entry = contest.boards[contest.index(i)];
                addText(menu, "Tablero " + (i + 1) + " · " + entry.size + "×" + entry.size, 16, INK, true);
            }
            if (contest.running()) contestClock = addText(menu, "", 17, GREEN, true);
            action(row(menu), contest.running() ? "Volver al tablero" : "Iniciar reloj", true, () -> {
                if (!contest.running() && rankedContest) {
                    ContestRun ready = contest;
                    playTemple(TempleMotion.Scene.COUNTDOWN, contest.roundTitle(round), contest.roundMinutes(round) + " minutos", () -> {
                        if (screen == Screen.CONTEST_ROUND && contest == ready && contest.round == round) startContestClock();
                    });
                } else startContestClock();
            });
        }
        if (!rankedContest) {
            gap(menu, 8);
            LinearLayout print = row(menu);
            action(print, "Guardar cuadernillo", false, () -> chooseContestPdf(false));
            if (!contest.running()) action(print, "Guardar clave", false, () -> chooseContestPdf(true));
        }
        if (contest.running()) updateContestClock();
    }

    private void startContestClock() {
        contest.start(SystemClock.elapsedRealtime(), System.currentTimeMillis());
        openContestBoard(contest.board);
    }

    private void showContestRoundResult(LinearLayout menu, ContestRun run, int round) {
        int count = run.boardCount(round);
        addText(menu, (count == 1 ? (run.solved(round) == 1 ? "Final resuelta" : "Final sin resolver")
            : run.solved(round) + " de " + count + " tableros correctos") + " · " + run.score(round) + " puntos", 18, GREEN, true);
        addText(menu, "Tiempo utilizado · " + contestTime(run.usedTime(round)), 14, MUTED, false);
        for (int i = 0; i < count; i++) {
            int index = round * ContestRun.PER_ROUND + i;
            Puzzle entry = run.boards[index];
            choiceCard(menu, null, "Tablero " + (i + 1) + " · " + entry.size + "×" + entry.size,
                run.correctCells(index) + "/" + entry.cage.length + " casillas correctas",
                () -> showContestCorrection(run, index));
        }
    }

    private void showContestCorrection(ContestRun run, int index) {
        if (run.usedTime(index / ContestRun.PER_ROUND) < 0) return;
        Puzzle entry = run.boards[index];
        int[] submitted = run.values[index];
        ScrollView scroll = new ScrollView(this);
        LinearLayout report = new LinearLayout(this);
        report.setOrientation(LinearLayout.VERTICAL);
        report.setPadding(dp(12), dp(10), dp(12), dp(12));
        scroll.addView(report);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Cuaderno de errores")
            .setView(scroll).setPositiveButton("Cerrar", null).create();
        addText(report, "Tu tablero entregado · " + run.correctCells(index) + "/" + submitted.length + " correctas", 16, INK, true);
        addText(report, "Toca una casilla. × indica una diferencia y — una casilla vacía.", 13, MUTED, false);
        TextView cellDetail = new TextView(this);
        Button practice = new Button(this);
        LinearLayout cells = null;
        int firstError = -1;
        for (int i = 0; i < entry.solution.length; i++) {
            if (i % entry.size == 0) cells = row(report);
            int cell = i, group = entry.cage[i], value = submitted[i];
            boolean wrong = value != entry.solution[i];
            if (wrong && firstError < 0) firstError = i;
            String digit = value == 0 ? "—" : value + (wrong ? "×" : "");
            String clue = entry.first[group] == i ? entry.clue(group) : "";
            TextView tile = new TextView(this);
            SpannableString label = new SpannableString((clue.isEmpty() ? " " : clue) + "\n" + digit);
            label.setSpan(new RelativeSizeSpan(.70f), 0, label.toString().indexOf('\n'), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tile.setText(label);
            tile.setTextSize(13);
            tile.setGravity(Gravity.CENTER);
            tile.setTextColor(wrong ? 0xff802c22 : INK);
            tile.setBackground(reviewTile(entry, cell));
            tile.setContentDescription("Fila " + (i / entry.size + 1) + ", columna " + (i % entry.size + 1)
                + ", " + (value == 0 ? "vacía" : value + (wrong ? ", diferente" : ", correcta")));
            tile.setFocusable(true);
            tile.setOnClickListener(view -> updateCorrection(entry, submitted, cell, cellDetail, practice, dialog));
            cells.addView(tile, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        gap(report, 9);
        cellDetail.setTextSize(15);
        cellDetail.setTextColor(INK);
        cellDetail.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        report.addView(cellDetail, new LinearLayout.LayoutParams(-1, -2));
        practice.setAllCaps(false);
        report.addView(practice, new LinearLayout.LayoutParams(-1, -2));
        updateCorrection(entry, submitted, Math.max(0, firstError), cellDetail, practice, dialog);
        dialog.show();
    }

    private Drawable reviewTile(Puzzle entry, int cell) {
        int group = entry.cage[cell], column = cell % entry.size, row = cell / entry.size;
        int edge = dp(2);
        return new android.graphics.drawable.LayerDrawable(new Drawable[]{
            new ColorDrawable(INK),
            new android.graphics.drawable.InsetDrawable(new ColorDrawable(CAGE_COLORS[group % CAGE_COLORS.length]),
                column == 0 || entry.cage[cell - 1] != group ? edge : 0,
                row == 0 || entry.cage[cell - entry.size] != group ? edge : 0,
                column == entry.size - 1 ? edge : 0,
                row == entry.size - 1 ? edge : 0)
        });
    }

    private void updateCorrection(Puzzle entry, int[] submitted, int cell, TextView detail, Button practice, AlertDialog dialog) {
        int value = submitted[cell];
        detail.setText("Fila " + (cell / entry.size + 1) + " · Columna " + (cell % entry.size + 1)
            + "\nTu respuesta · " + (value == 0 ? "Vacía" : value) + "    Solución · " + entry.solution[cell]
            + "\n" + entry.reviewCell(submitted, cell));
        char operation = entry.operationAt(cell);
        practice.setVisibility(operation == '=' ? View.GONE : View.VISIBLE);
        practice.setText("Practicar " + operationName(operation).toLowerCase(Locale.ROOT) + " · " + entry.size + "×" + entry.size);
        practice.setOnClickListener(view -> {
            dialog.dismiss();
            practiceOperation = operation;
            practiceSize = entry.size;
            practiceExcluded = entry.id;
            showPractice();
        });
    }

    private void showContestResults() {
        LinearLayout menu = menuPage(Screen.CONTEST_RESULTS, notebookHistory ? "Cuaderno de errores"
                : rankedContest ? "Historial de torneos" : "Historial de simulacros",
            rankedContest ? "Tus resultados y puntos de rango." : "Resultados guardados de las tres rondas.");
        if (!loadContest()) return;
        if (rankedContest) addRankProgress(menu);
        List<ContestRun> records = contestRecords();
        if (records.isEmpty()) addText(menu, rankedContest ? "Termina tu primer torneo para guardar el resultado."
            : "Completa las tres rondas para guardar tu primer resultado.", 15, MUTED, false);
        for (ContestRun run : records) {
            String date = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                .format(new java.util.Date(run.id));
            choiceCard(menu, null, run.label() + " · " + run.score() + " puntos",
                rankedContest ? date + " · " + signedPoints(savedRankChange(run)) + " de rango" : date, () -> showContestRecord(run.id));
        }
    }

    private List<ContestRun> contestRecords() {
        return contestRecords(rankedContest);
    }

    private List<ContestRun> contestRecords(boolean ranked) {
        List<ContestRun> records = new ArrayList<>();
        String history = ranked ? RANKED_HISTORY : CONTEST_HISTORY;
        for (Map.Entry<String, ?> item : prefs.getAll().entrySet()) {
            if (!item.getKey().startsWith(history) || !(item.getValue() instanceof String)) continue;
            ContestRun run = ContestRun.decode((String) item.getValue(), contestBoards);
            if (run != null && run.finished()) records.add(run);
        }
        java.util.Collections.sort(records, (a, b) -> Long.compare(b.id, a.id));
        return records;
    }

    private void showPersonalRecords() {
        LinearLayout menu = menuPage(Screen.RECORDS, "Marcas personales", "Tu progreso guardado en este teléfono.");
        addText(menu, "Cuaderno de errores", 20, INK, true);
        action(row(menu), "Revisar torneos", true, () -> openNotebook(true));
        action(row(menu), "Revisar simulacros", false, () -> openNotebook(false));
        action(row(menu), "Exportar reporte para el maestro", false, () -> {
            if (allContestRecords().isEmpty()) {
                Toast.makeText(this, "Completa un torneo o simulacro para exportar el reporte.", Toast.LENGTH_LONG).show();
            } else openExportPicker("Tlapohua - reporte de estudio.csv", "text/csv", SAVE_STUDY_REPORT);
        });
        gap(menu, 12);
        addText(menu, "Mejor tiempo por tamaño", 20, INK, true);
        addText(menu, "Cuenta las victorias nuevas de Juego libre, Ruta del bosque y Reto diario.", 14, MUTED, false);
        for (int size = 2; size <= 9; size++) {
            long best = prefs.getLong(BEST_TIME + size, 0);
            addText(menu, size + "×" + size + " · " + (best == 0 ? "Sin marca" : contestTime(best)), 16, INK, true);
        }
        gap(menu, 12);
        selectContestMode(true);
        if (!loadContest()) return;
        addContestMarks(menu, "Cumbre de Turquesa", contestRecords(true), true);
        addContestMarks(menu, "Entrenamiento", contestRecords(false), false);
    }

    private List<ContestRun> allContestRecords() {
        selectContestMode(true);
        if (!loadContest()) return java.util.Collections.emptyList();
        List<ContestRun> records = contestRecords(true);
        records.addAll(contestRecords(false));
        java.util.Collections.sort(records, (a, b) -> Long.compare(b.id, a.id));
        return records;
    }

    private java.util.UUID localFriendId() {
        String stored = prefs.getString(FRIEND_ID, "");
        if (!stored.isEmpty()) return java.util.UUID.fromString(stored);
        java.util.UUID id = java.util.UUID.randomUUID();
        prefs.edit().putString(FRIEND_ID, id.toString()).apply();
        return id;
    }

    private FriendCard myFriendCard() {
        String name = prefs.getString(FRIEND_NAME, "");
        if (name.isEmpty()) return null;
        long[] times = new long[8];
        for (int size = 2; size <= 9; size++) times[size - 2] = prefs.getLong(BEST_TIME + size, 0);
        int rankedSolved = 0;
        selectContestMode(true);
        if (loadContest()) {
            for (ContestRun run : contestRecords(true))
                for (int round = 0; round <= run.round; round++) rankedSolved += run.solved(round);
        }
        return new FriendCard(localFriendId(), name, System.currentTimeMillis(), prefs.getInt(RANKED_POINTS, 0),
            collectedBoards().size(), rankedSolved, times);
    }

    private FriendCard savedFriend(String key) {
        try {
            return FriendCard.decode(android.util.Base64.decode(prefs.getString(key, ""), android.util.Base64.NO_WRAP));
        } catch (java.io.IOException | IllegalArgumentException invalid) {
            return null;
        }
    }

    private void showFriends() {
        LinearLayout menu = menuPage(Screen.FRIENDS, "Liga de amigos",
            "Compara fichas guardadas en este teléfono. Intercambia otra ficha para actualizar los datos.");
        FriendCard own = myFriendCard();
        action(row(menu), own == null ? "Elegir mi apodo" : "Mi apodo · " + own.name, true, this::editFriendName);
        action(row(menu), "Exportar mi ficha", false, () -> {
            if (requireFriendProfile()) openExportPicker("Mi ficha Tlapohua.tlapohua", "application/octet-stream", EXPORT_FRIEND);
        });
        action(row(menu), "Importar ficha", false, () -> {
            try {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*"), IMPORT_FRIEND);
            } catch (ActivityNotFoundException error) {
                Toast.makeText(this, "No hay un selector de archivos disponible.", Toast.LENGTH_LONG).show();
            }
        });
        action(row(menu), "Intercambiar por Bluetooth", false, this::openFriendBluetooth);
        gap(menu, 10);
        List<FriendCard> cards = new ArrayList<>();
        if (own != null) cards.add(own);
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(FRIEND_PREFIX)) continue;
            FriendCard card = savedFriend(key);
            if (card != null) cards.add(card);
        }
        java.util.Collections.sort(cards, (left, right) -> {
            int points = Integer.compare(right.points, left.points);
            return points != 0 ? points : left.name.compareToIgnoreCase(right.name);
        });
        if (cards.isEmpty()) addText(menu, "Importa una ficha o intercámbiala con un amigo cercano.", 15, MUTED, false);
        int position = 0;
        for (FriendCard card : cards) {
            boolean isMe = own != null && card.id.equals(own.id);
            choiceCard(menu, TlapohuaArt.Mark.SUN, (++position) + ". " + card.name + (isMe ? " · Tú" : ""),
                friendRank(card) + " · " + card.points + " puntos · " + card.rankedSolved + " tableros de Cumbre"
                    + (isMe ? "" : "\nFicha del " + friendDate(card.updatedAt)), () -> showFriendDetail(card, isMe));
        }
    }

    private String friendRank(FriendCard card) { return ContestRun.rankName(ContestRun.rankIndex(card.points)); }

    private String friendDate(long timestamp) {
        return java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
            .format(new java.util.Date(timestamp));
    }

    private void showFriendDetail(FriendCard card, boolean isMe) {
        StringBuilder detail = new StringBuilder(friendRank(card)).append(" · ").append(card.points).append(" puntos de rango")
            .append("\nTableros en Mi colección · ").append(card.collected)
            .append("\nTableros resueltos en Cumbre · ").append(card.rankedSolved)
            .append("\nFicha del ").append(friendDate(card.updatedAt)).append("\n\nMejores tiempos por tamaño");
        for (int i = 0; i < card.times.length; i++) {
            int size = i + 2;
            detail.append('\n').append(size).append('×').append(size).append(" · ")
                .append(card.times[i] == 0 ? "Sin marca" : contestTime(card.times[i]));
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle(card.name).setMessage(detail)
            .setPositiveButton("Cerrar", null);
        if (!isMe) dialog.setNegativeButton("Quitar ficha", (window, which) -> {
            prefs.edit().remove(FRIEND_PREFIX + card.id).apply();
            showFriends();
        });
        dialog.show();
    }

    private boolean requireFriendProfile() {
        if (prefs.contains(FRIEND_NAME)) return true;
        editFriendName();
        return false;
    }

    private void editFriendName() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine();
        input.setHint("Apodo");
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        input.setText(prefs.getString(FRIEND_NAME, ""));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Tu apodo en la liga").setView(input)
            .setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String name = input.getText().toString().trim();
            if (!FriendCard.validName(name)) { input.setError("Escribe un apodo de hasta 24 caracteres."); return; }
            localFriendId();
            prefs.edit().putString(FRIEND_NAME, name).apply();
            dialog.dismiss();
            showFriends();
        });
    }

    private void acceptFriendCard(FriendCard card) {
        String message;
        String key = FRIEND_PREFIX + card.id;
        if (card.id.equals(localFriendId())) message = "Esta ficha es tuya.";
        else if (!card.newerThan(savedFriend(key))) message = "Ya tienes esta ficha o una más reciente.";
        else {
            try {
                prefs.edit().putString(key, android.util.Base64.encodeToString(card.encode(), android.util.Base64.NO_WRAP)).apply();
                message = "Ficha guardada · " + card.name;
            } catch (java.io.IOException invalid) {
                message = "No se pudo guardar la ficha.";
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (screen == Screen.FRIENDS) showFriends();
    }

    private void openFriendBluetooth() {
        if (!requireFriendProfile()) return;
        if (android.os.Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{BLUETOOTH_CONNECT}, FRIENDS_BLUETOOTH_PERMISSION);
            return;
        }
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) {
            Toast.makeText(this, "Este dispositivo no tiene Bluetooth disponible.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!adapter.isEnabled()) {
            new AlertDialog.Builder(this).setTitle("Activa Bluetooth")
                .setMessage("Enciéndelo en los ajustes de Android y vuelve a Liga de amigos.")
                .setNegativeButton("Cerrar", null).setPositiveButton("Abrir ajustes", (dialog, which) -> openBluetoothSettings()).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Intercambiar por Bluetooth")
            .setMessage("Vinculen los teléfonos una vez en los ajustes de Android. Luego uno toca «Esperar a mi amigo» y el otro «Elegir teléfono».")
            .setNeutralButton("Vincular teléfonos", (dialog, which) -> openBluetoothSettings())
            .setNegativeButton("Esperar a mi amigo", (dialog, which) -> startFriendExchange(adapter, null))
            .setPositiveButton("Elegir teléfono", (dialog, which) -> chooseFriendDevice(adapter)).show();
    }

    private void openBluetoothSettings() {
        try { startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)); }
        catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Abre los ajustes de Bluetooth de Android.", Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != FRIENDS_BLUETOOTH_PERMISSION || screen != Screen.FRIENDS) return;
        if (grants.length > 0 && grants[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) openFriendBluetooth();
        else Toast.makeText(this, "Puedes intercambiar fichas por archivo o habilitar Dispositivos cercanos en Ajustes.",
            Toast.LENGTH_LONG).show();
    }

    private void chooseFriendDevice(BluetoothAdapter adapter) {
        List<BluetoothDevice> devices = new ArrayList<>(adapter.getBondedDevices());
        if (devices.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Vincula el teléfono de tu amigo")
                .setMessage("Después de vincularlo en Android, vuelve aquí y elige su teléfono.")
                .setNegativeButton("Cerrar", null).setPositiveButton("Abrir ajustes", (dialog, which) -> openBluetoothSettings()).show();
            return;
        }
        String[] labels = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            BluetoothDevice device = devices.get(i);
            String name = device.getName();
            labels[i] = (name == null || name.isEmpty() ? "Dispositivo Bluetooth" : name) + " · " + device.getAddress().substring(12);
        }
        new AlertDialog.Builder(this).setTitle("Elige el teléfono de tu amigo")
            .setItems(labels, (dialog, which) -> startFriendExchange(adapter, devices.get(which))).show();
    }

    private void startFriendExchange(BluetoothAdapter adapter, BluetoothDevice device) {
        FriendCard own = myFriendCard();
        if (own == null) return;
        stopFriendExchange();
        friendExchange = new BluetoothFriends();
        showFriendWait(device == null ? "Pide a tu amigo que elija tu teléfono." : "Conectando con el teléfono elegido…");
        contestHandler.postDelayed(friendTimeout, 65_000);
        friendExchange.start(adapter, device, own, new BluetoothFriends.Listener() {
            @Override public void confirm(BluetoothFriends exchange, String deviceName, int code) {
                runOnUiThread(() -> {
                    if (friendExchange != exchange) return;
                    friendDialog.dismiss();
                    friendDialog = new AlertDialog.Builder(MainActivity.this).setTitle("Confirma el código " + code)
                        .setMessage((deviceName == null ? "Teléfono vinculado" : deviceName)
                            + "\nSi ambos ven el mismo código, acepten para compartir sus fichas.")
                        .setNegativeButton("Cancelar", (dialog, which) -> stopFriendExchange())
                        .setPositiveButton("Coincide", (dialog, which) -> {
                            exchange.approve(true);
                            showFriendWait("Esperando confirmación del otro teléfono…");
                        }).setOnCancelListener(dialog -> stopFriendExchange()).create();
                    friendDialog.show();
                });
            }

            @Override public void completed(BluetoothFriends exchange, FriendCard received) {
                runOnUiThread(() -> {
                    if (friendExchange != exchange) return;
                    stopFriendExchange();
                    acceptFriendCard(received);
                });
            }

            @Override public void failed(BluetoothFriends exchange) {
                runOnUiThread(() -> {
                    if (friendExchange != exchange) return;
                    stopFriendExchange();
                    Toast.makeText(MainActivity.this, "No se completó el intercambio. Verifica que tu amigo esté esperando.",
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showFriendWait(String message) {
        friendDialog = new AlertDialog.Builder(this).setTitle("Liga de amigos").setMessage(message)
            .setNegativeButton("Cancelar", (dialog, which) -> stopFriendExchange())
            .setOnCancelListener(dialog -> stopFriendExchange()).create();
        friendDialog.show();
    }

    private void stopFriendExchange() {
        contestHandler.removeCallbacks(friendTimeout);
        if (friendExchange != null) {
            friendExchange.close();
            friendExchange = null;
        }
        if (friendDialog != null) {
            friendDialog.dismiss();
            friendDialog = null;
        }
    }

    private void transferFriendFile(android.net.Uri uri, boolean exporting) {
        FriendCard own = exporting ? myFriendCard() : null;
        if (exporting && own == null) return;
        new Thread(() -> {
            try {
                if (exporting) {
                    try (java.io.OutputStream out = getApplicationContext().getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new java.io.IOException("Sin destino.");
                        out.write(own.encode());
                    }
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        new AlertDialog.Builder(this).setTitle("Ficha exportada")
                            .setMessage("Puedes enviarla ahora o compartir el archivo después desde Archivos.")
                            .setNegativeButton("Cerrar", null)
                            .setPositiveButton("Compartir ficha", (dialog, which) -> shareFriendFile(uri)).show();
                    });
                } else {
                    FriendCard card;
                    try (java.io.InputStream in = getApplicationContext().getContentResolver().openInputStream(uri)) {
                        if (in == null) throw new java.io.IOException("Sin archivo.");
                        card = FriendCard.read(in);
                    }
                    runOnUiThread(() -> acceptFriendCard(card));
                }
            } catch (java.io.IOException | RuntimeException error) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                    exporting ? "No se pudo guardar la ficha." : "El archivo no es una ficha válida de Tlapohua.",
                    Toast.LENGTH_LONG).show());
            }
        }, "tlapohua-ficha").start();
    }

    private void shareFriendFile(android.net.Uri uri) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("application/octet-stream")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(android.content.ClipData.newRawUri("Ficha Tlapohua", uri));
        try { startActivity(Intent.createChooser(send, "Compartir ficha de Tlapohua")); }
        catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "Comparte la ficha guardada desde Archivos.", Toast.LENGTH_LONG).show();
        }
    }

    private void addContestMarks(LinearLayout menu, String label, List<ContestRun> records, boolean ranked) {
        addText(menu, label, 20, INK, true);
        int finals = ranked && contest != null && !contest.finished() && contest.qualified() ? 1 : 0;
        if (records.isEmpty()) {
            addText(menu, "Tus resultados aparecerán al terminar una partida.", 14, MUTED, false);
        } else {
            int[] rounds = new int[ContestRun.ROUNDS];
            int best = 0;
            for (ContestRun run : records) {
                best = Math.max(best, run.qualifierSolved());
                if (run.qualified()) finals++;
                for (int i = 0; i < rounds.length; i++) rounds[i] = Math.max(rounds[i], run.solved(i));
            }
            addText(menu, "Mejor clasificatoria · " + best + "/9", 16, GREEN, true);
            addText(menu, "Récord por ronda · " + rounds[0] + "/3  ·  " + rounds[1] + "/3  ·  " + rounds[2] + "/3", 14, INK, false);
        }
        if (ranked) addText(menu, "Clasificaciones a la final · " + finals, 14, INK, false);
        gap(menu, 10);
    }

    private void showContestRecord(long id) {
        showContestRecord(id, false);
    }

    private void showContestRecord(long id, boolean animateRank) {
        ContestRun record = loadContest() ? ContestRun.decode(prefs.getString(contestHistoryKey() + id, ""), contestBoards) : null;
        if (record == null || !record.finished()) { showContestResults(); return; }
        contestRecordId = id;
        LinearLayout menu = menuPage(Screen.CONTEST_RECORD, rankedContest ? "Resultado del torneo" : "Resultado del simulacro",
            rankedContest ? record.label() : record.label() + " · " + record.score() + " puntos");
        if (rankedContest) addRankedResult(menu, record, animateRank);
        for (int round = 0; round <= record.round; round++) {
            gap(menu, 10);
            addText(menu, record.roundTitle(round), 21, INK, true);
            showContestRoundResult(menu, record, round);
        }
    }

    private int savedRankChange(ContestRun run) {
        return prefs.getInt(RANKED_CHANGE + run.id, run.rankPoints());
    }

    private String signedPoints(int points) {
        if (points == 0) return "0";
        return points > 0 ? "+" + points : "−" + (-points);
    }

    private void addRankedResult(LinearLayout menu, ContestRun run, boolean animateRank) {
        int change = savedRankChange(run);
        boolean finalSolved = run.round == ContestRun.ROUNDS && run.solved(run.round) == 1;
        String outcome = finalSolved ? "¡Final resuelta!" : run.qualified() ? "Final sin resolver" : "Fuera de la final";
        addText(menu, outcome, 20, INK, true);
        addText(menu, "Marcador del torneo · " + run.score() + " puntos", 18, INK, true);
        addText(menu, run.qualifierSolved() + " de 9 tableros clasificatorios resueltos", 15, MUTED, false);
        int earlierBest = -1;
        for (ContestRun earlier : contestRecords()) {
            if (earlier.id < run.id && earlier.level == run.level) earlierBest = Math.max(earlierBest, earlier.qualifierSolved());
        }
        if (earlierBest >= 0) {
            String label = run.qualifierSolved() > earlierBest ? "¡Nueva marca en tu división!" : "Mejor marca anterior en esta división";
            addText(menu, label + " · " + earlierBest + "/9", 14, MUTED, false);
        }

        int color = change < 0 ? 0xff8d3327 : GREEN;
        addText(menu, signedPoints(change) + " puntos de rango", 19, color, true);
        int previous = prefs.getInt(RANKED_START_POINTS + run.id, -1);
        if (previous < 0) return;

        int total = previous + change;
        int from = ContestRun.rankIndex(previous), to = ContestRun.rankIndex(total);
        TextView points = addText(menu, "", 24, color, true);
        LinearLayout banner = row(menu);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        boolean animate = animateRank && change != 0 && motionEnabled();
        int initialRank = animate ? from : to;
        View emblem = addRankEmblem(banner, initialRank);
        TextView rank = addText(banner, "Rango " + ContestRun.rankName(initialRank), 20, INK, true);
        android.widget.ProgressBar progress = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        menu.addView(progress, new LinearLayout.LayoutParams(-1, dp(12)));
        setRankBar(progress, animate ? previous : total);
        if (from != to) {
            addText(menu, (to > from ? "Ascenso" : "Descenso") + " · " + ContestRun.rankName(from)
                + " → " + ContestRun.rankName(to), 15, MUTED, false);
        }
        if (!animate) {
            points.setText(total + " puntos acumulados");
            return;
        }
        points.setText(previous + " puntos acumulados");
        int[] shownRank = {from};
        rankAnimation = ValueAnimator.ofInt(previous, total);
        rankAnimation.setDuration(1200);
        rankAnimation.addUpdateListener(animation -> {
            int current = (int) animation.getAnimatedValue();
            points.setText(current + " puntos acumulados");
            setRankBar(progress, current);
            int material = ContestRun.rankIndex(current);
            String label = "Rango " + ContestRun.rankName(material);
            if (shownRank[0] != material) {
                rank.setText(label);
                emblem.animate().cancel();
                TransitionDrawable texture = new TransitionDrawable(new Drawable[]{
                    TlapohuaArt.rankSeal(shownRank[0]), TlapohuaArt.rankSeal(material)});
                texture.setCrossFadeEnabled(true);
                emblem.setBackground(texture);
                texture.startTransition(310);
                shownRank[0] = material;
                emblem.setScaleX(change > 0 ? 1.24f : .82f);
                emblem.setScaleY(emblem.getScaleX());
                emblem.animate().scaleX(1f).scaleY(1f).setDuration(360).start();
            }
        });
        rankAnimation.start();
    }

    private void setRankBar(android.widget.ProgressBar bar, int points) {
        int rank = ContestRun.rankIndex(points);
        int base = ContestRun.rankThreshold(rank);
        int span = ContestRun.hasNextRank(rank) ? ContestRun.rankThreshold(rank + 1) - base : 1;
        bar.setMax(span);
        bar.setProgress(ContestRun.hasNextRank(rank) ? points - base : span);
    }

    private void openContestBoard(int index) {
        if (!contestCanPlay()) return;
        pauseClock();
        mode = Mode.CONTEST;
        contest.board = index;
        puzzle = contest.boards[contest.index(index)];
        values = contest.values[contest.index(index)];
        notes = contest.notes[contest.index(index)];
        clockKey = null;
        attempt = null;
        accepted = wasSolved = reviewing = pencil = provisionalMode = false;
        undo.clear();
        selected = 0;
        while (selected < values.length - 1 && values[selected] != 0) selected++;
        saveContest();
        showGame();
        refresh(null);
        updateContestClock();
    }

    private boolean contestCanPlay() {
        if (!contest.running()) return false;
        if (contestRemaining() > 0) return true;
        finishContestRound();
        return false;
    }

    private long contestRemaining() {
        return contest.remaining(SystemClock.elapsedRealtime(), System.currentTimeMillis());
    }

    private void updateContestClock() {
        contestHandler.removeCallbacks(contestTick);
        boolean showingRound = screen == Screen.CONTEST_ROUND || (screen == Screen.GAME && mode == Mode.CONTEST);
        if (!showingRound || contestClock == null || contest == null || !contestCanPlay()) return;
        contestClock.setText("Tiempo restante · " + contestTime(contestRemaining() + 999));
        if (resumed) contestHandler.postDelayed(contestTick, 200);
    }

    private String contestTime(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void confirmContestRound() {
        if (!contestCanPlay()) return;
        boolean isFinal = contest.round == ContestRun.ROUNDS;
        contestConfirmation = new AlertDialog.Builder(this).setTitle(isFinal ? "¿Entregar la final?" : "¿Entregar la ronda?")
            .setMessage((isFinal ? "Se corregirá el tablero y se detendrá el reloj." : "Se corregirán los tres tableros y se detendrá el reloj.")
                + "\nLos números provisionales se consideran casillas vacías. Confírmalos antes de entregar.")
            .setNegativeButton("Seguir jugando", null)
            .setPositiveButton("Entregar", (dialog, which) -> finishContestRound()).create();
        contestConfirmation.setOnDismissListener(dialog -> contestConfirmation = null);
        contestConfirmation.show();
    }

    private void finishContestRound() {
        if (!closeContestRound()) return;
        if (rankedContest && contest.finished()) showContestRecord(contest.id, true);
        else showContestRound();
    }

    private boolean closeExpiredContestRound() {
        return contest != null && contest.running() && contestRemaining() == 0 && closeContestRound();
    }

    private boolean closeContestRound() {
        if (!contest.running()) return false;
        contest.finishRound(SystemClock.elapsedRealtime(), System.currentTimeMillis());
        contestCooldown.record(contest, contest.round);
        if (contestConfirmation != null) contestConfirmation.dismiss();
        saveContest();
        return true;
    }

    private void showCollection() {
        List<Puzzle> collected = collectedBoards();
        LinearLayout content = menuPage(Screen.COLLECTION, "Mi colección", collected.isEmpty()
            ? "Resuelve un tablero completo para desbloquear tu primera página para imprimir."
            : collected.size() + (collected.size() == 1 ? " tablero desbloqueado." : " tableros desbloqueados."));
        if (collected.isEmpty()) return;
        LinearLayout actions = row(content);
        action(actions, "Vista previa", true, () -> showPdfPreview(0));
        action(actions, "Guardar PDF", false, this::choosePdfLocation);
        gap(content, 10);
        addText(content, "Cada tablero sale en una hoja con sus pistas y casillas en blanco.", 14, MUTED, false);
        for (int i = 0; i < collected.size(); i++) {
            int page = i;
            Puzzle entry = collected.get(i);
            choiceCard(content, null, entry.title, entry.size + " × " + entry.size + "  ·  Página " + (i + 1),
                () -> showPdfPreview(page));
        }
    }

    private void showPdfPreview(int page) {
        List<Puzzle> collected = collectedBoards();
        if (collected.isEmpty()) {
            showCollection();
            return;
        }
        previewPage = Math.max(0, Math.min(page, collected.size() - 1));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(TlapohuaArt.paper(density(), false));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(8), dp(14), dp(8));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = row(content);
        action(actions, "‹  Mi colección", false, () -> goBack(this::showCollection));
        action(actions, "Guardar PDF", true, this::choosePdfLocation);
        gap(content, 6);
        addText(content, "Vista previa", 22, INK, true);
        TextView counter = addText(content, "", 14, MUTED, false);
        counter.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        CollectionPdf.Preview preview = new CollectionPdf.Preview(this, collected);
        content.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout pages = row(content);
        Button previous = action(pages, "‹  Anterior", false, null);
        Button next = action(pages, "Siguiente  ›", false, null);
        Runnable displayPage = () -> {
            preview.showPage(previewPage);
            counter.setText("Página " + (previewPage + 1) + " de " + collected.size() + "  ·  Tamaño carta");
            previous.setEnabled(previewPage > 0);
            next.setEnabled(previewPage < collected.size() - 1);
            previous.setAlpha(previous.isEnabled() ? 1f : .5f);
            next.setAlpha(next.isEnabled() ? 1f : .5f);
        };
        previous.setOnClickListener(view -> { previewPage--; displayPage.run(); });
        next.setOnClickListener(view -> { previewPage++; displayPage.run(); });
        displayPage.run();
        present(root, Screen.PDF_PREVIEW);
    }

    private void choosePdfLocation() {
        openPdfPicker("Tlapohua - Mi coleccion.pdf", SAVE_COLLECTION);
    }

    private void chooseContestPdf(boolean answers) {
        if (contest == null || (answers && contest.running())) return;
        contestPdfState = contest.encode();
        openPdfPicker("Tlapohua - " + (answers ? "Clave" : "Cuadernillo") + " - Ronda " + (contest.round + 1) + ".pdf",
            answers ? SAVE_CONTEST_KEY : SAVE_CONTEST);
    }

    private void openPdfPicker(String filename, int request) {
        openExportPicker(filename, "application/pdf", request);
    }

    private void openExportPicker(String filename, String mime, int request) {
        Intent picker = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mime).putExtra(Intent.EXTRA_TITLE, filename);
        try {
            startActivityForResult(picker, request);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No hay un selector de archivos disponible.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == EXPORT_PROGRESS || request == IMPORT_PROGRESS) {
            transferProgress(data.getData(), request == EXPORT_PROGRESS);
            return;
        }
        if (request == EXPORT_FRIEND || request == IMPORT_FRIEND) {
            transferFriendFile(data.getData(), request == EXPORT_FRIEND);
            return;
        }
        if (request == CHOOSE_INTRO_LOGO) {
            saveIntroLogo(data.getData());
            return;
        }
        if (request == SAVE_STUDY_REPORT) {
            saveStudyReport(data.getData());
            return;
        }
        List<Puzzle> printable;
        String label;
        if (request == SAVE_COLLECTION) {
            printable = collectedBoards();
            label = "";
        } else if (request == SAVE_CONTEST || request == SAVE_CONTEST_KEY) {
            if (!loadContest()) return;
            ContestRun run = ContestRun.decode(contestPdfState, contestBoards);
            contestPdfState = "";
            if (run == null) return;
            printable = Arrays.asList(Arrays.copyOfRange(run.boards, run.index(0), run.index(0) + ContestRun.PER_ROUND));
            label = run.label() + " · Ronda " + (run.round + 1) + " · " + ContestRun.minutes(run.pro, run.round) + " min";
        } else return;
        if (printable.isEmpty()) return;
        android.net.Uri destination = data.getData();
        new Thread(() -> {
            String message;
            try {
                CollectionPdf.write(getApplicationContext().getContentResolver(), destination, printable, label, request == SAVE_CONTEST_KEY);
                message = "PDF guardado.";
            } catch (java.io.IOException | RuntimeException error) {
                android.util.Log.w("Tlapohua", "No se pudo guardar el PDF", error);
                message = "No se pudo guardar el PDF. Prueba en otra carpeta.";
            }
            String feedback = message;
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), feedback, Toast.LENGTH_LONG).show());
        }, "tlapohua-pdf").start();
    }

    private void saveStudyReport(android.net.Uri destination) {
        List<ContestRun> records = allContestRecords();
        if (records.isEmpty()) return;
        new Thread(() -> {
            String message;
            try (java.io.OutputStream stream = getApplicationContext().getContentResolver().openOutputStream(destination, "wt")) {
                if (stream == null) throw new java.io.IOException("No se pudo abrir el destino.");
                stream.write(PracticeInsights.report(records).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                message = "Reporte CSV guardado.";
            } catch (java.io.IOException | RuntimeException error) {
                android.util.Log.w("Tlapohua", "No se pudo guardar el reporte", error);
                message = "No se pudo guardar el reporte.";
            }
            String feedback = message;
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), feedback, Toast.LENGTH_LONG).show());
        }, "tlapohua-reporte").start();
    }

    private void chooseIntroLogo() {
        if (savingIntroLogo) return;
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*"), CHOOSE_INTRO_LOGO);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No hay un selector de imágenes disponible.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveIntroLogo(android.net.Uri uri) {
        savingIntroLogo = true;
        new Thread(() -> {
            String message;
            try {
                IntroLogo.save(getApplicationContext(), uri);
                message = "Logo guardado para la próxima apertura.";
            } catch (java.io.IOException | RuntimeException error) {
                android.util.Log.w("Tlapohua", "No se pudo importar el logo", error);
                message = "No se pudo abrir esa imagen. Prueba con un PNG o JPG.";
            }
            String feedback = message;
            runOnUiThread(() -> {
                savingIntroLogo = false;
                Toast.makeText(getApplicationContext(), feedback, Toast.LENGTH_LONG).show();
            });
        }, "tlapohua-logo").start();
    }

    private void showIntroSettings() {
        if (savingIntroLogo) return;
        boolean hasCustomLogo = IntroLogo.exists(this);
        AlertDialog.Builder settings = new AlertDialog.Builder(this).setTitle("Logo de inicio")
            .setMessage("La firma ATR aparece después de Tlapohua. Puedes elegir otra imagen para esa pantalla en este teléfono.")
            .setPositiveButton("Elegir imagen", (dialog, which) -> chooseIntroLogo())
            .setNeutralButton("Ver presentación", (dialog, which) -> showIntro());
        if (hasCustomLogo) settings.setNegativeButton("Restaurar ATR", (dialog, which) -> {
            IntroLogo.remove(this);
            Toast.makeText(this, "Firma ATR restaurada.", Toast.LENGTH_SHORT).show();
        });
        settings.show();
    }

    private LinearLayout menuPage(Screen destination, String title, String subtitle) {
        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        root.setBackground(TlapohuaArt.paper(density(), false));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(20));
        root.addView(content);
        TextView brand = addMasthead(content, false);
        if (destination == Screen.HOME) {
            brand.setContentDescription("Tlapohua. Mantén pulsado para configurar el logo de inicio.");
            brand.setOnLongClickListener(view -> {
                showIntroSettings();
                return true;
            });
            addFrieze(content);
        }
        gap(content, 12);
        if (destination != Screen.HOME) {
            action(row(content), "‹  Volver", false, this::onBackPressed);
            gap(content, 12);
        }
        addText(content, title, 24, INK, true);
        addText(content, subtitle, 14, MUTED, false);
        gap(content, 7);
        present(root, destination);
        return content;
    }

    private void choiceCard(LinearLayout choices, TlapohuaArt.Mark mark, String title, String detail, Runnable open) {
        gap(choices, 8);
        LinearLayout card = row(choices);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setMinimumHeight(dp(54));
        card.setPadding(dp(12), dp(9), dp(12), dp(9));
        card.setBackground(background(mark == TlapohuaArt.Mark.TRAIL ? 0xffdce9d7 : 0xfff1e5cb, 14));
        card.setFocusable(true);
        addPressMotion(card);
        card.setContentDescription(detail.isEmpty() ? title : title + ". " + detail);
        card.setOnClickListener(view -> open.run());
        if (mark != null) {
            View icon = new View(this);
            icon.setBackground(TlapohuaArt.seal(mark));
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams position = new LinearLayout.LayoutParams(dp(44), dp(44));
            position.rightMargin = dp(10);
            card.addView(icon, position);
        }
        LinearLayout label = new LinearLayout(this);
        label.setOrientation(LinearLayout.VERTICAL);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        card.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        addText(label, title, 16, INK, true);
        if (!detail.isEmpty()) addText(label, detail, 13, MUTED, false);
        addText(card, " ›", 25, GREEN, true).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void chooseLesson() {
        LinearLayout choices = menuPage(Screen.LESSONS, "Tutorial por casos", "Elige la regla que quieres practicar.");
        for (int i = 0; i < Lesson.ALL.length; i++) {
            int index = i;
            choiceCard(choices, null, (i + 1) + " · " + Lesson.ALL[i].title, "", () -> openLesson(index));
        }
    }

    private void openLesson(int index) {
        lessonIndex = Math.max(0, Math.min(Lesson.ALL.length - 1, index));
        Puzzle example = findPuzzle(Lesson.ALL[lessonIndex].puzzleId);
        if (example == null) return;
        mode = Mode.LESSON;
        loadBoard(example);
        board.post(() -> {
            if (screen == Screen.GAME && mode == Mode.LESSON && puzzle == example && !isFinishing()) explainLesson();
        });
    }

    private void explainLesson() {
        Lesson lesson = Lesson.ALL[lessonIndex];
        new AlertDialog.Builder(this).setTitle("Lección " + (lessonIndex + 1) + " · " + lesson.title)
            .setMessage(lesson.instruction).setPositiveButton("Entendido", null).show();
    }

    private Calendar restoredDailyDate(Bundle state, String field) {
        Calendar saved = DailyDates.parse(state.getString(field, ""));
        return saved == null ? DailyDates.today() : saved;
    }

    private boolean dailyCompleted(String date) {
        if (prefs.getBoolean(dailyCompletedKey(date), false)) return true;
        Puzzle assigned = findPuzzle(prefs.getString("daily-board-" + date, ""));
        if (assigned == null) return false;
        String key = dailyStorageKey(date, assigned.id);
        if (prefs.contains(key + "-accepted")) return prefs.getBoolean(key + "-accepted", false);
        return encode(assigned.solution).equals(prefs.getString(key + "-values", ""));
    }

    private void showDailyCalendar(Calendar reference) {
        Calendar today = DailyDates.today();
        Calendar todayMonth = DailyDates.month(today);
        Calendar month = DailyDates.month(reference);
        if (month.after(todayMonth)) month = todayMonth;
        dailyMonth = month;
        LinearLayout content = menuPage(Screen.DAILY_CALENDAR, "Reto diario",
            "Abre una fecha para jugar, continuar o ver el tablero que completaste.");
        action(row(content), dailyCompleted(DailyDates.key(today)) ? "Ver reto de hoy" : "Jugar hoy", true,
            () -> openDaily(today));
        gap(content, 10);
        LinearLayout navigation = row(content);
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = action(navigation, "‹", false, () -> changeDailyMonth(-1));
        previous.setContentDescription("Mes anterior");
        previous.setEnabled(month.get(Calendar.YEAR) > 1 || month.get(Calendar.MONTH) > Calendar.JANUARY);
        TextView title = addText(navigation, DailyDates.label(month, "MMMM yyyy"), 18, INK, true);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 4));
        Button next = action(navigation, "›", false, () -> changeDailyMonth(1));
        next.setContentDescription("Mes siguiente");
        next.setEnabled(month.before(todayMonth));
        next.setAlpha(next.isEnabled() ? 1f : .5f);
        gap(content, 8);
        LinearLayout slab = new LinearLayout(this);
        slab.setOrientation(LinearLayout.VERTICAL);
        slab.setBackground(TlapohuaArt.stone(0xffa6997e, density(), false));
        slab.setPadding(dp(4), dp(4), dp(4), dp(5));
        content.addView(slab, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout headings = row(slab);
        for (String weekday : new String[]{"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"}) {
            TextView text = addText(headings, weekday, 12, INK, true);
            text.setGravity(Gravity.CENTER);
            text.setLayoutParams(new LinearLayout.LayoutParams(0, dp(24), 1));
        }
        int days = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int first = DailyDates.leadingDays(month);
        for (int index = 0; index < first + days; ) {
            LinearLayout week = row(slab);
            for (int column = 0; column < 7; column++, index++) {
                int day = index - first + 1;
                if (day < 1 || day > days) week.addView(new View(this), dailySlot());
                else addDailyCell(week, month, day, today);
            }
        }
        gap(content, 6);
        addText(content, "✓ Grabado al completar   ·   • En curso", 13, MUTED, false);
    }

    private void addDailyCell(LinearLayout week, Calendar month, int day, Calendar today) {
        Calendar date = (Calendar) month.clone();
        date.set(Calendar.DAY_OF_MONTH, day);
        String key = DailyDates.key(date);
        boolean available = !date.after(today);
        boolean done = available && dailyCompleted(key);
        boolean started = available && !done && prefs.contains("daily-board-" + key);
        boolean current = date.equals(today);
        int color = done ? 0xffaeb7a2 : current ? TlapohuaArt.TERRACOTTA : 0xffccc0a5;
        Button cell = new Button(this);
        cell.setAllCaps(false);
        cell.setText(day + (started ? " •" : ""));
        cell.setTextSize(13);
        cell.setTextColor(current && !done ? WHITE : available ? INK : MUTED);
        cell.setPadding(0, 0, 0, done ? dp(11) : 0);
        cell.setMinimumWidth(0);
        cell.setMinWidth(0);
        cell.setBackground(TlapohuaArt.stone(color, density(), available));
        cell.setEnabled(available);
        cell.setAlpha(available ? 1f : .45f);
        addPressMotion(cell);
        cell.setContentDescription(DailyDates.label(date, "d 'de' MMMM 'de' yyyy") + (current ? ", hoy" : "")
            + (done ? ", completado" : started ? ", en curso" : available ? ", pendiente" : ", aún no disponible"));
        cell.setOnClickListener(view -> openDaily(date));
        week.addView(cell, dailySlot());
        if (done) addCalendarCarving(cell, key);
    }

    private void addCalendarCarving(Button cell, String key) {
        TlapohuaArt.Carving carving = new TlapohuaArt.Carving();
        cell.setForeground(carving);
        if (!key.equals(prefs.getString(DAILY_CARVING, ""))) return;
        if (!motionEnabled()) {
            prefs.edit().remove(DAILY_CARVING).apply();
            return;
        }
        carving.setProgress(0f);
        cell.post(() -> {
            if (!cell.isAttachedToWindow() || screen != Screen.DAILY_CALENDAR) return;
            prefs.edit().remove(DAILY_CARVING).apply();
            ValueAnimator cutting = ValueAnimator.ofFloat(0f, 1f);
            cutting.setDuration(1050);
            cutting.addUpdateListener(frame -> carving.setProgress((float) frame.getAnimatedValue()));
            cutting.start();
        });
    }

    private LinearLayout.LayoutParams dailySlot() {
        LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, dp(48), 1);
        slot.setMargins(dp(1), dp(1), dp(1), dp(1));
        return slot;
    }

    private void changeDailyMonth(int amount) {
        Calendar month = (Calendar) dailyMonth.clone();
        month.add(Calendar.MONTH, amount);
        if (amount < 0) goBack(() -> showDailyCalendar(month));
        else showDailyCalendar(month);
    }

    private void backToDailyCalendar() {
        goBack(() -> showDailyCalendar(dailyMonth));
    }

    private void openDaily(Calendar date) {
        if (date.after(DailyDates.today()) || dailyPool.isEmpty()) {
            showDailyCalendar(DailyDates.today());
            return;
        }
        dailyDate = DailyDates.key(date);
        dailyMonth = DailyDates.month(date);
        Puzzle daily = findPuzzle(prefs.getString("daily-board-" + dailyDate, ""));
        if (daily == null) {
            daily = DailyDates.boardFor(date, dailyPool);
            prefs.edit().putString("daily-board-" + dailyDate, daily.id).apply();
        }
        mode = Mode.DAILY;
        loadBoard(daily);
    }

    private void showJourney() {
        mode = Mode.FOREST;
        ScrollView root = new ScrollView(this);
        root.setBackground(TlapohuaArt.paper(density(), true));
        LinearLayout map = new LinearLayout(this);
        map.setOrientation(LinearLayout.VERTICAL);
        map.setPadding(dp(16), dp(12), dp(16), dp(20));
        root.addView(map);
        addMasthead(map, true);
        gap(map, 12);
        addText(map, "Ruta del bosque", 23, INK, true);
        addText(map, "Siete niveles de 3×3 a 9×9. Elige un mapa y reúne victorias para avanzar.", 14, MUTED, false);
        gap(map, 8);
        LinearLayout menu = row(map);
        action(menu, "Modos de juego", false, () -> goBack(this::showPlay));
        action(menu, "Reglas de la ruta", false, () -> journeyRules());
        android.widget.Switch casual = new android.widget.Switch(this);
        casual.setText("Ruta casual · Conservar victorias del nivel");
        casual.setTextColor(INK);
        casual.setChecked(prefs.getBoolean(CASUAL_FOREST, false));
        casual.setOnCheckedChangeListener((view, checked) -> prefs.edit().putBoolean(CASUAL_FOREST, checked).apply());
        map.addView(casual, new LinearLayout.LayoutParams(-1, -2));
        gap(map, 14);
        for (int stage = 0; stage < Journey.SIZES.length; stage++) {
            if (stage > 0) {
                View connector = new View(this);
                connector.setBackgroundColor(0xff9b7753);
                LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(dp(3), dp(13));
                line.leftMargin = dp(19);
                map.addView(connector, line);
            }
            boolean done = journey.finished || stage < journey.stage;
            boolean current = !journey.finished && stage == journey.stage;
            LinearLayout node = row(map);
            node.setGravity(Gravity.CENTER_VERTICAL);
            TextView marker = new TextView(this);
            marker.setText(done ? "✓" : String.valueOf(stage + 1));
            marker.setTextSize(16);
            marker.setTypeface(Typeface.DEFAULT_BOLD);
            marker.setGravity(Gravity.CENTER);
            marker.setTextColor(current || done ? WHITE : MUTED);
            marker.setBackground(TlapohuaArt.tile(current ? TlapohuaArt.TERRACOTTA
                : done ? TlapohuaArt.JADE : SOFT, density(), 15, false));
            node.addView(marker, new LinearLayout.LayoutParams(dp(40), dp(40)));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), dp(7), dp(8), dp(7));
            labels.setBackground(TlapohuaArt.tile(current ? 0xfff0d4a8 : 0xffefe6cd, density(), 10, false));
            LinearLayout.LayoutParams labelPosition = new LinearLayout.LayoutParams(0, -2, 1);
            labelPosition.leftMargin = dp(8);
            node.addView(labels, labelPosition);
            addText(labels, Journey.PLACES[stage], 16, INK, true);
            String state = done ? "Resuelto" : current ? "Estás aquí" : "Más adelante";
            addText(labels, Journey.SIZES[stage] + "×" + Journey.SIZES[stage] + " · " + state, 13, MUTED, false);
            if (current) addJourneyBranches(map);
        }
        if (journey.finished) {
            gap(map, 12);
            addText(map, "¡Llegaste a la Cumbre de jade!", 20, GREEN, true);
            action(row(map), "Iniciar otra ruta", true, () -> confirmNewJourney());
        }
        present(root, Screen.ROUTE);
    }

    private void addJourneyBranches(LinearLayout map) {
        Puzzle chosen = findPuzzle(journey.active);
        LinearLayout paths = new LinearLayout(this);
        paths.setOrientation(LinearLayout.VERTICAL);
        paths.setPadding(dp(12), dp(8), dp(12), dp(10));
        paths.setBackground(TlapohuaArt.tile(0xffd5e3cd, density(), 14, false));
        LinearLayout.LayoutParams branchPosition = new LinearLayout.LayoutParams(-1, -2);
        branchPosition.leftMargin = dp(50);
        branchPosition.topMargin = dp(5);
        map.addView(paths, branchPosition);
        if (chosen != null && prefs.getBoolean(forestKey(journey.stage, chosen.id) + "-accepted", false)) {
            recordForestWin();
            addText(paths, routeProgress(), 14, GREEN, true);
            action(row(paths), advanceLabel(), true, this::advanceForest);
            return;
        }
        addText(paths, routeProgress(), 14, GREEN, true);
        List<Puzzle> options = chosen == null ? journey.choices(catalog, forestBoards) : java.util.Collections.singletonList(chosen);
        addText(paths, chosen == null ? "Elige tu próximo mapa" : "Tu mapa en curso", 14, MUTED, false);
        for (Puzzle option : options) {
            gap(paths, 5);
            action(row(paths), (chosen == null ? "↳ " : "Continuar · ") + option.title + "\n" + option.operationSymbols(),
                true, () -> openForestBoard(option));
        }
    }

    private void journeyRules() {
        boolean casual = prefs.getBoolean(CASUAL_FOREST, false);
        new AlertDialog.Builder(this).setTitle("Reglas de la ruta")
            .setMessage("Para pasar de nivel, gana 3 mapas seguidos sin errores o reúne 5 victorias. Una entrega incorrecta corta la racha.\n\n"
                + (casual ? "En Ruta casual, el tercer error reinicia el tablero y conserva las victorias del nivel."
                    : "En la ruta normal, el tercer error reinicia las victorias del nivel actual.")
                + "\n\nLos mapas ya ganados quedan fuera de las nuevas opciones. Reiniciar un tablero a mano conserva los errores del nivel.")
            .setPositiveButton("Entendido", null).show();
    }

    private String routeProgress() {
        return "Victorias " + journey.wins + "/" + Journey.TOTAL_WINS + "  ·  Racha limpia " + journey.streak
            + "/" + Journey.CLEAN_WINS + "\nErrores " + journey.mistakes + "/2 permitidos";
    }

    private void recordForestWin() {
        if (journey.recordWin()) prefs.edit().putString("forest-route", journey.encode()).apply();
    }

    private void openForestBoard(Puzzle option) {
        if (journey.finished || option.size != Journey.SIZES[journey.stage]) return;
        if (!journey.active.isEmpty() && !journey.active.equals(option.id)) return;
        journey.active = option.id;
        prefs.edit().putString("forest-route", journey.encode()).apply();
        mode = Mode.FOREST;
        loadBoard(option);
    }

    private void confirmNewJourney() {
        new AlertDialog.Builder(this).setTitle("Iniciar otra ruta")
            .setMessage("Empezarás en la Entrada del bosque con mapas nuevos para tu ruta.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Iniciar", (dialog, which) -> {
                SharedPreferences.Editor editor = prefs.edit();
                for (String key : prefs.getAll().keySet()) if (key.startsWith("forest-")) editor.remove(key);
                journey.restart();
                editor.putString("forest-route", journey.encode()).apply();
                showJourney();
            }).show();
    }

    private void submitBoard() {
        if (attempt == null || accepted || reviewing || !timedCanPlay()) return;
        for (int value : values) {
            if (value == 0) { refresh("Completa las casillas y confirma tus números provisionales antes de entregar."); return; }
        }
        reviewing = true;
        pauseClock();
        int[] submitted = values.clone();
        BoardAttempt.Review review = attempt.review(values, puzzle.solution);
        accepted = review.solved;
        undo.clear();
        pencil = false;
        SharedPreferences.Editor editor = prefs.edit();
        boolean returnToRoute = !accepted && mode == Mode.FOREST && failRoute(editor);
        String result;
        if (accepted) {
            if (mode == Mode.FOREST) journey.recordWin();
            if (mode != Mode.PRACTICE) {
                long time = clockTime(), previous = prefs.getLong(BEST_TIME + puzzle.size, 0);
                if (time > 0 && (previous == 0 || time < previous)) editor.putLong(BEST_TIME + puzzle.size, time);
            }
            if (mode == Mode.PRACTICE) {
                String count = practiceCountKey(isTimedPractice() ? "timed-win" : "solved", puzzle.size, practiceOperation);
                editor.putInt(count, prefs.getInt(count, 0) + 1);
                if (isTimedPractice()) {
                    elapsed = timedPractice.minutes * 60_000L - practiceRemaining();
                    editor.remove(TIMED_ACTIVE);
                }
            }
            result = "Mapa completo · " + attempt.total() + " puntos";
        } else if (returnToRoute) {
            result = prefs.getBoolean(CASUAL_FOREST, false) ? "Tercer error. Conservas las victorias del nivel y puedes elegir otro tablero."
                : "Tercer error. Se reinicia el nivel «" + Journey.PLACES[journey.stage] + "».";
        } else {
            if (mode == Mode.FOREST) mistakes = journey.mistakes;
            else mistakes++;
            if (mistakes >= Journey.ATTEMPTS) {
                result = "Sin intentos. El tablero se reiniciará con tres intentos nuevos.";
                mistakes = 0;
                resetBoard();
            } else {
                selected = review.frames.get(review.frames.size() - 1).cell;
                result = "Intentos restantes · " + (Journey.ATTEMPTS - mistakes) + ". Se conservan las jugadas anteriores al error.";
            }
        }
        if (!accepted && !review.frames.isEmpty()) {
            int cell = review.frames.get(review.frames.size() - 1).cell;
            result += "\n\nFila " + (cell / puzzle.size + 1) + " · Columna " + (cell % puzzle.size + 1)
                + "\n" + puzzle.reviewCell(submitted, cell);
        }
        if (!returnToRoute) save(editor);
        editor.apply();
        BoardReplay.show(this, puzzle, review, result, !isTimedPractice() && motionEnabled(), () -> afterBoardReplay(returnToRoute));
    }

    private void afterBoardReplay(boolean returnToRoute) {
        if (mode == Mode.FOREST && (accepted || returnToRoute) && motionEnabled()) {
            forestTravel = new ForestTravel(this, returnToRoute, () -> {
                forestTravel = null;
                finishBoardReview(returnToRoute);
            });
            forestTravel.show();
            return;
        }
        finishBoardReview(returnToRoute);
    }

    private void finishBoardReview(boolean returnToRoute) {
        reviewing = false;
        if (returnToRoute) showJourney();
        else {
            refresh(null);
            updatePracticeCountdown();
            if (accepted) showVictory();
        }
    }

    private boolean failRoute(SharedPreferences.Editor editor) {
        String key = storageKey();
        if (!journey.fail(prefs.getBoolean(CASUAL_FOREST, false))) return false;
        clearForestBoard(editor, key);
        editor.putString("forest-route", journey.encode());
        return true;
    }

    private void clearForestBoard(SharedPreferences.Editor editor, String key) {
        editor.remove(key + "-values").remove(key + "-notes").remove(key + "-accepted")
            .remove(key + "-last-rejected").remove(key + ELAPSED).remove(key + ATTEMPT).remove(key + "-score");
    }

    private void applyInsets(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
    }

    private void present(View root, Screen destination) {
        if (rankAnimation != null) {
            rankAnimation.cancel();
            rankAnimation = null;
        }
        if (destination != Screen.GAME) {
            contestHandler.removeCallbacks(contestTick);
            contestHandler.removeCallbacks(practiceTick);
            pauseClock();
            clock = null;
        }
        boolean animate = visiblePage != null && motionEnabled();
        if (visiblePage != null) visiblePage.animate().cancel();
        visiblePage = root;
        screen = destination;
        applyInsets(root);
        if (animate) {
            root.setAlpha(.72f);
            root.setTranslationX(dp(returning ? -14 : 14));
        }
        setContentView(root);
        root.requestApplyInsets();
        if (animate) {
            root.animate().alpha(1f).translationX(0).setDuration(210)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void goBack(Runnable showPage) {
        returning = true;
        try {
            showPage.run();
        } finally {
            returning = false;
        }
    }

    private void showGame() {
        rememberCurrentGame();
        clock = null;
        routeStatus = null;
        contestClock = null;
        practiceCountdown = null;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(TlapohuaArt.paper(density(), mode == Mode.FOREST));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(8));
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.addView(content);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        addMasthead(content, mode == Mode.FOREST);
        gap(content, 8);
        if (mode == Mode.LESSON) {
            addText(content, "Lección " + (lessonIndex + 1) + " · " + Lesson.ALL[lessonIndex].title, 19, INK, true);
            addText(content, "Practica en la casilla resaltada.", 14, MUTED, false);
        } else if (mode == Mode.CONTEST) {
            int count = contest.boardCount(contest.round);
            addText(content, contest.roundTitle(contest.round) + (count == 1 ? " · 8×8" : " · Tablero " + (contest.board + 1)), 19, INK, true);
            addText(content, contest.label(), 14, MUTED, false);
            if (count > 1) {
                LinearLayout switcher = row(content);
                for (int i = 0; i < count; i++) {
                    int index = i, size = contest.boards[contest.index(i)].size;
                    action(switcher, (i + 1) + " · " + size + "×" + size, i == contest.board, () -> {
                        if (contest.board != index) openContestBoard(index);
                    });
                }
            }
        } else {
            addText(content, puzzle.title, 19, INK, true);
            String context = mode == Mode.DAILY ? "Reto diario · " + dailyDate
                : mode == Mode.PRACTICE ? (isTimedPractice() ? "Reto cronometrado · " : "Práctica · ") + operationName(practiceOperation)
                : mode == Mode.FOREST ? Journey.PLACES[journey.stage] : familyName(puzzle.family);
            addText(content, puzzle.size + " × " + puzzle.size + "  ·  " + context, 14, MUTED, false);
            clock = new Chronometer(this);
            clock.setFormat("Tiempo · %s");
            clock.setTextSize(14);
            clock.setTextColor(GREEN);
            clock.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            LinearLayout info = row(content);
            info.setGravity(Gravity.CENTER_VERTICAL);
            info.addView(clock, new LinearLayout.LayoutParams(0, -2, 1));
            TextView operations = addText(info, puzzle.operationSymbols(), 15, GREEN, true);
            operations.setPadding(dp(8), dp(2), dp(8), dp(2));
            operations.setBackground(TlapohuaArt.tile(TlapohuaArt.SAND, density(), 10, false));
            if (mode == Mode.FOREST) routeStatus = addText(content, routeProgress(), 13, MUTED, false);
        }
        gap(content, 11);
        LinearLayout menu = row(content);
        if (mode == Mode.CONTEST) action(menu, "Ver ronda", false, () -> goBack(this::showContestRound));
        else if (mode == Mode.PRACTICE) action(menu, "Práctica", false, () -> goBack(this::showPractice));
        else action(menu, "Inicio", false, () -> goBack(this::showHome));
        if (mode == Mode.FOREST) action(menu, "Ver ruta", false, () -> goBack(this::showJourney));
        else if (mode == Mode.LESSON) action(menu, "Ver explicación", false, () -> explainLesson());
        else if (mode != Mode.CONTEST) {
            if (mode == Mode.DAILY) action(menu, "Calendario", false, this::backToDailyCalendar);
            action(menu, "Cómo jugar", false, this::rules);
        }
        LinearLayout finishActions = row(content);
        nextBoardButton = action(finishActions, "Siguiente tablero", true, () -> advanceAfterWin());
        nextBoardButton.setVisibility(View.GONE);
        String submitLabel = mode != Mode.CONTEST ? "Entregar tablero"
            : contest.round == ContestRun.ROUNDS ? "Entregar final" : "Entregar ronda";
        submitButton = action(finishActions, submitLabel, false,
            mode == Mode.CONTEST ? this::confirmContestRound : this::submitBoard);
        submitButton.setVisibility(View.GONE);
        gap(content, 14);
        LinearLayout stoneFrame = new LinearLayout(this);
        stoneFrame.setOrientation(LinearLayout.VERTICAL);
        stoneFrame.setPadding(dp(5), dp(5), dp(5), dp(5));
        stoneFrame.setBackground(TlapohuaArt.tile(0xff927353, density(), 10, false));
        content.addView(stoneFrame, new LinearLayout.LayoutParams(-1, -2));
        board = new Board();
        android.widget.HorizontalScrollView boardScroll = new android.widget.HorizontalScrollView(this);
        boardScroll.setFillViewport(true);
        boardScroll.setHorizontalScrollBarEnabled(true);
        boardScroll.addView(board, new ViewGroup.LayoutParams(-2, -2));
        stoneFrame.addView(boardScroll, new LinearLayout.LayoutParams(-1, -2));
        addFrieze(stoneFrame);
        gap(content, 10);
        if (mode == Mode.LESSON) {
            action(row(content), "Repetir lección", false, () -> openLesson(lessonIndex));
        } else if (mode != Mode.CONTEST && !isTimedPractice()) {
            LinearLayout help = row(content);
            action(help, "Pista", false, () -> hint());
            restartButton = action(help, "Reiniciar", false, this::restart);
        }
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(16), dp(6), dp(16), dp(8));
        GradientDrawable ledge = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xffead9b8, 0xfff7ebd4});
        ledge.setStroke(dp(1), 0xffc0a279);
        footer.setBackground(ledge);
        footer.setElevation(dp(4));
        ScrollView controls = new ScrollView(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int limit = dp(getResources().getConfiguration().screenHeightDp * .48f);
                if (MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED)
                    limit = Math.min(limit, MeasureSpec.getSize(heightSpec));
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST));
            }
        };
        controls.setContentDescription("Teclado y herramientas de la partida");
        controls.addView(footer, new ViewGroup.LayoutParams(-1, -2));
        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));
        if (mode == Mode.CONTEST) {
            contestClock = addText(footer, "", 17, GREEN, true);
            contestClock.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        }
        if (isTimedPractice()) practiceCountdown = addText(footer, accepted ? "Resuelto a tiempo" : "", 17, GREEN, true);
        status = addText(footer, "", 14, MUTED, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        gap(footer, 6);
        LinearLayout keypad = row(footer);
        digitButtons = new Button[puzzle.size + 1];
        int availableDp = getResources().getConfiguration().screenWidthDp - 32;
        int keysPerRow = Math.max(2, (availableDp + 5) / 53);
        int firstRowSize = Math.min(keysPerRow, puzzle.size <= 6 ? puzzle.size : (puzzle.size + 1) / 2);
        for (int number = 1; number <= puzzle.size; number++) {
            if (number > 1 && (number - 1) % firstRowSize == 0) {
                gap(footer, 5);
                keypad = row(footer);
            }
            final int value = number;
            Button digit = action(keypad, String.valueOf(number), true, () -> enter(value));
            digit.setTextSize(prefs.getBoolean(LARGE_NUMBERS, false) ? 23 : 20);
            digit.setMinHeight(dp(52));
            digitButtons[number] = digit;
        }
        gap(footer, 5);
        editRow = row(footer);
        noteButton = action(editRow, "Notas", false, () -> {
            pencil = !pencil;
            provisionalMode = false;
            refresh(pencil ? "Modo notas. Toca los números que quieras anotar." : null);
        });
        action(editRow, "Borrar", false, () -> change(0, 0));
        undoButton = action(editRow, "Deshacer", false, () -> undoMove());
        provisionalRow = row(footer);
        provisionalButton = action(provisionalRow, "Provisional", false, () -> {
            provisionalMode = !provisionalMode;
            pencil = false;
            refresh(provisionalMode ? "Número provisional azul. Confírmalo cuando estés seguro." : null);
        });
        confirmProvisionalButton = action(provisionalRow, "Confirmar número", false, () -> {
            int number = notes[selected] >> puzzle.size;
            if (number == 0) return;
            provisionalMode = pencil = false;
            enter(number);
        });
        if (mode == Mode.LESSON) action(provisionalRow, "Borrar", false, () -> change(0, 0));
        present(root, Screen.GAME);
    }

    private TextView addMasthead(LinearLayout content, boolean forest) {
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(dp(14), dp(9), dp(9), dp(9));
        GradientDrawable stone = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff173c35, 0xff25564a});
        stone.setCornerRadius(dp(16));
        stone.setStroke(dp(1), 0xffba925c);
        heading.setBackground(stone);
        content.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        addText(title, "LÓGICA Y NÚMEROS", 11, TlapohuaArt.GOLD, true);
        TextView brand = addText(title, "Tlapohua", 30, TlapohuaArt.CREAM, true);
        View landscape = new View(this);
        landscape.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        landscape.setBackground(TlapohuaArt.scene(forest));
        heading.addView(landscape, new LinearLayout.LayoutParams(dp(112), dp(73)));
        return brand;
    }

    private void addFrieze(LinearLayout content) {
        View ornament = new View(this);
        ornament.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ornament.setBackground(TlapohuaArt.frieze());
        LinearLayout.LayoutParams space = new LinearLayout.LayoutParams(-1, dp(12));
        space.topMargin = dp(4);
        content.addView(ornament, space);
    }

    private void chooseLevel() {
        LinearLayout choices = menuPage(Screen.ZONES, "Juego libre", "Elige una zona para explorar sus tableros.");
        if (prefs.contains("active-board") || prefs.contains("level")) {
            choiceCard(choices, TlapohuaArt.Mark.GRID, "Continuar partida libre", "Abre el último tablero que jugaste.", this::openFree);
        }
        for (String family : new String[]{"patio", "plaza", "senderos"}) {
            choiceCard(choices, null, familyName(family), familyRange(family), () -> chooseSize(family));
        }
        if (prefs.contains("senderos-values")) {
            choiceCard(choices, null, familyName("archivo"), "", () -> chooseSize("archivo"));
        }
    }

    private void chooseSize(String family) {
        selectedFamily = family;
        Map<Integer, List<Puzzle>> bySize = new TreeMap<>();
        for (Puzzle option : catalog) {
            if (!option.family.equals(family)) continue;
            List<Puzzle> group = bySize.get(option.size);
            if (group == null) {
                group = new ArrayList<>();
                bySize.put(option.size, group);
            }
            group.add(option);
        }
        LinearLayout choices = menuPage(Screen.SIZES, familyName(family), "Escoge el tamaño del tablero.");
        for (Map.Entry<Integer, List<Puzzle>> entry : bySize.entrySet()) {
            int size = entry.getKey();
            List<Puzzle> group = entry.getValue();
            String detail = group.size() == 1 ? group.get(0).title : group.size() + " tableros";
            choiceCard(choices, null, size + " × " + size, detail, () -> {
                if (group.size() == 1) openLevel(group.get(0));
                else chooseBoard(family, size);
            });
        }
    }

    private void chooseBoard(String family, int size) {
        List<Puzzle> boards = new ArrayList<>();
        for (Puzzle option : catalog) {
            if (option.family.equals(family) && option.size == size) boards.add(option);
        }
        if (boards.size() <= 1) {
            chooseSize(family);
            return;
        }
        selectedFamily = family;
        selectedSize = size;
        LinearLayout choices = menuPage(Screen.BOARDS, size + " × " + size, familyName(family));
        for (Puzzle option : boards) choiceCard(choices, null, option.title, option.operationSymbols(), () -> openLevel(option));
    }

    private String familyName(String family) {
        switch (family) {
            case "patio": return "Patio de las operaciones";
            case "plaza": return "Plaza de los cuatro rumbos";
            case "senderos": return "Senderos de piedra";
            default: return "Mapas anteriores";
        }
    }

    private String familyRange(String family) {
        switch (family) {
            case "patio": return "2×2 a 4×4";
            case "plaza": return "5×5 y 6×6";
            case "senderos": return "7×7 a 9×9";
            default: return "";
        }
    }

    private void rules() {
        new AlertDialog.Builder(this).setTitle("Cómo jugar")
            .setMessage("Tlapohua significa «contar algo» en náhuatl.\n\n"
                + "Completa el tablero con números del 1 al " + puzzle.size + ". Cada número aparece una sola vez por fila, columna y región.\n\n"
                + "Los bordes gruesos separan regiones. Los números de sus casillas deben cumplir la pista de la esquina. "
                + "En una resta o división el número mayor va a la izquierda o arriba. Una pista sin operación indica el número de esa casilla.\n\n"
                + "El contador del teclado muestra cuántas veces usaste cada número. Al alcanzar el total, puedes liberar ese número borrando una de sus casillas. En Notas puedes usar todos los números.\n\n"
                + "Provisional coloca un número azul, pequeño y subrayado para probar una idea. Selecciona la casilla y toca Confirmar número para convertirlo en respuesta, o Borrar para quitarlo.\n\n"
                + "Al entregar el tablero lleno verás tus jugadas. Los aciertos seguidos suman 5, 10, 15 puntos y los números revelados con Pista valen 0. "
                + "Un error corta la racha y cuesta un intento. Las jugadas anteriores al error quedan confirmadas.")
            .setPositiveButton("Entendido", null).show();
    }

    private void enter(int number) {
        if (wasSolved || reviewing) return;
        if (provisionalMode) {
            if (values[selected] != 0) {
                refresh("Borra la respuesta de esta casilla antes de probar un número provisional.");
                return;
            }
            if (mode == Mode.LESSON && selected != Lesson.ALL[lessonIndex].cell) {
                selected = Lesson.ALL[lessonIndex].cell;
                refresh("Prueba en la casilla resaltada.");
                return;
            }
            change(0, number << puzzle.size);
            return;
        }
        if (mode == Mode.LESSON) {
            Lesson lesson = Lesson.ALL[lessonIndex];
            if (selected != lesson.cell) {
                selected = lesson.cell;
                refresh("Prueba el número en la casilla resaltada.");
                return;
            }
            if (number != puzzle.solution[selected]) {
                refresh("Ese número todavía no cumple la pista. Puedes volver a leer la explicación.");
                return;
            }
            change(number, 0);
            return;
        }
        if (wasSolved) return;
        if (pencil) {
            if (values[selected] != 0) { refresh("Borra el número de esta casilla antes de añadir notas."); return; }
            change(0, (notes[selected] & ((1 << puzzle.size) - 1)) ^ (1 << (number - 1)));
        } else {
            if (mode != Mode.CONTEST && puzzle.digitCounts(values)[number] >= puzzle.size) {
                refresh("Este número ya tiene todos sus usos. Borra una respuesta para poder confirmarlo aquí.");
                return;
            }
            change(number, 0);
        }
    }

    private void change(int value, int candidateMask) {
        change(value, candidateMask, false);
    }

    private void change(int value, int candidateMask, boolean fromHint) {
        if (mode == Mode.CONTEST && !contestCanPlay()) return;
        if (!timedCanPlay()) return;
        if (attempt != null && (wasSolved || reviewing)) return;
        if (attempt != null && attempt.locked(selected)) {
            refresh("Esta casilla ya quedó confirmada.");
            return;
        }
        if (values[selected] == value && notes[selected] == candidateMask) return;
        boolean entered = value != 0 && values[selected] != value;
        int cage = puzzle.cage[selected];
        boolean freeEntry = entered && mode == Mode.FREE;
        boolean previouslyComplete = freeEntry && puzzle.cageComplete(values, cage);
        if (undo.size() >= 100) undo.removeLast();
        undo.push(new Move(selected, values[selected], notes[selected], attempt));
        if (attempt != null && values[selected] != value) attempt.set(selected, value, fromHint);
        values[selected] = value;
        notes[selected] = candidateMask;
        save();
        refresh(fromHint ? "Pista aplicada · esta casilla lleva " + value + "." : null);
        if (entered) board.playEntry(selected, freeEntry && !previouslyComplete && puzzle.cageComplete(values, cage));
    }

    private void undoMove() {
        if (mode == Mode.CONTEST && !contestCanPlay()) return;
        if (!timedCanPlay()) return;
        if (wasSolved) return;
        Move move = undo.poll();
        if (move == null) return;
        selected = move.cell;
        values[selected] = move.value;
        notes[selected] = move.notes;
        if (attempt != null) attempt.restore(selected, move.position, move.entry);
        save();
        refresh("Último cambio deshecho.");
    }

    private void restart() {
        if ((mode == Mode.FOREST || mode == Mode.DAILY) && accepted) return;
        new AlertDialog.Builder(this).setTitle("Reiniciar nivel")
            .setMessage("Se borrarán los números y las notas. El tiempo y los puntos vuelven a cero. Conservas los intentos que te quedan.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Reiniciar", (dialog, which) -> {
                resetBoard();
                save();
                refresh(null);
            }).show();
    }

    private void resetBoard() {
        accepted = false;
        Arrays.fill(values, 0);
        Arrays.fill(notes, 0);
        attempt = new BoardAttempt(values, "");
        undo.clear();
        pencil = false;
        selected = 0;
        elapsed = 0;
        startedAt = -1;
    }

    private void hint() {
        if (wasSolved) return;
        if (attempt.locked(selected)) {
            refresh("Elige una casilla sin confirmar para pedir una pista.");
            return;
        }
        final int cell = selected;
        Puzzle board = puzzle;
        new AlertDialog.Builder(this).setTitle("Pista 1 · Revisa la regla")
            .setMessage(board.hintRule(cell))
            .setNegativeButton("Seguir intentando", null)
            .setPositiveButton("Ver candidatos", (dialog, which) -> showCandidateHint(board, cell)).show();
    }

    private void showCandidateHint(Puzzle board, int cell) {
        if (screen != Screen.GAME || puzzle != board || accepted || reviewing) return;
        int[] candidates = board.candidates(values, cell);
        String choices = candidates.length == 0 ? "Revisa los números que ya colocaste. Con esa configuración no hay candidatos para esta casilla."
            : "Candidatos compatibles con la fila, columna y región actuales · " + Arrays.toString(candidates);
        new AlertDialog.Builder(this).setTitle("Pista 2 · Acota tus opciones")
            .setMessage(choices + "\n\nEstos candidatos dependen de tus respuestas actuales. Puedes probarlos como provisionales.")
            .setNegativeButton("Seguir intentando", null)
            .setPositiveButton("Revelar respuesta", (dialog, which) -> {
                if (screen != Screen.GAME || puzzle != board || accepted || reviewing) return;
                new AlertDialog.Builder(this).setTitle("Pista 3 · Revelar una casilla")
                    .setMessage("La casilla revelada vale 0 puntos en la repetición de tus jugadas.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Revelar", (window, button) -> {
                        if (screen != Screen.GAME || puzzle != board || accepted || reviewing) return;
                        selected = cell;
                        change(board.solution[cell], 0, true);
                    }).show();
            }).show();
    }

    private void refresh(String message) {
        boolean solved = isComplete();
        boolean justSolved = solved && !wasSolved;
        wasSolved = solved;
        if (justSolved || startedAt < 0) updateClock(solved);
        String position = "Fila " + (selected / puzzle.size + 1) + ", columna " + (selected % puzzle.size + 1)
            + " · Región " + puzzle.clue(puzzle.cage[selected]);
        if (values[selected] == 0 && (notes[selected] >> puzzle.size) > 0) position += " · Número provisional";
        if (mode != Mode.LESSON && mode != Mode.CONTEST && message == null && !solved)
            position += " · " + (Journey.ATTEMPTS - mistakes) + " intentos";
        status.setText(solved ? (mode == Mode.LESSON ? "¡Caso resuelto!" : "¡Mapa resuelto! · " + resultPoints() + " puntos")
            : message != null ? message : position);
        status.setTextColor(solved ? GREEN : MUTED);
        if (routeStatus != null) routeStatus.setText(routeProgress());
        editRow.setVisibility(mode == Mode.LESSON || solved ? View.GONE : View.VISIBLE);
        undoButton.setEnabled(!undo.isEmpty());
        undoButton.setAlpha(undo.isEmpty() ? .5f : 1f);
        noteButton.setText(pencil ? "Notas ✓" : "Notas");
        noteButton.setBackground(background(pencil ? 0xffbddbc6 : SOFT, 12));
        provisionalRow.setVisibility(solved ? View.GONE : View.VISIBLE);
        provisionalButton.setText(provisionalMode ? "Provisional ✓" : "Provisional");
        provisionalButton.setBackground(background(provisionalMode ? 0xffc8dafa : SOFT, 12));
        confirmProvisionalButton.setEnabled(!solved && (notes[selected] >> puzzle.size) > 0);
        confirmProvisionalButton.setAlpha(confirmProvisionalButton.isEnabled() ? 1f : .5f);
        board.updateCells();
        updateDigitButtons();
        if (mode != Mode.CONTEST) nextBoardButton.setText(advanceLabel());
        nextBoardButton.setVisibility(solved ? View.VISIBLE : View.GONE);
        submitButton.setVisibility(mode != Mode.LESSON && !solved ? View.VISIBLE : View.GONE);
        if (mode == Mode.FOREST || mode == Mode.DAILY) restartButton.setEnabled(!solved);
        if (justSolved && mode == Mode.LESSON) {
            Board completed = board;
            completed.post(() -> {
                if (screen == Screen.GAME && board == completed && wasSolved && !isFinishing() && !isDestroyed()) showVictory();
            });
        }
    }

    private void updateDigitButtons() {
        int[] counts = puzzle.digitCounts(values);
        for (int digit = 1; digit <= puzzle.size; digit++) {
            Button button = digitButtons[digit];
            if (mode == Mode.CONTEST) {
                button.setText(String.valueOf(digit));
                continue;
            }
            boolean usedUp = counts[digit] >= puzzle.size;
            boolean completed = wasSolved && counts[digit] == puzzle.size;
            String tally = digit + "\n" + counts[digit] + "/" + puzzle.size;
            SpannableString label = new SpannableString(tally);
            label.setSpan(new RelativeSizeSpan(.61f), tally.indexOf('\n') + 1, tally.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(label);
            button.setEnabled(!wasSolved && (pencil || provisionalMode || !usedUp));
            button.setTextColor(completed ? COMPLETED_INK : usedUp ? INK : WHITE);
            button.setBackground(background(completed ? COMPLETED : usedUp ? AT_LIMIT : TlapohuaArt.JADE, 12));
            button.setContentDescription("Número " + digit + ", " + counts[digit] + " de " + puzzle.size
                + (completed ? ", completo" : "") + (button.isEnabled() ? "" : ", sin usos disponibles"));
        }
    }

    private Puzzle nextPuzzle() {
        boolean currentFound = false;
        for (Puzzle entry : catalog) {
            if (currentFound && !entry.family.equals("archivo")) return entry;
            if (entry.id.equals(puzzle.id)) currentFound = true;
        }
        return null;
    }

    private void advanceAfterWin() {
        if (mode == Mode.FOREST) {
            if (screen != Screen.GAME || !accepted) return;
            advanceForest();
            return;
        }
        if (mode == Mode.LESSON) {
            if (lessonIndex + 1 < Lesson.ALL.length) openLesson(lessonIndex + 1);
            else chooseLesson();
            return;
        }
        if (mode == Mode.DAILY) { backToDailyCalendar(); return; }
        if (mode == Mode.PRACTICE) { showPractice(); return; }
        Puzzle next = nextPuzzle();
        if (next == null) chooseLevel();
        else openLevel(next);
    }

    private void advanceForest() {
        journey.continueAfterWin();
        prefs.edit().putString("forest-route", journey.encode()).apply();
        showJourney();
    }

    private void showVictory() {
        if (mode != Mode.FREE) {
            String title = mode == Mode.LESSON ? "¡Correcto!" : "¡Felicidades!";
            String detail = mode == Mode.LESSON ? "Aplicaste la regla de este caso."
                : mode == Mode.DAILY ? "Completaste el reto del " + dailyDate + "."
                : mode == Mode.PRACTICE ? "Completaste este tablero de práctica."
                : (journey.qualified() ? "¡Nivel superado!" : "Victoria registrada. Elige otro mapa de este nivel.")
                    + "\n\n" + routeProgress();
            if (mode != Mode.LESSON) detail += "\n\n" + victoryResult();
            new AlertDialog.Builder(this).setTitle(title).setMessage(detail)
                .setNegativeButton("Ver tablero", null)
                .setPositiveButton(advanceLabel(), (dialog, which) -> advanceAfterWin()).show();
            return;
        }
        Puzzle next = nextPuzzle();
        String message = "Resolviste «" + puzzle.title + "».\n\n" + victoryResult();
        if (next != null) message += "\n\nSiguiente reto · " + next.title + " (" + next.size + "×" + next.size + ").";
        new AlertDialog.Builder(this).setTitle("¡Felicidades!")
            .setMessage(message)
            .setNegativeButton("Ver tablero", null)
            .setPositiveButton(next == null ? "Elegir otro tablero" : "Siguiente tablero",
                (dialog, which) -> advanceAfterWin()).show();
    }

    private String victoryResult() {
        return clock.getText() + "\nPuntos · " + resultPoints()
            + (mode == Mode.PRACTICE ? "" : "\n\nPuedes imprimir este tablero desde Mi colección.");
    }

    private int resultPoints() {
        if (prefs.contains(storageKey() + ATTEMPT)) return attempt.total();
        String oldScore = prefs.getString(storageKey() + "-score", "0");
        try {
            return Integer.parseInt(oldScore.split(",", 2)[0]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String advanceLabel() {
        if (mode == Mode.FOREST) {
            if (!journey.qualified()) return "Elegir otro mapa";
            return journey.stage == Journey.SIZES.length - 1 ? "Llegar a la cumbre" : "Siguiente nivel";
        }
        if (mode == Mode.LESSON) return lessonIndex + 1 < Lesson.ALL.length ? "Siguiente lección" : "Ver lecciones";
        if (mode == Mode.DAILY) return "Ver calendario";
        if (mode == Mode.PRACTICE) return "Elegir otra práctica";
        return nextPuzzle() == null ? "Elegir otro tablero" : "Siguiente tablero";
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("screen", screen.name());
        state.putString("mode", mode.name());
        state.putString("family", selectedFamily);
        state.putInt("size", selectedSize);
        state.putInt("lesson", lessonIndex);
        if (screen == Screen.GAME && mode == Mode.LESSON) state.putIntArray("lesson-notes", notes);
        state.putInt("preview-page", previewPage);
        state.putString("daily-date", dailyDate == null ? "" : dailyDate);
        state.putBoolean("contest-pro", contestPro);
        state.putBoolean("ranked-contest", rankedContest);
        state.putBoolean("notebook-history", notebookHistory);
        state.putInt("practice-operation", practiceOperation);
        state.putInt("practice-size", practiceSize);
        state.putString("practice-excluded", practiceExcluded);
        state.putString("practice-board", practiceBoard);
        if (screen == Screen.GAME && isTimedPractice()) state.putString("timed-practice", timedPractice.encode());
        state.putLong("contest-record", contestRecordId);
        state.putString("contest-pdf", contestPdfState);
        state.putBoolean("tournament-guide-open", tournamentGuide != null && tournamentGuide.isShowing());
        state.putInt("tournament-guide-step", tournamentGuideStep);
        state.putBoolean("tournament-guide-enrollment", tournamentGuideEnrollment);
        if (dailyMonth != null) state.putString("daily-month", DailyDates.key(dailyMonth));
    }

    @Override public void onBackPressed() {
        switch (screen) {
            case INTRO: finishIntro(); break;
            case PDF_PREVIEW: goBack(this::showCollection); break;
            case CONTEST_RECORD: goBack(this::showContestResults); break;
            case CONTEST_RESULTS:
                goBack(notebookHistory ? this::showPersonalRecords : this::showContest);
                break;
            case CONTEST_SETUP:
            case CONTEST_ROUND:
                goBack(this::showContest);
                break;
            case PRACTICE:
            case RECOMMENDED:
            case LEARNING:
            case FRIENDS:
            case RECORDS:
            case DISPLAY:
            case BACKUP:
            case PRIVACY:
            case CONTEST:
            case ROUTE:
            case DAILY_CALENDAR:
            case ZONES:
                goBack(this::showPlay);
                break;
            case PLAY:
            case COLLECTION:
            case LESSONS:
                goBack(this::showHome);
                break;
            case SIZES: goBack(this::chooseLevel); break;
            case BOARDS: goBack(() -> chooseSize(selectedFamily)); break;
            case GAME:
                if (mode == Mode.FOREST) goBack(this::showJourney);
                else if (mode == Mode.LESSON) goBack(this::chooseLesson);
                else if (mode == Mode.DAILY) backToDailyCalendar();
                else if (mode == Mode.CONTEST) goBack(this::showContestRound);
                else if (mode == Mode.PRACTICE) goBack(this::showPractice);
                else goBack(() -> chooseBoard(puzzle.family, puzzle.size));
                break;
            default: finishAndRemoveTask();
        }
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private Button action(LinearLayout row, String label, boolean primary, Runnable click) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(primary ? WHITE : GREEN);
        button.setMinimumHeight(dp(48));
        button.setMinimumWidth(0);
        button.setMinWidth(0);
        button.setPadding(dp(3), dp(4), dp(3), dp(4));
        button.setBackground(background(primary ? TlapohuaArt.TERRACOTTA : SOFT, 12));
        addPressMotion(button);
        if (click != null) button.setOnClickListener(view -> click.run());
        LinearLayout.LayoutParams position = new LinearLayout.LayoutParams(0, -2, 1);
        if (row.getChildCount() > 0) position.leftMargin = dp(5);
        row.addView(button, position);
        return button;
    }

    private void addPressMotion(View view) {
        if (!motionEnabled()) return;
        StateListAnimator press = new StateListAnimator();
        press.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, pressFrame(view, .96f, dp(2), 85));
        press.addState(new int[0], pressFrame(view, 1f, 0, 140));
        view.setStateListAnimator(press);
    }

    private ObjectAnimator pressFrame(View view, float scale, float shift, int milliseconds) {
        return ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat(View.SCALE_X, scale),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, scale), PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, shift))
            .setDuration(milliseconds);
    }

    private Drawable background(int color, int radius) {
        return TlapohuaArt.tile(color, density(), radius, true);
    }

    private TextView addText(LinearLayout parent, String label, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT_BOLD);
        parent.addView(text);
        return text;
    }

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private float density() { return getResources().getDisplayMetrics().density; }

    private int dp(float size) { return Math.round(size * density()); }

    private final class Board extends ViewGroup {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean highContrast = prefs.getBoolean(HIGH_CONTRAST, false);
        private final boolean largeNumbers = prefs.getBoolean(LARGE_NUMBERS, false);
        private final boolean zoomBoard = prefs.getBoolean(ZOOM_BOARD, false);
        private final boolean[] completedCages = new boolean[puzzle.first.length];
        private final Path cageOutline = new Path(), tracedOutline = new Path();
        private final PathMeasure outlineMeasure = new PathMeasure();
        private ValueAnimator digitMotion, outlineMotion;
        private int pulsingCell = -1, pulsingValue;
        private float digitScale = 1f, outlineLength, outlineProgress;
        private int side;

        Board() {
            super(MainActivity.this);
            setWillNotDraw(false);
            for (int i = 0; i < values.length; i++) {
                final int cell = i;
                View tile = new View(MainActivity.this);
                tile.setFocusable(true);
                tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                tile.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                    @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.setClassName(Button.class.getName());
                        info.setCollectionItemInfo(android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo.obtain(
                            cell / puzzle.size, 1, cell % puzzle.size, 1, false, cell == selected));
                    }
                });
                tile.setOnFocusChangeListener((view, hasFocus) -> {
                    if (hasFocus) { selected = cell; refresh(null); }
                });
                tile.setOnClickListener(view -> { selected = cell; refresh(null); });
                addView(tile);
            }
        }

        void playEntry(int cell, boolean traceCage) {
            if (!motionEnabled()) return;
            if (digitMotion != null) digitMotion.cancel();
            pulsingCell = cell;
            pulsingValue = values[cell];
            digitMotion = ValueAnimator.ofFloat(.82f, 1.09f, 1f);
            digitMotion.setDuration(175);
            digitMotion.addUpdateListener(frame -> {
                digitScale = (float) frame.getAnimatedValue();
                invalidate();
            });
            digitMotion.start();
            if (!traceCage) return;

            if (outlineMotion != null) outlineMotion.cancel();
            buildOutline(puzzle.cage[cell]);
            outlineLength = 0;
            outlineMeasure.setPath(cageOutline, false);
            do { outlineLength += outlineMeasure.getLength(); } while (outlineMeasure.nextContour());
            outlineMotion = ValueAnimator.ofFloat(0f, 1f);
            outlineMotion.setDuration(690);
            outlineMotion.addUpdateListener(frame -> {
                outlineProgress = (float) frame.getAnimatedValue();
                invalidate();
            });
            outlineMotion.start();
        }

        private void buildOutline(int cage) {
            int size = puzzle.size, stride = size + 1;
            int[] step = {1, stride, -1, -stride};
            boolean[][] edges = new boolean[stride * stride][4];
            for (int cell = 0; cell < puzzle.cage.length; cell++) {
                if (puzzle.cage[cell] != cage) continue;
                int x = cell % size, y = cell / size, vertex = y * stride + x;
                if (y == 0 || puzzle.cage[cell - size] != cage) edges[vertex][0] = true;
                if (x == size - 1 || puzzle.cage[cell + 1] != cage) edges[vertex + 1][1] = true;
                if (y == size - 1 || puzzle.cage[cell + size] != cage) edges[vertex + stride + 1][2] = true;
                if (x == 0 || puzzle.cage[cell - 1] != cage) edges[vertex + stride][3] = true;
            }
            cageOutline.reset();
            for (int start = 0; start < edges.length; start++) {
                for (int initial = 0; initial < 4; initial++) {
                    if (!edges[start][initial]) continue;
                    int vertex = start, direction = initial;
                    cageOutline.moveTo(vertex % stride, vertex / stride);
                    while (edges[vertex][direction]) {
                        edges[vertex][direction] = false;
                        vertex += step[direction];
                        cageOutline.lineTo(vertex % stride, vertex / stride);
                        if (vertex == start) break;
                        // Each directed edge keeps the region on the right.
                        int right = (direction + 1) % 4;
                        if (edges[vertex][right]) direction = right;
                        else if (!edges[vertex][direction]) direction = (direction + 3) % 4;
                    }
                }
            }
        }

        @Override protected void onDetachedFromWindow() {
            if (digitMotion != null) digitMotion.cancel();
            if (outlineMotion != null) outlineMotion.cancel();
            super.onDetachedFromWindow();
        }

        void updateCells() {
            for (int group = 0; group < completedCages.length; group++)
                completedCages[group] = wasSolved && puzzle.cageComplete(values, group);
            for (int i = 0; i < values.length; i++) {
                getChildAt(i).setContentDescription(puzzle.cellDescription(values, notes[i], i,
                    attempt != null && attempt.locked(i), completedCages[puzzle.cage[i]]));
                getChildAt(i).setSelected(i == selected);
            }
            invalidate();
        }

        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setCollectionInfo(android.view.accessibility.AccessibilityNodeInfo.CollectionInfo.obtain(puzzle.size, puzzle.size, false));
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED)
                width = dp(getResources().getConfiguration().screenWidthDp - 42);
            if (zoomBoard) width = Math.max(width, puzzle.size * dp(48));
            side = Math.min(width, dp(460));
            if (zoomBoard) side = Math.max(side, puzzle.size * dp(48));
            setMeasuredDimension(width, side);
            int tile = MeasureSpec.makeMeasureSpec((side + puzzle.size - 1) / puzzle.size, MeasureSpec.EXACTLY);
            for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(tile, tile);
        }

        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int offset = (getWidth() - side) / 2;
            for (int i = 0; i < getChildCount(); i++) {
                int x = i % puzzle.size, y = i / puzzle.size;
                getChildAt(i).layout(offset + x * side / puzzle.size, y * side / puzzle.size,
                    offset + (x + 1) * side / puzzle.size, (y + 1) * side / puzzle.size);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float square = side / (float) puzzle.size;
            float offset = (getWidth() - side) / 2f;
            canvas.save();
            canvas.translate(offset, 0);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < values.length; i++) {
                float x = (i % puzzle.size) * square, y = (i / puzzle.size) * square;
                int cage = puzzle.cage[i];
                boolean confirmed = attempt != null && attempt.locked(i);
                paint.setColor(completedCages[cage] || confirmed ? COMPLETED
                    : highContrast ? (cage % 2 == 0 ? WHITE : 0xffe6e6e6) : CAGE_COLORS[cage % CAGE_COLORS.length]);
                canvas.drawRect(x, y, x + square, y + square, paint);
                if (i == selected || puzzle.cage[i] == puzzle.cage[selected]) {
                    paint.setColor(highContrast ? (i == selected ? 0xffffdc60 : 0x45ebae0c)
                        : i == selected ? 0xffa6d3b8 : 0x21436b53);
                    canvas.drawRect(x, y, x + square, y + square, paint);
                }
                drawCell(canvas, i, x, y, square);
                if (i == selected) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(highContrast ? 0xff090909 : GREEN);
                    float inset = dp(2);
                    canvas.drawRect(x + inset, y + inset, x + square - inset, y + square - inset, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
            paint.setColor(highContrast ? 0xff666666 : 0xffa3b4aa);
            paint.setStrokeWidth(dp(1));
            for (int i = 1; i < puzzle.size; i++) {
                canvas.drawLine(i * square, 0, i * square, side, paint);
                canvas.drawLine(0, i * square, side, i * square, paint);
            }
            paint.setColor(highContrast ? 0xff090909 : INK);
            paint.setStrokeWidth(Math.min(dp(highContrast ? 4 : 3), square * (highContrast ? .09f : .07f)));
            for (int i = 0; i < values.length; i++) {
                int column = i % puzzle.size, row = i / puzzle.size;
                float x = column * square, y = row * square;
                if (column > 0 && puzzle.cage[i] != puzzle.cage[i - 1]) canvas.drawLine(x, y, x, y + square, paint);
                if (row > 0 && puzzle.cage[i] != puzzle.cage[i - puzzle.size]) canvas.drawLine(x, y, x + square, y, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            float inset = paint.getStrokeWidth() / 2;
            canvas.drawRect(inset, inset, side - inset, side - inset, paint);
            if (outlineProgress > 0f && outlineProgress < 1f && square > 0f) drawCageTrace(canvas, square);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
        }

        private void drawCageTrace(Canvas canvas, float square) {
            tracedOutline.reset();
            outlineMeasure.setPath(cageOutline, false);
            float remaining = outlineLength * Math.min(1f, outlineProgress / .76f);
            do {
                float length = outlineMeasure.getLength();
                outlineMeasure.getSegment(0f, Math.min(remaining, length), tracedOutline, true);
                remaining -= length;
            } while (remaining > 0f && outlineMeasure.nextContour());
            canvas.save();
            canvas.scale(square, square);
            paint.setStrokeWidth(dp(3) / square);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xff21a987);
            paint.setAlpha((int) (255 * Math.min(1f, (1f - outlineProgress) / .24f)));
            canvas.drawPath(tracedOutline, paint);
            paint.setAlpha(255);
            paint.setStrokeCap(Paint.Cap.BUTT);
            canvas.restore();
        }

        private void drawCell(Canvas canvas, int cell, float x, float y, float square) {
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(highContrast ? 0xff090909 : INK);
            if (puzzle.first[puzzle.cage[cell]] == cell) {
                paint.setTextAlign(Paint.Align.LEFT);
                String clue = puzzle.clue(puzzle.cage[cell]);
                boolean completed = completedCages[puzzle.cage[cell]];
                paint.setTextSize(square * (largeNumbers ? .28f : .22f));
                float textWidth = paint.measureText(clue);
                float maxWidth = square * (completed ? .60f : .82f);
                if (textWidth > maxWidth) paint.setTextSize(paint.getTextSize() * maxWidth / textWidth);
                canvas.drawText(clue, x + square * .09f, y + square * .25f, paint);
                if (completed) {
                    paint.setColor(COMPLETED_INK);
                    paint.setStrokeWidth(square * .045f);
                    canvas.drawLine(x + square * .74f, y + square * .19f, x + square * .80f, y + square * .25f, paint);
                    canvas.drawLine(x + square * .80f, y + square * .25f, x + square * .91f, y + square * .12f, paint);
                    paint.setColor(highContrast ? 0xff090909 : INK);
                }
            }
            paint.setTextAlign(Paint.Align.CENTER);
            if (values[cell] != 0) {
                float scale = cell == pulsingCell && values[cell] == pulsingValue ? digitScale : 1f;
                paint.setTextSize(square * (largeNumbers ? .58f : .49f) * scale);
                canvas.drawText(String.valueOf(values[cell]), x + square * .5f, y + square * .76f, paint);
            } else if ((notes[cell] >> puzzle.size) > 0) {
                paint.setColor(highContrast ? 0xff123784 : 0xff285ab0);
                paint.setTextSize(square * (largeNumbers ? .44f : .37f));
                canvas.drawText(String.valueOf(notes[cell] >> puzzle.size), x + square * .5f, y + square * .73f, paint);
                paint.setStrokeWidth(Math.max(1f, square * .025f));
                canvas.drawLine(x + square * .36f, y + square * .82f, x + square * .64f, y + square * .82f, paint);
            } else if (notes[cell] != 0) {
                paint.setColor(highContrast ? 0xff090909 : MUTED);
                paint.setTextSize(square * (largeNumbers ? .21f : .17f));
                for (int i = 0; i < puzzle.size; i++) if ((notes[cell] & (1 << i)) != 0)
                    canvas.drawText(String.valueOf(i + 1), x + square * (.22f + (i % 3) * .28f),
                        y + square * (.48f + (i / 3) * .21f), paint);
            }
        }
    }
}
