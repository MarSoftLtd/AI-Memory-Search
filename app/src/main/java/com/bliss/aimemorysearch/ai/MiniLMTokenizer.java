package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MiniLMTokenizer {

    private static MiniLMTokenizer instance;

    private final Map<String, Integer> vocab =
            new HashMap<>();

    private boolean loaded = false;

    private static final int MAX_LEN = 128;
    private static final int PAD_ID = 0;
    private static final int UNK_ID = 100;
    private static final int CLS_ID = 101;
    private static final int SEP_ID = 102;

    public static synchronized MiniLMTokenizer getInstance() {

        if (instance == null) {
            instance = new MiniLMTokenizer();
        }

        return instance;
    }

    public void initialize(Context context) {

        if (loaded) {
            return;
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    context.getAssets()
                                            .open("models/vocab.txt")
                            )
                    );

            String line;
            int index = 0;

            while ((line = reader.readLine()) != null) {

                vocab.put(
                        line.trim(),
                        index
                );

                index++;
            }

            reader.close();

            loaded = true;

            android.util.Log.d(
                    "TOKENIZER",
                    "WORDPIECE VOCAB LOADED = "
                            + vocab.size()
            );

        } catch (Exception e) {

            android.util.Log.e(
                    "TOKENIZER",
                    "FAILED",
                    e
            );
        }
    }

    public TokenizedInput encode(String text) {
        android.util.Log.e(
                "VOCAB_SIZE",
                String.valueOf(vocab.size())
        );

        android.util.Log.e(
                "DOG_EXISTS",
                String.valueOf(vocab.containsKey("dog"))
        );

        android.util.Log.e(
                "CAR_EXISTS",
                String.valueOf(vocab.containsKey("car"))
        );

        android.util.Log.e(
                "ROSE_EXISTS",
                String.valueOf(vocab.containsKey("rose"))
        );
        long[] inputIds =
                new long[MAX_LEN];

        long[] attentionMask =
                new long[MAX_LEN];

        long[] tokenTypeIds =
                new long[MAX_LEN];

        if (text == null) {
            text = "";
        }

        text =
                normalize(text);

        List<String> words =
                basicTokenize(text);

        List<Integer> tokenIds =
                new ArrayList<>();

        tokenIds.add(CLS_ID);

        for (String word : words) {

            List<Integer> pieces =
                    wordPieceTokenize(word);

            for (Integer id : pieces) {

                if (tokenIds.size() >= MAX_LEN - 1) {
                    break;
                }

                tokenIds.add(id);
            }

            if (tokenIds.size() >= MAX_LEN - 1) {
                break;
            }
        }

        tokenIds.add(SEP_ID);

        for (int i = 0; i < MAX_LEN; i++) {

            if (i < tokenIds.size()) {

                inputIds[i] =
                        tokenIds.get(i);

                attentionMask[i] =
                        1;

            } else {

                inputIds[i] =
                        PAD_ID;

                attentionMask[i] =
                        0;
            }

            tokenTypeIds[i] =
                    0;
        }

        return new TokenizedInput(
                inputIds,
                attentionMask,
                tokenTypeIds
        );
    }

    private String normalize(String input) {

        String value =
                input.toLowerCase(Locale.ROOT)
                        .trim();

        value =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        value =
                value.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        value =
                value.replaceAll(
                        "[^a-z0-9]+",
                        " "
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    private List<String> basicTokenize(String text) {

        List<String> words =
                new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return words;
        }

        String[] split =
                text.split(" ");

        for (String part : split) {

            String word =
                    part.trim();

            if (!word.isEmpty()) {
                words.add(word);
            }
        }

        return words;
    }

    private List<Integer> wordPieceTokenize(String word) {

        List<Integer> output =
                new ArrayList<>();

        if (word.length() > 100) {

            output.add(UNK_ID);

            return output;
        }

        int start = 0;

        boolean isBad = false;

        while (start < word.length()) {

            int end =
                    word.length();

            String currentSubString =
                    null;

            while (start < end) {

                String sub =
                        word.substring(
                                start,
                                end
                        );

                if (start > 0) {
                    sub = "##" + sub;
                }

                if (vocab.containsKey(sub)) {

                    currentSubString =
                            sub;

                    break;
                }

                end--;
            }

            if (currentSubString == null) {

                isBad =
                        true;

                break;
            }

            output.add(
                    vocab.get(currentSubString)
            );

            start =
                    end;
        }

        if (isBad) {

            output.clear();

            output.add(
                    UNK_ID
            );
        }

        return output;
    }

    public static class TokenizedInput {

        public long[] inputIds;
        public long[] attentionMask;
        public long[] tokenTypeIds;

        public TokenizedInput(
                long[] inputIds,
                long[] attentionMask,
                long[] tokenTypeIds
        ) {

            this.inputIds =
                    inputIds;

            this.attentionMask =
                    attentionMask;

            this.tokenTypeIds =
                    tokenTypeIds;
        }
    }
}