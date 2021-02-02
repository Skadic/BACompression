import areacomp.sequiturnaive.AreaCompSequiturNaive;
import areacomp.sequiturnaive.Ruleset;
import sequitur.Sequitur;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Main {


    public static void main(String[] args) {

        var s = """
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

        Ruleset set = AreaCompSequiturNaive.run(s, (sa, isa, lcp, low, hi) -> {
            // Get the longest common prefix length from the given range in the lcp array
            var lcpLen = Arrays.stream(lcp, low, hi).min().orElse(0);
            // If the length of the longest common prefix is less than or equal to 1, there is no use in replacing it.
            if(lcpLen <= 1) return 0;

            return lcpLen * (hi - low);
        });

        System.out.println();

        Sequitur.run(s);

        System.out.println();

        System.out.println(set.reconstruct());
    }
}
