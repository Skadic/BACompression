import areacomp.AreaFunction;
import areacomp.areas.NaiveArea;
import areacomp.v1.AreaCompV1;
import repair.RePairDataStructure;
import sequitur.Sequitur;

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
        var test = "abababracadabraraaaa";
        var test2 = "abcbeeaabcbeeadbcbzzddbcbzzd";
        var test3 = "ababababababababa";
        var actualTestText = text;

        AreaFunction area = new NaiveArea();

        /*System.out.println("Input length: = " + actualTestText.length());
        System.out.println();
        testAreaCompV1(actualTestText, area);
        testAreaCompV2(actualTestText, area);*/
        //new Sequitur().benchmark(actualTestText);
        //new RePair().benchmark(actualTestText);
        //testRePair(actualTestText);
        new AreaCompV1(area).benchmark(text);

    }

    static void testAreaCompV1(String s, AreaFunction area) {
        System.out.println("Testing AreaComp V1 Algorithm:");

        var now = System.nanoTime();
        areacomp.v1.Ruleset set = new areacomp.v1.Ruleset(s);
        set.compress(area);
        var duration = System.nanoTime() - now;

        System.out.println("Compression V1: " + (duration / 1000000) + "ms");
        System.out.println("Grammar size: " + set.ruleSetSize());
        if(PRINT) set.print();
        System.out.println();
    }

    static void testAreaCompV2(String s, AreaFunction area) {
        System.out.println("Testing AreaComp V2 Algorithm:");

        var now = System.nanoTime();
        areacomp.v2.Ruleset set = new areacomp.v2.Ruleset(s);
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
        var rule = new Sequitur().compress(s);
        var duration = System.nanoTime() - now;

        System.out.println("Compression Sq: " + (duration / 1000000) + "ms");
        System.out.println("Grammar size: " + rule.toUnified().rulesetSize());
        if(PRINT) System.out.println(rule.toUnified().getAsString());
        System.out.println();
    }

    static void testRePair(String s) {
        System.out.println("Testing RePair Algorithm:");

        var now = System.nanoTime();
        RePairDataStructure rePair = new RePairDataStructure(s);
        rePair.compress();
        var duration = System.nanoTime() - now;

        System.out.println("Compression RePair: " + (duration / 1000000) + "ms");

        System.out.println(rePair.getAllRules(!PRINT));
        System.out.println();
    }

}
