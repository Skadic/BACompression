package lzss;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws IOException {
        // Require a file to be given as input
        if (args.length != 1) {
            System.out.println("Please input a file");
            return;
        }

        // Get the relative path to a file from the current directory
        var path = Paths.get(".", args[0]);
        // Read the file into a string
        String file = null;
        try {
            file = Files.readString(path);
        } catch (NoSuchFileException e) {
            System.err.println("File \"" + path.toString() + "\" not found");
            return;
        }


        // Generate the lz factorization string and print it
        System.out.println(LZ.factorizedString(file));

    }
}
