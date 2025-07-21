package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DaggerThrowCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
			.displayItem(Material.DIRT);

	public PlaceholderAbility(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		return false;
	}
}
