package dev.notalpha.dashloader.dev;

import com.mojang.blaze3d.platform.NativeImage;
import dev.notalpha.dashloader.DashLoader;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.zip.CRC32;

/**
 * Debugging aid: records a hash of the results of everything DashLoader caches, so a run which created the cache can be compared with a run
 * which loaded it. Only active when the DASHLOADER_DUMP environment variable is set.
 */
public final class StateDump {
	private static final boolean ENABLED = System.getenv("DASHLOADER_DUMP") != null;
	private static final Map<String, String> LINES = new ConcurrentSkipListMap<>();

	private StateDump() {
	}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void record(String category, String key, long hash) {
		LINES.put(category + " " + key, Long.toHexString(hash));
	}

	public static void write(String name) {
		if (!ENABLED) {
			return;
		}
		try {
			Path dir = Path.of("dashloader-debug");
			Files.createDirectories(dir);
			List<String> out = new ArrayList<>(LINES.size());
			LINES.forEach((key, value) -> out.add(key + " " + value));
			Files.write(dir.resolve(name + ".txt"), out);
			DashLoader.LOG.info("Wrote {} state hashes to {}", out.size(), dir.resolve(name + ".txt").toAbsolutePath());
		} catch (IOException e) {
			DashLoader.LOG.error("Could not write the state dump", e);
		}
	}

	public static long hash(NativeImage[] images) {
		long h = 17;
		for (NativeImage image : images) {
			h = h * 31 + (image == null ? 0 : hash(image));
		}
		return h;
	}

	public static long hash(NativeImage image) {
		ByteBuffer pixels = MemoryUtil.memByteBuffer(image.getPointer(), image.getWidth() * image.getHeight() * image.format().components());
		CRC32 crc = new CRC32();
		crc.update(pixels.duplicate());
		return crc.getValue() * 31 + image.getWidth() * 7919L + image.getHeight() + image.format().ordinal() * 104729L;
	}

	/** Content of the quads, independent of object identity. */
	public static long hash(QuadCollection collection) {
		long h = 17;
		for (BakedQuad quad : collection.getAll()) {
			for (int corner = 0; corner < 4; corner++) {
				h = h * 31 + Float.floatToRawIntBits(quad.position(corner).x());
				h = h * 31 + Float.floatToRawIntBits(quad.position(corner).y());
				h = h * 31 + Float.floatToRawIntBits(quad.position(corner).z());
				h = h * 31 + quad.packedUV(corner);
			}
			BakedQuad.MaterialInfo info = quad.materialInfo();
			h = h * 31 + quad.direction().ordinal();
			h = h * 31 + info.sprite().contents().name().toString().hashCode();
			h = h * 31 + info.layer().ordinal();
			h = h * 31 + info.tintIndex();
			h = h * 31 + (info.shadeDirectionOverride() == null ? -1 : info.shadeDirectionOverride().ordinal());
			h = h * 31 + info.lightEmission();
		}
		return h * 31 + collection.getAll().size();
	}
}
