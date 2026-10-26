package com.mahghuuuls.setworks.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds the set block to every standard item tooltip (inventory, containers, item browsers) from
 * the client store. Registered by the client proxy only.
 */
public final class TooltipHandler {

    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)\\}");

    /** Language lookup for descriptions: a bare key is translated when known, {@code ${key}} tokens always. */
    public static final TooltipRenderer.Translator I18N = new TooltipRenderer.Translator() {
        @Override
        public String translate(String textOrKey) {
            if (textOrKey == null) {
                return "";
            }
            if (textOrKey.indexOf("${") >= 0) {
                Matcher matcher = TOKEN.matcher(textOrKey);
                StringBuffer out = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(out, Matcher.quoteReplacement(I18n.format(matcher.group(1))));
                }
                matcher.appendTail(out);
                return out.toString();
            }
            return I18n.hasKey(textOrKey) ? I18n.format(textOrKey) : textOrKey;
        }
    };

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!ClientConfig.showTooltips()) {
            return;
        }
        ClientStore store = ClientStore.get();
        List<String> sets = store.setsOf(event.getItemStack());
        if (sets.isEmpty()) {
            return;
        }
        event.getToolTip().addAll(TooltipRenderer.render(store.snapshot(), store.state(), sets, GuiScreen.isShiftKeyDown(), I18N));
    }
}
