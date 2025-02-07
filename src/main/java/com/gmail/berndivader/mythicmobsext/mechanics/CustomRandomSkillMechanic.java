package com.gmail.berndivader.mythicmobsext.mechanics;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.placeholders.parsers.PlaceholderStringImpl;

@ExternalAnnotation(name = "customrandomskill,advrandomskill", author = "BerndiVader")
public class CustomRandomSkillMechanic extends SkillMechanic implements IMetaSkill {
	static SkillManager skillmanager;
	LinkedList<SkillEntry> entrylist;
	boolean b1, shuffle;

	public CustomRandomSkillMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;
		this.target_creative = true;
		skillmanager = Utils.mythicmobs.getSkillManager();
		this.entrylist = new LinkedList<>();

		String[] parse = mlc.getString(new String[] { "skills", "s" }).split(",");
		b1(mlc.getBoolean(new String[] { "renewrandom", "newrandom", "rnr" }, false));
		shuffle = mlc.getBoolean("shuffle", false);

		if (parse.length > 0) {
			for (int a = 0; a < parse.length; a++) {
				String s = null;
				String c = "0";
				String par[] = parse[a].split(":");
				s = par[0];
				if (par.length > 1)
					c = par[1];
				SkillEntry entry = new SkillEntry(a, c, s);
				if (entry.isSkillPresent())
					entrylist.add(a, entry);
			}
		}
	}

	private void b1(boolean b1) {
		this.b1 = b1;
	}

	@Override
	public SkillResult cast(SkillMetadata data) {
		double r = ThreadLocalRandom.current().nextDouble();
		if (shuffle)
			Collections.shuffle(entrylist);
		for (SkillEntry sentry : entrylist) {
			if (b1)
				r = ThreadLocalRandom.current().nextDouble();
			if (r <= sentry.getChance(data)) {
				if (sentry.isSkillPresent() && sentry.getSkill().isUsable(data)) {
					sentry.getSkill().execute(data);
					return SkillResult.SUCCESS;
				}
			}
		}
		return SkillResult.CONDITION_FAILED;
	}

	public class SkillEntry {
		PlaceholderString chance;
		int priority;
		Optional<Skill> skill = Optional.empty();

		public SkillEntry(int priority, String chance, String skill) {
			this.skill = skillmanager.getSkill(skill);
			this.chance = new PlaceholderStringImpl(chance);
			this.priority = priority;
		}

		public boolean isSkillPresent() {
			return this.skill.isPresent();
		}

		public Skill getSkill() {
			return this.skill.get();
		}

		public double getChance(SkillMetadata data) {
			double c;
			if (Character.isDigit(this.chance.get().charAt(0)))
				return Double.parseDouble(this.chance.get());
			AbstractEntity t = data.getEntityTargets() != null && data.getEntityTargets().iterator().hasNext()
					? data.getEntityTargets().iterator().next()
					: null;
			try {
				c = Double.parseDouble(this.chance.get(data, t)) / 100;
			} catch (Exception e) {
				c = 0d;
			}
			return c;
		}

		public int getPriority() {
			return this.priority;
		}
	}

}
