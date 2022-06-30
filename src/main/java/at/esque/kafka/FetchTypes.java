package at.esque.kafka;

public enum FetchTypes {
    OLDEST,
    NEWEST,
    SPECIFIC_OFFSET,
    STARTING_FROM_INSTANT,
    CONTINUOUS
}
