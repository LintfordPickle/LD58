package net.lintfordlib.ld58.screens.menu;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.textures.Texture;
import net.lintfordlib.screenmanager.Screen;
import net.lintfordlib.screenmanager.ScreenManager;

public class MainMenuBackground extends Screen {

	// ---------------------------------------------
	// Variables
	// ---------------------------------------------

	private Texture mBackgroundTexture;

	// ---------------------------------------------
	// Constructor
	// ---------------------------------------------

	public MainMenuBackground(ScreenManager screenManager) {
		super(screenManager);

		screenManager.core().createNewGameCamera();
	}

	// ---------------------------------------------
	// Core-Methods
	// ---------------------------------------------

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mBackgroundTexture = resourceManager.textureManager().loadTexture("TEXTURE_MENU_BACKGROUND", "res/textures/textureMainMenuScreen.png", entityGroupUid());
		mCoreSpritesheet = resourceManager.spriteSheetManager().coreSpritesheet();
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mBackgroundTexture = null;
		mCoreSpritesheet = null;
	}

	@Override
	public void draw(LintfordCore core) {
		super.draw(core);

		final var canvasBox = core.gameCamera().boundingRectangle();
		final var textureBatch = rendererManager().sharedResources().uiSpriteBatch();

		core.gameCamera().update(core);
		core.config().display().reapplyGlViewport();

		final var srcWidth = mBackgroundTexture.getTextureWidth();
		final var srcHeight = mBackgroundTexture.getTextureHeight();

		textureBatch.setColorWhite();
		textureBatch.begin(core.gameCamera());
		textureBatch.draw(mBackgroundTexture, 0, 0, srcWidth, srcHeight, canvasBox.left(), canvasBox.top(), canvasBox.width(), canvasBox.height(), .85f);
		textureBatch.end();
	}
}
