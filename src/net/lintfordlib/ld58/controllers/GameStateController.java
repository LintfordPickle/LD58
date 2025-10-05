package net.lintfordlib.ld58.controllers;

import net.lintfordlib.controllers.BaseController;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.debug.Debug;
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

	public GameState gameState() {
		return mGameState;
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
	public void update(LintfordCore core) {
		super.update(core);

		if (!mGameState.hasGameStarted() || mGameState.hasGameEnded())
			return;

		if (mGameState.health() <= 0) {
			Debug.debugManager().logger().w(GameStateController.class.getSimpleName(), "Game lost state initiated (died)");

			mGameState.endGame();
			mGameStateListener.onGameLost();
			return;
		}

		if (mGameState.playerDistance() >= mGameState.endTrackDistance()) {

			if (mGameState.coins() >= mGameState.endLevelCoinAmt() || mGameState.endLevelCoinAmt() == 0) {
				Debug.debugManager().logger().w(GameStateController.class.getSimpleName(), "Game wonstate initiated");

				mGameState.endGame();
				mGameStateListener.onGameWon();
			} else {
				Debug.debugManager().logger().w(GameStateController.class.getSimpleName(), "Game lost state initiated (not enough coins)");

				mGameState.endGame();
				mGameStateListener.onGameLost();
			}

			return;
		}
	}
}
