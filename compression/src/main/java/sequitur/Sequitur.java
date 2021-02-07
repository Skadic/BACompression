package sequitur;

public class Sequitur {

    public static Rule run(String s) {
        Rule firstRule = new Rule();

        Rule.resetNumRules();
        for(char c : s.toCharArray()) {
            firstRule.last().insertAfter(new Terminal(c));
            firstRule.last().prev.check();
        }
        return firstRule;
    }

}
