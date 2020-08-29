package segfault.abak.common.widgets;

import android.app.ActionBar;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.FloatingWindow;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import java.lang.ref.WeakReference;

/**
 * A simple navigation listener which updates ActionBar corresponding to destination changes.
 * The reason not to use NavigationUI is that it depends on AppCompat which is too large for a simple ap.
 */
public class ActionbarNavigationListener implements NavController.OnDestinationChangedListener {
    private final int mTopLevelDestination;
    private final WeakReference<ActionBar> mActionBar;

    public ActionbarNavigationListener(@IdRes int topLevelDestination, @NonNull ActionBar actionBar) {
        mTopLevelDestination = topLevelDestination;
        mActionBar = new WeakReference<>(actionBar);
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
        if (destination instanceof FloatingWindow) return;
        final ActionBar actionBar = mActionBar.get();
        if (actionBar == null) return;

        // Title
        actionBar.setTitle(destination.getLabel());

        // "Back" button
        boolean isTopLevelDestination = Nav.matchDestination(destination, mTopLevelDestination);
        actionBar.setDisplayHomeAsUpEnabled(!isTopLevelDestination);
    }
}
