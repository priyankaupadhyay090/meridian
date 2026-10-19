package com.meridian.contract.domain;

import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.WeightBand;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingClauseReader {

    public static final int DEFAULT_PRIORITY = 100;

    private static final String NUMBER = "(\\d+(?:[.,]\\d+)?)";
    private static final Pattern PREFIX = Pattern.compile(
            "postcodes (?:starting|beginning) with (\\d{1,5})|postleitzahlen,? die mit (\\d{1,5}) beginnen");
    private static final Pattern LEVEL = Pattern.compile("\\b(express|standard|economy)(?: parcels|sendungen)\\b");
    private static final Pattern TIER = Pattern.compile(
            "\\b(premium|business|basic) customers\\b|\\b(premium|geschäfts|basis)kunden\\b");
    private static final Pattern WEIGHT_RANGE = Pattern.compile(
            "from " + NUMBER + " kg up to (?:but not including )?" + NUMBER + " kg|von " + NUMBER + " kg bis unter " + NUMBER + " kg");
    private static final Pattern WEIGHT_OVER = Pattern.compile("(?:over|above|more than|über) " + NUMBER + " kg");
    private static final Pattern WEIGHT_UNDER = Pattern.compile("(?:under|below|up to|unter|bis) " + NUMBER + " kg");
    private static final Pattern PRIORITY = Pattern.compile("(?:priority|priorität) (\\d{1,4})");

    private final ClauseCatalog catalog;

    public RoutingClauseReader(ClauseCatalog catalog) {
        this.catalog = catalog;
    }

    public boolean isRoutingClause(Clause clause) {
        return PREFIX.matcher(clause.lowerText()).find();
    }

    public Optional<UnresolvedClause> instruction(Clause clause) {
        return firstMarker(clause.lowerText(), catalog.instructionMarkers())
                .map(marker -> new UnresolvedClause(clause,
                        "reads as an instruction (\"%s\"), not as a contract term".formatted(marker)));
    }

    public ClauseOutcome read(Clause clause) {
        String text = clause.lowerText();
        Optional<String> instruction = firstMarker(text, catalog.instructionMarkers());
        if (instruction.isPresent()) {
            return ClauseOutcome.unresolved(clause, "reads as an instruction (\"%s\"), not as a contract term"
                    .formatted(instruction.get()));
        }
        Optional<String> condition = firstMarker(text, catalog.conditionMarkers());
        if (condition.isPresent()) {
            return ClauseOutcome.unresolved(clause, "depends on a condition (\"%s\") that a routing rule cannot express"
                    .formatted(condition.get()));
        }
        List<CarrierService> services = catalog.carrierServices().stream()
                .filter(alias -> text.contains(alias.phrase()))
                .map(CarrierAlias::service)
                .distinct()
                .toList();
        if (services.isEmpty()) {
            return ClauseOutcome.unresolved(clause, "names no carrier service from the catalogue");
        }
        if (services.size() > 1) {
            return ClauseOutcome.unresolved(clause, "names more than one carrier service: " + services);
        }
        Matcher prefix = PREFIX.matcher(text);
        prefix.find();
        String destinationPrefix = prefix.group(1) != null ? prefix.group(1) : prefix.group(2);
        return ClauseOutcome.proposal(new RuleProposal(destinationPrefix, serviceLevel(text), weightBand(text),
                customerTier(text), priority(text), services.getFirst(), clause.text()));
    }

    private static Optional<String> firstMarker(String text, List<String> markers) {
        return markers.stream()
                .filter(marker -> Pattern.compile("(?<![\\p{L}])" + Pattern.quote(marker) + "(?![\\p{L}])")
                        .matcher(text).find())
                .findFirst();
    }

    private static ServiceLevel serviceLevel(String text) {
        Matcher level = LEVEL.matcher(text);
        return level.find() ? ServiceLevel.valueOf(level.group(1).toUpperCase()) : null;
    }

    private static CustomerTier customerTier(String text) {
        Matcher tier = TIER.matcher(text);
        if (!tier.find()) {
            return null;
        }
        String word = tier.group(1) != null ? tier.group(1) : tier.group(2);
        return switch (word) {
            case "premium" -> CustomerTier.PREMIUM;
            case "business", "geschäfts" -> CustomerTier.BUSINESS;
            default -> CustomerTier.BASIC;
        };
    }

    private static WeightBand weightBand(String text) {
        Matcher range = WEIGHT_RANGE.matcher(text);
        if (range.find()) {
            String from = range.group(1) != null ? range.group(1) : range.group(3);
            String to = range.group(2) != null ? range.group(2) : range.group(4);
            return new WeightBand(Quantities.grams(from), Quantities.grams(to));
        }
        Matcher over = WEIGHT_OVER.matcher(text);
        if (over.find()) {
            return new WeightBand(Quantities.grams(over.group(1)), Integer.MAX_VALUE);
        }
        Matcher under = WEIGHT_UNDER.matcher(text);
        if (under.find()) {
            return new WeightBand(0, Quantities.grams(under.group(1)));
        }
        return null;
    }

    private static int priority(String text) {
        Matcher priority = PRIORITY.matcher(text);
        return priority.find() ? Integer.parseInt(priority.group(1)) : DEFAULT_PRIORITY;
    }
}
