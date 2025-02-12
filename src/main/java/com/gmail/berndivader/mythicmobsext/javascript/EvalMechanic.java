package com.gmail.berndivader.mythicmobsext.javascript;

import java.io.File;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.INoTargetSkill;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.SkillString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.externals.ExternalAnnotation;
import com.gmail.berndivader.mythicmobsext.utils.Utils;

@ExternalAnnotation(name = "math", author = "BerndiVader")
public class EvalMechanic extends SkillMechanic implements INoTargetSkill, ITargetedEntitySkill {
	PlaceholderString eval;
	String js = "mme_eval";
	String[] parse;

	public EvalMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;
		String temp = mlc.getString(new String[] { "evaluate", "eval", "e" }, "");
		if ((temp.startsWith("\""))) {
			temp = SkillString.unparseMessageSpecialChars(temp.substring(1, temp.length() - 1));
		}
		;
		eval = PlaceholderString.of(temp);
		String s1 = mlc.getString(new String[] { "storage", "store", "s" }, "<mob.meta.test>");
		parse = (s1.substring(1, s1.length() - 1)).split(Pattern.quote("."));
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
		return eval(data, target.getBukkitEntity(), null);
	}

	@Override
	public SkillResult cast(SkillMetadata data) {
		return eval(data, data.getCaster().getEntity().getBukkitEntity(),
				data.getCaster().getEntity().getBukkitEntity().getLocation());
	}

	SkillResult eval(SkillMetadata data, Entity e1, Location l1) {
		double s1 = 0d;
		try {
			if(Nashorn.invocable!=null) {
				s1 = ((Number) Nashorn.invocable.invokeFunction(js, eval.get(data))).doubleValue();
			} else {
				Main.logger.warning("No javascriptengine present!");
			}
		} catch (NoSuchMethodException | ScriptException e) {
			e.printStackTrace();
		}
		Entity target = null;
		switch (parse[0]) {
		case "mob":
		case "caster":
			target = data.getCaster().getEntity().getBukkitEntity();
			break;
		case "target":
			target = e1;
			break;
		case "trigger":
			target = data.getTrigger().getBukkitEntity();
			break;
		}
		if (target != null) {
			if (parse[1].equals("meta")) {
				target.setMetadata(parse[2], new FixedMetadataValue(Main.getPlugin(), s1));
			} else if (parse[1].equals("score")) {
				Objective o1 = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(parse[2]);
				if (o1 != null) {
					o1.getScore(target instanceof Player ? target.getName() : target.getUniqueId().toString())
							.setScore((int) s1);
					;
				} else {
					Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(parse[2], Criteria.DUMMY, parse[2])
							.getScore(target instanceof Player ? target.getName() : target.getUniqueId().toString())
							.setScore((int) (s1));
				}
			} else if (parse[1].equals("stance")) {
				ActiveMob am = Utils.mobmanager.getMythicMobInstance(target);
				if (am != null)
					am.setStance(Double.toString(s1));
			}
		} else if (parse[0].equals("score")) {
			Objective o1 = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(parse[1]);
			if (o1 != null) {
				o1.getScore(parse[2]).setScore((int) (s1));
			} else {
				Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(parse[1], Criteria.DUMMY, parse[1])
						.getScore(parse[2]).setScore((int) (s1));
			}
		}
		return SkillResult.SUCCESS;
	}

}
