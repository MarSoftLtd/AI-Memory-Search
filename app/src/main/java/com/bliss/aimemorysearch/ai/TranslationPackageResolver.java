package com.bliss.aimemorysearch.ai;

import java.util.List;

public final class TranslationPackageResolver {

    private final TranslationPackageRepository repository;

    public TranslationPackageResolver(
            TranslationPackageRepository repository
    ) {
        this.repository =
                repository;
    }

    public TranslationPackageInfo resolve(
            TranslationModelId modelId
    ) {
        TranslationModelInfo modelInfo =
                TranslationModelRegistry.getModel(
                        modelId
                );

        List<TranslationPackageInfo> packages =
                repository.getPackages();

        for (TranslationPackageInfo packageInfo : packages) {
            if (
                    modelInfo.getTranslationFamily().equals(
                            packageInfo.getTranslationFamily()
                    )
            ) {
                return packageInfo;
            }
        }

        throw new IllegalStateException(
                "No translation package found for family: "
                        + modelInfo.getTranslationFamily()
        );
    }
}
