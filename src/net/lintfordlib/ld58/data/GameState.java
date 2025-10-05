package net.lintfordlib.ld58.data;

import net.lintfordlib.core.debug.Debug;

public class GameState {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final int MAX_HEALTH = 3;

	// --------------------------------------
	// Variables
	// --------------------------------------

	private int mCoins;
	private int mEnemiesKilled;
	private int mHealth;

	private boolean mHasGameStarted;
	private boolean mHasGameEnded;

	private float mTrackLengthTotal;
	private float mEndLevelDistance;
	private int mEndLevelCoinAmt;
	private float mPlayerDistance;

	private float mSpeed;

	// --------------------------------------
	// Properties
	// --------------------------------------

	public float speed() {
		return mSpeed;
	}

	public void speed(float speed) {
		mSpeed = speed;
	}

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

	public float endTrackDistance() {
		return mEndLevelDistance;
	}

	public int endLevelCoinAmt() {
		return mEndLevelCoinAmt;
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
		mHealth = MAX_HEALTH;

		mHasGameStarted = false;
		mHasGameEnded = false;
	}

	public void readyGame(float trackLength, float endTrackLength, int amtCoinsNeeded) {
		mHealth = MAX_HEALTH;
		
		mTrackLengthTotal = trackLength;
		mEndLevelCoinAmt = amtCoinsNeeded;
		mEndLevelDistance = endTrackLength;
	}

	public void startGame() {
		if (mHasGameStarted) {
			Debug.debugManager().logger().w(GameState.class.getSimpleName(), "Game has already been started!");
			return;
		}

		if (mHasGameEnded)
			reset();

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
		return (int) (mCoins * 20 + mEnemiesKilled * 50);
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
