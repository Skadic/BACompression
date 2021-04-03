package compression.areacomp.v3;

import compression.areacomp.AreaFunction;
import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedCompressor;

import java.util.Objects;

public class AreaCompV3 implements UnifiedCompressor {

    /**
     * The {@link AreaFunction} which this algorithm should use to prioritize intervals in the LCP array
     */
    private AreaFunction area;

    /**
     * Creates a new instance with a given {@link AreaFunction}
     * @param area The given {@link AreaFunction}
     */
    public AreaCompV3(AreaFunction area) {
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
        return AreaCompV3.class.getSimpleName() + "/" + area.getClass().getSimpleName();
    }
}
