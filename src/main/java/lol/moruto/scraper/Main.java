package lol.moruto.scraper;

import com.formdev.flatlaf.FlatDarkLaf;
import lol.moruto.scraper.filter.FilterContext;
import lol.moruto.scraper.filter.FilterManager;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Main {
    private static boolean headless = false;

    public static void main(String[] args) {
        Set<String> desiredCapStrings = new HashSet<>();
        Set<String> blockedCapStrings = new HashSet<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--headless".equalsIgnoreCase(arg)) {
                headless = true;
            } else if ("--desiredCapes".equalsIgnoreCase(arg) && i + 1 < args.length) {
                desiredCapStrings.addAll(Arrays.asList(args[++i].split(",")));
            } else if ("--blockedCapes".equalsIgnoreCase(arg) && i + 1 < args.length) {
                blockedCapStrings.addAll(Arrays.asList(args[++i].split(",")));
            }
        }

        if (headless) {
            System.out.println("Running in headless mode...");

            EnumSet<CapeType> desired = EnumSet.noneOf(CapeType.class);
            EnumSet<CapeType> blocked = EnumSet.noneOf(CapeType.class);

            for (String s : desiredCapStrings) {
                try {
                    desired.add(CapeType.valueOf(s.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown desired cape type: " + s);
                }
            }
            if (desired.isEmpty()) desired = EnumSet.allOf(CapeType.class);

            boolean excludeDesired = blockedCapStrings.stream()
                    .anyMatch(s -> s.trim().equalsIgnoreCase("EXCLUDEDESIRED"));

            if (excludeDesired) {
                blocked = EnumSet.allOf(CapeType.class);
                blocked.removeAll(desired);
            } else {
                for (String s : blockedCapStrings) {
                    try {
                        blocked.add(CapeType.valueOf(s.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Unknown blocked cape type: " + s);
                    }
                }
            }

            FilterContext ctx = new FilterContext();
            ctx.put("desiredCapes", desired);
            ctx.put("blockedCapes", blocked);
            ctx.put("desiredRank", "Don't Filter");

            System.out.println("Starting cape filtering...");
            var results = new FilterManager().startFiltering(ctx);
            if (results.isEmpty()) {
                System.out.println("No players found after filtering.");
            } else {
                results.forEach(System.out::println);
            }
            System.out.println("Done.");
        } else {
            try {
                javax.swing.UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception ignored) {}
            new Gui();
        }
    }

    public static void log(String message) {
        if (headless) {
            System.out.println(message);
        } else {
            Gui.log(message);
        }
    }
}
