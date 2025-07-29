package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ProjectileDamageAdd implements Attribute {

	@Override
	public String getName() {
		return "Projectile Damage Add";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.PROJECTILE_DAMAGE_ADD;
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	final Set<EntityType> SPECIALCASE = Set.of(
		EntityType.THROWN_EXP_BOTTLE,
		EntityType.FISHING_HOOK
	);

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj) {
			if (proj instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
				value *= Math.max(0, Math.min(1, arrow.getVelocity().length() / Constants.PLAYER_BOW_INITIAL_SPEED / Math.abs(ProjectileSpeed.getProjectileSpeedModifier(arrow))));
			}
			event.setFlatDamage(value);
		}
	}

	@Override
	public void onProjectileHit(Plugin plugin, Player player, double value, ProjectileHitEvent event, Projectile projectile){
		if(SPECIALCASE.contains(projectile.getType())
			&& event.getHitEntity() instanceof LivingEntity enemy) { // implicit null check
			DamageUtils.damage(player, enemy, DamageType.PROJECTILE, value, null, false, true);
			ItemStatManager.PlayerItemStats pstats = DamageListener.getProjectileItemStats(projectile);
			if (projectile.getType() != EntityType.FISHING_HOOK ||
				(pstats != null && pstats.getItemStats().get(EnchantmentType.DELETE_BOBBER) > 0)) {
				playsound(projectile.getType(), projectile.getLocation());
				projectile.remove();
				event.setCancelled(true);
			}
		}
	}

	private void playsound(EntityType type, Location loc) {
		switch(type) {
			case THROWN_EXP_BOTTLE:
				loc.getWorld().playSound(loc, Sound.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 0.9F + 0.1F * FastUtils.RANDOM.nextFloat(), 1.0F);
				break;
			case FISHING_HOOK:
				loc.getWorld().playSound(loc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.0F, 0.8F + 0.4F * FastUtils.RANDOM.nextFloat());
				break;
		}
	}
}
