package info.kgeorgiy.ja.erov.hello;

import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Hello helper.
 */
public final class HelloUtils {

    private HelloUtils() {
    }


    /**
     * Transforms specified datagram data into string.
     *
     * @param packet datagram packet to data transformation
     * @return string value of packet data
     */
    public static String stringValue(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    /**
     * Closes specified thread pool.
     *
     * @param pool service for closing
     */
    public static void closeExecutorsPool(ExecutorService pool) {
        pool.shutdown();

        boolean terminated = false;
        while (!terminated) {
            try {
                terminated = pool.awaitTermination(50, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Parses integer value argument of array.
     *
     * @param args arguments array
     * @param ind index of specified argument to parsing
     * @return integer value of specified argument
     *
     * @throws NumberFormatException specified by {@code ind} exception, if value cannot be parsed as integer
     */
    public static int parseIntArgument(String[] args, int ind) {
        try {
            return Integer.parseInt(args[ind]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(String.format("argument no.%d must be an integer value%n", ind));
        }
    }

}
