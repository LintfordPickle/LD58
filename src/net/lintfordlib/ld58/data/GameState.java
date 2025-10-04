package net.lintfordlib.ld58.data;

import net.lintfordlib.core.debug.Debug;

public class GameState {

	// --------------------------------------
	// Constants
	// --------------------------------------

	// --------------------------------------
	// Variables
	// --------------------------------------

	private int mCoins;
	private int mEnemiesKilled;
	private int mHealth;

	private boolean mHasGameStarted;
	private boolean mHasGameEnded;

	private float mTrackLengthTotal;
	private float mPlayerDistance;

	// --------------------------------------
	// Properties
	// --------------------------------------

	public boolean hasGameStarted() {
		return mHasGameStarted;
	}

	public boolean hasGameEnded() {
		return mHasGameEnded;
	}

	public int coins() {
		return mCoins;
	}

	public int health() {
		return mHealth;
	}

	public int kills() {
		return mEnemiesKilled;
	}

	public void playerDistance(float distance) {
		mPlayerDistance = distance;
	}

	public float playerDistance() {
		return mPlayerDistance;
	}

	public float trackLength() {
		return mTrackLengthTotal;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameState() {
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	public void reset() {
		mPlayerDistance = 0;
		mCoins = 0;
		mEnemiesKilled = 0;
		mHealth = 3;

		mHasGameStarted = false;
		mHasGameEnded = false;
	}

	public void startGame(float trackLength) {
		if (mHasGameStarted) {
			Debug.debugManager().logger().w(GameState.class.getSimpleName(), "Game has already been started!");
			return;
		}

		if (mHasGameEnded) {
			reset();
		}

		mHealth = 3;
		mTrackLengthTotal = trackLength;
		mHasGameStarted = true;
	}

	public void endGame() {
		if (!mHasGameStarted) {
			Debug.debugManager().logger().w(GameState.class.getSimpleName(), "Cannot end the game, it has not been started!");
			return;
		}

		mHasGameEnded = true;
	}

	public int getScore() {
		return 0;
	}

	public void addCoins(int amt) {
		mCoins += amt;
	}

	public void addKill() {
		mEnemiesKilled++;
	}

	// returns true if dead
	public boolean removeHealth() {
		mHealth--;

		if (mHealth <= 0)
			return true;

		return false;
	}
}
