package dev.notalpha.dashloader.dev;

import dev.notalpha.dashloader.misc.DebugHooks;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Connects the development tools to DashLoader, before the game starts. */
public class DevPreLaunch implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		DebugHooks.sink = new DevSink();
	}
}
