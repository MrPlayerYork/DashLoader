package dev.notalpha.dashloader.client.shader;

import dev.notalpha.dashloader.api.DashObject;
import dev.notalpha.dashloader.api.collection.ObjectObjectList;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import net.minecraft.client.renderer.ShaderDefines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DashDefines implements DashObject<ShaderDefines, ShaderDefines> {
	public final ObjectObjectList<String, String> values;
	public final String[] flags;

	public DashDefines(ShaderDefines defines) {
		this.values = new ObjectObjectList<>(new ArrayList<>(defines.values().size()));
		defines.values().forEach(this.values::put);

		this.flags = defines.flags().toArray(new String[0]);
	}

	public DashDefines(ObjectObjectList<String, String> values, String[] flags) {
		this.values = values;
		this.flags = flags;
	}

	@Override
	public ShaderDefines export(RegistryReader reader) {
		var map = new HashMap<String, String>(this.values.list().size());
		this.values.forEach(map::put);
		return new ShaderDefines(map, new HashSet<>(List.of(this.flags)));
	}
}
