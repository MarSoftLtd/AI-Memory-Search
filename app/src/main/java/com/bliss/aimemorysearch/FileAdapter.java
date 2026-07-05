package com.bliss.aimemorysearch;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.io.File;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<FileItem> fileList;

    public FileAdapter(List<FileItem> fileList) {
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_file,
                parent,
                false
        );

        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FileViewHolder holder,
            int position
    ) {

        FileItem item = fileList.get(position);
        holder.itemView.setOnClickListener(v -> {

            if (
                    item.getType().equals("IMAGE")
                            && item.getImagePath() != null
            ) {

                Intent intent = new Intent(
                        holder.itemView.getContext(),
                        PreviewActivity.class
                );

                intent.putExtra(
                        "path",
                        item.getImagePath()
                );

                holder.itemView.getContext()
                        .startActivity(intent);

            } else if (
                    item.getType().equals("PDF")
            ) {

                try {

                    android.net.Uri uri =
                            androidx.core.content.FileProvider.getUriForFile(
                                    holder.itemView.getContext(),
                                    holder.itemView.getContext()
                                            .getPackageName()
                                            + ".provider",
                                    new File(item.getPath())
                            );

                    Intent intent =
                            new Intent(Intent.ACTION_VIEW);

                    intent.setDataAndType(
                            uri,
                            "application/pdf"
                    );

                    intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    holder.itemView.getContext()
                            .startActivity(intent);

                } catch (Exception e) {

                    android.widget.Toast.makeText(
                            holder.itemView.getContext(),
                            "No PDF app found",
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
        holder.fileName.setText(
                item.getName()
        );

        holder.filePath.setText(
                item.getPath()
        );

        holder.fileType.setText(
                item.getType()
        );

        if (
                item.getType().equals("IMAGE")
                        && item.getImagePath() != null
        ) {

            Glide.with(holder.itemView.getContext())
                    .load(new File(item.getImagePath()))
                    .centerCrop()
                    .into(holder.icon);

        } else {

            switch (item.getType()) {

                case "PDF":
                    holder.icon.setImageResource(
                            android.R.drawable.ic_menu_save
                    );
                    break;

                case "DOC":
                    holder.icon.setImageResource(
                            android.R.drawable.ic_menu_edit
                    );
                    break;

                default:
                    holder.icon.setImageResource(
                            android.R.drawable.ic_menu_help
                    );
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView fileName;
        TextView filePath;
        TextView fileType;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            filePath = itemView.findViewById(R.id.filePath);
            fileType = itemView.findViewById(R.id.fileType);
        }
    }
}