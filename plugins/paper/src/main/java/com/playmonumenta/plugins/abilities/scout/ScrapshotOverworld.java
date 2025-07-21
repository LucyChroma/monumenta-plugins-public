package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.*;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.*;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class ScrapshotOverworld extends Ability {
	public static final String ABILITY_NAME = "Scrapshot";
	private static final int COOLDOWN = 15 * 20;
	private static final int DAMAGE_VALLEY = 20;
	private static final int DAMAGE_ISLES = 30;
	private static final int DAMAGE_RING = 40;
	private static final double RECOIL_VELOCITY = 1;
	private static final int RANGE = 8;
	private static final double SHRAPNEL_DAMAGE_PERCENT = 0.5;
	private static final double SHRAPNEL_RANGE = 4;
	private static final double SHRAPNEL_CONE_ANGLE = 50;
	private static final int ENHANCEMENT_DURATION = 2 * 20;

	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(140, 140, 140), 1.0f);
	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(100, 100, 100), 1.0f);

	public static final String CHARM_COOLDOWN = "Scrapshot Cooldown";
	public static final String CHARM_DAMAGE = "Scrapshot Damage";
	public static final String CHARM_RANGE = "Scrapshot Range";
	public static final String CHARM_SHRAPNEL_CONE_ANGLE = "Scrapshot Shrapnel Cone Angle";
	public static final String CHARM_SHRAPNEL_RANGE = "Scrapshot Shrapnel Range";
	public static final String CHARM_RECOIL_VELOCITY = "Scrapshot Recoil Velocity";

	public static final AbilityInfo<ScrapshotOverworld> INFO =
		new AbilityInfo<>(ScrapshotOverworld.class, ABILITY_NAME, ScrapshotOverworld::new)
			.linkedSpell(ClassAbility.SCRAPSHOT_OVERWORLD)
			.scoreboardId("Scrapshot")
			.shorthandName("Scr")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ScrapshotOverworld::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true), AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.NETHERITE_SCRAP)
			.simpleDescription("Fire a short-range blast to hit enemies and make a quick escape.")
			.descriptions(
				String.format("Left click while sneaking and holding a projectile weapon to fire a blunderbuss shot " +
					"that goes up to %s blocks away, dealing (R1 %s / R2 %s / R3 %s) projectile damage to the first mob " +
					"and knocking you backwards if it hits a mob. Damage is decreased based on distance if the distance is " +
					"greater than %s%% of the max range. Cooldown: %ss.",
					RANGE,
					DAMAGE_VALLEY,
					DAMAGE_ISLES,
					DAMAGE_RING,
					50,
					StringUtils.ticksToSeconds(COOLDOWN)),
				String.format("The shot splits into shrapnel after hitting a mob and deals %s%% of the initial damage dealt " +
					"to all mobs in a %s block cone behind the target.",
					(int) (100 * SHRAPNEL_DAMAGE_PERCENT),
					SHRAPNEL_RANGE),
				String.format("The shot is now a flashbang, stunning all hit mobs for %ss.",
					StringUtils.ticksToSeconds(ENHANCEMENT_DURATION))
			);

	private final double mRingDamage;
	private final double mRange;
	private final double mShrapnelDamagePercent;
	private final double mShrapnelRange;
	private final double mShrapnelConeAngle;

	public ScrapshotOverworld(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRingDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_RING);
		mRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RANGE);
		mShrapnelDamagePercent = SHRAPNEL_DAMAGE_PERCENT;
		mShrapnelRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SHRAPNEL_RANGE, SHRAPNEL_RANGE);
		mShrapnelConeAngle = Math.min(180, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SHRAPNEL_CONE_ANGLE, SHRAPNEL_CONE_ANGLE));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		World world = mPlayer.getWorld();

		RayTraceResult result = world.rayTrace(loc, dir, mRange, FluidCollisionMode.NEVER, true, 0.5,
			e -> e instanceof LivingEntity && EntityUtils.isHostileMob(e) && !e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		Location endLoc;
		LivingEntity target = null;
		if (result == null) {
			endLoc = loc.clone().add(dir.clone().multiply(mRange));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
			if (result.getHitEntity() instanceof LivingEntity le) {
				target = le;
			}
		}

		double mult = 0;
		double baseDamage = ServerProperties.getAbilityEnhancementsEnabled(mPlayer) ? mRingDamage :
			ServerProperties.getClassSpecializationsEnabled(mPlayer) ? DAMAGE_ISLES :
				DAMAGE_VALLEY;
		if (target != null) {
			double dist = endLoc.distance(loc);
			mult = Math.min(1, (mRange * 1.5 - dist) / mRange);
			double damage = mult * baseDamage;
			DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.PROJECTILE_SKILL, damage, mInfo.getLinkedSpell(), true, true);
			if(isEnhanced()) { EntityUtils.applyStun(mPlugin, ENHANCEMENT_DURATION, target); }

			new PartialParticle(Particle.SQUID_INK, target.getLocation(), (int) ((mRange - dist) * 2), 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
			world.playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0);

			new PartialParticle(Particle.EXPLOSION_LARGE, endLoc, 1).spawnAsPlayerActive(mPlayer);

			// shrapnel cone
			if (isLevelTwo()) {
				Location shrapnelOrigin = LocationUtils.getHalfHeightLocation(target).add(0, -mShrapnelRange, 0).setDirection(dir);
				Hitbox hitbox = Hitbox.approximateCylinderSegment(shrapnelOrigin, 2 * mShrapnelRange, mShrapnelRange, Math.toRadians(mShrapnelConeAngle));
				for (LivingEntity mob : hitbox.getHitMobs()) {
					if (mob == target) { // don't hit the target again
						continue;
					}
					DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.PROJECTILE_SKILL, damage * mShrapnelDamagePercent, mInfo.getLinkedSpell(), true, true);
					if(isEnhanced()) { EntityUtils.applyStun(mPlugin, ENHANCEMENT_DURATION, mob); }
				}

				Location targetLocation = LocationUtils.getHalfHeightLocation(target);
				double degree = 90 - mShrapnelConeAngle;
				int degreeSteps = ((int) (2 * mShrapnelConeAngle)) / 12;
				double degreeStep = 2 * mShrapnelConeAngle / degreeSteps;
				for (int step = 0; step < degreeSteps + 1; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					Vector vec = new Vector(FastUtils.cos(radian1) * mShrapnelRange, 0, FastUtils.sin(radian1) * mShrapnelRange);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw()); // rotate to match the shot direction
					Location shrapnelEnd = targetLocation.clone().add(vec);

					new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(target), shrapnelEnd).countPerMeter(2).delta(0.1).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(target), shrapnelEnd).countPerMeter(2).delta(0.1).data(DARK_COLOR).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.SMOKE_NORMAL, LocationUtils.getHalfHeightLocation(target), shrapnelEnd).countPerMeter(4).delta(0.15).spawnAsPlayerActive(mPlayer);
				}
			}
		}

		Vector velocity = mPlayer.getLocation().getDirection().multiply(-CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RECOIL_VELOCITY, RECOIL_VELOCITY));
		mPlayer.setVelocity(velocity.setY(Math.max(0.1, velocity.getY())));

		Location startLoc = loc.clone().add(dir);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, startLoc, 0, 0.3, 0, 90, 30, 0.5f, true, 0, 1.5, Particle.SMOKE_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, startLoc, 0, 0.3, 0, 90, 40, 0.75f, true, 0, 2, Particle.SMOKE_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, startLoc, 0, 0.3, 0, 90, 50, 1.0f, true, 0, 2.5, Particle.SMOKE_NORMAL);
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).countPerMeter(7).delta(0.1).extra(0.05).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).countPerMeter(4).delta(0.25).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).countPerMeter(4).delta(0.25).data(DARK_COLOR).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1.4f);

		if (mult > 0.75) { // extra sounds for close-range high damage hits
			world.playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 1f, 1.2f);
			world.playSound(loc, Sound.BLOCK_IRON_DOOR_CLOSE, SoundCategory.PLAYERS, 1f, 0.65f);
			world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1f, 1.2f);
		}

		return true;
	}
}
