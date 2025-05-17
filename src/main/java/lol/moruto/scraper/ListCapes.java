package lol.moruto.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ListCapes {
    private final JTextArea consoleArea;

    public ListCapes(Set<CapeType> desiredCapes, Set<CapeType> noCapes, JTextArea consoleArea) {
        this.consoleArea = consoleArea;
        String baseUrl = "https://capes.me/capes";

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("capes", desiredCapes.stream().map(CapeType::getCode).collect(Collectors.joining(",")));
        if (!noCapes.isEmpty()) {
            queryParams.put("filter_capes", noCapes.stream().map(CapeType::getCode).collect(Collectors.joining(",")));
        }

        String currentUrl = baseUrl + "?" + toQueryString(queryParams);

        logToConsole("Starting URL: " + currentUrl);
        Set<String> loggedIGNs = new HashSet<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("results.txt"))) {
            while (currentUrl != null) {
                int newPlayersThisPage = 0;

                try {
                    Document doc = Jsoup.connect(currentUrl).userAgent("Mozilla/5.0").timeout(10000).get();
                    Elements users = doc.select("div.full-user");

                    if (users.isEmpty()) logToConsole("No users found on this page.");

                    for (Element user : users) {
                        String ign = user.select("h3.name > div").text().trim();
                        if (loggedIGNs.contains(ign)) continue;

                        Elements capes = user.select("div.cape-div");

                        List<String> allCapeNames = new ArrayList<>();
                        for (Element cape : capes) {
                            String capeName = cape.select("p").text().trim();
                            allCapeNames.add(capeName);
                        }

                        boolean hasDesiredCape = allCapeNames.stream().anyMatch(name -> desiredCapes.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name)));
                        boolean hasExcludedCape = allCapeNames.stream().anyMatch(name -> noCapes.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name)));

                        if (hasDesiredCape && !hasExcludedCape) {
                            writer.write(ign + " - Capes: " + String.join(", ", allCapeNames));
                            writer.newLine();
                            loggedIGNs.add(ign);
                            newPlayersThisPage++;
                        }
                    }

                    if (newPlayersThisPage == 0) {
                        logToConsole("No new players on this page, ending the scraping.");
                        break;
                    }

                    Element nextPage = doc.selectFirst("a.pageNext[href]");
                    if (nextPage != null) {
                        String href = nextPage.attr("href").trim();
                        String pageParam = extractPageParam(href);
                        if (pageParam != null) {
                            queryParams.put("page", pageParam);
                            currentUrl = baseUrl + "?" + toQueryString(queryParams);
                        } else currentUrl = null;

                        logToConsole("Moving to next page: " + currentUrl);
                    } else currentUrl = null;

                    Thread.sleep(500);
                } catch (Exception e) {
                    logToConsole("Error during scraping: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }

            logToConsole("Scraping complete. Output saved to results.txt");
        } catch (IOException e) {
            logToConsole("Could not write to file: " + e.getMessage());
        }
    }

    private String toQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private String extractPageParam(String href) {
        if (href.contains("page=")) {
            String[] parts = href.split("[?&]");
            for (String part : parts) {
                if (part.startsWith("page=")) {
                    return part.substring("page=".length());
                }
            }
        }
        return null;
    }

    private void logToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }
}
