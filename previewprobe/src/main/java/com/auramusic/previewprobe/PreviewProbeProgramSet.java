package com.auramusic.previewprobe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Parses deterministic publisher arguments without Android dependencies. */
final class PreviewProbeProgramSet {
    private static final List<String> DEFAULT = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

    private PreviewProbeProgramSet() {}

    static List<String> parse(String requested) {
        if (requested == null) return new ArrayList<>(DEFAULT);
        if (requested.trim().isEmpty()) return new ArrayList<>();

        String[] values = requested.split(",", -1);
        List<String> result = new ArrayList<>(values.length);
        Set<String> seen = new HashSet<>();
        for (String value : values) {
            String id = value.trim().toUpperCase(Locale.ROOT);
            if (!id.matches("[A-H]") || !seen.add(id)) {
                throw new IllegalArgumentException(
                        "previewprobe_programs must be unique comma-separated IDs A-H: " + requested);
            }
            result.add(id);
        }
        return result;
    }
}
