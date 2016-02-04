package io.piotrjastrzebski.bte2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class AIEditorTest extends Game {
	private static final String TAG = AIEditorTest.class.getSimpleName();

	private AIEditor editor;

	@Override public void create () {
		editor = new AIEditor();

	}

	@Override public void render () {
		Gdx.app.log(TAG, "Update woo!");
	}

	@Override public void dispose () {
		editor.dispose();
	}

	public static void main (String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1280;
		config.height = 720;
		config.useHDPI = true;
		new LwjglApplication(new AIEditorTest(), config);
	}
}
