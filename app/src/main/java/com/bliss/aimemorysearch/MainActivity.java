package com.bliss.aimemorysearch;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bliss.aimemorysearch.ai.E5EmbeddingEngine;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.bliss.aimemorysearch.ai.ImageRuntimeLoader;
import com.bliss.aimemorysearch.ai.concept.ConceptSimilarityTest;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.List;
import android.provider.Settings;
import android.content.Intent;

import com.bliss.aimemorysearch.ai.MiniLMTokenizer;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String PENDING_SEARCH_STATE = "pending_search_state";
    private static final String PENDING_QUERY = "pending_query";
    private static final String PENDING_LANGUAGE = "pending_language";
    private static final String PENDING_FAMILY = "pending_family";
    private static final String RECONCILIATION_FAMILY = "reconciliation_family";
    private static final String RECONCILIATION_PACKAGE_ID = "reconciliation_package_id";
    private static final String RECONCILIATION_WORK_ID = "reconciliation_work_id";
    private static final String PACKAGE_OPERATION_FAMILY = "package_operation_family";
    private static final String PACKAGE_OPERATION_ID = "package_operation_id";
    private EditText searchEdit;
    private CardView searchCard;
    private AppDatabase database;
    private SharedPreferences prefs;
    private int ocrProcessedCount = 0;
    private int ocrStartedCount = 0;
    private android.widget.Button btnGrantAccess;
    private CardView permissionCard;
    private final Handler uiHandler =
    new Handler( Looper.getMainLooper());
    private View dimView;
    private ImageView clearSearch;
    private ImageView searchButton;
    private View searchPreparationPanel;
    private TextView searchPreparationText;
    private CardView exitCard;
    private TextView btnYesExit;
    private TextView btnNoExit;
    private boolean exitVisible = false;
    private TextView indexedCountText;
    private TextView pendingCountText;
    private TextView failedCountText;
    private TextView lastScanText;
    private TextView liveIndexingText;
    private TextView pdfCountText;
    private TextView jpgCountText;
    private TextView ocrCountText;
    private TextView embeddingCountText;
    private ProgressBar pdfGauge;
    private ProgressBar jpgGauge;
    private ProgressBar ocrGauge;
    private ProgressBar embeddingGauge;
    private TextView titleText_unu;
    private TextView subtitleText_unu;
    private TextView speedText;
    private TextView etaText;
    private TextView stageText;
    private TextView indexLiveInfoText;
    private TextView indexLongFileWarningText;
    private TextView indexStateText;
    private TextView indexOverallProgressText;
    private com.google.android.material.progressindicator.LinearProgressIndicator
            indexOverallProgress;
    private View indexLiveInfoPanel;
    private View indexLongFileActions;
    private View indexLongFileWarningCard;
    private String pendingIndexDecisionToken = "";
    private long indexingStartTime = 0L;
    private View indexLoader;
    private com.bliss.aimemorysearch.ai.LanguagePackageRouter languagePackageRouter;
    private com.bliss.aimemorysearch.ai.SearchRequest pendingSearchRequest;
    private String selectedLanguageFamily = "";
    private boolean languagePackagePromptVisible = false;
    private com.bliss.aimemorysearch.ui.AIPackageDialog aiPackageDialog;
    private com.bliss.aimemorysearch.ai.AiPackageDownloadManager aiPackageDownloadManager;
    private com.bliss.aimemorysearch.ai.AtomicAiPackageInstaller aiPackageInstaller;
    private com.bliss.aimemorysearch.ai.model.AIPackageInfo activeAiPackageInfo;
    private com.bliss.aimemorysearch.ai.TranslationPackageManager
            translationPackageManager;
    private com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator aiSearchBundleCoordinator;
    private boolean aiSearchReady;
    private boolean searchWorkflowReady;
    private boolean initialLanguagePackageReady;
    private boolean startupPackageOperationActive;
    private boolean startupRuntimeActivationActive;
    private boolean initialIndexOperationActive;
    private java.util.List<com.bliss.aimemorysearch.ai.model.AIPackageInfo>
            activeBundlePackages = java.util.Collections.emptyList();
    private int activeBundlePackageIndex;
    private long activeBundleCompletedBytes;
    private long activeBundleTotalBytes;
    private com.bliss.aimemorysearch.ai.model.AIPackageInfo activeDownloadPackageInfo;
    private androidx.lifecycle.LiveData<androidx.work.WorkInfo>
            reconciliationWorkLiveData;
    private boolean reconciliationRecoveryInFlight;
    private boolean indexObserverRegistered;
    private boolean indexWorkActive;
    private androidx.appcompat.app.AlertDialog firstIndexExplanationDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.bliss.aimemorysearch.workers.CanonicalEnrichmentLifecycle.initialize(this);
//====================================================================================================
        // validateSentencePiece();
//====================================================================================================
        // Model self-tests run only after repository-backed activation.
//====================================================================================================
        // AI runtimes are restored after the package UI is available.
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows( getWindow(), false );
        setContentView(R.layout.activity_main);
        aiPackageDialog =
                new com.bliss.aimemorysearch.ui.AIPackageDialog(
                        findViewById(R.id.aiPackageCard)
                );
        aiPackageDownloadManager =
                new com.bliss.aimemorysearch.ai.AiPackageDownloadManager(this);
        aiPackageInstaller = new com.bliss.aimemorysearch.ai.AtomicAiPackageInstaller(this);
        translationPackageManager =
                com.bliss.aimemorysearch.ai.TranslationPackageManager
                        .getInstance(this);
        aiSearchBundleCoordinator =
                new com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator(this);
        initializeLanguagePackageRouting();
        aiSearchReady = aiSearchBundleCoordinator.isInstalled(
                com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID)
                && aiSearchBundleCoordinator.activate(
                com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID);
        database = AppDatabase.getInstance(this);
        rebuildVocabularyCache();
        prefs = getSharedPreferences( "index_state", MODE_PRIVATE );
        btnGrantAccess = findViewById(R.id.btnGrantAccess);
        permissionCard = findViewById(R.id.permissionCard);
        searchEdit = findViewById(R.id.searchEdit);
        searchCard = findViewById(R.id.searchCard);
        titleText_unu = findViewById( R.id.titleText_unu );
        subtitleText_unu = findViewById( R.id.subtitleText_unu );
        clearSearch = findViewById(R.id.clearSearch);
        searchButton = findViewById(R.id.searchButton);
        searchPreparationPanel = findViewById(R.id.searchPreparationPanel);
        searchPreparationText = findViewById(R.id.searchPreparationText);
        exitCard = findViewById(R.id.exitCard);
        dimView = findViewById( R.id.dimView );
        btnYesExit = findViewById(R.id.btnYesExit);
        btnNoExit = findViewById(R.id.btnNoExit);
        indexedCountText =
                findViewById(
                        R.id.indexedCountText
                );
        
        pdfCountText =
                findViewById(
                        R.id.pdfCountText
                );

        jpgCountText =
                findViewById(
                        R.id.jpgCountText
                );

        ocrCountText =
                findViewById(
                        R.id.ocrCountText
                );

        embeddingCountText =
                findViewById(
                        R.id.embeddingCountText
                );
        speedText =
                findViewById(
                        R.id.speedText
                );

        etaText =
                findViewById(
                        R.id.etaText
                );

        stageText =
                findViewById(
                        R.id.stageText
                );
        pdfGauge =
                findViewById(
                        R.id.pdfGauge
                );

        jpgGauge =
                findViewById(
                        R.id.jpgGauge
                );

        ocrGauge =
                findViewById(
                        R.id.ocrGauge
                );

        embeddingGauge =
                findViewById(
                        R.id.embeddingGauge
                );
        liveIndexingText =
                findViewById(
                        R.id.liveIndexingText
                );
        indexLoader =
                findViewById(
                        R.id.indexLoader
                );
        indexedCountText.setText("—");
        pdfCountText.setText(R.string.dashboard_synchronizing);
        jpgCountText.setText(R.string.dashboard_synchronizing);
        ocrCountText.setText(R.string.dashboard_synchronizing);
        embeddingCountText.setText(R.string.dashboard_synchronizing);
        setupSearch();
        showLastIndexInfo();
        btnGrantAccess.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                        },
                        STORAGE_PERMISSION_CODE
                );

            } else {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_CODE
                );
            }
        });
        btnNoExit.setOnClickListener(v -> {
            hideExitDialog();
        });
        btnYesExit.setOnClickListener(v -> {
            finishAffinity();
        });
        getOnBackPressedDispatcher().addCallback( this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (languagePackagePromptVisible) {
                            if (aiPackageDialog.canClose()) {
                                closeAiPackageDialog();
                            }
                            return;
                        }
                        if (exitVisible) {
                            hideExitDialog();
                            return;
                        }
                        showExitDialog();
                    }
                }
        );
        // =============================  AI
        if (aiSearchReady) {
            startModelDependentRuntime();
        }
        liveIndexingText =
                findViewById(
                        R.id.liveIndexingText
                );
        indexLiveInfoText = findViewById(R.id.indexLiveInfoText);
        indexLongFileWarningText = findViewById(R.id.indexLongFileWarningText);
        indexStateText = findViewById(R.id.indexStateText);
        indexOverallProgressText = findViewById(R.id.indexOverallProgressText);
        indexOverallProgress = findViewById(R.id.indexOverallProgress);
        indexLiveInfoPanel = findViewById(R.id.indexLiveInfoPanel);
        indexLongFileActions = findViewById(R.id.indexLongFileActions);
        indexLongFileWarningCard = findViewById(R.id.indexLongFileWarningCard);
        observeIndexWorker();
        observeIndexStats();
        findViewById(R.id.indexContinueButton).setOnClickListener(
                view -> submitIndexDecision("continue"));
        findViewById(R.id.indexSkipButton).setOnClickListener(
                view -> submitIndexDecision("skip"));
        restorePendingPackageLifecycle();
        uiHandler.post(this::continueStartupPreparation);

    }
    private void observeIndexStats() {

        AppDatabase database =
                AppDatabase.getInstance(this);

        database.fileDao()
                .getIndexedCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }

                    indexedCountText.setText(
                            String.valueOf(count)
                    );
                });

        database.fileDao()
                .getPdfCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }
                    if (indexWorkActive) return;

                    pdfCountText.setText(
                            getString(
                                    R.string.documents_count,
                                    count
                            )
                    );

                });

        database.fileDao()
                .getImageCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }
                    if (indexWorkActive) return;

                    jpgCountText.setText(
                            getString(
                                    R.string.images_count,
                                    count
                            )
                    );

                });

        database.fileDao()
                .getOcrCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }
                    if (indexWorkActive) return;

                    ocrCountText.setText(
                            getString(
                                    R.string.text_detected_count,
                                    count
                            )
                    );

                });

        database.fileDao()
                .getEmbeddingCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }
                    if (indexWorkActive) return;

                    embeddingCountText.setText(
                            getString(
                                    R.string.ai_indexed_count,
                                    count
                            )
                    );

                });
    }
    private void setupSearch() {

        clearSearch.setVisibility(
                View.VISIBLE
        );

        searchButton.setVisibility(
                View.GONE
        );

        searchEdit.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s
                    ) {

                        String query =
                                s.toString().trim();

                        if (query.isEmpty()) {

                            clearSearch.setVisibility(
                                    View.VISIBLE
                            );

                            searchButton.setVisibility(
                                    View.GONE
                            );

                        } else {

                            clearSearch.setVisibility(
                                    View.GONE
                            );

                            searchButton.setVisibility(
                                    View.VISIBLE
                            );
                        }
                    }
                }
        );

        searchButton.setOnClickListener(v -> {

            hideKeyboard();

            searchEdit.clearFocus();

            String query =
                    searchEdit.getText()
                            .toString()
                            .trim();

            if (!query.isEmpty()) {

                searchInDatabase(query);

                searchButton.setVisibility(
                        View.GONE
                );

                clearSearch.setVisibility(
                        View.VISIBLE
                );
            }
        });

        clearSearch.setOnClickListener(v -> {

            searchEdit.setText("");

            hideKeyboard();

            searchEdit.clearFocus();

            clearSearch.setVisibility(
                    View.VISIBLE
            );

            searchButton.setVisibility(
                    View.GONE
            );
        });

        searchEdit.setOnEditorActionListener(
                (v, actionId, event) -> {

                    hideKeyboard();

                    searchEdit.clearFocus();

                    String query =
                            searchEdit.getText()
                                    .toString()
                                    .trim();

                    if (!query.isEmpty()) {

                        searchInDatabase(query);

                        searchButton.setVisibility(
                                View.GONE
                        );

                        clearSearch.setVisibility(
                                View.VISIBLE
                        );
                    }

                    return true;
                }
        );
    }
    private void hideKeyboard() {

        InputMethodManager imm =
                (InputMethodManager)
                        getSystemService(
                                INPUT_METHOD_SERVICE
                        );

        if (imm != null) {

            imm.hideSoftInputFromWindow(
                    searchEdit.getWindowToken(),
                    0
            );
        }
    }
    private void searchInDatabase(String query) {
        android.util.Log.e(
                "SEARCH_ENTRY",
                "MAINACTIVITY SEARCH CALLED = " + query
        );
        if (query == null || query.trim().isEmpty()) {

            return;
        }

        showSearchPreparation(getString(R.string.search_phase_detecting));

        com.bliss.aimemorysearch.ai.SearchRequest request =
                com.bliss.aimemorysearch.ai.QueryUnderstandingEngine
                        .createSearchRequest(query.trim());
        new Thread(() -> {
            com.bliss.aimemorysearch.ai.LanguageDetectionEngine.DetectionResult detection =
                    com.bliss.aimemorysearch.ai.LanguageDetectionEngine
                            .getInstance(this)
                            .detectLanguage(query.trim(), selectedLanguageFamily);
            android.util.Log.d("TRANSLATION_ROUTER",
                    "input | query=" + query.trim()
                            + " | detectedLanguage=" + detection.language
                            + " | confidence=" + detection.confidence
                            + " | fallbackFamily=" + selectedLanguageFamily);
            request.setDetectedLanguage(detection.language);
            request.setSelectedLanguageFamily(detection.family);
            android.util.Log.d("TRANSLATION_ROUTER",
                    "output | query=" + query.trim()
                            + " | family=" + detection.family);
            android.util.Log.d("MULTILINGUAL_PIPELINE",
                    "Query language=" + detection.language
                            + " | family=" + detection.family
                            + " | confidence=" + detection.confidence
                            + " | fallback=" + detection.fallback);
            runOnUiThread(() -> continueSearch(request));
        }).start();
    }

    private void continueStartupPreparation() {
        if (searchWorkflowReady || hasActiveAiPackageOperation()) {
            return;
        }

        String bundleId =
                com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator
                        .AI_SEARCH_BUNDLE_ID;
        if (!aiSearchBundleCoordinator.isInstalled(bundleId)) {
            showAiSearchBundlePrompt();
            return;
        }
        if (!aiSearchReady) {
            startupRuntimeActivationActive = true;
            refreshKeepScreenAwake();
            aiSearchReady = aiSearchBundleCoordinator.activate(bundleId);
            startupRuntimeActivationActive = false;
            refreshKeepScreenAwake();
            if (!aiSearchReady) {
                showAiSearchBundlePrompt();
                return;
            }
            startModelDependentRuntime();
        }

        String deviceFamily = languagePackageRouter.getRecommendedFamily(
                java.util.Locale.getDefault()
        );
        if (deviceFamily == null || deviceFamily.isEmpty()) {
            completeStartupPreparation();
            return;
        }

        selectedLanguageFamily = deviceFamily;
        getSharedPreferences("language_package_routing", MODE_PRIVATE)
                .edit()
                .putString("selected_family", deviceFamily)
                .apply();

        if (languagePackageRouter.isInstalled(deviceFamily)
                && languagePackageRouter.activateInstalledPackage(deviceFamily)) {
            completeStartupPreparation();
            return;
        }

        String packageId = languagePackageRouter.getRequiredPackageId(deviceFamily);
        showLanguagePackagePrompt(packageId, true);
        if (languagePackagePromptVisible && activeAiPackageInfo != null) {
            markInitialLanguagePromptShown(true);
            startAiPackageDownload();
        }
    }

    private void completeStartupPreparation() {
        languagePackageRouter.restoreInstalledPackages();
        initialLanguagePackageReady = true;
        if (hasStorageAccess()) {
            startIndexing();
        }
        updateStartupCompletion();
    }

    private void updateStartupCompletion() {
        boolean initialIndexComplete =
                prefs != null && prefs.getBoolean("first_index_done", false);
        boolean ready = aiSearchReady
                && initialLanguagePackageReady
                && !hasActiveAiPackageOperation()
                && initialIndexComplete;
        searchWorkflowReady = ready;
        searchCard.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (ready) {
            resumePendingSearch();
        }
    }

    private void refreshKeepScreenAwake() {
        boolean keepAwake = startupPackageOperationActive
                || startupRuntimeActivationActive
                || initialIndexOperationActive;
        if (keepAwake) {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void continueSearch(com.bliss.aimemorysearch.ai.SearchRequest request) {

        if (hasActiveAiPackageOperation()) {
            showSearchPreparation(getString(
                    persistedReconciliationWorkId().isEmpty()
                            ? R.string.search_phase_waiting_package
                            : R.string.search_phase_waiting_reconciliation,
                    request.getOriginalQuery()));
            return;
        }
        showSearchPreparation(getString(R.string.search_phase_capability));

        String aiSearchBundleId =
                com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID;
        if (!aiSearchBundleCoordinator.isInstalled(aiSearchBundleId)) {
            setPendingSearchRequest(request);
            showSearchPreparation(getString(
                    R.string.search_phase_waiting_package,
                    request.getOriginalQuery()));
            showAiSearchBundlePrompt();
            return;
        }
        if (!aiSearchBundleCoordinator.activate(aiSearchBundleId)) {
            setPendingSearchRequest(request);
            showAiSearchBundlePrompt();
            return;
        }

        String queryLanguageFamily = request.getSelectedLanguageFamily();
        if (!queryLanguageFamily.isEmpty()) {
            String requiredPackageId =
                    languagePackageRouter.getRequiredPackageId(
                            queryLanguageFamily
                    );
            if (
                    requiredPackageId != null
                            &&
                            !translationPackageManager.isInstalled(
                                    requiredPackageId
                            )
            ) {
                setPendingSearchRequest(request);
                showSearchPreparation(getString(
                        R.string.search_phase_waiting_package,
                        request.getOriginalQuery()));
                showLanguagePackagePrompt(requiredPackageId, false);
                return;
            }
            languagePackageRouter.activateInstalledPackage(
                    queryLanguageFamily
            );
        }

        executeSearch(request);
    }

    private void continueWithCollectionCoverage(
            com.bliss.aimemorysearch.ai.SearchRequest request) {
        showSearchPreparation(getString(R.string.search_phase_coverage));
        new Thread(() -> {
            String missingPackageId = null;
            int missingFiles = 0;
            String missingLanguage = null;
            try (com.bliss.aimemorysearch.ai.canonical.CanonicalIndexStore store =
                         new com.bliss.aimemorysearch.ai.canonical.CanonicalIndexStore(this)) {
                for (com.bliss.aimemorysearch.ai.canonical.CanonicalIndexStore.LanguageSummary
                        summary : store.languageSummaries()) {
                    if (!com.bliss.aimemorysearch.ai.canonical.CanonicalIndexingPipeline
                            .STATUS_PACKAGE_MISSING.equals(summary.canonicalStatus)) continue;
                    String packageId = languagePackageRouter.getRequiredPackageId(
                            summary.translationFamily);
                    if (packageId != null && !translationPackageManager.isInstalled(packageId)) {
                        missingPackageId = packageId;
                        missingFiles = summary.fileCount;
                        missingLanguage = summary.languageTag;
                        break;
                    }
                }
            }
            String finalPackageId = missingPackageId;
            int finalMissingFiles = missingFiles;
            String finalMissingLanguage = missingLanguage;
            runOnUiThread(() -> {
                if (finalPackageId == null) {
                    executeSearch(request);
                    return;
                }
                setPendingSearchRequest(request);
                android.widget.Toast.makeText(this,
                        getString(R.string.multilingual_collection_package_missing,
                                finalMissingFiles, finalMissingLanguage),
                        android.widget.Toast.LENGTH_LONG).show();
                showLanguagePackagePrompt(finalPackageId, false);
            });
        }).start();
    }

    private void executeSearch(
            com.bliss.aimemorysearch.ai.SearchRequest request
    ) {
        showSearchPreparation(getString(R.string.search_phase_searching));
        new SearchCoordinator(
                this
        ).search(
                request,
                new SearchCoordinator.ProgressCallback() {
                    @Override
                    public void onPhase(String phase) {
                        showSearchPreparation(phase);
                    }

                    @Override
                    public void onBusy() {
                        showSearchPreparation(getString(R.string.search_phase_busy));
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        showSearchPreparation(getString(R.string.search_phase_failed));
                    }
                },
                finalResults -> {
                    hideSearchPreparation();
                    for (FileEntity file : finalResults) {

                        android.util.Log.e(
                                "FINAL_RESULT",
                                file.name
                                        + " | "
                                        + file.type
                                        + " | score="
                                        + SearchExplanationHolder.scores.get(
                                        file.path
                                )
                        );
                    }
                    openSearchResults(
                            finalResults
                    );

                    android.util.Log.d(
                            "CHUNK_SEARCH",
                            "RESULTS = "
                                    + finalResults.size()
                    );
                }
        );
    }

    private void initializeLanguagePackageRouting() {
        languagePackageRouter =
                new com.bliss.aimemorysearch.ai.LanguagePackageRouter(this);
        languagePackageRouter.restoreInstalledPackages();

        SharedPreferences routingPreferences =
                getSharedPreferences(
                        "language_package_routing",
                        MODE_PRIVATE
                );
        selectedLanguageFamily =
                routingPreferences.getString(
                        "selected_family",
                        ""
                );

        if (selectedLanguageFamily == null || selectedLanguageFamily.isEmpty()) {
            selectedLanguageFamily =
                    languagePackageRouter.getRecommendedFamily(
                            java.util.Locale.getDefault()
                    );
            routingPreferences.edit()
                    .putString(
                            "selected_family",
                            selectedLanguageFamily
                    )
                    .apply();
        }

    }

    private void showLanguagePackagePrompt(
            String packageId,
            boolean initialPrompt
    ) {
        if (
                languagePackagePromptVisible
                        ||
                        packageId == null
                        ||
                        packageId.trim().isEmpty()
        ) {
            return;
        }

        com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata =
                translationPackageManager.getMetadata(packageId);
        if (metadata == null) {
            return;
        }
        languagePackagePromptVisible = true;
        activeAiPackageInfo = metadata;
        aiPackageDialog.bind(metadata);
        aiPackageDialog.showReadyState();
        configureAiPackageReadyActions();
        aiPackageDialog.show();
    }

    private void showAiPackagePrompt(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata
    ) {
        if (languagePackagePromptVisible || metadata == null) {
            return;
        }
        languagePackagePromptVisible = true;
        activeAiPackageInfo = metadata;
        aiPackageDialog.bind(metadata);
        aiPackageDialog.showReadyState();
        configureAiPackageReadyActions();
        aiPackageDialog.show();
    }

    private void showAiSearchBundlePrompt() {
        String bundleId =
                com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID;
        com.bliss.aimemorysearch.ai.model.AIPackageInfo presentation =
                aiSearchBundleCoordinator.createPresentationInfo(bundleId);
        java.util.List<com.bliss.aimemorysearch.ai.model.AIPackageInfo> missing =
                aiSearchBundleCoordinator.getMissingPackages(bundleId);
        if (missing.isEmpty() && !aiSearchBundleCoordinator.isActive(bundleId)) {
            missing = aiSearchBundleCoordinator.getPackages(bundleId);
        }
        if (presentation == null || missing.isEmpty() || languagePackagePromptVisible) {
            return;
        }
        activeBundlePackages = missing;
        activeBundlePackageIndex = 0;
        activeBundleCompletedBytes = 0L;
        activeBundleTotalBytes = 0L;
        for (com.bliss.aimemorysearch.ai.model.AIPackageInfo info : missing) {
            activeBundleTotalBytes += info.getDownloadSizeBytes();
        }
        showAiPackagePrompt(presentation);
    }

    private void configureAiPackageReadyActions() {
        aiPackageDialog.setPrimaryActionListener(v -> startAiPackageDownload());
        aiPackageDialog.setSecondaryActionListener(v -> closeAiPackageDialog());
    }

    private void startAiPackageDownload() {
        if (activeAiPackageInfo == null) {
            return;
        }
        startupPackageOperationActive = true;
        refreshKeepScreenAwake();
        if (activeAiPackageInfo.getPackageType()
                == com.bliss.aimemorysearch.ai.AiPackageType.TRANSLATION) {
            persistPackageOperation(activeAiPackageInfo);
        }
        if (!activeBundlePackages.isEmpty()) {
            startNextBundlePackageDownload();
            return;
        }
        activeDownloadPackageInfo = activeAiPackageInfo;
        aiPackageDialog.setPrimaryActionListener(v -> aiPackageDownloadManager.cancel());
        aiPackageDialog.setSecondaryActionListener(null);
        aiPackageDownloadManager.start(
                activeAiPackageInfo,
                this::handleAiPackageDownloadEvent
        );
    }

    private void startNextBundlePackageDownload() {
        if (activeBundlePackageIndex >= activeBundlePackages.size()) {
            finishAiSearchBundleInstallation();
            return;
        }
        activeDownloadPackageInfo = activeBundlePackages.get(activeBundlePackageIndex);
        aiPackageDialog.setPrimaryActionListener(v -> aiPackageDownloadManager.cancel());
        aiPackageDialog.setSecondaryActionListener(null);
        aiPackageDownloadManager.start(
                activeDownloadPackageInfo,
                this::handleAiPackageDownloadEvent
        );
    }

    private void handleAiPackageDownloadEvent(
            com.bliss.aimemorysearch.ai.AiPackageDownloadManager.DownloadEvent event
    ) {
        switch (event.getEvent()) {
            case STARTED:
                aiPackageDialog.showDownloadingState(
                        bundleProgress(0L),
                        activeBundleCompletedBytes,
                        bundleTotal(event.getTotalBytes()),
                        activeBundlePackages.isEmpty() ? 1 : activeBundlePackageIndex + 1,
                        activeBundlePackages.isEmpty() ? 1 : activeBundlePackages.size()
                );
                break;
            case PROGRESS:
                long totalBytes = event.getTotalBytes();
                int progress = totalBytes > 0L
                        ? (int) Math.min(100L, event.getDownloadedBytes() * 100L / totalBytes)
                        : 0;
                aiPackageDialog.showDownloadingState(
                        activeBundlePackages.isEmpty()
                                ? progress : bundleProgress(event.getDownloadedBytes()),
                        activeBundlePackages.isEmpty()
                                ? event.getDownloadedBytes()
                                : activeBundleCompletedBytes + event.getDownloadedBytes(),
                        bundleTotal(totalBytes),
                        activeBundlePackages.isEmpty() ? 1 : activeBundlePackageIndex + 1,
                        activeBundlePackages.isEmpty() ? 1 : activeBundlePackages.size()
                );
                break;
            case VERIFYING:
                aiPackageDialog.showVerifyingState();
                aiPackageDialog.setSecondaryActionListener(
                        v -> aiPackageDownloadManager.cancel()
                );
                break;
            case COMPLETED:
                installVerifiedAiPackage(event.getTemporaryFile());
                break;
            case CANCELLED:
                startupPackageOperationActive = false;
                refreshKeepScreenAwake();
                clearPackageOperationState();
                aiPackageDialog.showReadyState();
                configureAiPackageReadyActions();
                break;
            case FAILED:
                startupPackageOperationActive = false;
                refreshKeepScreenAwake();
                clearPackageOperationState();
                aiPackageDialog.showDownloadErrorState(
                        getString(downloadFailureMessage(event.getFailureReason()))
                );
                aiPackageDialog.setPrimaryActionListener(v -> closeAiPackageDialog());
                break;
        }
    }

    private void installVerifiedAiPackage(File packageFile) {
        com.bliss.aimemorysearch.ai.model.AIPackageInfo packageInfo =
                activeDownloadPackageInfo != null
                        ? activeDownloadPackageInfo : activeAiPackageInfo;
        if (packageInfo == null || packageFile == null) {
            showAiPackageInstallationError(
                    com.bliss.aimemorysearch.ai.AtomicAiPackageInstaller.FailureReason.INVALID_PACKAGE
            );
            return;
        }
        aiPackageDialog.showInstallingState();
        aiPackageDialog.setSecondaryActionListener(null);
        new Thread(() -> {
            final long[] lastProgressUpdate = {0L};
            com.bliss.aimemorysearch.ai.AtomicAiPackageInstaller.Result result =
                    aiPackageInstaller.install(
                            packageInfo,
                            packageFile,
                            (extractedBytes, totalBytes) -> {
                                long now = android.os.SystemClock.elapsedRealtime();
                                if (now - lastProgressUpdate[0] < 250L
                                        && (totalBytes <= 0L
                                        || extractedBytes < totalBytes)) {
                                    return;
                                }
                                lastProgressUpdate[0] = now;
                                uiHandler.post(() ->
                                        aiPackageDialog.showInstallingState(
                                                extractedBytes,
                                                totalBytes
                                        )
                                );
                            }
                    );
            boolean activationSucceeded = false;
            String reconciliationFamily = "";
            if (result.isSuccess()) {
                if (packageInfo.getPackageType()
                        == com.bliss.aimemorysearch.ai.AiPackageType.TRANSLATION) {
                    com.bliss.aimemorysearch.ai.AiPackageLifecycleResult activationResult =
                            translationPackageManager.activateInstalledPackage(
                                    packageInfo, result.getInstalledDirectory());
                    activationSucceeded = activationResult.isSuccess();
                    if (activationSucceeded) {
                        reconciliationFamily = translationFamily(packageInfo);
                        activationSucceeded = !reconciliationFamily.isEmpty();
                    }
                } else if (packageInfo.getPackageType()
                        == com.bliss.aimemorysearch.ai.AiPackageType.MODEL) {
                    activationSucceeded = true;
                }
            }
            boolean finalActivationSucceeded = activationSucceeded;
            String finalReconciliationFamily = reconciliationFamily;
            uiHandler.post(() -> {
                if (result.isSuccess()
                        && finalActivationSucceeded) {
                    if (packageInfo.getPackageType()
                            == com.bliss.aimemorysearch.ai.AiPackageType.TRANSLATION) {
                        startupPackageOperationActive = false;
                        refreshKeepScreenAwake();
                        beginCanonicalReconciliation(
                                packageInfo,
                                finalReconciliationFamily
                        );
                        aiPackageDialog.showInstalledState();
                        closeAiPackageDialog();
                    } else {
                        activeBundleCompletedBytes += packageInfo.getDownloadSizeBytes();
                        activeBundlePackageIndex++;
                        startNextBundlePackageDownload();
                    }
                } else if (result.isSuccess()) {
                    startupPackageOperationActive = false;
                    refreshKeepScreenAwake();
                    clearPackageOperationState();
                    aiPackageDialog.showDownloadErrorState(
                            getString(R.string.ai_package_activation_error)
                    );
                    aiPackageDialog.setPrimaryActionListener(v -> closeAiPackageDialog());
                } else {
                    startupPackageOperationActive = false;
                    refreshKeepScreenAwake();
                    clearPackageOperationState();
                    showAiPackageInstallationError(result.getFailureReason());
                }
            });
        }).start();
    }

    private void startModelDependentRuntime() {
        Intent liveService = new Intent(
                this,
                com.bliss.aimemorysearch.services.LiveIndexingService.class
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(liveService);
        } else {
            startService(liveService);
        }
    }

    private void showAiPackageInstallationError(
            com.bliss.aimemorysearch.ai.AtomicAiPackageInstaller.FailureReason reason
    ) {
        int messageResId;
        if (reason == null) {
            messageResId = R.string.ai_package_installation_error;
        } else {
            switch (reason) {
                case INVALID_MANIFEST:
                    messageResId = R.string.ai_package_manifest_error;
                    break;
                case UNSUPPORTED_FORMAT:
                    messageResId = R.string.ai_package_format_error;
                    break;
                case INVALID_CONTENTS:
                    messageResId = R.string.ai_package_contents_error;
                    break;
                case INVALID_PACKAGE:
                case INSTALLATION:
                default:
                    messageResId = R.string.ai_package_installation_error;
                    break;
            }
        }
        aiPackageDialog.showDownloadErrorState(getString(messageResId));
        aiPackageDialog.setPrimaryActionListener(v -> closeAiPackageDialog());
    }

    private int downloadFailureMessage(
            com.bliss.aimemorysearch.ai.AiPackageDownloadManager.FailureReason reason
    ) {
        if (reason == null) {
            return R.string.ai_package_network_error;
        }
        switch (reason) {
            case NOT_AVAILABLE:
                return R.string.ai_package_not_available;
            case HTTP:
                return R.string.ai_package_http_error;
            case STORAGE:
                return R.string.ai_package_storage_error;
            case CHECKSUM:
                return R.string.ai_package_checksum_error;
            case NETWORK:
            default:
                return R.string.ai_package_network_error;
        }
    }

    private void closeAiPackageDialog() {
        aiPackageDialog.hide();
        languagePackagePromptVisible = false;
        activeAiPackageInfo = null;
        activeDownloadPackageInfo = null;
        activeBundlePackages = java.util.Collections.emptyList();
    }

    private long bundleTotal(long packageTotal) {
        return activeBundlePackages.isEmpty() ? packageTotal : activeBundleTotalBytes;
    }

    private int bundleProgress(long currentPackageBytes) {
        if (activeBundlePackages.isEmpty() || activeBundleTotalBytes <= 0L) {
            return 0;
        }
        return (int) Math.min(100L,
                (activeBundleCompletedBytes + currentPackageBytes) * 100L
                        / activeBundleTotalBytes);
    }

    private void finishAiSearchBundleInstallation() {
        startupRuntimeActivationActive = true;
        refreshKeepScreenAwake();
        boolean activated = aiSearchBundleCoordinator.activate(
                com.bliss.aimemorysearch.ai.AiPackageBundleCoordinator.AI_SEARCH_BUNDLE_ID);
        startupRuntimeActivationActive = false;
        if (!activated) {
            startupPackageOperationActive = false;
            refreshKeepScreenAwake();
            aiPackageDialog.showDownloadErrorState(
                    getString(R.string.ai_package_activation_error));
            aiPackageDialog.setPrimaryActionListener(v -> closeAiPackageDialog());
            return;
        }
        aiPackageDialog.showInstalledState();
        closeAiPackageDialog();
        aiSearchReady = true;
        startModelDependentRuntime();
        startupPackageOperationActive = false;
        refreshKeepScreenAwake();
        continueStartupPreparation();
    }

    private void markInitialLanguagePromptShown(
            boolean initialPrompt
    ) {
        if (!initialPrompt) {
            return;
        }

        getSharedPreferences(
                "language_package_routing",
                MODE_PRIVATE
        ).edit()
                .putBoolean(
                        "initial_prompt_shown",
                        true
                )
                .apply();
    }

    private void resumePendingSearch() {
        com.bliss.aimemorysearch.ai.SearchRequest request =
                pendingSearchRequest;
        pendingSearchRequest = null;
        clearPendingSearchState();

        if (request != null) {
            executeSearch(request);
        }
    }

    private void showSearchPreparation(String message) {
        if (searchPreparationPanel == null || searchPreparationText == null) return;
        searchPreparationText.setText(message);
        searchPreparationPanel.setVisibility(View.VISIBLE);
    }

    private void hideSearchPreparation() {
        if (searchPreparationPanel != null) {
            searchPreparationPanel.setVisibility(View.GONE);
        }
    }

    private void setPendingSearchRequest(
            com.bliss.aimemorysearch.ai.SearchRequest request
    ) {
        pendingSearchRequest = request;
        if (request == null) {
            clearPendingSearchState();
            return;
        }
        getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                .putString(PENDING_QUERY, request.getOriginalQuery())
                .putString(PENDING_LANGUAGE, request.getDetectedLanguage())
                .putString(PENDING_FAMILY, request.getSelectedLanguageFamily())
                .apply();
    }

    private void beginCanonicalReconciliation(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo packageInfo,
            String family
    ) {
        java.util.UUID workId =
                com.bliss.aimemorysearch.workers.CanonicalReindexWorker.enqueue(
                        this,
                        family
                );
        getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                .putString(RECONCILIATION_FAMILY, family)
                .putString(RECONCILIATION_PACKAGE_ID, packageInfo.getPackageId())
                .putString(RECONCILIATION_WORK_ID, workId.toString())
                .apply();
        activeAiPackageInfo = packageInfo;
        aiPackageDialog.bind(packageInfo);

        observeCanonicalReconciliation(family, workId);
    }

    private void observeCanonicalReconciliation(
            String family,
            java.util.UUID workId
    ) {
        if (reconciliationWorkLiveData != null) {
            reconciliationWorkLiveData.removeObservers(this);
        }
        reconciliationWorkLiveData = androidx.work.WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(workId);
        reconciliationWorkLiveData.observe(this, workInfo -> {
            if (!family.equals(persistedReconciliationFamily())) {
                return;
            }
            if (workInfo == null) {
                if (reconciliationRecoveryInFlight
                        || !workId.toString().equals(persistedReconciliationWorkId())) {
                    return;
                }
                reconciliationRecoveryInFlight = true;
                com.bliss.aimemorysearch.workers.CanonicalReindexWorker.enqueue(
                        this,
                        family,
                        workId
                );
                return;
            }
            reconciliationRecoveryInFlight = false;
            if (workInfo.getState() == androidx.work.WorkInfo.State.BLOCKED) {
                reconciliationRecoveryInFlight = true;
                java.util.UUID replacementId =
                        com.bliss.aimemorysearch.workers.CanonicalReindexWorker.enqueue(
                                this,
                                family
                        );
                getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                        .putString(RECONCILIATION_WORK_ID, replacementId.toString())
                        .apply();
                observeCanonicalReconciliation(family, replacementId);
                return;
            }
            if (!workInfo.getState().isFinished()) {
                aiPackageDialog.showReconcilingState();
                return;
            }
            if (workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                clearReconciliationState();
                clearPackageOperationState();
                aiPackageDialog.showInstalledState();
                closeAiPackageDialog();
                if (!searchWorkflowReady) {
                    continueStartupPreparation();
                    return;
                }
                resumePendingSearch();
                if (hasStorageAccess()) {
                    startIndexing();
                }
                return;
            }
            clearReconciliationState();
            clearPackageOperationState();
            aiPackageDialog.showDownloadErrorState(
                    getString(R.string.ai_package_reconciliation_error)
            );
            aiPackageDialog.setPrimaryActionListener(v -> closeAiPackageDialog());
            if (hasStorageAccess()) {
                startIndexing();
            }
        });
    }

    private void restorePendingPackageLifecycle() {
        android.content.SharedPreferences state =
                getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE);
        restorePendingSearchRequest(state);
        String family = state.getString(RECONCILIATION_FAMILY, "");
        String packageId = state.getString(RECONCILIATION_PACKAGE_ID, "");
        if (family == null || family.isEmpty()) {
            family = state.getString(PACKAGE_OPERATION_FAMILY, "");
            packageId = state.getString(PACKAGE_OPERATION_ID, "");
        }
        if (family == null || family.isEmpty() || packageId == null
                || packageId.isEmpty()) {
            if (pendingSearchRequest != null) {
                com.bliss.aimemorysearch.ai.SearchRequest request = pendingSearchRequest;
                uiHandler.post(() -> continueSearch(request));
            }
            return;
        }
        com.bliss.aimemorysearch.ai.model.AIPackageInfo packageInfo =
                translationPackageManager.getMetadata(packageId);
        if (packageInfo == null) {
            clearReconciliationState();
            return;
        }
        languagePackagePromptVisible = true;
        activeAiPackageInfo = packageInfo;
        aiPackageDialog.bind(packageInfo);
        if (translationPackageManager.isInstalled(packageId)) {
            String persistedWorkId = state.getString(RECONCILIATION_WORK_ID, "");
            java.util.UUID workId;
            try {
                if (persistedWorkId == null || persistedWorkId.isEmpty()) {
                    throw new IllegalArgumentException("missing reconciliation work id");
                }
                workId = java.util.UUID.fromString(persistedWorkId);
            } catch (IllegalArgumentException invalidWorkId) {
                workId = com.bliss.aimemorysearch.workers.CanonicalReindexWorker.enqueue(
                        this,
                        family
                );
                state.edit()
                        .putString(RECONCILIATION_WORK_ID, workId.toString())
                        .apply();
            }
            observeCanonicalReconciliation(family, workId);
        } else {
            clearPackageOperationState();
            aiPackageDialog.showErrorState(getString(R.string.ai_package_interrupted));
            aiPackageDialog.setPrimaryActionListener(v -> startAiPackageDownload());
            aiPackageDialog.setSecondaryActionListener(v -> closeAiPackageDialog());
            aiPackageDialog.show();
        }
    }

    private void restorePendingSearchRequest(
            android.content.SharedPreferences state
    ) {
        String query = state.getString(PENDING_QUERY, "");
        if (query == null || query.isEmpty()) {
            return;
        }
        com.bliss.aimemorysearch.ai.SearchRequest request =
                com.bliss.aimemorysearch.ai.QueryUnderstandingEngine
                        .createSearchRequest(query);
        request.setDetectedLanguage(state.getString(PENDING_LANGUAGE, ""));
        request.setSelectedLanguageFamily(state.getString(PENDING_FAMILY, ""));
        pendingSearchRequest = request;
    }

    private String persistedReconciliationFamily() {
        return getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE)
                .getString(RECONCILIATION_FAMILY, "");
    }

    private String persistedReconciliationWorkId() {
        return getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE)
                .getString(RECONCILIATION_WORK_ID, "");
    }

    private boolean hasActiveAiPackageOperation() {
        android.content.SharedPreferences state =
                getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE);
        String packageFamily = state.getString(PACKAGE_OPERATION_FAMILY, "");
        String packageId = state.getString(PACKAGE_OPERATION_ID, "");
        String reconciliationFamily = state.getString(RECONCILIATION_FAMILY, "");
        String reconciliationWorkId = state.getString(RECONCILIATION_WORK_ID, "");
        return (!packageFamily.isEmpty() && !packageId.isEmpty())
                || (!reconciliationFamily.isEmpty()
                && !reconciliationWorkId.isEmpty());
    }

    private void clearReconciliationState() {
        getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                .remove(RECONCILIATION_FAMILY)
                .remove(RECONCILIATION_PACKAGE_ID)
                .remove(RECONCILIATION_WORK_ID)
                .apply();
    }

    private void persistPackageOperation(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo packageInfo
    ) {
        String family = translationFamily(packageInfo);
        if (family.isEmpty()) {
            return;
        }
        getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                .putString(PACKAGE_OPERATION_FAMILY, family)
                .putString(PACKAGE_OPERATION_ID, packageInfo.getPackageId())
                .apply();
    }

    private void clearPackageOperationState() {
        getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                .remove(PACKAGE_OPERATION_FAMILY)
                .remove(PACKAGE_OPERATION_ID)
                .apply();
    }

    private void clearPendingSearchState() {
        getSharedPreferences(PENDING_SEARCH_STATE, MODE_PRIVATE).edit()
                .remove(PENDING_QUERY)
                .remove(PENDING_LANGUAGE)
                .remove(PENDING_FAMILY)
                .apply();
    }

    private static String translationFamily(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo packageInfo
    ) {
        for (String language : packageInfo.getSupportedLanguages()) {
            com.bliss.aimemorysearch.ai.TranslationModelInfo model =
                    com.bliss.aimemorysearch.ai.TranslationModelRegistry
                            .getModelForLanguage(language);
            if (model != null) {
                return model.getTranslationFamily();
            }
        }
        return "";
    }
    private void openSearchResults(
            List<FileEntity> results
    ) {

        Intent intent =
                new Intent(
                        this,
                        SearchResultsActivity.class
                );

        startActivity(intent);
    }
    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                        },
                        STORAGE_PERMISSION_CODE
                );

            } else {

                continueStartupPreparation();
            }

        } else {

            if (
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_CODE
                );

            } else {

                continueStartupPreparation();
            }
        }
    }
    private WorkInfo selectDisplayedWork(List<WorkInfo> workInfos) {
        WorkInfo blocked = null;
        WorkInfo enqueued = null;
        for (WorkInfo candidate : workInfos) {
            if (candidate.getState() == WorkInfo.State.RUNNING) {
                return candidate;
            }
            if (candidate.getState() == WorkInfo.State.BLOCKED) {
                blocked = candidate;
            } else if (candidate.getState() == WorkInfo.State.ENQUEUED) {
                enqueued = candidate;
            }
        }
        if (blocked != null) return blocked;
        if (enqueued != null) return enqueued;
        return workInfos.isEmpty() ? null : workInfos.get(workInfos.size() - 1);
    }

    private void updateOverallProgress(int processed, int total, WorkInfo.State state) {
        if (indexOverallProgress == null || indexOverallProgressText == null) return;
        boolean active = state == WorkInfo.State.RUNNING
                || state == WorkInfo.State.ENQUEUED
                || state == WorkInfo.State.BLOCKED;
        boolean hasTotal = total > 0;
        boolean show = active || hasTotal;
        indexOverallProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        indexOverallProgressText.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        if (!hasTotal) {
            indexOverallProgress.setIndeterminate(true);
            indexOverallProgressText.setText(R.string.overall_progress_discovering);
            return;
        }
        int boundedProcessed = Math.max(0, Math.min(processed, total));
        int percent = boundedProcessed * 100 / total;
        indexOverallProgress.setIndeterminate(false);
        indexOverallProgress.setProgressCompat(percent, true);
        indexOverallProgressText.setText(getString(
                R.string.overall_progress_value, boundedProcessed, total, percent));
    }

    private void updateCategoryProgress(boolean reconciliation,
            int documentsProcessed, int documentsTotal,
            int imagesProcessed, int imagesTotal,
            int ocrProcessed, int ocrEstimated,
            int embeddingsProcessed, int embeddingsEstimated) {
        if (reconciliation || documentsTotal + imagesTotal <= 0) {
            pdfGauge.setVisibility(View.GONE);
            jpgGauge.setVisibility(View.GONE);
            ocrGauge.setVisibility(View.GONE);
            embeddingGauge.setVisibility(View.GONE);
            return;
        }
        bindCategoryProgress(pdfGauge, pdfCountText, documentsProcessed, documentsTotal,
                getString(R.string.documents_workload));
        bindCategoryProgress(jpgGauge, jpgCountText, imagesProcessed, imagesTotal,
                getString(R.string.images_workload));
        bindCategoryProgress(ocrGauge, ocrCountText, ocrProcessed, ocrEstimated,
                getString(R.string.ocr_workload_estimate));
        bindCategoryProgress(embeddingGauge, embeddingCountText,
                embeddingsProcessed, embeddingsEstimated,
                getString(R.string.embedding_workload_estimate));
    }

    private void bindCategoryProgress(ProgressBar bar, TextView label,
            int processed, int total, String name) {
        bar.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        bar.setMax(Math.max(1, total));
        bar.setProgress(Math.max(0, Math.min(processed, total)));
        label.setText(getString(R.string.category_workload_value,
                name, processed, total));
    }

    private void updateWorkState(
            WorkInfo.State state,
            boolean reconciliation,
            int runAttemptCount
    ) {
        if (indexStateText == null) return;
        int message;
        if (state == WorkInfo.State.RUNNING) {
            if (runAttemptCount > 0) {
                indexStateText.setText(getString(
                        reconciliation
                                ? R.string.reconciliation_state_retrying
                                : R.string.index_state_retrying,
                        runAttemptCount + 1));
                return;
            }
            message = reconciliation
                    ? R.string.reconciliation_state_running
                    : R.string.index_state_running;
        } else if (state == WorkInfo.State.ENQUEUED) {
            message = reconciliation
                    ? R.string.reconciliation_state_queued
                    : R.string.index_state_queued;
        } else if (state == WorkInfo.State.BLOCKED) {
            message = reconciliation
                    ? R.string.reconciliation_state_waiting
                    : R.string.index_state_waiting;
        } else if (state == WorkInfo.State.SUCCEEDED) {
            message = reconciliation
                    ? R.string.reconciliation_state_completed
                    : R.string.index_state_completed;
        } else if (state == WorkInfo.State.FAILED) {
            message = reconciliation
                    ? R.string.reconciliation_state_failed
                    : R.string.index_state_failed;
        } else if (state == WorkInfo.State.CANCELLED) {
            message = reconciliation
                    ? R.string.reconciliation_state_cancelled
                    : R.string.index_state_cancelled;
        } else {
            message = R.string.index_state_idle;
        }
        indexStateText.setText(message);
    }

    private void observeIndexWorker() {

        if (indexObserverRegistered) return;
        indexObserverRegistered = true;

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(
                        "ai_memory_index_worker_debug"
                )
                .observe(this, workInfos -> {

                    if (
                            workInfos == null
                                    || workInfos.isEmpty()
                    ) {
                        return;
                    }

                    WorkInfo workInfo = selectDisplayedWork(workInfos);
                    if (workInfo == null) {
                        return;
                    }
                    indexWorkActive = workInfo.getState() == WorkInfo.State.RUNNING
                            || workInfo.getState() == WorkInfo.State.ENQUEUED
                            || workInfo.getState() == WorkInfo.State.BLOCKED;

                    androidx.work.Data progress =
                            workInfo.getProgress();
                    boolean isCanonicalReconciliation =
                            workInfo.getTags().contains(
                                    com.bliss.aimemorysearch.workers
                                            .CanonicalReindexWorker.RECONCILIATION_TAG
                            );
                    if (!isCanonicalReconciliation) {
                        initialIndexOperationActive = indexWorkActive
                                && !prefs.getBoolean("first_index_done", false);
                        if (workInfo.getState().isFinished()) {
                            initialIndexOperationActive = false;
                        }
                        refreshKeepScreenAwake();
                    }
                    if (isCanonicalReconciliation
                            && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        progress = workInfo.getOutputData();
                    }

                    int processed =
                            progress.getInt(
                                    "processed",
                                    0
                            );

                    int total =
                            progress.getInt(
                                    "total",
                                    0
                            );
                    int pdfCount =
                            progress.getInt(
                                    "pdfCount",
                                    0
                            );

                    int jpgCount =
                            progress.getInt(
                                    "jpgCount",
                                    0
                            );

                    int ocrCount =
                            progress.getInt(
                                    "ocrCount",
                                    0
                            );

                    int embeddingCount =
                            progress.getInt(
                                    "embeddingCount",
                                    0
                            );
                    int documentTotal = progress.getInt("documentTotal", 0);
                    int imageTotal = progress.getInt("imageTotal", 0);
                    int ocrEstimatedTotal = progress.getInt("ocrEstimatedTotal", 0);
                    int embeddingEstimatedTotal = progress.getInt(
                            "embeddingEstimatedTotal", 0);
                    int documentProcessed = progress.getInt("documentProcessed", 0);
                    int imageProcessed = progress.getInt("imageProcessed", 0);
                    int libraryDocumentTotal = progress.getInt("libraryDocumentTotal", 0);
                    String status =
                            progress.getString(
                                    "status"
                            );
                    String stage =
                            progress.getString(
                                    "stage"
                            );
                    String currentFile =
                            progress.getString(
                                    "currentFile"
                            );
                    boolean hasIndexProgress = progress.getKeyValueMap()
                            .containsKey("processed");
                    String fileName = progress.getString("fileName");
                    long fileSize = progress.getLong("fileSize", 0L);
                    long progressElapsedMs = progress.getLong("elapsedMs", 0L);
                    long workerStartedAt = progress.getLong("workerStartedAt", 0L);
                    if (workerStartedAt > 0L) {
                        progressElapsedMs = Math.max(
                                progressElapsedMs,
                                System.currentTimeMillis() - workerStartedAt);
                    }
                    long progressEtaSeconds = progress.getLong("etaSeconds", 0L);
                    double progressSpeed = progress.getDouble("speed", 0d);
                    int indexed = progress.getInt("indexed", 0);
                    int skipped = progress.getInt("skipped", 0);
                    int skippedByUser = progress.getInt("skippedByUser", 0);
                    if (progressElapsedMs > 0L) {
                        progressSpeed = processed * 1000d / progressElapsedMs;
                        progressEtaSeconds = progressSpeed > 0d
                                ? (long) Math.max(
                                0d, (total - processed) / progressSpeed)
                                : 0L;
                    }
                    boolean awaitingDecision = progress.getBoolean(
                            com.bliss.aimemorysearch.workers.IndexWorker.KEY_AWAITING_DECISION,
                            false);
                    String decisionToken = progress.getString(
                            com.bliss.aimemorysearch.workers.IndexWorker.KEY_DECISION_TOKEN);
                    String currentOperation = progress.getString(
                            com.bliss.aimemorysearch.workers.IndexWorker.KEY_CURRENT_OPERATION);
                    long operationElapsedMs = progress.getLong(
                            com.bliss.aimemorysearch.workers.IndexWorker.KEY_OPERATION_ELAPSED_MS,
                            0L);

                    if (status == null) {

                        status =
                                getString(
                                        R.string.indexing_status_preparing
                                );
                    }

                    if (currentFile == null) {

                        currentFile = "";
                    }
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = currentFile;
                    }

                    updateIndexLiveInfo(
                            fileName,
                            fileSize,
                            workerStartedAt,
                            progressElapsedMs,
                            progressEtaSeconds,
                            processed,
                            total,
                            indexed,
                            skipped,
                            skippedByUser,
                            progressSpeed,
                            stage,
                            awaitingDecision,
                            decisionToken,
                            currentOperation,
                            operationElapsedMs,
                            isCanonicalReconciliation
                    );

                    updateOverallProgress(processed, total, workInfo.getState());
                    updateCategoryProgress(isCanonicalReconciliation,
                            documentProcessed, documentTotal,
                            imageProcessed, imageTotal,
                            ocrCount, ocrEstimatedTotal,
                            embeddingCount, embeddingEstimatedTotal);
                    updateWorkState(
                            workInfo.getState(),
                            isCanonicalReconciliation,
                            workInfo.getRunAttemptCount());
                    if (isCanonicalReconciliation
                            && !workInfo.getState().isFinished()) {
                        String family = persistedReconciliationFamily();
                        String packageId = getSharedPreferences(
                                PENDING_SEARCH_STATE, MODE_PRIVATE)
                                .getString(RECONCILIATION_PACKAGE_ID, "");
                        indexStateText.setText(getString(
                                R.string.reconciliation_identity,
                                family == null || family.isEmpty() ? "Unknown family" : family,
                                packageId == null || packageId.isEmpty()
                                        ? "Translation package" : packageId));
                    }

                    if (
                            workInfo.getState()
                                    == WorkInfo.State.RUNNING
                    )
                    {
                        indexLiveInfoPanel.setVisibility(
                                hasIndexProgress ? View.VISIBLE : View.GONE);
                        if (indexLoader != null) {

                            if (indexLoader.getVisibility() != View.VISIBLE) {

                                indexLoader.setVisibility(
                                        View.VISIBLE
                                );

                            }
                        }
                        titleText_unu.setText(
                                getString(
                                        R.string.indexing_title_running
                                )
                        );

                        subtitleText_unu.setText(
                                getString(
                                        R.string.indexing_body_running
                                )
                        );

                        liveIndexingText.setText(
                                getString(
                                        isCanonicalReconciliation
                                                ? R.string.reconciliation_library_progress
                                                : R.string.indexing_progress,
                                        processed,
                                        total,
                                        isCanonicalReconciliation
                                                ? libraryDocumentTotal : currentFile
                                )
                        );
                        String etaTextValue = progressEtaSeconds > 0L
                                ? formatIndexDuration(progressEtaSeconds)
                                : getString(R.string.indexing_status_preparing);

                        String speedValue = String.format(
                                java.util.Locale.getDefault(),
                                "%.1f",
                                progressSpeed);

                        speedText.setText(
                                getString(
                                        R.string.speed_value,
                                        speedValue
                                )
                        );

                        etaText.setText(
                                getString(
                                        R.string.eta_value,
                                        etaTextValue
                                )
                        );

                        if (
                                stage != null
                                        &&
                                        !stage.trim().isEmpty()
                        ) {

                            stageText.setText(
                                    getString(
                                            R.string.stage_value,
                                            stage
                                    )
                            );

                        } else {

                            stageText.setText(
                                    getString(
                                            R.string.stage_processing
                                    )
                            );
                        }
                    }

                    else if (
                            workInfo.getState()
                                    == WorkInfo.State.SUCCEEDED
                    )
                    {
                        if (!isCanonicalReconciliation) {
                            updateStartupCompletion();
                        }
                        indexLiveInfoPanel.setVisibility(
                                isCanonicalReconciliation ? View.VISIBLE : View.GONE);
                        if (indexLoader != null) {
                            
                            indexLoader.setVisibility(
                                    View.GONE
                            );
                        }
                        titleText_unu.setText(
                                getString(
                                        R.string.indexing_title_completed
                                )
                        );

                        subtitleText_unu.setText(
                                isCanonicalReconciliation
                                        ? getString(
                                        R.string.reconciliation_completed_time,
                                        formatIndexDuration(progressElapsedMs / 1000L))
                                        : getString(R.string.indexing_body_completed)
                        );

                        liveIndexingText.setText(
                                isCanonicalReconciliation
                                        ? getString(
                                        R.string.reconciliation_progress,
                                        processed,
                                        total)
                                        : getString(R.string.live_indexing_active)
                        );
                        speedText.setText(
                                getString(
                                        R.string.speed_done
                                )
                        );

                        etaText.setText(
                                getString(
                                        R.string.eta_done
                                )
                        );

                        stageText.setText(
                                getString(
                                        R.string.stage_completed
                                )
                        );
                        new android.os.Handler(
                                android.os.Looper.getMainLooper()
                        ).postDelayed(() -> {

                            titleText_unu.setText(
                                    getString(
                                            R.string.search_invite_title
                                    )
                            );

                            subtitleText_unu.setText(
                                    getString(
                                            R.string.search_invite_body
                                    )
                            );

                        }, 3000);
                    }

                    else if (
                            workInfo.getState()
                                    == WorkInfo.State.FAILED
                    ) {
                        indexLiveInfoPanel.setVisibility(
                                hasIndexProgress ? View.VISIBLE : View.GONE);
                        if (indexLoader != null) {
                            
                            indexLoader.setVisibility(
                                    View.GONE
                            );
                        }
                        liveIndexingText.setText(
                                isCanonicalReconciliation
                                        ? getString(R.string.reconciliation_state_failed)
                                        : getString(R.string.index_state_failed)
                        );
                        stageText.setText(isCanonicalReconciliation
                                ? getString(R.string.reconciliation_state_failed)
                                : getString(R.string.index_state_failed));
                        speedText.setText(getString(R.string.speed_done));
                        etaText.setText(getString(R.string.eta_done));
                    } else if (workInfo.getState() == WorkInfo.State.CANCELLED) {
                        indexLoader.setVisibility(View.GONE);
                        liveIndexingText.setText(isCanonicalReconciliation
                                ? getString(R.string.reconciliation_state_cancelled)
                                : getString(R.string.index_state_cancelled));
                    } else if (workInfo.getState() == WorkInfo.State.ENQUEUED
                            || workInfo.getState() == WorkInfo.State.BLOCKED) {
                        indexLoader.setVisibility(View.VISIBLE);
                        indexLiveInfoPanel.setVisibility(
                                hasIndexProgress ? View.VISIBLE : View.GONE);
                        liveIndexingText.setText(isCanonicalReconciliation
                                ? getString(R.string.reconciliation_state_queued)
                                : getString(R.string.index_state_queued));
                    }
                });
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == STORAGE_PERMISSION_CODE) {

            boolean granted = true;

            for (int result : grantResults) {

                if (result != PackageManager.PERMISSION_GRANTED) {

                    granted = false;
                    break;
                }
            }

            if (!granted) {

                liveIndexingText.setText(
                        getString(
                                R.string.storage_permission_denied
                        )
                );
                showPermissionCard();

                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (!Environment.isExternalStorageManager()) {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);

                    return;
                }
            }

            liveIndexingText.setText(
                    getString(
                            R.string.starting_indexing
                    )
            );

            hidePermissionCard();
            continueStartupPreparation();
        }
    }
    private void startIndexing() {
        if (!aiSearchReady) {
            return;
        }
        indexingStartTime =
                System.currentTimeMillis();

        observeIndexWorker();

        new Thread(() -> {
            boolean firstIndexDone = prefs.getBoolean("first_index_done", false);
            int persistedCount = prefs.getInt("last_indexed_count", 0);
            int actualCount = database.fileDao().countIndexed();

            if (firstIndexDone
                    && actualCount > 0
                    && actualCount >= persistedCount) {
                uiHandler.post(this::updateStartupCompletion);
                return;
            }

            prefs.edit().putBoolean("first_index_done", false).apply();

            uiHandler.post(() -> {
                liveIndexingText.setText(getString(R.string.indexing_status_preparing));
                showFirstIndexExplanationThen(this::startBackgroundIndexing);
            });
        }).start();
    }

    private void showFirstIndexExplanationThen(Runnable continueIndexing) {
        if (prefs.getBoolean("first_index_explanation_shown", false)) {
            continueIndexing.run();
            return;
        }
        if (firstIndexExplanationDialog != null
                && firstIndexExplanationDialog.isShowing()) {
            return;
        }
        firstIndexExplanationDialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.first_index_explanation_title)
                .setMessage(R.string.first_index_explanation_body)
                .setCancelable(false)
                .setPositiveButton(R.string.first_index_explanation_continue,
                        (dialog, which) -> {
                            prefs.edit()
                                    .putBoolean("first_index_explanation_shown", true)
                                    .commit();
                            dialog.dismiss();
                            firstIndexExplanationDialog = null;
                            continueIndexing.run();
                        })
                .create();
        firstIndexExplanationDialog.setOnDismissListener(dialog -> {
            firstIndexExplanationDialog = null;
        });
        firstIndexExplanationDialog.show();
    }
    private void startBackgroundIndexing() {
        initialIndexOperationActive = true;
        refreshKeepScreenAwake();

        androidx.work.OneTimeWorkRequest request =
                new androidx.work.OneTimeWorkRequest.Builder(
                        com.bliss.aimemorysearch.workers.IndexWorker.class
                )
                        .build();

        androidx.work.WorkManager
                .getInstance(this)
                .enqueueUniqueWork(
                        "ai_memory_index_worker_debug",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        request
                );
    }

    private void startIndexMaintenance() {
        if (prefs.getBoolean("index_idempotency_repaired", false)) {
            return;
        }

        androidx.work.OneTimeWorkRequest request =
                new androidx.work.OneTimeWorkRequest.Builder(
                        com.bliss.aimemorysearch.workers.IndexWorker.class
                )
                        .setInputData(
                                new androidx.work.Data.Builder()
                                        .putBoolean(
                                                com.bliss.aimemorysearch.workers.IndexWorker.KEY_MAINTENANCE,
                                                true
                                        )
                                        .build()
                        )
                        .build();

        androidx.work.WorkManager
                .getInstance(this)
                .enqueueUniqueWork(
                        "ai_memory_index_maintenance",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        request
                );
    }
    private void showLastIndexInfo() {

        long lastIndexTime =
                prefs.getLong(
                        "last_index_time",
                        0
                );

        int lastIndexedCount =
                prefs.getInt(
                        "last_indexed_count",
                        0
                );

        boolean firstIndexDone =
                prefs.getBoolean(
                        "first_index_done",
                        false
                );

        indexedCountText.setText(
                String.valueOf(
                        lastIndexedCount
                )
        );

        if (!firstIndexDone) {

            liveIndexingText.setText(
                    getString(
                            R.string.indexing_status_preparing
                    )
            );

            return;
        }

        String formattedTime =
                new java.text.SimpleDateFormat(
                        "HH:mm",
                        java.util.Locale.getDefault()
                ).format(
                        new java.util.Date(
                                lastIndexTime
                        )
                );

        liveIndexingText.setText(
                getString(
                        R.string.last_scan,
                        formattedTime
                )
        );
    }
    private void showPermissionCard() {
        dimView.setAlpha(0.3f);
        dimView.setVisibility(View.VISIBLE);
        permissionCard.setVisibility(
                View.VISIBLE
        );

        permissionCard.setScaleX(0.88f);
        permissionCard.setScaleY(0.88f);
        permissionCard.setAlpha(0f);

        permissionCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_modal
                                )
                )
                .start();
    }
    private void hidePermissionCard() {
        permissionCard.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_dialog_exit
                                )
                )
                .withEndAction(() -> {

                    permissionCard.setVisibility(
                            View.GONE
                    );

                })
                .start();
    }
    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }
    private void showExitDialog() {
        exitVisible = true;
        dimView.setVisibility(
                android.view.View.VISIBLE
        );
        dimView.animate()
                .alpha(1f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_medium
                                )
                )
                .start();

        exitCard.setVisibility(
                android.view.View.VISIBLE
        );

        exitCard.setScaleX(0.85f);
        exitCard.setScaleY(0.85f);

        exitCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_dialog_enter
                                )
                )
                .start();
    }
    private void hideExitDialog() {

        exitVisible = false;
        dimView.animate()
                .alpha(0f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_medium
                                )
                )
                .withEndAction(() -> {

                    dimView.setVisibility(
                            android.view.View.GONE
                    );

                })
                .start();

        exitCard.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_medium
                                )
                )
                .withEndAction(() -> {

                    exitCard.setVisibility(
                            android.view.View.GONE
                    );

                })
                .start();
    }
    private void rebuildVocabularyCache() {

        new Thread(() -> {

            try {

                com.bliss.aimemorysearch.ai
                        .PrefixVocabularyCache
                        .clear();

                List<com.bliss.aimemorysearch.db.ChunkEntity>
                        chunks =
                        AppDatabase
                                .getInstance(this)
                                .chunkDao()
                                .getAllChunks();

                if (chunks == null) {
                    return;
                }

                for (
                        com.bliss.aimemorysearch.db.ChunkEntity chunk
                        : chunks
                ) {

                    if (chunk == null) {
                        continue;
                    }

                    if (
                            chunk.normalizedText == null
                    ) {

                        continue;
                    }

                    com.bliss.aimemorysearch.ai
                            .PrefixVocabularyCache
                            .addText(
                                    chunk.normalizedText
                            );
                }

                android.util.Log.d(
                        "VOCAB_REBUILD",
                        "DONE"
                );

                android.util.Log.d(
                        "VOCAB_REBUILD",
                        "SIZE="
                                + com.bliss.aimemorysearch.ai
                                .PrefixVocabularyCache
                                .getVocabularySize()
                );

            } catch (Exception e) {

                e.printStackTrace();
            }

        }).start();
    }
    @Override
    protected void onResume() {

        super.onResume();

        if (hasStorageAccess()) {

            hidePermissionCard();

            if (!aiSearchReady) {
                showAiSearchBundlePrompt();
                return;
            }

            boolean firstIndexDone =
                    prefs.getBoolean(
                            "first_index_done",
                            false
                    );
            continueStartupPreparation();
            if (firstIndexDone) {
                startIndexMaintenance();
            }

            

        } else {

            showPermissionCard();
        }
    }

    private void updateIndexLiveInfo(
            String fileName,
            long fileSize,
            long workerStartedAt,
            long elapsedMs,
            long etaSeconds,
            int processed,
            int total,
            int indexed,
            int skipped,
            int skippedByUser,
            double speed,
            String phase,
            boolean awaitingDecision,
            String decisionToken,
            String currentOperation,
            long operationElapsedMs,
            boolean reconciliation
    ) {
        if (indexLiveInfoText == null) {
            return;
        }
        String displayName = fileName == null || fileName.isEmpty()
                ? "Scanning filesystem" : fileName;
        String displaySize = fileSize > 0L
                ? android.text.format.Formatter.formatFileSize(this, fileSize)
                : "Not available";
        String displayPhase = phase == null || phase.isEmpty() ? "Preparing" : phase;
        int remaining = Math.max(0, total - processed);
        String start = workerStartedAt > 0L
                ? new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date(workerStartedAt))
                : "Not available";
        String eta = etaSeconds > 0L
                ? formatIndexDuration(etaSeconds)
                : (total > 0 && processed >= total ? "0:00" : "Calculating");
        String scope = reconciliation ? "Reconciled" : "Processed";
        String subject = reconciliation ? "Current document" : "Current file";
        indexLiveInfoText.setText(
                subject + ": " + displayName
                        + (reconciliation ? "" : " | Size: " + displaySize)
                        + "\nStarted: " + start + " | Elapsed: "
                        + formatIndexDuration(elapsedMs / 1000L) + " | ETA: " + eta
                        + "\n" + scope + ": " + processed + " / " + total
                        + " | Remaining: " + remaining
                        + (reconciliation ? "" : "\nIndexed this attempt: " + indexed
                        + " | Skipped: " + skipped + " | By user: " + skippedByUser)
                        + "\nSpeed: " + String.format(java.util.Locale.getDefault(),
                        reconciliation ? "%.2f documents/s" : "%.2f files/s", speed)
                        + " | Phase: " + displayPhase
                        + (reconciliation
                        ? "\nMultilingual search remains gated until preparation completes."
                        : "")
        );

        pendingIndexDecisionToken = decisionToken == null ? "" : decisionToken;
        if (awaitingDecision && !pendingIndexDecisionToken.isEmpty()) {
            indexLongFileWarningText.setText(getString(
                    R.string.index_long_file_warning,
                    displayName,
                    currentOperation == null || currentOperation.isEmpty()
                            ? displayPhase : currentOperation,
                    formatIndexDuration(operationElapsedMs / 1000L)));
            indexLongFileWarningText.setVisibility(View.VISIBLE);
            indexLongFileActions.setVisibility(View.VISIBLE);
            indexLongFileWarningCard.setVisibility(View.VISIBLE);
        } else {
            indexLongFileWarningText.setVisibility(View.GONE);
            indexLongFileActions.setVisibility(View.GONE);
            indexLongFileWarningCard.setVisibility(View.GONE);
        }
    }

    private void submitIndexDecision(String decision) {
        if (pendingIndexDecisionToken.isEmpty()) {
            return;
        }
        getSharedPreferences(
                com.bliss.aimemorysearch.workers.IndexWorker.USER_SKIP_PREFS,
                MODE_PRIVATE)
                .edit()
                .putString("decision." + pendingIndexDecisionToken, decision)
                .apply();
        indexLongFileActions.setVisibility(View.GONE);
    }

    private static String formatIndexDuration(long seconds) {
        if (seconds <= 0L) {
            return "0:00";
        }
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        return hours > 0L
                ? String.format(java.util.Locale.getDefault(), "%d:%02d:%02d",
                hours, minutes, remainingSeconds)
                : String.format(java.util.Locale.getDefault(), "%d:%02d",
                minutes, remainingSeconds);
    }

    @Override
    protected void onDestroy() {
        if (firstIndexExplanationDialog != null) {
            firstIndexExplanationDialog.dismiss();
            firstIndexExplanationDialog = null;
        }
        if (aiPackageDownloadManager != null) {
            aiPackageDownloadManager.close();
        }
        super.onDestroy();
    }

}
