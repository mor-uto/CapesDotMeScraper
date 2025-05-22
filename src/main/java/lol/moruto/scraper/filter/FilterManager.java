package lol.moruto.scraper.filter;

import lol.moruto.scraper.filter.impl.FilterByCapes;
import lol.moruto.scraper.filter.impl.FilterByHypixelRank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterManager {
    private final List<Filter> filters = Arrays.asList(
            new FilterByCapes(),
            new FilterByHypixelRank()
    );

    public List<String> startFiltering(FilterContext context) {
        List<String> currentList = new ArrayList<>();

        for (Filter filter : filters) {
            currentList = filter.filter(currentList, context);
            if (currentList.isEmpty()) break;
        }

        return currentList;
    }
}
