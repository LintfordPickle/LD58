package net.lintfordlib.samples.controllers;

import net.lintfordlib.controllers.BaseController;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.samples.data.IGameStateListener;

public class GameStateController extends BaseController {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String CONTROLLER_NAME = "GameState Controller";

	// --------------------------------------
	// Variables
	// --------------------------------------

	private IGameStateListener mGameStateListener;

	// --------------------------------------
	// Properties
	// --------------------------------------

	public void setGameStateListener(IGameStateListener listener) {
		mGameStateListener = listener;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameStateController(ControllerManager controllerManager, int entityGroupUId) {
		super(controllerManager, CONTROLLER_NAME, entityGroupUId);
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void initialize(LintfordCore core) {
		super.initialize(core);

		// Here you can get references to other controllers created within the screen.

		// final var lControllerManager = core.controllerManager();
		// mGameComponentController = (GameComponentController) lControllerManager.getControllerByNameRequired(GameComponentController.CONTROLLER_NAME, entityGroupUid());
	}

	@Override
	public void update(LintfordCore core) {
		super.update(core);

		// Check for win/lose conditions

		// mGameStateListener.onGameLost();
		// mGameStateListener.onGameWon();
	}
}
