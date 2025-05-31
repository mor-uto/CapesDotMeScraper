package lol.moruto.scraper.filter.impl;

import lol.moruto.scraper.Main;
import lol.moruto.scraper.filter.Filter;
import lol.moruto.scraper.filter.FilterContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FilterByNameHistory implements Filter {

    @Override
    public List<String> filter(List<String> ignList, FilterContext ctx) {
        Set<Integer> allowedCounts = ctx.get("ncs", Set.class);
        if (allowedCounts == null || allowedCounts.isEmpty()) return ignList;

        String site = ctx.get("ncWebsite", String.class);
        if (site == null) site = "namemc.com";

        List<String> matchedPlayers = switch (site) {
            case "namemc.com" -> parseNamemc(ignList, allowedCounts);
            default -> {
                Main.log("Unknown name history site: " + site);
                yield new ArrayList<>();
            }
        };

        Main.log("Finished processing all IGNs.");
        return matchedPlayers;
    }

    private List<String> parseNamemc(List<String> ignList, Set<Integer> allowedCounts) {
        List<String> matchedPlayers = new ArrayList<>();

        for (String ign : ignList) {
            ign = ign.trim();
            if (ign.isEmpty()) continue;

            Main.log("Searching NameMC for IGN: " + ign);
            Integer nc = fetchNameChangeCount(ign);
            if (nc == null) continue;

            Main.log(ign + ": " + nc + " name changes");

            if (allowedCounts.contains(nc)) {
                matchedPlayers.add(ign);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Main.log("Interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }

        return matchedPlayers;
    }

    private Integer fetchNameChangeCount(String ign) {
        try {
            String url = "https://namemc.com/profile/" + ign;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();

            Element historySection = doc.selectFirst("h2:contains(Name History) + div");
            if (historySection == null) {
                Main.log(ign + ": No name history section found.");
                return null;
            }

            Elements nameRows = historySection.select(".row");
            return !nameRows.isEmpty() ? nameRows.size() - 1 : 0;
        } catch (IOException e) {
            Main.log(ign + ": Failed to load page: " + e.getMessage());
            return null;
        }
    }
}
