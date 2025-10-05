package net.lintfordlib.ld58.renderers;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.fonts.CharAtlasRenderer;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.core.graphics.textures.Texture;
import net.lintfordlib.core.rendering.RenderPass;
import net.lintfordlib.ld58.ConstantsGame;
import net.lintfordlib.ld58.controllers.GameStateController;
import net.lintfordlib.ld58.data.GameState;
import net.lintfordlib.ld58.data.HudTextureNames;
import net.lintfordlib.renderers.BaseRenderer;
import net.lintfordlib.renderers.RendererManagerBase;

public class HudRenderer extends BaseRenderer {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String RENDERER_NAME = "Hud Renderer";

	private GameStateController mGameStateController;
	private SpriteSheetDefinition mHudSpriteSheet;
	private CharAtlasRenderer mCharAtlasRenderer;
	private Texture mDigitsTexture;

	// --------------------------------------
	// Properties
	// --------------------------------------

	@Override
	public boolean isInitialized() {
		return true;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public HudRenderer(RendererManagerBase rendererManager, int entityGroupUid) {
		super(rendererManager, RENDERER_NAME, entityGroupUid);

		mCharAtlasRenderer = new CharAtlasRenderer();

	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void initialize(LintfordCore core) {
		super.initialize(core);

		final var controllerManager = core.controllerManager();
		mGameStateController = (GameStateController) controllerManager.getControllerByNameRequired(GameStateController.CONTROLLER_NAME, ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mHudSpriteSheet = resourceManager.spriteSheetManager().getSpriteSheet("SPRITESHEET_HUD", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mDigitsTexture = resourceManager.textureManager().getTexture("TEXTURE_DIGITS", ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mCharAtlasRenderer.textureAtlas(mDigitsTexture);

	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mHudSpriteSheet = null;
		mDigitsTexture = null;
	}

	@Override
	public boolean handleInput(LintfordCore core) {
		return super.handleInput(core);

		// Handle any hud input actions
	}

	@Override
	public void update(LintfordCore core) {
		super.update(core);

		// Update hud elements
	}

	@Override
	public void draw(LintfordCore core, RenderPass renderPass) {
		final var hudBounds = core.gameCamera().boundingRectangle();
		final var textureBatch = mRendererManager.sharedResources().uiSpriteBatch();
		final var healthFrame = mHudSpriteSheet.getSpriteFrame(HudTextureNames.HEALTH);

		final var gameState = mGameStateController.gameState();

		textureBatch.begin(core.gameCamera());
		textureBatch.setColorWhite();

		int lives = GameState.MAX_HEALTH;
		for (int i = 0; i < lives; i++) {
			float xx = hudBounds.left() + 10 + i * 18;
			float yy = hudBounds.top() + 10;

			if (i < gameState.health()) {
				textureBatch.setColorWhite();
			} else {
				textureBatch.setColorBlack();
			}

			textureBatch.draw(mHudSpriteSheet, healthFrame, xx, yy, 16, 16, 5.f);
		}

		textureBatch.setColorWhite();

		// Distance
		final var distTotal = gameState.trackLength();
		final var distTotalWidth = String.valueOf((int) distTotal).length() * 16 * .3f;
		mCharAtlasRenderer.drawNumber(textureBatch, (int) distTotal, hudBounds.centerX() + 160 - 10 - distTotalWidth, hudBounds.top() + 20, 0.3f, .3f);

		final var distTravelled = gameState.playerDistance();
		final var distTravelledWidth = String.valueOf((int) distTravelled).length() * 16 * .8f;
		mCharAtlasRenderer.drawNumber(textureBatch, (int) distTravelled, hudBounds.centerX() + 160 - 20 - distTravelledWidth - distTotalWidth, hudBounds.top() + 12, 0.3f, .8f);

		// Score
		final var score = gameState.getScore();
		final var scoreWidth = String.valueOf(score).length() * 16;

		mCharAtlasRenderer.drawNumber(textureBatch, score, hudBounds.centerX() - scoreWidth / 2, hudBounds.top() + 11, 0.3f, .9f);

		// Speed
		final var speed = gameState.speed();
		mCharAtlasRenderer.drawNumber(textureBatch, (int) speed, hudBounds.left() + 10, hudBounds.top() + 32, 0.3f, .5f);

		textureBatch.end();
	}
}
