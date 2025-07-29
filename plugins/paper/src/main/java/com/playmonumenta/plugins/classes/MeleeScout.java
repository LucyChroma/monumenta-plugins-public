package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.PlaceholderAbility;
import com.playmonumenta.plugins.abilities.meleescout.AdrenalineRush;
import com.playmonumenta.plugins.abilities.meleescout.HasteWithoutWaste;
import com.playmonumenta.plugins.abilities.meleescout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.meleescout.ranger.WhirlingBlade;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;


public class MeleeScout extends PlayerClass {

	public static final int CLASS_ID = 9;
	public static final int RANGER_SPEC_ID = 17;
	public static final int STRATEGIST_SPEC_ID = 18;

	public MeleeScout() {
		mAbilities.add(PlaceholderAbility.INFO); // Reckless Acceleration
		mAbilities.add(HasteWithoutWaste.INFO); // Haste without Waste
		mAbilities.add(PlaceholderAbility.INFO); // The Wind Rises
		mAbilities.add(AdrenalineRush.INFO); // Adrenaline Rush
		mAbilities.add(PlaceholderAbility.INFO); // Machete Slash
		mAbilities.add(PlaceholderAbility.INFO); // Skyfeller
		mAbilities.add(PlaceholderAbility.INFO); // Mixed-Range Tactics
		mAbilities.add(PlaceholderAbility.INFO); // Agility
		mClass = CLASS_ID;
		mClassName = "MeleeScout";
		mClassColor = TextColor.fromHexString("#FF8000");
		mClassGlassFiller = Material.ORANGE_STAINED_GLASS_PANE;
		mDisplayItem = Material.DIAMOND_AXE;
		mClassDescription = "Replace me";
		mClassPassiveDescription = "Replace me";
		mClassPassiveName = "Wind's Friend";

		mSpecOne.mAbilities.add(PlaceholderAbility.INFO); // Galos Windeater
		mSpecOne.mAbilities.add(WhirlingBlade.INFO);
		mSpecOne.mAbilities.add(TacticalManeuver.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = RANGER_SPEC_ID;
		mSpecOne.mSpecName = "Ranger";
		mSpecOne.mDisplayItem = Material.WHEAT;
		mSpecOne.mDescription = "Rangers are agile experts of exploration that have unparalleled mastery of movement.";

		mSpecTwo.mAbilities.add(PlaceholderAbility.INFO); // Pre-Planned Path
		mSpecTwo.mAbilities.add(PlaceholderAbility.INFO); // Strategist's Foresight
		mSpecTwo.mAbilities.add(PlaceholderAbility.INFO); // A Thousand Cuts
		mSpecTwo.mSpecQuestScoreboard = "Quest103";
		mSpecTwo.mSpecialization = STRATEGIST_SPEC_ID;
		mSpecTwo.mSpecName = "Strategist";
		mSpecTwo.mDisplayItem = Material.FILLED_MAP;
		mSpecTwo.mDescription = "Strategist does some stuff that I CAN'T READ I'M AN UNDERTALE FAN blah blah blah replace this.";

		mTriggerOrder = ImmutableList.of(
			TacticalManeuver.INFO,
			WhirlingBlade.INFO // after wind bomb
		);
	}
}
