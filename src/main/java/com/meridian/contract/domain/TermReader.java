package com.meridian.contract.domain;

import com.meridian.compliance.domain.ContractTerms;
import com.meridian.compliance.domain.GermanFreightLawCheck;
import com.meridian.compliance.domain.ContractTerms.LiabilityCap;
import com.meridian.compliance.domain.ContractTerms.LimitationPeriod;
import com.meridian.compliance.domain.ContractTerms.NoticePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TermReader {

    private static final String NUMBER = "(\\d+(?:[.,]\\d+)?)";
    private static final Pattern LIABILITY = Pattern.compile(NUMBER
            + "\\s*(?:sdr|special drawing rights|units of account|sonderziehungsrechte|szr|rechnungseinheiten)"
            + "\\s*(?:per|je|pro)\\s*(?:kilogram|kilogramm|kg)");
    private static final Pattern PERIOD = Pattern.compile("(\\d+)\\s*(hours?|days?|stunden|tagen?|werktagen?)");
    private static final Pattern MONTHS = Pattern.compile("(\\d+)\\s*(months?|monaten?|years?|jahren?)");
    private static final Pattern WEIGHT = Pattern.compile(NUMBER + "\\s*kg");
    private static final Pattern HIDDEN = Pattern.compile("hidden|concealed|not visible|verdeckt|äußerlich nicht erkennbar");
    private static final Pattern DELAY = Pattern.compile("delay|late delivery|lieferfrist");
    private static final Pattern NOTICE = Pattern.compile("notif|report|reported|anzuzeigen|anzeige|melden|gemeldet");
    private static final Pattern LIMITATION = Pattern.compile("time-barred|expire|limitation|verjähr");
    private static final Pattern EXCLUDED = Pattern.compile("no liability|not liable|excluded|keine haftung|ausgeschlossen");
    private static final Pattern LABELLING = Pattern.compile("label|marked|gekennzeichnet|kennzeichn");
    private static final Pattern STATUTORY_CAP = Pattern.compile(
            "statutory (?:liability )?limits|gesetzlichen haftungs(?:grenzen|höchstbeträge|beschränkungen)");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.;!?])\\s+(?=[\\p{Lu}(])");
    private static final Pattern ABBREVIATION = Pattern.compile(
            "(?i)(?:\\b(?:bspw|bzw|ggf|inkl|ca|vgl|nr|abs|ziff|evtl|z|b|e\\.g|i\\.e|etc|usw)\\.)$");
    private static final Pattern TWO_PERSON = Pattern.compile("two persons|two-person|two people|technical aid|zwei personen|hilfsmittel");

    public boolean isTermClause(Clause clause) {
        String text = clause.lowerText();
        return LIABILITY.matcher(text).find()
                || STATUTORY_CAP.matcher(text).find()
                || (NOTICE.matcher(text).find() && PERIOD.matcher(text).find())
                || (LIMITATION.matcher(text).find() && MONTHS.matcher(text).find())
                || (DELAY.matcher(text).find() && EXCLUDED.matcher(text).find())
                || (LABELLING.matcher(text).find() && WEIGHT.matcher(text).find())
                || TWO_PERSON.matcher(text).find();
    }

    public ContractTerms read(List<Clause> clauses) {
        LiabilityCap liabilityCap = null;
        NoticePeriod hidden = null;
        NoticePeriod delay = null;
        LimitationPeriod limitation = null;
        String delayExclusion = null;
        String labelling = null;
        String twoPerson = null;
        int heaviest = 0;
        LiabilityCap statutoryCap = null;
        for (Clause clause : clauses) {
            String text = clause.lowerText();
            Matcher weight = WEIGHT.matcher(text);
            while (weight.find()) {
                heaviest = Math.max(heaviest, Quantities.grams(weight.group(1)));
            }
            Matcher liability = LIABILITY.matcher(text);
            if (liability.find()) {
                liabilityCap = new LiabilityCap(Quantities.number(liability.group(1)), clause.text());
            } else if (statutoryCap == null && STATUTORY_CAP.matcher(text).find()) {
                statutoryCap = new LiabilityCap(GermanFreightLawCheck.STATUTORY_SDR_PER_KG, clause.text());
            }
            for (String part : sentences(clause.text())) {
                String sentence = part.toLowerCase();
                Matcher period = PERIOD.matcher(sentence);
                if (NOTICE.matcher(sentence).find() && period.find()) {
                    NoticePeriod notice = new NoticePeriod(hours(period), clause.text());
                    if (HIDDEN.matcher(sentence).find()) {
                        hidden = notice;
                    } else if (DELAY.matcher(sentence).find()) {
                        delay = notice;
                    }
                }
                Matcher months = MONTHS.matcher(sentence);
                if (LIMITATION.matcher(sentence).find() && months.find()) {
                    limitation = new LimitationPeriod(months(months), clause.text());
                }
                if (DELAY.matcher(sentence).find() && EXCLUDED.matcher(sentence).find()) {
                    delayExclusion = part.strip();
                }
            }
            if (LABELLING.matcher(text).find() && WEIGHT.matcher(text).find()) {
                labelling = clause.text();
            }
            if (TWO_PERSON.matcher(text).find()) {
                twoPerson = clause.text();
            }
        }
        if (liabilityCap == null) {
            liabilityCap = statutoryCap;
        }
        return new ContractTerms(liabilityCap, hidden, delay, limitation, delayExclusion, heaviest, labelling, twoPerson);
    }

    private static List<String> sentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String part : SENTENCE_END.split(text)) {
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(part);
            if (!ABBREVIATION.matcher(part).find()) {
                sentences.add(current.toString());
                current = new StringBuilder();
            }
        }
        if (!current.isEmpty()) {
            sentences.add(current.toString());
        }
        return sentences;
    }

    private static int hours(Matcher period) {
        int amount = Integer.parseInt(period.group(1));
        String unit = period.group(2);
        return unit.startsWith("hour") || unit.startsWith("stunde") ? amount : amount * 24;
    }

    private static int months(Matcher period) {
        int amount = Integer.parseInt(period.group(1));
        String unit = period.group(2);
        return unit.startsWith("year") || unit.startsWith("jahr") ? amount * 12 : amount;
    }
}
