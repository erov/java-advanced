package info.kgeorgiy.ja.erov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;


/**
 * Crawls websites.
 */
public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;


    /**
     * Calls {@link #download(String, int)} on specified parameters, or default parameters values for absence once.
     * Prints into {@link System#err} message if any errors occur.
     *
     * @param args parameters for downloading: {@code url [depth [downloads [extractors [perHost]]]]}
     */
    public static void main(String[] args) {
        if (args.length == 0 || args.length > 5) {
            System.err.println("USAGE: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        int[] parameters = new int[4];
        for (int i = 0; i != 4; ++i) {
            try {
                parameters[i] = i + 1 < args.length
                        ? Integer.parseInt(args[i + 1])
                        : 1;
            } catch (NumberFormatException e) {
                System.err.printf("WebCrawler error: optional argument no.%d must be an integer value%n", i);
                return;
            }
        }

        Result result;
        try {
            try(WebCrawler webCrawler = new WebCrawler(
                    new CachingDownloader(),
                    parameters[1],
                    parameters[2],
                    parameters[3])) {

                result = webCrawler.download(args[0], parameters[0]);
            }
        } catch (IOException e) {
            System.err.printf("WebCrawler error: %s%n", e.getMessage());
            return;
        }

        System.out.printf("Downloaded: %s%nErrors: %s%n", result.getDownloaded(), result.getErrors());
    }


    /**
     * Constructs {@code AdvancedCrawler} with specified bounds.
     *
     * @param downloader instrument for downloading documents and extracting links from them
     * @param downloaders maximum number of simultaneously downloading pages
     * @param extractors maximum number of pages for simultaneously links extracting
     * @param perHost maximum number of pages for simultaneously downloading from same host
     *
     * @see URLUtils#getHost(String)
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders, Executors.defaultThreadFactory());
        this.extractorsPool = Executors.newFixedThreadPool(extractors, Executors.defaultThreadFactory());
        this.perHost = perHost;
    }


    /**
     * @throws IllegalStateException if method is called after closing
     * @throws WebCrawlerException wrapped {@link MalformedURLException}, occurred during parsing host while crawling
     * @throws WebCrawlerException wrapped {@link RejectedExecutionException},
     *          occurred while submitting task to {@code ThreadPool}
     */
    @Override
    public Result download(String url, int depth, List<String> hosts) {
        if (hosts == null) {
            throw new IllegalArgumentException("hosts must be non-null list of urls");
        }
        return downloadImpl(url, depth, hosts, true);
    }

    /**
     * @throws IllegalStateException if method is called after closing
     * @throws WebCrawlerException wrapped {@link MalformedURLException}, occurred during parsing host while crawling
     * @throws WebCrawlerException wrapped {@link RejectedExecutionException},
     *           occurred while submitting task to {@code ThreadPool}
     */
    @Override
    public Result download(String url, int depth) {
        return downloadImpl(url, depth, null, false);
    }

    @Override
    public void close() {
        closePool(downloadersPool);
        closePool(extractorsPool);
    }

    private void closePool(ExecutorService pool) {
        pool.shutdown();

        boolean terminated = false;
        while (!terminated) {
            try {
                terminated = pool.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private Result downloadImpl(String url, int depth, List<String> hosts, boolean followHosts) {
        if (url == null) {
            throw new IllegalArgumentException("url must be non-null value");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be a non-negative integer");
        }
        if (downloadersPool.isShutdown() || extractorsPool.isShutdown()) {
            throw new IllegalStateException("Cannot download url via closed WebCrawler");
        }

        URLWalker urlWalker = new URLWalker(followHosts ? getHostConcurrentSet(hosts) : null);
        urlWalker.walk(url, depth);
        return urlWalker.getResult();
    }

    private Set<String> getHostConcurrentSet(List<String> hosts) {
        Set<String> result = ConcurrentHashMap.newKeySet();
        result.addAll(hosts);
        return result;
    }


    /**
     * Implements thread-safe website downloading up to specified depth.
     */
    private class URLWalker {
        private final ConcurrentMap<String, IOException> errors;
        private final Set<String> downloaded;
        private final Set<String> followingHosts;
        private final ConcurrentHashMap<String, HostController> hostControllers;


        /**
         * Constructs {@code Walker} with specified following hosts list.
         *
         * @param hosts following hosts, pages on another domain are ignores
         */
        public URLWalker(final Set<String> hosts) {
            errors = new ConcurrentHashMap<>();
            downloaded = ConcurrentHashMap.newKeySet();
            followingHosts = hosts;
            hostControllers = new ConcurrentHashMap<>();
        }

        /**
         * Downloads website up to specified depth using non-parallel breadth-first search.
         *
         * @param rootUrl start <a href="http://tools.ietf.org/html/rfc3986">URL</a>
         * @param depth download depth
         */
        public void walk(final String rootUrl, final int depth) {
            if (depth == 0) {
                return;
            }

            final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
            final ConcurrentHashMap<String, Integer> visited = new ConcurrentHashMap<>();
            final Phaser phaser = new Phaser();

            urlQueue.add(rootUrl);
            visited.put(rootUrl, depth);
            phaser.register();

            while (!urlQueue.isEmpty()) {
                String url = urlQueue.poll();
                submitDownloadingTask(urlQueue, visited, phaser, url);

                if (urlQueue.isEmpty() || !Objects.equals(visited.get(urlQueue.peek()), visited.get(url))) {
                    phaser.arriveAndAwaitAdvance();
                }
            }

            phaser.arriveAndAwaitAdvance();
        }

        private void submitExtractingTask(final Queue<String> urlQueue,
                                          final ConcurrentHashMap<String, Integer> visited,
                                          final Phaser phaser,
                                          final String url,
                                          final Document document) {
            phaser.register();
            extractorsPool.submit(() -> {
                try {
                    List<String> links;
                    try {
                        links = document.extractLinks();
                    } catch (IOException e) {
                        errors.put(url, e);
                        return;
                    }

                    urlQueue.addAll(links.stream()
                            .filter(link -> visited.putIfAbsent(link, visited.get(url) - 1) == null)
                            .toList()
                    );
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }

        private void submitDownloadingTask(final Queue<String> urlQueue,
                                           final ConcurrentHashMap<String, Integer> visited,
                                           final Phaser phaser,
                                           final String url) {
            String host = getHost(url);
            if (!checkHostFollowing(host)) {
                return;
            }

            phaser.register();
            HostController hostController = hostControllers.computeIfAbsent(host, ignored -> new HostController());

            try {
                hostController.connectionAcquire(() -> {
                    try {
                        Document document;
                        try {
                            document = downloader.download(url);
                        } catch (IOException e) {
                            errors.put(url, e);
                            return;
                        } finally {
                            hostController.connectionRelease();
                        }

                        downloaded.add(url);

                        if (visited.get(url) != 1) {
                            submitExtractingTask(urlQueue, visited, phaser, url, document);
                        }
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                });
            } catch (RejectedExecutionException e) {
                throw new WebCrawlerException("cannot handling task: neither free threads, nor free space in queue", e);
            }
        }

        private boolean checkHostFollowing(final String host) {
            return followingHosts == null || followingHosts.contains(host);
        }

        public Result getResult() {
            return new Result(downloaded.stream().toList(), errors);
        }

        private String getHost(final String url) {
            try {
                return URLUtils.getHost(url);
            } catch (MalformedURLException e) {
                throw new WebCrawlerException("Invalid URL was found during pages walking", e);
            }
        }


        /**
         * Controls number of simultaneously downloading pages from same host
         * using saving extra connection into first-in-first-out order.
         */
        private class HostController {
            private final Queue<Runnable> extraConnections;
            private int connections;

            /**
             * Constructs {@code Controller} with zeroes connection.
             */
            public HostController() {
                extraConnections = new ArrayDeque<>(1 << 10);
                connections = 0;
            }

            /**
             * Acquires connection if it's possible, postpones in queue otherwise.
             *
             * @param task connection handler
             */
            public synchronized void connectionAcquire(final Runnable task) {
                if (connections != perHost) {
                    ++connections;
                    downloadersPool.submit(task);
                } else {
                    extraConnections.add(task);
                }
            }

            /**
             * Released connection, acquires first of postponed if any exists, allows new connection otherwise.
             */
            public synchronized void connectionRelease() {
                if (extraConnections.isEmpty()) {
                    --connections;
                } else {
                    downloadersPool.submit(extraConnections.poll());
                }
            }

        }

    }

}
