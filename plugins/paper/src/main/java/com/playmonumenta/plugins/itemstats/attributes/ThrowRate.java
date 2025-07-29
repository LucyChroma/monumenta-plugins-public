package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.Quickdraw;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enchantments.Oversized;
import com.playmonumenta.plugins.itemstats.enchantments.Snowy;
import com.playmonumenta.plugins.itemstats.enchantments.TwoHanded;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.player.EnderPearlTracker;
import com.playmonumenta.plugins.utils.*;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class ThrowRate implements Attribute {

	@Override
	public String getName() {
		return "Throw Rate";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.THROW_RATE;
	}

	final HashMap<EntityType, Sound> THROWABLE_MATERIALS = new HashMap<>();

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile proj) {
		Quickdraw quickdraw = AbilityManager.getManager().getPlayerAbility(player, Quickdraw.class);
		boolean isQuickdraw = quickdraw != null && quickdraw.isQuickDraw(proj);

		if (isQuickdraw) {
			return;
		}

		int cooldown = (int) (20 / value);

		if (proj instanceof Trident trident) {
			ItemStack item = trident.getItemStack();

			//Check for Two Handed Curse.
			if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TWO_HANDED) > 0) {
				if (TwoHanded.checkForOffhand(plugin, player)) {
					return;
				}
			}

			// Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0 && value > 0) {
				event.setCancelled(true);
				// If a trident made from the volley skill, don't run sound/unbreaking
				boolean isVolley = AbilityUtils.isVolley(player, trident);
				if (isVolley) {
					return;
				}

				// Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant

				player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1);
				player.setCooldown(item.getType(), cooldown);
				new BukkitRunnable() {
					@Override
					public void run() {
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2.0f, 1);
					}
				}.runTaskLater(plugin, cooldown);

				// Duplicate the entity, then cancel the throw event so the trident doesn't leave inventory
				Trident newProj = NmsUtils.getVersionAdapter().duplicateEntity(trident);

				// Set a bunch of stuff that isn't caught by the entity duplication
				newProj.setShooter(player);
				DamageListener.addProjectileItemStats(newProj, player);

				newProj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

				ItemUtils.damageItemWithUnbreaking(plugin, player, player.getInventory().getItemInMainHand(), 1, true);

				AbilityManager.getManager().playerShotProjectileEvent(player, newProj);
			} else {
				return;
			}
		} else if (proj instanceof ThrowableProjectile throwableProjectile) {
			if (value > 0) {

				THROWABLE_MATERIALS.put(EntityType.SNOWBALL, Sound.ENTITY_SNOWBALL_THROW);
				THROWABLE_MATERIALS.put(EntityType.EGG, Sound.ENTITY_EGG_THROW);
				THROWABLE_MATERIALS.put(EntityType.THROWN_EXP_BOTTLE, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW);
				THROWABLE_MATERIALS.put(EntityType.ENDER_PEARL, Sound.ENTITY_ENDER_PEARL_THROW);

				// If a snowball made from the volley skill, don't run sound/unbreaking
				boolean isVolley = AbilityUtils.isVolley(player, throwableProjectile);
				if (isVolley) {
					return;
				}
				// if event is cancelled (either from TwoHanded or Snowy), do not create projectile
				if (event.isCancelled()) {
					return;
				}

				ThrowableProjectile thrown = (ThrowableProjectile) player.getWorld().spawnEntity(proj.getLocation(), throwableProjectile.getType());
				thrown.setShooter(player);
				thrown.setVelocity(proj.getVelocity());
				DamageListener.addProjectileItemStats(thrown, player);
				ItemUtils.setSnowballItem(thrown, throwableProjectile.getItem());

				Snowy.transferProjectileMode(throwableProjectile, thrown);

				player.playSound(player.getLocation(), THROWABLE_MATERIALS.get(thrown.getType()), SoundCategory.PLAYERS, 0.5f, 0.5f);
				AbilityManager.getManager().playerShotProjectileEvent(player, thrown);
				if (thrown.getType() == EntityType.ENDER_PEARL && DamageListener.getProjectileItemStats(thrown).getItemStats().get(EnchantmentType.NO_TELEPORT) == 0
					&& !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.DISABLE_MAGIC_TESS)) {
					EnderPearlTracker.startTracking(player, (EnderPearl) thrown);
				}

				player.setCooldown(throwableProjectile.getItem().getType(), cooldown);
				event.setCancelled(true);
				// For clearing weapon snowballs after 10s (to prevent being stuck in bubble columns):
				EntityListener.clearEntityLater(thrown);
			} else {
				return;
			}
		} else if (proj instanceof FishHook hook) {
			if (value > 0) {
				// If a snowball made from the volley skill, don't run sound/unbreaking
				boolean isVolley = AbilityUtils.isVolley(player, hook);
				if (isVolley) {
					return;
				}
				// if event is cancelled (either from TwoHanded or Snowy), do not create projectile
				if (event.isCancelled()) {
					return;
				}

				AbilityManager.getManager().playerShotProjectileEvent(player, hook);
				player.setCooldown(Material.FISHING_ROD, cooldown);
			} else {
				return;
			}
		}

		Oversized.onAnyShoot(player, cooldown, true, true);
	}
}
