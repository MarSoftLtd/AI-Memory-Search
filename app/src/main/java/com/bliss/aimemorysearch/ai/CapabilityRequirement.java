package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CapabilityRequirement {

    private final AiCapability capability;
    private final CapabilityStatus status;
    private final List<AiPackageInfo> candidatePackages;
    private final AiPackageInfo selectedPackage;
    private final boolean required;
    private final String reason;

    public CapabilityRequirement(
            AiCapability capability,
            CapabilityStatus status,
            List<AiPackageInfo> candidatePackages,
            AiPackageInfo selectedPackage,
            boolean required,
            String reason
    ) {
        this.capability =
                capability;
        this.status =
                status;
        this.candidatePackages =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                candidatePackages == null
                                        ? Collections.emptyList()
                                        : candidatePackages
                        )
                );
        this.selectedPackage =
                selectedPackage;
        this.required =
                required;
        this.reason =
                reason;
    }

    public AiCapability getCapability() {
        return capability;
    }

    public CapabilityStatus getStatus() {
        return status;
    }

    public List<AiPackageInfo> getCandidatePackages() {
        return candidatePackages;
    }

    public AiPackageInfo getSelectedPackage() {
        return selectedPackage;
    }

    public boolean isRequired() {
        return required;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSatisfied() {
        return status == CapabilityStatus.AVAILABLE;
    }

    public boolean requiresInstallation() {
        return status == CapabilityStatus.INSTALL_REQUIRED
                || status == CapabilityStatus.MISSING;
    }
}
