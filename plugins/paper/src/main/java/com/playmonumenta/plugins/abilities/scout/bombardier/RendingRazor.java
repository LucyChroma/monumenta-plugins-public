package com.playmonumenta.plugins.abilities.scout.bombardier;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class RendingRazor extends Ability {

	public static final AbilityInfo<RendingRazor> INFO =
		new AbilityInfo<>(RendingRazor.class, "Rending Razor", RendingRazor::new)
			.linkedSpell(ClassAbility.RENDING_RAZOR)
			.scoreboardId("RendingRazor")
			.descriptions(
				String.format("Placeholder ability level %s.",
					1),
				String.format("Placeholder ability level %s.",
					2),
				String.format("Placeholder ability enhancement. :%s",
					3))
			.simpleDescription("Placeholder.")
			.displayItem(Material.SHEARS);

	public RendingRazor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		mPlayer.sendRawMessage("Rending Razor cast! (placeholder)");
		return false;
	}
}
