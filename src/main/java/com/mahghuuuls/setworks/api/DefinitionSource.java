package com.mahghuuuls.setworks.api;

import java.util.Objects;

/**
 * Where a definition came from: the registering mod, a script file (with its line when known), or a
 * Setworks built-in such as the examples. Shown by the list command and in every validation error.
 */
public final class DefinitionSource {

    public enum Kind { MOD, SCRIPT, BUILTIN }

    private final Kind kind;
    private final String name;
    private final String detail;

    private DefinitionSource(Kind kind, String name, String detail) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.name = Objects.requireNonNull(name, "name");
        this.detail = detail == null ? "" : detail;
    }

    public static DefinitionSource mod(String modId) {
        return new DefinitionSource(Kind.MOD, modId, null);
    }

    public static DefinitionSource script(String file, String lineDetail) {
        return new DefinitionSource(Kind.SCRIPT, file, lineDetail);
    }

    public static DefinitionSource builtin(String name) {
        return new DefinitionSource(Kind.BUILTIN, name, null);
    }

    public Kind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public String detail() {
        return detail;
    }

    /** Short human form, for example {@code mod:jass}, {@code script:sets.zs:12}, {@code setworks:examples}. */
    public String describe() {
        switch (kind) {
            case MOD:
                return "mod:" + name;
            case SCRIPT:
                return detail.isEmpty() ? "script:" + name : "script:" + name + ":" + detail;
            default:
                return name;
        }
    }

    @Override
    public String toString() {
        return describe();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefinitionSource)) {
            return false;
        }
        DefinitionSource other = (DefinitionSource) o;
        return kind == other.kind && name.equals(other.name) && detail.equals(other.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name, detail);
    }
}
