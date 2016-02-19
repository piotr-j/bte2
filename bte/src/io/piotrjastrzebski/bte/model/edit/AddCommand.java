package io.piotrjastrzebski.bte.model.edit;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class AddCommand extends Command {
	private static Pool<AddCommand> pool = new Pool<AddCommand>() {
		@Override protected AddCommand newObject () {
			return new AddCommand();
		}
	};
	public static AddCommand obtain (TaskModel what, TaskModel where) {
		return pool.obtain().init(what, where);
	}
	public static AddCommand obtain (int at, TaskModel what, TaskModel where) {
		return pool.obtain().init(at, what, where);
	}
	private TaskModel what;
	private TaskModel target;
	private int at = -1;

	protected AddCommand () {
		super(Type.ADD);
	}

	public AddCommand init (TaskModel what, TaskModel target) {
		return init(-1, what, target);
	}

	public AddCommand init (int at, TaskModel what, TaskModel target) {
		this.at = at;
		// we can make a copy of what, but cant of target duh
		// do we even want to copy stuff?
		this.what = what;//.copy();
		this.target = target;
		return this;
	}

	public void execute () {
		// TODO do we want this to return something?
		if (at > -1) {
			target.insertChild(at, what);
		} else {
			target.addChild(what);
		}
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
		at = -1;
	}
}
