package com.playmonumenta.plugins.abilities.meleescout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;
import org.bukkit.util.Vector;

public class MacheteSlash extends Ability {
	public static final int COOLDOWN = 20 * 10;
	public static final double RANGE_1 = 4;
	public static final double RANGE_2 = 6;
	public static final double HEIGHT = 3;
	// 50 degrees on either side, 100 total
	public static final double ANGLE = 50;
	public static final double DAMAGE = 0.7;
	public static final int BLEED_DURATION = 20 * 6;

	public static final EnumMap<Material, Material> blocksReplace = new EnumMap<>(Map.of(
		Material.SOUL_SAND, Material.SOUL_SOIL,
		Material.HONEY_BLOCK, Material.YELLOW_CONCRETE,
		Material.SLIME_BLOCK, Material.LIME_CONCRETE,
		Material.MAGMA_BLOCK, Material.ORANGE_CONCRETE
	));

	public static final EnumSet<Material> blocksRemove = EnumSet.of(
		Material.FIRE,
		Material.SOUL_FIRE,
		Material.COBWEB,
		Material.VINE,
		Material.GLOW_LICHEN,
		Material.LILAC,
		Material.PEONY,
		Material.PITCHER_PLANT,
		Material.ROSE_BUSH,
		Material.SUNFLOWER,
		Material.WITHER_ROSE,
		Material.LARGE_FERN,
		Material.SHORT_GRASS,
		Material.TALL_GRASS,
		Material.FERN,
		Material.SWEET_BERRY_BUSH,
		Material.TWISTING_VINES_PLANT,
		Material.WEEPING_VINES_PLANT
	);

	public static final String CHARM_COOLDOWN = "Machete Slash Cooldown";
	public static final String CHARM_RANGE = "Machete Slash Range";
	public static final String CHARM_CONE = "Machete Slash Angle";
	public static final String CHARM_DAMAGE = "Machete Slash Damage";

	private static double mRange;
	private static double mAngle;

	public static final AbilityInfo<MacheteSlash> INFO =
		new AbilityInfo<>(MacheteSlash.class, "Machete Slash", MacheteSlash::new)
			.linkedSpell(ClassAbility.MACHETE_SLASH)
			.descriptions(
				String.format("Placeholder ability level %s.",
					1),
				String.format("Placeholder ability level %s.",
					2),
				String.format("Placeholder ability enhancement. :%s",
					3))
			.simpleDescription("Make terrain easy to traverse.")
			.scoreboardId("MacheteSlash")
			.displayItem(Material.IRON_SWORD)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MacheteSlash::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.shorthandName("MS");

	public MacheteSlash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, isLevelTwo() ? RANGE_2 : RANGE_1);
		mAngle = isEnhanced() ? 180 : Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);
	}

	public boolean cast() {
		if(isOnCooldown()) return false;
		putOnModifiedCooldown();

		Hitbox hitbox = Hitbox.approximateCylinderSegment(
			LocationUtils.getHalfHeightLocation(mPlayer).add(0, -HEIGHT, 0), 2 * HEIGHT, mRange, Math.toRadians(mAngle));

		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);

		ItemStatManager.PlayerItemStats stats = mPlugin.mItemStatManager.getPlayerItemStats(mPlayer);
		if (stats != null) {
			ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
			if (map != null) {
				ItemStat atk = Objects.requireNonNull(AttributeType.ATTACK_DAMAGE_ADD.getItemStat());
				double temp = 1 + map.get(atk);
				damage *= temp;
			}
		}

		for(LivingEntity entity : hitbox.getHitMobs()) {
			DamageUtils.damage(mPlayer, entity, DamageEvent.DamageType.MELEE_SKILL, damage,
				ClassAbility.MACHETE_SLASH, false, isLevelTwo());
			if(isLevelTwo()) {
				EntityUtils.applyBleed(mPlugin, BLEED_DURATION, 0.3, entity);
			}
		}

		int particleEveryAngle = 20;
		int modifiedAngle = (int) (particleEveryAngle * Math.floor(mAngle / particleEveryAngle));
		double particleEveryDistance = 2;
		double modifiedDistance = (particleEveryDistance * Math.floor(mRange / particleEveryDistance));
		for(int angle = -modifiedAngle; angle <= modifiedAngle; angle += particleEveryAngle) {
			Vector dir = VectorUtils.rotateTargetDirection(mPlayer.getEyeLocation().getDirection(), angle, 0).setY(0);
			Location location = LocationUtils.getHalfHeightLocation(mPlayer);
			for(double distance = particleEveryDistance; distance <= modifiedDistance; distance += particleEveryDistance) {
				new PartialParticle(Particle.SWEEP_ATTACK, location.clone().add(dir.clone().multiply(distance)), 1, 0, 0, 0, 1).spawnAsPlayerActive(mPlayer);
			}
		}

		// i hate this but i dont have a better idea
		// meow
		BoundingBox bb = hitbox.getBoundingBox();
		World world = mPlayer.getWorld();
		for(int x = (int) Math.floor(bb.getMinX()); x < Math.ceil(bb.getMaxX()); x++) {
			for(int y = (int) Math.floor(bb.getMinY()); y < Math.ceil(bb.getMaxY()); y++) {
				for(int z = (int) Math.floor(bb.getMinZ()); z < Math.ceil(bb.getMaxZ()); z++) {
					if (hitbox.intersects(new BoundingBox(x,y,z,x+1,y+1,z+1))) {
						Block block = world.getBlockAt(x, y, z);
						if(blocksRemove.contains(block.getType())) {
							block.breakNaturally(true, true);
						} else if(blocksReplace.containsKey(block.getType())) {
							block.setType(blocksReplace.get(block.getType()));
						}
					}
				}
			}
		}

		return true;
	}

	private void putOnModifiedCooldown() {
		int cooldown = getModifiedCooldown();
		// double aspd = 2;
//		ItemStatManager.PlayerItemStats stats = mPlugin.mItemStatManager.getPlayerItemStats(mPlayer);
//		if (stats != null) {
//			ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
//			if (map != null) {
//				AttributeType stat = Objects.requireNonNull(AttributeType.ATTACK_SPEED);
//				aspd = map.get(stat);
//			}
//		}
		double aspd = EntityUtils.getAttributeOrDefault(mPlayer, Attribute.GENERIC_ATTACK_SPEED, 2);
		if(aspd > 2) aspd = 2;
		mPlayer.sendRawMessage(String.valueOf(cooldown));
		mPlayer.sendRawMessage(String.valueOf(aspd));
		mPlayer.sendRawMessage(String.valueOf((int) (cooldown / aspd)));
		putOnCooldown((int) (cooldown / aspd));
	}
}
