import compression.areacomp.areas.LengthFirstArea;
import compression.areacomp.v4.AreaCompV4;
import compression.unified.interfaces.UnifiedCompressor;
import compression.utils.Benchmark;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {

        if(args.length != 1) {
            System.out.println("Please specify an input file");
            return;
        }

        final List<UnifiedCompressor> compressors = List.of(
                //new Sequitur(),
                //new RePair(),
                //,new AreaCompV1(area)
                //new AreaCompV2(area),
                //new AreaCompV3(new ChildArea()),
                //new AreaCompV3(new LengthFirstArea()),
                new AreaCompV4(new LengthFirstArea())
                //new AreaCompV3(new DepthWithAddArea()),
        );

        final String fileName = args[0];
        final var inputPath = Paths.get("input", fileName).toAbsolutePath();
        final var outputPath = Paths.get("output", fileName).toAbsolutePath();

        final String input;
        try {
             input = Files.readString(inputPath);
        } catch (NoSuchFileException e) {
            System.err.printf("File '%s' does not exist%n. Please make sure the input file is an a directory called \"input\".\n The input path should be relative to that directory", fileName);
            return;
        }

        if(!Files.exists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        try (PrintStream out = new PrintStream(Files.newOutputStream(outputPath))) {
            out.println("On a test string of " + input.length() + " characters.\n");
            for (UnifiedCompressor compressor : compressors) {
                System.out.println("Testing " + compressor.name());
                compressor.benchmark(input, out);
                System.out.println("Done testing " + compressor.name());
            }
        }

        Benchmark.getAllValues().forEach((algName, values) -> {
            System.out.println("Times for " + algName + ":");
            values.forEach((counterName, time) -> System.out.printf("%-25s: %s%n", counterName, time.toString()));
            System.out.println();
        });

    }
}
