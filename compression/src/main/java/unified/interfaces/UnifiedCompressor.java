package unified.interfaces;

public interface UnifiedCompressor {

    ToUnifiedRuleset compress(String s);

    default void benchmark(String s) {
        System.out.println("Testing " + name() + " Algorithm:");

        var now = System.nanoTime();
        ToUnifiedRuleset ruleset = compress(s);
        var duration = System.nanoTime() - now;

        System.out.println("Compression " + name() + ": " + (duration / 1000000) + "ms");

        System.out.println(ruleset.toUnified().getAsString());
        System.out.println();
    }

    String name();

}
