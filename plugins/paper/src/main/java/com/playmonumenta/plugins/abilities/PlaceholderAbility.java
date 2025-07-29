package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlaceholderAbility extends Ability {

	public static final AbilityInfo<PlaceholderAbility> INFO =
		new AbilityInfo<>(PlaceholderAbility.class, "Placeholder Ability", PlaceholderAbility::new)
			.linkedSpell(null)
			.descriptions(
				String.format("Placeholder ability level %s.",
					1),
				String.format("Placeholder ability level %s.",
					2),
				String.format("Placeholder ability enhancement. :%s",
					3))
			.simpleDescription("Placeholder.")
			.scoreboardId("PlaceholderAbility")
			.displayItem(Material.DIRT);

	public PlaceholderAbility(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		return false;
	}
}
