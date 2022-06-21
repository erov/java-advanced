package info.kgeorgiy.ja.erov.crawler;

/**
 * {@code Exception} wrapper for message of error occurred in {@link WebCrawler}.
 */
public class WebCrawlerException extends RuntimeException {
    /**
     * Constructs {@code Exception} with specified detail message.
     *
     * @param message detail message
     */
    public WebCrawlerException(String message) {
        super(message);
    }

    /**
     * Constructs {@code Exception} with specified detail message and cause.
     *
     * @param message detail message
     * @param throwable exception cause
     */
    public WebCrawlerException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
