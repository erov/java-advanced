package info.kgeorgiy.ja.erov.walk;

import java.io.IOException;

public class WriterException extends IOException {
    WriterException(String message, Throwable cause) {
        super(message, cause);
    }
}
