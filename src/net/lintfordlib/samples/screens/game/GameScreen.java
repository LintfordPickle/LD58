package net.lintfordlib.samples.screens.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.textures.Texture;
import net.lintfordlib.data.DataManager;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.renderers.SimpleRendererManager;
import net.lintfordlib.samples.ConstantsGame;
import net.lintfordlib.samples.NewGameKeyActions;
import net.lintfordlib.samples.controllers.AnimationController;
import net.lintfordlib.samples.controllers.GameStateController;
import net.lintfordlib.samples.data.GameOptions;
import net.lintfordlib.samples.data.GameWorld;
import net.lintfordlib.samples.data.IGameStateListener;
import net.lintfordlib.samples.renderers.AnimationRenderer;
import net.lintfordlib.samples.renderers.HudRenderer;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.screens.BaseGameScreen;

public class GameScreen extends BaseGameScreen implements IGameStateListener {

	// --------------------------------------
	// Variables
	// --------------------------------------

	private SceneHeader mSceneHeader;
	private GameOptions mGameOptions;

	// Data
	private GameWorld mGameWorld; // reference to data related to the scene

	private Texture mGameBackgroundTexture;

	// Controllers
	private GameStateController mGameStateController;
	private AnimationController mAnimationController;

	// Renderers
	private AnimationRenderer mAnimationRenderer;
	private HudRenderer mHudRenderer;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions options) {
		super(screenManager, new SimpleRendererManager(screenManager.core(), ConstantsGame.GAME_RESOURCE_GROUP_ID));

		mSceneHeader = sceneHeader;
		mGameOptions = options;
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mGameBackgroundTexture = resourceManager.textureManager().getTexture("TEXTURE_GAME_BACKGROUND", ConstantsGame.GAME_RESOURCE_GROUP_ID);

	}

	@Override
	public void handleInput(LintfordCore core) {
		super.handleInput(core);

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_ESCAPE, this) || core.input().gamepads().isGamepadButtonDownTimed(GLFW.GLFW_GAMEPAD_BUTTON_START, this)) {
			screenManager.addScreen(new PauseScreen(screenManager, mSceneHeader, mGameOptions));
			return;
		}

		// For simple games you could add code to handle the player input here.
		// However usually, components would be updated in dedicated BaseControllers (see the CONTROLLERS Section below).

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_SPACE, this)) {
			// Game-specific controller abstracts the animation logic and lets you just play 'explosion'.
			mAnimationController.playBigExplosionAnimation(0, 0);
		}

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_LEFT_CONTROL, this)) {
			// Game-specific controller abstracts the animation logic and let you play a named animation frame from a spritesheet.
			mAnimationController.playAnimationByName("explosion", (float) Math.cos(core.gameTime().totalTimeSeconds()) * 100.f, (float) Math.sin(core.gameTime().totalTimeSeconds()) * 100.f);
		}
	}

	@Override
	public void update(LintfordCore core, boolean otherScreenHasFocus, boolean coveredByOtherScreen) {
		super.update(core, otherScreenHasFocus, coveredByOtherScreen);

		// For simple games you could add code to perform the game update logic here.
		// However usually, components would be updated in dedicated BaseControllers (see the CONTROLLERS Section below).

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(NewGameKeyActions.KEY_BINDING_PRIMARY_FIRE)) {
			screenManager.toastManager().addMessage(getClass().getSimpleName(), "PRIMARY FIRE", 1500);
		}

	}

	@Override
	public void draw(LintfordCore core) {

		GL11.glClearColor(0.08f, .02f, 0.03f, 1.f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		final var lTextureBatch = rendererManager().sharedResources().uiSpriteBatch();
		lTextureBatch.setColorWhite();

		final var sx = 0.f;
		final var sy = 0.f;
		final var sw = 960.f;
		final var sh = 540.f;

		final var dstx = -960.f / 2.f;
		final var dsty = -540.f / 2.f;
		final var dstw = 960.f;
		final var dsth = 540.f;

		final var zDepth = 1.f;

		lTextureBatch.begin(core.gameCamera());
		lTextureBatch.draw(mGameBackgroundTexture, sx, sy, sw, sh, dstx, dsty, dstw, dsth, zDepth);
		lTextureBatch.end();

		super.draw(core);

		// For simple games you could add code to render a basic scene directly here.
		// However usually, the rendering would be performed by a specific BaseRenderer (see the RENDERERS section below).

	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	// DATA ----------------------------------------

	@Override
	protected void createData(DataManager dataManager) {
		mGameWorld = new GameWorld();
	}

	// CONTROLLERS ---------------------------------

	@Override
	protected void createControllers(ControllerManager controllerManager) {
		mGameStateController = new GameStateController(controllerManager, ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mAnimationController = new AnimationController(controllerManager, ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mGameStateController.setGameStateListener(this);
	}

	@Override
	protected void initializeControllers(LintfordCore core) {
		mGameStateController.initialize(core);
		mAnimationController.initialize(core);
	}

	// RENDERERS -----------------------------------

	@Override
	protected void createRenderers(LintfordCore core) {
		mAnimationRenderer = new AnimationRenderer(mRendererManager, mAnimationController, ConstantsGame.GAME_RESOURCE_GROUP_ID);
		mHudRenderer = new HudRenderer(mRendererManager, ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

	@Override
	protected void createRendererStructure(LintfordCore core) {
		mRendererManager.addRenderer(mAnimationRenderer);
		mRendererManager.addRenderer(mHudRenderer);

	}

	// --------------------------------------
	// Callback-Methods
	// --------------------------------------

	@Override
	public void onGameWon() {
		screenManager.addScreen(new WonScreen(screenManager, mSceneHeader, mGameOptions));
	}

	@Override
	public void onGameLost() {
		screenManager.addScreen(new LostScreen(screenManager, mSceneHeader, mGameOptions));
	}
}
