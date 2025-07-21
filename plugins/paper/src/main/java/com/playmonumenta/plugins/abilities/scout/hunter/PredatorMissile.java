package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PredatorMissile extends Ability {
	private static final int COOLDOWN_1 = 16 * 20;
	private static final int COOLDOWN_2 = 12 * 20;
	private static final int DELAY_1 = 2 * 20;
	private static final int DELAY_2 = 1 * 20;
	private static final String PREDATOR_MISSILE_PROJECTILE_TAG = "PredatorMissileProjectile";

	public static final String CHARM_COOLDOWN = "Predator Missile Cooldown";

	private boolean mPrimed;
	private @Nullable BukkitRunnable mRunnable;
	private @Nullable ProjectileType mType;

	private enum ProjectileType {
		ARROW,
		TRIDENT,
		SNOWBALL
	}

	public PredatorMissile(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static final AbilityInfo<PredatorMissile> INFO = new AbilityInfo<>(PredatorMissile.class, "Predator Missile", PredatorMissile::new)
		.linkedSpell(ClassAbility.PREDATOR_MISSILE)
		.scoreboardId("PredatorMissile")
		.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
		.addTrigger(new AbilityTriggerInfo<>("prime", "prime", PredatorMissile::prime,
			new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
		.descriptions("Description level 1", "Description level 2")
		.shorthandName("PrM")
		.simpleDescription("Call down a barrage on your foe. Effects vary by weapon type.");

	public boolean prime(){
		if(isOnCooldown()) return false;
		mPrimed = !mPrimed;
		mPlayer.sendActionBar(Component.text("Predator Missile is "+(mPrimed ? "now" : "no longer")+" primed", INFO.getActionBarColor()));
		return true;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile){
		if (!mPrimed || !EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			return true;
		}

		new BukkitRunnable() {
			public void run(){
				putOnModifiedCooldown();
			}
		}.runTask(mPlugin);
		ScoreboardUtils.toggleTag(projectile, PREDATOR_MISSILE_PROJECTILE_TAG);
		projectile.setFireTicks(9999);
		return true;
	}

	public void putOnModifiedCooldown(){
		putOnCooldown(getModifiedCooldown()-1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		if (event.getType() == DamageEvent.DamageType.PROJECTILE && proj instanceof Projectile && ScoreboardUtils.checkTag(proj, PREDATOR_MISSILE_PROJECTILE_TAG)) {
			mType = proj instanceof Snowball ? ProjectileType.SNOWBALL :
				proj instanceof Trident ? ProjectileType.TRIDENT :
					ProjectileType.ARROW; // arrow last bc spectralarrow doesnt extend arrow
			mRunnable = new BukkitRunnable() {
				int mTicks = 0;
				final LivingEntity mEntity = enemy;
				final ProjectileType mTempType = mType;

				@Override
				public void run() {
					switch (mTempType) {
						case TRIDENT -> {
							if(mEntity.isDead() || !mEntity.isValid()) this.cancel();
							// do damage
							this.cancel();
						}
						case SNOWBALL -> {

						}
						case ARROW -> {
							// shrug
						}
					}
					mTicks++;
				}
			};
			if(mType == ProjectileType.TRIDENT) {
				cancelOnDeath(mRunnable.runTaskTimer(mPlugin, isLevelOne() ? DELAY_1 : DELAY_2, 1));
			} else {
				mRunnable.runTaskTimer(mPlugin, isLevelOne() ? DELAY_1 : DELAY_2, 1);
			}
		}
		return false;
	}

	@Override
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		if (mType == ProjectileType.TRIDENT && mRunnable != null) {
			mRunnable.cancel();
		}
	}
}
