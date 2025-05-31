package lol.moruto.scraper;

import java.util.*;

public class JSONParser {

    public static Set<String> parseCapeTypes(String json) {
        Set<String> result = new HashSet<>();

        try {
            int capesStart = json.indexOf("\"capes\": [");
            if (capesStart == -1) return result;

            int arrayStart = json.indexOf('[', capesStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            if (arrayStart == -1 || arrayEnd == -1) return result;

            String capesArray = json.substring(arrayStart + 1, arrayEnd);

            String[] capeObjects = capesArray.split("},\\s*\\{");

            for (String raw : capeObjects) {
                String capeObj = raw.trim();
                if (!capeObj.startsWith("{")) capeObj = "{" + capeObj;
                if (!capeObj.endsWith("}")) capeObj = capeObj + "}";

                String type = extractJsonStringValue(capeObj, "type");
                boolean removed = extractJsonBooleanValue(capeObj, "removed");

                if (type != null && !removed) {
                    result.add(type.toLowerCase(Locale.ROOT));
                }
            }

        } catch (Exception e) {
            Main.log("JSONParser error: " + e.getMessage());
        }

        return result;
    }

    public static String extractJsonStringValue(String jsonFragment, String key) {
        String search = "\"" + key + "\"";
        int idx = jsonFragment.indexOf(search);
        if (idx == -1) return null;

        int colon = jsonFragment.indexOf(":", idx);
        if (colon == -1) return null;

        int start = jsonFragment.indexOf("\"", colon + 1);
        int end = jsonFragment.indexOf("\"", start + 1);
        if (start == -1 || end == -1) return null;

        return jsonFragment.substring(start + 1, end);
    }

    public static boolean extractJsonBooleanValue(String jsonFragment, String key) {
        String search = "\"" + key + "\"";
        int idx = jsonFragment.indexOf(search);
        if (idx == -1) return false;

        int colon = jsonFragment.indexOf(":", idx);
        if (colon == -1) return false;

        String value = jsonFragment.substring(colon + 1).trim();
        return value.startsWith("true");
    }

    public static int findMatchingBracket(String text, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            if (text.charAt(i) == '[') depth++;
            else if (text.charAt(i) == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
