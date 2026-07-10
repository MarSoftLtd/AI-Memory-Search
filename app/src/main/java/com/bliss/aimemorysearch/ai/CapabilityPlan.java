package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CapabilityPlan {

    private final List<CapabilityRequirement> requirements;
    private final ExecutionMode executionMode;
    private final boolean canContinue;
    private final String message;

    public CapabilityPlan(
            List<CapabilityRequirement> requirements,
            ExecutionMode executionMode,
            boolean canContinue,
            String message
    ) {
        this.requirements =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                requirements == null
                                        ? Collections.emptyList()
                                        : requirements
                        )
                );
        this.executionMode =
                executionMode;
        this.canContinue =
                canContinue;
        this.message =
                message;
    }

    public List<CapabilityRequirement> getRequirements() {
        return requirements;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public boolean canContinue() {
        return canContinue;
    }

    public String getMessage() {
        return message;
    }

    public boolean isComplete() {
        for (CapabilityRequirement requirement : requirements) {
            if (!requirement.isSatisfied()) {
                return false;
            }
        }

        return true;
    }

    public List<CapabilityRequirement> getMissingRequirements() {
        List<CapabilityRequirement> missing =
                new ArrayList<>();

        for (CapabilityRequirement requirement : requirements) {
            if (!requirement.isSatisfied()) {
                missing.add(
                        requirement
                );
            }
        }

        return Collections.unmodifiableList(
                missing
        );
    }
}
