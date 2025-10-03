package net.lintfordlib.samples.screens.menu;

import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.ColorConstants;
import net.lintfordlib.samples.data.GameOptions;
import net.lintfordlib.samples.data.SampleSceneHeader;
import net.lintfordlib.samples.screens.game.GameScreen;
import net.lintfordlib.screenmanager.MenuEntry;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.ScreenManagerConstants.FILLTYPE;
import net.lintfordlib.screenmanager.ScreenManagerConstants.LAYOUT_ALIGNMENT;
import net.lintfordlib.screenmanager.ScreenManagerConstants.LAYOUT_WIDTH;
import net.lintfordlib.screenmanager.entries.MenuEnumEntry;
import net.lintfordlib.screenmanager.layouts.ListLayout;
import net.lintfordlib.screenmanager.screens.LoadingScreen;

public class SelectScreen extends MenuScreen {

	// ---------------------------------------------
	// Constants
	// ---------------------------------------------

	private static final int BUTTON_PLAY = 11;
	private static final int BUTTON_BACK = 12;

	private final MenuEnumEntry mDifficulty;

	// ---------------------------------------------
	// Constructors
	// ---------------------------------------------

	public SelectScreen(ScreenManager pScreenManager) {
		super(pScreenManager, null);

		final var lLayout = new ListLayout(this);
		lLayout.setDrawBackground(true, ColorConstants.WHITE());
		lLayout.layoutWidth(LAYOUT_WIDTH.HALF);
		lLayout.layoutFillType(FILLTYPE.TAKE_WHATS_NEEDED);

		lLayout.showTitle(true);
		lLayout.title("Select Parameters");
		lLayout.cropPaddingTop(10.f);
		lLayout.cropPaddingBottom(10.f);

		mDifficulty = new MenuEnumEntry(pScreenManager, this, "Difficulty");
		mDifficulty.addItems("Easy", "Normal", "Hard");
		mDifficulty.horizontalFillType(FILLTYPE.FILL_CONTAINER);
		mDifficulty.setButtonsEnabled(true);
		mDifficulty.showInfoButton(true);
		mDifficulty.setToolTip("Sets the game difficulty.");

		final var lStartGameEntry = new MenuEntry(screenManager, this, "Play");
		lStartGameEntry.horizontalFillType(FILLTYPE.FILL_CONTAINER);
		lStartGameEntry.registerClickListener(this, BUTTON_PLAY);

		final var lBackEntry = new MenuEntry(screenManager, this, "Back");
		lBackEntry.horizontalFillType(FILLTYPE.FILL_CONTAINER);
		lBackEntry.registerClickListener(this, BUTTON_BACK);

		lLayout.addMenuEntry(mDifficulty);
		lLayout.addMenuEntry(MenuEntry.menuSeparator());
		lLayout.addMenuEntry(lStartGameEntry);
		lLayout.addMenuEntry(MenuEntry.menuSeparator());
		lLayout.addMenuEntry(lBackEntry);

		mScreenPaddingTop = 30.f;
		mLayoutPaddingHorizontal = 50.f;
		mLayoutAlignment = LAYOUT_ALIGNMENT.LEFT;

		mShowBackgroundScreens = false;

		mLayouts.add(lLayout);
	}

	// ---------------------------------------------
	// Core-Methods
	// ---------------------------------------------

	@Override
	public void update(LintfordCore core, boolean otherScreenHasFocus, boolean coveredByOtherScreen) {
		super.update(core, otherScreenHasFocus, coveredByOtherScreen);
	}

	@Override
	public void draw(LintfordCore core) {
		super.draw(core);
	}

	// ---------------------------------------------
	// Methods
	// ---------------------------------------------

	@Override
	protected void handleOnClick() {
		switch (mClickAction.consume()) {
		case BUTTON_PLAY:

			final var gameOptions = new GameOptions();

			// Set the game options before instantiating the GameScreen.

			final var lNewSceneHeader = new SampleSceneHeader("Sample Game");

			final var lNewGameScreen = new GameScreen(screenManager, lNewSceneHeader, gameOptions);
			screenManager.createLoadingScreen(new LoadingScreen(screenManager, true, true, lNewGameScreen));
			break;

		case BUTTON_BACK:
			exitScreen();
			break;
		}
	}
}
