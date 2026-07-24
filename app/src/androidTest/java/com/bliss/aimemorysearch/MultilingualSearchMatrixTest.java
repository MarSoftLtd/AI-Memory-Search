package com.bliss.aimemorysearch;

import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.bliss.aimemorysearch.ai.LanguageDetectionEngine;
import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchRequest;
import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public final class MultilingualSearchMatrixTest {
    private static final String[][] GROUPS = {
            {"dog", "perro", "caine"},
            {"invoice", "factură", "facture", "Rechnung", "fattura", "factura"},
            {"house", "casă", "Haus", "maison"},
            {"red", "roșu", "rot", "rouge"},
            {"car", "mașină", "Auto", "voiture"}
    };

    @Test
    public void logPhysicalDeviceRegressionMatrix() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        Intent launch = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.startActivitySync(launch);
        instrumentation.waitForIdleSync();

        for (String[] group : GROUPS) {
            for (String query : group) {
                List<FileEntity> results = search(context, query);
                List<String> identities = new ArrayList<>();
                for (FileEntity result : results) {
                    identities.add(result.type + ":" + result.path);
                }
                Log.i("MATRIX_RESULT", "query=" + query + " | results=" + identities);
            }
        }
    }

    private static List<FileEntity> search(Context context, String query) throws Exception {
        SearchRequest request = QueryUnderstandingEngine.createSearchRequest(query);
        LanguageDetectionEngine.DetectionResult detection =
                LanguageDetectionEngine.getInstance(context).detectLanguage(query, "");
        request.setDetectedLanguage(detection.language);
        request.setSelectedLanguageFamily(detection.family);
        CountDownLatch latch = new CountDownLatch(1);
        List<FileEntity> captured = new ArrayList<>();
        new SearchCoordinator(context).search(request, results -> {
            captured.addAll(results);
            latch.countDown();
        });
        assertTrue("Search timed out for " + query, latch.await(30, TimeUnit.SECONDS));
        return captured;
    }
}
