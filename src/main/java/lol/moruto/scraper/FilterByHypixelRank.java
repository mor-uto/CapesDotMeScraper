package lol.moruto.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FilterByHypixelRank {
    private final List<String> filteredIGNs = Collections.synchronizedList(new ArrayList<>());

    public void startFiltering(List<String> ignList, String desiredRank) {
        int threads = Math.min(ignList.size(), 10);
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        Main.log("Filtering players with rank: " + desiredRank + " using " + threads + " threads.");

        for (String ign : ignList) {
            executorService.submit(() -> filterIGN(ign, desiredRank));
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                Main.log("Timeout reached, forcing shutdown.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.log("Shutdown interrupted: " + e.getMessage());
        }

        Main.log("Finished filtering.");
    }

    private void filterIGN(String ign, String desiredRank) {
        try {
            Document doc = Jsoup.connect("https://plancke.io/hypixel/player/stats/" + ign)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();

            Elements rankElements = doc.select("div:contains(Rank) + div");

            String rank = "UNKNOWN";
            if (!rankElements.isEmpty()) {
                rank = rankElements.first().text().trim();
            }

            if (rank.equalsIgnoreCase(desiredRank)) {
                filteredIGNs.add(ign);
                Main.log("✔ " + ign + " has rank " + rank);
            } else {
                Main.log("✘ " + ign + " has rank " + rank);
            }

        } catch (IOException e) {
            Main.log("Error fetching data for " + ign + ": " + e.getMessage());
        }
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
