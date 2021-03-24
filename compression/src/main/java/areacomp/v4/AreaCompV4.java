package areacomp.v4;

import areacomp.AreaFunction;
import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedCompressor;

import java.util.Objects;

public class AreaCompV4 implements UnifiedCompressor {

    /**
     * The {@link AreaFunction} which this algorithm should use to prioritize intervals in the LCP array
     */
    private AreaFunction area;

    /**
     * Creates a new instance with a given {@link AreaFunction}
     * @param area The given {@link AreaFunction}
     */
    public AreaCompV4(AreaFunction area) {
        Objects.requireNonNull(area);
        this.area = area;
    }

    @Override
    public ToUnifiedRuleset compress(String s) {
        Ruleset ruleset = new Ruleset(s);
        ruleset.compress(area);
        return ruleset;
    }

    @Override
    public String name() {
        return AreaCompV4.class.getSimpleName() + "/" + area.getClass().getSimpleName();
    }
}
