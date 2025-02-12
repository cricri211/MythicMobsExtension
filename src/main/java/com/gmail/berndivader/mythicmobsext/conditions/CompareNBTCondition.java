package com.gmail.berndivader.mythicmobsext.conditions;

import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.volatilecode.Volatile;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;

@ExternalAnnotation(name = "cmpnbt,comparenbt", author = "BerndiVader")
public class CompareNBTCondition extends AbstractCustomCondition implements IEntityCondition {
	private String s1;

	public CompareNBTCondition(String line, MythicLineConfig mlc) {
		super(line, mlc);
		s1 = mlc.getString("nbt", null);
	}

	@Override
	public boolean check(AbstractEntity e) {
		if (s1 != null) {
			String s = s1.replaceAll("\\(", "{").replaceAll("\\)", "}");
			return Volatile.handler.getNBTValueOf(e.getBukkitEntity(), s, false);
		}
		return false;
	}

}
