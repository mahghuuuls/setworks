package com.mahghuuuls.setworks.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes which item stacks satisfy a piece. A filter is either an ore dictionary name or an item
 * id with optional metadata, required NBT paths, absent NBT paths, and an exact whole-NBT text.
 * An item-id-only filter ignores metadata and NBT. Immutable; build with the static factories and
 * the {@code with...} methods.
 */
public final class ItemFilter {

    /** Metadata value meaning "any metadata". */
    public static final int ANY_METADATA = -1;

    /** One required NBT entry: a slash-separated path and the value it must hold, or {@code null} for "any value". */
    public static final class NbtRule {
        private final String path;
        private final String value;

        /** {@code value} {@code null} or empty means the key only has to exist. */
        public NbtRule(String path, String value) {
            this.path = requireText(path, "path");
            this.value = value == null || value.isEmpty() ? null : value;
        }

        public String path() {
            return path;
        }

        /** {@code null} means the key only has to exist. */
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NbtRule)) {
                return false;
            }
            NbtRule other = (NbtRule) o;
            return path.equals(other.path) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, value);
        }

        @Override
        public String toString() {
            return value == null ? path : path + "=" + value;
        }
    }

    private final String itemId;
    private final int metadata;
    private final List<NbtRule> requiredNbt;
    private final List<String> absentNbt;
    private final String exactNbt;
    private final String oreName;

    private ItemFilter(String itemId, int metadata, List<NbtRule> requiredNbt, List<String> absentNbt,
                       String exactNbt, String oreName) {
        this.itemId = itemId;
        this.metadata = metadata;
        this.requiredNbt = Collections.unmodifiableList(new ArrayList<NbtRule>(requiredNbt));
        this.absentNbt = Collections.unmodifiableList(new ArrayList<String>(absentNbt));
        this.exactNbt = exactNbt;
        this.oreName = oreName;
    }

    /** Matches the item with any metadata and any NBT. */
    public static ItemFilter item(String itemId) {
        return new ItemFilter(requireText(itemId, "itemId"), ANY_METADATA, Collections.<NbtRule>emptyList(),
                Collections.<String>emptyList(), null, null);
    }

    /** Matches the item with exactly this metadata value. */
    public static ItemFilter item(String itemId, int metadata) {
        if (metadata < 0) {
            throw new IllegalArgumentException("metadata must be 0 or more");
        }
        return new ItemFilter(requireText(itemId, "itemId"), metadata, Collections.<NbtRule>emptyList(),
                Collections.<String>emptyList(), null, null);
    }

    /** Matches any item registered under this ore dictionary name. */
    public static ItemFilter ore(String oreName) {
        return new ItemFilter(null, ANY_METADATA, Collections.<NbtRule>emptyList(), Collections.<String>emptyList(),
                null, requireText(oreName, "oreName"));
    }

    public ItemFilter withRequiredNbt(String path, String value) {
        requireItemForm("requiredNbt");
        List<NbtRule> rules = new ArrayList<NbtRule>(requiredNbt);
        rules.add(new NbtRule(path, value));
        return new ItemFilter(itemId, metadata, rules, absentNbt, exactNbt, oreName);
    }

    public ItemFilter withAbsentNbt(String path) {
        requireItemForm("absentNbt");
        List<String> paths = new ArrayList<String>(absentNbt);
        paths.add(requireText(path, "path"));
        return new ItemFilter(itemId, metadata, requiredNbt, paths, exactNbt, oreName);
    }

    /** Requires the stack's whole NBT compound to equal this text (vanilla command NBT syntax). */
    public ItemFilter withExactNbt(String nbtText) {
        requireItemForm("exactNbt");
        return new ItemFilter(itemId, metadata, requiredNbt, absentNbt, requireText(nbtText, "nbtText"), oreName);
    }

    public boolean isOre() {
        return oreName != null;
    }

    /** {@code null} for an ore filter. */
    public String itemId() {
        return itemId;
    }

    /** {@link #ANY_METADATA} when metadata is not restricted. */
    public int metadata() {
        return metadata;
    }

    public List<NbtRule> requiredNbt() {
        return requiredNbt;
    }

    public List<String> absentNbt() {
        return absentNbt;
    }

    /** {@code null} when no exact match is required. */
    public String exactNbt() {
        return exactNbt;
    }

    /** {@code null} for an item filter. */
    public String oreName() {
        return oreName;
    }

    public boolean hasNbtRules() {
        return !requiredNbt.isEmpty() || !absentNbt.isEmpty() || exactNbt != null;
    }

    /** Short text for lists and errors, for example {@code minecraft:wool@5} or {@code ore:ingotIron}. */
    public String describe() {
        if (isOre()) {
            return "ore:" + oreName;
        }
        StringBuilder sb = new StringBuilder(itemId);
        if (metadata != ANY_METADATA) {
            sb.append('@').append(metadata);
        }
        for (NbtRule rule : requiredNbt) {
            sb.append(" +").append(rule);
        }
        for (String path : absentNbt) {
            sb.append(" -").append(path);
        }
        if (exactNbt != null) {
            sb.append(" =").append(exactNbt);
        }
        return sb.toString();
    }

    private void requireItemForm(String what) {
        if (isOre()) {
            throw new IllegalStateException(what + " cannot be combined with an ore dictionary filter");
        }
    }

    private static String requireText(String text, String what) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(what + " must not be empty");
        }
        return text.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemFilter)) {
            return false;
        }
        ItemFilter other = (ItemFilter) o;
        return metadata == other.metadata && Objects.equals(itemId, other.itemId)
                && requiredNbt.equals(other.requiredNbt) && absentNbt.equals(other.absentNbt)
                && Objects.equals(exactNbt, other.exactNbt) && Objects.equals(oreName, other.oreName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, metadata, requiredNbt, absentNbt, exactNbt, oreName);
    }

    @Override
    public String toString() {
        return describe();
    }
}
