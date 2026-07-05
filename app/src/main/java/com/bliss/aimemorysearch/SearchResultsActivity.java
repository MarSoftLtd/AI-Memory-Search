package com.bliss.aimemorysearch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bliss.aimemorysearch.db.FileEntity;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class SearchResultsActivity
        extends AppCompatActivity {

    private RecyclerView resultsRecycler;
    private ImageView backgroundBlur;
    private TextView resultsCount;

    private List<FileEntity> currentResults;

    private int lastBackgroundPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_search_results
        );

        resultsRecycler =
                findViewById(
                        R.id.resultsRecycler
                );

        resultsCount =
                findViewById(
                        R.id.resultsCount
                );

        backgroundBlur =
                findViewById(
                        R.id.backgroundBlur
                );

        currentResults =
                SearchResultsHolder.results;
        Log.d(
                "SEARCH_RESULTS",
                "RESULTS COUNT = "
                        + SearchResultsHolder.results.size()
        );

        if (
                currentResults == null
                        ||
                        currentResults.isEmpty()
        ) {

            resultsCount.setText(
                    "No memories found"
            );

            return;
        }

        resultsCount.setText(
                currentResults.size()
                        + " memories found"
        );

        LinearLayoutManager manager =
                new LinearLayoutManager(
                        this,
                        RecyclerView.HORIZONTAL,
                        false
                );

        resultsRecycler.setLayoutManager(
                manager
        );

        SearchCarouselAdapter adapter =
                new SearchCarouselAdapter(
                        currentResults
                );

        resultsRecycler.setAdapter(
                adapter
        );

        resultsRecycler.setClipToPadding(
                false
        );

        resultsRecycler.setPadding(
                90,
                0,
                90,
                0
        );

        resultsRecycler.setItemAnimator(
                null
        );

        int startPosition =
                Integer.MAX_VALUE / 2;

        startPosition =
                startPosition
                        - (
                        startPosition
                                % currentResults.size()
                );

        resultsRecycler.scrollToPosition(
                startPosition
        );

        LinearSnapHelper snapHelper =
                new LinearSnapHelper();

        snapHelper.attachToRecyclerView(
                resultsRecycler
        );

        resultsRecycler.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrolled(
                            @NonNull RecyclerView recyclerView,
                            int dx,
                            int dy
                    ) {

                        super.onScrolled(
                                recyclerView,
                                dx,
                                dy
                        );

                        scaleCenterItems();
                    }

                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView,
                            int newState
                    ) {

                        super.onScrollStateChanged(
                                recyclerView,
                                newState
                        );

                        scaleCenterItems();
                    }
                }
        );

        resultsRecycler.post(
                this::scaleCenterItems
        );
    }

    private void scaleCenterItems() {

        if (
                resultsRecycler == null
                        ||
                        currentResults == null
                        ||
                        currentResults.isEmpty()
        ) {
            return;
        }

        int recyclerCenter =
                resultsRecycler.getWidth() / 2;

        int closestPosition =
                RecyclerView.NO_POSITION;

        int closestDistance =
                Integer.MAX_VALUE;

        for (
                int i = 0;
                i < resultsRecycler.getChildCount();
                i++
        ) {

            View child =
                    resultsRecycler.getChildAt(
                            i
                    );

            int childCenter =
                    (
                            child.getLeft()
                                    +
                                    child.getRight()
                    ) / 2;

            int distance =
                    Math.abs(
                            recyclerCenter
                                    -
                                    childCenter
                    );

            if (
                    distance < closestDistance
            ) {

                closestDistance =
                        distance;

                int adapterPosition =
                        resultsRecycler.getChildAdapterPosition(
                                child
                        );

                if (
                        adapterPosition
                                != RecyclerView.NO_POSITION
                ) {

                    closestPosition =
                            adapterPosition;
                }
            }

            float scale =
                    1.0f
                            -
                            Math.min(
                                    0.14f,
                                    distance / 1600f
                            );

            float alpha =
                    1.0f
                            -
                            Math.min(
                                    0.32f,
                                    distance / 900f
                            );

            child.animate()
                    .scaleX(
                            scale
                    )
                    .scaleY(
                            scale
                    )
                    .alpha(
                            alpha
                    )
                    .setDuration(
                            120
                    )
                    .start();

            if (
                    distance < 120
            ) {

                child.setTranslationY(
                        -10f
                );

                child.setElevation(
                        24f
                );

            } else {

                child.setTranslationY(
                        0f
                );

                child.setElevation(
                        0f
                );
            }
        }

        if (
                closestPosition
                        != RecyclerView.NO_POSITION
        ) {

            updateDynamicBackground(
                    closestPosition
            );
        }
    }

    private void updateDynamicBackground(
            int position
    ) {

        if (
                currentResults == null
                        ||
                        currentResults.isEmpty()
                        ||
                        backgroundBlur == null
        ) {
            return;
        }

        int realPosition =
                position % currentResults.size();

        if (
                realPosition < 0
        ) {
            return;
        }

        if (
                realPosition == lastBackgroundPosition
        ) {
            return;
        }

        lastBackgroundPosition =
                realPosition;

        FileEntity item =
                currentResults.get(
                        realPosition
                );

        if (
                item.imagePath == null
                        ||
                        item.imagePath.trim().isEmpty()
        ) {
            return;
        }

        backgroundBlur.animate()
                .alpha(
                        0f
                )
                .setDuration(
                        180
                )
                .withEndAction(() -> {

                    Glide.with(
                                    SearchResultsActivity.this
                            )
                            .load(
                                    new File(
                                            item.imagePath
                                    )
                            )
                            .centerCrop()
                            .into(
                                    backgroundBlur
                            );

                    backgroundBlur.setTranslationX(
                            80f
                    );

                    backgroundBlur.animate()
                            .translationX(
                                    0f
                            )
                            .alpha(
                                    0.22f
                            )
                            .setDuration(
                                    520
                            )
                            .start();
                })
                .start();
    }
}