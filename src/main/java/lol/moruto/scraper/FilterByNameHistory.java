package lol.moruto.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

public class FilterByNameHistory {

    public void startProcessing(List<String> igns) {
        new Thread(() -> {
            for (String ign : igns) {
                if (ign.trim().isEmpty()) continue;
                processIgn(ign.trim());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Main.log("Interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            Main.log("Finished processing all IGNs.");
        }).start();
    }

    private void processIgn(String ign) {
        try {
            Main.log("Searching NameMC for IGN: " + ign);
            String url = "https://namemc.com/profile/" + ign;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(8000)
                    .get();

            Main.log("Loaded: " + url);

            Element historySection = doc.selectFirst("h2:contains(Name History) + div");
            if (historySection == null) {
                Main.log(ign + ": No name history section found.");
                return;
            }

            Elements nameRows = historySection.select(".row");
            if (nameRows.isEmpty()) {
                Main.log(ign + ": No name history entries found.");
                return;
            }

            Main.log(ign + " Name history:");
            for (Element row : nameRows) {
                String name = "Unknown";
                String date = "";

                Element nameSpan = row.selectFirst("div.col-12.col-md-6 span");
                if (nameSpan != null) {
                    name = nameSpan.text().trim();
                }

                Element dateDiv = row.selectFirst("div.text-right.text-muted");
                if (dateDiv != null) {
                    date = dateDiv.text().trim();
                }

                Main.log("- " + name + (date.isEmpty() ? "" : " (changed on " + date + ")"));
            }
        } catch (IOException e) {
            Main.log(ign + " Failed to load page: " + e.getMessage());
        }
    }
}
