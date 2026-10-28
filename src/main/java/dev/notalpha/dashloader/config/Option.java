package dev.notalpha.dashloader.config;

public enum Option {
	CACHE_FONT("cache.font"),                                   // Caches fonts and their images.
	CACHE_MODEL_LOADER("cache.model"),                          // Caches the geometry of models and which block states share a model
	CACHE_SPLASH_TEXT("cache.SplashTextResourceSupplierMixin"), // Caches the splash texts from the main screen
	CACHE_SPRITE_CONTENT("cache.sprite.content"),               // Caches sprite loading and mipmaps

	FAST_RESOURCE_FILTER("misc.ResourceFilterMixin");           // Speeds up resource pack filters which block many paths with regular expressions

	public final String mixinContains;

	Option(String mixinContains) {
		this.mixinContains = mixinContains;
	}
}
