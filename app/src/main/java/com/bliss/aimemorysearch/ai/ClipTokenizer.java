package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClipTokenizer {

    private static final String TAG = "CLIP_TOKENIZER";

    private static final int MAX_LEN = 77;

    private static ClipTokenizer instance;

    private final Map<String, Integer> vocab =
            new HashMap<>();

    private final Map<String, Integer> bpeRanks =
            new HashMap<>();

    private final Map<Integer, String> byteEncoder =
            new HashMap<>();

    private final Map<String, List<Integer>> cache =
            new ConcurrentHashMap<>();

    private boolean loaded = false;

    private int startTokenId = 49406;
    private int endTokenId = 49407;

    private final Pattern tokenPattern =
            Pattern.compile(
                    "<\\|startoftext\\|>|<\\|endoftext\\|>|\\p{L}+|\\p{N}+|[^\\s\\p{L}\\p{N}]+"
            );

    public static synchronized ClipTokenizer getInstance() {

        if (instance == null) {
            instance = new ClipTokenizer();
        }

        return instance;
    }

    public synchronized void initialize(
            Context context
    ) {

        if (loaded) {
            return;
        }

        try {

            buildByteEncoder();

            loadVocab(context);

            loadMerges(context);

            if (vocab.containsKey("<|startoftext|>")) {
                startTokenId =
                        vocab.get("<|startoftext|>");
            }

            if (vocab.containsKey("<|endoftext|>")) {
                endTokenId =
                        vocab.get("<|endoftext|>");
            }

            loaded = true;

            Log.d(
                    TAG,
                    "CLIP TOKENIZER LOADED | vocab="
                            + vocab.size()
                            + " merges="
                            + bpeRanks.size()
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "FAILED",
                    e
            );
        }
    }

    public long[] encode(
            String text
    ) {

        long[] inputIds =
                new long[MAX_LEN];

        for (int i = 0; i < MAX_LEN; i++) {
            inputIds[i] = endTokenId;
        }

        List<Integer> tokens =
                new ArrayList<>();

        tokens.add(
                startTokenId
        );

        if (text == null) {
            text = "";
        }

        text =
                cleanText(
                        text
                );

        Matcher matcher =
                tokenPattern.matcher(
                        text
                );

        while (matcher.find()) {

            String token =
                    matcher.group();

            List<Integer> bpeTokens =
                    encodeToken(
                            token
                    );

            for (Integer id : bpeTokens) {

                if (tokens.size() >= MAX_LEN - 1) {
                    break;
                }

                tokens.add(id);
            }

            if (tokens.size() >= MAX_LEN - 1) {
                break;
            }
        }

        tokens.add(
                endTokenId
        );

        for (int i = 0; i < Math.min(tokens.size(), MAX_LEN); i++) {
            inputIds[i] =
                    tokens.get(i);
        }

        return inputIds;
    }

    private void loadVocab(
            Context context
    ) throws Exception {

        String json =
                readAsset(
                        context,
                        "models/clip/vocab.json"
                );

        JSONObject object =
                new JSONObject(json);

        Iterator<String> keys =
                object.keys();

        while (keys.hasNext()) {

            String key =
                    keys.next();

            vocab.put(
                    key,
                    object.getInt(key)
            );
        }
    }

    private void loadMerges(
            Context context
    ) throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                context.getAssets()
                                        .open(
                                                "models/clip/merges.txt"
                                        ),
                                StandardCharsets.UTF_8
                        )
                );

        String line;
        int rank = 0;

        while ((line = reader.readLine()) != null) {

            line =
                    line.trim();

            if (
                    line.isEmpty()
                            ||
                            line.startsWith("#")
            ) {
                continue;
            }

            bpeRanks.put(
                    line,
                    rank
            );

            rank++;
        }

        reader.close();
    }

    private String readAsset(
            Context context,
            String path
    ) throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                context.getAssets()
                                        .open(path),
                                StandardCharsets.UTF_8
                        )
                );

        StringBuilder builder =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();

        return builder.toString();
    }

    private List<Integer> encodeToken(
            String token
    ) {

        if (cache.containsKey(token)) {
            return cache.get(token);
        }

        byte[] bytes =
                token.getBytes(
                        StandardCharsets.UTF_8
                );

        List<String> chars =
                new ArrayList<>();

        for (byte value : bytes) {

            int unsigned =
                    value & 0xFF;

            chars.add(
                    byteEncoder.get(unsigned)
            );
        }

        if (chars.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> word =
                new ArrayList<>();

        for (int i = 0; i < chars.size(); i++) {

            if (i == chars.size() - 1) {

                word.add(
                        chars.get(i) + "</w>"
                );

            } else {

                word.add(
                        chars.get(i)
                );
            }
        }

        List<String> merged =
                applyBpe(
                        word
                );

        List<Integer> ids =
                new ArrayList<>();

        for (String part : merged) {

            Integer id =
                    vocab.get(part);

            if (id != null) {
                ids.add(id);
            } else {
                ids.add(endTokenId);
            }
        }

        cache.put(
                token,
                ids
        );

        return ids;
    }

    private List<String> applyBpe(
            List<String> word
    ) {

        if (word.size() <= 1) {
            return word;
        }

        while (true) {

            List<Pair> pairs =
                    getPairs(
                            word
                    );

            if (pairs.isEmpty()) {
                break;
            }

            Pair best =
                    pairs.stream()
                            .filter(pair ->
                                    bpeRanks.containsKey(
                                            pair.left + " " + pair.right
                                    )
                            )
                            .min(
                                    Comparator.comparingInt(pair ->
                                            bpeRanks.get(
                                                    pair.left + " " + pair.right
                                            )
                                    )
                            )
                            .orElse(null);

            if (best == null) {
                break;
            }

            List<String> newWord =
                    new ArrayList<>();

            int i = 0;

            while (i < word.size()) {

                if (
                        i < word.size() - 1
                                &&
                                word.get(i).equals(best.left)
                                &&
                                word.get(i + 1).equals(best.right)
                ) {

                    newWord.add(
                            best.left + best.right
                    );

                    i += 2;

                } else {

                    newWord.add(
                            word.get(i)
                    );

                    i++;
                }
            }

            word =
                    newWord;

            if (word.size() == 1) {
                break;
            }
        }

        return word;
    }

    private List<Pair> getPairs(
            List<String> word
    ) {

        List<Pair> pairs =
                new ArrayList<>();

        for (int i = 0; i < word.size() - 1; i++) {

            pairs.add(
                    new Pair(
                            word.get(i),
                            word.get(i + 1)
                    )
            );
        }

        return pairs;
    }

    private String cleanText(
            String text
    ) {

        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void buildByteEncoder() {

        List<Integer> bytes =
                new ArrayList<>();

        for (int i = 33; i <= 126; i++) {
            bytes.add(i);
        }

        for (int i = 161; i <= 172; i++) {
            bytes.add(i);
        }

        for (int i = 174; i <= 255; i++) {
            bytes.add(i);
        }

        List<Integer> chars =
                new ArrayList<>(
                        bytes
                );

        int n = 0;

        for (int i = 0; i < 256; i++) {

            if (!bytes.contains(i)) {

                bytes.add(i);

                chars.add(
                        256 + n
                );

                n++;
            }
        }

        for (int i = 0; i < bytes.size(); i++) {

            byteEncoder.put(
                    bytes.get(i),
                    new String(
                            Character.toChars(
                                    chars.get(i)
                            )
                    )
            );
        }
    }

    private static class Pair {

        final String left;
        final String right;

        Pair(
                String left,
                String right
        ) {
            this.left = left;
            this.right = right;
        }
    }
}