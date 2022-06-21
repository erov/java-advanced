package info.kgeorgiy.ja.erov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HexFormat;

public class Walker extends SimpleFileVisitor<Path> {
    private final static int BUFFER_SIZE = 1 << 16;
    private final MessageDigest hasher;
    private final BufferedWriter writer;
    private final String NULL_HASH;

    public Walker(MessageDigest hasher, BufferedWriter writer, String NULL_HASH) {
        this.hasher = hasher;
        this.writer = writer;
        this.NULL_HASH = NULL_HASH;
    }

    public void print(String hash, String file) throws WriterException {
        try {
            writer.write(String.format("%s %s", hash, file));
            writer.newLine();
        } catch (IOException e) {
            throw new WriterException(String.format("cannot write '%s' file info: ", file), e);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        try (InputStream fileReader = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int readBytes;
            hasher.reset();
            while ((readBytes = fileReader.read(buffer)) != -1) {
                hasher.update(buffer, 0, readBytes);
            }
        } catch (IOException fileReadingExc) {
            print(NULL_HASH, file.toString());
            return FileVisitResult.CONTINUE;
        }
        print(HexFormat.of().formatHex(hasher.digest()), file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if (exc instanceof WriterException) {
            throw exc;
        }
        print(NULL_HASH, file.toString());
        return FileVisitResult.CONTINUE;
    }
}
