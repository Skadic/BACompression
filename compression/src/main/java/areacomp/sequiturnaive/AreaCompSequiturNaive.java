package areacomp.sequiturnaive;

import areacomp.AreaFunction;
import org.javatuples.Pair;
import utils.AugmentedString;

import java.util.*;
import java.util.stream.IntStream;

public class AreaCompSequiturNaive {

    public static Ruleset run(String s, AreaFunction fun) {

        System.out.println("s.length() = " + s.length());

        Ruleset ruleset = new Ruleset(s);

        boolean unchanged = false;
        while (!unchanged) {
            unchanged = true;
            var ruleList = new ArrayList<>(ruleset.rules());
            for (var rule : ruleList) {
                if(rule.length() <= 3) continue;

                final var augS = new AugmentedString(rule);
                // Create a Priority Queue which holds possible intervals in the LCP array
                // The priority value is calculated through the given area function.
                // This function should return a high value for promising intervals in the LCP array.
                var queue = new PriorityQueue<Pair<Integer, Integer>>(
                        Comparator.comparingInt(
                                r -> -fun.area(augS.getSuffixArray(), augS.getInverseSuffixArray(), augS.getLcp(), r.getValue0(), r.getValue1())
                        )
                );

                // Add all possible intervals
                for (int i = 1; i <= augS.length(); i++) {
                    for (int j = i + 1; j <= augS.length(); j++) {
                        queue.add(new Pair<>(i, j));
                    }
                }

                // Poll the best interval
                var interval = queue.poll();

                // The positions at which the pattern can be found
                int[] positions = IntStream.range(interval.getValue0() - 1, interval.getValue1())
                        .map(augS::suffixIndex)
                        .toArray();


                // Get the length of the longest common prefix in this range of the lcp array
                // This will be the length of the pattern that is to be replaced.
                int len = IntStream.range(interval.getValue0(), interval.getValue1()).map(augS::lcp).min().getAsInt();

                // This means there is no repeated subsequence of 2 or more characters. In this case, abort
                if(len > 1) {
                    unchanged = false;
                } else {
                    continue;
                }


                Arrays.sort(positions);

                positions = nonOverlapping(positions, len);

                rule.factorize(len, positions);
            }
        }

        System.out.println(ruleset.getTopLevelRule().getAllRules());

        return ruleset;
    }


    private static int[] nonOverlapping(int[] positions, int patternLength) {
        List<Integer> list = new ArrayList<>();
        int last = Short.MIN_VALUE;
        for (Integer i : positions) {
            if(i - last >= patternLength) {
                list.add(i);
                last = i;
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

}
