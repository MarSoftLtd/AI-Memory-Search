package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiCapabilityResolver {

    private final AiCapabilityRegistry registry;
    private final AiPackageRepository packageRepository;

    public AiCapabilityResolver(
            AiCapabilityRegistry registry,
            AiPackageRepository packageRepository
    ) {
        this.registry =
                registry;
        this.packageRepository =
                packageRepository;
    }

    public CapabilityRequirement resolve(
            AiCapability capability
    ) {
        AiCapabilityRegistry.CapabilityDefinition definition =
                registry.getDefinition(
                        capability
                );

        if (definition == null) {
            return new CapabilityRequirement(
                    capability,
                    CapabilityStatus.UNKNOWN,
                    Collections.emptyList(),
                    null,
                    true,
                    "Capability is not registered"
            );
        }

        if (definition.isBundledInCore()) {
            return new CapabilityRequirement(
                    capability,
                    CapabilityStatus.AVAILABLE,
                    Collections.emptyList(),
                    null,
                    true,
                    "Capability is available in the core application"
            );
        }

        List<AiPackageInfo> candidates =
                findCandidatePackages(
                        definition
                );

        return new CapabilityRequirement(
                capability,
                candidates.isEmpty()
                        ? CapabilityStatus.MISSING
                        : CapabilityStatus.INSTALL_REQUIRED,
                candidates,
                candidates.isEmpty()
                        ? null
                        : candidates.get(0),
                true,
                candidates.isEmpty()
                        ? "No package is currently known for this capability"
                        : "Capability requires an installable AI package"
        );
    }

    public List<CapabilityRequirement> resolve(
            List<AiCapability> capabilities
    ) {
        if (capabilities == null || capabilities.isEmpty()) {
            return Collections.emptyList();
        }

        List<CapabilityRequirement> requirements =
                new ArrayList<>();

        for (AiCapability capability : capabilities) {
            if (capability == null) {
                continue;
            }

            requirements.add(
                    resolve(
                            capability
                    )
            );
        }

        return Collections.unmodifiableList(
                requirements
        );
    }

    private List<AiPackageInfo> findCandidatePackages(
            AiCapabilityRegistry.CapabilityDefinition definition
    ) {
        if (packageRepository == null) {
            return Collections.emptyList();
        }

        List<AiPackageInfo> packages =
                packageRepository.getPackages();

        if (packages == null || packages.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiPackageInfo> candidates =
                new ArrayList<>();

        for (AiPackageInfo packageInfo : packages) {
            if (packageInfo == null) {
                continue;
            }

            if (
                    definition.getPackageType() == packageInfo.getPackageType()
                            &&
                            definition.getCapabilityKey().equals(
                                    packageInfo.getCapabilityKey()
                            )
            ) {
                candidates.add(
                        packageInfo
                );
            }
        }

        return Collections.unmodifiableList(
                candidates
        );
    }
}
