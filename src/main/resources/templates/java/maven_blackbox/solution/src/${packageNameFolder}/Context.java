package ${packageName};

import java.util.*;

public class Context {
    private SortStrategy sortAlgorithm  = new MergeSort();

    private List<Date> dates = new ArrayList<>();

    public List<Date> getDates() {
        return dates;
    }

    public void setDates(List<Date> dates) {
        this.dates = dates;
    }

    public void addDates(List<Date> dates) {
        this.dates.addAll(dates);
    }

    public void clearDates() {
        this.dates = new ArrayList<>();
    }

    public void setSortAlgorithm(SortStrategy sa) {
        sortAlgorithm = sa;
    }

    public SortStrategy getSortAlgorithm() {
        return sortAlgorithm;
    }

    /**
     * Runs the configured sort algorithm.
     */
    public void sort() {
        if (sortAlgorithm != null) {
            sortAlgorithm.performSort(this.dates);
        }
    }
}