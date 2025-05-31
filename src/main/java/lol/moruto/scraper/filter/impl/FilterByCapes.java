package lol.moruto.scraper.filter.impl;

import lol.moruto.scraper.CapeType;
import lol.moruto.scraper.JSONParser;
import lol.moruto.scraper.Main;
import lol.moruto.scraper.filter.Filter;
import lol.moruto.scraper.filter.FilterContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FilterByCapes implements Filter {
    @Override
    public List<String> filter(List<String> ignList, FilterContext ctx) {
        Set<CapeType> desired = ctx.get("desiredCapes", Set.class);
        Set<CapeType> blocked = ctx.get("blockedCapes", Set.class);
        List<String> specificPlayers = ctx.get("specificPlayers", List.class);

        if (specificPlayers != null && !specificPlayers.isEmpty()) {
            return filterFromSpecificList(specificPlayers, desired, blocked);
        }

        String baseUrl = "https://capes.me/capes";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("capes", desired.stream().map(CapeType::getCode).collect(Collectors.joining(",")));
        if (!blocked.isEmpty()) params.put("filter_capes", blocked.stream().map(CapeType::getCode).collect(Collectors.joining(",")));

        String url = baseUrl + "?" + toQueryString(params);
        Set<String> desiredNames = toLowerSet(desired);
        Set<String> blockedNames = toLowerSet(blocked);

        Main.log("Starting scrape at: " + url);

        Set<String> foundIGNs = new HashSet<>();

        try {
            while (true) {
                Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
                Elements users = doc.select("div.full-user");
                if (users.isEmpty()) break;

                int newCount = 0;
                for (Element user : users) {
                    String ign = user.selectFirst("h3.name > div").text().trim();
                    if (foundIGNs.contains(ign)) continue;

                    Set<String> userCapes = new HashSet<>();
                    for (Element cape : user.select("div.cape-div > p")) {
                        userCapes.add(cape.text().toLowerCase(Locale.ROOT).trim());
                    }

                    boolean hasDesired = userCapes.stream().anyMatch(desiredNames::contains);
                    boolean hasBlocked = userCapes.stream().anyMatch(blockedNames::contains);

                    if (hasDesired && !hasBlocked) {
                        foundIGNs.add(ign);
                        newCount++;
                    }
                }

                if (newCount == 0) {
                    Main.log("No new players found, stopping.");
                    break;
                }

                Element nextPage = doc.selectFirst("a.pageNext[href]");
                if (nextPage == null) break;

                String nextHref = nextPage.attr("href");
                String nextPageParam = extractPage(nextHref);
                if (nextPageParam == null) break;

                params.put("page", nextPageParam);
                url = baseUrl + "?" + toQueryString(params);
                Main.log("Moving to next page: " + url);
            }
            Main.log("Cape filtering done, found " + foundIGNs.size() + " players.");
        } catch (IOException e) {
            Main.log("Error: " + e.getMessage());
        }

        return new ArrayList<>(foundIGNs);
    }

    public List<String> filterFromSpecificList(List<String> players, Set<CapeType> filterCapes, Set<CapeType> blockedCapes) {
        Set<String> desiredCapes = filterCapes.stream().map(c -> c.getCode().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> blockedCapesSet = blockedCapes.stream().map(c -> c.getCode().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<String> matchingPlayers = new ArrayList<>();

        for (String player : players) {
            try {
                String apiUrl = "https://capes.me/api/user/" + URLEncoder.encode(player, StandardCharsets.UTF_8);
                String json = Jsoup.connect(apiUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .execute()
                        .body();

                Set<String> playerCapes = new HashSet<>();
                String capesArray = json.substring(json.indexOf("\"capes\": [") + 9);
                capesArray = capesArray.substring(0, capesArray.indexOf("]") + 1);

                String[] capeObjects = capesArray.split("\\{");
                for (String capeObj : capeObjects) {
                    if (!capeObj.contains("type")) continue;

                    String type = JSONParser.extractJsonStringValue(capeObj, "type");
                    String removedStr = String.valueOf(JSONParser.extractJsonBooleanValue(capeObj, "removed"));

                    boolean removed = removedStr.equals("true");
                    if (!removed && type != null) {
                        playerCapes.add(type.toLowerCase(Locale.ROOT));
                    }
                }

                boolean hasDesired = playerCapes.stream().anyMatch(desiredCapes::contains);
                boolean hasBlocked = playerCapes.stream().anyMatch(blockedCapesSet::contains);

                if (hasDesired && !hasBlocked) {
                    matchingPlayers.add(player);
                }
            } catch (Exception e) {
                Main.log("Error fetching or parsing player " + player + ": " + e.getMessage());
            }
        }

        return matchingPlayers;
    }

    private Set<String> toLowerSet(Set<CapeType> capes) {
        if (capes == null) return Collections.emptySet();
        Set<String> lowered = new HashSet<>();
        for (CapeType c : capes) lowered.add(c.getName().toLowerCase(Locale.ROOT));
        return lowered;
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String extractPage(String href) {
        if (href == null) return null;
        int idx = href.indexOf("page=");
        if (idx == -1) return null;
        int start = idx + 5;
        int end = href.indexOf('&', start);
        return (end == -1) ? href.substring(start) : href.substring(start, end);
    }
}
