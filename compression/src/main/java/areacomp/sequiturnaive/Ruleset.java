package areacomp.sequiturnaive;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Ruleset {

    private Rule topLevelRule;
    private Map<Integer, Rule> ruleMap = new HashMap<>();
    private int numRules;

    public Ruleset(String s) {
        this.topLevelRule = new Rule(s, this);
    }

    public void substituteAllOccurences(Rule other) {
        int n = other.length();

        for (var rule : ruleMap.values()) {
            // Skip this rule, if it's the same as the other rule
            // Otherwise we would basically delete the right side of the original rule
            if(rule == other) continue;

            var occurences = rule.getRuleIndices(other);

            for (Symbol occurence : occurences) {
                occurence.substitute(other, n);
                rule.decreaseLength(n - 1);
            }
        }
    }

    public Collection<Rule> rules() {
        return ruleMap.values();
    }

    public Map<Integer, Rule> getRuleMap() {
        return ruleMap;
    }

    public int nextRuleNumber() {
        return numRules++;
    }

    public Rule getTopLevelRule() {
        return topLevelRule;
    }

    public long ruleSetSize() {
        return rules().stream().flatMap(Rule::stream).count();
    }

    public String reconstruct() {
        boolean done = false;
        while(!done) {
            done = true;

            // Repeatedly iterate through the top level rule
            // Every Non-terminal that is found is expanded until no more non-terminals are found
            for(Symbol sym : topLevelRule.stream().collect(Collectors.toList())) {
                if (sym.isNonTerminal()) {
                    ((NonTerminal) sym).expand();
                    done = false;
                }
            }
        }
        return (String) topLevelRule.subSequence(0, topLevelRule.expandedLength());
    }
}
