package com.gmail.berndivader.mythicmobsext.thiefs;

import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.gmail.berndivader.mythicmobsext.Main;

public class Thiefs implements Listener {
	public static ThiefHandler thiefhandler;

	static {
		thiefhandler = new ThiefHandler();
	}

	public Thiefs() {
		Bukkit.getServer().getPluginManager().registerEvents(this, Main.getPlugin());
	}

	public void onDisable() {
		if (thiefhandler != null)
			thiefhandler.cancelTask();
	}

	@EventHandler
	public void onMechanicLoad(MythicMechanicLoadEvent e) {
		String s1;
		SkillMechanic sm;
		s1 = e.getMechanicName().toLowerCase();
		switch (s1) {
		case "dropstolenitems":
		case "dropstolenitems_et":
			sm = new DropStolenItemsMechanic(e.getContainer().getManager(), e.getContainer().getFile(), e.getConfig().getLine(), e.getConfig());
			//sm = new DropStolenItemsMechanic(e.getContainer().getManager(), e.getContainer().getConfigLine(), e.getConfig());
			e.register(sm);
			break;
		case "steal":
		case "steal_ext":
			sm = new StealMechanic(e.getContainer().getManager(), e.getContainer().getFile(), e.getConfig().getLine(), e.getConfig());
			//sm = new StealMechanic(e.getContainer().getManager(), e.getContainer().getConfigLine(), e.getConfig());
			e.register(sm);
			break;
		}
	}

	@EventHandler
	public void onConditionLoad(MythicConditionLoadEvent e) {
		if (e.getConditionName().toLowerCase().equals("isthief")) {
			e.register(new IsThiefCondition(e.getConfig().getLine(), e.getConfig()));
		}
	}
}
