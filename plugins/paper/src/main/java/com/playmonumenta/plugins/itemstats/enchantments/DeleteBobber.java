package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class DeleteBobber implements Enchantment {

	@Override
	public String getName() {
		return "DeleteBobber";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DELETE_BOBBER;
	}
}
