package com.bliss.aimemorysearch.ai.explanation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.bliss.aimemorysearch.ai.concepts.Interpretation;
import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExplanationModelTest {

    @Test
    public void modelClassesAndFieldsAreFinal() {
        Class<?>[] modelClasses = {
                ExplanationInput.class,
                Explanation.class,
                QuerySummary.class,
                InterpretationSummary.class,
                SelectionSummary.class,
                ResultExplanation.class,
                AgreementPresentation.class,
                ConfidencePresentation.class,
                EvidenceReference.class,
                MissingEvidenceItem.class,
                GenerationMetadata.class
        };

        for (Class<?> modelClass : modelClasses) {
            assertTrue(Modifier.isFinal(modelClass.getModifiers()));
            for (Field field : modelClass.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    assertTrue(
                            modelClass.getSimpleName() + "." + field.getName(),
                            Modifier.isFinal(field.getModifiers())
                    );
                }
            }
        }
    }

    @Test
    public void inputDefensivelyCopiesNestedCollections() {
        EvidenceReference evidence = evidence("e1");
        List<EvidenceReference> sourceEvidence = new ArrayList<>();
        sourceEvidence.add(evidence);
        Map<String, List<EvidenceReference>> evidenceMap =
                new LinkedHashMap<>();
        evidenceMap.put("r1", sourceEvidence);
        List<ResultExplanation> results = new ArrayList<>();
        results.add(result(evidence));

        ExplanationInput input = new ExplanationInput(
                "search-1",
                "1",
                new QuerySummary("q1", "white cat", true),
                results,
                interpretation(),
                new ArrayList<>(),
                selection(),
                evidenceMap,
                new LinkedHashMap<>(),
                new LinkedHashMap<>()
        );

        results.clear();
        sourceEvidence.clear();
        evidenceMap.clear();

        assertEquals(1, input.getOrderedResults().size());
        assertEquals(1, input.getEvidenceByResultId().get("r1").size());
        assertUnsupported(() -> input.getOrderedResults().clear());
        assertUnsupported(
                () -> input.getEvidenceByResultId().get("r1").clear()
        );
        assertUnsupported(() -> input.getEvidenceByResultId().clear());
    }

    @Test
    public void explanationDefensivelyCopiesAllCollections() {
        EvidenceReference evidence = evidence("e1");
        List<ResultExplanation> results =
                new ArrayList<>(Collections.singletonList(result(evidence)));
        List<EvidenceReference> evidenceSummary =
                new ArrayList<>(Collections.singletonList(evidence));
        List<String> limitations = new ArrayList<>(
                Collections.singletonList("observational only")
        );

        Explanation explanation = new Explanation(
                "explanation-1",
                "search-1",
                "1",
                Explanation.Availability.AVAILABLE,
                new QuerySummary("q1", "white cat", true),
                interpretation(),
                new ArrayList<>(),
                selection(),
                results,
                evidenceSummary,
                new ArrayList<>(),
                limitations,
                new GenerationMetadata(1L, "1", null)
        );

        results.clear();
        evidenceSummary.clear();
        limitations.clear();

        assertEquals(1, explanation.getResultExplanations().size());
        assertEquals(1, explanation.getEvidenceSummary().size());
        assertEquals(
                Collections.singletonList("observational only"),
                explanation.getLimitations()
        );
        assertUnsupported(() -> explanation.getResultExplanations().clear());
        assertUnsupported(() -> explanation.getEvidenceSummary().clear());
        assertUnsupported(() -> explanation.getLimitations().clear());
    }

    @Test
    public void componentAndPresentationCollectionsAreDefensive() {
        List<String> components = new ArrayList<>(Arrays.asList("white", "cat"));
        InterpretationSummary interpretation = new InterpretationSummary(
                "i1",
                Interpretation.Kind.ATOMIC_CONJUNCTION,
                components,
                "atomic",
                false,
                true,
                true,
                0.9f,
                2,
                1,
                1
        );
        components.clear();
        assertEquals(Arrays.asList("white", "cat"), interpretation.getComponents());
        assertUnsupported(() -> interpretation.getComponents().clear());

        Map<String, EvidenceReference> signals = new LinkedHashMap<>();
        signals.put("semantic", evidence("e2"));
        ConfidencePresentation confidence = new ConfidencePresentation(
                true, 0.8f, "Strong", "1", signals
        );
        signals.clear();
        assertEquals(1, confidence.getNormalizedSignals().size());
        assertUnsupported(() -> confidence.getNormalizedSignals().clear());
    }

    private static InterpretationSummary interpretation() {
        return new InterpretationSummary(
                "i1",
                Interpretation.Kind.ATOMIC_CONJUNCTION,
                Arrays.asList("white", "cat"),
                "atomic",
                false,
                true,
                true,
                0.9f,
                1,
                1,
                0
        );
    }

    private static SelectionSummary selection() {
        return new SelectionSummary(1, 1, 0, "i1", null, true);
    }

    private static EvidenceReference evidence(String id) {
        return new EvidenceReference(
                id,
                "r1",
                EvidenceReference.Category.SEMANTIC,
                "DocumentEvidence",
                "bestSemanticScore",
                EvidenceReference.Scope.RESULT,
                true,
                0.7d,
                0.8f,
                Collections.singletonList("original"),
                "i1",
                "1"
        );
    }

    private static ResultExplanation result(EvidenceReference evidence) {
        return new ResultExplanation(
                "r1",
                "/document.pdf",
                InterpretationExecutionResult.Modality.DOCUMENT,
                1,
                0.9f,
                "i1",
                null,
                null,
                Collections.singletonList(evidence),
                Collections.singletonList("original"),
                Collections.emptyList(),
                ResultExplanation.Completeness.COMPLETE
        );
    }

    private static void assertUnsupported(Runnable operation) {
        try {
            operation.run();
            fail("Expected an immutable collection");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }
}
