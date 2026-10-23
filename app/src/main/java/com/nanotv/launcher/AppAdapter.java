package com.nanotv.launcher;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app, boolean isFavoriteSection);
        void onAppLongClick(AppInfo app, boolean isFavoriteSection, int position);
        void onItemMoved(int fromPosition, int toPosition, boolean isFavoriteSection);
        void onMoveModeFinished(boolean isFavoriteSection);
    }

    private final List<AppInfo> apps = new ArrayList<>();
    private final boolean isHorizontal;
    private final OnAppClickListener listener;
    private int movingPosition = -1;

    public AppAdapter(boolean isHorizontal, OnAppClickListener listener) {
        this.isHorizontal = isHorizontal;
        this.listener = listener;
    }

    public void setApps(List<AppInfo> newApps) {
        apps.clear();
        if (newApps != null) {
            apps.addAll(newApps);
        }
        notifyDataSetChanged();
    }

    public List<AppInfo> getApps() {
        return apps;
    }

    public void startMoveMode(int position) {
        this.movingPosition = position;
        notifyDataSetChanged();
    }

    public void stopMoveMode() {
        this.movingPosition = -1;
        notifyDataSetChanged();
    }

    public boolean isMoveMode() {
        return movingPosition != -1;
    }

    public int getMovingPosition() {
        return movingPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_app_horizontal : R.layout.item_app;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.tvName.setText(app.getLabel());
        holder.ivIcon.setImageDrawable(app.getIcon());

        boolean isCurrentlyMoving = (position == movingPosition);
        if (isCurrentlyMoving) {
            holder.itemView.setBackgroundResource(R.drawable.bg_card_moving);
            holder.itemView.setScaleX(1.10f);
            holder.itemView.setScaleY(1.10f);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.selector_card);
            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (movingPosition != -1) {
                // Ao clicar no app enquanto move, confirma a posição
                if (listener != null) {
                    listener.onMoveModeFinished(isHorizontal);
                }
                return;
            }
            if (listener != null) {
                listener.onAppClick(app, isHorizontal);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (movingPosition != -1) {
                return true;
            }
            if (listener != null) {
                listener.onAppLongClick(app, isHorizontal, holder.getAdapterPosition());
                return true;
            }
            return false;
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (movingPosition == holder.getAdapterPosition()) {
                v.setScaleX(1.10f);
                v.setScaleY(1.10f);
                return;
            }
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
        });

        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (movingPosition != -1 && holder.getAdapterPosition() == movingPosition) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int cur = holder.getAdapterPosition();
                    int span = isHorizontal ? 1 : 6;
                    int target = -1;

                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (cur > 0) target = cur - 1;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (cur < apps.size() - 1) target = cur + 1;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !isHorizontal) {
                        if (cur - span >= 0) target = cur - span;
                        else if (cur > 0) target = 0;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !isHorizontal) {
                        if (cur + span < apps.size()) target = cur + span;
                        else if (cur < apps.size() - 1) target = apps.size() - 1;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (listener != null) {
                            listener.onMoveModeFinished(isHorizontal);
                        }
                        return true;
                    }

                    if (target != -1 && target != cur) {
                        AppInfo movedApp = apps.remove(cur);
                        apps.add(target, movedApp);
                        movingPosition = target;
                        notifyItemMoved(cur, target);

                        if (listener != null) {
                            listener.onItemMoved(cur, target, isHorizontal);
                        }

                        final int destPos = target;
                        v.post(() -> {
                            View parent = (View) v.getParent();
                            if (parent instanceof RecyclerView) {
                                RecyclerView rv = (RecyclerView) parent;
                                rv.scrollToPosition(destPos);
                                rv.postDelayed(() -> {
                                    RecyclerView.ViewHolder targetHolder = rv.findViewHolderForAdapterPosition(destPos);
                                    if (targetHolder != null) {
                                        targetHolder.itemView.requestFocus();
                                    }
                                }, 50);
                            }
                        });
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_app_icon);
            tvName = itemView.findViewById(R.id.tv_app_name);
        }
    }
}
