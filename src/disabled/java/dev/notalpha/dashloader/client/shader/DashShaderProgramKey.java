package dev.notalpha.dashloader.client.shader;

import dev.notalpha.dashloader.api.DashObject;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DashShaderProgramKey implements DashObject<ShaderProgram, ShaderProgram> {
	public final int id;
	public final int vertex;
	public final Map<String, String> values;
	public final String[] flags;

	public DashShaderProgramKey(ShaderProgram key, RegistryWriter writer) {
		this.id = writer.add(key.configId());
		this.vertex = writer.add(key.vertexFormat());
		this.values = key.defines().values();
		this.flags = key.defines().flags().toArray(String[]::new);
	}

	public DashShaderProgramKey(int id, int vertex, Map<String, String> values, String[] flags) {
		this.id = id;
		this.vertex = vertex;
		this.values = values;
		this.flags = flags;
	}

	@Override
	public ShaderProgram export(RegistryReader reader) {
		return new ShaderProgram(reader.get(id), reader.get(vertex), new ShaderDefines(this.values, new HashSet<>(List.of(this.flags))));
	}
}
