package lol.moruto.scraper.filter;

import java.util.List;

public interface Filter {
    List<String> filter(List<String> igns, FilterContext context);
}
