package io.piotrjastrzebski.bte2;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.VisUI;

/**
 * Main editor class
 *
 * Created by EvilEntity on 04/02/2016.
 */
public class AIEditor implements Disposable {
	private boolean ownsSkin;

	/**
	 * Create AIEditor with internal VisUI skin
	 * AIEditor must be disposed in this case
	 */
	public AIEditor () {
		this(null);
	}

	/**
	 * Create AIEditor with external VisUI skin
	 * @param skin Skin to use
	 */
	public AIEditor (Skin skin) {
		if (skin == null) {
			ownsSkin = true;
			skin = new Skin(VisUI.SkinScale.X1.getSkinFile());
		}
		VisUI.load(skin);
	}

	@Override public void dispose () {
		if (ownsSkin) {
			VisUI.dispose();
		}
	}
}
