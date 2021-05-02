package compression.sequitur;

import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedCompressor;

import java.util.HashMap;

public class Sequitur implements UnifiedCompressor {

    @Override
    public ToUnifiedRuleset compress(String s) {
        reset();

        Rule firstRule = new Rule();
        for(char c : s.toCharArray()) {
            firstRule.last().insertAfter(new Terminal(c));
            firstRule.last().prev.check();
        }

        return firstRule;
    }

    private static void reset() {
        Rule.resetNumRules();
        Symbol.DIGRAMS = new HashMap<>();
    }

    @Override
    public String name() {
        return "Sequitur";
    }
}
