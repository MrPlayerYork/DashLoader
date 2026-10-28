package dev.notalpha.dashloader.dev.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Takes a picture of the game after the world has been up for a while, then quits. Used to compare what is drawn with and without
 * DashLoader. Only applied when DASHLOADER_SHOT is set; its value is the name of the file (in the game folder, as png).
 * The picture is made by the game itself, so it only has the game window in it.
 */
@Mixin(Minecraft.class)
public abstract class ScreenshotMixin {
	@Shadow
	public ClientLevel level;
	@Shadow
	public LocalPlayer player;
	@Shadow
	public GameRenderer gameRenderer;
	@Shadow
	@org.spongepowered.asm.mixin.Final
	public File gameDirectory;

	@Shadow
	public abstract void stop();

	@Unique
	private int dashloader$ticks;

	@Inject(method = "tick", at = @At("TAIL"))
	private void dashloaderShot(CallbackInfo ci) {
		if (this.level == null || this.player == null) {
			return;
		}
		this.dashloader$ticks++;
		if (this.dashloader$ticks == 600) {
			Screenshot.grab(this.gameDirectory, System.getenv("DASHLOADER_SHOT") + ".png", this.gameRenderer.mainRenderTarget(), 1, message -> {
			});
		} else if (this.dashloader$ticks == 700) {
			this.stop();
		}
	}
}
