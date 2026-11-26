package dev.adryh01.skyfx;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

/** /skyfx            -> opens the menu
 *  /skyfx <id>       -> sets a sky directly
 *  /skyfx list       -> lists the sky ids
 */
public class SkyCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "skyfx";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/skyfx [sky id | list]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            SkyFX.openMenu = true; // opened on the next tick, after the chat screen closes
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
            StringBuilder sb = new StringBuilder("§7Skies: §f");
            for (int i = 0; i < Sky.ALL.size(); i++) {
                if (i > 0) sb.append("§7, §f");
                sb.append(Sky.ALL.get(i).id);
            }
            say(sender, sb.toString());
            return;
        }
        Sky s = Sky.byId(args[0]);
        if (s == null) {
            say(sender, "§cUnknown sky. Use /skyfx list");
            return;
        }
        SkyFX.setSky(s);
        say(sender, "§7Sky set to §f" + s.name);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> ids = new ArrayList<String>();
            ids.add("list");
            for (Sky s : Sky.ALL) ids.add(s.id);
            return getListOfStringsMatchingLastWord(args, ids);
        }
        return null;
    }

    private static void say(ICommandSender s, String msg) {
        s.addChatMessage(new ChatComponentText(msg));
    }
}
