package compression.areacomp.v1;

import compression.areacomp.AreaFunction;
import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedCompressor;

public class AreaCompV1 implements UnifiedCompressor {

    private AreaFunction area;

    public AreaCompV1(AreaFunction area) {
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
        return AreaCompV1.class.getSimpleName();
    }
}
