package net.lintfordlib.ld58.screens.game;

import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.ColorConstants;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.ld58.ConstantsGame;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.IResetLevel;
import net.lintfordlib.ld58.screens.MainMenu;
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

	private static final String[] WIN_QUOTES = new String[] { "YOU LOVE TO SEE IT!", "YOU CORKER!", "CRACKING!", "GET IN!", "YOU BEAUT!" };

	private static final int SCREEN_BUTTON_NEXT = 10;
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

	public WonScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions gameOptions, IResetLevel resetter) {
		super(screenManager, null);

		mSceneHeader = sceneHeader;
		mGameOptions = gameOptions;
		mLevelResetter = resetter;

		final var layout = new ListLayout(this);
		layout.layoutFillType(FILLTYPE.TAKE_WHATS_NEEDED);
		layout.setDrawBackground(false, ColorConstants.WHITE());

		final var nextLevelAvailable = gameOptions.levelNumber < ConstantsGame.NUM_LEVELS;

		final var nextButton = new MenuEntry(screenManager, this, "Next");
		nextButton.registerClickListener(this, SCREEN_BUTTON_NEXT);
		nextButton.enabled(nextLevelAvailable);

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
	public void draw(LintfordCore core) {
		super.draw(core);

		final var fontUnit = mRendererManager.sharedResources().uiHeaderFont();
		fontUnit.begin(core.gameCamera());
		fontUnit.setTextColorWhite();
		final var quote = WIN_QUOTES[0];
		final var quoteWidth = fontUnit.getStringWidth(quote);
		fontUnit.drawText(quote, 0 - quoteWidth / 2, -70, 1.f, 1.f);
		fontUnit.end();

	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected void handleOnClick() {
		switch (mClickAction.consume()) {
		case SCREEN_BUTTON_NEXT: {
			mGameOptions.levelNumber++;
			final var lLoadingScreen = new GameScreen(screenManager, mSceneHeader, mGameOptions);
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, true, true, lLoadingScreen));
		}
			return;

		case SCREEN_BUTTON_RESTART: {
			if (mLevelResetter != null) {
				mLevelResetter.resetLevel();
				exitScreen();
			} else {
				final var lNewGameScreen = new GameScreen(screenManager, mSceneHeader, mGameOptions);
				screenManager.createLoadingScreen(new LoadingScreen(screenManager, true, true, lNewGameScreen));
			}
		}
			break;

		case SCREEN_BUTTON_EXIT: {
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, false, false, new MainMenu(screenManager)));
		}
			break;

		}
	}
}
