package unified.interfaces;

import unified.UnifiedRuleset;

import java.io.PrintStream;

public interface UnifiedCompressor {

    boolean PRINT = true;

    ToUnifiedRuleset compress(String s);

    default void benchmark(String s) {
        benchmark(s, System.out);
    }

    default void benchmark(String s, PrintStream out) {
        out.println("Testing " + name() + " Algorithm:");

        var now = System.nanoTime();
        ToUnifiedRuleset ruleset = compress(s);
        var duration = System.nanoTime() - now;

        out.println("Compression " + name() + ": " + (duration / 1000000) + "ms");

        UnifiedRuleset unified = ruleset.toUnified();
        if(PRINT) out.print(unified.getAsString());
        out.println("Grammar size: " + unified.rulesetSize());
        out.println("Original String reconstructable? " + (unified.verify(s) ? "Yes" : "No"));
        out.println();
    }

    String name();

}
