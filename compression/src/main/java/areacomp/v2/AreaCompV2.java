package areacomp.v2;

import areacomp.AreaFunction;
import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedCompressor;

public class AreaCompV2 implements UnifiedCompressor {

    private AreaFunction area;

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
        return "AreaComp V2";
    }
}
