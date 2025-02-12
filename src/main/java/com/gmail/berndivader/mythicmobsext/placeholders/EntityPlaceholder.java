package com.gmail.berndivader.mythicmobsext.placeholders;

import java.util.function.BiFunction;

import com.gmail.berndivader.mythicmobsext.utils.Utils;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.core.skills.placeholders.Placeholder;

public class EntityPlaceholder implements IPlaceHolder<AbstractEntity> {
	String placeholder_name;
	static final String error = "#NOTFOUND>";

	public EntityPlaceholder() {
	}

	void register() {
		Utils.mythicmobs.getPlaceholderManager().register(placeholder_name,
				Placeholder.entity(this.transformer()));
	}

	@Override
	public BiFunction<AbstractEntity, String, String> transformer() {
		return null;
	}

}
