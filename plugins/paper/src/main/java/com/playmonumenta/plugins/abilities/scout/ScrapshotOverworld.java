package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ScrapshotOverworld extends Ability {

	public static final AbilityInfo<ScrapshotOverworld> INFO =
		new AbilityInfo<>(ScrapshotOverworld.class, "Placeholder Ability", ScrapshotOverworld::new)
			.linkedSpell(null)
			.descriptions(
				String.format("Placeholder ability level %s.",
					1),
				String.format("Placeholder ability level %s.",
					2),
				String.format("Placeholder ability enhancement. :%s",
					3))
			.simpleDescription("Placeholder.")
			.displayItem(Material.DIRT);

	public ScrapshotOverworld(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		return false;
	}
}
