package areacomp.v2;

import java.util.Objects;

record RuleIndex(int ruleId, int index) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleIndex ruleIndex = (RuleIndex) o;
        return ruleId == ruleIndex.ruleId &&
                index == ruleIndex.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, index);
    }
}
