import areacomp.AreaFunction;
import areacomp.areas.NaiveArea;
import areacomp.v1.AreaCompV1;
import areacomp.v2.AreaCompV2;
import repair.RePair;
import sequitur.Sequitur;
import unified.interfaces.UnifiedCompressor;
import utils.Benchmark;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {

        if(args.length == 1) {
            System.out.println("Please specify an input file");
        }

        final AreaFunction area = new NaiveArea();
        
        final var compressors = List.of(
                new Sequitur(),
                new RePair(),
                new AreaCompV2(area),
                new AreaCompV1(area)
        );

        final var inputPath = Paths.get("input", args[0]).toAbsolutePath();
        final var outputPath = Paths.get("output", args[0]).toAbsolutePath();
        final var input = Files.readString(inputPath);

        try (PrintStream out = new PrintStream(Files.newOutputStream(outputPath))) {
            for (UnifiedCompressor compressor : compressors) {
                compressor.benchmark(input, out);
            }
        }

        Benchmark.getAllValues().forEach((algName, values) -> {
            System.out.println("Times for " + algName + ":");
            values.forEach((counterName, time) -> System.out.printf("%s: %dµs%n", counterName, time / 1000));
            System.out.println();
        });
    }
}
