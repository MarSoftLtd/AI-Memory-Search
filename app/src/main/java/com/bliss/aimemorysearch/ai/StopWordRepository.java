package com.bliss.aimemorysearch.ai;

import java.util.Set;

public interface StopWordRepository {

    Set<String> getStopWords(
            String languageCode
    );
}
