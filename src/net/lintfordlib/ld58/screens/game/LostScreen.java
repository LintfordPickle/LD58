package net.lintfordlib.ld58.screens.game;

import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.ColorConstants;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.IResetLevel;
import net.lintfordlib.ld58.screens.MainMenu;
import net.lintfordlib.ld58.screens.menu.CreditsScreen;
import net.lintfordlib.screenmanager.MenuEntry;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.ScreenManagerConstants.FILLTYPE;
import net.lintfordlib.screenmanager.ScreenManagerConstants.LAYOUT_WIDTH;
import net.lintfordlib.screenmanager.layouts.ListLayout;
import net.lintfordlib.screenmanager.screens.LoadingScreen;

public class LostScreen extends MenuScreen {

	// --------------------------------------
	// Constants
	// --------------------------------------

	private static final String[] LOST_QUOTES = new String[] { "NEVER MIND!" };

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

	public LostScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions gameOptions, IResetLevel levelResetter) {
		super(screenManager, null);

		mSceneHeader = sceneHeader;
		mGameOptions = gameOptions;
		mLevelResetter = levelResetter;

		final var lLayout = new ListLayout(this);
		lLayout.layoutFillType(FILLTYPE.TAKE_WHATS_NEEDED);
		lLayout.layoutWidth(LAYOUT_WIDTH.FULL);
		lLayout.setDrawBackground(false, ColorConstants.WHITE());

		final var lRestartEntry = new MenuEntry(screenManager, this, "Restart");
		lRestartEntry.registerClickListener(this, SCREEN_BUTTON_RESTART);
		lRestartEntry.height(50);
		lRestartEntry.desiredHeight(50);

		final var lExitToMenuEntry = new MenuEntry(screenManager, this, "Exit");
		lExitToMenuEntry.registerClickListener(this, SCREEN_BUTTON_EXIT);
		lExitToMenuEntry.height(50);
		lExitToMenuEntry.desiredHeight(50);

		lLayout.addMenuEntry(lRestartEntry);
		lLayout.addMenuEntry(MenuEntry.menuSeparator());
		lLayout.addMenuEntry(lExitToMenuEntry);

		mLayouts.add(lLayout);

		mIsPopup = false;
		mShowBackgroundScreens = true;
		mESCBackEnabled = true;

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
		final var quote = LOST_QUOTES[0];
		final var quoteWidth = fontUnit.getStringWidth(quote);
		fontUnit.drawText(quote, 0 - quoteWidth / 2, -70, 1.f, 1.f);
		fontUnit.end();

	}

	@Override
	protected void onEscPressed() {
		super.onEscPressed();

		if (mLevelResetter != null) {
			mLevelResetter.resetLevel();
			exitScreen();
		}
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected void handleOnClick() {
		switch (mClickAction.consume()) {
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
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, false, false, new CreditsScreen(screenManager), new MainMenu(screenManager)));
			break;

		}
	}
}
