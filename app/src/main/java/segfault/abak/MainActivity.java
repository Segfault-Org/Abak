package segfault.abak;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import segfault.abak.common.widgets.Nav;
import segfault.abak.common.widgets.ActionbarNavigationListener;

/**
 * Entrance activity holding Navigation Host Fragment.
 */
public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final NavController navController = Nav.get(this);
        navController.addOnDestinationChangedListener(
                new ActionbarNavigationListener(
                        Nav.findStartDestination(navController.getGraph()).getId(),
                        getActionBar()));
    }

    @Override
    public void onBackPressed() {
        if (!Nav.get(this).popBackStack()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}