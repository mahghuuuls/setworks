package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.ItemFilter;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/** Byte helpers shared by the messages: strings (empty for null) and item filters. */
final class Codec {

    private Codec() {
    }

    static void writeString(ByteBuf buf, String text) {
        ByteBufUtils.writeUTF8String(buf, text == null ? "" : text);
    }

    static String readString(ByteBuf buf) {
        return ByteBufUtils.readUTF8String(buf);
    }

    static void writeFilter(ByteBuf buf, ItemFilter filter) {
        buf.writeBoolean(filter.isOre());
        if (filter.isOre()) {
            writeString(buf, filter.oreName());
            return;
        }
        writeString(buf, filter.itemId());
        buf.writeInt(filter.metadata());
        ByteBufUtils.writeVarInt(buf, filter.requiredNbt().size(), 5);
        for (ItemFilter.NbtRule rule : filter.requiredNbt()) {
            writeString(buf, rule.path());
            writeString(buf, rule.value());
        }
        ByteBufUtils.writeVarInt(buf, filter.absentNbt().size(), 5);
        for (String path : filter.absentNbt()) {
            writeString(buf, path);
        }
        writeString(buf, filter.exactNbt());
    }

    static ItemFilter readFilter(ByteBuf buf) {
        if (buf.readBoolean()) {
            return ItemFilter.ore(readString(buf));
        }
        String itemId = readString(buf);
        int metadata = buf.readInt();
        ItemFilter filter = metadata == ItemFilter.ANY_METADATA ? ItemFilter.item(itemId) : ItemFilter.item(itemId, metadata);
        int required = ByteBufUtils.readVarInt(buf, 5);
        for (int i = 0; i < required; i++) {
            String path = readString(buf);
            String value = readString(buf);
            filter = filter.withRequiredNbt(path, value.isEmpty() ? null : value);
        }
        int absent = ByteBufUtils.readVarInt(buf, 5);
        for (int i = 0; i < absent; i++) {
            filter = filter.withAbsentNbt(readString(buf));
        }
        String exact = readString(buf);
        if (!exact.isEmpty()) {
            filter = filter.withExactNbt(exact);
        }
        return filter;
    }
}
