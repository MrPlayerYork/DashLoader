package dev.notalpha.dashloader.client.model.geometry;

import dev.notalpha.dashloader.mixin.accessor.TextureSlotsAccessor;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import org.joml.Matrix4fc;

import java.util.Map;

/**
 * Identifies one bake of a model's geometry: which model, which textures, and the transformation it was baked with.
 * <br><br>
 * The model state is only observed through its matrices, which is also the only thing geometry baking can see of it,
 * so this works for model states DashLoader has never heard of.
 * <br><br>
 * The key is the model name plus a 128 bit hash of everything else. The hash is not cryptographic but it is wide and well mixed,
 * and a wrong match is also guarded against when saving by checking that equal keys always bake to equal results.
 */
public final class GeometryKey {
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final long M1 = 0x9E3779B97F4A7C15L;
	private static final long M2 = 0xC2B2AE3D27D4EB4FL;

	private GeometryKey() {
	}

	public static String of(String modelName, TextureSlots slots, ModelState state) {
		// Two independent lanes give 128 bits.
		long a = 0x243F6A8885A308D3L;
		long b = 0x13198A2E03707344L;

		// Texture slots. Combined with addition so the order of the map does not matter.
		long slotSum = 0;
		long slotXor = 0;
		for (Map.Entry<String, Material> entry : ((TextureSlotsAccessor) slots).getResolvedValues().entrySet()) {
			Material material = entry.getValue();
			long h = mix(hash(entry.getKey()) * M1 + hash(material.sprite().toString()) + (material.forceTranslucent() ? 0x51ED27L : 0));
			slotSum += h;
			slotXor ^= Long.rotateLeft(h, 21) * M2;
		}
		a = mix(a ^ slotSum);
		b = mix(b ^ slotXor);

		long[] lanes = {a, b};
		matrix(lanes, state.transformation().getMatrix());
		for (Direction direction : DIRECTIONS) {
			matrix(lanes, state.faceTransformation(direction));
		}

		StringBuilder out = new StringBuilder(modelName.length() + 33).append(modelName).append('#');
		appendHex(out, lanes[0]);
		appendHex(out, lanes[1]);
		return out.toString();
	}

	private static void matrix(long[] lanes, Matrix4fc matrix) {
		long a = lanes[0];
		long b = lanes[1];
		for (int column = 0; column < 4; column++) {
			for (int row = 0; row < 4; row++) {
				long bits = Float.floatToRawIntBits(matrix.get(column, row)) & 0xFFFFFFFFL;
				a = Long.rotateLeft(a ^ bits, 27) * M1;
				b = Long.rotateLeft(b + bits, 31) * M2 + a;
			}
		}
		lanes[0] = mix(a);
		lanes[1] = mix(b);
	}

	private static long hash(String string) {
		long h = 0xCBF29CE484222325L;
		for (int i = 0, length = string.length(); i < length; i++) {
			h = (h ^ string.charAt(i)) * 0x100000001B3L;
		}
		return mix(h);
	}

	private static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	private static void appendHex(StringBuilder out, long value) {
		for (int shift = 60; shift >= 0; shift -= 4) {
			out.append(Character.forDigit((int) (value >>> shift) & 0xF, 16));
		}
	}
}
