package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiCapabilityManager {

    private final AiCapabilityResolver resolver;

    public AiCapabilityManager(
            AiCapabilityResolver resolver
    ) {
        this.resolver =
                resolver;
    }

    public static AiCapabilityManager createDefault() {
        return new AiCapabilityManager(
                new AiCapabilityResolver(
                        new AiCapabilityRegistry(),
                        new EmptyAiPackageRepository(),
                        AiPlatform.getPackageManager()
                )
        );
    }

    public CapabilityPlan evaluate(
            SearchRequest request,
            SearchAnalysis analysis
    ) {
        return evaluate(
                inferCapabilities(
                        request,
                        analysis
                )
        );
    }

    public CapabilityPlan evaluate(
            List<AiCapability> capabilities
    ) {
        List<CapabilityRequirement> requirements =
                resolver.resolve(
                        capabilities
                );

        boolean canContinue =
                canContinue(
                        requirements
                );

        return new CapabilityPlan(
                requirements,
                canContinue
                        ? ExecutionMode.FULL
                        : ExecutionMode.BLOCKED,
                canContinue,
                canContinue
                        ? "All required capabilities are available"
                        : "One or more required capabilities are unavailable"
        );
    }

    private static List<AiCapability> inferCapabilities(
            SearchRequest request,
            SearchAnalysis analysis
    ) {
        List<AiCapability> capabilities =
                new ArrayList<>();

        capabilities.add(
                AiCapability.NLP
        );
        capabilities.add(
                AiCapability.INTENT_DETECTION
        );
        capabilities.add(
                AiCapability.ENTITY_EXTRACTION
        );

        if (
                analysis == null
                        ||
                        analysis.isDocumentIntent()
        ) {
            capabilities.add(
                    AiCapability.DOCUMENT_SEARCH
            );
        }

        if (
                analysis == null
                        ||
                        analysis.isImageIntent()
                        ||
                        !analysis.isDocumentIntent()
        ) {
            capabilities.add(
                    AiCapability.IMAGE_SEARCH
            );
        }

        if (
                request != null
                        &&
                        request.getTranslatedQuery() != null
                        &&
                        !request.getTranslatedQuery().trim().isEmpty()
                        &&
                        !request.getTranslatedQuery().equals(
                                request.getOriginalQuery()
                        )
        ) {
            capabilities.add(
                    AiCapability.TRANSLATION
            );
        }

        return Collections.unmodifiableList(
                capabilities
        );
    }

    private static boolean canContinue(
            List<CapabilityRequirement> requirements
    ) {
        for (CapabilityRequirement requirement : requirements) {
            if (
                    requirement.isRequired()
                            &&
                            !requirement.isSatisfied()
            ) {
                return false;
            }
        }

        return true;
    }

    private static final class EmptyAiPackageRepository
            implements AiPackageRepository {

        @Override
        public List<AiPackageInfo> getPackages() {
            return Collections.emptyList();
        }
    }
}
