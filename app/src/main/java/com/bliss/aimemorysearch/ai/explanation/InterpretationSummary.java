package com.bliss.aimemorysearch.ai.explanation;

import com.bliss.aimemorysearch.ai.concepts.Interpretation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable presentation of one supplied interpretation observation. */
public final class InterpretationSummary {
    private final String interpretationId;
    private final Interpretation.Kind kind;
    private final List<String> components;
    private final String strategyId;
    private final boolean reused;
    private final boolean selected;
    private final boolean available;
    private final Float bestScore;
    private final Integer resultCount;
    private final Integer documentCount;
    private final Integer imageCount;

    public InterpretationSummary(
            String interpretationId,
            Interpretation.Kind kind,
            List<String> components,
            String strategyId,
            boolean reused,
            boolean selected,
            boolean available,
            Float bestScore,
            Integer resultCount,
            Integer documentCount,
            Integer imageCount
    ) {
        this.interpretationId = interpretationId;
        this.kind = kind;
        this.components = Collections.unmodifiableList(
                new ArrayList<>(components)
        );
        this.strategyId = strategyId;
        this.reused = reused;
        this.selected = selected;
        this.available = available;
        this.bestScore = bestScore;
        this.resultCount = resultCount;
        this.documentCount = documentCount;
        this.imageCount = imageCount;
    }

    public String getInterpretationId() { return interpretationId; }
    public Interpretation.Kind getKind() { return kind; }
    public List<String> getComponents() { return components; }
    public String getStrategyId() { return strategyId; }
    public boolean isReused() { return reused; }
    public boolean isSelected() { return selected; }
    public boolean isAvailable() { return available; }
    public Float getBestScore() { return bestScore; }
    public Integer getResultCount() { return resultCount; }
    public Integer getDocumentCount() { return documentCount; }
    public Integer getImageCount() { return imageCount; }
}
