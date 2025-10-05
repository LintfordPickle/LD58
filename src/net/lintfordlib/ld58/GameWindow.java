package net.lintfordlib.ld58;

import net.lintfordlib.GameInfo;
import net.lintfordlib.GameVersion;
import net.lintfordlib.assets.ResourceLoader;
import net.lintfordlib.controllers.music.MusicController;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.fonts.BitmapFontManager;
import net.lintfordlib.core.input.KeyEventActionManager;
import net.lintfordlib.core.rendering.SharedResources;
import net.lintfordlib.ld58.controllers.SoundFxController;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.SampleSceneHeader;
import net.lintfordlib.ld58.screens.MainMenu;
import net.lintfordlib.ld58.screens.game.GameScreen;
import net.lintfordlib.ld58.screens.menu.MainMenuBackground;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.toast.ToastManager;

public class GameWindow extends LintfordCore {

	private final int APP_VERSION_MAJ = 0;
	private final int APP_VERSION_MIN = 1;
	private final int APP_VERSION_BUILD = 1;

	private final String APP_POSTFIX = "20251004";

	private void setGameVersion() {
		GameVersion.setGameVersion(APP_VERSION_MAJ, APP_VERSION_MIN, APP_VERSION_BUILD, APP_POSTFIX);
	}

	// ---------------------------------------------
	// Entry Point
	// ---------------------------------------------

	public static void main(String[] args) {
		new GameWindow(new LD58WindowsInfo(), args).createWindow();
	}

	// ---------------------------------------------
	// Variables
	// ---------------------------------------------

	protected int mEntityGroupID;

	protected LD58KeyActions mGameKeyActions;
	protected ResourceLoader mGameResourceLoader;
	protected ScreenManager mScreenManager;

	// ---------------------------------------------
	// Properties
	// ---------------------------------------------

	public ScreenManager screenManager() {
		return mScreenManager;
	}

	// ---------------------------------------------
	// Constructor
	// ---------------------------------------------

	public GameWindow(GameInfo pGameInfo, String[] pArgs) {
		super(pGameInfo, pArgs, false);

		setGameVersion();

		mEntityGroupID = ConstantsGame.GAME_RESOURCE_GROUP_ID;
		mIsFixedTimeStep = true;

		mScreenManager = new ScreenManager(this);

	}

	// ---------------------------------------------
	// Core-Methods
	// ---------------------------------------------

	@Override
	protected void onInitializeInputActions(KeyEventActionManager eventActionManager) {
		eventActionManager.addGameKeyActions(new LD58KeyActions());

		super.onInitializeInputActions(eventActionManager);
	}

	@Override
	protected void onInitializeBitmapFontSources(BitmapFontManager fontManager) {
		super.onInitializeBitmapFontSources(fontManager);

		ScreenManager.ScreenManagerFonts.AddOrUpdate(ScreenManager.FONT_MENU_TOOLTIP_NAME, "res/fonts/fontNulshock12.json");
		ScreenManager.ScreenManagerFonts.AddOrUpdate(ScreenManager.FONT_MENU_ENTRY_NAME, "res/fonts/fontNulshock16.json");
		ScreenManager.ScreenManagerFonts.AddOrUpdate(ScreenManager.FONT_MENU_BOLD_ENTRY_NAME, "res/fonts/fontNulshock16.json");
		ScreenManager.ScreenManagerFonts.AddOrUpdate(ScreenManager.FONT_MENU_TITLE_NAME, "res/fonts/fontNulshock22.json");

		ScreenManager.ScreenManagerFonts.AddOrUpdate(ToastManager.FONT_TOAST_NAME, "res/fonts/fontNulshock16.json");

		SharedResources.RendererManagerFonts.AddOrUpdate(SharedResources.HUD_FONT_TEXT_BOLD_SMALL_NAME, "res/fonts/fontBarlow14.json");

		SharedResources.RendererManagerFonts.AddOrUpdate(SharedResources.UI_FONT_TEXT_NAME, "res/fonts/fontBarlow14.json");
		SharedResources.RendererManagerFonts.AddOrUpdate(SharedResources.UI_FONT_TEXT_BOLD_NAME, "res/fonts/fontBarlow14.json");
		SharedResources.RendererManagerFonts.AddOrUpdate(SharedResources.UI_FONT_HEADER_NAME, "res/fonts/fontNulshock16.json");
		SharedResources.RendererManagerFonts.AddOrUpdate(SharedResources.UI_FONT_TITLE_NAME, "res/fonts/fontNulshock22.json");
	}

	@Override
	protected void onInitializeApp() {
		super.onInitializeApp();

		mScreenManager.initialize();
	}

	@Override
	protected void onLoadResources() {
		super.onLoadResources();

		mGameResourceLoader = new GameResourceLoader(mResourceManager, config().display(), 0);

		mGameResourceLoader.loadResources(mResourceManager);
		mGameResourceLoader.setMinimumTimeToShowLogosMs(ConstantsGame.START_GAME_IMMEDIATELY ? 0 : 2000);
		mGameResourceLoader.loadResourcesInBackground(this);

		mResourceManager.audioManager().loadAudioFilesFromMetafile("res/audio/_meta.json");
		mResourceManager.musicManager().loadMusicFromMetaFile("res/music/meta.json");

		var lMusic = new MusicController(mControllerManager, mResourceManager.musicManager(), LintfordCore.CORE_ENTITY_GROUP_ID);
		lMusic.playFromGroup(0, "menu");

		mScreenManager.loadResources(mResourceManager);
	}

	@Override
	protected void finializeAppSetup() {
		if (ConstantsGame.START_GAME_IMMEDIATELY) {

			var lSoundFxController = new SoundFxController(mControllerManager, mResourceManager.audioManager(), LintfordCore.CORE_ENTITY_GROUP_ID);
			lSoundFxController.initialize(this);

			final var options = new GameOptions();
			options.levelNumber = 2;

			mScreenManager.addScreen(new GameScreen(mScreenManager, new SampleSceneHeader(), options));
			return;
		}

		mScreenManager.addScreen(new MainMenuBackground(mScreenManager));
		mScreenManager.addScreen(new MainMenu(mScreenManager));

		var lSoundFxController = new SoundFxController(mControllerManager, mResourceManager.audioManager(), LintfordCore.CORE_ENTITY_GROUP_ID);
		lSoundFxController.initialize(this);

	}

	@Override
	protected void onUnloadResources() {
		super.onUnloadResources();

		mScreenManager.unloadResources();
	}

	@Override
	protected void onHandleInput() {
		super.onHandleInput();

		gameCamera().handleInput(this);
		mScreenManager.handleInput(this);
	}

	@Override
	protected void onUpdate() {
		super.onUpdate();

		mScreenManager.update(this);
	}

	@Override
	protected void onDraw() {
		super.onDraw();

		mScreenManager.draw(this);
	}

}
