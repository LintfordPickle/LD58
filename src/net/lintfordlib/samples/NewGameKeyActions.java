package net.lintfordlib.samples;

import org.lwjgl.glfw.GLFW;

import net.lintfordlib.core.input.GameKeyActions;

// this class is needed because, although the game can register new event actions in the startup, we need to further allocate a 'use' string 
// so we know what is to be mapped in the keybindings screen.

//@formatter:off

public class NewGameKeyActions extends GameKeyActions {

	public static final int KEY_BINDING_PRIMARY_FIRE 		= 1000;
	public static final int KEY_BINDING_PRIMARY_FORWARD 	= 1001;
	public static final int KEY_BINDING_PRIMARY_LEFT 		= 1002;
	public static final int KEY_BINDING_PRIMARY_RIGHT 		= 1003;

	public NewGameKeyActions() {
		addNewKeyboardBinding("Primary Fire", 	KEY_BINDING_PRIMARY_FIRE, 		GLFW.GLFW_KEY_SPACE);
		addNewKeyboardBinding("Forward", 		KEY_BINDING_PRIMARY_FORWARD, 	GLFW.GLFW_KEY_W);
		addNewKeyboardBinding("Left", 			KEY_BINDING_PRIMARY_LEFT, 		GLFW.GLFW_KEY_A);
		addNewKeyboardBinding("Right", 			KEY_BINDING_PRIMARY_RIGHT, 		GLFW.GLFW_KEY_D);
		
		addNewGamepadBinding("Primary Fire",    KEY_BINDING_PRIMARY_FORWARD,    GLFW.GLFW_GAMEPAD_BUTTON_X);
	}
}
