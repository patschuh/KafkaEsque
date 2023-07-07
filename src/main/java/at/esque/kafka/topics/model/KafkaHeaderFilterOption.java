package at.esque.kafka.topics.model;

import java.util.Objects;

public class KafkaHeaderFilterOption {
    private String header;
    private String filterString;
    private boolean exactMatch;

    public KafkaHeaderFilterOption(String header, String filterString, boolean exactMatch) {
        this.header = header;
        this.filterString = filterString;
        this.exactMatch = exactMatch;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFilterString() {
        return filterString;
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (KafkaHeaderFilterOption) obj;
        return Objects.equals(this.header, that.header) &&
                Objects.equals(this.filterString, that.filterString) &&
                this.exactMatch == that.exactMatch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, filterString, exactMatch);
    }

    @Override
    public String toString() {
        return "KafkaHeaderFilterOption[" +
                "header=" + header + ", " +
                "filterString=" + filterString + ", " +
                "exactMatch=" + exactMatch + ']';
    }

}
