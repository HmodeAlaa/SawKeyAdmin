package com.ladrraza.sawkeyadmin;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements KeyAdapter.Listener {

    private static final String DEFAULT_SERVER_URL =
            "https://script.google.com/macros/s/AKfycbyrO5ry9JL_5BAxyc2R3CdpYyS2nWJc7ekyKVc-CfeP9p7ND31D0hdrbuQe75GnPv1S_w/exec";
    private static final String DEFAULT_ADMIN_SECRET = "SAW_ADMIN_2026_MASTER";
    private static final String PREFS_NAME = "saw_key_admin_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_ADMIN_SECRET = "admin_secret";

    private final SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SecureRandom rng = new SecureRandom();

    private SharedPreferences prefs;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvStatTotal, tvStatActive, tvStatPaused;
    private EditText etCode, etExpiry, etNote, etSearch;
    private MaterialButton btnCreateKey;
    private KeyAdapter adapter;
    private final List<KeyItem> allKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatActive = findViewById(R.id.tvStatActive);
        tvStatPaused = findViewById(R.id.tvStatPaused);
        etCode = findViewById(R.id.etCode);
        etExpiry = findViewById(R.id.etExpiry);
        etNote = findViewById(R.id.etNote);
        etSearch = findViewById(R.id.etSearch);
        btnCreateKey = findViewById(R.id.btnCreateKey);

        RecyclerView rvKeys = findViewById(R.id.rvKeys);
        rvKeys.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KeyAdapter(this);
        rvKeys.setAdapter(adapter);

        etCode.setText(generateRandomCode());
        setExpiryOffsetDays(30);

        findViewById(R.id.btnRandomCode).setOnClickListener(v -> etCode.setText(generateRandomCode()));
        findViewById(R.id.btnPickDate).setOnClickListener(v -> showDatePicker(etExpiry.getText().toString(),
                dateStr -> etExpiry.setText(dateStr)));
        etExpiry.setOnClickListener(v -> showDatePicker(etExpiry.getText().toString(),
                dateStr -> etExpiry.setText(dateStr)));

        findViewById(R.id.btnPlus7).setOnClickListener(v -> setExpiryOffsetDays(7));
        findViewById(R.id.btnPlus30).setOnClickListener(v -> setExpiryOffsetDays(30));
        findViewById(R.id.btnPlus365).setOnClickListener(v -> setExpiryOffsetDays(365));

        btnCreateKey.setOnClickListener(v -> createKey());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadKeys());
        findViewById(R.id.btnSettings).setOnClickListener(v -> showServerSettingsDialog());
        swipeRefresh.setOnRefreshListener(this::loadKeys);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadKeys();
    }

    private String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    private String getAdminSecret() {
        return prefs.getString(KEY_ADMIN_SECRET, DEFAULT_ADMIN_SECRET);
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SAW-");
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        sb.append("-");
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    private void setExpiryOffsetDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        etExpiry.setText(ymdFormat.format(cal.getTime()));
    }

    private interface DateSelected {
        void onDate(String ymd);
    }

    private void showDatePicker(String initialYmd, DateSelected cb) {
        Calendar cal = Calendar.getInstance();
        try {
            if (initialYmd != null && initialYmd.matches("\\d{4}-\\d{2}-\\d{2}")) {
                cal.setTime(ymdFormat.parse(initialYmd));
            }
        } catch (Exception ignored) {}

        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d);
            cb.onDate(ymdFormat.format(picked.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void loadKeys() {
        swipeRefresh.setRefreshing(true);
        KeyApiClient.request(getServerUrl(), getAdminSecret(), "list", null, (ok, data, error) -> {
            swipeRefresh.setRefreshing(false);
            if (!ok) {
                Toast.makeText(this, "Sync failed: " + error, Toast.LENGTH_LONG).show();
                return;
            }
            allKeys.clear();
            allKeys.addAll(KeyApiClient.parseKeys(data));
            updateStats();
            applyFilter(etSearch.getText().toString());
        });
    }

    private void updateStats() {
        int total = allKeys.size();
        int active = 0;
        int inactive = 0;
        for (KeyItem item : allKeys) {
            if ("active".equalsIgnoreCase(item.status)) active++;
            else inactive++;
        }
        tvStatTotal.setText(String.valueOf(total));
        tvStatActive.setText(String.valueOf(active));
        tvStatPaused.setText(String.valueOf(inactive));
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) {
            adapter.setItems(allKeys);
            return;
        }
        List<KeyItem> filtered = new ArrayList<>();
        for (KeyItem item : allKeys) {
            if (item.code.toLowerCase(Locale.US).contains(q) ||
                item.note.toLowerCase(Locale.US).contains(q) ||
                item.hwid.toLowerCase(Locale.US).contains(q)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private void createKey() {
        String code = etCode.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (code.isEmpty() || expiry.isEmpty()) {
            Toast.makeText(this, "Enter key code and expiry date", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateKey.setEnabled(false);
        btnCreateKey.setText("SAVING...");

        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("expiry", expiry);
        params.put("note", note);
        params.put("status", "active");

        KeyApiClient.request(getServerUrl(), getAdminSecret(), "create", params, (ok, data, error) -> {
            btnCreateKey.setEnabled(true);
            btnCreateKey.setText("GENERATE & SAVE TO SERVER");
            if (!ok) {
                Toast.makeText(this, "Create failed: " + error, Toast.LENGTH_LONG).show();
                return;
            }
            copyToClipboard(code);
            Toast.makeText(this, "Created & copied: " + code, Toast.LENGTH_SHORT).show();
            etCode.setText(generateRandomCode());
            etNote.setText("");
            loadKeys();
        });
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("LicenseKey", text));
        }
    }

    @Override
    public void onCopy(KeyItem item) {
        copyToClipboard(item.code);
        Toast.makeText(this, "Copied: " + item.code, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onToggleStatus(KeyItem item) {
        Map<String, String> params = new HashMap<>();
        params.put("code", item.code);
        swipeRefresh.setRefreshing(true);
        KeyApiClient.request(getServerUrl(), getAdminSecret(), "toggle", params, (ok, data, error) -> {
            swipeRefresh.setRefreshing(false);
            if (!ok) {
                Toast.makeText(this, "Toggle failed: " + error, Toast.LENGTH_SHORT).show();
                return;
            }
            loadKeys();
        });
    }

    @Override
    public void onResetHwid(KeyItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Unbind iPhone HWID?")
                .setMessage("Clear bound device for " + item.code + "?")
                .setPositiveButton("Unbind", (d, w) -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("code", item.code);
                    swipeRefresh.setRefreshing(true);
                    KeyApiClient.request(getServerUrl(), getAdminSecret(), "reset_hwid", params, (ok, data, error) -> {
                        swipeRefresh.setRefreshing(false);
                        if (!ok) Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        else loadKeys();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditExpiry(KeyItem item) {
        showDatePicker(item.expiry, newDate -> {
            Map<String, String> params = new HashMap<>();
            params.put("code", item.code);
            params.put("expiry", newDate);
            swipeRefresh.setRefreshing(true);
            KeyApiClient.request(getServerUrl(), getAdminSecret(), "update_expiry", params, (ok, data, error) -> {
                swipeRefresh.setRefreshing(false);
                if (!ok) Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                else loadKeys();
            });
        });
    }

    @Override
    public void onDelete(KeyItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Key?")
                .setMessage("Permanently delete " + item.code + " from server?")
                .setPositiveButton("Delete", (d, w) -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("code", item.code);
                    swipeRefresh.setRefreshing(true);
                    KeyApiClient.request(getServerUrl(), getAdminSecret(), "delete", params, (ok, data, error) -> {
                        swipeRefresh.setRefreshing(false);
                        if (!ok) Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        else loadKeys();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showServerSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        EditText etUrl = new EditText(this);
        etUrl.setHint("Apps Script /exec URL");
        etUrl.setText(getServerUrl());
        layout.addView(etUrl);

        EditText etSecret = new EditText(this);
        etSecret.setHint("ADMIN_SECRET");
        etSecret.setText(getAdminSecret());
        layout.addView(etSecret);

        new AlertDialog.Builder(this)
                .setTitle("Server Settings")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit()
                            .putString(KEY_SERVER_URL, etUrl.getText().toString().trim())
                            .putString(KEY_ADMIN_SECRET, etSecret.getText().toString().trim())
                            .apply();
                    loadKeys();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
