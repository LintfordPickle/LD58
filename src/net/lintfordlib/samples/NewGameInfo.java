package net.lintfordlib.samples;

import net.lintfordlib.GameInfo;
import net.lintfordlib.core.debug.Debug.DebugLogLevel;

public class NewGameInfo implements GameInfo {

	@Override
	public DebugLogLevel debugLogLevel() {
		return DebugLogLevel.off;
	}

	@Override
	public String applicationName() {
		return ConstantsGame.APPLICATION_NAME;
	}

	@Override
	public String windowTitle() {
		return ConstantsGame.WINDOW_TITLE;
	}

	@Override
	public int minimumWindowWidth() {
		return ConstantsGame.GAME_CANVAS_WIDTH;
	}

	@Override
	public int minimumWindowHeight() {
		return ConstantsGame.GAME_CANVAS_HEIGHT;
	}

	@Override
	public int gameCanvasResolutionWidth() {
		return ConstantsGame.GAME_CANVAS_WIDTH;
	}

	@Override
	public int gameCanvasResolutionHeight() {
		return ConstantsGame.GAME_CANVAS_HEIGHT;
	}

	@Override
	public int uiCanvasResolutionWidth() {
		return ConstantsGame.GAME_CANVAS_WIDTH;
	}
	
	@Override
	public int uiCanvasResolutionHeight() {
		return ConstantsGame.GAME_CANVAS_HEIGHT;
	}
	
	@Override
	public boolean stretchGameResolution() {
		return true;
	}

	@Override
	public boolean windowResizeable() {
		return true;
	}
}
