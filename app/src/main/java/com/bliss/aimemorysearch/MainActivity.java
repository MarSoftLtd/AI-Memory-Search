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
import com.bliss.aimemorysearch.ai.EmbeddingEngine;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.bliss.aimemorysearch.ai.ImageEmbeddingEngine;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
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
    private boolean indexingStarted = false;
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
        EmbeddingEngine
                .getInstance()
                .initialize(this);
        ImageEmbeddingEngine
                .getInstance()
                .initialize(this);
        MobileClipTextEmbeddingEngine
                .getInstance()
                .initialize(this);
        MiniLMTokenizer
                .getInstance()
                .initialize(this);
        new Thread(() -> {

            float[] vector1 =
                    EmbeddingEngine
                            .getInstance()
                            .generateEmbedding(
                                    "factura emag"
                            );

            float[] vector2 =
                    EmbeddingEngine
                            .getInstance()
                            .generateEmbedding(
                                    "factura de la emag"
                            );

            float[] vector3 =
                    EmbeddingEngine
                            .getInstance()
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
                            "Documents " + count
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
                            "Images " + count
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
                            "Text detected " + count
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
                            "AI indexed " + count
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

        new SearchCoordinator(
                this
        ).search(
                query,
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
                            workInfos.get(0);

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
                                "Preparing AI indexing...";
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
                                "Semantic AI Indexing"
                        );

                        subtitleText_unu.setText(
                                "Please wait while the AI performs the initial semantic indexing process. The system is analyzing documents, OCR text, screenshots, images, file names and semantic vectors to build your private on-device AI memory search database. This extended indexing happens only once during the first setup and may take several minutes depending on the number of files stored on your device. After completion, the AI will automatically index only new or modified files in the background."
                        );

                        liveIndexingText.setText(
                                processed
                                        + " / "
                                        + total
                                        + " • "
                                        + currentFile
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
                                minutes
                                        + "m "
                                        + seconds
                                        + "s";

                        String speedValue =
                                String.format(
                                        java.util.Locale.US,
                                        "%.1f",
                                        filesPerSecond
                                );

                        speedText.setText(
                                "SPEED "
                                        + speedValue
                                        + "/s"
                        );

                        etaText.setText(
                                "ETA "
                                        + etaTextValue
                        );

                        if (
                                stage != null
                                        &&
                                        !stage.trim().isEmpty()
                        ) {

                            stageText.setText(
                                    "STAGE "
                                            + stage
                            );

                        } else {

                            stageText.setText(
                                    "STAGE Processing..."
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
                        prefs.edit()
                                .putBoolean(
                                        "first_index_done",
                                        true
                                )
                                .apply();

                        titleText_unu.setText(
                                "AI Index Completed"
                        );

                        subtitleText_unu.setText(
                                "Documents, images and semantic vectors indexed successfully."
                        );

                        liveIndexingText.setText(
                                "Live indexing active"
                        );
                        speedText.setText(
                                "SPEED DONE"
                        );

                        etaText.setText(
                                "ETA 0s"
                        );

                        stageText.setText(
                                "STAGE Completed"
                        );
                        new android.os.Handler(
                                android.os.Looper.getMainLooper()
                        ).postDelayed(() -> {

                            titleText_unu.setText(
                                    "Search your phone memory"
                            );

                            subtitleText_unu.setText(
                                    "Type something like invoice, screenshot, name, document, location or text from an image."
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
                                "AI indexing completed"
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
                        "Storage permission denied"
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
                    "Starting indexing..."
            );

            startIndexing();
        }
    }
    private void startIndexing() {

        indexingStarted = true;

        indexingStartTime =
                System.currentTimeMillis();

        observeIndexWorker();

        liveIndexingText.setText(
                "Preparing AI indexing..."
        );

        startBackgroundIndexing();
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
                        androidx.work.ExistingWorkPolicy.REPLACE,
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
                    "Preparing AI indexing..."
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
                "Last scan "
                        + formattedTime
        );
    }
    private void showPermissionCard() {

        dimView.setVisibility(
                View.VISIBLE
        );

        dimView.animate()
                .alpha(1f)
                .setDuration(250)
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
                .setDuration(260)
                .start();
    }
    private void hidePermissionCard() {

        dimView.animate()
                .alpha(0f)
                .setDuration(220)
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
                .setDuration(220)
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
                .setDuration(200)
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
                .setDuration(250)
                .start();
    }
    private void hideExitDialog() {

        exitVisible = false;

        dimView.animate()
                .alpha(0f)
                .setDuration(200)
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
                .setDuration(200)
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
            if (!indexingStarted) {

                startIndexing();
            }

            

        } else {

            showPermissionCard();
        }
    }

}
