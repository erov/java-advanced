package info.kgeorgiy.ja.erov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static info.kgeorgiy.ja.erov.hello.HelloUtils.parseIntArgument;


/**
 * Handles requests from {@link info.kgeorgiy.java.advanced.hello.HelloClient} multithreaded.
 */
public class HelloUDPServer implements HelloServer {
    private ExecutorService handlersPool;
    private DatagramSocket socket;
    private boolean started;


    /**
     * Calls {@link #start(int, int)} on specified parameters. Closes Hello server after reading {@code close} from
     * {@link System#in} or closing {@link System#in}.
     * Prints into {@link System#err} message if any errors occur.
     *
     * @param args parameters for start: {@code port threads}
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("USAGE: HelloUDPServer port threads");
            return;
        }

        final int port;
        final int threads;

        try {
            port = parseIntArgument(args, 0);
            threads = parseIntArgument(args, 1);
        } catch (NumberFormatException e) {
            System.err.printf("HelloUDPServer error: %s%n", e.getMessage());
            return;
        }

        try (HelloUDPServer server = new HelloUDPServer();
             Scanner scanner = new Scanner(System.in)) {

            server.start(port, threads);

            for (;;) {
                if (!scanner.hasNextLine() || Objects.equals("close", scanner.nextLine())) {
                    break;
                }
            }
        }

    }

    /**
     * Creates non-started Hello server.
     */
    public HelloUDPServer() {
        handlersPool = null;
        socket = null;
        started = false;
    }

    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port server port
     * @param threads number of working threads
     *
     * @throws HelloUDPClientException if socket cannot be opened
     * @throws HelloUDPClientException if {@link RejectedExecutionException} was suppressed while creating thread pool
     */
    @Override
    public void start(int port, int threads) {
        if (started) {
            return;
        }

        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be an integer value in [0, 65535]");
        }

        handlersPool = Executors.newFixedThreadPool(threads, Executors.defaultThreadFactory());

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new HelloUPDServerException("UDP socket error occurred", e);
        }

        final int bufferSize;
        try {
            bufferSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            closeSocket();
            throw new HelloUPDServerException("UDP socket error occurred", e);
        }

        int i = 0;
        try {
            for (i = 0; i != threads; ++i) {
                handlersPool.submit(getHandlerTask(socket, bufferSize));
            }
        } catch (RejectedExecutionException e) {
            closeSocket();
            throw new HelloUDPClientException(String.format("cannot create task for thread no.%d", i), e);
        }

        started = true;
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        if (started) {
            closeSocket();
        }
    }


    private void closeSocket() {
        socket.close();
        HelloUtils.closeExecutorsPool(handlersPool);
    }


    private Runnable getHandlerTask(final DatagramSocket socket, final int bufferSize) {
        return () -> {
            final byte[] buffer = new byte[bufferSize];
            final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (!Thread.interrupted() && !socket.isClosed()) {
                if (packet.getLength() != buffer.length) {
                    packet.setData(buffer);
                }

                try {
                    socket.receive(packet);
                } catch (IOException | IllegalBlockingModeException e) {
                    // ignore
                }

                // :NOTE: Несмотря на то, что текущий способ получения ответа по запросу очень прост,
                // сервер должен быть рассчитан на ситуацию,
                // когда этот процесс может требовать много ресурсов и времени.
                final byte[] message = getResponseMessageBytes(HelloUtils.stringValue(packet));
                packet.setData(message);

                try {
                    socket.send(packet);
                } catch (IOException | IllegalBlockingModeException e) {
                    // ignore
                }
            }
        };
    }


    private String getResponseMessage(final String request) {
        return String.format("Hello, %s", request);
    }

    private byte[] getResponseMessageBytes(final String request) {
        return getResponseMessage(request).getBytes(StandardCharsets.UTF_8);
    }
}
