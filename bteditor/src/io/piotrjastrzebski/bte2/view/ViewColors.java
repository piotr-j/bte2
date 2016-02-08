package io.piotrjastrzebski.bte2.view;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.Color;

/**
 * Created by PiotrJ on 31/10/15.
 */
public class ViewColors {
	// colors stolen from vis ui, hardcoded so we dont have to count on vis being used
	public static final Color VALID = new Color(0.105f, 0.631f, 0.886f, 1);
	public static final Color INVALID = new Color(0.862f, 0, 0.047f, 1);
	public final static Color SUCCEEDED = new Color(Color.GREEN);
	public final static Color RUNNING = new Color(Color.ORANGE);
	public final static Color FAILED = new Color(Color.RED);
	public final static Color CANCELLED = new Color(Color.YELLOW);
	public final static Color FRESH = new Color(Color.GRAY);

	public static Color getColor (Task.Status status) {
		if (status == null)
			return Color.GRAY;
		switch (status) {
		case SUCCEEDED:
			return SUCCEEDED;
		case RUNNING:
			return RUNNING;
		case FAILED:
			return FAILED;
		case CANCELLED:
			return CANCELLED;
		case FRESH:
		default:
			return FRESH;
		}
	}
}
