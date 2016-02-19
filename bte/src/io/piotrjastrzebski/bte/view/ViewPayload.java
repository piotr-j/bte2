package io.piotrjastrzebski.bte.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.Pool;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class ViewPayload extends DragAndDrop.Payload {
	private final static Pool<ViewPayload> pool = new Pool<ViewPayload>() {
		@Override protected ViewPayload newObject () {
			return new ViewPayload();
		}
	};
	public static ViewPayload obtain (String text, TaskModel payload) {
		return pool.obtain().init(text, payload);
	}

	public static void free (ViewPayload task) {
		pool.free(task);
	}

	protected enum Type {ADD, COPY, MOVE}
	protected VisLabel drag;
	protected VisLabel valid;
	protected VisLabel invalid;
	protected TaskModel task;
	protected Type type;

	protected ViewPayload () {
		LabelStyle labelStyle = VisUI.getSkin().get("label-background", LabelStyle.class);
		drag = new VisLabel("", labelStyle);
		setDragActor(drag);
		valid = new VisLabel("", labelStyle);
		valid.setColor(ViewColors.VALID);
		setValidDragActor(valid);
		invalid = new VisLabel("", labelStyle);
		invalid.setColor(ViewColors.INVALID);
		setInvalidDragActor(invalid);
	}

	private ViewPayload init (String text, TaskModel payload) {
		this.task = payload;
		setDragText(text);
		return this;
	}

	protected ViewPayload asAdd () {
		type = Type.ADD;
		return this;
	}

	protected ViewPayload asMove() {
		type = Type.MOVE;
		return this;
	}

	protected ViewPayload asCopy() {
		type = Type.COPY;
		return this;
	}

	public Type getType () {
		return type;
	}

	protected void setDragText (String text) {
		// NOTE got to pack so background is sized correctly
		drag.setText(text);
		drag.pack();
		valid.setText(text);
		valid.pack();
		invalid.setText(text);
		invalid.pack();
	}
}
