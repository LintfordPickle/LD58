package net.lintfordlib.ld58.screens.menu;

import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.ColorHelper;
import net.lintfordlib.screenmanager.MenuScreen;
import net.lintfordlib.screenmanager.ScreenManager;

public class CreditsScreen extends MenuScreen {

	// --------------------------------------
	// Variables
	// --------------------------------------

	// HSV color cycle
	private float[] hsvColors = new float[3];
	private float textHsv;

	// Marquee scroller
	private String mLdMessage = "Created by LintfordPickle LD58 - Thanks for playing! ";
	private int mScrollIndex = 0;
	private final int mVisibleLength = 25;
	private float mLastUpdate = 0;
	private int mScrollDelay = 150;
	private int mPauseDelay = 5000;
	private boolean mInPause = false;
	private char[] mCharBuffer = new char[mVisibleLength];

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
	public void update(LintfordCore core, boolean otherScreenHasFocus, boolean coveredByOtherScreen) {
		super.update(core, otherScreenHasFocus, coveredByOtherScreen);

		updateMarqueText(core);
	}

	@Override
	public void draw(LintfordCore core) {
		super.draw(core);

		final var lFontUnit = mRendererManager.sharedResources().uiTextFont();

		final var lHudBounds = core.HUD().boundingRectangle();
		final var lTextPadding = 2.f;

		final var dt = core.gameTime().elapsedTimeMilli() * .001f;
		final var speed = 110.f; // scroll speed: 110 degrees per second

		textHsv += dt * speed;
		textHsv %= 360.0f; // hue is a color wheel

		ColorHelper.hsvToRgb(textHsv, 1.0f, 1.0f, hsvColors);

		lFontUnit.setTextColorRGB(hsvColors[0], hsvColors[1], hsvColors[2]);
		lFontUnit.begin(core.HUD());
		lFontUnit.drawText(getMessage(), lHudBounds.left() + lTextPadding, lHudBounds.bottom() - lFontUnit.fontHeight() - lTextPadding, .01f, 1.f);
		lFontUnit.end();
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	private void updateMarqueText(LintfordCore core) {
		final var currentTime = (float) core.gameTime().totalTimeMilli();

		if (mInPause) {
			// Wait until pause time has elapsed
			if (currentTime - mLastUpdate > mPauseDelay) {
				mInPause = false;
				mLastUpdate = currentTime;
			}

			return;
		}

		if (currentTime - mLastUpdate > mScrollDelay) {
			mScrollIndex++;
			mLastUpdate = currentTime;

			if (mScrollIndex >= mLdMessage.length()) {
				// completed one full revolution
				mScrollIndex = 0;
				mInPause = true;
			}
		}

	}

	public String getMessage() {
		for (int i = 0; i < mVisibleLength; i++) {
			mCharBuffer[i] = mLdMessage.charAt((mScrollIndex + i) % mLdMessage.length());
		}
		return new String(mCharBuffer); // one allocation here
	}

	@Override
	protected void handleOnClick() {
		// ignored
	}
}
