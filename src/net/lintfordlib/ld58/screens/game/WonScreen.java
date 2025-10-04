package net.lintfordlib.ld58.screens.game;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.ColorConstants;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.ld58.ConstantsGame;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.screens.MainMenu;
import net.lintfordlib.ld58.screens.menu.CreditsScreen;
import net.lintfordlib.screenmanager.MenuEntry;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.ScreenManagerConstants.FILLTYPE;
import net.lintfordlib.screenmanager.layouts.ListLayout;
import net.lintfordlib.screenmanager.screens.LoadingScreen;

public class WonScreen extends MenuScreen {

	// --------------------------------------
	// Constants
	// --------------------------------------

	private static final int SCREEN_BUTTON_NEXT = 10;
	private static final int SCREEN_BUTTON_RESTART = 12;
	private static final int SCREEN_BUTTON_EXIT = 13;

	// --------------------------------------
	// Variables
	// --------------------------------------

	private SceneHeader mSceneHeader;
	private GameOptions mGameOptions;
	private SpriteSheetDefinition mGameSpritesheetDef;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public WonScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions gameOptions) {
		super(screenManager, null);

		mSceneHeader = sceneHeader;
		mGameOptions = gameOptions;

		final var layout = new ListLayout(this);
		layout.layoutFillType(FILLTYPE.TAKE_WHATS_NEEDED);
		layout.setDrawBackground(false, ColorConstants.WHITE());
		layout.showTitle(true);
		layout.title("You love to see it!");

		// TODO: Next level
		final var nextButton = new MenuEntry(screenManager, this, "Next");
		nextButton.registerClickListener(this, SCREEN_BUTTON_NEXT);
		nextButton.enabled(false);

		final var restartButton = new MenuEntry(screenManager, this, "Restart");
		restartButton.registerClickListener(this, SCREEN_BUTTON_RESTART);

		final var exitToMenuButton = new MenuEntry(screenManager, this, "Exit");
		exitToMenuButton.registerClickListener(this, SCREEN_BUTTON_EXIT);

		layout.addMenuEntry(nextButton);
		layout.addMenuEntry(restartButton);
		layout.addMenuEntry(MenuEntry.menuSeparator());
		layout.addMenuEntry(exitToMenuButton);

		mLayouts.add(layout);

		mIsPopup = false;
		mShowBackgroundScreens = true;
		mESCBackEnabled = false;

		mBlockGamepadInputInBackground = true;
		mBlockKeyboardInputInBackground = true;
		mBlockMouseInputInBackground = true;

		mShowContextualKeyHints = false;

		mScreenPaddingTop = 120;
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mGameSpritesheetDef = resourceManager.spriteSheetManager().getSpriteSheet("SPRITESHEET_GAME", ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mGameSpritesheetDef = null;
	}

	@Override
	public void draw(LintfordCore core) {
		super.draw(core);

		if (mGameSpritesheetDef == null)
			return;

		final var lSpriteFrame = mGameSpritesheetDef.getSpriteFrame("WONTEXT");

		if (lSpriteFrame != null) {
			final var lTextureBatch = mRendererManager.sharedResources().uiSpriteBatch();
			lTextureBatch.setColorWhite();

			lTextureBatch.begin(core.gameCamera());
			lTextureBatch.draw(mGameSpritesheetDef, lSpriteFrame, -lSpriteFrame.width() * .5f, core.gameCamera().boundingRectangle().top() + 32, lSpriteFrame.width(), lSpriteFrame.height(), .1f);
			lTextureBatch.end();
		}
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected void handleOnClick() {
		switch (mClickAction.consume()) {
		case SCREEN_BUTTON_NEXT:
			exitScreen();
			return;

		case SCREEN_BUTTON_RESTART:
			final var lLoadingScreen = new GameScreen(screenManager, mSceneHeader, mGameOptions);
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, true, true, lLoadingScreen));
			break;

		case SCREEN_BUTTON_EXIT:
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, false, false, new CreditsScreen(screenManager), new MainMenu(screenManager)));
			break;

		}
	}
}
