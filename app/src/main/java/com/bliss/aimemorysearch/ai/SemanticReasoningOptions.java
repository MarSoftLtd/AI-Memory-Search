package com.bliss.aimemorysearch.ai;

public final class SemanticReasoningOptions {

    private final boolean includeSameAs;
    private final boolean includeParentRelations;
    private final boolean includeChildRelations;
    private final boolean includeRelatedRelations;
    private final int maxReasoningDepth;
    private final int maxExpandedConcepts;
    private final boolean allowCycles;
    private final boolean includeOriginalConcept;

    public SemanticReasoningOptions(
            boolean includeSameAs,
            boolean includeParentRelations,
            boolean includeChildRelations,
            boolean includeRelatedRelations,
            int maxReasoningDepth,
            int maxExpandedConcepts,
            boolean allowCycles,
            boolean includeOriginalConcept
    ) {
        this.includeSameAs =
                includeSameAs;
        this.includeParentRelations =
                includeParentRelations;
        this.includeChildRelations =
                includeChildRelations;
        this.includeRelatedRelations =
                includeRelatedRelations;
        this.maxReasoningDepth =
                maxReasoningDepth;
        this.maxExpandedConcepts =
                maxExpandedConcepts;
        this.allowCycles =
                allowCycles;
        this.includeOriginalConcept =
                includeOriginalConcept;
    }

    public boolean isIncludeSameAs() {
        return includeSameAs;
    }

    public boolean isIncludeParentRelations() {
        return includeParentRelations;
    }

    public boolean isIncludeChildRelations() {
        return includeChildRelations;
    }

    public boolean isIncludeRelatedRelations() {
        return includeRelatedRelations;
    }

    public int getMaxReasoningDepth() {
        return maxReasoningDepth;
    }

    public int getMaxExpandedConcepts() {
        return maxExpandedConcepts;
    }

    public boolean isAllowCycles() {
        return allowCycles;
    }

    public boolean isIncludeOriginalConcept() {
        return includeOriginalConcept;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean includeSameAs =
                true;
        private boolean includeParentRelations =
                true;
        private boolean includeChildRelations =
                true;
        private boolean includeRelatedRelations =
                true;
        private int maxReasoningDepth =
                1;
        private int maxExpandedConcepts =
                50;
        private boolean allowCycles =
                false;
        private boolean includeOriginalConcept =
                true;

        public Builder includeSameAs(
                boolean includeSameAs
        ) {
            this.includeSameAs =
                    includeSameAs;
            return this;
        }

        public Builder includeParentRelations(
                boolean includeParentRelations
        ) {
            this.includeParentRelations =
                    includeParentRelations;
            return this;
        }

        public Builder includeChildRelations(
                boolean includeChildRelations
        ) {
            this.includeChildRelations =
                    includeChildRelations;
            return this;
        }

        public Builder includeRelatedRelations(
                boolean includeRelatedRelations
        ) {
            this.includeRelatedRelations =
                    includeRelatedRelations;
            return this;
        }

        public Builder maxReasoningDepth(
                int maxReasoningDepth
        ) {
            this.maxReasoningDepth =
                    maxReasoningDepth;
            return this;
        }

        public Builder maxExpandedConcepts(
                int maxExpandedConcepts
        ) {
            this.maxExpandedConcepts =
                    maxExpandedConcepts;
            return this;
        }

        public Builder allowCycles(
                boolean allowCycles
        ) {
            this.allowCycles =
                    allowCycles;
            return this;
        }

        public Builder includeOriginalConcept(
                boolean includeOriginalConcept
        ) {
            this.includeOriginalConcept =
                    includeOriginalConcept;
            return this;
        }

        public SemanticReasoningOptions build() {
            return new SemanticReasoningOptions(
                    includeSameAs,
                    includeParentRelations,
                    includeChildRelations,
                    includeRelatedRelations,
                    maxReasoningDepth,
                    maxExpandedConcepts,
                    allowCycles,
                    includeOriginalConcept
            );
        }
    }
}
