package com.bliss.aimemorysearch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AIPackageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<AIPackageRow> rows;

    public AIPackageAdapter(List<AIPackageRow> rows) {
        this.rows = rows;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == AIPackageRow.TYPE_HEADER) {
            return new HeaderViewHolder(
                    inflater.inflate(
                            R.layout.item_ai_packages_header,
                            parent,
                            false
                    )
            );
        }

        if (viewType == AIPackageRow.TYPE_SECTION_HEADER) {
            return new SectionHeaderViewHolder(
                    inflater.inflate(
                            R.layout.item_ai_packages_section_header,
                            parent,
                            false
                    )
            );
        }

        if (viewType == AIPackageRow.TYPE_STORAGE_SUMMARY) {
            return new StorageSummaryViewHolder(
                    inflater.inflate(
                            R.layout.item_storage,
                            parent,
                            false
                    )
            );
        }

        return new AIPackageViewHolder(
                inflater.inflate(
                        R.layout.item_ai_package,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        AIPackageRow row = rows.get(position);

        if (holder instanceof SectionHeaderViewHolder) {
            ((SectionHeaderViewHolder) holder).bind(row);
            return;
        }

        if (!(holder instanceof AIPackageViewHolder)) {
            return;
        }

        AIPackageUiModel aiPackage = row.getAiPackage();
        if (aiPackage == null) {
            return;
        }

        AIPackageViewHolder packageHolder = (AIPackageViewHolder) holder;

        packageHolder.icon.setImageResource(aiPackage.getIcon());
        packageHolder.title.setText(aiPackage.getTitle());
        packageHolder.description.setText(aiPackage.getDescription());
        packageHolder.status.setText(
                getStatusText(packageHolder, aiPackage)
        );

        Integer progress = aiPackage.getProgress();
        boolean showProgress =
                progress != null
                        && aiPackage.getState()
                        == AIPackageUiModel.State.DOWNLOADING;

        packageHolder.progress.setVisibility(
                showProgress ? View.VISIBLE : View.GONE
        );
        if (showProgress) {
            packageHolder.progress.setProgress(progress);
        }

        packageHolder.chevron.setVisibility(
                aiPackage.getState() == AIPackageUiModel.State.DISABLED
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private String getStatusText(
            AIPackageViewHolder holder,
            AIPackageUiModel aiPackage
    ) {
        int stringRes;

        switch (aiPackage.getState()) {
            case READY:
                stringRes = R.string.ai_package_status_ready;
                break;
            case INSTALLED:
                stringRes = R.string.ai_package_status_installed;
                break;
            case UPDATE_AVAILABLE:
                stringRes = R.string.ai_package_status_update_available;
                break;
            case DOWNLOADING:
                stringRes = R.string.ai_package_status_downloading;
                break;
            case DISABLED:
                stringRes = R.string.ai_package_status_disabled;
                break;
            case AVAILABLE:
            default:
                stringRes = R.string.ai_package_status_available;
                break;
        }

        return holder.itemView.getContext().getString(stringRes);
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView title;

        SectionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.aiPackageSectionTitle);
        }

        void bind(AIPackageRow row) {
            title.setText(row.getTitle());

            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            layoutParams.topMargin =
                    itemView.getResources().getDimensionPixelSize(
                            row.getTopMargin()
                    );
            itemView.setLayoutParams(layoutParams);
        }
    }

    static class StorageSummaryViewHolder extends RecyclerView.ViewHolder {

        StorageSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class AIPackageViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        TextView description;
        TextView status;
        ImageView chevron;
        ProgressBar progress;

        AIPackageViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.aiPackageIcon);
            title = itemView.findViewById(R.id.aiPackageTitle);
            description = itemView.findViewById(R.id.aiPackageDescription);
            status = itemView.findViewById(R.id.aiPackageStatus);
            chevron = itemView.findViewById(R.id.aiPackageChevron);
            progress = itemView.findViewById(R.id.aiPackageProgress);
        }
    }
}
