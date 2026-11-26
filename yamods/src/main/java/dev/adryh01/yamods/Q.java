package dev.adryh01.yamods;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

import java.util.Locale;

/** Small helpers and the session statistics shared by the info modules. */
final class Q {

    static final Minecraft mc = Minecraft.getMinecraft();

    // session statistics
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

    static EntityPlayerSP p() {
        return mc.thePlayer;
    }

    static BlockPos feet() {
        EntityPlayerSP p = mc.thePlayer;
        return new BlockPos(Math.floor(p.posX), Math.floor(p.posY), Math.floor(p.posZ));
    }

    static int count(Item item) {
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null && s.getItem() == item) n += s.stackSize;
        }
        return n;
    }

    static int count(Block b) {
        return count(Item.getItemFromBlock(b));
    }

    static int splashPotions() {
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null && s.getItem() instanceof ItemPotion && ItemPotion.isSplash(s.getMetadata())) n += s.stackSize;
        }
        return n;
    }

    static int freeSlots() {
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s == null) n++;
        }
        return n;
    }

    /** Percent of durability left (0..100), or -1 when the stack has no durability. */
    static int durabilityPercent(ItemStack s) {
        if (s == null || !s.isItemStackDamageable()) return -1;
        int max = s.getMaxDamage();
        return (max - s.getItemDamage()) * 100 / Math.max(1, max);
    }

    static String compass(float yaw) {
        String[] d = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int i = (int) Math.floor(((yaw % 360.0F + 360.0F) % 360.0F) / 45.0F + 0.5F) % 8;
        return d[i];
    }

    static int ping() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return 0;
        net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    /** Called every client tick. */
    static void tick(EntityPlayerSP p) {
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
            deathX = p.posX;
            deathY = p.posY;
            deathZ = p.posZ;
            deathDim = p.dimension;
        } else if (p.getHealth() > 0.0F) {
            deadFlag = false;
        }

        int gaps = count(net.minecraft.init.Items.golden_apple);
        boolean eating = p.isUsingItem() && p.getHeldItem() != null && p.getHeldItem().getItem() == net.minecraft.init.Items.golden_apple;
        if (wasEating && !eating && gaps < prevGapples) gapplesEaten++;
        wasEating = eating;
        prevGapples = gaps;

        double dx = p.posX - p.prevPosX, dz = p.posZ - p.prevPosZ;
        if (p.onGround && p.ridingEntity == null) walked += Math.sqrt(dx * dx + dz * dz);
    }

    static void hit() {
        lastHitTime = System.currentTimeMillis();
        hits++;
        combo++;
        if (combo > bestCombo) bestCombo = combo;
    }
}
