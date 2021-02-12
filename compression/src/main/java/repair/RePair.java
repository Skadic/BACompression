package repair;

import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedCompressor;

public class RePair implements UnifiedCompressor {
    @Override
    public ToUnifiedRuleset compress(String s) {
        var repair = new RePairDataStructure(s);
        repair.compress();
        return repair;
    }

    @Override
    public String name() {
        return "RePair";
    }
}
