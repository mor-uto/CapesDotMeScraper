package lol.moruto.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class FilterByHypixelRank {
    private final JTextArea consoleArea;
    private ExecutorService executorService;

    public FilterByHypixelRank(JTextArea consoleArea) {
        this.consoleArea = consoleArea;
    }

    public void startFiltering(List<String> ignList, String desiredRank) {
        int threads = Math.min(ignList.size(), 10); // Use up to 10 threads for parallel scraping
        executorService = Executors.newFixedThreadPool(threads);

        log("Filtering players with rank: " + desiredRank + " using " + threads + " threads.");

        for (String ign : ignList) {
            executorService.submit(() -> filterIGN(ign, desiredRank));
        }

        // Shut down executor and wait for all tasks to complete
        new Thread(() -> {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                    log("Timeout reached, forcing shutdown.");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("Shutdown interrupted: " + e.getMessage());
            }
            log("Finished filtering.");
        }).start();
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

            final String logMessage = (rank.equalsIgnoreCase(desiredRank)
                    ? "✔ " + ign + " has rank " + rank
                    : "✘ " + ign + " has rank " + rank);
            log(logMessage);

        } catch (IOException e) {
            log("Error fetching data for " + ign + ": " + e.getMessage());
        }
    }

    public void waitTillFinished() {
        if (executorService != null) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("Waiting interrupted: " + e.getMessage());
            }
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
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
