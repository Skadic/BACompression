import areacomp.AreaFunction;
import areacomp.areas.LengthFirstArea;
import areacomp.areas.NaiveArea;
import areacomp.v3.AreaCompV3;
import repair.RePair;
import sequitur.Sequitur;
import unified.interfaces.UnifiedCompressor;
import utils.Benchmark;

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

        final AreaFunction area = new NaiveArea();
        
        final var compressors = List.of(
                new Sequitur(),
                new RePair(),
                //new AreaCompV3(new ChildArea()),
                new AreaCompV3(new LengthFirstArea())//,
                //new AreaCompV3(new DepthWithAddArea())
                //,new AreaCompV2(area)
                //,new AreaCompV1(area)
        );

        final String fileName = args[0];
        final var inputPath = Paths.get("input", fileName).toAbsolutePath();
        final var outputPath = Paths.get("output", fileName).toAbsolutePath();

        final String input;
        try {
             input = Files.readString(inputPath);
        } catch (NoSuchFileException e) {
            System.err.printf("File '%s' does not exist%n", fileName);
            return;
        }


        try (PrintStream out = new PrintStream(Files.newOutputStream(outputPath))) {
            out.println("On a test string of " + input.length() + " characters.\n");
            for (UnifiedCompressor compressor : compressors) {
                compressor.benchmark(input, out);
            }
        }

        Benchmark.getAllValues().forEach((algName, values) -> {
            System.out.println("Times for " + algName + ":");
            values.forEach((counterName, time) -> System.out.printf("%-20s: %dms%n", counterName, time / 1000000));
            System.out.println();
        });

    }
}
