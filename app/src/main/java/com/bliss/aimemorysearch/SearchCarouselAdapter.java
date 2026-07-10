package com.bliss.aimemorysearch;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FavoriteEntity;
import com.bliss.aimemorysearch.db.FileEntity;
import com.bliss.aimemorysearch.SearchExplanationHolder;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FavoriteEntity;

public class SearchCarouselAdapter
        extends RecyclerView.Adapter<SearchCarouselAdapter.Holder> {

    private final List<FileEntity> results;

    public SearchCarouselAdapter(
            List<FileEntity> results
    ) {

        this.results = results;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(
                        parent.getContext()
                ).inflate(
                        R.layout.item_search_result,
                        parent,
                        false
                );

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position
    ) {
        FileEntity item =
                results.get(
                        position % results.size()
                );

        holder.title.setText(
                item.name
        );

        holder.path.setText(
                item.path
        );

        holder.snippet.setText("");

        holder.snippet.setVisibility(
                View.GONE
        );

        holder.snippetContainer.setVisibility(
                View.GONE
        );

        String snippet =
                SearchExplanationHolder.snippets.get(
                        item.path
                );

        if (
                snippet != null
                        &&
                        !snippet.trim().isEmpty()
        ) {

            holder.snippetContainer.setVisibility(
                    View.VISIBLE
            );

            holder.snippet.setVisibility(
                    View.VISIBLE
            );

            holder.snippet.setText(
                    holder.itemView
                            .getContext()
                            .getString(
                                    R.string.result_explanation_prefix,
                                    snippet
                            )
            );

        } else {

            holder.snippet.setText("");

            holder.snippet.setVisibility(
                    View.GONE
            );

            holder.snippetContainer.setVisibility(
                    View.GONE
            );
        }
        holder.card.setCardElevation(0f);

        holder.card.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(
                        holder.itemView
                                .getResources()
                                .getInteger(
                                        R.integer.duration_none
                                )
                )
                .start();

        if (
                item.imagePath != null
                        &&
                        !item.imagePath.trim().isEmpty()
        ) {

            Glide.with(
                            holder.image.getContext()
                    )
                    .load(
                            new File(item.imagePath)
                    )
                    .centerCrop()
                    .into(holder.image);

        } else {

            holder.image.setImageResource(
                    R.drawable.ic_image
            );
        }
        holder.card.setOnLongClickListener(v -> {

            showActionsDialog(
                    holder,
                    item
            );

            return true;
        });
        holder.card.setOnClickListener(v -> {

            try {

                File file =
                        new File(item.path);

                Uri uri =
                        FileProvider.getUriForFile(
                                holder.itemView.getContext(),
                                holder.itemView
                                        .getContext()
                                        .getPackageName()
                                        + ".provider",
                                file
                        );

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW
                        );

                if (
                        item.type != null
                ) {

                    if (
                            item.type.equalsIgnoreCase("PDF")
                    ) {

                        intent.setDataAndType(
                                uri,
                                "application/pdf"
                        );

                    } else if (
                            item.type.equalsIgnoreCase("IMAGE")
                    ) {

                        intent.setDataAndType(
                                uri,
                                "image/*"
                        );

                    } else {

                        intent.setDataAndType(
                                uri,
                                "*/*"
                        );
                    }

                } else {

                    intent.setDataAndType(
                            uri,
                            "*/*"
                    );
                }

                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                holder.itemView.getContext()
                        .startActivity(intent);

            } catch (Exception e) {

                android.widget.Toast.makeText(
                        holder.itemView.getContext(),
                        holder.itemView
                                .getContext()
                                .getString(
                                        R.string.cannot_open_file
                                ),
                        android.widget.Toast.LENGTH_LONG
                ).show();

                android.util.Log.e(
                        "OPEN_FILE",
                        "OPEN FAILED",
                        e
                );
            }
        });
        
    }
    public void updateList(List<FileEntity> newItems) {

        results.clear();

        if (newItems != null) {
            results.addAll(newItems);
        }

        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        if (results.isEmpty()) {
            return 0;
        }
        return Integer.MAX_VALUE;
    }
    
    private void showActionsDialog(
            Holder holder,
            FileEntity item
    ) {

        Dialog dialog =
                new Dialog(
                        holder.itemView.getContext()
                );

        dialog.setContentView(
                R.layout.dialog_result_actions
        );
        View actionFavorite =
                dialog.findViewById(
                        R.id.actionFavorite
                );
        View actionShare =
                dialog.findViewById(
                        R.id.actionShare
                );

        View actionEmail =
                dialog.findViewById(
                        R.id.actionEmail
                );

        View actionOpen =
                dialog.findViewById(
                        R.id.actionOpen
                );

        File file =
                new File(item.path);

        AppDatabase db =
                AppDatabase.getInstance(
                        holder.itemView.getContext()
                );

        Uri uri =
                FileProvider.getUriForFile(
                        holder.itemView.getContext(),
                        holder.itemView
                                .getContext()
                                .getPackageName()
                                + ".provider",
                        file
                );

        actionShare.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            intent.setType("*/*");

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            holder.itemView.getContext()
                    .startActivity(
                            Intent.createChooser(
                                    intent,
                                    holder.itemView
                                            .getContext()
                                            .getString(
                                                    R.string.share_file
                                            )
                            )
                    );

            dialog.dismiss();
        });
        actionFavorite.setOnClickListener(v -> {

            new Thread(() -> {

                FavoriteEntity favorite =
                        new FavoriteEntity(
                                item.path,
                                item.name,
                                item.type,
                                item.imagePath,
                                System.currentTimeMillis()
                        );

                db.favoriteDao()
                        .insert(favorite);

                ((android.app.Activity)
                        holder.itemView.getContext())
                        .runOnUiThread(() -> {

                            android.widget.Toast.makeText(
                                    holder.itemView.getContext(),
                                    holder.itemView
                                            .getContext()
                                            .getString(
                                                    R.string.saved_to_favorites
                                            ),
                                    android.widget.Toast.LENGTH_SHORT
                            ).show();
                        });

            }).start();

            dialog.dismiss();
        });
        actionEmail.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            intent.setType("*/*");

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    item.name
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            holder.itemView.getContext()
                    .startActivity(
                            Intent.createChooser(
                                    intent,
                                    holder.itemView
                                            .getContext()
                                            .getString(
                                                    R.string.send_email
                                            )
                            )
                    );

            dialog.dismiss();
        });

        actionOpen.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            intent.setDataAndType(
                    uri,
                    "*/*"
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            holder.itemView.getContext()
                    .startActivity(intent);

            dialog.dismiss();
        });

        dialog.show();
    }
    static class Holder
            extends RecyclerView.ViewHolder {

        CardView card;
        ImageView image;
        TextView title;
        TextView path;
        TextView snippet;
        View snippetContainer;

        public Holder(
                @NonNull View itemView
        ) {

            super(itemView);

            card =
                    itemView.findViewById(
                            R.id.resultCard
                    );

            image =
                    itemView.findViewById(
                            R.id.resultImage
                    );

            title =
                    itemView.findViewById(
                            R.id.resultTitle
                    );

            path =
                    itemView.findViewById(
                            R.id.resultPath
                    );

            snippet =
                    itemView.findViewById(
                            R.id.resultSnippet
                    );

            snippetContainer =
                    itemView.findViewById(
                            R.id.snippetContainer
                    );
        }
    }
}
