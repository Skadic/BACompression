package unified.interfaces;

import unified.UnifiedRuleset;

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

}
