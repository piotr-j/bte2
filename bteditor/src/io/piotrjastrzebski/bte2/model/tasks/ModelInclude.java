package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibrary;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BTModel;

/**
 * Wraps Include task
 *
 * For path to be considered valid it must be present in BehaviorTreeLibraryManager.getInstance().getLibrary()
 *
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class ModelInclude extends ModelTask implements Pool.Poolable {
	private final static Pool<ModelInclude> pool = new Pool<ModelInclude>() {
		@Override protected ModelInclude newObject () {
			return new ModelInclude();
		}
	};

	public static ModelInclude obtain (Include include, BTModel model) {
		return pool.obtain().init(include, model);
	}

	public static void free (ModelInclude leaf) {
		pool.free(leaf);
	}

	private static final String TAG = ModelInclude.class.getSimpleName();

	private ModelInclude () {
		super(Type.INCLUDE);
	}

	public ModelInclude init (Include include, BTModel model) {
		super.init(include, model);
		return this;
	}

	protected ModelInclude init (ModelInclude other) {
		// NOTE we can't clone this task as that will attempt to create subtree
		super.init(other.wrapped, other.model);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override public void validate () {
		// TODO check that we have a proper tree at specified subtree
		// TODO if it is valid, we want to add the sub tree as child of this task
		// TODO that will probably require custom include task that accepts children or something
		// TODO perhaps delegate path check to external thing, so it is possible to change it
		if (!dirty) return;
		// TODO set dirty when data is changed
		dirty = false;
//		valid = true;
		Include include = (Include)wrapped;
		// TODO test with dog.other
		include.subtree = "dog.other";
		include.lazy = true;
		BehaviorTreeLibrary library = BehaviorTreeLibraryManager.getInstance().getLibrary();
		if (include.subtree != null) {
			// TODO use this from snapshot eventually
//			if (library.hasBehaviorTree(include.subtree))
			try {
				include.addChild(library.createRootTask(include.subtree));
				valid = true;
			} catch (RuntimeException e) {
				// TODO proper handling, with type of error reported
				Gdx.app.error(TAG, "Subtree not found " + include.subtree);
			}
		}
		if (children.size != wrapped.getChildCount()) {
			for (int i = 0; i < children.size; i++) {
				free(children.get(i));
			}
			children.clear();
			for (int i = 0; i < wrapped.getChildCount(); i++) {
				ModelTask child = wrap(wrapped.getChild(i), model);
				child.setParent(this);
				child.setReadOnly(true);
				children.add(child);
			}
		}
	}

	@Override public ModelInclude copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "ModelInclude{"+wrapped+"}";
	}

	public static class FanycInclude {

	}
}
