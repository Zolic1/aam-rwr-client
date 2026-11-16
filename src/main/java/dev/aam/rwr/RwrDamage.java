package dev.aam.rwr;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 本地伤害估算喵（血量被插件隐藏时用喵）：
 *  原始伤害 = 攻击力属性 + 武器附魔 × 攻击冷却
 *  护甲减伤 = 读到真实护甲就按原版公式；读不到用默认值（玩家 20 / 怪物 1 喵）
 *  附魔减伤 = 读目标装备上的保护附魔；读不到用默认值（玩家 保护IV / 怪物 0 喵）
 */
public final class RwrDamage {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private RwrDamage() {}

    /** 估算「我这一下」对目标造成的最终伤害喵 */
    public static float estimate(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return (float) RwrConfig.hiddenHitDamage;

        // ==================== 原始伤害喵 ====================
        double raw = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack held = p.getMainHandItem();

        if (!held.isEmpty() && mc.level != null) {
            var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            int sharp = level(lookup, Enchantments.SHARPNESS, held);
            if (sharp > 0) raw += 1.0 + 0.5 * (sharp - 1);

            boolean undead = target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
            boolean arthropod = target.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD);
            if (undead) {
                int smite = level(lookup, Enchantments.SMITE, held);
                if (smite > 0) raw += 2.5 * smite;
            } else if (arthropod) {
                int bane = level(lookup, Enchantments.BANE_OF_ARTHROPODS, held);
                if (bane > 0) raw += 2.5 * bane;
            }
        }

        double cooldown = p.getAttackStrengthScale(0.5f);
        raw *= Math.max(0.2, Math.min(1.0, cooldown));

        // ==================== 护甲喵 ====================
        boolean isPlayer = target instanceof Player;
        double armor = readArmor(target, isPlayer);

        double toughness = 0.0;
        if (target.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS)) {
            toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        }

        double afterArmor = raw;
        if (armor > 0.0) {
            double f = 2.0 + toughness / 4.0;
            double g = Math.max(armor / 5.0, armor - raw / Math.max(0.001, f));
            g = Math.max(0.0, Math.min(20.0, g));
            afterArmor = raw * (1.0 - g / 25.0);
        }

        // ==================== 附魔减伤喵 ====================
        double protection = readProtection(target, isPlayer);

        double finalDmg = afterArmor * (1.0 - Math.max(0.0, Math.min(0.95, protection)));

        if (finalDmg <= 0.0 || Double.isNaN(finalDmg)) {
            return (float) RwrConfig.hiddenHitDamage;
        }
        return (float) finalDmg;
    }

    /** 读护甲值：能读就真值；读不到（属性缺失 / 穿着护甲却报 0）用默认值喵 */
    private static double readArmor(LivingEntity target, boolean isPlayer) {
        boolean hasAttr = target.getAttributes().hasAttribute(Attributes.ARMOR);
        double armor = hasAttr ? target.getArmorValue() : -1.0;

        boolean wearsArmor = false;
        if (isPlayer) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                if (!target.getItemBySlot(slot).isEmpty()) {
                    wearsArmor = true;
                    break;
                }
            }
        }

        // 穿着护甲却报 0（多半被插件隐藏了喵）→ 用默认值喵
        if (armor <= 0.01 && (wearsArmor || !hasAttr)) {
            return isPlayer ? RwrConfig.defaultPlayerArmor : RwrConfig.defaultMobArmor;
        }
        if (armor < 0.0) {
            return isPlayer ? RwrConfig.defaultPlayerArmor : RwrConfig.defaultMobArmor;
        }
        return armor;
    }

    /** 读目标装备上的保护附魔：EPF × 4%（上限 20 点喵） */
    private static double readProtection(LivingEntity target, boolean isPlayer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return isPlayer ? RwrConfig.defaultPlayerProtection : RwrConfig.defaultMobProtection;
        }
        var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> protHolder = lookup.get(Enchantments.PROTECTION).orElse(null);
        if (protHolder == null) {
            return isPlayer ? RwrConfig.defaultPlayerProtection : RwrConfig.defaultMobProtection;
        }

        int epf = 0;
        boolean sawAnyArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            sawAnyArmor = true;
            epf += EnchantmentHelper.getItemEnchantmentLevel(protHolder, stack);
        }

        if (!sawAnyArmor) {
            // 完全没穿护甲：真实值为 0 喵（不套默认保护喵）
            return 0.0;
        }
        return Math.min(20, epf) * 0.04;
    }

    private static int level(net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> lookup,
                             net.minecraft.resources.ResourceKey<Enchantment> key, ItemStack stack) {
        Holder<Enchantment> holder = lookup.get(key).orElse(null);
        if (holder == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }
}
