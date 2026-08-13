package com.bliss.aimemorysearch.workers;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Process-wide foreground gate for secondary canonical work.
 *
 * The launcher activity initializes the gate before the user can search. Activity callbacks then
 * cover every activity in the process without coupling scheduling to individual screens.
 */
public final class CanonicalEnrichmentLifecycle {
    private static final AtomicBoolean ACTIVE_FOREGROUND = new AtomicBoolean(false);
    private static final Object REGISTRATION_LOCK = new Object();
    private static final ExecutorService SCHEDULING_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "canonical-lifecycle");
                thread.setDaemon(true);
                return thread;
            });
    private static boolean registered;
    private static int startedActivities;

    private CanonicalEnrichmentLifecycle() {}

    public static void initialize(Activity activity) {
        Application application = activity.getApplication();
        synchronized (REGISTRATION_LOCK) {
            if (!registered) {
                application.registerActivityLifecycleCallbacks(new Callbacks(application));
                registered = true;
            }
        }
        enterForeground(application);
    }

    public static boolean isActiveForeground() {
        return ACTIVE_FOREGROUND.get();
    }

    public static boolean shouldPauseForForeground(Context context) {
        return ACTIVE_FOREGROUND.get()
                && !CanonicalBootstrapState.shouldRunWhileForeground(context);
    }

    private static void enterForeground(Application application) {
        if (ACTIVE_FOREGROUND.compareAndSet(false, true)) {
            SCHEDULING_EXECUTOR.execute(
                    () -> CanonicalEnrichmentWorker.pauseForForeground(application));
        }
    }

    private static void enterBackground(Application application) {
        if (ACTIVE_FOREGROUND.compareAndSet(true, false)) {
            SCHEDULING_EXECUTOR.execute(
                    () -> CanonicalEnrichmentWorker.resumePending(application));
        }
    }

    private static final class Callbacks implements Application.ActivityLifecycleCallbacks {
        private final Application application;

        private Callbacks(Application application) {
            this.application = application;
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            synchronized (REGISTRATION_LOCK) {
                startedActivities++;
            }
            enterForeground(application);
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            boolean background;
            synchronized (REGISTRATION_LOCK) {
                startedActivities = Math.max(0, startedActivities - 1);
                background = startedActivities == 0 && !activity.isChangingConfigurations();
            }
            if (background) {
                enterBackground(application);
            }
        }

        @Override public void onActivityCreated(
                @NonNull Activity activity, @Nullable Bundle state) {}
        @Override public void onActivityResumed(@NonNull Activity activity) {}
        @Override public void onActivityPaused(@NonNull Activity activity) {}
        @Override public void onActivitySaveInstanceState(
                @NonNull Activity activity, @NonNull Bundle state) {}
        @Override public void onActivityDestroyed(@NonNull Activity activity) {}
    }
}
