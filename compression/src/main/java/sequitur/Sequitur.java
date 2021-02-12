package sequitur;

import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedCompressor;

public class Sequitur implements UnifiedCompressor {

    @Override
    public ToUnifiedRuleset compress(String s) {
        Rule.resetNumRules();

        Rule firstRule = new Rule();
        for(char c : s.toCharArray()) {
            firstRule.last().insertAfter(new Terminal(c));
            firstRule.last().prev.check();
        }

        return firstRule;
    }

    @Override
    public String name() {
        return "Sequitur";
    }
}
