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
import com.bliss.aimemorysearch.ai.E5SentencePieceNative;
import com.bliss.aimemorysearch.ai.DocumentRuntimeLoader;

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
import com.bliss.aimemorysearch.ai.VectorUtils;
import com.github.ybq.android.spinkit.SpinKitView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;
    private EditText searchEdit;
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
    private long indexingStartTime = 0L;
    private SpinKitView indexLoader;
    private com.bliss.aimemorysearch.ai.LanguagePackageRouter languagePackageRouter;
    private com.bliss.aimemorysearch.ai.SearchRequest pendingSearchRequest;
    private String selectedLanguageFamily = "";
    private boolean languagePackagePromptVisible = false;
    private com.bliss.aimemorysearch.ui.AIPackageDialog aiPackageDialog;
    private com.bliss.aimemorysearch.ai.TranslationPackageManager
            translationPackageManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//====================================================================================================
        // validateSentencePiece();
//====================================================================================================
        new E5EmbeddingEngine(this).selfTest();
//====================================================================================================
        ConceptSimilarityTest.run(this);
        Intent liveService =
                new Intent(
                        this,
                        com.bliss.aimemorysearch.services.LiveIndexingService.class
                );

        try {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                startForegroundService(liveService);

            } else {

                startService(liveService);
            }

            android.util.Log.d(
                    "LIVE_INDEX",
                    "SERVICE START SUCCESS"
            );

        } catch (Exception e) {
            android.util.Log.e(
                    "LIVE_INDEX",
                    "SERVICE START FAILED",
                    e
            );
        }

        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows( getWindow(), false );
        setContentView(R.layout.activity_main);
        aiPackageDialog =
                new com.bliss.aimemorysearch.ui.AIPackageDialog(
                        findViewById(R.id.aiPackageCard)
                );
        translationPackageManager =
                com.bliss.aimemorysearch.ai.TranslationPackageManager
                        .getInstance(this);
        initializeLanguagePackageRouting();
        database = AppDatabase.getInstance(this);
        rebuildVocabularyCache();
        prefs = getSharedPreferences( "index_state", MODE_PRIVATE );
        btnGrantAccess = findViewById(R.id.btnGrantAccess);
        permissionCard = findViewById(R.id.permissionCard);
        searchEdit = findViewById(R.id.searchEdit);
        titleText_unu = findViewById( R.id.titleText_unu );
        subtitleText_unu = findViewById( R.id.subtitleText_unu );
        clearSearch = findViewById(R.id.clearSearch);
        searchButton = findViewById(R.id.searchButton);
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
                        if (exitVisible) {
                            hideExitDialog();
                            return;
                        }
                        showExitDialog();
                    }
                }
        );
        observeIndexStats();
        // =============================  AI
        DocumentRuntimeLoader
                .createDefault()
                .loadEmbeddingRuntime(this);
        ImageRuntimeLoader
                .createDefault()
                .loadImageEmbeddingRuntime(this);
        ImageRuntimeLoader
                .createDefault()
                .loadTextEmbeddingRuntime(this);
        MiniLMTokenizer
                .getInstance()
                .initialize(this);
        new Thread(() -> {

            float[] vector1 =
                    DocumentRuntimeLoader
                            .createDefault()
                            .getEmbeddingRuntime()
                            .generateEmbedding(
                                    "factura emag"
                            );

            float[] vector2 =
                    DocumentRuntimeLoader
                            .createDefault()
                            .getEmbeddingRuntime()
                            .generateEmbedding(
                                    "factura de la emag"
                            );

            float[] vector3 =
                    DocumentRuntimeLoader
                            .createDefault()
                            .getEmbeddingRuntime()
                            .generateEmbedding(
                                    "poza cu masina"
                            );

            float similarity1 =
                    VectorUtils.cosineSimilarity(
                            vector1,
                            vector2
                    );

            float similarity2 =
                    VectorUtils.cosineSimilarity(
                            vector1,
                            vector3
                    );

            android.util.Log.d(
                    "SEMANTIC",
                    "Factura vs Factura = "
                            + similarity1
            );

            android.util.Log.d(
                    "SEMANTIC",
                    "Factura vs Poza = "
                            + similarity2
            );

        }).start();
        liveIndexingText =
                findViewById(
                        R.id.liveIndexingText
                );

    }
    private void validateSentencePiece() {

        new Thread(() -> {

            try {

                File modelDir =
                        new File(
                                getFilesDir(),
                                "models/e5"
                        );

                if (!modelDir.exists()) {
                    modelDir.mkdirs();
                }

                File modelFile =
                        new File(
                                modelDir,
                                "sentencepiece.bpe.model"
                        );

                if (
                        !modelFile.exists()
                                ||
                                modelFile.length() == 0
                ) {

                    try (
                            InputStream inputStream =
                                    getAssets()
                                            .open(
                                                    "models/e5/sentencepiece.bpe.model"
                                            );
                            FileOutputStream outputStream =
                                    new FileOutputStream(
                                            modelFile
                                    )
                    ) {

                        byte[] buffer =
                                new byte[16 * 1024];

                        int read;

                        while (
                                (read = inputStream.read(buffer))
                                        != -1
                        ) {
                            outputStream.write(
                                    buffer,
                                    0,
                                    read
                            );
                        }
                    }
                }

                boolean loaded =
                        E5SentencePieceNative.loadModel(
                                modelFile.getAbsolutePath()
                        );

                if (loaded) {

                    android.util.Log.d(
                            "SP_VALIDATION",
                            "MODEL LOAD OK"
                    );

                    logSentencePieceTokens(
                            "dog"
                    );
                    logSentencePieceTokens(
                            "caine"
                    );
                    logSentencePieceTokens(
                            "Hund"
                    );
                    logSentencePieceTokens(
                            "chien"
                    );
                    logSentencePieceTokens(
                            "perro"
                    );

                } else {

                    android.util.Log.d(
                            "SP_VALIDATION",
                            "MODEL LOAD FAILED"
                    );
                }

            } catch (Exception e) {

                android.util.Log.e(
                        "SP_VALIDATION",
                        "MODEL LOAD FAILED",
                        e
                );
            }

        }).start();
    }
    private void logSentencePieceTokens(
            String text
    ) {

        int[] tokens =
                E5SentencePieceNative.encode(
                        text
                );

        android.util.Log.d(
                "SP_VALIDATION",
                text
                        + " = "
                        + Arrays.toString(tokens)
        );
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

                    pdfCountText.setText(
                            getString(
                                    R.string.documents_count,
                                    count
                            )
                    );

                    pdfGauge.setMax(
                            count + 10
                    );

                    pdfGauge.setProgress(count);
                });

        database.fileDao()
                .getImageCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }

                    jpgCountText.setText(
                            getString(
                                    R.string.images_count,
                                    count
                            )
                    );

                    jpgGauge.setMax(
                            count + 10
                    );

                    jpgGauge.setProgress(count);
                });

        database.fileDao()
                .getOcrCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }

                    ocrCountText.setText(
                            getString(
                                    R.string.text_detected_count,
                                    count
                            )
                    );

                    ocrGauge.setMax(
                            count + 10
                    );

                    ocrGauge.setProgress(count);
                });

        database.fileDao()
                .getEmbeddingCountLive()
                .observe(this, count -> {

                    if (count == null) {
                        return;
                    }

                    embeddingCountText.setText(
                            getString(
                                    R.string.ai_indexed_count,
                                    count
                            )
                    );

                    embeddingGauge.setMax(
                            count + 10
                    );

                    embeddingGauge.setProgress(count);
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

        com.bliss.aimemorysearch.ai.SearchRequest request =
                com.bliss.aimemorysearch.ai.QueryUnderstandingEngine
                        .createSearchRequest(query.trim());
        request.setSelectedLanguageFamily(
                selectedLanguageFamily
        );

        if (!selectedLanguageFamily.isEmpty()) {
            String requiredPackageId =
                    languagePackageRouter.getRequiredPackageId(
                            selectedLanguageFamily
                    );
            if (
                    requiredPackageId != null
                            &&
                            !translationPackageManager.isInstalled(
                                    requiredPackageId
                            )
            ) {
                pendingSearchRequest = request;
                showLanguagePackagePrompt(requiredPackageId, false);
                return;
            }
            languagePackageRouter.activateInstalledPackage(
                    selectedLanguageFamily
            );
        }

        executeSearch(request);
    }

    private void executeSearch(
            com.bliss.aimemorysearch.ai.SearchRequest request
    ) {
        new SearchCoordinator(
                this
        ).search(
                request,
                finalResults -> {
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
        aiPackageDialog.bind(metadata);
        aiPackageDialog.showReadyState();
        aiPackageDialog.show();
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

    private void installLanguagePackageAndResume(
            String family
    ) {
        new Thread(() -> {
            com.bliss.aimemorysearch.ai.AiPackageLifecycleResult result =
                    languagePackageRouter.installSelectedPackage(family);

            uiHandler.post(() -> {
                if (
                        result.isSuccess()
                                &&
                                languagePackageRouter.activateInstalledPackage(
                                        family
                                )
                ) {
                    resumePendingSearch();
                    return;
                }

                android.widget.Toast.makeText(
                        this,
                        result.getMessage(),
                        android.widget.Toast.LENGTH_LONG
                ).show();
            });
        }).start();
    }

    private void resumePendingSearch() {
        com.bliss.aimemorysearch.ai.SearchRequest request =
                pendingSearchRequest;
        pendingSearchRequest = null;

        if (request != null) {
            executeSearch(request);
        }
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

                startIndexing();
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

                startIndexing();
            }
        }
    }
    private void observeIndexWorker() {

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

                    WorkInfo workInfo =
                            workInfos.get(workInfos.size() - 1);

                    androidx.work.Data progress =
                            workInfo.getProgress();

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

                    if (status == null) {

                        status =
                                getString(
                                        R.string.indexing_status_preparing
                                );
                    }

                    if (currentFile == null) {

                        currentFile = "";
                    }

                    if (
                            workInfo.getState()
                                    == WorkInfo.State.RUNNING
                    )
                    {
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
                                        R.string.indexing_progress,
                                        processed,
                                        total,
                                        currentFile
                                )
                        );
                        long elapsedMs =
                                System.currentTimeMillis()
                                        - indexingStartTime;

                        float elapsedSeconds =
                                elapsedMs / 1000f;

                        float filesPerSecond = 0f;

                        if (elapsedSeconds > 0f) {

                            filesPerSecond =
                                    processed
                                            / elapsedSeconds;
                        }

                        int remainingFiles =
                                Math.max(
                                        0,
                                        total - processed
                                );

                        long etaSeconds = 0;

                        if (filesPerSecond > 0f) {

                            etaSeconds =
                                    (long)
                                            (
                                                    remainingFiles
                                                            / filesPerSecond
                                            );
                        }

                        long minutes =
                                etaSeconds / 60;

                        long seconds =
                                etaSeconds % 60;

                        String etaTextValue =
                                getString(
                                        R.string.eta_time_value,
                                        minutes,
                                        seconds
                                );

                        String speedValue =
                                String.format(
                                        java.util.Locale.US,
                                        "%.1f",
                                        filesPerSecond
                                );

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
                                getString(
                                        R.string.indexing_body_completed
                                )
                        );

                        liveIndexingText.setText(
                                getString(
                                        R.string.live_indexing_active
                                )
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
                        if (indexLoader != null) {
                            
                            indexLoader.setVisibility(
                                    View.GONE
                            );
                        }
                        liveIndexingText.setText(
                                getString(
                                        R.string.indexing_completed
                                )
                        );
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

            startIndexing();
        }
    }
    private void startIndexing() {
        indexingStartTime =
                System.currentTimeMillis();

        observeIndexWorker();

        new Thread(() -> {
            boolean firstIndexDone = prefs.getBoolean("first_index_done", false);
            int persistedCount = prefs.getInt("last_indexed_count", 0);
            int actualCount = database.fileDao().countIndexed();

            if (firstIndexDone && actualCount > 0 && actualCount >= persistedCount) {
                return;
            }

            prefs.edit().putBoolean("first_index_done", false).apply();

            uiHandler.post(() -> {
                liveIndexingText.setText(getString(R.string.indexing_status_preparing));
                startBackgroundIndexing();
            });
        }).start();
    }
    private void startBackgroundIndexing() {

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

        dimView.setVisibility(
                View.VISIBLE
        );

        dimView.animate()
                .alpha(1f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_dialog_enter
                                )
                )
                .start();

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

        dimView.animate()
                .alpha(0f)
                .setDuration(
                        getResources()
                                .getInteger(
                                        R.integer.duration_dialog_exit
                                )
                )
                .withEndAction(() -> {

                    dimView.setVisibility(
                            View.GONE
                    );

                })
                .start();

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

        boolean mediaGranted;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            mediaGranted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED;

        } else {

            mediaGranted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED;
        }

        boolean storageGranted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                        || Environment.isExternalStorageManager();

        return mediaGranted && storageGranted;
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

            boolean firstIndexDone =
                    prefs.getBoolean(
                            "first_index_done",
                            false
                    );
            startIndexing();
            if (firstIndexDone) {
                startIndexMaintenance();
            }

            

        } else {

            showPermissionCard();
        }
    }

}
