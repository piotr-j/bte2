package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibrary;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;

/**
 * Wraps Include task
 *
 * For path to be considered valid it must be present in BehaviorTreeLibraryManager.getInstance().getLibrary()
 *
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class IncludeModel extends TaskModel implements Pool.Poolable {
	private final static Pool<IncludeModel> pool = new Pool<IncludeModel>() {
		@Override protected IncludeModel newObject () {
			return new IncludeModel();
		}
	};

	public static IncludeModel obtain (Include include, BehaviorTreeModel model) {
		return pool.obtain().init(include, model);
	}

	public static void free (IncludeModel leaf) {
		pool.free(leaf);
	}

	private static final String TAG = IncludeModel.class.getSimpleName();

	private IncludeModel () {
		super(Type.INCLUDE);
	}

	public IncludeModel init (Include include, BehaviorTreeModel model) {
		super.init(include, model);
		return this;
	}

	protected IncludeModel init (IncludeModel other) {
		// NOTE we can't clone this task as that will attempt to create subtree
		super.init(other.wrapped, other.model);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override public boolean isValid () {
		// TODO check that we have a proper tree at specified subtree
		// TODO if it is valid, we want to add the sub tree as child of this task
		// TODO that will probably require custom include task that accepts children or something
		// TODO perhaps delegate path check to external thing, so it is possible to change it
		Include include = (Include)wrapped;
		// TODO test with dog.other
		include.subtree = "dog.other";
		include.lazy = true;
		BehaviorTreeLibrary library = BehaviorTreeLibraryManager.getInstance().getLibrary();
		boolean includeChanged = false;
		if (include.subtree != null) {
			// TODO use this from snapshot eventually
//			if (library.hasBehaviorTree(include.subtree))
			try {
				Task<Object> rootTask = library.createRootTask(include.subtree);
				if (include.getChildCount() > 0) {
					ReflectionUtils.remove(include.getChild(0), include);
				}
				include.addChild(rootTask);
				includeChanged = true;
				valid = true;
			} catch (RuntimeException e) {
				// TODO proper handling, with type of error reported
				Gdx.app.error(TAG, "Subtree not found " + include.subtree, e);
			}
		}
		if (includeChanged) {
			for (int i = 0; i < children.size; i++) {
				free(children.get(i));
			}
			children.clear();
			for (int i = 0; i < wrapped.getChildCount(); i++) {
				TaskModel child = wrap(wrapped.getChild(i), model);
				child.setParent(this);
				child.setReadOnly(true);
				children.add(child);
			}
		}
		return valid;
	}

	@Override public IncludeModel copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "IncludeModel{" +
			"name='" + name + '\'' +
			", subtree='" + (wrapped!=null?((Include)wrapped).subtree:"null") + '\'' +
			(valid?", valid":", invalid") +
			'}';
	}
	public static class FanycInclude {

	}
}
