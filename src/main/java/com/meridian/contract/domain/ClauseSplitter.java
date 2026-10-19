package com.meridian.contract.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClauseSplitter {

    private static final Pattern NUMBERED = Pattern.compile(
            "^\\s*(§\\s*\\d+(?:\\s*Abs\\.\\s*\\d+)?:?|\\(\\d+\\)|\\d+(?:\\.\\d+)*\\.?)\\s+(.*)$");

    public List<Clause> split(String text) {
        List<Clause> clauses = new ArrayList<>();
        String reference = null;
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            Matcher numbered = NUMBERED.matcher(line);
            if (numbered.matches()) {
                add(clauses, reference, current);
                reference = numbered.group(1).replaceAll(":$", "").trim();
                current = new StringBuilder(numbered.group(2).trim());
            } else if (current.isEmpty()) {
                current.append(line.trim());
            } else {
                current.append(' ').append(line.trim());
            }
        }
        add(clauses, reference, current);
        return clauses;
    }

    private static void add(List<Clause> clauses, String reference, StringBuilder text) {
        if (!text.isEmpty()) {
            clauses.add(new Clause(reference == null ? "clause " + (clauses.size() + 1) : reference, text.toString()));
        }
    }
}
