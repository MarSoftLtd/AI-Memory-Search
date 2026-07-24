package com.bliss.aimemorysearch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator;
import com.bliss.aimemorysearch.ai.TranslationPackageManager;
import com.bliss.aimemorysearch.ai.model.AIPackageInfo;

import java.util.ArrayList;
import java.util.List;

/** Displays the real installed and available offline AI package catalog. */
public class AIPackagesFragment extends Fragment {
    private RecyclerView packageList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_ai_packages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        packageList = view.findViewById(R.id.aiPackagesList);
        packageList.setLayoutManager(new LinearLayoutManager(requireContext()));
        bindCurrentState();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindCurrentState();
    }

    private void bindCurrentState() {
        if (packageList == null || !isAdded()) return;
        packageList.setAdapter(new AIPackageAdapter(createRows()));
    }

    private List<AIPackageRow> createRows() {
        List<AIPackageUiModel> installed = new ArrayList<>();
        List<AIPackageUiModel> available = new ArrayList<>();
        TranslationPackageManager translations =
                TranslationPackageManager.getInstance(requireContext());
        SharedPreferences operation = requireContext().getSharedPreferences(
                "pending_search_state", Context.MODE_PRIVATE);
        String activeOperationId = operation.getString("package_operation_id", "");
        String reconciliationId = operation.getString("reconciliation_package_id", "");

        AiPackageBundleCoordinator bundles = new AiPackageBundleCoordinator(requireContext());
        AIPackageInfo searchBundle = bundles.createPresentationInfo(
                AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID);
        if (searchBundle != null) {
            boolean bundleInstalled = bundles.isInstalled(
                    AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID);
            addByState(bundleInstalled ? installed : available,
                    toUiModel(searchBundle,
                            bundleInstalled
                                    ? AIPackageUiModel.State.ACTIVE
                                    : AIPackageUiModel.State.AVAILABLE,
                            R.drawable.ic_ai_package_document));
        }

        for (AIPackageInfo info : translations.getAvailablePackageMetadata()) {
            boolean isInstalled = translations.isInstalled(info.getPackageId());
            AIPackageUiModel.State state;
            if (info.getPackageId().equals(reconciliationId)) {
                state = AIPackageUiModel.State.PREPARING;
            } else if (info.getPackageId().equals(activeOperationId)) {
                state = AIPackageUiModel.State.DOWNLOADING;
            } else {
                state = isInstalled
                        ? AIPackageUiModel.State.INSTALLED
                        : AIPackageUiModel.State.AVAILABLE;
            }
            addByState(isInstalled ? installed : available,
                    toUiModel(info, state, R.drawable.ic_ai_package_language));
        }

        List<AIPackageRow> rows = new ArrayList<>();
        rows.add(AIPackageRow.header());
        if (!installed.isEmpty()) {
            rows.add(AIPackageRow.sectionHeader(
                    getString(R.string.ai_package_section_installed), R.dimen.spacing_32));
            for (AIPackageUiModel model : installed) rows.add(AIPackageRow.aiPackage(model));
        }
        if (!available.isEmpty()) {
            rows.add(AIPackageRow.sectionHeader(
                    getString(R.string.ai_package_section_available), R.dimen.spacing_24));
            for (AIPackageUiModel model : available) rows.add(AIPackageRow.aiPackage(model));
        }
        return rows;
    }

    private void addByState(List<AIPackageUiModel> target, AIPackageUiModel model) {
        target.add(model);
    }

    private AIPackageUiModel toUiModel(
            AIPackageInfo info,
            AIPackageUiModel.State state,
            int icon
    ) {
        boolean installed = state == AIPackageUiModel.State.INSTALLED
                || state == AIPackageUiModel.State.ACTIVE
                || state == AIPackageUiModel.State.PREPARING;
        return new AIPackageUiModel(
                info.getPackageId(),
                icon,
                resolve(info.getDisplayNameKey(), info.getPackageId()),
                resolve(info.getDescriptionKey(), info.getVersion()),
                state,
                installed,
                false,
                null);
    }

    private String resolve(String resourceName, String fallback) {
        if (resourceName != null && !resourceName.isEmpty()) {
            int id = getResources().getIdentifier(
                    resourceName, "string", requireContext().getPackageName());
            if (id != 0) return getString(id);
        }
        return fallback == null ? "" : fallback;
    }
}
