package compression.unified.interfaces;

import org.apache.commons.collections4.list.TreeList;
import compression.unified.UnifiedNonTerminal;
import compression.unified.UnifiedRuleset;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An interface allowing the implementing class to be converted to a {@link UnifiedRuleset}
 */
@FunctionalInterface
public interface ToUnifiedRuleset {

    /**
     * Converts this object to a {@link UnifiedRuleset}
     * @return The {@link UnifiedRuleset} representation of this object
     */
    UnifiedRuleset toUnified();


    /**
     * Reconstructs the original string from the grammar. This method can be overridden if there are more efficient ways to reconstruct the string.
     * @return The string represented by this grammar
     */
    default String buildString() {
        UnifiedRuleset unified = toUnified();
        List<UnifiedSymbol> symbols = new TreeList<>(unified.rules().get(unified.getTopLevelRuleId()));
        int i = 0;
        do {
            final var symbol = symbols.get(i);
            if(symbol instanceof UnifiedNonTerminal nonTerminal) {
                symbols.addAll(i + 1, unified.rules().get(nonTerminal.id()));
                symbols.remove(i);
            } else {
                i++;
            }
        } while (i < symbols.size());

        return symbols.stream()
                .map(Object::toString)
                .collect(Collectors.joining());
    }


    /**
     * Check, whether the string represented by the ruleset matches the given string.
     * This is used to verify, that the grammar can be used to build the original string.
     * @param reference The reference string to compare the result against.
     * @return
     */
    default boolean verify(String reference) {
        if (reference == null) throw new IllegalArgumentException("String to compare to is null");
        return buildString().equals(reference);
    }
}
