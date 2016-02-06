package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.BranchTask;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BTModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
public class ModelBranch extends ModelTask implements Pool.Poolable {
	private final static Pool<ModelBranch> pool = new Pool<ModelBranch>() {
		@Override protected ModelBranch newObject () {
			return new ModelBranch();
		}
	};

	public static ModelBranch obtain (BranchTask task, BTModel model) {
		return pool.obtain().init(task, model);
	}

	public static void free (ModelBranch leaf) {
		pool.free(leaf);
	}

	private ModelBranch () {
		super(Type.BRANCH);
	}

	public ModelBranch init (BranchTask task, BTModel model) {
		super.init(task, model);
		return this;
	}

	protected ModelBranch init (ModelBranch other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public ModelBranch copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "ModelBranch{"+wrapped+"}";
	}
}
