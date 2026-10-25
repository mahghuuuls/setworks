package com.mahghuuuls.setworks.core.effect;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parsed attribute modifier parameters: {@code attribute} (a vanilla attribute name such as
 * {@code generic.armor}), {@code amount}, and {@code operation} ({@code add}, {@code multiply_base},
 * {@code multiply_total}, the three vanilla modifier operations in that order).
 */
public final class AttributeParams {

    public static final int ADD = 0;
    public static final int MULTIPLY_BASE = 1;
    public static final int MULTIPLY_TOTAL = 2;

    private static final String[] OPERATION_NAMES = {"add", "multiply_base", "multiply_total"};

    /**
     * Every attribute a vanilla player carries (registered by the living-entity and player constructors,
     * including the two Forge adds). Other namespaces belong to addons, which register their
     * attributes at entity construction, so they are checked when the effect is applied.
     */
    public static final Set<String> PLAYER_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "generic.maxHealth", "generic.knockbackResistance", "generic.movementSpeed", "generic.armor",
            "generic.armorToughness", "generic.attackDamage", "generic.attackSpeed", "generic.luck",
            "generic.reachDistance", "forge.swimSpeed")));

    private final String attribute;
    private final double amount;
    private final int operation;

    AttributeParams(String attribute, double amount, int operation) {
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
    }

    public String attribute() {
        return attribute;
    }

    public double amount() {
        return amount;
    }

    /** The vanilla operation code: 0 add, 1 multiply base, 2 multiply total. */
    public int operation() {
        return operation;
    }

    public String operationName() {
        return OPERATION_NAMES[operation];
    }

    /** For example {@code generic.armor add 2} or {@code generic.movementSpeed multiply_total 0.1}. */
    public String describe() {
        return attribute + " " + operationName() + " " + formatAmount(amount);
    }

    static String formatAmount(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /** Parses the parameters or throws {@link IllegalArgumentException} with the problem. */
    public static AttributeParams parse(Map<String, String> params) {
        String attribute = params.get("attribute");
        if (attribute == null || attribute.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute is missing");
        }
        String name = attribute.trim();
        if ((name.startsWith("generic.") || name.startsWith("forge.")) && !PLAYER_ATTRIBUTES.contains(name)) {
            throw new IllegalArgumentException("attribute " + name + " does not exist on players");
        }
        String amountText = params.get("amount");
        if (amountText == null || amountText.trim().isEmpty()) {
            throw new IllegalArgumentException("amount is missing");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount " + amountText + " is not a number");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("amount must be a finite number");
        }
        String operationText = params.get("operation");
        int operation = -1;
        if (operationText != null) {
            for (int i = 0; i < OPERATION_NAMES.length; i++) {
                if (OPERATION_NAMES[i].equalsIgnoreCase(operationText.trim())) {
                    operation = i;
                }
            }
        }
        if (operation < 0) {
            throw new IllegalArgumentException("operation " + operationText + " is not one of add, multiply_base, multiply_total");
        }
        return new AttributeParams(attribute.trim(), amount, operation);
    }

    /** The problem with the parameters, or {@code null} when they parse. */
    public static String problem(Map<String, String> params) {
        try {
            parse(params);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
