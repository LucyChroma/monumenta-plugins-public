package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class NoTeleport implements Enchantment {

	@Override
	public String getName() {
		return "NoTeleport";
	}

	@Override
	public EnchantmentType getEnchantmentType() {

		return EnchantmentType.NO_TELEPORT;
	}
}
