package dev.adryh01.yamods;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

import java.util.List;

/** The text/info modules of YA CLIENT that exist in 1.8.9. */
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
            EntityPlayerSP p = Q.p();
            double dx = p.posX - p.prevPosX, dz = p.posZ - p.prevPosZ;
            return Q.f("Speed: %.2f b/s", Math.sqrt(dx * dx + dz * dz) * 20.0D);
        });
        info(all, "direction", "Direction", "Compass direction you face.", "Info", "Direction: N",
                m -> "Direction: " + Q.compass(Q.p().rotationYaw));
        info(all, "session", "Session Time", "How long you have been playing.", "Info", "Session: 0:12:34", m -> {
            long s = (System.currentTimeMillis() - Q.SESSION_START) / 1000L;
            return Q.f("Session: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        });
        info(all, "memory", "Memory", "Java memory in use.", "Info", "Memory: 512/2048 MB", m -> {
            Runtime r = Runtime.getRuntime();
            return Q.f("Memory: %d/%d MB", (r.totalMemory() - r.freeMemory()) / 1048576L, r.maxMemory() / 1048576L);
        });
        info(all, "playercount", "Player Count", "Players online on the server.", "Info", "Players: 24", m -> {
            if (Q.mc.getNetHandler() == null) return "";
            return "Players: " + Q.mc.getNetHandler().getPlayerInfoMap().size();
        });
        info(all, "server", "Server Address", "The server you are on.", "Info", "Server: play.example.com", m -> {
            ServerData sd = Q.mc.getCurrentServerData();
            return "Server: " + (sd == null ? "Singleplayer" : sd.serverIP);
        });
        info(all, "entities", "Entity Count", "Entities around you.", "Info", "Entities: 128",
                m -> "Entities: " + Q.mc.theWorld.loadedEntityList.size());
        info(all, "falldistance", "Fall Distance", "Blocks you have fallen.", "Info", "Fall: 0.0",
                m -> Q.f("Fall: %.1f", Q.p().fallDistance));
        info(all, "health", "Health", "Your health.", "Info", "Health: 20.0 HP",
                m -> Q.f("Health: %.1f HP", Q.p().getHealth() + Q.p().getAbsorptionAmount()));
        info(all, "food", "Food", "Hunger and saturation.", "Info", "Food: 20  Sat 5.0",
                m -> Q.f("Food: %d  Sat %.1f", Q.p().getFoodStats().getFoodLevel(), Q.p().getFoodStats().getSaturationLevel()));
        info(all, "xp", "XP Level", "Your experience level.", "Info", "XP Level: 30",
                m -> "XP Level: " + Q.p().experienceLevel);
        info(all, "xpprogress", "XP Progress", "Percent to the next level.", "Info", "XP: 45%",
                m -> Q.f("XP: %d%%", (int) (Q.p().experience * 100.0F)));
        info(all, "blockinfo", "Block Info", "Name of the block you look at.", "Info", "Block: Stone", m -> {
            MovingObjectPosition o = Q.mc.objectMouseOver;
            if (o == null || o.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return "";
            return "Block: " + Q.mc.theWorld.getBlockState(o.getBlockPos()).getBlock().getLocalizedName();
        });
        info(all, "iteminfo", "Item Info", "Held item and durability.", "Info", "Diamond Sword (1561/1561)", m -> {
            ItemStack h = Q.p().getHeldItem();
            if (h == null) return "";
            String s = h.getDisplayName();
            if (h.isItemStackDamageable()) s += " (" + (h.getMaxDamage() - h.getItemDamage()) + "/" + h.getMaxDamage() + ")";
            return s;
        });
        info(all, "nearby", "Nearby Players", "Players within 64 blocks.", "Info", "Steve 12m\nAlex 31m", m -> {
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (EntityPlayer e : Q.mc.theWorld.playerEntities) {
                if (e == Q.p()) continue;
                float d = Q.p().getDistanceToEntity(e);
                if (d > 64.0F) continue;
                if (n++ >= 6) break;
                sb.append(e.getName()).append(' ').append((int) d).append("m\n");
            }
            return n == 0 ? "No players nearby" : sb.toString().trim();
        });
        info(all, "netherxyz", "Nether Coords", "Matching Nether / Overworld coordinates for portals.", "Info", "Nether: 16 64 -37", m -> {
            EntityPlayerSP p = Q.p();
            int x = (int) Math.floor(p.posX), y = (int) Math.floor(p.posY), z = (int) Math.floor(p.posZ);
            if (p.dimension == -1) return "Overworld: " + (x * 8) + " " + y + " " + (z * 8);
            return "Nether: " + Math.floorDiv(x, 8) + " " + y + " " + Math.floorDiv(z, 8);
        });
        info(all, "deathpos", "Death Location", "Where you died last, so you can go back for your items.", "Info", "Died: 120 64 -80 (Overworld)", m -> {
            if (!Q.hasDeath) return "No death yet";
            return "Died: " + (int) Q.deathX + " " + (int) Q.deathY + " " + (int) Q.deathZ + " (" + dimName(Q.deathDim) + ")";
        });
        info(all, "daycount", "Day Counter", "World day and in-game time.", "Info", "Day 12  14:30", m -> {
            long t = Q.mc.theWorld.getWorldTime();
            long tod = (t % 24000L + 6000L) % 24000L;
            return Q.f("Day %d  %02d:%02d", t / 24000L + 1, tod / 1000L, (tod % 1000L) * 60L / 1000L);
        });
        info(all, "lightlevel", "Light Level", "Light at your feet, below 8 mobs can spawn.", "Info", "Light: 12",
                m -> "Light: " + Q.mc.theWorld.getLightFromNeighbors(Q.feet()));
        info(all, "chunkpos", "Chunk Position", "Chunk you are in and your place inside it.", "Info", "Chunk: 8, -3  (4, 11)", m -> {
            EntityPlayerSP p = Q.p();
            return "Chunk: " + p.chunkCoordX + ", " + p.chunkCoordZ + "  (" + ((int) Math.floor(p.posX) & 15) + ", " + ((int) Math.floor(p.posZ) & 15) + ")";
        });
        info(all, "targetdist", "Target Distance", "Distance to the block or entity you look at.", "Info", "Target: 3.4 m", m -> {
            MovingObjectPosition o = Q.mc.objectMouseOver;
            if (o == null || o.hitVec == null || o.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) return "";
            return Q.f("Target: %.1f m", o.hitVec.distanceTo(Q.p().getPositionEyes(1.0F)));
        });
        info(all, "sprintstate", "Sprint State", "Shows when you are sprinting or sneaking.", "Info", "Sprinting",
                m -> Q.p().isSprinting() ? "Sprinting" : (Q.p().isSneaking() ? "Sneaking" : "Walking"));
        info(all, "gamemodeinfo", "Game Mode", "Creative, Spectator or Survival.", "Info", "Survival", m -> {
            String n = Q.mc.playerController.getCurrentGameType().getName();
            return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        });
        info(all, "lookangles", "Look Angles", "Your exact yaw and pitch.", "Info", "Yaw 90.0  Pitch 12.5",
                m -> Q.f("Yaw %.1f  Pitch %.1f", MathHelper.wrapAngleTo180_float(Q.p().rotationYaw), Q.p().rotationPitch));
        info(all, "walked", "Distance Walked", "Blocks walked this session.", "Info", "Walked: 842 blocks",
                m -> Q.f("Walked: %.0f blocks", Q.walked));
        info(all, "mountspeed", "Mount Speed", "Speed of your horse, boat or minecart.", "Info", "Mount 8.4 b/s", m -> {
            Entity r = Q.p().ridingEntity;
            if (r == null) return "";
            double dx = r.posX - r.prevPosX, dz = r.posZ - r.prevPosZ;
            return Q.f("Mount %.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20.0D);
        });
        info(all, "breath", "Breath", "Air left while underwater.", "Info", "Air: 80%", m -> {
            int a = Q.p().getAir();
            return a >= 300 ? "" : "Air: " + Math.max(0, a * 100 / 300) + "%";
        });
        info(all, "mobcount", "Mob Counter", "Hostile mobs within 32 blocks, handy for mob farms.", "Info", "Mobs: 14", m -> {
            int n = 0;
            for (Object o : Q.mc.theWorld.loadedEntityList) {
                if (o instanceof IMob && Q.p().getDistanceToEntity((Entity) o) <= 32.0F) n++;
            }
            return "Mobs: " + n;
        });
        info(all, "dropcount", "Dropped Items", "Item entities lying around you, for farms and drops.", "Info", "Drops: 36", m -> {
            int n = 0;
            for (Object o : Q.mc.theWorld.loadedEntityList) {
                if (o instanceof EntityItem && Q.p().getDistanceToEntity((Entity) o) <= 16.0F) n++;
            }
            return "Drops: " + n;
        });
        info(all, "plusstats", "Session Stats", "Hits, combo and best combo this session.", "Info", "Hits: 42  Combo: 3  Best: 9",
                m -> "Hits: " + Q.hits + "  Combo: " + Q.combo + "  Best: " + Q.bestCombo);

        // ------------------------------------------------------------ PvP
        info(all, "combo", "Combo", "Consecutive hits without being hit.", "PvP", "Combo: 3", m -> "Combo: " + Q.combo);
        info(all, "bowcharge", "Bow Charge", "How charged your bow is.", "PvP", "Bow: 100%", m -> {
            EntityPlayerSP p = Q.p();
            ItemStack h = p.getHeldItem();
            if (!p.isUsingItem() || h == null || !(h.getItem() instanceof ItemBow)) return "";
            return "Bow: " + Math.min(100, p.getItemInUseDuration() * 5) + "%";
        });
        info(all, "potcount", "Potion Count", "Splash potions in your inventory.", "PvP", "Potions: 12",
                m -> "Potions: " + Q.splashPotions());
        info(all, "gapcount", "Gapple Count", "Golden apples in your inventory.", "PvP", "Gapples: 8",
                m -> "Gapples: " + Q.count(Items.golden_apple));
        info(all, "pearlcount", "Pearl Count", "Ender pearls in your inventory.", "PvP", "Pearls: 16",
                m -> "Pearls: " + Q.count(Items.ender_pearl));
        info(all, "arrowcount", "Arrow Count", "Arrows in your inventory.", "PvP", "Arrows: 64",
                m -> "Arrows: " + Q.count(Items.arrow));
        info(all, "obsidiancount", "Obsidian Count", "Obsidian in your inventory.", "PvP", "Obsidian: 64",
                m -> "Obsidian: " + Q.count(Blocks.obsidian));
        info(all, "supplies", "Supplies", "Counts obsidian, gapples, pearls and potions.", "PvP",
                "Obsidian: 64\nGapples: 8\nPearls: 16\nPotions: 12",
                m -> "Obsidian: " + Q.count(Blocks.obsidian) + "\nGapples: " + Q.count(Items.golden_apple)
                        + "\nPearls: " + Q.count(Items.ender_pearl) + "\nPotions: " + Q.splashPotions());
        info(all, "crystalsnear", "Crystals Nearby", "End crystals within 12 blocks.", "PvP", "Crystals: 3", m -> {
            int n = 0;
            for (Object o : Q.mc.theWorld.loadedEntityList) {
                if (o instanceof EntityEnderCrystal && Q.p().getDistanceToEntity((Entity) o) <= 12.0F) n++;
            }
            return "Crystals: " + n;
        });
        info(all, "crystaldist", "Crystal Distance", "Distance to the nearest end crystal.", "PvP", "Nearest crystal 6.2 m", m -> {
            float best = -1.0F;
            for (Object o : Q.mc.theWorld.loadedEntityList) {
                if (!(o instanceof EntityEnderCrystal)) continue;
                float d = Q.p().getDistanceToEntity((Entity) o);
                if (best < 0.0F || d < best) best = d;
            }
            return best < 0.0F ? "Nearest crystal -" : Q.f("Nearest crystal %.1f m", best);
        });
        info(all, "armorvalue", "Armor Value", "Your total armor points.", "PvP", "Armor: 20",
                m -> "Armor: " + Q.p().getTotalArmorValue());
        info(all, "lastdamage", "Last Damage", "Damage of the last hit you took.", "PvP", "Last hit -4.0  (3s ago)", m -> {
            if (Q.lastDamageTime == 0L) return "No damage yet";
            return Q.f("Last hit -%.1f  (%ds ago)", Q.lastDamage, (System.currentTimeMillis() - Q.lastDamageTime) / 1000L);
        });
        info(all, "shieldstate", "Block State", "Shows when you are blocking with a sword.", "PvP", "Blocking",
                m -> Q.p().isBlocking() ? "Blocking" : "");
        warn(all, "burning", "Burning Alert", "Warns while you are on fire.", "PvP", "BURNING!",
                m -> Q.p().isBurning() ? "BURNING!" : "");
        warn(all, "armorwarn", "Armor Warning", "Warns when a piece of armor is almost broken.", "PvP", "ARMOR LOW!", m -> {
            for (ItemStack s : Q.p().inventory.armorInventory) {
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
                m -> Q.count(Items.golden_apple) == 0 ? "NO GAPPLES!" : "");
        info(all, "hittimer", "Hit Timer", "Time since your last attack.", "PvP", "Last hit: 0.4 s", m -> {
            if (Q.lastHitTime == 0L) return "Last hit: -";
            return Q.f("Last hit: %.1f s", Math.min(99.0D, (System.currentTimeMillis() - Q.lastHitTime) / 1000.0D));
        });
        all.add(new Pvp.SmoothSneak());
        all.add(new Pvp.OldAnimations());
        all.add(new Pvp.HitHearts());
        all.add(new Pvp.Hitbox());
        all.add(new Pvp.NoHurtCam());
        all.add(new Novel.HitWindow());
        all.add(new Novel.ThrowPreview());
        all.add(new Novel.FightRecap());
        all.add(new Novel.HitCoach());
        all.add(new Novel.HitSounds());
        all.add(new Novel.KillEffects());
        all.add(new Novel.DamagePopups());
        all.add(new Novel.TargetGear());
        all.add(new Fx.HitSparks());
        all.add(new Fx.NoFovEffects());
        all.add(new Fx.TargetHud());

        // ------------------------------------------------------------ Utility
        info(all, "freeslots", "Free Slots", "Empty inventory slots left.", "Utility", "Free slots: 12",
                m -> "Free slots: " + Q.freeSlots());
        info(all, "xpbottles", "XP Bottles", "Bottles o' enchanting in your inventory.", "Utility", "XP bottles: 32",
                m -> "XP bottles: " + Q.count(Items.experience_bottle));
        info(all, "handcount", "Held Item Count", "How many of the item in your hand you carry in total.", "Utility", "Cobblestone: 256", m -> {
            ItemStack h = Q.p().getHeldItem();
            if (h == null) return "";
            int n = 0;
            for (ItemStack s : Q.p().inventory.mainInventory) {
                if (s != null && s.isItemEqual(h)) n += s.stackSize;
            }
            return h.getDisplayName() + ": " + n;
        });
        info(all, "blocksleft", "Blocks Left", "How many of the block in your hand you carry (BedWars / SkyWars).", "Utility", "Blocks: 128", m -> {
            ItemStack h = Q.p().getHeldItem();
            if (h == null || !(h.getItem() instanceof ItemBlock)) return "";
            int n = 0;
            for (ItemStack s : Q.p().inventory.mainInventory) {
                if (s != null && s.getItem() == h.getItem()) n += s.stackSize;
            }
            return "Blocks: " + n;
        });
        info(all, "resources", "Resources", "Iron, gold, diamonds and emeralds you carry (BedWars).", "Utility",
                "Iron: 32  Gold: 8\nDiamonds: 2  Emeralds: 1",
                m -> "Iron: " + Q.count(Items.iron_ingot) + "  Gold: " + Q.count(Items.gold_ingot)
                        + "\nDiamonds: " + Q.count(Items.diamond) + "  Emeralds: " + Q.count(Items.emerald));
        info(all, "bwitems", "BedWars Items", "Fireballs, TNT and water buckets.", "Utility", "Fireballs: 2  TNT: 4  Water: 1",
                m -> "Fireballs: " + Q.count(Items.fire_charge) + "  TNT: " + Q.count(Blocks.tnt) + "  Water: " + Q.count(Items.water_bucket));
        info(all, "buckets", "Bucket Count", "Water and lava buckets.", "Utility", "Water: 2  Lava: 1",
                m -> "Water: " + Q.count(Items.water_bucket) + "  Lava: " + Q.count(Items.lava_bucket));
        info(all, "fireworks", "Firework Count", "Firework rockets in your inventory.", "Utility", "Fireworks: 24",
                m -> "Fireworks: " + Q.count(Items.fireworks));
        warn(all, "hungerwarn", "Hunger Warning", "Warns when your food is low.", "Utility", "HUNGRY!",
                m -> Q.p().getFoodStats().getFoodLevel() <= m.v("limit") ? "HUNGRY!" : "",
                Setting.slider("limit", "Warn at food", 6.0F, 1.0F, 19.0F, 1.0F));
        warn(all, "toolwarn", "Tool Warning", "Warns when your held tool is about to break.", "Utility", "TOOL ABOUT TO BREAK!", m -> {
            int pct = Q.durabilityPercent(Q.p().getHeldItem());
            return pct >= 0 && pct <= m.v("limit") ? "TOOL ABOUT TO BREAK!" : "";
        }, Setting.slider("limit", "Warn below (%)", 10.0F, 5.0F, 50.0F, 5.0F));
        warn(all, "fallalert", "Fall Alert", "Big warning when a fall would hurt.", "Utility", "FALL 12 blocks!", m -> {
            EntityPlayerSP p = Q.p();
            if (p.capabilities.isFlying || p.isInWater() || p.fallDistance < m.v("limit")) return "";
            return Q.f("FALL %.0f blocks!", p.fallDistance);
        }, Setting.slider("limit", "Warn from (blocks)", 4.0F, 2.0F, 20.0F, 1.0F));
        info(all, "hotbarslot", "Hotbar Slot", "Selected hotbar slot and the item in it.", "Utility", "Slot 1: Diamond Sword", m -> {
            ItemStack h = Q.p().getHeldItem();
            return "Slot " + (Q.p().inventory.currentItem + 1) + ": " + (h == null ? "empty" : h.getDisplayName());
        });
        all.add(new Panels.Board());
        all.add(new Panels.ChatBox());
        all.add(new Panels.TabList());
        all.add(new Pvp.ChatTime());
        all.add(new DiscordRpc());
        all.add(new Fx.ToggleSneak());
        all.add(new Fx.Fov());
        all.add(new Fx.FpsBoost());
        all.add(new Fx.UnlimitedFps());
        all.add(new Fx.VsyncOff());
        all.add(new Fx.InventoryHud());

        // ------------------------------------------------------------ Visual
        all.add(new NickTag());
        all.add(new Pvp.BlockOutline());
        all.add(new Pvp.TimeChanger());
        all.add(new Pvp.ClearWeather());
        all.add(new Fx.Crosshair());
        all.add(new Fx.LowHealth());
        all.add(new Fx.NoBobbing());
        all.add(new Fx.NoClouds());
        all.add(new Fx.NoScreenEffects());
        all.add(new Fx.RainbowHud());
        all.add(new Fx.Trail());
    }
}
