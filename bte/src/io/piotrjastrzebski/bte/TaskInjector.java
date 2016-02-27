package io.piotrjastrzebski.bte;

import com.badlogic.gdx.ai.btree.Task;

/**
 * TaskInjector that should inject dependencies into {@link Task}s and its children if any
 *
 * @see {@link AIEditor#setTaskInjector(TaskInjector)}
 *
 * Created by PiotrJ on 26/02/16.
 */
public interface TaskInjector {
	/**
	 * Inject dependencies into given {@link Task} and its children
	 * @param task task to inject
	 */
	void inject(Task task);
}
