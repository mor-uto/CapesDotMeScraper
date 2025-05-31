package lol.moruto.scraper;

import com.formdev.flatlaf.FlatDarkLaf;
import lol.moruto.scraper.filter.Filter;
import lol.moruto.scraper.filter.FilterContext;
import lol.moruto.scraper.filter.impl.FilterByCapes;
import lol.moruto.scraper.filter.impl.FilterByHypixelRank;
import lol.moruto.scraper.filter.impl.FilterByNameHistory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final List<Filter> filters = Arrays.asList(
            new FilterByCapes(),
            new FilterByHypixelRank(),
            new FilterByNameHistory()
    );

    public static boolean headless = false;

    public static void main(String[] args) throws Exception {
        headless = Arrays.asList(args).contains("--headless");
        FilterContext ctx = new FilterContext();

        boolean outputJson = false;

        if (headless) {
            EnumSet<CapeType> desired = EnumSet.allOf(CapeType.class);
            EnumSet<CapeType> blocked = EnumSet.noneOf(CapeType.class);
            String desiredRank = "Don't Filter";

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--desiredCapes" -> desired = parseCapes(args[++i]);
                    case "--blockedCapes" -> blocked = parseCapes(args[++i]);
                    case "--hypixelRank" -> desiredRank = args[++i];
                    case "--outputJson" -> outputJson = true;
                    case "--ncs" -> ctx.put("ncs", args[++i]);
                }
            }

            ctx.put("desiredCapes", desired);
            ctx.put("blockedCapes", blocked);
            ctx.put("desiredRank", desiredRank);

            List<String> results = startFiltering(ctx);
            results.forEach(System.out::println);
            writeResults(results, outputJson);

            System.out.println("Done.");
        } else {
            javax.swing.UIManager.setLookAndFeel(new FlatDarkLaf());
            new Gui(ctx);
        }
    }

    public static List<String> startFiltering(FilterContext context) {
        List<String> currentList = null;

        for (Filter filter : filters) {
            currentList = filter.filter(Objects.requireNonNullElse(currentList, Collections.emptyList()), context);
            if (currentList.isEmpty()) break;
        }

        return currentList;
    }

    private static EnumSet<CapeType> parseCapes(String csv) {
        EnumSet<CapeType> set = EnumSet.noneOf(CapeType.class);
        for (String s : csv.split(",")) {
            CapeType type = CapeType.fromCode(s.trim());
            if (type != null) set.add(type);
        }
        return set;
    }

    public static void log(String message) {
        if (headless) {
            System.out.println(message);
        } else {
            Gui.log(message);
        }
    }

    public static void writeResults(List<String> results, boolean isJson) {
        if (isJson) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter("results.json"))) {
                w.write("[\n");
                for (int i = 0; i < results.size(); i++) {
                    String ign = results.get(i);
                    w.write("  " + "\"" + ign.chars().mapToObj(c -> switch(c) { case '"'->"\\\""; case '\\'->"\\\\"; case '\b'->"\\b"; case '\f'->"\\f"; case '\n'->"\\n"; case '\r'->"\\r"; case '\t'->"\\t"; default -> (c < 0x20 || c > 0x7E) ? String.format("\\u%04x", c) : String.valueOf((char)c); }).reduce("", String::concat) + "\"");
                    if (i < results.size() - 1) {
                        w.write(",");
                    }
                    w.write("\n");
                }
                w.write("]\n");
                log("Results saved to results.json");
            } catch (IOException ex) {
                log("Failed to write JSON results: " + ex.getMessage());
            }
        } else {
            try (BufferedWriter w = new BufferedWriter(new FileWriter("results.txt"))) {
                for (String ign : results) {
                    w.write(ign + "\n");
                }
                log("Results saved to results.txt");
            } catch (IOException ex) {
                log("Failed to write results: " + ex.getMessage());
            }
        }
    }
}
