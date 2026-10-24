package at.esque.kafka.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small, dependency free version number used to compare the version this application was built from with the latest
 * published release tag.
 *
 * <p>Versions are parsed from strings such as {@code v2.9.7}, {@code 2.10.0} or {@code 2.9.7-rc-1}. Numeric segments are
 * compared first. A version with the same numeric segments but a qualifier (e.g. {@code -rc-1}) sorts before the
 * corresponding final release.</p>
 */
public final class Version implements Comparable<Version> {

    private static final int UNKNOWN_QUALIFIER_RANK = 6;
    private static final int FINAL_QUALIFIER_RANK = 7;
    private static final Map<String, Integer> QUALIFIER_RANKS = createQualifierRanks();

    private final String original;
    private final List<Integer> numbers;
    private final List<String> qualifiers;

    private Version(String original, List<Integer> numbers, List<String> qualifiers) {
        this.original = original;
        this.numbers = numbers;
        this.qualifiers = qualifiers;
    }

    /**
     * Parses a version string, optionally prefixed with a {@code v}.
     *
     * @param versionString the version to parse, may be {@code null}
     * @return the parsed version or {@code null} if {@code versionString} is {@code null}
     * @throws IllegalArgumentException if the string does not contain any numeric segment
     */
    public static Version parse(String versionString) {
        if (versionString == null) {
            return null;
        }

        String value = versionString.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }

        List<Integer> numbers = new ArrayList<>();
        List<String> qualifiers = new ArrayList<>();
        boolean numeric = true;
        for (String token : value.split("[^A-Za-z0-9]+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (numeric) {
                Integer number = parseNumber(token);
                if (number != null) {
                    numbers.add(number);
                    continue;
                }
                numeric = false;
            }
            qualifiers.add(token.toLowerCase(Locale.ROOT));
        }

        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Not a valid version: " + versionString);
        }
        if (qualifiers.size() == 1 && qualifierRank(qualifiers) == FINAL_QUALIFIER_RANK) {
            qualifiers.clear();
        }
        return new Version(versionString, numbers, qualifiers);
    }

    @Override
    public int compareTo(Version other) {
        int length = Math.max(numbers.size(), other.numbers.size());
        for (int i = 0; i < length; i++) {
            int left = i < numbers.size() ? numbers.get(i) : 0;
            int right = i < other.numbers.size() ? other.numbers.get(i) : 0;
            if (left != right) {
                return Integer.compare(left, right);
            }
        }
        return compareQualifiers(qualifiers, other.qualifiers);
    }

    private static int compareQualifiers(List<String> left, List<String> right) {
        int rankComparison = Integer.compare(qualifierRank(left), qualifierRank(right));
        if (rankComparison != 0) {
            return rankComparison;
        }

        int length = Math.max(left.size(), right.size());
        for (int i = 0; i < length; i++) {
            if (i >= left.size()) {
                return -1;
            }
            if (i >= right.size()) {
                return 1;
            }
            int comparison = compareQualifierTokens(left.get(i), right.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int compareQualifierTokens(String left, String right) {
        Integer leftNumber = parseNumber(left);
        Integer rightNumber = parseNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Integer.compare(leftNumber, rightNumber);
        }
        return left.compareTo(right);
    }

    private static int qualifierRank(List<String> qualifiers) {
        if (qualifiers.isEmpty()) {
            return FINAL_QUALIFIER_RANK;
        }
        return QUALIFIER_RANKS.getOrDefault(qualifiers.get(0), UNKNOWN_QUALIFIER_RANK);
    }

    private static Integer parseNumber(String token) {
        if (!token.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static Map<String, Integer> createQualifierRanks() {
        Map<String, Integer> ranks = new HashMap<>();
        ranks.put("dev", 0);
        ranks.put("snapshot", 1);
        ranks.put("alpha", 2);
        ranks.put("beta", 3);
        ranks.put("milestone", 4);
        ranks.put("m", 4);
        ranks.put("rc", 5);
        ranks.put("cr", 5);
        ranks.put("final", FINAL_QUALIFIER_RANK);
        ranks.put("ga", FINAL_QUALIFIER_RANK);
        ranks.put("release", FINAL_QUALIFIER_RANK);
        ranks.put("sp", 8);
        return ranks;
    }

    @Override
    public String toString() {
        return original;
    }
}
