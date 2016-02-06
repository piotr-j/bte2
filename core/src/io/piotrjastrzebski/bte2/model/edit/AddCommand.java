package io.piotrjastrzebski.bte2.model.edit;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.tasks.ModelTask;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class AddCommand extends Command {
	private static Pool<AddCommand> pool = new Pool<AddCommand>() {
		@Override protected AddCommand newObject () {
			return new AddCommand();
		}
	};
	public static Command obtain (ModelTask what, ModelTask where) {
		return pool.obtain().init(what, where);
	}
	private ModelTask what;

	private ModelTask target;

	protected AddCommand () {
		super(Type.ADD);
	}

	public Command init (ModelTask what, ModelTask target) {
		// we can make a copy of what, but cant of target duh
		// do we even want to copy stuff?
		this.what = what.copy();
		this.target = target;
		return this;
	}

	public void execute () {
		// TODO do we want this to return something?
		target.addChild(what);
	}

	public void undo () {
		target.removeChild(what);
	}

	@Override public void free () {
		pool.free(this);
	}

	@Override public void reset () {
		// TODO free or whatever
		what = null;
		target = null;
	}
}
