package compression.areacomp.v2;

import compression.areacomp.AreaFunction;
import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedCompressor;

public class AreaCompV2 implements UnifiedCompressor {

    /**
     * The {@link AreaFunction} which this algorithm should use to prioritize intervals in the LCP array
     */
    private AreaFunction area;

    /**
     * Creates a new instance with a given {@link AreaFunction}
     * @param area The given {@link AreaFunction}
     */
    public AreaCompV2(AreaFunction area) {
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
        return AreaCompV2.class.getSimpleName();
    }
}
