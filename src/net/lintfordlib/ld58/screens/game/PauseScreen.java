package net.lintfordlib.ld58.screens.game;

import net.lintfordlib.core.graphics.ColorConstants;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.IResetLevel;
import net.lintfordlib.ld58.screens.MainMenu;
import net.lintfordlib.ld58.screens.menu.MainMenuBackground;
import net.lintfordlib.ld58.screens.menu.OptionsScreen;
import net.lintfordlib.screenmanager.MenuEntry;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.ScreenManagerConstants.FILLTYPE;
import net.lintfordlib.screenmanager.layouts.ListLayout;
import net.lintfordlib.screenmanager.screens.LoadingScreen;

public class PauseScreen extends MenuScreen {

	// --------------------------------------
	// Constants
	// --------------------------------------

	private static final int SCREEN_BUTTON_CONTINUE = 10;
	private static final int SCREEN_BUTTON_OPTIONS = 11;
	private static final int SCREEN_BUTTON_RESTART = 12;
	private static final int SCREEN_BUTTON_EXIT = 13;

	// --------------------------------------
	// Variables
	// --------------------------------------

	private SceneHeader mSceneHeader;
	private GameOptions mGameOptions;
	private IResetLevel mLevelResetter;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public PauseScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions gameOptions, IResetLevel resetter) {
		super(screenManager, null);

		mSceneHeader = sceneHeader;
		mGameOptions = gameOptions;
		mLevelResetter = resetter;

		final var lLayout = new ListLayout(this);
		lLayout.layoutFillType(FILLTYPE.TAKE_WHATS_NEEDED);
		lLayout.setDrawBackground(false, ColorConstants.WHITE());
		lLayout.showTitle(true);
		lLayout.title("Paused");

		final var lContinueEntry = new MenuEntry(screenManager, this, "Continue");
		lContinueEntry.registerClickListener(this, SCREEN_BUTTON_CONTINUE);

		final var lOptionsEntry = new MenuEntry(screenManager, this, "Options");
		lOptionsEntry.registerClickListener(this, SCREEN_BUTTON_OPTIONS);

		final var lRestartEntry = new MenuEntry(screenManager, this, "Restart");
		lRestartEntry.registerClickListener(this, SCREEN_BUTTON_RESTART);

		final var lExitToMenuEntry = new MenuEntry(screenManager, this, "Exit");
		lExitToMenuEntry.registerClickListener(this, SCREEN_BUTTON_EXIT);

		lLayout.addMenuEntry(lContinueEntry);
		lLayout.addMenuEntry(lRestartEntry);
		lLayout.addMenuEntry(MenuEntry.menuSeparator());
		lLayout.addMenuEntry(lOptionsEntry);
		lLayout.addMenuEntry(MenuEntry.menuSeparator());
		lLayout.addMenuEntry(lExitToMenuEntry);

		mLayouts.add(lLayout);

		mIsPopup = true;
		mShowBackgroundScreens = true;

		mBlockGamepadInputInBackground = true;
		mBlockKeyboardInputInBackground = true;
		mBlockMouseInputInBackground = true;

		mShowContextualKeyHints = false;
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected void handleOnClick() {
		switch (mClickAction.consume()) {
		case SCREEN_BUTTON_CONTINUE:
			exitScreen();
			return;

		case SCREEN_BUTTON_OPTIONS:
			screenManager.addScreen(new OptionsScreen(screenManager));
			break;

		case SCREEN_BUTTON_RESTART:
			if (mLevelResetter != null) {
				mLevelResetter.resetLevel();
				exitScreen();
			} else {
				final var lNewGameScreen = new GameScreen(screenManager, mSceneHeader, mGameOptions);
				screenManager.createLoadingScreen(new LoadingScreen(screenManager, true, true, lNewGameScreen));
			}
			break;

		case SCREEN_BUTTON_EXIT:
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, false, false, new MainMenuBackground(screenManager), new MainMenu(screenManager)));
			break;

		}
	}
}
