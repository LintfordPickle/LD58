package net.lintfordlib.samples.screens.menu;

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

		final var lCanvasBox = core.gameCamera().boundingRectangle();
		final var lTextureBatch = rendererManager().sharedResources().uiSpriteBatch();

		core.gameCamera().update(core);
		core.config().display().reapplyGlViewport();

		lTextureBatch.setColorWhite();
		lTextureBatch.begin(core.gameCamera());
		lTextureBatch.draw(mBackgroundTexture, 0, 0, 960, 576, lCanvasBox.left(), lCanvasBox.top(), lCanvasBox.width(), lCanvasBox.height(), .85f);
		lTextureBatch.end();
	}
}
