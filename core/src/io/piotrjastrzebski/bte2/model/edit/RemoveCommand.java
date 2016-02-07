package io.piotrjastrzebski.bte2.model.edit;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.tasks.ModelTask;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class RemoveCommand extends Command implements Pool.Poolable {
	private static Pool<RemoveCommand> pool = new Pool<RemoveCommand>() {
		@Override protected RemoveCommand newObject () {
			return new RemoveCommand();
		}
	};

	public static RemoveCommand obtain (ModelTask what) {
		return pool.obtain().init(what);
	}

	protected RemoveCommand() {
		super(Type.REMOVE);
	}

	protected ModelTask what;
	protected ModelTask parent;

	protected int idInParent;

	public RemoveCommand init (ModelTask what) {
		this.what = what;
		parent = what.getParent();
		idInParent = parent.getChildId(what);
		return this;
	}

	@Override public void execute () {
		parent.removeChild(what);
	}

	@Override public void undo () {
		parent.insertChild(idInParent, what);
	}

	@Override public void free () {
		pool.free(this);
	}

	@Override public void reset () {
		// free pools or whatever
		what = null;
		parent = null;
		idInParent = -1;
	}
}
