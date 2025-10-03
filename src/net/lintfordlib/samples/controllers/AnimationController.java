package net.lintfordlib.samples.controllers;

import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.controllers.core.FafAnimationController;

public class AnimationController extends FafAnimationController {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String CONTROLLER_NAME = "Animations Controller";

	public static final String EXPLOSION_ANIM = "explosion";

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public AnimationController(ControllerManager controllerManager, int entityGroupUid) {
		super(controllerManager, CONTROLLER_NAME, entityGroupUid);
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	public void playBigExplosionAnimation(float worldX, float worldY) {
		playAnimationByName(EXPLOSION_ANIM, worldX, worldY);
	}
}
