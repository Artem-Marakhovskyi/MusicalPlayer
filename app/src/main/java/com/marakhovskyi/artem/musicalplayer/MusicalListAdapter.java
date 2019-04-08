package com.marakhovskyi.artem.musicalplayer;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class MusicalListAdapter extends RecyclerView.Adapter<MusicalListAdapter.ViewHolder> {

    private final View.OnClickListener mClickListener;
    private final Activity mActivity;
    private List<Track> mValues;
    private final TracksManager mTracksManager;

    public MusicalListAdapter(
            Activity activity,
            TracksManager tracksManager,
            View.OnClickListener clickListener) {
        mActivity = activity;
        mTracksManager = tracksManager;
        mValues = tracksManager.getItems();
        mClickListener = clickListener;
    }

    public void refresh() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mValues = mTracksManager.getItems();
                notifyDataSetChanged();
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_content, viewGroup, false);
            return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.durationTextView.setText(mValues.get(i).duration);
        viewHolder.displayNameTextView.setText(mValues.get(i).displayName);
        viewHolder.titleTextView.setText(mValues.get(i).title);
        viewHolder.containerView.setOnClickListener(mClickListener);
        viewHolder.containerView.setTag(mValues.get(i).id);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final TextView displayNameTextView;
        final TextView durationTextView;
        final LinearLayout containerView;

        ViewHolder(View view) {
            super(view);
            titleTextView = (TextView) view.findViewById(R.id.title);
            displayNameTextView = (TextView) view.findViewById(R.id.displayname);
            durationTextView = (TextView) view.findViewById(R.id.duration);
            containerView = (LinearLayout) view.findViewById(R.id.container);
        }
    }
}
