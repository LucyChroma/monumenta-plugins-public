package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class OwnTempoLevelTwoCooldown extends ZeroArgumentEffect {
	public static final String effectID = "OwnTempoLevelTwoCooldown";

	public OwnTempoLevelTwoCooldown(int duration) {
		super(duration, effectID);
	}

	public static OwnTempoLevelTwoCooldown deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new OwnTempoLevelTwoCooldown(duration);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		entity.getWorld().playSound(((LivingEntity) entity).getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3F, 0.7F);
	}

	@Override
	public String toString() {
		return String.format("OwnTempoLevelTwoCooldown duration:%d", this.getDuration());
	}
}
