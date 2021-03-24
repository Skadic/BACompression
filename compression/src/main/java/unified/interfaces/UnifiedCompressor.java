package unified.interfaces;

import unified.UnifiedRuleset;

import java.io.PrintStream;

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

        // Output the compression time in milliseconts
        out.println("Compression " + name() + ": " + (duration / 1000000) + "ms");

        // Turn this Ruleset into a unified representation
        UnifiedRuleset unified = ruleset.toUnified();

        // If enabled, print the grammar to the PrintStream
        if(PRINT) out.print(unified.getAsString());

        // Print the size of the generated grammar
        out.println("Grammar size: " + unified.rulesetSize());

        // Verify, whether this grammar can reproduce the original string
        //String reconstructed = unified.buildString();
        //boolean reconstructable = reconstructed.equals(s);
        //out.println("Original String reconstructable? " + (reconstructable ? "Yes" : "No"));
        //if(!reconstructable && PRINT) out.println(reconstructed);
        out.println();
    }

    /**
     * Get the name of this algorithm. This is used in {@link #benchmark(String, PrintStream)} to output the name of the algorithm
     * @return The name
     */
    String name();

}
