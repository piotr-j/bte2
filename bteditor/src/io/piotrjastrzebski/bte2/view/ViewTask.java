package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.Pool;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 09/02/2016.
 */
class ViewTask extends Tree.Node implements Pool.Poolable, TaskModel.ChangeListener {

	enum DropPoint {
		ABOVE, MIDDLE, BELOW;
	}

	private final static Pool<ViewTask> pool = new Pool<ViewTask>() {
		@Override protected ViewTask newObject () {
			return new ViewTask();
		}
	};

	public static ViewTask obtain (TaskModel task, BehaviorTreeView view) {
		return pool.obtain().init(task, view);
	}

	public static void free (ViewTask task) {
		pool.free(task);
	}

	private static final String TAG = ViewTask.class.getSimpleName();
	protected static final float DROP_MARGIN = 0.25f;

	protected DragAndDrop dad;
	protected BehaviorTreeModel model;
	protected TaskModel task;
	protected VisTable container;
	protected VisLabel prefix;
	protected VisLabel label;
	protected VisLabel status;
	protected ViewTarget target;
	protected ViewSource source;
	protected VisImage separator;

	protected boolean isMoving;

	protected boolean isMarkedAsGuarded;

	public ViewTask () {
		super(new VisTable());
		container = (VisTable)getActor();
		separator = new VisImage();
		prefix = new VisLabel();
		container.add(prefix);
		label = new VisLabel();
		container.add(label);
		status = new VisLabel("");
		status.setColor(ViewColors.FRESH);
		container.add(status).padLeft(5);
		container.setTouchable(Touchable.enabled);

		setObject(this);
		target = new ViewTarget(container) {
			boolean copy = false;

			@Override public boolean onDrag (ViewSource source, ViewPayload payload, float x, float y) {
				copy = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
				Actor actor = getActor();
				DropPoint dropPoint = getDropPoint(actor, y);
				boolean isValid = !task.isReadOnly() && payload.task != task;
				if (isValid) {
					switch (dropPoint) {
					case ABOVE:
						if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
							isValid = model.canMoveBefore(payload.task, task);
						} else {
							isValid = model.canAddBefore(payload.task, task);
						}
						break;
					case MIDDLE:
						if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
							isValid = model.canMove(payload.task, task);
						} else {
							isValid = model.canAdd(payload.task, task);
						}
						break;
					case BELOW:
						if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
							isValid = model.canMoveAfter(payload.task, task);
						} else {
							isValid = model.canAddAfter(payload.task, task);
						}
						break;
					default:
						isValid = false;
					}
				}
				updateSeparator(dropPoint, isValid);
				return isValid;
			}

			@Override public void onDrop (ViewSource source, ViewPayload payload, float x, float y) {
				DropPoint dropPoint = getDropPoint(getActor(), y);
				switch (dropPoint) {
				case ABOVE:
					if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
						model.moveBefore(payload.task, task);
					} else {
						model.addBefore(copy ? payload.task.copy() : payload.task, task);
					}
					break;
				case MIDDLE:
					if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
						model.move(payload.task, task);
					} else {
						model.add(copy ? payload.task.copy() : payload.task, task);
					}
					break;
				case BELOW:
					if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
						model.moveAfter(payload.task, task);
					} else {
						model.addAfter(copy ? payload.task.copy() : payload.task, task);
					}
					break;
				}
			}

			@Override public void reset (DragAndDrop.Source source, DragAndDrop.Payload payload) {
				resetSeparator();
			}
		};
		source = new ViewSource(label) {
			@Override public DragAndDrop.Payload dragStart (InputEvent event, float x, float y, int pointer) {
				isMoving = true;
				updateNameColor();
				return ViewPayload.obtain(task.getName(), task).asMove();
			}

			@Override public void dragStop (InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload,
				DragAndDrop.Target target) {
				isMoving = false;
				updateNameColor();
				ViewPayload.free((ViewPayload)payload);
			}
		};
		reset();
	}

	private ViewTask init (TaskModel task, BehaviorTreeView view) {
		// TODO add * after root/include when tree/subtree is not saved
		this.task = task;
		this.dad = view.dad;
		this.model = view.model;
		separator.setDrawable(view.dimImg);
		separator.setVisible(false);
		container.addActor(separator);
		label.setText(task.getName());
		if (task.getType() != TaskModel.Type.ROOT && !task.isReadOnly()) {
			dad.addSource(source);
		}
		if (task.getType() != TaskModel.Type.GUARD) {
			task.addListener(this);
			status.setText("FRESH");
		}
		updateNameColor();
		dad.addTarget(target);
		return this;
	}

	private void updateNameColor () {
		// NOTE it is possible that the task is freed before this is called from target callback
		// this can happen when we drop stuff back to drawer, it gets removed, tree is updated but the callbad didnt yet fire
		if (task == null)
			return;
		if (task.isReadOnly()) {
			label.setColor(Color.GRAY);
		} else if (task.isValid()) {
			if (isMoving) {
				label.setColor(Color.CYAN);
			} else if (task.isGuard()) {
				label.setColor(ViewColors.GUARD);
			} else if (isMarkedAsGuarded) {
				label.setColor(ViewColors.GUARDED);
			} else {
				label.setColor(Color.WHITE);
			}
		} else {
			label.setColor(ViewColors.INVALID);
		}
		prefix.setColor(label.getColor());
	}

	private void updateSeparator (DropPoint dropPoint, boolean isValid) {
		resetSeparator();
		Color color = isValid ? ViewColors.VALID : ViewColors.INVALID;
		separator.setColor(color);
		separator.setWidth(container.getWidth());
		separator.setHeight(container.getHeight() / 4f);
		switch (dropPoint) {
		case ABOVE:
			separator.setVisible(true);
			separator.setPosition(0, container.getHeight() - separator.getHeight() / 2);
			break;
		case MIDDLE:
			label.setColor(color);
			break;
		case BELOW:
			separator.setVisible(true);
			separator.setPosition(0, -separator.getHeight() / 2);
			break;
		}
	}

	private void resetSeparator () {
		separator.setVisible(false);
		updateNameColor();
	}

	public boolean isGuard () {
		return task != null && task.isGuard();
	}

	public void markAsGuarded (boolean marked) {
		ViewTask parent = this;
		while ((parent = (ViewTask)parent.getParent()) != null) {
			if (!parent.task.isGuard())
				break;
		}
		if (parent == null) {
			Gdx.app.error(TAG, "No guard parent, wtf?");
			return;
		}
		parent.isMarkedAsGuarded = marked;
		if (marked) {
			parent.prefix.setText("(GT) ");
		} else {
			parent.prefix.setText("");
		}
		parent.updateNameColor();
	}

	@Override public void statusChanged (Task.Status from, Task.Status to) {
		status.setText(to.toString());
		status.setColor(ViewColors.getColor(to));
		status.clearActions();
		status.addAction(Actions.color(Color.GRAY, 1.5f, Interpolation.pow3In));
	}

	@Override public void reset () {
		if (task != null)
			task.removeListener(this);
		task = null;
		label.setText("<INVALID>");
		status.setText("");
		if (dad != null) {
			dad.removeSource(source);
			dad.removeTarget(target);
		}
		separator.setVisible(false);
		model = null;
		for (Tree.Node node : getChildren()) {
			free((ViewTask)node);
		}
		getChildren().clear();
	}

	private static DropPoint getDropPoint (Actor actor, float y) {
		float a = y / actor.getHeight();
		if (a < DROP_MARGIN) {
			return DropPoint.BELOW;
		} else if (a > 1 - DROP_MARGIN) {
			return DropPoint.ABOVE;
		}
		return DropPoint.MIDDLE;
	}
}
