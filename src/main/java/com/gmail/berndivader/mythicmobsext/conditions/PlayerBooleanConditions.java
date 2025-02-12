package com.gmail.berndivader.mythicmobsext.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import org.bukkit.entity.Player;

import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.volatilecode.Volatile;

@ExternalAnnotation(name = "crouching,running,sleeping,jumping", author = "BerndiVader")
public class PlayerBooleanConditions extends AbstractCustomCondition implements IEntityCondition {
	private char tt;

	public PlayerBooleanConditions(String line, MythicLineConfig mlc) {
		super(line, mlc);
		this.tt = line.toUpperCase().charAt(0);
	}

	@Override
	public boolean check(AbstractEntity entity) {
		if (entity.isPlayer()) {
			switch (this.tt) {
			case 'C':
				return Volatile.handler.playerIsCrouching((Player) entity.getBukkitEntity());
			case 'R':
				return Volatile.handler.playerIsRunning((Player) entity.getBukkitEntity());
			case 'S':
				return Volatile.handler.playerIsSleeping((Player) entity.getBukkitEntity());
			case 'J':
				return Volatile.handler.playerIsJumping((Player) entity.getBukkitEntity());
			}
		}
		return false;
	}
}
