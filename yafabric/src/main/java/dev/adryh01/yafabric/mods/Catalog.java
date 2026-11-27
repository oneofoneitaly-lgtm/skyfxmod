package dev.adryh01.yafabric.mods;

import net.minecraft.block.Block;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.EndCrystalEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/** The text/info modules of YA CLIENT (ported from the Forge build). */
final class Catalog {

    private Catalog() {
    }

    private static void info(List<Module> all, String id, String name, String desc, String cat, String sample, Info.Text t, Setting... extra) {
        all.add(new Info(id, name, desc, cat, sample, false, t, extra));
    }

    private static void warn(List<Module> all, String id, String name, String desc, String cat, String sample, Info.Text t, Setting... extra) {
        all.add(new Info(id, name, desc, cat, sample, true, t, extra));
    }

    private static String dimName(int d) {
        return d == -1 ? "Nether" : (d == 1 ? "End" : "Overworld");
    }

    static void register(List<Module> all) {
        // ------------------------------------------------------------ Info
        info(all, "speed", "Speed", "Your speed in blocks per second.", "Info", "Speed: 4.32 b/s", m -> {
            ClientPlayerEntity p = Q.p();
            double dx = p.x - p.prevX, dz = p.z - p.prevZ;
            return Q.f("Speed: %.2f b/s", Math.sqrt(dx * dx + dz * dz) * 20.0D);
        });
        info(all, "direction", "Direction", "Compass direction you face.", "Info", "Direction: N",
                m -> "Direction: " + Q.compass(Q.p().yaw));
        info(all, "session", "Session Time", "How long you have been playing.", "Info", "Session: 0:12:34", m -> {
            long s = (System.currentTimeMillis() - Q.SESSION_START) / 1000L;
            return Q.f("Session: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        });
        info(all, "memory", "Memory", "Java memory in use.", "Info", "Memory: 512/2048 MB", m -> {
            Runtime r = Runtime.getRuntime();
            return Q.f("Memory: %d/%d MB", (r.totalMemory() - r.freeMemory()) / 1048576L, r.maxMemory() / 1048576L);
        });
        info(all, "playercount", "Player Count", "Players online on the server.", "Info", "Players: 24", m -> {
            if (Q.mc.getNetworkHandler() == null) return "";
            return "Players: " + Q.mc.getNetworkHandler().getPlayerList().size();
        });
        info(all, "server", "Server Address", "The server you are on.", "Info", "Server: play.example.com", m -> {
            ServerInfo sd = Q.mc.getCurrentServerEntry();
            return "Server: " + (sd == null ? "Singleplayer" : sd.address);
        });
        info(all, "entities", "Entity Count", "Entities around you.", "Info", "Entities: 128",
                m -> "Entities: " + Q.mc.world.loadedEntities.size());
        info(all, "falldistance", "Fall Distance", "Blocks you have fallen.", "Info", "Fall: 0.0",
                m -> Q.f("Fall: %.1f", Q.p().fallDistance));
        info(all, "health", "Health", "Your health.", "Info", "Health: 20.0 HP",
                m -> Q.f("Health: %.1f HP", Q.p().getHealth()));
        info(all, "food", "Food", "Hunger and saturation.", "Info", "Food: 20  Sat 5.0",
                m -> Q.f("Food: %d  Sat %.1f", Q.p().getHungerManager().getFoodLevel(), Q.p().getHungerManager().getSaturationLevel()));
        info(all, "xp", "XP Level", "Your experience level.", "Info", "XP Level: 30",
                m -> "XP Level: " + Q.p().experienceLevel);
        info(all, "xpprogress", "XP Progress", "Percent to the next level.", "Info", "XP: 45%",
                m -> Q.f("XP: %d%%", (int) (Q.p().experienceProgress * 100.0F)));
        info(all, "blockinfo", "Block Info", "Name of the block you look at.", "Info", "Block: Stone", m -> {
            BlockHitResult o = Q.mc.result;
            if (o == null || o.type != BlockHitResult.Type.BLOCK) return "";
            return "Block: " + Q.mc.world.getBlockState(o.getBlockPos()).getBlock().getTranslatedName();
        });
        info(all, "iteminfo", "Item Info", "Held item and durability.", "Info", "Diamond Sword (1561/1561)", m -> {
            ItemStack h = Q.p().getMainHandStack();
            if (h == null) return "";
            String s = Q.name(h);
            if (h.isDamageable()) s += " (" + (h.getMaxDamage() - h.getDamage()) + "/" + h.getMaxDamage() + ")";
            return s;
        });
        info(all, "nearby", "Nearby Players", "Players within 64 blocks.", "Info", "Steve 12m\nAlex 31m", m -> {
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (PlayerEntity e : Q.mc.world.playerEntities) {
                if (e == Q.p()) continue;
                float d = Q.p().distanceTo(e);
                if (d > 64.0F) continue;
                if (n++ >= 6) break;
                sb.append(e.getGameProfile().getName()).append(' ').append((int) d).append("m\n");
            }
            return n == 0 ? "No players nearby" : sb.toString().trim();
        });
        info(all, "netherxyz", "Nether Coords", "Matching Nether / Overworld coordinates for portals.", "Info", "Nether: 16 64 -37", m -> {
            ClientPlayerEntity p = Q.p();
            int x = (int) Math.floor(p.x), y = (int) Math.floor(p.y), z = (int) Math.floor(p.z);
            if (p.dimension == -1) return "Overworld: " + (x * 8) + " " + y + " " + (z * 8);
            return "Nether: " + Math.floorDiv(x, 8) + " " + y + " " + Math.floorDiv(z, 8);
        });
        info(all, "deathpos", "Death Location", "Where you died last, so you can go back for your items.", "Info", "Died: 120 64 -80 (Overworld)", m -> {
            if (!Q.hasDeath) return "No death yet";
            return "Died: " + (int) Q.deathX + " " + (int) Q.deathY + " " + (int) Q.deathZ + " (" + dimName(Q.deathDim) + ")";
        });
        info(all, "daycount", "Day Counter", "World day and in-game time.", "Info", "Day 12  14:30", m -> {
            long t = Q.mc.world.getTimeOfDay();
            long tod = (t % 24000L + 6000L) % 24000L;
            return Q.f("Day %d  %02d:%02d", t / 24000L + 1, tod / 1000L, (tod % 1000L) * 60L / 1000L);
        });
        info(all, "lightlevel", "Light Level", "Light at your feet, below 8 mobs can spawn.", "Info", "Light: 12",
                m -> "Light: " + Q.mc.world.getLightLevelWithNeighbours(Q.feet()));
        info(all, "chunkpos", "Chunk Position", "Chunk you are in and your place inside it.", "Info", "Chunk: 8, -3  (4, 11)", m -> {
            ClientPlayerEntity p = Q.p();
            return "Chunk: " + p.chunkX + ", " + p.chunkZ + "  (" + ((int) Math.floor(p.x) & 15) + ", " + ((int) Math.floor(p.z) & 15) + ")";
        });
        info(all, "targetdist", "Target Distance", "Distance to the block or entity you look at.", "Info", "Target: 3.4 m", m -> {
            BlockHitResult o = Q.mc.result;
            if (o == null || o.pos == null || o.type == BlockHitResult.Type.MISS) return "";
            return Q.f("Target: %.1f m", o.pos.distanceTo(Q.p().getCameraPosVec(1.0F)));
        });
        info(all, "sprintstate", "Sprint State", "Shows when you are sprinting or sneaking.", "Info", "Sprinting",
                m -> Q.p().isSprinting() ? "Sprinting" : (Q.p().isSneaking() ? "Sneaking" : "Walking"));
        info(all, "gamemodeinfo", "Game Mode", "Creative, Spectator or Survival.", "Info", "Survival", m -> {
            String n = Q.mc.interactionManager.getCurrentGameMode().getName();
            return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        });
        info(all, "lookangles", "Look Angles", "Your exact yaw and pitch.", "Info", "Yaw 90.0  Pitch 12.5",
                m -> Q.f("Yaw %.1f  Pitch %.1f", MathHelper.wrapDegrees(Q.p().yaw), Q.p().pitch));
        info(all, "walked", "Distance Walked", "Blocks walked this session.", "Info", "Walked: 842 blocks",
                m -> Q.f("Walked: %.0f blocks", Q.walked));
        info(all, "mountspeed", "Mount Speed", "Speed of your horse, boat or minecart.", "Info", "Mount 8.4 b/s", m -> {
            Entity r = Q.p().vehicle;
            if (r == null) return "";
            double dx = r.x - r.prevX, dz = r.z - r.prevZ;
            return Q.f("Mount %.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20.0D);
        });
        info(all, "breath", "Breath", "Air left while underwater.", "Info", "Air: 80%", m -> {
            int a = Q.p().getAir();
            return a >= 300 ? "" : "Air: " + Math.max(0, a * 100 / 300) + "%";
        });
        info(all, "mobcount", "Mob Counter", "Hostile mobs within 32 blocks, handy for mob farms.", "Info", "Mobs: 14", m -> {
            int n = 0;
            for (Object o : Q.mc.world.loadedEntities) {
                if (o instanceof Monster && Q.p().distanceTo((Entity) o) <= 32.0F) n++;
            }
            return "Mobs: " + n;
        });
        info(all, "dropcount", "Dropped Items", "Item entities lying around you, for farms and drops.", "Info", "Drops: 36", m -> {
            int n = 0;
            for (Object o : Q.mc.world.loadedEntities) {
                if (o instanceof ItemEntity && Q.p().distanceTo((Entity) o) <= 16.0F) n++;
            }
            return "Drops: " + n;
        });
        info(all, "plusstats", "Session Stats", "Hits, combo and best combo this session.", "Info", "Hits: 42  Combo: 3  Best: 9",
                m -> "Hits: " + Q.hits + "  Combo: " + Q.combo + "  Best: " + Q.bestCombo);

        // ------------------------------------------------------------ PvP
        info(all, "combo", "Combo", "Consecutive hits without being hit.", "PvP", "Combo: 3", m -> "Combo: " + Q.combo);
        info(all, "bowcharge", "Bow Charge", "How charged your bow is.", "PvP", "Bow: 100%", m -> {
            ClientPlayerEntity p = Q.p();
            ItemStack h = p.getMainHandStack();
            if (!p.isUsingItem() || h == null || !(h.getItem() instanceof BowItem)) return "";
            return "Bow: " + Math.min(100, p.getItemUseTicks() * 5) + "%";
        });
        info(all, "potcount", "Potion Count", "Splash potions in your inventory.", "PvP", "Potions: 12",
                m -> "Potions: " + Q.splashPotions());
        info(all, "gapcount", "Gapple Count", "Golden apples in your inventory.", "PvP", "Gapples: 8",
                m -> "Gapples: " + Q.count("golden_apple"));
        info(all, "pearlcount", "Pearl Count", "Ender pearls in your inventory.", "PvP", "Pearls: 16",
                m -> "Pearls: " + Q.count("ender_pearl"));
        info(all, "arrowcount", "Arrow Count", "Arrows in your inventory.", "PvP", "Arrows: 64",
                m -> "Arrows: " + Q.count("arrow"));
        info(all, "obsidiancount", "Obsidian Count", "Obsidian in your inventory.", "PvP", "Obsidian: 64",
                m -> "Obsidian: " + Q.count("obsidian"));
        info(all, "supplies", "Supplies", "Counts obsidian, gapples, pearls and potions.", "PvP",
                "Obsidian: 64\nGapples: 8\nPearls: 16\nPotions: 12",
                m -> "Obsidian: " + Q.count("obsidian") + "\nGapples: " + Q.count("golden_apple")
                        + "\nPearls: " + Q.count("ender_pearl") + "\nPotions: " + Q.splashPotions());
        info(all, "crystalsnear", "Crystals Nearby", "End crystals within 12 blocks.", "PvP", "Crystals: 3", m -> {
            int n = 0;
            for (Object o : Q.mc.world.loadedEntities) {
                if (o instanceof EndCrystalEntity && Q.p().distanceTo((Entity) o) <= 12.0F) n++;
            }
            return "Crystals: " + n;
        });
        info(all, "crystaldist", "Crystal Distance", "Distance to the nearest end crystal.", "PvP", "Nearest crystal 6.2 m", m -> {
            float best = -1.0F;
            for (Object o : Q.mc.world.loadedEntities) {
                if (!(o instanceof EndCrystalEntity)) continue;
                float d = Q.p().distanceTo((Entity) o);
                if (best < 0.0F || d < best) best = d;
            }
            return best < 0.0F ? "Nearest crystal -" : Q.f("Nearest crystal %.1f m", best);
        });
        info(all, "armorvalue", "Armor Value", "Your total armor points.", "PvP", "Armor: 20",
                m -> "Armor: " + Q.p().getArmorProtectionValue());
        info(all, "lastdamage", "Last Damage", "Damage of the last hit you took.", "PvP", "Last hit -4.0  (3s ago)", m -> {
            if (Q.lastDamageTime == 0L) return "No damage yet";
            return Q.f("Last hit -%.1f  (%ds ago)", Q.lastDamage, (System.currentTimeMillis() - Q.lastDamageTime) / 1000L);
        });
        info(all, "shieldstate", "Block State", "Shows when you are blocking with a sword.", "PvP", "Blocking",
                m -> Q.p().isUsingItem() && Q.p().getUsedItem() != null && Q.p().getUsedItem().getUseAction() == UseAction.BLOCK ? "Blocking" : "");
        warn(all, "burning", "Burning Alert", "Warns while you are on fire.", "PvP", "BURNING!",
                m -> Q.p().isOnFire() ? "BURNING!" : "");
        warn(all, "armorwarn", "Armor Warning", "Warns when a piece of armor is almost broken.", "PvP", "ARMOR LOW!", m -> {
            for (ItemStack s : Q.p().inventory.armor) {
                int pct = Q.durabilityPercent(s);
                if (pct >= 0 && pct <= m.v("limit")) return "ARMOR LOW!";
            }
            return "";
        }, Setting.slider("limit", "Warn below (%)", 10.0F, 5.0F, 50.0F, 5.0F));
        warn(all, "pingwarn", "Lag Warning", "Warns when your ping is high.", "PvP", "LAG! 250 ms", m -> {
            int ms = Q.ping();
            return ms > m.v("limit") ? "LAG! " + ms + " ms" : "";
        }, Setting.slider("limit", "Warn above (ms)", 150.0F, 50.0F, 500.0F, 10.0F));
        info(all, "gapplepops", "Gapples Eaten", "Golden apples you ate this session.", "PvP", "Gapples eaten: 6",
                m -> "Gapples eaten: " + Q.gapplesEaten);
        warn(all, "gapplewarn", "Gapple Warning", "Warns when you carry no golden apples.", "PvP", "NO GAPPLES!",
                m -> Q.count("golden_apple") == 0 ? "NO GAPPLES!" : "");
        info(all, "hittimer", "Hit Timer", "Time since your last attack.", "PvP", "Last hit: 0.4 s", m -> {
            if (Q.lastHitTime == 0L) return "Last hit: -";
            return Q.f("Last hit: %.1f s", Math.min(99.0D, (System.currentTimeMillis() - Q.lastHitTime) / 1000.0D));
        });

        // ------------------------------------------------------------ Utility
        info(all, "freeslots", "Free Slots", "Empty inventory slots left.", "Utility", "Free slots: 12",
                m -> "Free slots: " + Q.freeSlots());
        info(all, "xpbottles", "XP Bottles", "Bottles o' enchanting in your inventory.", "Utility", "XP bottles: 32",
                m -> "XP bottles: " + Q.count("experience_bottle"));
        info(all, "handcount", "Held Item Count", "How many of the item in your hand you carry in total.", "Utility", "Cobblestone: 256", m -> {
            ItemStack h = Q.p().getMainHandStack();
            if (h == null) return "";
            int n = 0;
            for (ItemStack s : Q.p().inventory.main) {
                if (s != null && s.getItem() == h.getItem()) n += s.count;
            }
            return Q.name(h) + ": " + n;
        });
        info(all, "blocksleft", "Blocks Left", "How many of the block in your hand you carry (BedWars / SkyWars).", "Utility", "Blocks: 128", m -> {
            ItemStack h = Q.p().getMainHandStack();
            if (h == null || !(h.getItem() instanceof BlockItem)) return "";
            int n = 0;
            for (ItemStack s : Q.p().inventory.main) {
                if (s != null && s.getItem() == h.getItem()) n += s.count;
            }
            return "Blocks: " + n;
        });
        info(all, "resources", "Resources", "Iron, gold, diamonds and emeralds you carry (BedWars).", "Utility",
                "Iron: 32  Gold: 8\nDiamonds: 2  Emeralds: 1",
                m -> "Iron: " + Q.count("iron_ingot") + "  Gold: " + Q.count("gold_ingot")
                        + "\nDiamonds: " + Q.count("diamond") + "  Emeralds: " + Q.count("emerald"));
        info(all, "bwitems", "BedWars Items", "Fireballs, TNT and water buckets.", "Utility", "Fireballs: 2  TNT: 4  Water: 1",
                m -> "Fireballs: " + Q.count("fire_charge") + "  TNT: " + Q.count("tnt") + "  Water: " + Q.count("water_bucket"));
        info(all, "buckets", "Bucket Count", "Water and lava buckets.", "Utility", "Water: 2  Lava: 1",
                m -> "Water: " + Q.count("water_bucket") + "  Lava: " + Q.count("lava_bucket"));
        info(all, "fireworks", "Firework Count", "Firework rockets in your inventory.", "Utility", "Fireworks: 24",
                m -> "Fireworks: " + Q.count("fireworks"));
        warn(all, "hungerwarn", "Hunger Warning", "Warns when your food is low.", "Utility", "HUNGRY!",
                m -> Q.p().getHungerManager().getFoodLevel() <= m.v("limit") ? "HUNGRY!" : "",
                Setting.slider("limit", "Warn at food", 6.0F, 1.0F, 19.0F, 1.0F));
        warn(all, "toolwarn", "Tool Warning", "Warns when your held tool is about to break.", "Utility", "TOOL ABOUT TO BREAK!", m -> {
            int pct = Q.durabilityPercent(Q.p().getMainHandStack());
            return pct >= 0 && pct <= m.v("limit") ? "TOOL ABOUT TO BREAK!" : "";
        }, Setting.slider("limit", "Warn below (%)", 10.0F, 5.0F, 50.0F, 5.0F));
        warn(all, "fallalert", "Fall Alert", "Big warning when a fall would hurt.", "Utility", "FALL 12 blocks!", m -> {
            ClientPlayerEntity p = Q.p();
            if (p.abilities.flying || p.isTouchingWater() || p.fallDistance < m.v("limit")) return "";
            return Q.f("FALL %.0f blocks!", p.fallDistance);
        }, Setting.slider("limit", "Warn from (blocks)", 4.0F, 2.0F, 20.0F, 1.0F));
        info(all, "hotbarslot", "Hotbar Slot", "Selected hotbar slot and the item in it.", "Utility", "Slot 1: Diamond Sword", m -> {
            ItemStack h = Q.p().getMainHandStack();
            return "Slot " + (Q.p().inventory.selectedSlot + 1) + ": " + (h == null ? "empty" : Q.name(h));
        });

        // ------------------------------------------------------------ Visual
    }
}
