package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
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
	// colors stolen from vis ui, hardcoded so we dont have to count on vis being used
	public static final Color COLOR_VALID = new Color(0.105f, 0.631f, 0.886f, 1);
	public static final Color COLOR_INVALID = new Color(0.862f, 0, 0.047f, 1);
	// TODO proper colors
	public final static Color COLOR_SUCCEEDED = new Color(Color.GREEN);
	public final static Color COLOR_RUNNING = new Color(Color.YELLOW);
	public final static Color COLOR_FAILED = new Color(Color.RED);
	public final static Color COLOR_CANCELLED = new Color(Color.ORANGE);
	public final static Color COLOR_FRESH = new Color(Color.GRAY);
	private static final String TAG = BTView.class.getSimpleName();
	private Skin skin;
	private BTModel model;
	private VisTable topMenu;
	private VisScrollPane drawerScrollPane;
	private VisTree taskDrawer;
	private VisTree tree;
	private VisTable taskEdit;
	private DragAndDrop dad;
	private ViewTarget removeTarget;

	public BTView (final BTModel model) {
		this.model = model;
		debugAll();
		model.addChangeListener(this);

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
		// TODO entire task drawer as target for task removal
		taskDrawer = new VisTree();
		// TODO part of drawer is clipped in pane for whatever reason
		taskDrawer.setYSpacing(-2);
		taskDrawer.setFillParent(true);
		VisTable treeView = new VisTable(true);
		tree = new VisTree();
		tree.setYSpacing(-2);
		treeView.add(tree).fill().expand();
		taskEdit = new VisTable(true);
		drawerScrollPane = new VisScrollPane(taskDrawer);
		taskDrawer.debugAll();
		// TODO understand how this bullshit works
		add(drawerScrollPane).expand(1, 1).fill().pad(5);
		add(treeView).expand(2, 1).fill().pad(5, 0, 5, 0);
		add(taskEdit).expand(1, 1).fill().pad(5);


		removeTarget = new ViewTarget(drawerScrollPane) {
			@Override public boolean onDrag (ViewSource source, ViewPayload payload, float x, float y) {
				// TODO this should be move
				return payload.getType() == ViewPayload.Type.REMOVE;
			}

			@Override public void onDrop (ViewSource source, ViewPayload payload, float x, float y) {
				// TODO this should be move
				// if target is this, remove, if its another task move/copy
//				Gdx.app.log("", "drop " + payload);
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

//		Gdx.app.log(TAG, "tree rebuilt");
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

	private static class ViewTask extends Tree.Node implements Pool.Poolable {
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

		protected VisLabel label;
		protected ViewTarget target;
		protected ViewSource source;

		public ViewTask () {
			super(new VisLabel());
			label = (VisLabel)getActor();
			setObject(this);
			target = new ViewTarget(label) {
				@Override public boolean onDrag (ViewSource source, ViewPayload payload, float x, float y) {
					if (payload.getType() == ViewPayload.Type.REMOVE) return false;
					// TODO need to ask model if this is valid target
					return model.canAdd(payload.task, task);
				}

				@Override public void onDrop (ViewSource source, ViewPayload payload, float x, float y) {
//					Gdx.app.log("", "drop " + payload);
					// TODO tell model to add task from payload to our task
					model.add(payload.task, task);
				}
			};
			source = new ViewSource(label) {
				@Override public DragAndDrop.Payload dragStart (InputEvent event, float x, float y, int pointer) {
					return ViewPayload.obtain(task.getName(), task).asRemove();
				}

				@Override public void dragStop (InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload,
					DragAndDrop.Target target) {
					// TODO do some other stuff if needed
					ViewPayload.free((ViewPayload)payload);
				}
			};
			reset();
		}

		private ViewTask init (ModelTask task, BTView view) {
			this.task = task;
			this.dad = view.dad;
			this.model = view.model;
			label.setText(task.getName());
			if (task.getType() != ModelTask.Type.ROOT) {
				dad.addSource(source);
			}
			if (task.isValid()) {
				label.setColor(Color.WHITE);
			} else {
				label.setColor(COLOR_INVALID);
			}
			dad.addTarget(target);
			return this;
		}

		@Override public void reset () {
			task = null;
			label.setText("<INVALID>");
			if (dad != null) {
				dad.removeSource(source);
				dad.removeTarget(target);
			}
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
