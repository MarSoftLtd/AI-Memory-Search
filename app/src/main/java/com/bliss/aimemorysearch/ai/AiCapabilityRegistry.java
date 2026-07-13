package com.bliss.aimemorysearch.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class AiCapabilityRegistry {

    private final Map<AiCapability, CapabilityDefinition> definitions;

    public AiCapabilityRegistry() {
        EnumMap<AiCapability, CapabilityDefinition> values =
                new EnumMap<>(
                        AiCapability.class
                );

        values.put(
                AiCapability.DOCUMENT_SEARCH,
                new CapabilityDefinition(
                        AiCapability.DOCUMENT_SEARCH,
                        AiPackageType.DOCUMENT,
                        "document-search",
                        true
                )
        );
        values.put(
                AiCapability.IMAGE_SEARCH,
                new CapabilityDefinition(
                        AiCapability.IMAGE_SEARCH,
                        AiPackageType.IMAGE,
                        "image-search",
                        true
                )
        );
        values.put(
                AiCapability.TRANSLATION,
                new CapabilityDefinition(
                        AiCapability.TRANSLATION,
                        AiPackageType.TRANSLATION,
                        "romance",
                        false
                )
        );
        values.put(
                AiCapability.OCR,
                new CapabilityDefinition(
                        AiCapability.OCR,
                        AiPackageType.OCR,
                        "ocr",
                        true
                )
        );
        values.put(
                AiCapability.SPEECH_RECOGNITION,
                new CapabilityDefinition(
                        AiCapability.SPEECH_RECOGNITION,
                        AiPackageType.SPEECH,
                        "speech-recognition",
                        false
                )
        );
        values.put(
                AiCapability.VOICE_OUTPUT,
                new CapabilityDefinition(
                        AiCapability.VOICE_OUTPUT,
                        AiPackageType.VOICE,
                        "voice-output",
                        false
                )
        );
        values.put(
                AiCapability.NLP,
                new CapabilityDefinition(
                        AiCapability.NLP,
                        AiPackageType.KNOWLEDGE,
                        "nlp",
                        true
                )
        );
        values.put(
                AiCapability.KNOWLEDGE_GRAPH,
                new CapabilityDefinition(
                        AiCapability.KNOWLEDGE_GRAPH,
                        AiPackageType.KNOWLEDGE,
                        "knowledge-graph",
                        false
                )
        );
        values.put(
                AiCapability.INTENT_DETECTION,
                new CapabilityDefinition(
                        AiCapability.INTENT_DETECTION,
                        AiPackageType.KNOWLEDGE,
                        "intent-detection",
                        true
                )
        );
        values.put(
                AiCapability.ENTITY_EXTRACTION,
                new CapabilityDefinition(
                        AiCapability.ENTITY_EXTRACTION,
                        AiPackageType.KNOWLEDGE,
                        "entity-extraction",
                        true
                )
        );

        definitions =
                Collections.unmodifiableMap(
                        values
                );
    }

    public CapabilityDefinition getDefinition(
            AiCapability capability
    ) {
        return definitions.get(
                capability
        );
    }

    public Map<AiCapability, CapabilityDefinition> getDefinitions() {
        return definitions;
    }

    public static final class CapabilityDefinition {

        private final AiCapability capability;
        private final AiPackageType packageType;
        private final String capabilityKey;
        private final boolean bundledInCore;

        public CapabilityDefinition(
                AiCapability capability,
                AiPackageType packageType,
                String capabilityKey,
                boolean bundledInCore
        ) {
            this.capability =
                    capability;
            this.packageType =
                    packageType;
            this.capabilityKey =
                    capabilityKey;
            this.bundledInCore =
                    bundledInCore;
        }

        public AiCapability getCapability() {
            return capability;
        }

        public AiPackageType getPackageType() {
            return packageType;
        }

        public String getCapabilityKey() {
            return capabilityKey;
        }

        public boolean isBundledInCore() {
            return bundledInCore;
        }
    }
}
