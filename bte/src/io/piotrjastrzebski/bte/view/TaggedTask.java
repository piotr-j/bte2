package io.piotrjastrzebski.bte.view;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.Pool;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import io.piotrjastrzebski.bte.model.BehaviorTreeModel;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 09/02/2016.
 */
class TaggedTask extends Tree.Node implements Pool.Poolable, Comparable<TaggedTask> {
	private final static Pool<TaggedTask> pool = new Pool<TaggedTask>() {
		@Override protected TaggedTask newObject () {
			return new TaggedTask();
		}
	};

	public static TaggedTask obtain (String tag, Class<? extends Task> cls, BehaviorTreeView view) {
		return pool.obtain().init(tag, cls, view);
	}

	public static void free (TaggedTask task) {
		pool.free(task);
	}

	private static final String TAG = TaggedTask.class.getSimpleName();

	protected VisTable container;
	protected VisLabel label;
	protected String tag;
	protected Class<? extends Task> cls;
	protected DragAndDrop dad;
	protected BehaviorTreeModel model;
	protected String simpleName;
	protected ViewSource source;
	protected TaggedRoot parentTag;

	public TaggedTask () {
		super(new VisTable());
		container = (VisTable)getActor();
		label = new VisLabel();
		container.add(label);
		final VisCheckBox hide = new VisCheckBox("", "radio");
		hide.setChecked(true);
		container.add(hide).padLeft(5);
		hide.addListener(new ClickListener() {
			@Override public void clicked (InputEvent event, float x, float y) {
				if (parentTag != null) {
					parentTag.toggle(TaggedTask.this, hide.isChecked());
				}
			}
		});

		setObject(this);
		source = new ViewSource(label) {
			@Override public DragAndDrop.Payload dragStart (InputEvent event, float x, float y, int pointer) {
				return ViewPayload.obtain(simpleName, TaskModel.wrap(cls, model)).asAdd();
			}

			@Override public void dragStop (InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload,
				DragAndDrop.Target target) {
				// TODO do some other stuff if needed
				ViewPayload.free((ViewPayload)payload);
			}
		};
		reset();
	}

	private TaggedTask init (String tag, Class<? extends Task> cls, BehaviorTreeView view) {
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

	public void setParentTag (TaggedRoot parentTag) {
		this.parentTag = parentTag;
	}

	@Override public void reset () {
		tag = "<INVALID>";
		simpleName = "<INVALID>";
		cls = null;
		label.setText(tag);
		// TODO remove source/target from dad
		if (dad != null)
			dad.removeSource(source);
		dad = null;
		parentTag = null;
	}

	@Override public int compareTo (TaggedTask o) {
		if (tag.equals(o.tag)) {
			return simpleName.compareTo(o.simpleName);
		}
		return tag.compareTo(o.tag);
	}
}
