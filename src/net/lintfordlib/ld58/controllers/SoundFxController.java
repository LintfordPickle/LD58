package net.lintfordlib.ld58.controllers;

import net.lintfordlib.controllers.BaseController;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.audio.AudioFireAndForgetManager;
import net.lintfordlib.core.audio.AudioManager;

public class SoundFxController extends BaseController {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String CONTROLLER_NAME = "Sound Fx Controller";

	public static final String SOUND_SHOOT = "SOUND_SHOOT";
	public static final String SOUND_COIN_0 = "SOUND_COIN_0";
	public static final String SOUND_COIN_1 = "SOUND_COIN_1";
	public static final String SOUND_COLLIDE = "SOUND_COLLIDE";
	public static final String SOUND_EXPLOSION = "SOUND_EXPLOSION";
	public static final String SOUND_HURT = "SOUND_HURT";
	public static final String SOUND_JUMP = "SOUND_JUMP";
	public static final String SOUND_LAND = "SOUND_LAND";
	public static final String SOUND_POWERUP = "SOUND_POWERUP";
	public static final String SOUND_FALL = "SOUND_FALL";

	// --------------------------------------
	// Variables
	// --------------------------------------

	private AudioFireAndForgetManager mAudioFireAndForgetManager;

	// --------------------------------------
	// Properties
	// --------------------------------------

	@Override
	public boolean isInitialized() {
		return mAudioFireAndForgetManager != null;
	}

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public SoundFxController(ControllerManager controllerManager, AudioManager audioManager, int entityGroupID) {
		super(controllerManager, CONTROLLER_NAME, entityGroupID);

		mAudioFireAndForgetManager = new AudioFireAndForgetManager(audioManager);

	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void initialize(LintfordCore core) {
		super.initialize(core);

		mAudioFireAndForgetManager.acquireAudioSources(10);
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	// this takes a string (look in the constants in this class).
	public void playSound(String soundFxName) {
		mAudioFireAndForgetManager.play(soundFxName);

	}
}
