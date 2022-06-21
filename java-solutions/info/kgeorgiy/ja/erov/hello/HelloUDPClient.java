package info.kgeorgiy.ja.erov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static info.kgeorgiy.ja.erov.hello.HelloUtils.parseIntArgument;


/**
 * Sends requests to {@link info.kgeorgiy.java.advanced.hello.HelloServer} multithreaded.
 */
public class HelloUDPClient implements HelloClient {

    /**
     * Calls {@link #run(String, int, String, int, int)} on specified parameters.
     * Prints into {@link System#err} message if any errors occur.
     *
     * @param args parameters for run: {@code host port prefix threads requests}
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("USAGE: HelloUDPClient host port prefix threads requests");
            return;
        }

        final String host = args[0];
        final int port;
        final String prefix = args[2];
        final int threads;
        final int requests;

        try {
            port = parseIntArgument(args, 1);
            threads = parseIntArgument(args, 3);
            requests = parseIntArgument(args, 4);
        } catch (NumberFormatException e) {
            System.err.printf("HelloUDPClient error: %s%n", e.getMessage());
            return;
        }

        final HelloUDPClient client = new HelloUDPClient();
        client.run(host, port, prefix, threads, requests);
    }


    /**
     * Runs Hello client.
     * This method should return when all requests completed.
     *
     * @param host server host
     * @param port server port
     * @param prefix request prefix
     * @param threads number of request threads
     * @param requests number of requests per thread
     *
     * @throws HelloUDPClientException if {@link RuntimeException} was occurred during sending requests
     * @throws HelloUDPClientException if {@link RejectedExecutionException} was suppressed while creating thread pool
     */
    @Override
    public void run(final String host,
                    final int port,
                    final String prefix,
                    final int threads,
                    final int requests) {

        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be an integer value in [0, 65535]");
        }

        if (prefix == null) {
            throw new IllegalArgumentException("prefix must be a non-null string value");
        }

        if (requests < 0) {
            throw new IllegalArgumentException("requests must be a non-negative integer value");
        }

        final SocketAddress serverAddress;
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("host must be valid IP or hostname");
        }

        final ExecutorService sendersPool = Executors.newFixedThreadPool(threads, Executors.defaultThreadFactory());
        final List<Future<?>> sendersFuture = new ArrayList<>();

        for (int i = 0; i != threads; ++i) {
            try {
                sendersFuture.add(sendersPool.submit(getSenderTask(i, requests, prefix, serverAddress)));
            } catch (RejectedExecutionException e) {
                throw new HelloUDPClientException(String.format("cannot create task for thread no.%d", i), e);
            }
        }

        HelloUtils.closeExecutorsPool(sendersPool);

        HelloUDPClientException exception = null;
        for (final Future<?> future : sendersFuture) {
            try {
                future.get();
            } catch (ExecutionException e) {
                if (exception == null) {
                    exception = new HelloUDPClientException("exception was occurred during execution", e);
                } else {
                    exception.addSuppressed(e);
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (exception != null) {
            throw exception;
        }
    }


    private String getMessage(final String prefix, final int threadId, final int requestId) {
        return String.format("%s%d_%d", prefix, threadId, requestId);
    }

    private byte[] getMessageBytes(final String prefix, final int threadId, final int requestId) {
        return getMessage(prefix, threadId, requestId).getBytes(StandardCharsets.UTF_8);
    }


    private Runnable getSenderTask(final int threadId,
                                   final int requests,
                                   final String prefix,
                                   final SocketAddress serverAddress) {

        return () -> {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(25);

                final int bufferSize = socket.getReceiveBufferSize();
                final DatagramPacket responsePacket = new DatagramPacket(new byte[bufferSize], bufferSize);
                final DatagramPacket requestPacket = new DatagramPacket(new byte[0], 0, serverAddress);

                for (int i = 0; i != requests; ++i) {
                    final byte[] message = getMessageBytes(prefix, threadId, i);
                    requestPacket.setData(message);

                    for (;;) {
                        try {
                            socket.send(requestPacket);
                            socket.receive(responsePacket);

                            String response = HelloUtils.stringValue(responsePacket);
                            if (checkResponse(response, String.valueOf(threadId), String.valueOf(i))) {
                                printRequestInfo(HelloUtils.stringValue(requestPacket), response);
                                break;
                            }
                        } catch (IOException e) {
                            // try to send again
                        }
                    }
                }

            } catch (SocketException e) {
                throw new HelloUDPClientException("UDP socket error occurred", e);
            }
        };
    }


    private void printRequestInfo(final String request, final String response) {
        synchronized (System.out) {
            System.out.printf("Client: %s%nServer: %s%n", request, response);
        }
    }

    private boolean checkResponse(final String response, final String threadId, final String requestId) {
        return response.matches(String.format("[\\D]*%s[\\D]+%s[\\D]*", threadId, requestId));
    }

}
