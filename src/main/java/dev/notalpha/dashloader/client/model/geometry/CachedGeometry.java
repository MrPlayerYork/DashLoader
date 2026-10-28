package dev.notalpha.dashloader.client.model.geometry;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A baked {@link QuadCollection} flattened into primitive arrays. Sprites are stored by name (an index into a table shared by
 * the whole cache) because the atlases do not exist yet when the cache is read.
 * <br><br>
 * Quad order: the unculled quads, then the quads of every direction in {@link Direction#values()} order. {@link #counts}
 * holds the number of quads of each of those 7 groups.
 */
public final class CachedGeometry {
	private static final int META_SIZE = 6;
	private static final Direction[] DIRECTIONS = Direction.values();

	/** The distinct corner positions of the geometry, x, y, z each. */
	public final float[] vertices;
	/** 4 per quad: the index of each corner in {@link #vertices}. */
	public final int[] corners;
	/** 4 per quad. */
	public final long[] uvs;
	/** {@link #META_SIZE} per quad: direction, sprite index, transparency kind, tint index, shade direction override (-1 for none), light emission. */
	public final int[] meta;
	public final int[] counts;

	public CachedGeometry(float[] vertices, int[] corners, long[] uvs, int[] meta, int[] counts) {
		this.vertices = vertices;
		this.corners = corners;
		this.uvs = uvs;
		this.meta = meta;
		this.counts = counts;
	}

	/**
	 * @param spriteIndex Maps a sprite name to its index in the shared table, adding it if needed.
	 */
	public static CachedGeometry of(QuadCollection collection, Function<Identifier, Integer> spriteIndex) {
		List<BakedQuad> quads = new ArrayList<>(collection.getAll().size());
		int[] counts = new int[1 + DIRECTIONS.length];
		counts[0] = collection.unculled.size();
		quads.addAll(collection.unculled);
		for (int i = 0; i < DIRECTIONS.length; i++) {
			List<BakedQuad> culled = collection.getQuads(DIRECTIONS[i]);
			counts[1 + i] = culled.size();
			quads.addAll(culled);
		}

		Map<VertexKey, Integer> vertexIndex = new HashMap<>();
		List<Float> vertexList = new ArrayList<>();
		int[] corners = new int[quads.size() * 4];
		long[] uvs = new long[quads.size() * 4];
		int[] meta = new int[quads.size() * META_SIZE];
		for (int q = 0; q < quads.size(); q++) {
			BakedQuad quad = quads.get(q);
			for (int corner = 0; corner < 4; corner++) {
				Vector3fc position = quad.position(corner);
				VertexKey key = new VertexKey(Float.floatToRawIntBits(position.x()), Float.floatToRawIntBits(position.y()), Float.floatToRawIntBits(position.z()));
				Integer index = vertexIndex.get(key);
				if (index == null) {
					index = vertexList.size() / 3;
					vertexIndex.put(key, index);
					vertexList.add(position.x());
					vertexList.add(position.y());
					vertexList.add(position.z());
				}
				corners[q * 4 + corner] = index;
				uvs[q * 4 + corner] = quad.packedUV(corner);
			}

			BakedQuad.MaterialInfo info = quad.materialInfo();
			TextureAtlasSprite sprite = info.sprite();
			meta[q * META_SIZE] = quad.direction().ordinal();
			meta[q * META_SIZE + 1] = spriteIndex.apply(sprite.contents().name());
			meta[q * META_SIZE + 2] = kindOf(info.layer());
			meta[q * META_SIZE + 3] = info.tintIndex();
			Direction shade = info.shadeDirectionOverride();
			meta[q * META_SIZE + 4] = shade == null ? -1 : shade.ordinal();
			meta[q * META_SIZE + 5] = info.lightEmission();
		}
		float[] vertices = new float[vertexList.size()];
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = vertexList.get(i);
		}
		return new CachedGeometry(vertices, corners, uvs, meta, counts);
	}

	/**
	 * @param sprites  The shared sprite table.
	 * @param resolver Resolves a sprite name into the baked material of the currently loaded atlases.
	 * @param baker    Used to share vectors and material infos like vanilla does, may be null.
	 */
	public QuadCollection build(Identifier[] sprites, Function<Identifier, Material.Baked> resolver, @Nullable ModelBaker baker) {
		Material.Baked[] resolved = new Material.Baked[sprites.length];
		Map<Long, BakedQuad.MaterialInfo> infos = new HashMap<>(8);
		// Every distinct position is shared and interned once, instead of once per quad corner.
		Vector3fc[] positions = new Vector3fc[this.vertices.length / 3];
		for (int i = 0; i < positions.length; i++) {
			Vector3fc position = new Vector3f(this.vertices[i * 3], this.vertices[i * 3 + 1], this.vertices[i * 3 + 2]);
			positions[i] = baker == null ? position : baker.interner().vector(position);
		}
		QuadCollection.Builder builder = new QuadCollection.Builder();
		int q = 0;
		for (int group = 0; group < this.counts.length; group++) {
			for (int i = 0; i < this.counts[group]; i++, q++) {
				BakedQuad quad = this.quad(q, positions, sprites, resolved, infos, resolver, baker);
				if (group == 0) {
					builder.addUnculledFace(quad);
				} else {
					builder.addCulledFace(DIRECTIONS[group - 1], quad);
				}
			}
		}
		return builder.build();
	}

	private BakedQuad quad(int q, Vector3fc[] positions, Identifier[] sprites, Material.Baked[] resolved, Map<Long, BakedQuad.MaterialInfo> infos, Function<Identifier, Material.Baked> resolver, @Nullable ModelBaker baker) {
		BakedQuad.MaterialInfo info = this.materialInfo(q, sprites, resolved, infos, resolver, baker);

		return new BakedQuad(positions[this.corners[q * 4]], positions[this.corners[q * 4 + 1]], positions[this.corners[q * 4 + 2]], positions[this.corners[q * 4 + 3]],
				this.uvs[q * 4], this.uvs[q * 4 + 1], this.uvs[q * 4 + 2], this.uvs[q * 4 + 3],
				DIRECTIONS[this.meta[q * META_SIZE]], info);
	}

	private BakedQuad.MaterialInfo materialInfo(int q, Identifier[] sprites, Material.Baked[] resolved, Map<Long, BakedQuad.MaterialInfo> infos, Function<Identifier, Material.Baked> resolver, @Nullable ModelBaker baker) {
		int base = q * META_SIZE;
		// sprite, transparency, tint, shade and light decide the info, the direction of the quad does not.
		// Packed exactly: sprite (24 bits) | kind (2) | tint + 1 (9) | shade + 1 (4) | light (5). Values outside those ranges are not memoised.
		int tint = this.meta[base + 3];
		int shadeValue = this.meta[base + 4];
		int light = this.meta[base + 5];
		boolean memo = this.meta[base + 1] >>> 24 == 0 && tint >= -1 && tint < 511 && shadeValue >= -1 && shadeValue < 14 && light >= 0 && light < 32;
		long key = ((long) this.meta[base + 1] << 20) | ((long) this.meta[base + 2] << 18) | ((long) (tint + 1) << 9) | ((long) (shadeValue + 1) << 5) | light;
		BakedQuad.MaterialInfo info = memo ? infos.get(key) : null;
		if (info == null) {
			int spriteIndex = this.meta[base + 1];
			Material.Baked material = resolved[spriteIndex];
			if (material == null) {
				material = resolved[spriteIndex] = resolver.apply(sprites[spriteIndex]);
			}
			int shade = this.meta[base + 4];
			info = BakedQuad.MaterialInfo.of(material, transparencyOf(this.meta[base + 2]), this.meta[base + 3], shade < 0 ? null : DIRECTIONS[shade], this.meta[base + 5]);
			if (baker != null) {
				info = baker.interner().materialInfo(info);
			}
			if (memo) {
				infos.put(key, info);
			}
		}
		return info;
	}

	private record VertexKey(int x, int y, int z) {
	}

	// MaterialInfo.of only looks at whether a transparency is translucent / transparent, which is exactly what the layer encodes.
	private static int kindOf(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> 0;
			case CUTOUT -> 1;
			case TRANSLUCENT -> 2;
		};
	}

	private static Transparency transparencyOf(int kind) {
		return switch (kind) {
			case 0 -> Transparency.NONE;
			case 1 -> Transparency.TRANSPARENT;
			default -> Transparency.TRANSLUCENT;
		};
	}
}
