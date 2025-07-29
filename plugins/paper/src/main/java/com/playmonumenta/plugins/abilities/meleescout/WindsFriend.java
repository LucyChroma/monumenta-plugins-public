package com.playmonumenta.plugins.abilities.meleescout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.MeleeScout;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class WindsFriend extends Ability {

	private static final double DAMAGE_REDUCTION = 0.5;
	private static final EnumSet<DamageEvent.DamageType> REDUCED_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.FIRE,
		DamageEvent.DamageType.FALL
	);

	public static final AbilityInfo<WindsFriend> INFO =
		new AbilityInfo<>(WindsFriend.class, "Wind's Friend", WindsFriend::new)
			.canUse(player -> AbilityUtils.getClassNum(player) == MeleeScout.CLASS_ID)
			.priorityAmount(6000);

	public WindsFriend(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if(REDUCED_DAMAGE_TYPES.contains(event.getType())) {
			event.setFlatDamage(event.getFlatDamage() * (1 - DAMAGE_REDUCTION));
		}
	}
}
