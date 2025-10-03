package net.lintfordlib.samples.screens.menu;

import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;

public class CreditsScreen extends MenuScreen {

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public CreditsScreen(ScreenManager screenManager) {
		super(screenManager, null);

		mIsPopup = false;
		mShowBackgroundScreens = true;
		mESCBackEnabled = false;
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void draw(LintfordCore core) {
		super.draw(core);

		final var lFontUnit = mRendererManager.sharedResources().uiTextFont();

		final var lHudBounds = core.HUD().boundingRectangle();
		final var lTextPadding = 2.f;

		lFontUnit.begin(core.HUD());
		lFontUnit.drawText("Created by LintfordPickle", lHudBounds.left() + lTextPadding, lHudBounds.bottom() - lFontUnit.fontHeight() - lTextPadding, .01f, 1.f);
		lFontUnit.end();
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected void handleOnClick() {
		// ignored
	}
}
