package com.example.telecom.audit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes what an update changed for the audit trail. Personal details are
 * listed by field name only; other fields show the old and new value.
 */
public final class ChangeSet {

    private final List<String> changes = new ArrayList<>();

    /** Notes that a personal-data field changed, without its values. */
    public ChangeSet field(String name, Object before, Object after) {
        if (!same(before, after)) {
            changes.add(name);
        }
        return this;
    }

    /** Notes a change with its old and new value. */
    public ChangeSet value(String name, Object before, Object after) {
        if (!same(before, after)) {
            changes.add(name + " " + format(before) + " → " + format(after));
        }
        return this;
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public String describe() {
        return changes.isEmpty() ? "no changes" : String.join(", ", changes);
    }

    private static boolean same(Object before, Object after) {
        if (before instanceof BigDecimal a && after instanceof BigDecimal b) {
            return a.compareTo(b) == 0;
        }
        return Objects.equals(before, after);
    }

    private static String format(Object value) {
        if (value == null) {
            return "none";
        }
        if (value instanceof BigDecimal amount) {
            return amount.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }
}
