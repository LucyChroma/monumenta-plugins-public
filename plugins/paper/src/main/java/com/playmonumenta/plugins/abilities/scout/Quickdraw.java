package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.QuickdrawCS;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.*;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 20 * 6;
	private static final int QUICKDRAW_2_COOLDOWN = 20 * 3;
	private static final int DAMAGE_VALLEY = 11;
	private static final int DAMAGE_ISLES = 17;
	private static final int DAMAGE_RING = 22;

	public static final String CHARM_DAMAGE = "Quickdraw Damage";
	public static final String CHARM_COOLDOWN = "Quickdraw Cooldown";
	public static final String CHARM_PIERCING = "Quickdraw Piercing";

	public static final AbilityInfo<Quickdraw> INFO =
		new AbilityInfo<>(Quickdraw.class, "Quickdraw", Quickdraw::new)
			.linkedSpell(ClassAbility.QUICKDRAW)
			.scoreboardId("Quickdraw")
			.shorthandName("Qd")
			.descriptions(
				String.format("Left-clicking with a projectile weapon instantly fires that projectile, fully charged. " +
					"This skill can only apply Recoil once before touching the ground. Cooldown: %ds.", QUICKDRAW_1_COOLDOWN / 20),
				String.format("Cooldown: %ds.", QUICKDRAW_2_COOLDOWN / 20),
				"Arrows shot with this skill are given +1 piercing."
			)
			.simpleDescription("Instantly fire the held projectile weapon.")
			.cooldown(QUICKDRAW_1_COOLDOWN, QUICKDRAW_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Quickdraw::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.BLAZE_POWDER);

	public @Nullable Projectile mProjectile;
	private final QuickdrawCS mCosmetic;

	public Quickdraw(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new QuickdrawCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if ((ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.TWO_HANDED)
			&& !(ItemUtils.isNullOrAir(inOffHand) || ItemStatUtils.hasEnchantment(inOffHand, EnchantmentType.WEIGHTLESS)))
			|| ItemUtils.isShootableItem(inOffHand)) {
			return false;
		}
		mCosmetic.quickdrawCast(mPlayer);

		boolean launched = shootProjectile(inMainHand, 0);
		if (launched) {
			putOnCooldown();

			if (ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.MULTISHOT) > 0) {
				for (int i = 0; i < 2; i++) {
					shootProjectile(inMainHand, 2 * i - 1);
				}
			}
			return true;
		}
		return false;
	}

	private boolean shootProjectile(ItemStack inMainHand, int deviation) {
		Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			Location l = mPlayer.getLocation();
			l.setPitch(l.getPitch() - 90);
			direction.rotateAroundNonUnitAxis(l.getDirection(), deviation * 10.0 * Math.PI / 180);
		}

		World world = mPlayer.getWorld();
		Location eyeLoc = mPlayer.getEyeLocation();
		Projectile proj;
		switch (inMainHand.getType()) {
			case BOW, CROSSBOW -> proj = world.spawnArrow(eyeLoc, direction, 3.0f, 0, Arrow.class);
			case TRIDENT -> proj = world.spawnArrow(eyeLoc, direction, 2.5f, 0, Trident.class);
			case SNOWBALL -> {
				Snowball snowball = world.spawn(eyeLoc, Snowball.class);
				ItemUtils.setSnowballItem(snowball, inMainHand);
				proj = snowball;
				proj.setVelocity(direction.normalize().multiply(3.0f));
			}
			case EGG -> {
				Egg egg = world.spawn(eyeLoc, Egg.class);
				ItemUtils.setSnowballItem(egg, inMainHand);
				proj = egg;
				proj.setVelocity(direction.normalize().multiply(3.0f));
			}
			case EXPERIENCE_BOTTLE -> {
				ThrownExpBottle thrownExpBottle = world.spawn(eyeLoc, ThrownExpBottle.class);
				ItemUtils.setSnowballItem(thrownExpBottle, inMainHand);
				proj = thrownExpBottle;
				proj.setVelocity(direction.normalize().multiply(3.0f));
			}
			case ENDER_PEARL -> {
				EnderPearl enderPearl = world.spawn(eyeLoc, EnderPearl.class);
				ItemUtils.setSnowballItem(enderPearl, inMainHand);
				proj = enderPearl;
				proj.setVelocity(direction.normalize().multiply(3.0f));
			}
			case FISHING_ROD -> {
//				mPlayer.sendRawMessage("fishdraw");
//
//				FishHook hook = (FishHook) NmsUtils.getVersionAdapter();
//				proj = hook;
//				proj.setVelocity(direction.normalize().multiply(3.0f));
				proj = world.spawnArrow(eyeLoc, direction, 3.0f, 0, Arrow.class);
			}
			default -> {
				if (ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.THROWING_KNIFE)) {
					proj = world.spawnArrow(eyeLoc, direction, 3f, 0, Arrow.class);
				} else {
					// How did we get here?
					mPlayer.sendRawMessage(inMainHand.toString());
					return false;
				}
			}
		}
		mProjectile = proj;

		if (!mPlayer.isSneaking() && ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.RECOIL) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				proj.addScoreboardTag("NoRecoil");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}
		if (!mPlayer.isSneaking() && ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.GRAPPLING) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				proj.addScoreboardTag("NoGrapple");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
			proj.addScoreboardTag("SourceQuickDraw");
			new BukkitRunnable() {
				@Override
				public void run() {
					mPlayer.updateInventory();
				}
			}.runTaskLater(mPlugin, 1);
		}

		proj.setShooter(mPlayer);
		if (proj instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(Math.max(0, Math.min((isEnhanced() ? 1 : 0) + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING), 127)));
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(proj);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		if (!eventLaunch.isCancelled()) {
			mCosmetic.quickdrawProjectileEffect(mPlugin, proj);
		}

		ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(proj);
		if (stats != null) {
			ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
			if (map != null) {
				ItemStat projDamageAdd = Objects.requireNonNull(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
				double damage = PlayerUtils.getDifferentValuePerRegion(mPlayer, DAMAGE_VALLEY, DAMAGE_ISLES, DAMAGE_RING);
				map.set(projDamageAdd, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage));
			}
		}

		return !eventLaunch.isCancelled() || proj instanceof Trident;
	}

	public boolean isQuickDraw(Projectile projectile) {
		return projectile == mProjectile;
	}
}
