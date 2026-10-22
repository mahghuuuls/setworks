package com.mahghuuuls.setworks.command;

import com.mahghuuuls.setworks.SetworksMod;
import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import com.mahghuuuls.setworks.core.location.VanillaLocations;
import com.mahghuuuls.setworks.core.eval.PlayerSetState;
import com.mahghuuuls.setworks.core.eval.PlayerStateTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@code /setworks list | validate | inspect <player>}, operators only (permission level 2). */
public final class SetworksCommand extends CommandBase {

    private static final String USAGE = "/setworks <list|validate|inspect <player>>";

    @Override
    public String getName() {
        return "setworks";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(USAGE);
        }
        List<String> lines;
        switch (args[0].toLowerCase()) {
            case "list":
                lines = list();
                break;
            case "validate":
                lines = SetworksMod.runtime().registry().report().lines();
                break;
            case "inspect":
                if (args.length < 2) {
                    throw new WrongUsageException("/setworks inspect <player>");
                }
                lines = inspect(server, args[1]);
                break;
            default:
                throw new WrongUsageException(USAGE);
        }
        for (String line : lines) {
            sender.sendMessage(new TextComponentString(line));
        }
        SetworksMod.LOGGER.info("/setworks {} by {}: {} lines", args[0], sender.getName(), lines.size());
        for (String line : lines) {
            SetworksMod.LOGGER.info("  {}", line);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "validate", "inspect");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return new ArrayList<String>();
    }

    private static List<String> inspect(MinecraftServer server, String name) {
        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(name);
        if (target == null) {
            return Arrays.asList("no online player named " + name);
        }
        PlayerStateTracker tracker = SetworksMod.runtime().tracker();
        if (tracker == null) {
            return Arrays.asList("evaluation is not running yet");
        }
        PlayerSetState state = tracker.stateOf(target.getUniqueID());
        if (state == null) {
            return Arrays.asList("no state for " + name + " (fake player, or not evaluated yet)");
        }
        return InspectReport.lines(state, tracker.evaluator().definitions());
    }

    private static List<String> list() {
        FrozenDefinitions definitions = SetworksMod.runtime().registry().definitions();
        List<String> lines = new ArrayList<String>();
        lines.add("sets: " + definitions.enabledSetCount() + " enabled of " + definitions.allSets().size());
        for (FrozenDefinitions.Entry<SetDefinition> entry : definitions.allSets()) {
            SetDefinition set = entry.definition();
            lines.add(" " + set.id() + " \"" + set.displayName() + "\" pieces=" + set.pieces().size()
                    + " mode=" + set.thresholdMode().name().toLowerCase()
                    + (entry.enabled() ? "" : " DISABLED") + " source=" + entry.source().describe());
        }
        lines.add("bonuses: " + definitions.enabledBonusCount() + " enabled of " + definitions.allBonuses().size());
        for (FrozenDefinitions.Entry<BonusDefinition> entry : definitions.allBonuses()) {
            BonusDefinition bonus = entry.definition();
            lines.add(" " + bonus.id() + " requires=" + bonus.requirements()
                    + (bonus.description() == null ? "" : " \"" + bonus.description() + "\"")
                    + " visibility=" + bonus.visibility().name().toLowerCase()
                    + (bonus.isScripted() ? " scripted" : " effects=" + bonus.effects().size())
                    + (bonus.conditions().isEmpty() ? "" : " conditions=" + bonus.conditions().size())
                    + (entry.enabled() ? "" : " DISABLED") + " source=" + entry.source().describe());
        }
        lines.add("locations: " + SetworksMod.runtime().locations().all().size());
        int slots = 0;
        String slotSource = null;
        for (ILocation location : SetworksMod.runtime().locations().all()) {
            String source = location.source() == null ? "unknown" : location.source().describe();
            if (location.id().startsWith(VanillaLocations.SLOT_PREFIX)) {
                slots++;
                slotSource = source;
                continue;
            }
            lines.add(" " + location.id() + " source=" + source);
        }
        if (slots > 0) {
            lines.add(" " + VanillaLocations.SLOT_PREFIX + "0 to " + VanillaLocations.SLOT_PREFIX + (slots - 1)
                    + " (" + slots + " single slots) source=" + slotSource);
        }
        return lines;
    }
}
