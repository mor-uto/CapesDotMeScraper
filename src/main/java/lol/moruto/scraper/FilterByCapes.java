package lol.moruto.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class FilterByCapes {
    private final Set<CapeType> desiredCapes;
    private final Set<CapeType> noCapes;
    private final Set<String> loggedIGNs = new HashSet<>();

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; capes.me scraper)";
    private static final int TIMEOUT_MS = 10000;

    public FilterByCapes(Set<CapeType> desiredCapes, Set<CapeType> noCapes) {
        this.desiredCapes = desiredCapes;
        this.noCapes = noCapes;
    }

    public List<String> startScraping() {
        final String baseUrl = "https://capes.me/capes";

        String desiredCodes = desiredCapes.stream().map(CapeType::getCode).collect(Collectors.joining(","));
        String noCodes = noCapes.isEmpty() ? "" : noCapes.stream().map(CapeType::getCode).collect(Collectors.joining(","));

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("capes", desiredCodes);
        if (!noCodes.isEmpty()) {
            queryParams.put("filter_capes", noCodes);
        }

        String currentUrl = baseUrl + "?" + toQueryString(queryParams);

        Set<String> desiredNamesLower = desiredCapes.stream().map(c -> c.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> noNamesLower = noCapes.stream().map(c -> c.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        Main.log("Starting URL: " + currentUrl);

        try {
            while (true) {
                int newPlayersThisPage = 0;

                Document doc = Jsoup.connect(currentUrl).userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();

                Elements users = doc.select("div.full-user");
                if (users.isEmpty()) {
                    Main.log("No users found on this page.");
                    break;
                }

                for (Element user : users) {
                    String ign = user.selectFirst("h3.name > div").text().trim();
                    if (loggedIGNs.contains(ign)) continue;

                    Elements capes = user.select("div.cape-div > p");
                    Set<String> userCapeNames = new HashSet<>(capes.size());
                    for (Element cape : capes) {
                        userCapeNames.add(cape.text().trim().toLowerCase(Locale.ROOT));
                    }

                    boolean hasDesired = userCapeNames.stream().anyMatch(desiredNamesLower::contains);
                    boolean hasExcluded = userCapeNames.stream().anyMatch(noNamesLower::contains);

                    if (hasDesired && !hasExcluded) {
                        loggedIGNs.add(ign);
                        newPlayersThisPage++;
                    }
                }

                if (newPlayersThisPage == 0) {
                    Main.log("No new players on this page, ending scraping.");
                    break;
                }

                Element nextPage = doc.selectFirst("a.pageNext[href]");
                if (nextPage == null) {
                    Main.log("No next page, scraping finished.");
                    break;
                }

                String href = nextPage.attr("href");
                String pageParam = extractPageParam(href);
                if (pageParam == null) {
                    Main.log("Could not find page parameter in next page link, finishing.");
                    break;
                }

                queryParams.put("page", pageParam);
                currentUrl = baseUrl + "?" + toQueryString(queryParams);
                Main.log("Moving to next page: " + currentUrl);
            }

            Main.log("Cape filtering complete. Found " + loggedIGNs.size() + " players.");

        } catch (IOException e) {
            Main.log("I/O Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Main.log("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        return new ArrayList<>(loggedIGNs);
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            try {
                if (sb.length() > 0) sb.append("&");
                sb.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
                if (sb.length() > 0) sb.append("&");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return sb.toString();
    }


    private String extractPageParam(String href) {
        if (href == null) return null;
        int idx = href.indexOf("page=");
        if (idx == -1) return null;
        int start = idx + 5;
        int end = href.indexOf('&', start);
        return (end == -1) ? href.substring(start) : href.substring(start, end);
    }
}
