package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public abstract class ViewTarget extends DragAndDrop.Target {
	public ViewTarget (Actor actor) {
		super(actor);
	}

	@Override public boolean drag (DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y,
		int pointer) {
		return onDrag((ViewSource)source, (ViewPayload)payload, x, y);
	}

	@Override public void drop (DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
		onDrop((ViewSource)source, (ViewPayload)payload, x, y);
	}

	public abstract boolean onDrag (ViewSource source, ViewPayload payload, float x, float y);
	public abstract void onDrop (ViewSource source, ViewPayload payload, float x, float y);
}
