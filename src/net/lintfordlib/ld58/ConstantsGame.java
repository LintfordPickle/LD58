package net.lintfordlib.ld58;

import net.lintfordlib.assets.ResourceGroupProvider;

public class ConstantsGame {

	// ---------------------------------------------
	// Setup
	// ---------------------------------------------

	public static final String FOOTER_TEXT = "(c) 2025 LintfordPickle";

	public static final String APPLICATION_NAME = "LD58";
	public static final String WINDOW_TITLE = "LD58";

	public static final int GAME_CANVAS_SCALE = 1;
	public static final int GAME_CANVAS_WIDTH = 320 * GAME_CANVAS_SCALE;
	public static final int GAME_CANVAS_HEIGHT = 240 * GAME_CANVAS_SCALE;

	public static final float ASPECT_RATIO = (float) GAME_CANVAS_WIDTH / (float) GAME_CANVAS_HEIGHT;

	public static final int GAME_RESOURCE_GROUP_ID = ResourceGroupProvider.getRollingEntityNumber();

	// ---------------------------------------------
	// Game
	// ---------------------------------------------

	public static final int NUM_LEVELS = 2;
	public static final boolean LOCK_ZOOM_TO_ONE = true;

	// ---------------------------------------------
	// Debug
	// ---------------------------------------------

	public static final boolean IS_DEBUG_MODE = false;
	public static final boolean STOP_ON_BACKWARDS = IS_DEBUG_MODE && true;
	public static final boolean ENABLED_AUTOWALK = true;

	public static final boolean START_GAME_IMMEDIATELY = false;

}
