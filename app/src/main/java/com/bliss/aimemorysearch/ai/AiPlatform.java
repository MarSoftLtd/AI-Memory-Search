package com.bliss.aimemorysearch.ai;

public final class AiPlatform {

    private static final AiRuntimeManager RUNTIME_MANAGER =
            new AiRuntimeManager();

    private AiPlatform() {
    }

    public static AiRuntimeManager getRuntimeManager() {
        return RUNTIME_MANAGER;
    }
}
