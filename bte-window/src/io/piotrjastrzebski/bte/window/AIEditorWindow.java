package io.piotrjastrzebski.bte.window;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.piotrjastrzebski.bte.AIEditor;

/**
 * Simple util class that will open a new Lwjgl3 window with editor in it
 *
 * Created by EvilEntity on 19/02/2016.
 */
public class AIEditorWindow {
	private static final String TAG = AIEditorWindow.class.getSimpleName();
	private Lwjgl3Window editorWindow;
	private Lwjgl3WindowConfiguration editorWindowConfig;
	private ApplicationListener editorWindowListener;

	/**
	 * Open new window for given editor
	 * @param editor editor for window
	 * @return if window opened successfully
	 */
	public boolean open (final AIEditor editor) {
		return open(-1, -1, 800, 600, editor, null);
	}

	/**
	 * Open new window for given editor
	 * @param width width of the window
	 * @param height height of the window
	 * @param editor editor for window
	 * @return if window opened successfully
	 */
	public boolean open (int width, int height, final AIEditor editor) {
		return open(-1, -1, width, height, editor, null);
	}

	/**
	 * Open new window for given editor
	 * @param x x position of the window
	 * @param y y position of the window
	 * @param width width of the window
	 * @param height height of the window
	 * @param editor editor for window
	 * @return if window opened successfully
	 */
	public boolean open (int x, int y, int width, int height, final AIEditor editor) {
		return open(x, y, width, height, editor, null);
	}

	/**
	 * Open new window for given editor
	 * @param x x position of the window
	 * @param y y position of the window
	 * @param width width of the window
	 * @param height height of the window
	 * @param editor editor for window
	 * @param listener listener for window events
	 * @return if window opened successfully
	 */
	public boolean open (int x, int y, int width, int height, final AIEditor editor, final WindowListener listener) {
		if (!(Gdx.app instanceof Lwjgl3Application)) {
			Gdx.app.error(TAG, "MultiWindow is supported only in Lwjgl3 backend!");
			return false;
		}
		if (editorWindowConfig == null) {
			editorWindowConfig = new Lwjgl3WindowConfiguration();
			editorWindowConfig.setWindowListener(new Lwjgl3WindowAdapter(){
				@Override public boolean windowIsClosing () {
					if (listener != null) {
						listener.onClose();
					}
					return true;
				}
			});
			editorWindowConfig.setWindowPosition(x, y);
			editorWindowConfig.setWindowedMode(width, height);
			editorWindowConfig.setTitle("AIEditor");
			editorWindowListener = new ApplicationListener() {
				com.badlogic.gdx.scenes.scene2d.Stage stage;
				@Override public void create () {
					stage = new com.badlogic.gdx.scenes.scene2d.Stage(new ScreenViewport());
					stage.addActor(editor.getWindow(false));
					Gdx.input.setInputProcessor(stage);
				}

				@Override public void resize (int width, int height) {
					stage.getViewport().update(width, height);
				}

				@Override public void render () {
					Gdx.gl.glClearColor(0f, 0f, 0f, 1);
					Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
					stage.act(Gdx.graphics.getDeltaTime());
					stage.draw();
				}

				@Override public void pause () {

				}

				@Override public void resume () {

				}

				@Override public void dispose () {
					stage.dispose();
				}
			};
		}
		Lwjgl3Application app = (Lwjgl3Application)Gdx.app;
		editorWindow = app.newWindow(editorWindowListener, editorWindowConfig);

		return true;
	}

	/**
	 * Close the window if it is visible
	 */
	public void close () {
		if (editorWindow != null) {
			editorWindow.closeWindow();
			editorWindow = null;
		}
	}

	/**
	 * Underlying {@link Lwjgl3Window}
	 * @return {@link Lwjgl3Window}
	 */
	public Lwjgl3Window getEditorWindow () {
		return editorWindow;
	}

	public interface WindowListener {
		/**
		 * Called when open window is being closed
		 */
		void onClose();
	}
	public static void main (String[] args) {
		System.out.println("Hi");
	}
}
