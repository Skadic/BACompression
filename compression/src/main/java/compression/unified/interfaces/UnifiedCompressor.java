package compression.unified.interfaces;

import compression.areacomp.v4.RuleIntervalIndex;
import compression.unified.UnifiedRuleset;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.StringJoiner;

/**
 * An interface that offers the {@link #compress(String)} method to compress a string, and an already implemented
 * {@link #benchmark(String, PrintStream)} method, which tests this algorithms performance.
 */
public interface UnifiedCompressor {

    /**
     * A constant detailing, whether generated Grammars should be included in the output of {@link #benchmark(String, PrintStream)}
     */
    boolean PRINT = false;

    /**
     * Compresses the string and generates a Grammar as a result
     * @param s The string to compress
     * @return A Grammar, that can be represented as a {@link UnifiedRuleset}
     */
    ToUnifiedRuleset compress(String s);

    /**
     * Benchmarks this compressor with the given String and output to the console
     * See {@link #benchmark(String, PrintStream)} for a more detailed explanation.
     * @param s The string to compress during benchmarking
     */
    default void benchmark(String s) {
        benchmark(s, System.out);
    }

    /**
     * Measures the performance and size of the produced grammar of this {@link UnifiedCompressor} and writes the results to a {@link PrintStream}
     *
     * If {@link #PRINT} is true, then the grammar itself is also written to the {@link PrintStream}.
     *
     * @param s The string to compress during benchmarking
     * @param out The {@link PrintStream} to write the results to. If the results should be written to the console,
     *            just use {@link #benchmark(String)} or pass {@link System#out} as this parameter
     *
     * @see #benchmark(String)
     */
    default void benchmark(String s, PrintStream out) {
        // Print the name of the algorithm
        out.println("Testing " + name() + " Algorithm:");

        // Measure the time it takes to compress the string
        var now = System.nanoTime();
        ToUnifiedRuleset ruleset = compress(s);
        var duration = System.nanoTime() - now;

        // Output the compression time in milliseconds
        out.println("Compression " + name() + ": " + (duration / 1000000) + "ms");


        // Turn this Ruleset into a compression.unified representation
        now = System.nanoTime();
        UnifiedRuleset unified = ruleset.toUnified();
        duration = System.nanoTime() - now;

        out.println("Time to turn into unified Ruleset: " + (duration / 1000000) + "ms");


        // If enabled, print the grammar to the PrintStream
        if(PRINT) out.print(unified.getAsString());

        // Print the size of the generated grammar
        out.println("Grammar size: " + unified.rulesetSize());
        out.println("Grammar depth: " + unified.rulesetDepth());

        // Verify, whether this grammar can reproduce the original string
        String reconstructed = unified.buildString();
        boolean reconstructable = reconstructed.equals(s);
        out.println("Original String reconstructable? " + (reconstructable ? "Yes" : "No"));
        if(!reconstructable && PRINT) {
            var recLines = reconstructed.split("\n");
            var origLines = s.split("\n");
            for (int i = 0; i < Math.min(recLines.length, origLines.length); i++) {
                var rec = recLines[i].replace("\n", "\\n").replace("\r", "\\r");
                var orig = origLines[i].replace("\n", "\\n").replace("\r", "\\r");
                if(!rec.equals(orig)) {
                    out.println("Line " + i + ": " + rec + " /// " + orig);
                }
            }
            //out.println(reconstructed);
        }
        out.println();
    }

    default void benchmarkSimple(String input, PrintStream out, boolean printGrammar) {
        // Print the name of the algorithm
        out.println("Testing " + name() + " Algorithm on input with " + input.length() + " characters:");

        // Measure the time it takes to compress the string
        var now = System.nanoTime();
        ToUnifiedRuleset ruleset = compress(input);
        UnifiedRuleset unified = ruleset.toUnified();
        var duration = System.nanoTime() - now;

        // Output the compression time in milliseconds
        out.println("Compression Time: " + (duration / 1000000) + "ms");

        // Print the size of the generated grammar
        out.println("Grammar size: " + unified.rulesetSize());
        out.println("Grammar depth: " + unified.rulesetDepth());
        out.println("Rule count: " + unified.ruleCount());
        out.println("Average rule length: " + unified.averageRuleLength());


        // If enabled, print the grammar to the PrintStream
        if(printGrammar) {
            out.println();
            out.print(unified.getAsString());
        };
    }

    /**
     * Creates an sqlplot-tools RESULT line with the results of compressing the given file
     * @param inputFileName The file to compress
     * @throws IOException If unable to create the output directories
     */
    default void sqlplot(String inputFileName) throws IOException {
        Path inPath = Paths.get("input", inputFileName);
        String s = Files.readString(inPath, StandardCharsets.ISO_8859_1);

        StringJoiner result = new StringJoiner(" ", "RESULT ", "");

        result.add("algo=" + name());
        result.add("inputsize=" + s.length());

        // Measure the time it takes to compress the string
        var now = System.nanoTime();
        ToUnifiedRuleset ruleset = compress(s);
        var duration = System.nanoTime() - now;

        // Output the compression time in milliseconds
        result.add("comptime=" + (duration / 1000000));


        // Turn this Ruleset into a compression.unified representation
        now = System.nanoTime();
        UnifiedRuleset unified = ruleset.toUnified();
        duration = System.nanoTime() - now;

        result.add("unifytime=" + (duration / 1000000));

        result.add("size=" + unified.rulesetSize());
        result.add("numRules=" + unified.ruleCount());
        result.add("avgLen=" + unified.averageRuleLength());
        result.add("depth=" + unified.rulesetDepth());

        result.add("bucketsize=" + RuleIntervalIndex.BUCKET_SIZE_EXPONENT);

        // I am very sorry
        final int datasetsize;
        if(inputFileName.contains("50")) {
            datasetsize = 50;
        } else if(inputFileName.contains("40")) {
            datasetsize = 40;
        } else if(inputFileName.contains("30")) {
            datasetsize = 30;
        } else if(inputFileName.contains("20")) {
            datasetsize = 20;
        } else if(inputFileName.contains("10")) {
            datasetsize = 10;
        } else {
            datasetsize = -1;
        }
        result.add("datasetsize=" + datasetsize + "MB");

        Path outPath = Paths.get("sql", inputFileName.substring(0, inputFileName.lastIndexOf(String.valueOf(datasetsize)) - 1) + ".txt");
        if(!Files.exists(outPath.getParent())) {
            Files.createDirectories(outPath.getParent());
        }


        //result.add("verified=" + unified.verify(s));

        try(var stream = new PrintStream(Files.newOutputStream(outPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE))) {
            stream.println(result);
            stream.flush();
        }
    }

    /**
     * Get the name of this algorithm. This is used in {@link #benchmark(String, PrintStream)} to output the name of the algorithm
     * @return The name
     */
    String name();

}
