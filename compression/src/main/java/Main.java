import compression.areacomp.AreaFunction;
import compression.areacomp.areas.ChildArea;
import compression.areacomp.areas.HeightAdvantageArea;
import compression.areacomp.areas.HeightFirstArea;
import compression.areacomp.areas.WidthFirstArea;
import compression.areacomp.v4.AreaCompV4;
import compression.repair.RePair;
import compression.sequitur.Sequitur;
import compression.unified.interfaces.UnifiedCompressor;
import compression.utils.Benchmark;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {

    private static final int REPETITIONS = 1;

    public static void main(String[] args) throws Exception {
        run(args);
    }

    private static void benchmarkOnFiles(String[] args) throws IOException {
        if(args.length < 1) {
            System.out.println("Please specify an input file. The input file should be in a folder called \"input\" and the given path should be relative to that folder.");
            return;
        }

        for (String file : args) {
            benchmark(file);
            System.gc();
        }
    }

    private static void benchmark(String fileName) throws IOException {
        final List<UnifiedCompressor> compressors = List.of(
                new Sequitur(),
                new RePair(),
                new AreaCompV4(new HeightFirstArea()),
                new AreaCompV4(new HeightAdvantageArea()),
                new AreaCompV4(new ChildArea()),
                new AreaCompV4(new WidthFirstArea())
                //,new AreaCompV1(area)
                //new AreaCompV4(new PotentialCompressionArea()),
                //new AreaCompV2(new NaiveArea()),
                //new AreaCompV3(new ChildArea()),
                //new AreaCompV3(new LengthFirstArea()),
                //new AreaCompV3(new DepthWithAddArea()),
        );

        final var inputPath = Paths.get("input", fileName).toAbsolutePath();
        final var outputPath = Paths.get("output", fileName).toAbsolutePath();

        System.out.println("Processing " + fileName);

        final String input;
        try {
            input = Files.readString(inputPath, StandardCharsets.ISO_8859_1);
        } catch (NoSuchFileException e) {
            System.err.printf("File '%s' does not exist%n. Please make sure the input file is an a directory called \"input\".\n The input path should be relative to that directory", fileName);
            return;
        }

        if(!Files.exists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        try (PrintStream out = new PrintStream(Files.newOutputStream(outputPath))) {
            out.println("On a test string of " + input.length() + " characters.\n");
            for (int i = 0; i < REPETITIONS; i++) {
                for (UnifiedCompressor compressor : compressors) {
                    System.out.println("Testing " + compressor.name());
                    compressor.sqlplot(fileName);
                    //compressor.benchmark(input, out);
                    System.out.println("Done testing " + compressor.name());
                }
            }
        }

        Benchmark.getAllValues().forEach((algName, values) -> {
            System.out.println("Times for " + algName + ":");
            values.forEach((counterName, time) -> System.out.printf("%-25s: %s%n", counterName, time.toString()));
            System.out.println();
        });
    }

    private static void run(String[] args) throws Exception {
        Reflections reflections = new Reflections("compression.areacomp");
        Map<String, Class<? extends AreaFunction>> areaFunctionClasses = reflections.getSubTypesOf(AreaFunction.class)
                .stream()
                .filter(c -> !c.getSimpleName().equals("NaiveArea"))
                .collect(Collectors.toMap(c -> c.getSimpleName().toLowerCase(), Function.identity(), (c1, c2) -> c2, TreeMap::new));

        if(args.length < 3) {
            System.out.println("Usage: [input file] [print grammar] [algorithm] <area function if AreaComp> \n" +
                    "Print grammar: true or false\n" +
                    "Algorithm: one of Sequitur, RePair, AreaComp\n" +
                    "Area function: one of " + String.join(", ", areaFunctionClasses.keySet()));
            return;
        }

        var filePath = Paths.get(args[0]).toAbsolutePath();
        var fileName = filePath.getFileName().toString();

        if(!Files.exists(filePath)) {
            System.out.println("File '" + filePath + "' does not exist");
            return;
        }

        if(!"true".equalsIgnoreCase(args[1]) && !"false".equalsIgnoreCase(args[1])) {
            System.out.println("'Print grammar' can only be true or false");
            return;
        }

        var printGrammar = Boolean.parseBoolean(args[1]);

        var alg = switch (args[2].toLowerCase()) {
            case "repair", "re-pair" -> new RePair();
            case "sequitur" -> new Sequitur();
            case "areacomp" -> {
                if(args.length < 4) {
                    System.out.println("Missing area function while using AreaComp. One of: " + String.join(", ", areaFunctionClasses.keySet()));
                    yield null;
                }

                var area = areaFunctionClasses.get(args[3].toLowerCase());
                if(area == null) {
                    System.out.println("Invalid Area Function: " + args[3]);
                    yield null;
                }

                yield new AreaCompV4(area.getConstructor().newInstance());
            }
            default -> null;
        };

        if(alg == null) {
            if (!List.of("repair", "re-pair", "sequitur", "areacomp").contains(args[2].toLowerCase())) {
                System.out.println("Invalid algorithm: " + args[2]);
            }
            return;
        }

        var input = Files.readString(filePath);
        var outputPath = Paths.get(
                filePath.getParent().toString(),
                fileName.substring(0, fileName.lastIndexOf(".")) + ".out" + fileName.substring(fileName.lastIndexOf("."))
        );

        alg.benchmarkSimple(input, new PrintStream(Files.newOutputStream(outputPath, StandardOpenOption.TRUNCATE_EXISTING)), printGrammar);
        System.out.println("Results written to " + outputPath.toString());
    }
}
