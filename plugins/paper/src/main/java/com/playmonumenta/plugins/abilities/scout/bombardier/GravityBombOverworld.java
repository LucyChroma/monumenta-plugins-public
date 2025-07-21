package com.playmonumenta.plugins.abilities.scout.bombardier;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class GravityBombOverworld extends Ability {

	public static final AbilityInfo<GravityBombOverworld> INFO =
		new AbilityInfo<>(GravityBombOverworld.class, "Gravity Bomb", GravityBombOverworld::new)
			.linkedSpell(ClassAbility.GRAVITY_BOMB_OVERWORLD)
			.scoreboardId("GravityBomb")
			.descriptions(
				String.format("Placeholder ability level %s.",
					1),
				String.format("Placeholder ability level %s.",
					2),
				String.format("Placeholder ability enhancement. :%s",
					3))
			.simpleDescription("Placeholder.")
			.displayItem(Material.GRAY_GLAZED_TERRACOTTA);

	public GravityBombOverworld(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		mPlayer.sendRawMessage("Gravity Bomb cast! (placeholder)");
		return false;
	}
}
