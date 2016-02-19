package io.piotrjastrzebski.bte.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.Pool;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Created by EvilEntity on 10/02/2016.
 */
public class TaggedRoot extends Tree.Node implements Pool.Poolable {
	private final static Pool<TaggedRoot> pool = new Pool<TaggedRoot>() {
		@Override protected TaggedRoot newObject () {
			return new TaggedRoot();
		}
	};

	public static TaggedRoot obtain (String tag, BehaviorTreeView view) {
		return pool.obtain().init(tag, view);
	}

	public static void free (TaggedRoot task) {
		pool.free(task);
	}

	private static final String TAG = TaggedTask.class.getSimpleName();

	protected BehaviorTreeView view;
	protected VisTable container;
	protected VisLabel tagLabel;
	protected VisCheckBox toggleHidden;
	protected String tag;
	protected Array<TaggedTask> tasks;
	protected BooleanArray visibleTasks;

	protected TaggedRoot () {
		super(new VisTable());
		container = (VisTable)getActor();
		tagLabel = new VisLabel();
		container.add(tagLabel);
		toggleHidden = new VisCheckBox("", "radio");
		container.add(toggleHidden).padLeft(5);
		toggleHidden.addListener(new ClickListener() {
			@Override public void clicked (InputEvent event, float x, float y) {
				toggleHiddenTasks(toggleHidden.isChecked());
			}
		});
		tasks = new Array<>();
		visibleTasks = new BooleanArray();
	}

	private TaggedRoot init (String tag, BehaviorTreeView view) {
		this.tag = tag;
		this.view = view;
		tagLabel.setText(tag);
		return this;
	}

	@Override public void reset () {
		tag = "<RESET>";
		tagLabel.setText(tag);
		tasks.clear();
		visibleTasks.clear();
	}

	@Override public void add (Tree.Node node) {
		super.add(node);
		if (node instanceof TaggedTask) {
			TaggedTask task = (TaggedTask)node;
			task.setParentTag(this);
			tasks.add(task);
			visibleTasks.add(true);
		} else {
			Gdx.app.error(TAG, "Node added to TaggedRoot should be TaggedTask, was " + node);
		}
	}

	public void toggle (TaggedTask task, boolean show) {
		for (int i = 0; i < tasks.size; i++) {
			TaggedTask other = tasks.get(i);
			if (other == task) {
				boolean isVisible = visibleTasks.get(i);
				if (show && !isVisible) {
					visibleTasks.set(i, true);
					if (task.getParent() == null)
						insert(i, task);
				} else if (!show && isVisible) {
					visibleTasks.set(i, false);
					if (!hiddenRevealed) {
						task.remove();
					}
				}
				break;
			}
		}
	}

	private boolean hiddenRevealed;
	private void toggleHiddenTasks (boolean checked) {
		hiddenRevealed = checked;
		if (hiddenRevealed) {
			for (int i = 0; i < tasks.size; i++) {
				TaggedTask task = tasks.get(i);
				if (!visibleTasks.get(i)) {
					insert(i, task);
				}
			}
			expandAll();
		} else {
			for (int i = 0; i < tasks.size; i++) {
				TaggedTask task = tasks.get(i);
				if (!visibleTasks.get(i)) {
					task.remove();
				}
			}
		}
	}

	public void free () {
		pool.free(this);
	}
}
