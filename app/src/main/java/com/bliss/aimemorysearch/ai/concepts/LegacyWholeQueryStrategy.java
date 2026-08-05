package com.bliss.aimemorysearch.ai.concepts;

import com.bliss.aimemorysearch.ai.ChunkSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.ImageSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchAnalysis;
import com.bliss.aimemorysearch.ai.SearchRequest;

import java.util.ArrayList;
import java.util.List;

/** Adapter that delegates unchanged to the legacy whole-query retrievers. */
public final class LegacyWholeQueryStrategy
        implements InterpretationRetrievalStrategy {

    private static final String STRATEGY_ID = "legacy-whole-query-v1";

    @Override
    public String getStrategyId() { return STRATEGY_ID; }

    @Override
    public String executionKey(
            RetrievalContext context,
            Interpretation interpretation
    ) {
        return STRATEGY_ID + '\u0000' + context.getNormalizedQuery();
    }

    @Override
    public InterpretationExecutionResult execute(
            RetrievalContext context,
            RetrievalBudget budget,
            Interpretation interpretation
    ) {
        long startedNanos = System.nanoTime();
        String query = context.getNormalizedQuery();
        SearchRequest request =
                QueryUnderstandingEngine.createSearchRequest(query);
        SearchAnalysis analysis =
                QueryUnderstandingEngine.createSearchAnalysis(request);
        List<ChunkSemanticSearchEngine.ChunkResult> documents =
                ChunkSemanticSearchEngine.search(
                        context.getApplicationContext(),
                        request,
                        analysis,
                        context.getCanonicalQueries()
                );

        float[] imageEmbedding = MobileClipTextEmbeddingEngine
                .getInstance()
                .generateEmbedding(query);
        List<ImageSemanticSearchEngine.SearchResult> images =
                new ImageSemanticSearchEngine(
                        context.getApplicationContext()
                ).search(
                        imageEmbedding,
                        budget.getMaxResultsPerModality()
                );

        float bestScore = 0f;
        int retainedCapacity = budget.getMaxResultsPerModality() * 2;
        List<InterpretationExecutionResult.RetrievedResult> retained =
                new ArrayList<>(retainedCapacity);
        int documentLimit = Math.min(
                documents.size(),
                budget.getMaxResultsPerModality()
        );
        for (int index = 0; index < documents.size(); index++) {
            ChunkSemanticSearchEngine.ChunkResult result = documents.get(index);
            bestScore = Math.max(bestScore, result.score);
            if (index < documentLimit
                    && result.chunk != null
                    && result.chunk.filePath != null) {
                retained.add(new InterpretationExecutionResult.RetrievedResult(
                        result.chunk.filePath,
                        InterpretationExecutionResult.Modality.DOCUMENT,
                        result.score
                ));
            }
        }
        int imageLimit = Math.min(
                images.size(),
                budget.getMaxResultsPerModality()
        );
        for (int index = 0; index < images.size(); index++) {
            ImageSemanticSearchEngine.SearchResult result = images.get(index);
            bestScore = Math.max(bestScore, result.score);
            if (index < imageLimit
                    && result.file != null
                    && result.file.path != null) {
                retained.add(new InterpretationExecutionResult.RetrievedResult(
                        result.file.path,
                        InterpretationExecutionResult.Modality.IMAGE,
                        result.score
                ));
            }
        }

        return new InterpretationExecutionResult(
                interpretation.getId(),
                interpretation.getKind(),
                STRATEGY_ID,
                retained,
                bestScore,
                documents.size() + images.size(),
                documents.size(),
                images.size(),
                (System.nanoTime() - startedNanos) / 1_000_000L,
                false
        );
    }
}
