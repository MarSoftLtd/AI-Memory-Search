package com.bliss.aimemorysearch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AIPackagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_ai_packages,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView aiPackageList =
                view.findViewById(R.id.aiPackagesList);

        aiPackageList.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        aiPackageList.setAdapter(
                new AIPackageAdapter(createRows())
        );

        aiPackageList.setAlpha(0f);
        aiPackageList.setTranslationY(
                getResources().getDimension(R.dimen.spacing_16)
        );
        aiPackageList.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(
                        getResources().getInteger(R.integer.duration_medium)
                )
                .start();
    }

    private List<AIPackageRow> createRows() {
        List<AIPackageRow> rows = new ArrayList<>();

        rows.add(AIPackageRow.header());
        rows.add(
                AIPackageRow.sectionHeader(
                        getString(R.string.ai_package_section_installed),
                        R.dimen.spacing_32
                )
        );
        for (AIPackageUiModel aiPackage : createInstalledAIPackages()) {
            rows.add(AIPackageRow.aiPackage(aiPackage));
        }
        rows.add(
                AIPackageRow.sectionHeader(
                        getString(R.string.ai_package_section_available),
                        R.dimen.spacing_24
                )
        );
        for (AIPackageUiModel aiPackage : createAvailableAIPackages()) {
            rows.add(AIPackageRow.aiPackage(aiPackage));
        }
        rows.add(
                AIPackageRow.sectionHeader(
                        getString(R.string.ai_package_section_storage),
                        R.dimen.spacing_24
                )
        );
        rows.add(AIPackageRow.storageSummary());

        return rows;
    }

    private List<AIPackageUiModel> createInstalledAIPackages() {
        return Arrays.asList(
                new AIPackageUiModel(
                        "documents",
                        R.drawable.ic_ai_package_document,
                        getString(R.string.ai_package_document_title),
                        getString(R.string.ai_package_document_description),
                        AIPackageUiModel.State.READY,
                        true,
                        false,
                        null
                ),
                new AIPackageUiModel(
                        "images",
                        R.drawable.ic_ai_package_image,
                        getString(R.string.ai_package_image_title),
                        getString(R.string.ai_package_image_description),
                        AIPackageUiModel.State.INSTALLED,
                        true,
                        false,
                        null
                )
        );
    }

    private List<AIPackageUiModel> createAvailableAIPackages() {
        return Arrays.asList(
                new AIPackageUiModel(
                        "language",
                        R.drawable.ic_ai_package_language,
                        getString(R.string.ai_package_language_title),
                        getString(R.string.ai_package_language_description),
                        AIPackageUiModel.State.AVAILABLE,
                        false,
                        false,
                        null
                ),
                new AIPackageUiModel(
                        "voice",
                        R.drawable.ic_ai_package_voice,
                        getString(R.string.ai_package_voice_title),
                        getString(R.string.ai_package_voice_description),
                        AIPackageUiModel.State.AVAILABLE,
                        false,
                        false,
                        null
                )
        );
    }
}
