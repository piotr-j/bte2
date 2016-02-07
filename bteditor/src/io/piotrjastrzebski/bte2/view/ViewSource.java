package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public abstract class ViewSource extends DragAndDrop.Source {
	public ViewSource (Actor actor) {
		super(actor);
	}
}
