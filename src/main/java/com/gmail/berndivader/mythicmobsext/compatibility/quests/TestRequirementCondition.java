package com.gmail.berndivader.mythicmobsext.compatibility.quests;

import com.gmail.berndivader.mythicmobsext.conditions.AbstractCustomCondition;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.core.skills.SkillString;
import me.pikamug.quests.BukkitQuestsPlugin;
import me.pikamug.quests.player.Quester;
import me.pikamug.quests.quests.Quest;

public class TestRequirementCondition extends AbstractCustomCondition implements IEntityCondition {
	BukkitQuestsPlugin quests = QuestsSupport.inst().quests();
	String s1;

	public TestRequirementCondition(String line, MythicLineConfig mlc) {
		super(line, mlc);
		s1 = mlc.getString("quest", "").toLowerCase();
		if (!s1.isEmpty() && s1.charAt(0) == '"')
			s1 = SkillString.parseMessageSpecialChars(s1.substring(1, s1.length() - 1));
	}

	@Override
	public boolean check(AbstractEntity e) {
		if (!e.isPlayer() || s1.isEmpty() || quests.canUseQuests(e.getUniqueId()))
			return false;
		boolean bl1 = false;
		Quester quester = quests.getQuester(e.getUniqueId());
		Quest quest = quests.getQuest(s1);
		if (quest != null)
			bl1 = quest.testRequirements(quester);
		return bl1;
	}
}
