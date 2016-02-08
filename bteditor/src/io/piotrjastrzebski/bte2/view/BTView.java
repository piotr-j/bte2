package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import io.piotrjastrzebski.bte2.model.BTModel;
import io.piotrjastrzebski.bte2.model.tasks.ModelTask;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class BTView<E> extends Table implements BTModel.BTChangeListener {
	public static String DRAWABLE_WHITE = "dialogDim";
	private static final String TAG = BTView.class.getSimpleName();
	private Skin skin;
	private BTModel model;
	private VisTable topMenu;
	private VisScrollPane drawerScrollPane;
	private VisScrollPane treeScrollPane;
	private VisTree taskDrawer;
	private VisTree tree;
	private VisTable taskEdit;
	private DragAndDrop dad;
	private ViewTarget removeTarget;
	private SpriteDrawable dimImg;

	public BTView (final BTModel model) {
		this.model = model;
		debugAll();
		model.addChangeListener(this);
		dimImg = new SpriteDrawable((SpriteDrawable)VisUI.getSkin().getDrawable(DRAWABLE_WHITE));
		dimImg.getSprite().setColor(Color.WHITE);
		// create label style with background used by ViewPayloads
		VisTextButton.ButtonStyle btnStyle = VisUI.getSkin().get(VisTextButton.ButtonStyle.class);
		VisLabel.LabelStyle labelStyle = new Label.LabelStyle(VisUI.getSkin().get(VisLabel.LabelStyle.class));
		labelStyle.background = btnStyle.up;
		VisUI.getSkin().add("label-background", labelStyle);

		dad = new DragAndDrop();
		topMenu = new VisTable(true);
		add(topMenu).colspan(3);
		// TODO add undo/redo buttons
		// TODO add update control, step, pause/resume, reset
		topMenu.add(new VisLabel("Top Menu - TODO add stuff in here!")).expandX().fillX();
		VisTextButton undoBtn = new VisTextButton("Undo");
		undoBtn.addListener(new ClickListener(){
			@Override public void clicked (InputEvent event, float x, float y) {
				model.undo();
			}
		});
		VisTextButton redoBtn = new VisTextButton("Redo");
		redoBtn.addListener(new ClickListener(){
			@Override public void clicked (InputEvent event, float x, float y) {
				model.redo();
			}
		});
		topMenu.add(undoBtn);
		topMenu.add(redoBtn);
		row();
		taskDrawer = new VisTree();
		taskDrawer.setYSpacing(-2);
		taskDrawer.setFillParent(true);
		VisTable treeView = new VisTable(true);
		tree = new VisTree();
		tree.setYSpacing(0);
		// add dim to tree so its in same coordinates as nodes
		treeView.add(tree).fill().expand();
		treeScrollPane = new VisScrollPane(treeView);
		taskEdit = new VisTable(true);
		VisTable taskView = new VisTable(true);
		taskView.add(taskDrawer).fill().expand();
		drawerScrollPane = new VisScrollPane(taskView);
		taskDrawer.debugAll();

		// TODO understand how this bullshit works
		add(drawerScrollPane).expand(1, 1).fill().pad(5);
		add(treeScrollPane).expand(2, 1).fill().pad(5, 0, 5, 0);
		add(taskEdit).expand(1, 1).fill().pad(5);


		removeTarget = new ViewTarget(drawerScrollPane) {
			@Override public boolean onDrag (ViewSource source, ViewPayload payload, float x, float y) {
				return payload.getType() == ViewPayload.Type.MOVE;
			}

			@Override public void onDrop (ViewSource source, ViewPayload payload, float x, float y) {
				model.remove(payload.task);
			}
		};
		dad.addTarget(removeTarget);
	}

	@Override public void onInit (BTModel model) {
		this.model = model;

		rebuildTree();
	}

	private void rebuildTree () {
		for (Tree.Node node : tree.getNodes()) {
			ViewTask.free((ViewTask)node);
		}
		tree.clearChildren();

		fillTree(null, model.getRoot());
		tree.expandAll();
	}

	private void fillTree (Tree.Node parent, ModelTask task) {
		Tree.Node node = ViewTask.obtain(task, this);
		// since tree is not a node for whatever reason, we do this garbage
		if (parent == null) {
			tree.add(node);
		} else {
			parent.add(node);
		}
		for (int i = 0; i < task.getChildCount(); i++) {
			fillTree(node, task.getChild(i));
		}
	}

	private Array<TaggedTask> taggedTasks = new Array<>();
	private ObjectMap<String, Tree.Node> tagToNode = new ObjectMap<>();
	public void addSrcTask (String tag, Class<? extends Task> cls) {
		TaggedTask taggedTask = TaggedTask.obtain(tag, cls, this);
		taggedTasks.add(taggedTask);
		taggedTasks.sort();

		// TODO ability to toggle visibility of each node, so it is easier to reduce clutter by hiding rarely used tasks
		for (TaggedTask task : taggedTasks) {
			Tree.Node categoryNode = tagToNode.get(task.tag, null);
			if (categoryNode == null) {
				// TODO do we want a custom class for those?
				categoryNode = new Tree.Node(new VisLabel(task.tag));
				tagToNode.put(tag, categoryNode);
				taskDrawer.add(categoryNode);
			}
			if (categoryNode.findNode(task) == null){
				categoryNode.add(task);
			}
		}
		taskDrawer.expandAll();
	}

	private static class TaggedTask extends Tree.Node implements Pool.Poolable, Comparable<TaggedTask> {
		private final static Pool<TaggedTask> pool = new Pool<TaggedTask>() {
			@Override protected TaggedTask newObject () {
				return new TaggedTask();
			}
		};

		public static TaggedTask obtain (String tag, Class<? extends Task> cls, BTView view) {
			return pool.obtain().init(tag, cls, view);
		}

		public static void free (TaggedTask task) {
			pool.free(task);
		}

		protected VisLabel label;
		protected String tag;
		protected Class<? extends Task> cls;
		private DragAndDrop dad;
		private BTModel model;
		private String simpleName;
		private ViewSource source;
		public TaggedTask () {
			super(new VisLabel());
			label = (VisLabel)getActor();
			setObject(this);
			source = new ViewSource(label){
				@Override public DragAndDrop.Payload dragStart (InputEvent event, float x, float y, int pointer) {
					return ViewPayload.obtain(simpleName, ModelTask.wrap(cls, model)).asAdd();
				}

				@Override public void dragStop (InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload,
					DragAndDrop.Target target) {
					// TODO do some other stuff if needed
					ViewPayload.free((ViewPayload)payload);
				}
			};
			reset();
		}

		private TaggedTask init (String tag, Class<? extends Task> cls, BTView view) {
			this.tag = tag;
			this.cls = cls;
			dad = view.dad;
			model = view.model;
			simpleName = cls.getSimpleName();
			label.setText(simpleName);
			// source for adding task to tree
			dad.addSource(source);
			return this;
		}

		@Override public void reset () {
			tag = "<INVALID>";
			simpleName = "<INVALID>";
			cls = null;
			label.setText(tag);
			// TODO remove source/target from dad
			if (dad != null) dad.removeSource(source);
			dad = null;
		}

		@Override public int compareTo (TaggedTask o) {
			if (tag.equals(o.tag)) {
				return simpleName.compareTo(o.simpleName);
			}
			return tag.compareTo(o.tag);
		}
	}

	private static class ViewTask extends Tree.Node implements Pool.Poolable, ModelTask.ChangeListener {
		private final static Pool<ViewTask> pool = new Pool<ViewTask>() {
			@Override protected ViewTask newObject () {
				return new ViewTask();
			}
		};

		public static ViewTask obtain (ModelTask task, BTView view) {
			return pool.obtain().init(task, view);
		}

		public static void free (ViewTask task) {
			pool.free(task);
		}

		private DragAndDrop dad;
		private BTModel model;
		private ModelTask task;

		protected VisTable container;
		protected VisLabel label;
		protected VisLabel status;
		protected ViewTarget target;
		protected ViewSource source;
		protected VisImage separator;
		protected boolean isMoving;

		public ViewTask () {
			super(new VisTable());
			container = (VisTable)getActor();
			separator = new VisImage();

			label = new VisLabel();
			container.add(label);
			status = new VisLabel("FRESH");
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
						default: isValid = false;
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
							model.addBefore(copy?payload.task.copy():payload.task, task);
						}
						break;
					case MIDDLE:
						if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
							model.move(payload.task, task);
						} else {
							model.add(copy?payload.task.copy():payload.task, task);
						}
						break;
					case BELOW:
						if (!copy && payload.getType() == ViewPayload.Type.MOVE) {
							model.moveAfter(payload.task, task);
						} else {
							model.addAfter(copy?payload.task.copy():payload.task, task);
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

		@Override public void statusChanged (Task.Status from, Task.Status to) {
			status.setText(to.toString());
			status.setColor(ViewColors.getColor(to));
			status.clearActions();
			status.addAction(Actions.color(Color.GRAY, 1.5f, Interpolation.pow3In));
		}

		enum DropPoint {
			ABOVE, MIDDLE, BELOW
		}

		public static final float DROP_MARGIN = 0.25f;
		private DropPoint getDropPoint (Actor actor, float y) {
			float a = y / actor.getHeight();
			if (a < DROP_MARGIN) {
				return DropPoint.BELOW;
			} else if (a > 1 - DROP_MARGIN) {
				return DropPoint.ABOVE;
			}
			return DropPoint.MIDDLE;
		}

		private void resetSeparator () {
			separator.setVisible(false);
			updateNameColor();
		}

		private void updateNameColor () {
			// NOTE it is possible that the task is freed before this is called from target callback
			// this can happen when we drop stuff back to drawer, it gets removed, tree is updated but the callbad didnt yet fire
			if (task == null) return;
			if (task.isReadOnly()){
				label.setColor(Color.GRAY);
			} else if (task.isValid()) {
				// TODO some better color/indicator for isMoving?
				label.setColor(isMoving? Color.CYAN:Color.WHITE);
			} else {
				label.setColor(ViewColors.INVALID);
			}
		}

		private void updateSeparator (DropPoint dropPoint, boolean isValid) {
			resetSeparator();
			Color color = isValid ? ViewColors.VALID : ViewColors.INVALID;
			separator.setColor(color);
			separator.setWidth(container.getWidth());
			separator.setHeight(container.getHeight()/4f);
			switch (dropPoint) {
			case ABOVE:
				separator.setVisible(true);
				separator.setPosition(0, container.getHeight() - separator.getHeight()/2);
				break;
			case MIDDLE:
				label.setColor(color);
				break;
			case BELOW:
				separator.setVisible(true);
				separator.setPosition(0, - separator.getHeight()/2);
				break;
			}
		}

		private ViewTask init (ModelTask task, BTView view) {
			// TODO add * after root/include when tree/subtree is not saved
			this.task = task;
			task.addListener(this);
			this.dad = view.dad;
			this.model = view.model;
			separator.setDrawable(view.dimImg);
			separator.setVisible(false);
			container.addActor(separator);
			label.setText(task.getName());
			if (task.getType() != ModelTask.Type.ROOT && !task.isReadOnly()) {
				dad.addSource(source);
			}
			updateNameColor();
			dad.addTarget(target);
			return this;
		}

		@Override public void reset () {
			if (task != null)
				task.removeListener(this);
			task = null;
			label.setText("<INVALID>");
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
	}

	@Override public void onChange (BTModel model) {
		rebuildTree();
	}

	@Override public void onListenerAdded (BTModel model) {

	}

	@Override public void onListenerRemoved (BTModel model) {

	}

	@Override public void onReset (BTModel model) {

	}
}
