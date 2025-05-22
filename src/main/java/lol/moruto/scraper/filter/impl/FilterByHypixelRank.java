package lol.moruto.scraper.filter.impl;

import lol.moruto.scraper.Main;
import lol.moruto.scraper.filter.Filter;
import lol.moruto.scraper.filter.FilterContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class FilterByHypixelRank implements Filter {

    @Override
    public List<String> filter(List<String> ignList, FilterContext ctx) {
        String desiredRank = ctx.get("desiredRank", String.class);
        if (desiredRank == null || desiredRank.equalsIgnoreCase("Don't Filter")) {
            Main.log("Rank filtering skipped.");
            return ignList;
        }

        List<String> filteredIGNs = Collections.synchronizedList(new ArrayList<>());

        int threadCount = Math.max(1, Math.min(10, ignList.size()));
        Main.log("Filtering for rank " + desiredRank + " with " + threadCount + " threads.");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (String ign : ignList) {
            tasks.add(() -> {
                checkRank(ign, desiredRank, filteredIGNs);
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

    private void checkRank(String ign, String desiredRank, List<String> filteredIGNs) {
        try {
            Document doc = Jsoup.connect("https://plancke.io/hypixel/player/stats/" + ign)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();
            Element rankElem = doc.selectFirst("h4.card-title span.label");
            String rank = rankElem == null ? "NONE" : normalizeRank(rankElem.text());
            if (rank.equalsIgnoreCase(desiredRank)) {
                filteredIGNs.add(ign);
                Main.log("✔ " + ign + " has rank " + rank);
            } else {
                Main.log("✘ " + ign + " has rank " + rank);
            }
        } catch (IOException e) {
            Main.log("Error for " + ign + ": " + e.getMessage());
        }
    }

    private String normalizeRank(String text) {
        return text.replaceAll("[\\[\\]+]", "").replace(" ", "_").replace("++", "_PLUS_PLUS").replace("+", "_PLUS").toUpperCase();
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
