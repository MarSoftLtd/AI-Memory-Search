package com.bliss.aimemorysearch.indexing;

import org.junit.Test;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileIndexingPolicyTest {
    private final FileIndexingPolicy policy = new FileIndexingPolicy();

    @Test public void skipsLargeStructuredGeneratedOutput() throws Exception {
        File build = new File(Files.createTempDirectory("policy").toFile(), "build");
        assertTrue(build.mkdirs());
        File artifact = new File(build, "dependency-report.html");
        String row = "<div class=\"node\"><a href=\"module/path_name.py\">module_name</a></div>\n";
        Files.write(artifact.toPath(), row.repeat(12000).getBytes(StandardCharsets.UTF_8));
        FileIndexingPolicy.Evaluation result = policy.evaluate(artifact);
        assertFalse(result.shouldIndex());
        assertTrue(result.signals().contains("large_file"));
        assertTrue(result.signals().contains("generated_output_path"));
        assertTrue(result.signals().contains("markup_heavy"));
    }

    @Test public void indexesNormalUserDocument() throws Exception {
        File document = new File(Files.createTempDirectory("policy").toFile(), "travel-notes.txt");
        Files.write(document.toPath(), ("Our family visited the coast last summer. We stayed for a week "
                + "and kept notes about the places we enjoyed.").getBytes(StandardCharsets.UTF_8));
        assertTrue(policy.evaluate(document).shouldIndex());
    }

    @Test public void doesNotSkipForSingleWeakSignal() throws Exception {
        File build = new File(Files.createTempDirectory("policy").toFile(), "build");
        assertTrue(build.mkdirs());
        File document = new File(build, "meeting-notes.txt");
        Files.write(document.toPath(), "Discussion notes and decisions from the meeting."
                .getBytes(StandardCharsets.UTF_8));
        assertTrue(policy.evaluate(document).shouldIndex());
    }

    @Test public void indexesLargeUserProseEvenInsideBuildDirectory() throws Exception {
        File build = new File(Files.createTempDirectory("policy").toFile(), "build");
        assertTrue(build.mkdirs());
        File document = new File(build, "research-journal.txt");
        String paragraph = "This journal records observations from the study in complete sentences. "
                + "The participants described their experiences and the conclusions were reviewed.\n";
        Files.write(document.toPath(), paragraph.repeat(5000).getBytes(StandardCharsets.UTF_8));
        assertTrue(policy.evaluate(document).shouldIndex());
    }
}
