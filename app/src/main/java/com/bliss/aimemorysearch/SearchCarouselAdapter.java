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
import com.bliss.aimemorysearch.db.EmailEntity;
import com.bliss.aimemorysearch.SearchExplanationHolder;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FavoriteEntity;

public class SearchCarouselAdapter
        extends RecyclerView.Adapter<SearchCarouselAdapter.Holder> {

    private final List<FileEntity> results;
    private List<TechnicalSearchReportBuilder.ResultBlock> reports;
    private final Set<String> expandedReports = new HashSet<>();

    public SearchCarouselAdapter(
            List<FileEntity> results
    ) {

        this.results = results;
        this.reports = TechnicalSearchReportBuilder.build(results).results;
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

        int availableWidth = parent.getResources().getDisplayMetrics().widthPixels
                - parent.getResources().getDimensionPixelSize(R.dimen.spacing_48)
                - parent.getResources().getDimensionPixelSize(R.dimen.spacing_8);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = Math.max(
                parent.getResources().getDimensionPixelSize(
                        R.dimen.component_result_card_width),
                availableWidth);
        view.setLayoutParams(params);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position
    ) {
        FileEntity item = results.get(position);
        TechnicalSearchReportBuilder.ResultBlock report = reports.get(position);
        bindTechnicalReport(holder, item, report);

        holder.title.setText(
                item.name
        );
        int sourceColor = ContextCompat.getColor(
                holder.itemView.getContext(),
                SourceVisuals.colorResource(item.type)
        );
        holder.sourceAccent.setBackgroundColor(sourceColor);
        holder.title.setTextColor(sourceColor);
        holder.sourceLabel.setTextColor(sourceColor);
        holder.sourceLabel.setText(ResultSourceLabel.from(item));
        holder.sourceLabel.setTag(item.path);
        if ("EMAIL".equalsIgnoreCase(item.type)) {
            loadEmailSourceLabel(holder, item, sourceColor);
        }

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

            if ("EMAIL".equalsIgnoreCase(item.type)) {
                openEmail(holder, item);
                return;
            }

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
        reports = TechnicalSearchReportBuilder.build(results).results;
        expandedReports.clear();

        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        if (results.isEmpty()) {
            return 0;
        }
        return results.size();
    }

    private void bindTechnicalReport(
            Holder holder,
            FileEntity item,
            TechnicalSearchReportBuilder.ResultBlock report
    ) {
        StringBuilder text = new StringBuilder();
        text.append('#').append(report.rank).append("  ")
                .append(report.type).append("  ").append(report.title)
                .append('\n').append("Source: ")
                .append(ResultSourceLabel.from(item));
        for (String metric : report.metrics) {
            text.append('\n').append(metric);
        }
        holder.technicalText.setText(text.toString());
        boolean expanded = expandedReports.contains(item.path);
        holder.normalContent.setVisibility(expanded ? View.GONE : View.VISIBLE);
        holder.technicalScroll.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.technicalToggle.setRotation(expanded ? 270f : 90f);
        holder.technicalToggle.setOnClickListener(view -> {
            boolean show = !expandedReports.contains(item.path);
            if (show) expandedReports.add(item.path);
            else expandedReports.remove(item.path);
            View appearing = show ? holder.technicalScroll : holder.normalContent;
            View disappearing = show ? holder.normalContent : holder.technicalScroll;
            appearing.setAlpha(0f);
            appearing.setVisibility(View.VISIBLE);
            disappearing.animate().alpha(0f)
                    .setDuration(holder.itemView.getResources().getInteger(
                            R.integer.duration_short))
                    .withEndAction(() -> {
                        disappearing.setVisibility(View.GONE);
                        disappearing.setAlpha(1f);
                    }).start();
            appearing.animate().alpha(1f)
                    .setDuration(holder.itemView.getResources().getInteger(
                            R.integer.duration_short)).start();
            holder.technicalToggle.animate().rotation(show ? 270f : 90f)
                    .setDuration(holder.itemView.getResources().getInteger(
                            R.integer.duration_short)).start();
        });
    }

    private void loadEmailSourceLabel(Holder holder, FileEntity item, int color) {
        new Thread(() -> {
            EmailEntity email = AppDatabase.getInstance(
                    holder.itemView.getContext().getApplicationContext())
                    .emailDao().getById(item.path);
            if (email == null) return;
            String detail = "EMAIL";
            if (email.provider != null && !email.provider.trim().isEmpty()) {
                detail += " · " + email.provider.trim().toUpperCase(
                        java.util.Locale.ROOT);
            }
            if (email.sender != null && !email.sender.trim().isEmpty()) {
                detail += " · " + email.sender.trim();
            }
            String finalDetail = detail;
            holder.itemView.post(() -> {
                if (item.path.equals(holder.sourceLabel.getTag())) {
                    holder.sourceLabel.setTextColor(color);
                    holder.sourceLabel.setText(finalDetail);
                }
            });
        }).start();
    }

    private void openEmail(Holder holder, FileEntity item) {
        android.content.Context context = holder.itemView.getContext();
        new Thread(() -> {
            EmailEntity email = AppDatabase.getInstance(
                    context.getApplicationContext()).emailDao().getById(item.path);
            holder.itemView.post(() -> {
                if (email == null) {
                    showCannotOpen(context);
                    return;
                }
                Uri uri = EmailOpenUri.from(email);
                if (uri == null) {
                    showCannotOpen(context);
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if ("gmail".equalsIgnoreCase(email.provider)) {
                    Intent gmail = new Intent(Intent.ACTION_VIEW,
                            EmailOpenUri.gmailConversation(email));
                    gmail.setPackage("com.google.android.gm");
                    try {
                        context.startActivity(gmail);
                        return;
                    } catch (android.content.ActivityNotFoundException
                             | SecurityException unavailable) {
                        android.util.Log.w("OPEN_EMAIL",
                                "Gmail conversation deep-link unavailable; "
                                        + "using exact web fallback", unavailable);
                    }
                }
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException unavailable) {
                    showCannotOpen(context);
                }
            });
        }).start();
    }

    private void showCannotOpen(android.content.Context context) {
        android.widget.Toast.makeText(context,
                context.getString(R.string.cannot_open_file),
                android.widget.Toast.LENGTH_LONG).show();
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
        TextView sourceLabel;
        View snippetContainer;
        View sourceAccent;
        View normalContent;
        View technicalScroll;
        TextView technicalText;
        android.widget.ImageButton technicalToggle;

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
            sourceLabel = itemView.findViewById(R.id.resultSourceLabel);
            sourceAccent = itemView.findViewById(R.id.resultSourceAccent);
            normalContent = itemView.findViewById(R.id.resultNormalContent);
            technicalScroll = itemView.findViewById(R.id.resultTechnicalScroll);
            technicalText = itemView.findViewById(R.id.resultTechnicalText);
            technicalToggle = itemView.findViewById(R.id.resultTechnicalToggle);
        }
    }
}
