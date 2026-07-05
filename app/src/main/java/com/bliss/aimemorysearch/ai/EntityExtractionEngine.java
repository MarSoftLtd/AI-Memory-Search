package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityExtractionEngine {

    public static String extractPersons(
            String text
    ) {

        if (text == null) {
            return "";
        }

        List<String> persons =
                new ArrayList<>();

        Pattern pattern =
                Pattern.compile(
                        "\\b[A-ZĂÂÎȘȚ][a-zăâîșț]+\\s+[A-ZĂÂÎȘȚ][a-zăâîșț]+\\b"
                );

        Matcher matcher =
                pattern.matcher(text);

        while (matcher.find()) {

            String person =
                    matcher.group();

            if (!persons.contains(person)) {

                persons.add(person);
            }
        }

        return android.text.TextUtils.join(
                ", ",
                persons
        );
    }

    public static String extractNumbers(
            String text
    ) {

        if (text == null) {
            return "";
        }

        List<String> numbers =
                new ArrayList<>();

        Pattern pattern =
                Pattern.compile(
                        "\\b\\d{4,}\\b"
                );

        Matcher matcher =
                pattern.matcher(text);

        while (matcher.find()) {

            String value =
                    matcher.group();

            if (!numbers.contains(value)) {

                numbers.add(value);
            }
        }

        return android.text.TextUtils.join(
                ", ",
                numbers
        );
    }

    public static String detectDocumentType(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String lower =
                text.toLowerCase();

        if (
                lower.contains("invoice")
                        ||
                        lower.contains("factura")
        ) {

            return "invoice";
        }

        if (
                lower.contains("identity")
                        ||
                        lower.contains("identitate")
        ) {

            return "identity";
        }

        if (
                lower.contains("contract")
        ) {

            return "contract";
        }

        return "";
    }
}