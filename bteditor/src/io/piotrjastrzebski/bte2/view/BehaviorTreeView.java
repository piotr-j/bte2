package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import io.piotrjastrzebski.bte2.AIEditor;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;
import io.piotrjastrzebski.bte2.view.edit.ViewTaskAttributeEdit;

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
	protected final VisTextButton btToggle;
	protected final VisTextButton btStep;
	protected final VisTextButton btReset;
	private Tree.Node selectedNode;
	private final ViewTaskAttributeEdit vtEdit;

	public BehaviorTreeView (final AIEditor editor) {
		this.model = editor.getModel();
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
		topMenu.add(redoBtn).padRight(20);

		VisTable btControls = new VisTable(true);
		topMenu.add(btControls);
		btToggle = new VisTextButton("AutoStep", "toggle");
		btToggle.setChecked(true);
		btControls.add(btToggle);
		btToggle.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				editor.setAutoStepBehaviorTree(btToggle.isChecked());
			}
		});
		btStep = new VisTextButton("Step");
		btStep.addListener(new ClickListener(){
			@Override public void clicked (InputEvent event, float x, float y) {
				editor.forceStepBehaviorTree();
			}
		});
		btControls.add(btStep);
		btReset = new VisTextButton("Restart");
		btReset.addListener(new ClickListener(){
			@Override public void clicked (InputEvent event, float x, float y) {
				editor.restartBehaviorTree();
			}
		});
		btControls.add(btReset);

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
		tree.getSelection().setMultiple(false);
		tree.addListener(new ChangeListener() {
			@Override public void changed (ChangeEvent event, Actor actor) {
				Tree.Node newNode = tree.getSelection().getLastSelected();
				onSelectionChanged(selectedNode, newNode);
				selectedNode = newNode;
			}
		});
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

		VisTable taskView =  new VisTable(true);
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

		taskEdit = new VisTable(true);
		taskEdit.add(vtEdit = new ViewTaskAttributeEdit()).expand().top();
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

	private void onSelectionChanged (Tree.Node oldNode, Tree.Node newNode) {
//		Gdx.app.log(TAG, "selection changed from " + oldNode + " to " + newNode);
		// add stuff to taskEdit
		if (newNode instanceof ViewTask) {
			TaskModel task = ((ViewTask)newNode).task;
			if (task != null && task.getWrapped() != null) {
				vtEdit.startEdit(task);
			} else {
				Gdx.app.error(TAG, "Error for " + task);
			}
		}
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
		if (model.isValid()) {
			btToggle.setDisabled(false);
			btStep.setDisabled(false);
			btReset.setDisabled(false);
		} else {
			btToggle.setDisabled(true);
			btStep.setDisabled(true);
			btReset.setDisabled(true);
		}
	}

	@Override public void onListenerAdded (BehaviorTreeModel model) {

	}

	@Override public void onListenerRemoved (BehaviorTreeModel model) {

	}

	@Override public void onReset (BehaviorTreeModel model) {

	}
}
