package com.bliss.aimemorysearch.ai.concepts;

import android.util.Log;

import com.bliss.aimemorysearch.ai.ChunkSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.ImageSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchAnalysis;
import com.bliss.aimemorysearch.ai.SearchRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shadow strategy that measures agreement across atomic concept retrievals. */
public final class AtomicConjunctionStrategy
        implements InterpretationRetrievalStrategy {

    private static final String STRATEGY_ID = "atomic-conjunction-v1";
    private static final String LOG_TAG = "ATOMIC_CONJUNCTION";

    @Override
    public String getStrategyId() { return STRATEGY_ID; }

    @Override
    public String executionKey(
            RetrievalContext context,
            Interpretation interpretation
    ) {
        StringBuilder key = new StringBuilder(STRATEGY_ID);
        for (ConceptGraph.Concept component : interpretation.getComponents()) {
            key.append('\u0000').append(component.getId());
        }
        return key.toString();
    }

    @Override
    public InterpretationExecutionResult execute(
            RetrievalContext context,
            RetrievalBudget budget,
            Interpretation interpretation
    ) {
        long startedNanos = System.nanoTime();
        List<ConceptGraph.Concept> components = distinctAtomicComponents(
                interpretation,
                budget.getMaxComponents()
        );
        if (components.isEmpty()) {
            return emptyResult(interpretation, startedNanos);
        }

        Map<String, float[]> imageEmbeddings = new HashMap<>(components.size());
        Map<String, CandidateAccumulator> candidates = new LinkedHashMap<>();
        int retainedLimit = budget.getMaxResultsPerModality();

        for (int componentIndex = 0;
             componentIndex < components.size();
             componentIndex++) {
            ConceptGraph.Concept component = components.get(componentIndex);
            String componentQuery = component.getText();
            long componentStartedNanos = System.nanoTime();

            SearchRequest request = QueryUnderstandingEngine
                    .createSearchRequest(componentQuery);
            SearchAnalysis analysis = QueryUnderstandingEngine
                    .createSearchAnalysis(request);
            List<ChunkSemanticSearchEngine.ChunkResult> documents =
                    ChunkSemanticSearchEngine.search(
                            context.getApplicationContext(),
                            request,
                            analysis,
                            Collections.singletonList(componentQuery)
                    );

            float[] imageEmbedding = imageEmbeddings.get(componentQuery);
            if (imageEmbedding == null) {
                imageEmbedding = MobileClipTextEmbeddingEngine
                        .getInstance()
                        .generateEmbedding(componentQuery);
                imageEmbeddings.put(componentQuery, imageEmbedding);
            }
            List<ImageSemanticSearchEngine.SearchResult> images =
                    new ImageSemanticSearchEngine(
                            context.getApplicationContext()
                    ).search(imageEmbedding, retainedLimit);

            Map<String, Float> componentDocuments = topDocuments(
                    documents,
                    retainedLimit
            );
            Map<String, Float> componentImages = topImages(images, retainedLimit);
            addComponentEvidence(
                    candidates,
                    componentDocuments,
                    InterpretationExecutionResult.Modality.DOCUMENT,
                    componentIndex
            );
            addComponentEvidence(
                    candidates,
                    componentImages,
                    InterpretationExecutionResult.Modality.IMAGE,
                    componentIndex
            );

            Log.d(
                    LOG_TAG,
                    "component=" + componentQuery
                            + " | index=" + (componentIndex + 1)
                            + "/" + components.size()
                            + " | documents=" + documents.size()
                            + " | images=" + images.size()
                            + " | retainedDocuments="
                            + componentDocuments.size()
                            + " | retainedImages=" + componentImages.size()
                            + " | latencyMs="
                            + elapsedMillis(componentStartedNanos)
            );
        }

        List<CandidateAccumulator> ordered = new ArrayList<>(
                candidates.values()
        );
        for (CandidateAccumulator candidate : ordered) {
            candidate.finish(components.size());
        }
        ordered.sort(Comparator.comparingDouble(
                (CandidateAccumulator value) -> value.agreementScore
        ).reversed());

        int maximumRetained = retainedLimit * 2;
        List<InterpretationExecutionResult.RetrievedResult> retained =
                new ArrayList<>(Math.min(maximumRetained, ordered.size()));
        int documentCandidates = 0;
        int imageCandidates = 0;
        float bestAgreement = 0f;
        for (int index = 0; index < ordered.size(); index++) {
            CandidateAccumulator candidate = ordered.get(index);
            if (candidate.modality
                    == InterpretationExecutionResult.Modality.DOCUMENT) {
                documentCandidates++;
            } else {
                imageCandidates++;
            }
            bestAgreement = Math.max(bestAgreement, candidate.agreementScore);
            if (index < maximumRetained) {
                retained.add(new InterpretationExecutionResult.RetrievedResult(
                        candidate.path,
                        candidate.modality,
                        candidate.agreementScore
                ));
                Log.d(
                        LOG_TAG,
                        "candidate=" + candidate.path
                                + " | modality=" + candidate.modality
                                + " | overlap=" + candidate.supportCount
                                + "/" + components.size()
                                + " | agreement=" + candidate.agreementScore
                );
            }
        }

        long elapsedMillis = elapsedMillis(startedNanos);
        Log.d(
                LOG_TAG,
                "complete"
                        + " | components=" + components.size()
                        + " | candidates=" + ordered.size()
                        + " | bestAgreement=" + bestAgreement
                        + " | executionMs=" + elapsedMillis
        );
        return new InterpretationExecutionResult(
                interpretation.getId(),
                interpretation.getKind(),
                STRATEGY_ID,
                retained,
                bestAgreement,
                ordered.size(),
                documentCandidates,
                imageCandidates,
                elapsedMillis,
                false
        );
    }

    static float agreementScore(
            int supportCount,
            int componentCount,
            float normalizedScoreSum
    ) {
        if (supportCount <= 0 || componentCount <= 0) return 0f;
        float meanStrength = normalizedScoreSum / supportCount;
        meanStrength = Math.max(0f, Math.min(1f, meanStrength));
        return (supportCount + meanStrength) / (componentCount + 1f);
    }

    private static List<ConceptGraph.Concept> distinctAtomicComponents(
            Interpretation interpretation,
            int maximumComponents
    ) {
        List<ConceptGraph.Concept> components = new ArrayList<>(
                Math.min(maximumComponents, interpretation.getComponents().size())
        );
        Set<String> seen = new LinkedHashSet<>();
        for (ConceptGraph.Concept component : interpretation.getComponents()) {
            if (components.size() >= maximumComponents) break;
            if (component.getKind() == ConceptGraph.ConceptKind.ATOMIC
                    && seen.add(component.getText())) {
                components.add(component);
            }
        }
        return components;
    }

    private static Map<String, Float> topDocuments(
            List<ChunkSemanticSearchEngine.ChunkResult> results,
            int limit
    ) {
        Map<String, Float> retained = new LinkedHashMap<>(limit);
        for (ChunkSemanticSearchEngine.ChunkResult result : results) {
            if (result == null
                    || result.chunk == null
                    || result.chunk.filePath == null) {
                continue;
            }
            String path = result.chunk.filePath;
            Float previous = retained.get(path);
            if (previous != null) {
                retained.put(path, Math.max(previous, result.score));
            } else if (retained.size() < limit) {
                retained.put(path, result.score);
            }
            if (retained.size() >= limit && previous == null) break;
        }
        return retained;
    }

    private static Map<String, Float> topImages(
            List<ImageSemanticSearchEngine.SearchResult> results,
            int limit
    ) {
        Map<String, Float> retained = new LinkedHashMap<>(limit);
        for (ImageSemanticSearchEngine.SearchResult result : results) {
            if (result == null
                    || result.file == null
                    || result.file.path == null) {
                continue;
            }
            if (!retained.containsKey(result.file.path)
                    && retained.size() >= limit) {
                break;
            }
            retained.put(
                    result.file.path,
                    Math.max(
                            retained.getOrDefault(result.file.path, 0f),
                            result.score
                    )
            );
        }
        return retained;
    }

    private static void addComponentEvidence(
            Map<String, CandidateAccumulator> candidates,
            Map<String, Float> componentResults,
            InterpretationExecutionResult.Modality modality,
            int componentIndex
    ) {
        float componentBest = 0f;
        for (float score : componentResults.values()) {
            componentBest = Math.max(componentBest, score);
        }
        for (Map.Entry<String, Float> entry : componentResults.entrySet()) {
            String key = modality + "\u0000" + entry.getKey();
            CandidateAccumulator candidate = candidates.get(key);
            if (candidate == null) {
                candidate = new CandidateAccumulator(entry.getKey(), modality);
                candidates.put(key, candidate);
            }
            float normalized = componentBest <= 0f
                    ? 0f
                    : Math.max(0f, Math.min(1f, entry.getValue() / componentBest));
            candidate.add(componentIndex, normalized);
        }
    }

    private static InterpretationExecutionResult emptyResult(
            Interpretation interpretation,
            long startedNanos
    ) {
        return new InterpretationExecutionResult(
                interpretation.getId(),
                interpretation.getKind(),
                STRATEGY_ID,
                Collections.emptyList(),
                0f,
                0,
                0,
                0,
                elapsedMillis(startedNanos),
                false
        );
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private static final class CandidateAccumulator {
        final String path;
        final InterpretationExecutionResult.Modality modality;
        float normalizedScoreSum;
        int supportCount;
        float agreementScore;
        int lastComponentIndex = -1;

        CandidateAccumulator(
                String path,
                InterpretationExecutionResult.Modality modality
        ) {
            this.path = path;
            this.modality = modality;
        }

        void add(int componentIndex, float normalizedScore) {
            if (lastComponentIndex != componentIndex) {
                normalizedScoreSum += normalizedScore;
                supportCount++;
                lastComponentIndex = componentIndex;
            }
        }

        void finish(int componentCount) {
            agreementScore = AtomicConjunctionStrategy.agreementScore(
                    supportCount,
                    componentCount,
                    normalizedScoreSum
            );
        }
    }
}
