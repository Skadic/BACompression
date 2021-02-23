package utils;

import java.util.Objects;

/**
 * Represents an index local to the rule with the given {@link #ruleId}
 *
 * For example the grammar:
 * R0 -> a b R1 R1 e f
 * R1 -> cd
 * Then the rule local index for 'c' would be (1, 0), since this 'c' appears in rule 1, at index 0
 */
public record RuleLocalIndex(int ruleId, int index) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleLocalIndex ruleLocalIndex = (RuleLocalIndex) o;
        return ruleId == ruleLocalIndex.ruleId &&
                index == ruleLocalIndex.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, index);
    }
}
