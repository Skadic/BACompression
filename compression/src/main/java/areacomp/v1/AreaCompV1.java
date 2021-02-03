package areacomp.v1;

import areacomp.AreaFunction;

import java.util.ArrayList;
import java.util.List;

public class AreaCompV1 {

    public static Ruleset run(String s, AreaFunction fun) {

        Ruleset ruleset = new Ruleset(s);

        System.out.println("Size before: " + ruleset.ruleSetSize());

        ruleset.compress(fun);

        System.out.println("Size after: " + ruleset.ruleSetSize());

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
