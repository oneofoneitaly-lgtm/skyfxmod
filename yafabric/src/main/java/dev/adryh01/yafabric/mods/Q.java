package dev.adryh01.yafabric.mods;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

/** Small helpers and the session statistics shared by the info modules. */
final class Q {

    static final MinecraftClient mc = MinecraftClient.getInstance();

    static final long SESSION_START = System.currentTimeMillis();
    static int hits, combo, bestCombo;
    static float lastDamage;
    static long lastDamageTime;
    static boolean hasDeath, deadFlag;
    static double deathX, deathY, deathZ;
    static int deathDim;
    static double walked;
    static int gapplesEaten;
    static long lastHitTime;

    private static boolean wasEating;
    private static int prevGapples;
    private static float prevHealth = -1.0F;
    private static int prevHurt;

    private Q() {
    }

    static String f(String fmt, Object... a) {
        return String.format(Locale.ROOT, fmt, a);
    }

    static ClientPlayerEntity p() {
        return mc.player;
    }

    static String name(ItemStack s) {
        return s.getItem().getDisplayName(s);
    }

    static Item item(String name) {
        return Item.getFromId("minecraft:" + name);
    }

    static BlockPos feet() {
        ClientPlayerEntity p = mc.player;
        return new BlockPos(Math.floor(p.x), Math.floor(p.y), Math.floor(p.z));
    }

    static int count(Item item) {
        if (item == null) return 0;
        int n = 0;
        for (ItemStack s : mc.player.inventory.main) {
            if (s != null && s.getItem() == item) n += s.count;
        }
        return n;
    }

    static int count(Block b) {
        return count(Item.fromBlock(b));
    }

    static int count(String name) {
        return count(item(name));
    }

    static int splashPotions() {
        int n = 0;
        for (ItemStack s : mc.player.inventory.main) {
            if (s != null && s.getItem() instanceof PotionItem && PotionItem.isThrowable(s.getData())) n += s.count;
        }
        return n;
    }

    static int freeSlots() {
        int n = 0;
        for (ItemStack s : mc.player.inventory.main) {
            if (s == null) n++;
        }
        return n;
    }

    /** Percent of durability left (0..100), or -1 when the stack has no durability. */
    static int durabilityPercent(ItemStack s) {
        if (s == null || !s.isDamageable()) return -1;
        int max = s.getMaxDamage();
        return (max - s.getDamage()) * 100 / Math.max(1, max);
    }

    static String compass(float yaw) {
        String[] d = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int i = (int) Math.floor(((yaw % 360.0F + 360.0F) % 360.0F) / 45.0F + 0.5F) % 8;
        return d[i];
    }

    static int ping() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        PlayerListEntry info = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return info == null ? 0 : info.getLatency();
    }

    /** Called every client tick. */
    static void tick(ClientPlayerEntity p) {
        if (prevHealth >= 0.0F && p.getHealth() < prevHealth - 0.01F) {
            lastDamage = prevHealth - p.getHealth();
            lastDamageTime = System.currentTimeMillis();
        }
        prevHealth = p.getHealth();
        if (p.hurtTime > prevHurt) combo = 0;
        prevHurt = p.hurtTime;
        if (p.getHealth() <= 0.0F && !deadFlag) {
            deadFlag = true;
            hasDeath = true;
            deathX = p.x;
            deathY = p.y;
            deathZ = p.z;
            deathDim = p.dimension;
        } else if (p.getHealth() > 0.0F) {
            deadFlag = false;
        }
        Item gapple = item("golden_apple");
        int gaps = count(gapple);
        boolean eating = p.isUsingItem() && p.getMainHandStack() != null && p.getMainHandStack().getItem() == gapple;
        if (wasEating && !eating && gaps < prevGapples) gapplesEaten++;
        wasEating = eating;
        prevGapples = gaps;
        double dx = p.x - p.prevX, dz = p.z - p.prevZ;
        if (p.onGround && p.vehicle == null) walked += Math.sqrt(dx * dx + dz * dz);
    }

    static void hit() {
        lastHitTime = System.currentTimeMillis();
        hits++;
        combo++;
        if (combo > bestCombo) bestCombo = combo;
    }
}
