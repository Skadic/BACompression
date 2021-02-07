import areacomp.AreaFunction;
import sequitur.Sequitur;
import utils.AugmentedString;

import java.util.Arrays;

public class Main {

    final static boolean PRINT = true;

    public static void main(String[] args) {

        var text = """
                Knox in box.
                Fox in socks.
                
                Knox on fox in socks in box.
                
                Socks on Knox and Knox in box.
                
                Fox in socks on box on Knox.
                
                Chicks with bricks come.
                Chicks with blocks come.
                Chicks with bricks and blocks and clocks come.
                
                Look, sir.  Look, sir.  Mr. Knox, sir.
                Let's do tricks with bricks and blocks, sir.
                Let's do tricks with chicks and clocks, sir.
                                
                Mr. Fox, sir,
                I won't do it.
                I can't say.
                I won't chew it.
                                
                Very well, sir.
                Step this way.
                We'll find another game to play.
                                
                Bim comes.
                Ben comes.
                Bim brings Ben broom.
                Ben brings Bim broom.
                                
                Ben bends Bim's broom.
                Bim bends Ben's broom.
                Bim's bends.
                Ben's bends.
                Ben's bent broom breaks.
                Bim's bent broom breaks.
                """;

        //var s = "aaaaaaaaaaaaaa";
        var test = "abracadcadabra";

        var test2 = "abcbeeaabcbeeadbcbzzddbcbzzd";


        var actualTestText = text;

        AreaFunction area = (sa, isa, lcp, low, hi) -> {
            // Get the longest common prefix length from the given range in the lcp array
            var lcpLen = Arrays.stream(lcp, low, hi).min().orElse(0);
            // If the length of the longest common prefix is less than or equal to 1, there is no use in replacing it.
            if(lcpLen <= 1) return 0;

            return lcpLen * (hi - low);
        };

        var a = new AugmentedString(test2);
        System.out.println(Arrays.toString(a.getLcp()));
        area.area(a.getSuffixArray(), a.getInverseSuffixArray(), a.getLcp(), 0, 2);

        System.out.println("s.length() = " + text.length());
        System.out.println();
        testAreaCompV1(actualTestText, area);
        testAreaCompV2(actualTestText, area);
        testSequitur(actualTestText);

    }

    static void testAreaCompV1(String s, AreaFunction area) {
        System.out.println("Testing AreaComp V1 Algorithm:");
        areacomp.v1.Ruleset set = new areacomp.v1.Ruleset(s);
        var now = System.nanoTime();
        set.compress(area);
        var duration = System.nanoTime() - now;
        System.out.println("Compression V1: " + (duration / 1000000) + "ms");
        System.out.println("Grammar size: " + set.ruleSetSize());
        if(PRINT) set.print();
        System.out.println();
    }

    static void testAreaCompV2(String s, AreaFunction area) {
        System.out.println("Testing AreaComp V2 Algorithm:");
        areacomp.v2.Ruleset set = new areacomp.v2.Ruleset(s);

        var now = System.nanoTime();
        set.compress(area);
        var duration = System.nanoTime() - now;
        System.out.println("Compression V2: " + (duration / 1000000) + "ms");
        System.out.println("Grammar size: " + set.ruleSetSize());
        if(PRINT) set.print();
        System.out.println();
    }

    static void testSequitur(String s) {
        System.out.println("Testing Sequitur Algorithm:");
        var now = System.nanoTime();
        var rule = Sequitur.run(s);
        var duration = System.nanoTime() - now;
        System.out.println("Compression Sq: " + (duration / 1000000) + "ms");
        System.out.println("Grammar size: " + rule.ruleSetSize());
        if(PRINT) System.out.println(rule.getRules());
        System.out.println();
    }

}
