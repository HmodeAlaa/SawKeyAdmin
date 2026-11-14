package com.ladrraza.sawkeyadmin;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class KeyAdapter extends RecyclerView.Adapter<KeyAdapter.KeyViewHolder> {

    public interface Listener {
        void onCopy(KeyItem item);
        void onToggleStatus(KeyItem item);
        void onResetHwid(KeyItem item);
        void onEditExpiry(KeyItem item);
        void onDelete(KeyItem item);
    }

    private final List<KeyItem> items = new ArrayList<>();
    private final Listener listener;

    public KeyAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<KeyItem> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KeyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_key_card, parent, false);
        return new KeyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KeyViewHolder holder, int position) {
        KeyItem item = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvKeyCode.setText(item.code);

        String hwidDisplay = item.hwid.isEmpty() ? "Unbound" : item.hwid;
        holder.tvMeta.setText("Exp: " + item.expiry + "  |  HWID: " + hwidDisplay);

        if (item.note.isEmpty()) {
            holder.tvNote.setVisibility(View.GONE);
        } else {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText("Note: " + item.note);
        }

        int badgeColor;
        String badgeText = item.status.toUpperCase();
        if ("active".equalsIgnoreCase(item.status)) {
            badgeColor = ContextCompat.getColor(ctx, R.color.status_active);
            holder.btnToggleStatus.setText("Pause");
        } else if ("expired".equalsIgnoreCase(item.status)) {
            badgeColor = ContextCompat.getColor(ctx, R.color.status_expired);
            holder.btnToggleStatus.setText("Pause");
        } else {
            badgeColor = ContextCompat.getColor(ctx, R.color.status_paused);
            holder.btnToggleStatus.setText("Resume");
        }

        holder.tvStatusBadge.setText(badgeText);
        holder.tvStatusBadge.setTextColor(badgeColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        bg.setColor((badgeColor & 0x00FFFFFF) | 0x26000000);
        holder.tvStatusBadge.setBackground(bg);

        holder.btnCopyKey.setOnClickListener(v -> listener.onCopy(item));
        holder.btnToggleStatus.setOnClickListener(v -> listener.onToggleStatus(item));
        holder.btnResetHwid.setOnClickListener(v -> listener.onResetHwid(item));
        holder.btnEditExpiry.setOnClickListener(v -> listener.onEditExpiry(item));
        holder.btnDeleteKey.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class KeyViewHolder extends RecyclerView.ViewHolder {
        final TextView tvKeyCode;
        final TextView tvStatusBadge;
        final TextView tvMeta;
        final TextView tvNote;
        final MaterialButton btnCopyKey;
        final MaterialButton btnToggleStatus;
        final MaterialButton btnResetHwid;
        final MaterialButton btnEditExpiry;
        final MaterialButton btnDeleteKey;

        KeyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKeyCode = itemView.findViewById(R.id.tvKeyCode);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvNote = itemView.findViewById(R.id.tvNote);
            btnCopyKey = itemView.findViewById(R.id.btnCopyKey);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
            btnResetHwid = itemView.findViewById(R.id.btnResetHwid);
            btnEditExpiry = itemView.findViewById(R.id.btnEditExpiry);
            btnDeleteKey = itemView.findViewById(R.id.btnDeleteKey);
        }
    }
}
