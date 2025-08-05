package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OwnTempoLevelTwoCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.NavigableSet;

public class OwnTempo extends Ability {

	public static final double LEVEL_1_DAMAGE = 0.3;
	public static final int COOLDOWN = 3 * 20;
	public static final int LEVEL_2_DELAY = 2 * 20;
	public static final double LEVEL_2_DAMAGE = 0.2;
	public static final String LEVEL_2_COOLDOWN_SOURCE = "OwnTempoLevelTwoCooldown";
	public static final double ENHANCEMENT_DAMAGE = 0.15;

	public static final String CHARM_COOLDOWN = "Own Tempo Cooldown";

	public static final AbilityInfo<OwnTempo> INFO =
		new AbilityInfo<>(OwnTempo.class, "Own Tempo", OwnTempo::new)
			.linkedSpell(ClassAbility.OWN_TEMPO)
			.descriptions(
				String.format("Your next critical projectile deals %s%% extra damage. Cooldown: %ss.",
					StringUtils.multiplierToPercentage(LEVEL_1_DAMAGE),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("The first critical projectile to hit a mob in %ss deals an extra %s%% damage.",
					StringUtils.ticksToSeconds(LEVEL_2_DELAY),
					StringUtils.multiplierToPercentage(LEVEL_2_DAMAGE)
				),
				String.format("Deal an extra %s%% damage to mobs which are not targeting you or are facing away from you.",
					StringUtils.multiplierToPercentage(LEVEL_2_DAMAGE)
				)
			)
			.simpleDescription("The first shot you take in a while deals extra damage.")
			.scoreboardId("OwnTempo")
			.shorthandName("OT")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.CLOCK);

	public OwnTempo(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if(event.getType() == DamageEvent.DamageType.PROJECTILE) {
			if(!isOnCooldown()) {
				putOnCooldown();
				event.updateDamageWithMultiplier(1 + LEVEL_1_DAMAGE);
			}

			if(isLevelTwo()) {
				NavigableSet<Effect> cooldownEffects = mPlugin.mEffectManager.getEffects(enemy, LEVEL_2_COOLDOWN_SOURCE + mPlayer.getName());
				if(cooldownEffects == null) {
					event.updateDamageWithMultiplier(1 + LEVEL_2_DAMAGE);
					enemy.getWorld().playSound(enemy.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 0.3F, 1.5F);
					mPlugin.mEffectManager.addEffect(enemy, LEVEL_2_COOLDOWN_SOURCE + mPlayer.getName(), new OwnTempoLevelTwoCooldown(LEVEL_2_DELAY));
				} else {
					cooldownEffects.getFirst().setDuration(LEVEL_2_DELAY);
					// update duration instead of reapplying, to not trigger the previous instance's entityLoseEffect()
				}

			}

			if(isEnhanced() && (!EntityUtils.isInFieldOfView(enemy, mPlayer) ||
				(enemy instanceof Mob mob && !mPlayer.equals(mob.getTarget())))) {
				event.updateDamageWithMultiplier(1 + ENHANCEMENT_DAMAGE);
			}
		}
		return false;
	}
}
