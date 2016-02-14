package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class BehaviorTreeView extends Table implements BehaviorTreeModel.BTChangeListener {
	public static String DRAWABLE_WHITE = "dialogDim";
	private static final String TAG = BehaviorTreeView.class.getSimpleName();
	protected BehaviorTreeModel model;
	protected VisTable topMenu;
	protected VisScrollPane drawerScrollPane;
	protected VisScrollPane treeScrollPane;
	protected VisTree taskDrawer;
	protected VisTree tree;
	protected VisTable taskEdit;
	protected DragAndDrop dad;
	protected ViewTarget removeTarget;
	protected SpriteDrawable dimImg;

	public BehaviorTreeView (final BehaviorTreeModel model) {
		this.model = model;
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
		tree = new VisTree() {
			@Override public void setOverNode (Node overNode) {
				Node old = tree.getOverNode();
				if (old != overNode) {
					onOverNodeChanged(old, overNode);
				}
				super.setOverNode(overNode);
			}
		};
		tree.setYSpacing(0);
		// add dim to tree so its in same coordinates as nodes
		treeView.add(tree).fill().expand();
		treeScrollPane = new VisScrollPane(treeView);
		treeScrollPane.addListener(new InputListener(){
			@Override public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) {
				getStage().setScrollFocus(null);
			}

			@Override public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
				getStage().setScrollFocus(treeScrollPane);
			}
		});
		taskEdit = new VisTable(true);
		VisTable taskView = new VisTable(true);
		taskView.add(taskDrawer).fill().expand();
		drawerScrollPane = new VisScrollPane(taskView);
		drawerScrollPane.addListener(new InputListener(){
			@Override public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) {
				getStage().setScrollFocus(null);
			}

			@Override public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
				getStage().setScrollFocus(drawerScrollPane);
			}
		});

		VisSplitPane drawerTreeSP = new VisSplitPane(drawerScrollPane, treeScrollPane, false);
		drawerTreeSP.setSplitAmount(.33f);
		VisSplitPane dtEditSP = new VisSplitPane(drawerTreeSP, taskEdit, false);
		dtEditSP.setSplitAmount(.75f);
		add(dtEditSP).grow().pad(5);

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

	private void onOverNodeChanged (Tree.Node oldNode, Tree.Node newNode) {

	}

	@Override public void onInit (BehaviorTreeModel model) {
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

	private void fillTree (Tree.Node parent, TaskModel task) {
		Tree.Node node = ViewTask.obtain(task, this);
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
	private ObjectMap<String, TaggedRoot> tagToNode = new ObjectMap<>();
	public void addSrcTask (String tag, Class<? extends Task> cls) {
		TaggedTask taggedTask = TaggedTask.obtain(tag, cls, this);
		taggedTasks.add(taggedTask);
		taggedTasks.sort();

		// TODO ability to toggle visibility of each node, so it is easier to reduce clutter by hiding rarely used tasks
		for (TaggedTask task : taggedTasks) {
			TaggedRoot categoryNode = tagToNode.get(task.tag, null);
			if (categoryNode == null) {
				// TODO do we want a custom class for those?
				categoryNode = TaggedRoot.obtain(task.tag, this);
				tagToNode.put(tag, categoryNode);
				taskDrawer.add(categoryNode);
			}
			if (categoryNode.findNode(task) == null){
				categoryNode.add(task);
			}
		}
		taskDrawer.expandAll();
	}

	@Override public void onChange (BehaviorTreeModel model) {
		rebuildTree();
	}

	@Override public void onListenerAdded (BehaviorTreeModel model) {

	}

	@Override public void onListenerRemoved (BehaviorTreeModel model) {

	}

	@Override public void onReset (BehaviorTreeModel model) {

	}
}
