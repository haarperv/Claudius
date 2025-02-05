package me.crimp.claudius.utils;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class DamageUtil implements Util {
    public static int getRoundedDamage(ItemStack stack) {
        return (int) getDamageInPercent(stack);
    }

    public static float getDamageInPercent(ItemStack stack) {
        return (float) (getItemDamage(stack) / stack.getMaxDamage()) * 100.0f;
    }

    public static boolean isArmorLow(EntityPlayer player, int durability) {
        for (ItemStack piece : player.inventory.armorInventory) {
            if (piece == null) {
                return true;
            }
            if (getItemDamage(piece) >= durability) continue;
            return true;
        }
        return false;
    }

    public static int getItemDamage(ItemStack stack) {
        return stack.getMaxDamage() - stack.getItemDamage();
    }

    public static boolean canTakeDamage(boolean suicide) {
        return !mc.player.capabilities.isCreativeMode && !suicide;
    }

    public static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0f;
        double distancedsize = entity.getDistance(posX, posY, posZ) / (double) doubleExplosionSize;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = 0.0;
        try {
            blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
        } catch (Exception exception) {
            // empty catch block
        }
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * (double) doubleExplosionSize + 1.0);
        double finald = 1.0;
        if (entity instanceof EntityLivingBase) {
            finald = getBlastReduction((EntityLivingBase) entity, getDamageMultiplied(damage), new Explosion(mc.world, null, posX, posY, posZ, 6.0f, false, true));
        }
        return (float) finald;
    }

    public static float getBlastReduction(EntityLivingBase entity, float damageI, Explosion explosion) {
        float damage = damageI;
        if (entity instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer) entity;
            DamageSource ds = DamageSource.causeExplosionDamage(explosion);
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) ep.getTotalArmorValue(), (float) ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
            int k = 0;
            try {
                k = EnchantmentHelper.getEnchantmentModifierDamage(ep.getArmorInventoryList(), ds);
            } catch (Exception ignored) {}
            float f = MathHelper.clamp((float) k, 0.0f, 20.0f);
            damage *= 1.0f - f / 25.0f;
            if (entity.isPotionActive(MobEffects.RESISTANCE)) {
                damage -= damage / 4.0f;
            }
            damage = Math.max(damage, 0.0f);
            return damage;
        }
        damage = CombatRules.getDamageAfterAbsorb(damage, (float) entity.getTotalArmorValue(), (float) entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
        return damage;
    }

    public static float getDamageMultiplied(float damage) {
        int diff = mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }

    public static float calculateDamage(BlockPos pos, Entity entity) {
        return calculateDamage((double) pos.getX() + 0.5, pos.getY() + 1, (double) pos.getZ() + 0.5, entity);
    }

    public static int getCooldownByWeapon(EntityPlayer player) {
        Item item = player.getHeldItemMainhand().getItem();
        if (item instanceof ItemSword) {
            return 600;
        }
        if (item instanceof ItemPickaxe) {
            return 850;
        }
        if (item == Items.IRON_AXE) {
            return 1100;
        }
        if (item == Items.STONE_HOE) {
            return 500;
        }
        if (item == Items.IRON_HOE) {
            return 350;
        }
        if (item == Items.WOODEN_AXE || item == Items.STONE_AXE) {
            return 1250;
        }
        if (item instanceof ItemSpade || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE || item == Items.WOODEN_HOE || item == Items.GOLDEN_HOE) {
            return 1000;
        }
        return 250;
    }

    @SideOnly(Side.CLIENT)
    public class DamageTagUtil extends NBTTagCompound {

        public int getStackDamage(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if(tag != null && tag instanceof DamageUtil.DamageTagUtil) {
                return ((me.crimp.claudius.utils.DamageUtil.DamageTagUtil)tag).getTrueDamage();
            }
            return stack.getItemDamage();
        }

        private boolean empty;
        private int true_damage;

        public DamageTagUtil(boolean empty, int true_damage) {
            this.empty = empty;
            this.true_damage = true_damage;
        }

        public DamageTagUtil(NBTTagCompound old, int true_damage) {
            super();
            if(old == null) this.empty = true;
            else {
                for(String key : old.getKeySet()) {
                    super.setTag(key, old.getTag(key));
                }
            }
            this.true_damage = true_damage;
        }

        public int getTrueDamage() {
            return this.true_damage;
        }

        public byte getId() {
            if(this.empty) return 0;
            return super.getId();
        }

        public NBTTagCompound copy() {
            NBTTagCompound copy = new me.crimp.claudius.utils.DamageUtil.DamageTagUtil(this.empty, this.true_damage);

            for (String s : this.getKeySet()) {
                ((me.crimp.claudius.utils.DamageUtil.DamageTagUtil)copy).setTagLegacy(s, this.getTag(s).copy());
            }

            return copy;
        }

        public boolean hasNoTags() { // do not clear me
            if(hasNoTags()) {
                this.empty = true;
            }
            return false;
        }

        public void setTag(String key, NBTBase value) {
            this.empty = false;
            super.setTag(key, value);
        }

        public void setTagLegacy(String key, NBTBase value) {
            super.setTag(key, value);
        }

        public void setInteger(String key, int value) {
            this.empty = false;
            super.setInteger(key, value);
        }

        public void setByte(String key, byte value) {
            this.empty = false;
            super.setByte(key, value);
        }

        public void setShort(String key, short value) {
            this.empty = false;
            super.setShort(key, value);
        }

        public void setLong(String key, long value) {
            this.empty = false;
            super.setLong(key, value);
        }

        public void setUniqueId(String key, UUID value) {
            this.empty = false;
            super.setUniqueId(key, value);
        }

        public void setFloat(String key, float value) {
            this.empty = false;
            super.setFloat(key, value);
        }

        public void setDouble(String key, double value) {
            this.empty = false;
            super.setDouble(key, value);
        }

        public void setString(String key, String value) {
            this.empty = false;
            super.setString(key, value);
        }

        public void setByteArray(String key, byte[] value) {
            this.empty = false;
            super.setByteArray(key, value);
        }

        public void setIntArray(String key, int[] value) {
            this.empty = false;
            super.setIntArray(key, value);
        }

    }
}

