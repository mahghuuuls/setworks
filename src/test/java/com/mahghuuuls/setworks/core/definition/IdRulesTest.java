package com.mahghuuuls.setworks.core.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdRulesTest {

    @Test
    void acceptsLowercaseWithOptionalNamespace() {
        assertTrue(IdRules.isValid("iron_armor"));
        assertTrue(IdRules.isValid("setworks:iron_armor2"));
        assertTrue(IdRules.isValid("a:b"));
    }

    @Test
    void rejectsSpacesUppercaseAndDoubleNamespaces() {
        assertFalse(IdRules.isValid("Iron Armor"));
        assertFalse(IdRules.isValid("IronArmor"));
        assertFalse(IdRules.isValid("a:b:c"));
        assertFalse(IdRules.isValid(":b"));
        assertFalse(IdRules.isValid("a:"));
        assertFalse(IdRules.isValid(""));
        assertFalse(IdRules.isValid(null));
    }
}
