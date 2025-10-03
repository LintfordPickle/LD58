package net.lintfordlib.samples;

import net.lintfordlib.GameInfo;
import net.lintfordlib.GameVersion;

public class GameWindow extends NewGameBase {

	private final int APP_VERSION_MAJ = 0;
	private final int APP_VERSION_MIN = 1;
	private final int APP_VERSION_BUILD = 1;

	private final String APP_POSTFIX = "09042024";

	// ---------------------------------------------
	// Entry Point
	// ---------------------------------------------

	public static void main(String[] args) {
		new GameWindow(new NewGameInfo(), args).createWindow();
	}

	// ---------------------------------------------
	// Constructor
	// ---------------------------------------------

	public GameWindow(GameInfo pGameInfo, String[] pArgs) {
		super(pGameInfo, pArgs);

		setGameVersion();
	}

	// ---------------------------------------------
	// Methods
	// ---------------------------------------------

	private void setGameVersion() {
		GameVersion.setGameVersion(APP_VERSION_MAJ, APP_VERSION_MIN, APP_VERSION_BUILD, APP_POSTFIX);
	}

}
