package lol.moruto.scraper.filter.impl;

import lol.moruto.scraper.Main;
import lol.moruto.scraper.filter.Filter;
import lol.moruto.scraper.filter.FilterContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class FilterByHypixelRank implements Filter {
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1500;

    private static final int MAX_REQUESTS_PER_SECOND = 5;
    private final Semaphore rateLimiter = new Semaphore(MAX_REQUESTS_PER_SECOND);

    private static final List<Rank> RANK_PRIORITY = Arrays.asList(
            Rank.MVP_PLUS_PLUS,
            Rank.MVP_PLUS,
            Rank.MVP,
            Rank.VIP_PLUS,
            Rank.VIP,
            Rank.NONE
    );

    @Override
    public List<String> filter(List<String> ignList, FilterContext ctx) {
        String desiredRank = ctx.get("desiredRank", String.class);
        if (desiredRank == null || desiredRank.equalsIgnoreCase("Don't Filter")) {
            Main.log("Rank filtering skipped.");
            return ignList;
        }

        List<String> filteredIGNs = Collections.synchronizedList(new ArrayList<>());
        int threadCount = 10; // higher concurrency
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (String ign : ignList) {
            tasks.add(() -> {
                try {
                    rateLimiter.acquire();
                    checkRankWithRetries(ign, desiredRank, filteredIGNs);
                } finally {
                    Executors.newSingleThreadScheduledExecutor().schedule(
                            () -> rateLimiter.release(),
                            1000 / MAX_REQUESTS_PER_SECOND,
                            TimeUnit.MILLISECONDS
                    );
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.log("Interrupted: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }

        Main.log("Rank filtering complete: " + filteredIGNs.size() + " matches.");
        return filteredIGNs;
    }

    private void checkRankWithRetries(String ign, String desiredRank, List<String> filteredIGNs) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                Connection.Response response = Jsoup.connect("https://plancke.io/hypixel/player/stats/" + ign)
                        .userAgent("Mozilla/5.0")
                        .timeout(8000)
                        .ignoreHttpErrors(true)
                        .execute();

                if (response.statusCode() == 429) {
                    attempts++;
                    long delay = BASE_DELAY_MS * attempts;
                    Main.log("⚠ 429 Too Many Requests for " + ign + ". Retrying in " + delay + "ms...");
                    Thread.sleep(delay);
                    continue;
                }

                if (response.statusCode() != 200) {
                    Main.log("✘ " + ign + " HTTP error: " + response.statusCode());
                    return;
                }

                Document doc = response.parse();

                Element infoBlock = doc.selectFirst("h3.m-t-0.header-title:contains(Player Information)");
                List<Element> spans = Collections.emptyList();

                if (infoBlock != null) {
                    Element sibling = infoBlock.nextElementSibling();
                    if (sibling != null) {
                        spans = sibling.select("span");
                    }
                }

                String detectedRank = detectBestRank(spans);

                if (detectedRank.equalsIgnoreCase(desiredRank)) {
                    filteredIGNs.add(ign);
                    Main.log("✔ " + ign + " has rank " + detectedRank);
                } else {
                    Main.log("✘ " + ign + " has rank " + detectedRank);
                }
                return;

            } catch (IOException | InterruptedException e) {
                Main.log("Error for " + ign + ": " + e.getMessage());
                return;
            }
        }
        Main.log("❌ Skipped " + ign + " after too many retries.");
    }

    private String detectBestRank(List<Element> spans) {
        Rank bestRank = Rank.NONE;
        for (Element span : spans) {
            String rawRank = normalizeRank(span.text());
            for (Rank rank : RANK_PRIORITY) {
                if (rank.name().equalsIgnoreCase(rawRank)) {
                    if (RANK_PRIORITY.indexOf(rank) < RANK_PRIORITY.indexOf(bestRank)) bestRank = rank;
                }
            }
        }
        return bestRank.name();
    }

    private String normalizeRank(String text) {
        return text.replaceAll("[\\[\\]]", "")
                .replace("++", "_PLUS_PLUS")
                .replace("+", "_PLUS")
                .replace(" ", "_")
                .toUpperCase();
    }

    public enum Rank {
        NONE,
        VIP,
        VIP_PLUS,
        MVP,
        MVP_PLUS,
        MVP_PLUS_PLUS
    }
}
