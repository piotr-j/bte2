package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BTModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class ModelLeaf extends ModelTask implements Pool.Poolable {
	private final static Pool<ModelLeaf> pool = new Pool<ModelLeaf>() {
		@Override protected ModelLeaf newObject () {
			return new ModelLeaf();
		}
	};

	public static ModelLeaf obtain (LeafTask task, BTModel model) {
		return pool.obtain().init(task, model);
	}

	public static void free (ModelLeaf leaf) {
		pool.free(leaf);
	}

	private ModelLeaf () {
		super(Type.LEAF);
	}

	public ModelLeaf init (LeafTask task, BTModel model) {
		super.init(task, model);
		return this;
	}

	protected ModelLeaf init (ModelLeaf other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public ModelLeaf copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "ModelLeaf{"+wrapped+"}";
	}
}
