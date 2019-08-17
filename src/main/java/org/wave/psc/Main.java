package org.wave.psc;

import me.tongfei.progressbar.ProgressBar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;


class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "thread-pool-size", true, "thread pool size for checker. Default: 100");
        options.addOption("t", "time-out-ms", true, "get proxies less than %s ms. Default: 1500");
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String logo = "   _____      _                  _____   _____  _____ \n" +
                "  / ____|    | |                |  __ \\ / ____|/ ____|\n" +
                " | (___   ___| | ___ _ __   __ _| |__) | (___ | |     \n" +
                "  \\___ \\ / _ \\ |/ _ \\ '_ \\ / _` |  ___/ \\___ \\| |     \n" +
                "  ____) |  __/ |  __/ | | | (_| | |     ____) | |____ \n" +
                " |_____/ \\___|_|\\___|_| |_|\\__,_|_|    |_____/ \\_____|\n" +
                "                                                      \n" +
                "                                                      ";
        String header = logo + "\nThis is proxy scrapper and checker from proxyscrape.com\n\n";
        String footer = "\nCreated by 2R(32-52)";
        HelpFormatter formatter = new HelpFormatter();
        assert line != null;
        if (line.hasOption('h')) {
            formatter.printHelp("SelenaPSC.jar", header, options, footer, true);
            System.exit(0);
        }
        System.out.println(logo);

        int threadPoolSize;
        if (line.getOptionValue('p') != null) {
            threadPoolSize = Integer.parseInt(line.getOptionValue('p'));
            Logger.info("threadPoolSize set on " + threadPoolSize);
        } else {
            Logger.info("threadPoolSize set on default - (100)");
            threadPoolSize = 100;
        }

        int timeOutMs;
        if (line.getOptionValue('t') != null) {
            timeOutMs = Integer.parseInt(line.getOptionValue('t'));
            Logger.info("timeOutMs set on " + timeOutMs);
        } else {
            Logger.info("timeOutMs set on default - (1500)");
            timeOutMs = 1500;
        }

        //http://patorjk.com/software/taag/#p=display&h=2&f=Big&t=v0.1
        Logger.info("Starting...");
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        Logger.info("Collecting proxies...");
        Request request = new Request.Builder()
                .url(String.format("https://api.proxyscrape.com/?request=getproxies&proxytype=http&timeout=%s&country=all&ssl=all&anonymity=all",
                        timeOutMs))
                .build();
        List<String> proxyList = Arrays.asList(Objects.requireNonNull(okHttpClient.newCall(request).execute().body()).string().split("\n"));
        okHttpClient.dispatcher().executorService().shutdown();
        okHttpClient.dispatcher().executorService().awaitTermination(1, TimeUnit.HOURS);
        Logger.info("Collecting proxies complete");
        Logger.info(String.format("Collected %s proxies", proxyList.size()));
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<String> goodProxies = new ArrayList<>();
        List<String> badProxies = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        ProgressBar progressBar = new ProgressBar("Checking proxies...", proxyList.size());
        for (String proxyString : proxyList) {
            executorService.submit(() -> {
                OkHttpClient proxyClient = null;
                try {
                    String[] splitProxy = proxyString.split(":");
                    Proxy proxy = new Proxy(Proxy.Type.HTTP,
                            InetSocketAddress.createUnresolved(
                                    splitProxy[0].trim(), Integer.valueOf(splitProxy[1].trim())));
                    proxyClient = new OkHttpClient().newBuilder().proxy(proxy).build();
                    Request proxyRequest = new Request.Builder().url("http://ip-api.com/json/").build();
                    Response response = proxyClient.newCall(proxyRequest).execute();
                    assert response.body() != null;
                    String body = response.body().string();
                    if (body.contains(splitProxy[0])) {
                        goodProxies.add(proxyString);
                    } else {
                        badProxies.add(proxyString + "|" + body);
                    }
                } catch (Exception e) {
                    errorList.add(proxyString + "|" + e.getMessage());
                } finally {
                    if (proxyClient != null) {
                        proxyClient.dispatcher().executorService().shutdown();
                    }
                    progressBar.step();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);


        progressBar.stop();
        Logger.info("-=-=-=-Statistics-=-=-=-");
        Logger.info(String.format("Good proxies:%s", goodProxies.size()));
        Logger.info(String.format("Bad proxies:%s", badProxies.size()));
        Logger.info(String.format("Errors:%s", errorList.size()));
        Logger.info("Writing to files...");

        StringBuilder goodProxiesStringBuilder = new StringBuilder();
        for (String goodProxy : goodProxies) goodProxiesStringBuilder.append(String.format("http://%s\n", goodProxy));
        goodProxiesStringBuilder.append("\n");
        Files.writeString(Paths.get("goodProxies.txt"), goodProxiesStringBuilder.toString());
        Logger.info("Writing goodProxies.txt complete");

        StringBuilder badProxiesStringBuilder = new StringBuilder();
        for (String badProxy : badProxies) badProxiesStringBuilder.append(String.format("%s\n", badProxy));
        badProxiesStringBuilder.append("\n");
        Files.writeString(Paths.get("badProxies.txt"), badProxiesStringBuilder.toString());
        Logger.info("Writing badProxies.txt complete");

        StringBuilder errorsStringBuilder = new StringBuilder();
        for (String error : errorList) errorsStringBuilder.append(String.format("%s\n", error));
        errorsStringBuilder.append("\n");
        Files.writeString(Paths.get("errorsProxies.txt"), errorsStringBuilder.toString());
        Logger.info("Writing errorsProxies.txt complete");

        Logger.info("Writing to files complete");
        Logger.info("Stopping...");
    }
}
