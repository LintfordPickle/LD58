package net.lintfordlib.ld58.controllers;

import net.lintfordlib.controllers.BaseController;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.ld58.data.GameState;
import net.lintfordlib.ld58.data.IGameStateListener;

public class GameStateController extends BaseController {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String CONTROLLER_NAME = "GameState Controller";

	// --------------------------------------
	// Variables
	// --------------------------------------

	private IGameStateListener mGameStateListener;
	private GameState mGameState;

	// --------------------------------------
	// Properties
	// --------------------------------------

	public void setGameStateListener(IGameStateListener listener) {
		mGameStateListener = listener;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameStateController(ControllerManager controllerManager, GameState gameState, int entityGroupUId) {
		super(controllerManager, CONTROLLER_NAME, entityGroupUId);

		mGameState = gameState;
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

		if (!mGameState.hasGameStarted() || mGameState.hasGameEnded())
			return;

		// Check for lose conditions
		if (mGameState.lives() <= 0) {
			mGameStateListener.onGameLost();
		}

		// TODO: Check for win conditions
		// mGameStateListener.onGameWon();

	}
}
