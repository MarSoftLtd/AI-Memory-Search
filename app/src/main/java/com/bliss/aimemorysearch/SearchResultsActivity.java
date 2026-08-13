package com.bliss.aimemorysearch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.ClickableSpan;
import android.text.method.LinkMovementMethod;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.core.content.ContextCompat;

import com.bliss.aimemorysearch.ai.concepts.Interpretation;
import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentEvidence;
import com.bliss.aimemorysearch.ai.explanation.AgreementPresentation;
import com.bliss.aimemorysearch.ai.explanation.CompactExplanationRenderer;
import com.bliss.aimemorysearch.ai.explanation.ConfidencePresentation;
import com.bliss.aimemorysearch.ai.explanation.EvidenceReference;
import com.bliss.aimemorysearch.ai.explanation.Explanation;
import com.bliss.aimemorysearch.ai.explanation.ExplanationEngine;
import com.bliss.aimemorysearch.ai.explanation.ExplanationInput;
import com.bliss.aimemorysearch.ai.explanation.InterpretationSummary;
import com.bliss.aimemorysearch.ai.explanation.MissingEvidenceItem;
import com.bliss.aimemorysearch.ai.explanation.QuerySummary;
import com.bliss.aimemorysearch.ai.explanation.ResultExplanation;
import com.bliss.aimemorysearch.ai.explanation.SelectionSummary;
import com.bliss.aimemorysearch.db.FileEntity;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchResultsActivity
        extends AppCompatActivity {

    private RecyclerView resultsRecycler;
    private ImageView backgroundBlur;
    private TextView resultsCount;
    private List<FileEntity> currentResults;
    private LinearLayoutManager resultsLayoutManager;
    private PagerSnapHelper resultsSnapHelper;
    private ImageButton previousResult;
    private ImageButton nextResult;

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
                SearchResultsHolder.getAllResults();
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
                    getString(
                            R.string.results_empty
                    )
            );

            return;
        }

        resultsCount.setText(
                getString(
                        R.string.results_count,
                        currentResults.size()
                )
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
        resultsLayoutManager = manager;
        previousResult = findViewById(R.id.resultPrevious);
        nextResult = findViewById(R.id.resultNext);

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

        resultsRecycler.setItemAnimator(
                null
        );
        resultsRecycler.setHasFixedSize(true);
        resultsRecycler.setItemViewCacheSize(5);
        resultsRecycler.setNestedScrollingEnabled(true);

        int startPosition = 0;

        PagerSnapHelper snapHelper = new PagerSnapHelper();

        snapHelper.attachToRecyclerView(
                resultsRecycler
        );
        resultsSnapHelper = snapHelper;

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
                        updateNavigationArrows();
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
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            updateNavigationArrows();
                        }
                    }
                }
        );

        resultsRecycler.post(() -> {
            int cardWidth = Math.max(
                    getResources().getDimensionPixelSize(
                            R.dimen.component_result_card_width),
                    resultsRecycler.getWidth()
                            - getResources().getDimensionPixelSize(R.dimen.spacing_48)
                            - getResources().getDimensionPixelSize(R.dimen.spacing_8));
            int cardMargin = getResources().getDimensionPixelSize(
                    R.dimen.component_result_card_margin);
            int sidePadding = Math.max(0,
                    (resultsRecycler.getWidth() - cardWidth
                            - cardMargin * 2) / 2);
            resultsRecycler.setPadding(sidePadding, 0, sidePadding, 0);
            manager.scrollToPositionWithOffset(startPosition, sidePadding);
            resultsRecycler.post(this::scaleCenterItems);
            resultsRecycler.post(this::updateNavigationArrows);
        });
        previousResult.setOnClickListener(view -> navigateBy(-1));
        nextResult.setOnClickListener(view -> navigateBy(1));
    }

    private ExplanationInput explanationInput(List<FileEntity> results) {
        List<ResultExplanation> resultExplanations = new ArrayList<>(
                results.size()
        );
        Map<String, List<EvidenceReference>> evidenceByResult =
                new LinkedHashMap<>();
        Map<String, AgreementPresentation> agreementByResult =
                new LinkedHashMap<>();
        Map<String, ConfidencePresentation> confidenceByResult =
                new LinkedHashMap<>();
        int documentCount = 0;
        int imageCount = 0;

        for (int index = 0; index < results.size(); index++) {
            FileEntity file = results.get(index);
            boolean image = file.type != null
                    && file.type.equalsIgnoreCase("IMAGE");
            if (image) imageCount++; else documentCount++;

            String resultId = file.path;
            List<EvidenceReference> evidence = evidenceFor(resultId);
            evidenceByResult.put(resultId, evidence);
            AgreementPresentation agreement = unavailableAgreement();
            agreementByResult.put(resultId, agreement);
            ConfidencePresentation confidence = confidenceFor(resultId);
            confidenceByResult.put(resultId, confidence);

            List<MissingEvidenceItem> missing = new ArrayList<>();
            missing.add(new MissingEvidenceItem(
                    EvidenceReference.Category.AGREEMENT,
                    resultId,
                    "production-results",
                    MissingEvidenceItem.State.UNAVAILABLE,
                    "AGREEMENT_NOT_PUBLISHED_TO_RESULTS_VIEW",
                    null
            ));
            if (evidence.isEmpty()) {
                missing.add(new MissingEvidenceItem(
                        EvidenceReference.Category.AVAILABILITY,
                        resultId,
                        "production-results",
                        image
                                ? MissingEvidenceItem.State.NOT_APPLICABLE
                                : MissingEvidenceItem.State.UNAVAILABLE,
                        image
                                ? "DOCUMENT_EVIDENCE_NOT_APPLICABLE"
                                : "DOCUMENT_EVIDENCE_NOT_AVAILABLE",
                        null
                ));
            }
            if (!confidence.isAvailable()) {
                missing.add(new MissingEvidenceItem(
                        EvidenceReference.Category.AVAILABILITY,
                        resultId,
                        "production-results",
                        image
                                ? MissingEvidenceItem.State.NOT_APPLICABLE
                                : MissingEvidenceItem.State.UNAVAILABLE,
                        "CONFIDENCE_NOT_AVAILABLE",
                        null
                ));
            }

            Float finalScore = SearchExplanationHolder.scores.get(file.path);
            resultExplanations.add(new ResultExplanation(
                    resultId,
                    file.path,
                    image
                            ? InterpretationExecutionResult.Modality.IMAGE
                            : InterpretationExecutionResult.Modality.DOCUMENT,
                    index + 1,
                    finalScore,
                    "production-results",
                    agreement,
                    confidence,
                    evidence,
                    provenanceFor(resultId),
                    missing,
                    missing.isEmpty()
                            ? ResultExplanation.Completeness.COMPLETE
                            : ResultExplanation.Completeness.PARTIAL
            ));
        }

        InterpretationSummary interpretation = new InterpretationSummary(
                "production-results",
                Interpretation.Kind.WHOLE_QUERY,
                Collections.emptyList(),
                "production-search",
                false,
                true,
                false,
                null,
                results.size(),
                documentCount,
                imageCount
        );
        return new ExplanationInput(
                stableExecutionId(results),
                "1",
                new QuerySummary(null, null, false),
                resultExplanations,
                interpretation,
                Collections.emptyList(),
                new SelectionSummary(
                        results.size(),
                        documentCount,
                        imageCount,
                        interpretation.getInterpretationId(),
                        unavailableAgreement(),
                        true
                ),
                evidenceByResult,
                agreementByResult,
                confidenceByResult
        );
    }

    private List<EvidenceReference> evidenceFor(String resultId) {
        DocumentEvidence evidence = SearchExplanationHolder
                .getDocumentEvidenceByPath().get(resultId);
        if (evidence == null) return Collections.emptyList();

        List<EvidenceReference> references = new ArrayList<>();
        references.add(reference(resultId, "bm25",
                EvidenceReference.Category.LEXICAL,
                evidence.getBestBm25(), null, true));
        references.add(reference(resultId, "semantic",
                EvidenceReference.Category.SEMANTIC,
                evidence.getBestSemanticScore(), null,
                evidence.isSemanticAvailable()));
        references.add(reference(resultId, "coverage",
                EvidenceReference.Category.COVERAGE,
                evidence.getBestCoverage(), null, true));
        references.add(reference(resultId, "canonicalCoverage",
                EvidenceReference.Category.CANONICAL_COVERAGE,
                evidence.getBestCanonicalCoverage(), null,
                evidence.isCanonicalAvailable()));
        references.add(reference(resultId, "exactTokenMatches",
                EvidenceReference.Category.EXACT_MATCH,
                evidence.getExactTokenMatches(), null, true));
        references.add(reference(resultId, "scoreMargin",
                EvidenceReference.Category.SCORE_MARGIN,
                evidence.getScoreMargin(), null, true));
        return references;
    }

    private EvidenceReference reference(
            String resultId,
            String field,
            EvidenceReference.Category category,
            double raw,
            Float normalized,
            boolean available
    ) {
        return new EvidenceReference(
                resultId + ':' + field,
                resultId,
                category,
                "DocumentEvidence",
                field,
                EvidenceReference.Scope.RESULT,
                available,
                raw,
                normalized,
                provenanceFor(resultId),
                "production-results",
                null
        );
    }

    private ConfidencePresentation confidenceFor(String resultId) {
        DocumentConfidence confidence = SearchExplanationHolder
                .getDocumentConfidences().get(resultId);
        if (confidence == null || !confidence.getConfidence().isAvailable()) {
            return new ConfidencePresentation(
                    false, null, null, null, Collections.emptyMap()
            );
        }
        Map<String, EvidenceReference> signals = new LinkedHashMap<>();
        addNormalized(signals, resultId, "bm25", confidence.getBm25());
        addNormalized(signals, resultId, "semantic", confidence.getSemantic());
        addNormalized(signals, resultId, "coverage", confidence.getCoverage());
        addNormalized(signals, resultId, "canonicalCoverage",
                confidence.getCanonicalCoverage());
        addNormalized(signals, resultId, "exactMatch",
                confidence.getExactMatch());
        addNormalized(signals, resultId, "scoreMargin",
                confidence.getScoreMargin());
        return new ConfidencePresentation(
                true,
                confidence.getConfidence().getValue(),
                null,
                null,
                signals
        );
    }

    private void addNormalized(
            Map<String, EvidenceReference> signals,
            String resultId,
            String field,
            DocumentConfidence.NormalizedSignal signal
    ) {
        if (signal == null) return;
        signals.put(field, reference(
                resultId,
                field,
                categoryFor(field),
                signal.getValue(),
                signal.isAvailable() ? signal.getValue() : null,
                signal.isAvailable()
        ));
    }

    private EvidenceReference.Category categoryFor(String field) {
        if ("bm25".equals(field)) return EvidenceReference.Category.LEXICAL;
        if ("semantic".equals(field)) return EvidenceReference.Category.SEMANTIC;
        if ("coverage".equals(field)) return EvidenceReference.Category.COVERAGE;
        if ("canonicalCoverage".equals(field)) {
            return EvidenceReference.Category.CANONICAL_COVERAGE;
        }
        if ("exactMatch".equals(field)) {
            return EvidenceReference.Category.EXACT_MATCH;
        }
        return EvidenceReference.Category.SCORE_MARGIN;
    }

    private List<String> provenanceFor(String resultId) {
        DocumentEvidence evidence = SearchExplanationHolder
                .getDocumentEvidenceByPath().get(resultId);
        if (evidence == null) return Collections.emptyList();
        DocumentEvidence.Provenance provenance =
                evidence.getProvenanceByPath().get(resultId);
        if (provenance == null) return Collections.emptyList();
        List<String> values = new ArrayList<>(5);
        if (provenance.isLexical()) values.add("lexical");
        if (provenance.isCanonical()) values.add("canonical");
        if (provenance.isOriginal()) values.add("original");
        if (provenance.isTranslation()) values.add("translation");
        if (provenance.isAlias()) values.add("alias");
        return values;
    }

    private AgreementPresentation unavailableAgreement() {
        return new AgreementPresentation(
                false, null, null, null, Collections.emptyList(),
                null, null, "production-results", "production-search"
        );
    }

    private String stableExecutionId(List<FileEntity> results) {
        int hash = 1;
        for (FileEntity result : results) {
            hash = 31 * hash + result.path.hashCode();
        }
        return "results-" + Integer.toHexString(hash);
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

            child.animate().cancel();
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(alpha);

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

    private int currentCarouselPosition() {
        if (resultsLayoutManager == null || resultsSnapHelper == null) return 0;
        View snapped = resultsSnapHelper.findSnapView(resultsLayoutManager);
        if (snapped != null) return resultsLayoutManager.getPosition(snapped);
        int visible = resultsLayoutManager.findFirstVisibleItemPosition();
        return visible == RecyclerView.NO_POSITION ? 0 : visible;
    }

    private void navigateBy(int delta) {
        if (currentResults == null || currentResults.isEmpty()) return;
        int target = currentCarouselPosition() + delta;
        if (target < 0 || target >= currentResults.size()) return;
        resultsRecycler.smoothScrollToPosition(target);
    }

    private void updateNavigationArrows() {
        if (previousResult == null || nextResult == null
                || currentResults == null || currentResults.isEmpty()) return;
        int position = currentCarouselPosition();
        CarouselNavigationState state = CarouselNavigationState.at(
                position, currentResults.size());
        previousResult.setVisibility(state.hasPrevious
                ? View.VISIBLE : View.GONE);
        nextResult.setVisibility(state.hasNext ? View.VISIBLE : View.GONE);
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
                        getResources()
                                .getInteger(
                                        R.integer.duration_background_out
                                )
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
                                    getResources()
                                            .getInteger(
                                                    R.integer.duration_background_in
                                            )
                            )
                            .start();
                })
                .start();
    }
}
