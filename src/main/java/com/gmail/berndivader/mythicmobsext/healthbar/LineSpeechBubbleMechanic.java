package com.gmail.berndivader.mythicmobsext.healthbar;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.SkillString;
import io.lumine.mythic.core.skills.placeholders.parsers.PlaceholderStringImpl;

import java.io.File;

public class LineSpeechBubbleMechanic extends SkillMechanic implements ITargetedEntitySkill {
	private PlaceholderString oline;
	private PlaceholderString nline;
	private String id;
	private String cmp;

	public LineSpeechBubbleMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;
		this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;

		this.id = mlc.getString("id", "bubble");
		this.cmp = mlc.getString(new String[] { "mode", "m" }, "replace").toLowerCase();
		String ol = mlc.getString(new String[] { "oldline", "ol" }, null);
		String nl = mlc.getString(new String[] { "newline", "nl" }, null);
		if (nl != null) {
			if (nl.startsWith("\"") && nl.endsWith("\"")) {
				this.nline = new PlaceholderStringImpl(
						SkillString.parseMessageSpecialChars((nl.substring(1, nl.length() - 1))));
			} else {
				this.nline = new PlaceholderStringImpl(nl);
			}
		}
		if (ol != null) {
			if (ol.startsWith("\"") && ol.endsWith("\"")) {
				this.oline = new PlaceholderStringImpl(
						SkillString.unparseMessageSpecialChars(ol.substring(1, ol.length() - 1)));
			} else {
				this.oline = new PlaceholderStringImpl(ol);
			}
		} else {
			this.oline = new PlaceholderStringImpl("");
		}
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
		if (HealthbarHandler.speechbubbles
				.containsKey(data.getCaster().getEntity().getUniqueId().toString() + this.id)) {
			SpeechBubble sb = HealthbarHandler.speechbubbles
					.get(data.getCaster().getEntity().getUniqueId().toString() + this.id);
			if (this.oline != null) {
				String s2 = "";
				String s1 = this.oline.get(data, target);
				if (this.nline != null)
					s2 = this.nline.get(data, target);
				if (this.cmp.equals("append")) {
					String[] arr1 = sb.template;
					String[] arr2 = new String[] { s2 };
					String[] arr = new String[arr1.length + arr2.length];
					System.arraycopy(arr1, 0, arr, 0, arr1.length);
					System.arraycopy(arr2, 0, arr, arr1.length, arr2.length);
					sb.template = arr;
					arr = null;
					arr1 = null;
					arr2 = null;
				} else {
					for (int i = 0; i < sb.template.length; i++) {
						if (!sb.template[i].contains(s1))
							continue;
						if (this.cmp.equals("replace")) {
							sb.template[i] = s2;
							continue;
						}
						if (this.cmp.equals("remove")) {
							String[] arr1 = sb.template;
							String[] arr = new String[arr1.length - 1];
							if (i >= 0 && arr.length > 0) {
								System.arraycopy(arr1, 0, arr, 0, i);
								System.arraycopy(arr1, i + 1, arr, i, arr.length - i);
								sb.template = arr;
								i--;
							}
						}
					}
				}
				sb.lines();
			}
			return SkillResult.SUCCESS;
		}
		return SkillResult.CONDITION_FAILED;
	}

}
