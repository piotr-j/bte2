package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.decorator.Include;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BTModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
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

	private ModelInclude () {
		super(Type.INCLUDE);
	}

	public ModelInclude init (Include include, BTModel model) {
		super.init(include, model);
		return this;
	}

	protected ModelInclude init (ModelInclude other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
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
}
