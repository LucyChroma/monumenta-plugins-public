package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.*;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import org.bukkit.*;
import org.bukkit.Particle.DustTransition;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;

public class FireworkBlastOverworld extends Ability {
	public static final String ABILITY_NAME = "Firework Blast";
	private static final int COOLDOWN = 15 * 20;
	private static final int DAMAGE_VALLEY = 10;
	private static final int DAMAGE_ISLES = 15;
	private static final int DAMAGE_RING = 20;
	private static final double RADIUS = 2;
	private static final double DAMAGE_INCREASE_PER_BLOCK = 0.1;
	private static final double DAMAGE_INCREASE_MAX_DISTANCE = 15;
	private static final double FLIGHT_SPEED_1 = 0.075;
	private static final double FLIGHT_SPEED_2 = 0.15;
	private static final double LEVEL_2_DIRECT_HIT_MULTIPLIER = 1.5;
	private static final int ENHANCEMENT_FIREWORKS = 5;
	private static final double ENHANCEMENT_DAMAGE = 0.2;

	public static final String CHARM_COOLDOWN = "Firework Blast Cooldown";
	public static final String CHARM_DAMAGE = "Firework Blast Damage";
	public static final String CHARM_RADIUS = "Firework Blast Radius";
	public static final String CHARM_DAMAGE_PER_BLOCK = "Firework Blast Damage Per Block";
	public static final String CHARM_DAMAGE_INCREASE_MAX_DISTANCE = "Firework Blast Damage Scaling Max Distance";
	public static final String CHARM_FLIGHT_SPEED = "Firework Blast Flight Speed";
	public static final String CHARM_ENHANCEMENT_FIREWORKS = "Firework Blast Enhancement Extra Fireworks";
	public static final String CHARM_ENHANCEMENT_DAMAGE = "Firework Blast Enhancement Damage";

	private enum FireworkShowStatus {
		NONE,
		MID_SHOW,
		END_OF_SHOW
	}

	public static final AbilityInfo<FireworkBlastOverworld> INFO =
		new AbilityInfo<>(FireworkBlastOverworld.class, ABILITY_NAME, FireworkBlastOverworld::new)
			.linkedSpell(ClassAbility.FIREWORK_BLAST_OVERWORLD)
			.scoreboardId("FireworkBlast")
			.shorthandName("FB")
			.descriptions(
				String.format("Press drop while holding a projectile weapon to fire a slow-moving firework, " +
						"dealing (R1 %s / R2 %s / R3 %s) projectile damage in a %s block sphere when it contacts " +
						"a mob or a wall. Damage increases by %s%% per block travelled, up to a maximum of +%s%%. " +
						"The firework is affected by your Projectile Speed. Cooldown: %ss.",
					DAMAGE_VALLEY,
					DAMAGE_ISLES,
					DAMAGE_RING,
					RADIUS,
					StringUtils.multiplierToPercentage(DAMAGE_INCREASE_PER_BLOCK),
					StringUtils.multiplierToPercentage(DAMAGE_INCREASE_PER_BLOCK * DAMAGE_INCREASE_MAX_DISTANCE),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("The firework moves twice as fast. " +
						"If an enemy is directly hit, it takes %sx as much damage.",
					LEVEL_2_DIRECT_HIT_MULTIPLIER),
				String.format("A firework show is set up at the explosion location, dealing %s%% " +
						"of the initial (non-distance scaled) blast's damage %s times in the same radius.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_DAMAGE),
					ENHANCEMENT_FIREWORKS
				)
			)
			.simpleDescription("Placeholder.")
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FireworkBlastOverworld::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false), AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.FIREWORK_ROCKET);

	private final double mRingDamage;
	private final double mDamagePerBlock;
	private final double mDamageIncreaseMaxDistance;
	private final double mRadius;
	private final double mFlightSpeed;

	public FireworkBlastOverworld(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRingDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_RING);
		mDamagePerBlock = DAMAGE_INCREASE_PER_BLOCK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_PER_BLOCK);
		mDamageIncreaseMaxDistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_INCREASE_MAX_DISTANCE, DAMAGE_INCREASE_MAX_DISTANCE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mFlightSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_FLIGHT_SPEED, isLevelOne() ? FLIGHT_SPEED_1 : FLIGHT_SPEED_2);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation().add(mPlayer.getEyeLocation().getDirection());
		Vector dir = loc.getDirection();

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.25f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20).extra(0.12).spawnAsPlayerActive(mPlayer);

		Vector up = VectorUtils.rotateTargetDirection(dir, 0, 90);
		Vector right = dir.getCrossProduct(up);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, loc, 0.7).data(new DustTransition(Color.WHITE, Color.BLACK, 1.0f))
			.axes(up, right).countPerMeter(8).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, loc.clone().add(dir), 0.5).data(new DustTransition(Color.WHITE, Color.BLACK, 1.0f))
			.axes(up, right).countPerMeter(8).spawnAsPlayerActive(mPlayer);

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = loc.clone();
			final Location mStartLoc = loc.clone();
			final Vector mDir = dir.clone();
			Vector mSpiral1 = VectorUtils.rotateTargetDirection(mDir, 0, -90).multiply(0.5);
			Vector mSpiral2 = VectorUtils.rotateTargetDirection(mDir, 0, -90).multiply(0.8);
			final double playerProjSpeed = mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(AttributeType.PROJECTILE_SPEED.getItemStat());
			final double flightSpeed = mFlightSpeed * playerProjSpeed;
			@Override
			public void run() {
				for (int i = 0; i < 4; i++) {
					int extraFireworks = 0;
					if (mTicks > 3) {
						new PPPeriodic(Particle.CRIT, mLoc).count(1).delta(0.1).spawnAsPlayerActive(mPlayer);
						new PPPeriodic(Particle.CRIT, mLoc.clone().add(mSpiral1)).count(1).spawnAsPlayerActive(mPlayer);
						extraFireworks = 2;
					}
					if (mTicks > 8) {
						new PPPeriodic(Particle.SPELL_INSTANT, mLoc).count(1).delta(0.15).spawnAsPlayerActive(mPlayer);
						new PPPeriodic(Particle.SPELL_INSTANT, mLoc.clone().add(mSpiral2)).count(1).spawnAsPlayerActive(mPlayer);
						extraFireworks = 3;
					}

					Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, 0.5);
					if (!hitbox.getHitMobs().isEmpty() || LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(0.25, 0.25, 0.25), mLoc.clone().add(-0.5, -0.25, -0.25)), mLoc.getWorld(), false)) {
						explode(mLoc, extraFireworks, hitbox.getHitMobs(), FireworkShowStatus.NONE);

						if (isEnhanced()) {
							new BukkitRunnable() {
								int mFireworks = 0;
								final int mMaxFireworks = ENHANCEMENT_FIREWORKS + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCEMENT_FIREWORKS);

								@Override
								public void run(){
									mFireworks++;
									explode(mLoc, 0, null, mFireworks == mMaxFireworks ? FireworkShowStatus.END_OF_SHOW : FireworkShowStatus.MID_SHOW);
									if(mFireworks >= mMaxFireworks) {
										this.cancel();
									}
								}
							}.runTaskTimer(mPlugin, 6, 6);
						}

						this.cancel();
						break;
					}

					new PPLine(Particle.DUST_COLOR_TRANSITION, mLoc, mDir, 0.5).data(new DustTransition(Color.WHITE, Color.BLACK, 1.0f))
						.countPerMeter(4).delta(0.1).spawnAsPlayerActive(mPlayer);
					new PPPeriodic(Particle.DUST_COLOR_TRANSITION, mLoc).count(3).delta(0.05).data(new DustTransition(Color.WHITE, Color.BLACK, 1.2f)).spawnAsPlayerActive(mPlayer);
					if (i % 2 == 0) {
						Vector sparkDir = VectorUtils.randomUnitVector().add(mDir);
						new PPPeriodic(Particle.FIREWORKS_SPARK, mLoc).count(1).extra(0.2)
							.directionalMode(true).delta(sparkDir.getX(), sparkDir.getY(), sparkDir.getZ()).spawnAsPlayerActive(mPlayer);
					}

					mSpiral1 = mSpiral1.rotateAroundAxis(mDir, Math.PI / 16);
					mSpiral2 = mSpiral2.rotateAroundAxis(mDir, -Math.PI / 16);

					mLoc.add(mDir.clone().multiply(flightSpeed));
					/*
					 * TODO:
					 *  this is probably a big fucking problem with high proj speed weapons
					 *  depths firework blast has a projectile speed of 0.45
					 *  aleph/wota lv2 firework blast has a projectile speed of THREE
					 *  which will almost certainly result in phasing through mobs
					 *  maybe needs to move multiple times per tick and check the hitbox each time
					 */
				}

				if (mTicks > 100) {
					this.cancel();
				}
				mTicks++;
			}

			private void explode(Location loc, int extraFireworks, List<LivingEntity> hitMobs, FireworkShowStatus fireworkShowStatus) {
				double dist = mStartLoc.distance(loc);
				double mult = (fireworkShowStatus == FireworkShowStatus.NONE)
					? (1 + Math.min(dist, mDamageIncreaseMaxDistance) * mDamagePerBlock)
					: (ENHANCEMENT_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCEMENT_DAMAGE));
				double baseDamage = PlayerUtils.getDifferentValuePerRegion(mPlayer, DAMAGE_VALLEY, DAMAGE_ISLES, mRingDamage);
				double damage = baseDamage * mult;

				// find directly hit mob for lv2
				// TODO: honestly this could just half-become a raycast and be way better
				LivingEntity direct = null;
				if(isLevelTwo() && hitMobs != null && !hitMobs.isEmpty()) {
					// find closest mob to the explosion, which is most likely the one directly hit
					double bestDistanceSquaredFound = 999999;
					for (LivingEntity mob : hitMobs){
						double ds = loc.distanceSquared(mob.getLocation());
						if(ds < bestDistanceSquaredFound) {
							bestDistanceSquaredFound = ds;
							direct = mob;
						}
					}
				}

				for (LivingEntity mob : new Hitbox.SphereHitbox(loc, mRadius).getHitMobs()) {
					double tempDamage = damage * (mob == direct ? LEVEL_2_DIRECT_HIT_MULTIPLIER : 1);
					DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, tempDamage, ClassAbility.FIREWORK_BLAST_OVERWORLD, true, true);
					MovementUtils.knockAway(loc, mob, 0.4f);
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getEntityCenter(mob), 20).delta(0.5).data(new DustTransition(Color.WHITE, Color.BLACK, 1.2f)).spawnAsPlayerActive(mPlayer);
				}

				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2.0f, 1.2f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.5f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.75f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 2.0f, 0.7f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 2.0f, 1.0f);

				Color randomColor = List.of(Color.WHITE, Color.GRAY, Color.fromRGB(0, 0, 0)).get(FastUtils.randomIntInRange(0, 2));
				Firework rocket = (Firework) mPlayer.getWorld().spawnEntity(loc, EntityType.FIREWORK);
				FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(randomColor).build();
				FireworkMeta meta = rocket.getFireworkMeta();
				meta.addEffect(effect);
				rocket.setFireworkMeta(meta);
				rocket.detonate();

				// don't put extra fireworks if enhancement is selected unless it's the last one in the show
				// if it is the end of a show, throw in the extra fireworks regardless of distance
				if(fireworkShowStatus == FireworkShowStatus.END_OF_SHOW) extraFireworks = 3;
				if (extraFireworks > 0 && (!isEnhanced() || fireworkShowStatus == FireworkShowStatus.END_OF_SHOW)) {
					int finalExtraFireworks = extraFireworks; // make intellij stop yelling at me
					new BukkitRunnable() {
						int mExplosions = 0;
						final Location mLoc = loc.clone();

						@Override
						public void run() {
							Location loc = LocationUtils.varyInUniform(mLoc, 2.5);
							int i = 0;
							while (LocationUtils.collidesWithSolid(loc)) {
								loc = LocationUtils.varyInUniform(mLoc, 2.5);
								i++;
								if (i > 5) {
									break;
								}
							}

							Color randomColor = List.of(Color.WHITE, Color.GRAY, Color.fromRGB(0, 0, 0)).get(FastUtils.randomIntInRange(0, 2));
							Firework rocket = (Firework) mPlayer.getWorld().spawnEntity(loc, EntityType.FIREWORK);
							FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(randomColor).build();
							FireworkMeta meta = rocket.getFireworkMeta();
							meta.addEffect(effect);
							rocket.setFireworkMeta(meta);
							rocket.detonate();

							new PartialParticle(Particle.ELECTRIC_SPARK, loc, 30, 0, 0, 0, 2).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.FLASH, loc, 1).spawnAsPlayerActive(mPlayer);
							world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 1.2f);

							mExplosions++;
							if (mExplosions >= finalExtraFireworks) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 2, 2);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}
}
