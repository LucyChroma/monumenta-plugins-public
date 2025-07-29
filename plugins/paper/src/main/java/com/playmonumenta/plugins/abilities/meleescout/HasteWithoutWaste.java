package com.playmonumenta.plugins.abilities.meleescout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.*;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.StringUtils;

import java.util.EnumMap;
import java.util.Objects;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HasteWithoutWaste extends Ability {

	private static final int LEVEL_1_EFFECT_LVL = 0;
	private static final int LEVEL_2_EFFECT_LVL = 1;
	private static final double ENHANCEMENT_SPEED = 0.02;
	private static final String ENHANCEMENT_SPEED_EFFECT_NAME = "HasteWithoutWasteEnhancement";

	public static final String CHARM_HASTE = "Haste Without Waste Haste Amplifier";

	private final int mHasteAmplifier;

	public static final AbilityInfo<HasteWithoutWaste> INFO =
		new AbilityInfo<>(HasteWithoutWaste.class, "Haste Without Waste", HasteWithoutWaste::new)
			.scoreboardId("HasteWithoutWaste")
			.shorthandName("HWW")
			.descriptions(
				String.format("You gain permanent Haste %s.", StringUtils.toRoman(LEVEL_1_EFFECT_LVL + 1)),
				String.format("You gain permanent Haste %s.", StringUtils.toRoman(LEVEL_2_EFFECT_LVL + 1)),
				String.format("Your base speed increases by %s.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_SPEED)))
			.simpleDescription("Gain haste.")
			.displayItem(Material.GOLDEN_PICKAXE);

	public HasteWithoutWaste(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHasteAmplifier = (isLevelOne() ? LEVEL_1_EFFECT_LVL : LEVEL_2_EFFECT_LVL) + (int) CharmManager.getLevel(mPlayer, CHARM_HASTE);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, 21, mHasteAmplifier, true, false));
			if(isEnhanced()) {
				// enhancement modifies your flat speed but that involves item stat modification
				// and/or attributes, and i'm not doing all that. so we're gonna fake it with an
				// effect instead. we need to apply multiplier M such that
				// (flat speed) * M = (flat speed + 0.02), because effects are multipliers and we need
				// to account for the fact that the player might alr have some flat speed from gear
				// gotta reinvent the wheel here though because itemstatmanager doesn't support getting
				// *just* the flat speed, only the final (0.1 + flat) * percent
				double baseSpeed = Objects.requireNonNull(mPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
				double flatSpeed = 0;
				PlayerInventory inventory = mPlayer.getInventory();
				EnumMap<Slot, ItemStack> items = new EnumMap<>(Slot.class);
				items.put(Slot.MAINHAND, inventory.getItemInMainHand());
				items.put(Slot.OFFHAND, inventory.getItemInOffHand());
				items.put(Slot.HEAD, inventory.getHelmet());
				items.put(Slot.CHEST, inventory.getChestplate());
				items.put(Slot.LEGS, inventory.getLeggings());
				items.put(Slot.FEET, inventory.getBoots());
				EnumMap<Slot, Double> stats = new EnumMap<>(Slot.class);
				for(Slot slot : items.keySet()) {
					if(items.get(slot) == null || items.get(slot).getType() == Material.AIR || items.get(slot).getAmount() == 0) continue;
					NBT.get(items.get(slot), nbt -> {
						ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);
						stats.put(slot, ItemStatUtils.getAttributeAmount(attributes, AttributeType.SPEED, Operation.ADD, slot));
					});
				}
				for(Slot slot : stats.keySet()) {
					flatSpeed += stats.get(slot);
					// why is this necessary? don't ask me, intellij is just giving me trouble for variables in lambdas
				}
				double multiplier = (baseSpeed + flatSpeed + ENHANCEMENT_SPEED) / (baseSpeed + flatSpeed) - 1;
				mPlayer.sendRawMessage(String.format("base: %s\nflat: %s\nenhance: %s\nmultiplier: %s", baseSpeed, flatSpeed, ENHANCEMENT_SPEED, multiplier));
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCEMENT_SPEED_EFFECT_NAME,
					new PercentSpeed(21, multiplier, ENHANCEMENT_SPEED_EFFECT_NAME));
			}
		}
	}
}
