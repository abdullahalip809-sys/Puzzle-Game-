package com.puzzlegame.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private int n;
    private int[] board;          // 0 = खाली जगह
    private TextView[] tiles;
    private TextView movesView, timeView;
    private FrameLayout holder;

    private int moves;
    private int elapsed;
    private boolean started, won;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private SharedPreferences prefs;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (started && !won) {
                elapsed++;
                updateTexts();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        n = getIntent().getIntExtra("size", 3);
        prefs = getSharedPreferences("puzzle_prefs", MODE_PRIVATE);

        AdHelper.loadBanner((AdView) findViewById(R.id.gameBanner));
        AdHelper.loadInterstitial(this);

        movesView = findViewById(R.id.movesView);
        timeView = findViewById(R.id.timeView);
        holder = findViewById(R.id.boardHolder);

        findViewById(R.id.shuffleBtn).setOnClickListener(v -> newGame());

        // Layout बनने के बाद board की साइज़ पता चलती है
        holder.post(this::buildBoard);
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private void buildBoard() {
        int avail = Math.min(holder.getWidth(), holder.getHeight()) - dp(24);
        int cell = avail / n;
        int margin = dp(3);

        GridLayout grid = new GridLayout(this);
        grid.setRowCount(n);
        grid.setColumnCount(n);

        tiles = new TextView[n * n];
        for (int i = 0; i < n * n; i++) {
            TextView t = new TextView(this);
            t.setGravity(Gravity.CENTER);
            t.setTextColor(Color.WHITE);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, cell * 0.4f);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell - 2 * margin;
            lp.height = cell - 2 * margin;
            lp.setMargins(margin, margin, margin, margin);

            final int idx = i;
            t.setOnClickListener(v -> onTileClick(idx));
            grid.addView(t, lp);
            tiles[i] = t;
        }

        holder.removeAllViews();
        holder.addView(grid, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        newGame();
    }

    private List<Integer> neighbors(int pos) {
        List<Integer> list = new ArrayList<>();
        int r = pos / n, c = pos % n;
        if (r > 0) list.add(pos - n);
        if (r < n - 1) list.add(pos + n);
        if (c > 0) list.add(pos - 1);
        if (c < n - 1) list.add(pos + 1);
        return list;
    }

    /** हल की हुई स्थिति से random चालें चलकर shuffle करता है, इसलिए puzzle हमेशा हल हो सकता है। */
    private void newGame() {
        handler.removeCallbacks(ticker);
        board = new int[n * n];
        do {
            for (int i = 0; i < n * n - 1; i++) board[i] = i + 1;
            board[n * n - 1] = 0;

            int empty = n * n - 1;
            int prev = -1;
            for (int s = 0; s < n * n * 40; s++) {
                List<Integer> nb = neighbors(empty);
                nb.remove(Integer.valueOf(prev));
                int pick = nb.get(rnd.nextInt(nb.size()));
                board[empty] = board[pick];
                board[pick] = 0;
                prev = empty;
                empty = pick;
            }
        } while (isSolved());

        moves = 0;
        elapsed = 0;
        started = false;
        won = false;
        render();
        updateTexts();
    }

    private boolean isSolved() {
        for (int i = 0; i < n * n - 1; i++) {
            if (board[i] != i + 1) return false;
        }
        return board[n * n - 1] == 0;
    }

    private void onTileClick(int idx) {
        if (won || board[idx] == 0) return;
        int empty = -1;
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 0) { empty = i; break; }
        }
        if (!neighbors(idx).contains(empty)) return;

        board[empty] = board[idx];
        board[idx] = 0;
        moves++;

        if (!started) {
            started = true;
            handler.postDelayed(ticker, 1000);
        }
        render();
        updateTexts();

        if (isSolved()) onWin();
    }

    private void render() {
        float radius = dp(12);
        for (int i = 0; i < board.length; i++) {
            TextView t = tiles[i];
            int v = board[i];
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(radius);
            if (v == 0) {
                t.setText("");
                bg.setColor(getColor(R.color.panel));
            } else {
                t.setText(String.valueOf(v));
                bg.setColor(getColor(v == i + 1 ? R.color.tile_ok : R.color.tile));
            }
            t.setBackground(bg);
        }
    }

    private String formatTime(int secs) {
        return String.format(Locale.US, "%02d:%02d", secs / 60, secs % 60);
    }

    private void updateTexts() {
        movesView.setText(getString(R.string.moves, moves));
        timeView.setText(getString(R.string.time, formatTime(elapsed)));
    }

    private void onWin() {
        won = true;
        handler.removeCallbacks(ticker);

        String key = "best_" + n;
        int best = prefs.getInt(key, 0);
        if (best == 0 || moves < best) {
            best = moves;
            prefs.edit().putInt(key, best).apply();
        }
        final int bestMoves = best;

        // जीत पर full-screen ad, फिर बधाई वाला dialog
        AdHelper.showNow(this, () -> {
            if (isFinishing()) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.win_title)
                    .setMessage(getString(R.string.win_msg, moves, formatTime(elapsed), bestMoves))
                    .setCancelable(false)
                    .setPositiveButton(R.string.play_again, (d, w) -> newGame())
                    .setNegativeButton(R.string.menu, (d, w) -> finish())
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
    }
}
