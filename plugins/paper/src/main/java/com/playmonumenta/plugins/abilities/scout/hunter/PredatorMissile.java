package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PredatorMissile extends Ability {
	private static final int COOLDOWN_1 = 16 * 20;
	private static final int COOLDOWN_2 = 12 * 20;
	private static final int DELAY_1 = 2 * 20;
	private static final int DELAY_2 = 1 * 20;
	private static final String PREDATOR_MISSILE_PROJECTILE_TAG = "PredatorMissileProjectile";

	private static final double ARROW_DAMAGE_MULTIPLIER = 0.5;
	private static final double ARROW_MISSILES = 5;
	private static final int ARROW_MISSILE_DELAY = 10;
	private static final int ARROW_MISSILE_MAX_LIFETIME = 10 * 20;
	private static final double ARROW_MISSILE_EXPLOSION_RADIUS = 1.5;

	private static final double TRIDENT_DAMAGE_MULTIPLIER = 1.5;
	private static final double TRIDENT_LINE_WIDTH = 3;

	private static final int SNOWBALL_SLOW_DURATION = 2 * 20;
	private static final double SNOWBALL_SLOW_MULTIPLIER = 0.2;
	private static final double SNOWBALL_DAMAGE_MULTIPLIER = 0.3;
	private static final double SNOWBALL_STORM_TICKS = 10;
	private static final int SNOWBALL_STORM_TICK_DELAY = 10;
	private static final double SNOWBALL_STORM_RADIUS = 5;

	public static final String CHARM_COOLDOWN = "Predator Missile Cooldown";

	private boolean mPrimed;
	private @Nullable BukkitRunnable mMainRunnable;
	private @Nullable BukkitRunnable mBarrageRunnable;
	private ProjectileType mType;

	private enum ProjectileType {
		ARROW(true),
		TRIDENT(true),
		SNOWBALL(false),
		NONE(false);

		final boolean cancelOnTeleport;

		ProjectileType(boolean cancelOnTeleport) {
			this.cancelOnTeleport = cancelOnTeleport;
		}
	}

	public PredatorMissile(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mType = ProjectileType.NONE;
	}

	public static final AbilityInfo<PredatorMissile> INFO = new AbilityInfo<>(PredatorMissile.class, "Predator Missile", PredatorMissile::new)
		.linkedSpell(ClassAbility.PREDATOR_MISSILE)
		.scoreboardId("PredatorMissile")
		.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
		.addTrigger(new AbilityTriggerInfo<>("prime", "prime", PredatorMissile::prime,
			new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
		.descriptions("Description level 1", "Description level 2")
		.shorthandName("PrM")
		.simpleDescription("Call down a barrage on your foe. Effects vary by weapon type.")
		.displayItem(Material.SPECTRAL_ARROW)
		.priorityAmount(5100); // needs to get final dmg dealt to apply percentages of it later

	public boolean prime(){
		if(isOnCooldown()) return false;
		mPrimed = !mPrimed;
		mPlayer.sendActionBar(Component.text("Predator Missile is "+(mPrimed ? "now" : "no longer")+" primed", INFO.getActionBarColor()));
		return true;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile){
		if (!mPrimed || !EntityUtils.isAbilityTriggeringProjectile(projectile, true) || AbilityUtils.isVolley(mPlayer, projectile)) {
			return true;
		}

		putOnModifiedCooldown(mPlayer.getEquipment().getItemInMainHand().getType(), mPlayer.getEquipment().getItemInMainHand().getEnchantmentLevel(Enchantment.QUICK_CHARGE));
		// unprime a tick later bc multiple flares can be shot in a tick with multishot
		Bukkit.getScheduler().runTask(mPlugin, () -> mPrimed = false);
		ScoreboardUtils.toggleTag(projectile, PREDATOR_MISSILE_PROJECTILE_TAG);
		projectile.setFireTicks(9999);
		return true;
	}

	private void putOnModifiedCooldown(Material mainhandItemType, int quickChargeLevels){
		int modifiedCooldown = getModifiedCooldown();
		if (mainhandItemType == Material.CROSSBOW) {
			modifiedCooldown = (int) (modifiedCooldown * (1.25 - 0.25 * quickChargeLevels));
		}
		putOnCooldown(modifiedCooldown);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		double amount = event.getFlatDamage();
		if (event.getType() == DamageEvent.DamageType.PROJECTILE && proj instanceof Projectile && ScoreboardUtils.checkTag(proj, PREDATOR_MISSILE_PROJECTILE_TAG)) {
			mType = proj instanceof Snowball ? ProjectileType.SNOWBALL :
				proj instanceof Trident ? ProjectileType.TRIDENT :
					ProjectileType.ARROW; // arrow last bc spectralarrow doesnt extend arrow
			mMainRunnable = new BukkitRunnable() {
				final LivingEntity mEntity = enemy;
				final ProjectileType mTempType = mType;
				final double mAmount = amount;

				@Override
				public void run() {
					switch (mTempType) {
						case TRIDENT -> trident(mEntity, mAmount);
						case SNOWBALL -> snowball(mEntity, mAmount);
						case ARROW -> arrow(mEntity, mAmount);
						default -> {}
					}
				}
			};
			if(mType == ProjectileType.TRIDENT) {
				cancelOnDeath(mMainRunnable.runTaskLater(mPlugin, isLevelOne() ? DELAY_1 : DELAY_2));
			} else {
				mMainRunnable.runTaskLater(mPlugin, isLevelOne() ? DELAY_1 : DELAY_2);
			}
		}
		return false;
	}

	@Override
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		if (mType.cancelOnTeleport) {
			if(mMainRunnable != null) mMainRunnable.cancel();
			if(mBarrageRunnable != null) mBarrageRunnable.cancel();
		}
	}

	private void arrow(LivingEntity entity, double originalDamage){
		mBarrageRunnable = new BukkitRunnable() {
			int mTimesFired = 0;
			final LivingEntity mEntity = entity;

			@Override
			public void run(){
				if(mTimesFired >= ARROW_MISSILES || entity.isDead() || !entity.isValid()) {
					this.cancel();
					return;
				}
				fireMissile(mEntity, originalDamage);
				// mPlayer.sendRawMessage("missile fired");
				// DamageUtils.damage(mPlayer, entity, DamageEvent.DamageType.TRUE, originalDamage * ARROW_DAMAGE_MULTIPLIER, mInfo.getLinkedSpell(), true, true);
				mTimesFired++;
			}
		};
		cancelOnDeath(mBarrageRunnable.runTaskTimer(mPlugin, 0, ARROW_MISSILE_DELAY));
	}

	private void fireMissile(LivingEntity entity, double originalDamage){
		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = mPlayer.getEyeLocation();
			final Vector dir = LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(entity), mPlayer.getEyeLocation());

			@Override
			public void run(){
				if(mTicks >= ARROW_MISSILE_MAX_LIFETIME || !mLoc.isChunkLoaded()) {
					this.cancel();
					return;
				}
				mLoc.add(dir.clone().multiply(1.2));
				Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, 0.5);
				if (!hitbox.getHitMobs().isEmpty() || LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(0.25, 0.25, 0.25), mLoc.clone().add(-0.25, -0.25, -0.25)), mLoc.getWorld(), false)) {
					hitbox = new Hitbox.SphereHitbox(mLoc, ARROW_MISSILE_EXPLOSION_RADIUS);
					for(LivingEntity mob : hitbox.getHitMobs()){
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.TRUE, originalDamage * ARROW_DAMAGE_MULTIPLIER, ClassAbility.PREDATOR_MISSILE, false, true);
						mLoc.getWorld().playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 0.7f, 1234L);
						ParticleUtils.spawnParticleAsLivingEntity(new PartialParticle(Particle.EXPLOSION_HUGE, mLoc), mPlayer);
					}
					this.cancel();
					return;
				}
				ParticleUtils.spawnParticleAsLivingEntity(new PartialParticle(Particle.FLAME, mLoc), mPlayer);
				if(mTicks % 5 == 0) {
					if(mTicks != 0) ParticleUtils.spawnParticleAsLivingEntity(new PartialParticle(Particle.EXPLOSION_LARGE, mLoc), mPlayer);
					mLoc.getWorld().playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.3f, 1.7f, 727L);
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void trident(LivingEntity entity, double originalDamage){
		if(entity.isDead() || !entity.isValid()) return;
		Location startLoc = mPlayer.getEyeLocation();
		Location endLoc = LocationUtils.getHalfHeightLocation(entity);
		List<LivingEntity> targetMobs = EntityUtils.getMobsInLine(startLoc, endLoc, TRIDENT_LINE_WIDTH / 2);
		for (LivingEntity mob : targetMobs) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.TRUE, originalDamage * TRIDENT_DAMAGE_MULTIPLIER, mInfo.getLinkedSpell(), true, true);
		}
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).countPerMeter(20).delta(0.15).extra(0.075).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.FLAME, startLoc, endLoc).countPerMeter(4).delta(0.2).extra(0.1).spawnAsPlayerActive(mPlayer);
		World world = mPlayer.getWorld();
		world.playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(startLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(startLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(startLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);

	}

	private void snowball(LivingEntity entity, double originalDamage) {
		new BukkitRunnable() {
			int mTimesDamaged = 0;
			final Location mLoc = LocationUtils.getHalfHeightLocation(entity);

			@Override
			public void run(){
				if(mTimesDamaged >= SNOWBALL_STORM_TICKS) {
					this.cancel();
					return;
				}
				Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, SNOWBALL_STORM_RADIUS);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					EntityUtils.applySlow(mPlugin, SNOWBALL_SLOW_DURATION, SNOWBALL_SLOW_MULTIPLIER, mob);
					DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.TRUE, originalDamage * SNOWBALL_DAMAGE_MULTIPLIER, mInfo.getLinkedSpell(), true, false);

					if (mob.getFireTicks() > 1) {
						mob.setFireTicks(1);
					}
				}
				new PPCircle(Particle.SNOWBALL, mLoc, SNOWBALL_STORM_RADIUS).count(160).extra(0.65).ringMode(false).spawnAsPlayerActive(mPlayer);
				World world = mPlayer.getWorld();
				world.playSound(mLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.7f);
				world.playSound(mLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.1f, 0.7f);
				world.playSound(mLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.2f, 2.0f);
				world.playSound(mLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 2.0f);
				mTimesDamaged++;
			}
		}.runTaskTimer(mPlugin, 0, SNOWBALL_STORM_TICK_DELAY);
	}
}
