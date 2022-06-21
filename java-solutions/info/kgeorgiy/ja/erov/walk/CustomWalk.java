package info.kgeorgiy.ja.erov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

public class CustomWalk {
    private final static String HASH_ALGORITHM = "SHA-1";
    private final static String NULL_HASH = "0000000000000000000000000000000000000000";

    public static void invoke(final String[] args,
                              final int walkerMaxDepth) {
        if (args == null || args.length != 2) {
            System.err.println("USAGE: <Walk mode> <input file> <output file>");
            return;
        }

        final Path inputPath;
        try {
            inputPath = Path.of(args[0]);
        } catch (InvalidPathException | NullPointerException e) {
            System.err.printf("Error: invalid input path: %s%n", e.getMessage());
            return;
        }

        final Path outputPath;
        try {
            outputPath = Path.of(args[1]);
        } catch (InvalidPathException | NullPointerException e) {
            System.err.printf("Error: invalid output path: %s%n", e.getMessage());
            return;
        }

        try {
            final Path outputParent = outputPath.getParent();
            if (outputParent != null) {
                Files.createDirectories(outputParent);
            } else {
                System.err.println("Error: output file must have parent dir to be created");
                return;
            }
        } catch (FileAlreadyExistsException e) {
            System.err.printf("Error: output file parent must be a dir to be created: %s%n", e.getMessage());
            return;
        } catch (IOException e) {
            System.err.printf("Error: I/O error while creating output file parent dir: %s%n", e.getMessage());
            return;
        }

        final MessageDigest hasher;
        try {
            hasher = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            System.err.printf("Error: hash algorithm %s is unsupported: %s%n", HASH_ALGORITHM, e.getMessage());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            Walker walker = new Walker(hasher, writer, NULL_HASH);
            String walkingPath;
            while ((walkingPath = reader.readLine()) != null) {
                try {
                    Files.walkFileTree(
                            Path.of(walkingPath),
                            EnumSet.noneOf(FileVisitOption.class),
                            walkerMaxDepth,
                            walker);
                } catch (InvalidPathException pathCreationExc) {
                    walker.print(NULL_HASH, walkingPath);
                }
            }
        } catch (WriterException e) {
            System.err.printf("Output error: %s%n", e.getMessage());
        } catch (IOException e) {
            System.err.printf("Input error: %s%n", e.getMessage());
        }
    }
}
