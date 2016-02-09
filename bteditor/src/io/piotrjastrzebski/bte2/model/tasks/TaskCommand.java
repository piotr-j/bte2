package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BranchTask;
import com.badlogic.gdx.ai.btree.Decorator;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

/**
 * Created by PiotrJ on 15/10/15.
 */
@SuppressWarnings("rawtypes")
abstract class TaskCommand implements Pool.Poolable {
	protected Task task;
	protected Task target;

	public TaskCommand init (Task target, Task task) {
		if (target == null)
			throw new IllegalArgumentException("Target cannot be null");
		if (task == null)
			throw new IllegalArgumentException("Task cannot be null");

		this.task = task;
		this.target = target;

		return this;
	}

	@Override public void reset () {
		task = null;
		target = null;
	}

	abstract boolean execute ();

	public static class Insert extends TaskCommand {
		protected int at;

		public TaskCommand init (Task target, Task task, int at) {
			super.init(target, task);
			if (at < 0)
				throw new IllegalArgumentException("at cannot be < 0, is " + at);
			this.at = at;
			return this;
		}

		@Override boolean execute () {
			try {
				Gdx.app.log("INSERT", task + " to " + target + " at " + at);
				// we need to check it task is in target before we add, as that will happen on init
				if (target instanceof BranchTask) {
					Field field = ClassReflection.getDeclaredField(BranchTask.class, "children");
					field.setAccessible(true);
					@SuppressWarnings("unchecked")
					Array<Task> children = (Array<Task>)field.get(target);
					// disallow if out of bounds,  allow to insert if empty
					if (at > children.size && at > 0) {
						Gdx.app.error("INSERT", "cannot insert " + task + " to " + target + " at " + at + " as its out of range");
						return false;
					}
					if (!children.contains(task, true)) {
						children.insert(at, task);
					} else {
						Gdx.app.error("INSERT",
							"cannot insert " + task + " to " + target + " at " + at + ", target already contains task");
						return false;
					}
					return true;
				} else if (target instanceof Decorator) {
					// can insert if decorator is empty
					Field field = ClassReflection.getDeclaredField(Decorator.class, "child");
					field.setAccessible(true);
					Object old = field.get(target);
					// ignore at, just replace
					if (old == null || old != task) {
						field.set(target, task);
						return true;
					} else {
						Gdx.app.error("INSERT", "cannot insert " + task + " to " + target + " as its a decorator");
					}
				} else {
					Gdx.app.error("INSERT", "cannot insert " + task + " to " + target + " as its a leaf");
				}
			} catch (ReflectionException e) {
				Gdx.app.error("REMOVE", "ReflectionException error", e);
			}
			return false;
		}

		@Override public void reset () {
			super.reset();
			at = 0;
		}
	}

	public static class Remove extends TaskCommand {
		@Override boolean execute () {
			if (task.getStatus() == Task.Status.RUNNING) {
				task.cancel();
			}
			// remove from bt
			try {
				Gdx.app.log("REMOVE", task + " from " + target);
				// we need to check it task is in target before we add, as that will happen on init
				if (target instanceof BranchTask) {
					Field field = ClassReflection.getDeclaredField(BranchTask.class, "children");
					field.setAccessible(true);
					@SuppressWarnings("unchecked")
					Array<Task> children = (Array<Task>)field.get(target);
					return children.removeValue(task, true);
				} else if (target instanceof Decorator) {
					Field field = ClassReflection.getDeclaredField(Decorator.class, "child");
					field.setAccessible(true);
					Object old = field.get(target);
					if (old == task || old == null) {
						field.set(target, null);
					} else {
						return false;
					}
					return old != null;
				} else {
					Gdx.app.error("REMOVE", "cannot remove " + task + " from " + target + " as its a leaf");
				}
			} catch (ReflectionException e) {
				Gdx.app.error("REMOVE", "ReflectionException error", e);
			}
			return false;
		}
	}

	public static TaskCommand insert (Task task, Task target, int at) {
		return new Insert().init(task, target, at);
	}

	public static TaskCommand remove (Task task, Task target) {
		return new Remove().init(task, target);
	}
}
