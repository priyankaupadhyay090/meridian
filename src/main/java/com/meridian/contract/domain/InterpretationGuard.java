package com.meridian.contract.domain;

import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.WeightBand;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterpretationGuard {

    private static final Pattern PREFIX = Pattern.compile("\\d{1,5}");
    private static final Pattern WEIGHT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:kg|kilogramm|kilogram)");
    private static final Map<ServiceLevel, Pattern> LEVEL_WORDS = Map.of(
            ServiceLevel.EXPRESS, Pattern.compile("express"),
            ServiceLevel.STANDARD, Pattern.compile("standard"),
            ServiceLevel.ECONOMY, Pattern.compile("economy|eco\\b|spar"));

    private static final Map<CustomerTier, Pattern> TIER_WORDS = Map.of(
            CustomerTier.PREMIUM, Pattern.compile("premium"),
            CustomerTier.BUSINESS, Pattern.compile("business|geschäft"),
            CustomerTier.BASIC, Pattern.compile("basic|basis"));

    private final ClauseCatalog catalog;

    public InterpretationGuard(ClauseCatalog catalog) {
        this.catalog = catalog;
    }

    public boolean worthAsking(Clause clause) {
        String text = normalise(clause.text());
        if (!text.matches(".*\\d.*")) {
            return false;
        }
        boolean namesCourier = catalog.carrierServices().stream()
                .map(alias -> alias.phrase().split(" ")[0])
                .anyMatch(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find());
        return namesCourier && !hasMarker(text, catalog.instructionMarkers()) && !hasMarker(text, catalog.conditionMarkers());
    }

    private static boolean hasMarker(String text, List<String> markers) {
        return markers.stream().anyMatch(marker -> Pattern.compile("(?<![\\p{L}])" + Pattern.quote(marker)
                + "(?![\\p{L}])").matcher(text).find());
    }

    public AssistedReading check(Clause clause, ClauseInterpretation interpretation) {
        String text = normalise(clause.text());
        if (!interpretation.routingRule()) {
            return AssistedReading.outcome(clause, AssistOutcome.NOT_A_RULE, reasonOr(interpretation,
                    "the model reads no routing rule in this clause"));
        }
        if (interpretation.conditional()) {
            return AssistedReading.outcome(clause, AssistOutcome.KEPT_FOR_PERSON,
                    "the model reads a condition that a routing rule cannot express");
        }
        Optional<String> problem = problem(text, interpretation);
        if (problem.isPresent()) {
            return AssistedReading.outcome(clause, AssistOutcome.REJECTED, problem.get());
        }
        WeightBand band = weightBand(interpretation);
        RuleProposal proposal = new RuleProposal(interpretation.destinationPrefix(), interpretation.serviceLevel(),
                band, interpretation.customerTier(), RoutingClauseReader.DEFAULT_PRIORITY,
                interpretation.carrierService(), clause.text());
        return AssistedReading.accepted(clause, proposal, reasonOr(interpretation, "read by the model"));
    }

    private Optional<String> problem(String text, ClauseInterpretation interpretation) {
        String quote = interpretation.quote() == null ? "" : normalise(interpretation.quote());
        if (quote.isBlank() || !text.contains(quote)) {
            return Optional.of("the quote the model gave is not in the clause");
        }
        String prefix = interpretation.destinationPrefix();
        if (prefix == null || !PREFIX.matcher(prefix).matches()
                || !Pattern.compile("(?<!\\d)" + prefix + "(?!\\d)").matcher(text).find()) {
            return Optional.of("the postcode prefix the model gave is not in the clause");
        }
        CarrierService service = interpretation.carrierService();
        if (service == null || catalog.carrierServices().stream()
                .noneMatch(alias -> alias.service() == service && text.contains(alias.phrase()))) {
            return Optional.of("the courier service the model gave is not named in the clause");
        }
        if (interpretation.serviceLevel() != null
                && !LEVEL_WORDS.get(interpretation.serviceLevel()).matcher(withoutServiceNames(text)).find()) {
            return Optional.of("the service level the model gave is not in the clause");
        }
        if (interpretation.customerTier() != null
                && !TIER_WORDS.get(interpretation.customerTier()).matcher(text).find()) {
            return Optional.of("the customer tier the model gave is not in the clause");
        }
        String withoutNames = withoutServiceNames(text);
        if (interpretation.serviceLevel() == null
                && LEVEL_WORDS.values().stream().anyMatch(word -> word.matcher(withoutNames).find())) {
            return Optional.of("the clause names a service level the model left out");
        }
        if (interpretation.customerTier() == null && TIER_WORDS.values().stream()
                .anyMatch(word -> Pattern.compile(word.pattern() + "\\w*\\s*(?:customers|kunden)").matcher(text).find())) {
            return Optional.of("the clause names a customer tier the model left out");
        }
        Set<Integer> weights = weightsIn(text);
        if (!weights.isEmpty() && interpretation.minWeightKg() == null && interpretation.maxWeightKg() == null) {
            return Optional.of("the clause names a weight the model left out");
        }
        for (Double kilograms : new Double[]{interpretation.minWeightKg(), interpretation.maxWeightKg()}) {
            if (kilograms != null && kilograms > 0 && !weights.contains(grams(kilograms))) {
                return Optional.of("a weight the model gave is not in the clause");
            }
        }
        WeightBand band = weightBand(interpretation);
        if (band != null && !band.isValid()) {
            return Optional.of("the weight band the model gave is not valid");
        }
        if (hasMarker(text, catalog.instructionMarkers())) {
            return Optional.of("the clause reads as an instruction and stays with a person");
        }
        return Optional.empty();
    }

    private String withoutServiceNames(String text) {
        String result = text;
        for (CarrierAlias alias : catalog.carrierServices()) {
            result = result.replace(alias.phrase(), " ");
        }
        return result;
    }

    private static WeightBand weightBand(ClauseInterpretation interpretation) {
        Double min = interpretation.minWeightKg();
        Double max = interpretation.maxWeightKg();
        if (min == null && max == null) {
            return null;
        }
        return new WeightBand(min == null ? 0 : grams(min), max == null ? Integer.MAX_VALUE : grams(max));
    }

    private static Set<Integer> weightsIn(String text) {
        Set<Integer> weights = new HashSet<>();
        Matcher matcher = WEIGHT.matcher(text);
        while (matcher.find()) {
            weights.add(Quantities.grams(matcher.group(1)));
        }
        return weights;
    }

    private static int grams(double kilograms) {
        return (int) Math.round(kilograms * 1000);
    }

    private static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static String reasonOr(ClauseInterpretation interpretation, String fallback) {
        return interpretation.reason() == null || interpretation.reason().isBlank() ? fallback : interpretation.reason();
    }
}
