package net.lintfordlib.ld58;

import org.lwjgl.glfw.GLFW;

import net.lintfordlib.core.input.GameKeyActions;

// this class is needed because, although the game can register new event actions in the startup, we need to further allocate a 'use' string 
// so we know what is to be mapped in the keybindings screen.

//@formatter:off

public class LD58KeyActions extends GameKeyActions {

	public static final int KEY_BINDING_FIRE 				= 1000;
	public static final int KEY_BINDING_JUMP 				= 1001;
	public static final int KEY_BINDING_FORWARD 			= 1002;
	public static final int KEY_BINDING_BACKWARD 			= 1003;
	public static final int KEY_BINDING_LEFT 		= 1004;
	public static final int KEY_BINDING_RIGHT 		= 1005;

	public LD58KeyActions() {
		addNewKeyboardBinding("Primary Fire", 	KEY_BINDING_FIRE, 				GLFW.GLFW_KEY_Z);
		addNewKeyboardBinding("Jump", 			KEY_BINDING_JUMP, 				GLFW.GLFW_KEY_X);
		addNewKeyboardBinding("Forward", 		KEY_BINDING_FORWARD, 			GLFW.GLFW_KEY_UP);
		addNewKeyboardBinding("Backwards", 		KEY_BINDING_BACKWARD, 			GLFW.GLFW_KEY_DOWN);
		addNewKeyboardBinding("Left", 			KEY_BINDING_LEFT, 				GLFW.GLFW_KEY_LEFT);
		addNewKeyboardBinding("Right", 			KEY_BINDING_RIGHT, 				GLFW.GLFW_KEY_RIGHT);
		
		addNewGamepadBinding("Primary Fire",    KEY_BINDING_FIRE,    			GLFW.GLFW_GAMEPAD_BUTTON_B);
		addNewGamepadBinding("Jump",    		KEY_BINDING_JUMP,    			GLFW.GLFW_GAMEPAD_BUTTON_A);
	}
}
