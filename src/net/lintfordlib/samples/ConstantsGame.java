package net.lintfordlib.samples;

import net.lintfordlib.assets.ResourceGroupProvider;

public class ConstantsGame {

	// ---------------------------------------------
	// Setup
	// ---------------------------------------------

	public static final String FOOTER_TEXT = "(c) 2025 LintfordPickle";

	public static final String APPLICATION_NAME = "LintfordLib Sample";
	public static final String WINDOW_TITLE = "LintfordLib Sample";

	public static final float ASPECT_RATIO = 16.f / 9.f;

	public static final int GAME_CANVAS_WIDTH = 960;
	public static final int GAME_CANVAS_HEIGHT = 576;

	public static final int GAME_RESOURCE_GROUP_ID = ResourceGroupProvider.getRollingEntityNumber();

	// ---------------------------------------------
	// Game
	// ---------------------------------------------

	public static final boolean LOCK_ZOOM_TO_ONE = true;

	// ---------------------------------------------
	// Debug
	// ---------------------------------------------

	public static final boolean IS_DEBUG_MODE = true;
	public static final boolean IS_DEBUG_RENDERING_MODE = true;

	public static final boolean IS_AI_ENABLED = true;
	public static final boolean CAMERA_DEBUG_MODE = true;

}
