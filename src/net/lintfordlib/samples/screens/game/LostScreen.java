package net.lintfordlib.samples.screens.game;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.ColorConstants;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.samples.ConstantsGame;
import net.lintfordlib.samples.data.GameOptions;
import net.lintfordlib.samples.screens.MainMenu;
import net.lintfordlib.samples.screens.menu.CreditsScreen;
import net.lintfordlib.screenmanager.MenuEntry;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.ScreenManagerConstants.FILLTYPE;
import net.lintfordlib.screenmanager.layouts.ListLayout;
import net.lintfordlib.screenmanager.screens.LoadingScreen;

public class LostScreen extends MenuScreen {

	// --------------------------------------
	// Constants
	// --------------------------------------

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

	public LostScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions gameOptions) {
		super(screenManager, null);

		mSceneHeader = sceneHeader;
		mGameOptions = gameOptions;

		final var lLayout = new ListLayout(this);
		lLayout.layoutFillType(FILLTYPE.TAKE_WHATS_NEEDED);
		lLayout.setDrawBackground(true, ColorConstants.WHITE());
		lLayout.showTitle(true);
		lLayout.title("You hate to see it!");

		final var lRestartEntry = new MenuEntry(screenManager, this, "Restart");
		lRestartEntry.registerClickListener(this, SCREEN_BUTTON_RESTART);

		final var lExitToMenuEntry = new MenuEntry(screenManager, this, "Exit");
		lExitToMenuEntry.registerClickListener(this, SCREEN_BUTTON_EXIT);

		lLayout.addMenuEntry(lRestartEntry);
		lLayout.addMenuEntry(MenuEntry.menuSeparator());
		lLayout.addMenuEntry(lExitToMenuEntry);

		mLayouts.add(lLayout);

		mIsPopup = false;
		mShowBackgroundScreens = true;

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

		final var lTextureBatch = mRendererManager.sharedResources().uiSpriteBatch();
		final var lSpriteFramef = mGameSpritesheetDef.getSpriteFrame("LOSTTEXT");

		lTextureBatch.setColorWhite();

		lTextureBatch.begin(core.gameCamera());
		lTextureBatch.draw(mGameSpritesheetDef, lSpriteFramef, -lSpriteFramef.width() * .5f, core.gameCamera().boundingRectangle().top() + 32, lSpriteFramef.width(), lSpriteFramef.height(), .1f);
		lTextureBatch.end();

	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected void handleOnClick() {
		switch (mClickAction.consume()) {
		case SCREEN_BUTTON_RESTART:

			final var lNewGameScreen = new GameScreen(screenManager, mSceneHeader, mGameOptions);
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, true, true, lNewGameScreen));
			break;

		case SCREEN_BUTTON_EXIT:
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, false, false, new CreditsScreen(screenManager), new MainMenu(screenManager)));
			break;

		}
	}
}
