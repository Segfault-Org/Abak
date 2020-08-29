package segfault.abak;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import segfault.abak.R;
import segfault.abak.common.widgets.Nav;

public class MainFragment extends Fragment implements View.OnClickListener {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Setup UI
        final View view = inflater.inflate(R.layout.fragment_main, container, false);
        view.findViewById(R.id.button_create_backup).setOnClickListener(this);
        view.findViewById(R.id.button_restore_backup).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_create_backup:
                Nav.get(this).navigate(R.id.action_fragment_main_to_fragment_backup);
                break;
            case R.id.button_restore_backup:
                Nav.get(this).navigate(R.id.action_fragment_main_to_fragment_restore);
                break;
        }
    }
}
