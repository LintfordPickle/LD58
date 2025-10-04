package net.lintfordlib.ld58.data;

public class GameState {

	// --------------------------------------
	// Constants
	// --------------------------------------

	// --------------------------------------
	// Variables
	// --------------------------------------

	private float mDistanceTravelled;
	private int mCoins;
	private int mEnemiesKilled;
	private int mLives;
	private int mHealth;

	private boolean mHasGameStarted;
	private boolean mHasGameEnded;

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

	public int lives() {
		return mLives;
	}

	public int health() {
		return mHealth;
	}

	public int kills() {
		return mEnemiesKilled;
	}

	public float distanceTravelled() {
		return mDistanceTravelled;
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
		mDistanceTravelled = 0;
		mCoins = 0;
		mEnemiesKilled = 0;
		mLives = 3;
		mHealth = 3;

		mHasGameStarted = false;
		mHasGameEnded = false;
	}

	public void startGame() {
		if (mHasGameStarted)
			return;

		if (mHasGameEnded) {
			reset();
		}

		mHasGameStarted = true;
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

	public void removeLife() {
		mLives--;

	}

}
