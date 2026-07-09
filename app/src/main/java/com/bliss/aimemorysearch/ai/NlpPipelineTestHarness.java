package com.bliss.aimemorysearch.ai;

public final class NlpPipelineTestHarness {

    public String run(
            String query
    ) {
        NlpPipeline pipeline =
                new NlpPipeline();

        SearchRequest request =
                pipeline.process(
                        query
                );

        return format(
                request
        );
    }

    private static String format(
            SearchRequest request
    ) {
        StringBuilder builder =
                new StringBuilder();

        appendSection(
                builder,
                "Original Query",
                request.getOriginalQuery()
        );
        appendSection(
                builder,
                "Normalized Query",
                request.getNormalizedQuery()
        );
        appendSection(
                builder,
                "Tokens",
                request.getTokens().toString()
        );
        appendSection(
                builder,
                "Concepts",
                request.getConcepts().toString()
        );
        appendSection(
                builder,
                "Search Targets",
                request.getSearchTargets().toString()
        );

        return builder.toString();
    }

    private static void appendSection(
            StringBuilder builder,
            String title,
            String value
    ) {
        builder
                .append("----------------------------------------")
                .append('\n')
                .append(title)
                .append('\n')
                .append("----------------------------------------")
                .append('\n')
                .append(value)
                .append('\n');
    }
}
