package segfault.abak.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import segfault.abak.sdk.SdkConstants;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView textView = findViewById(R.id.text1);
        textView.setText(getIntent().getBundleExtra(SdkConstants.EXTRA_SETTINGS) == null ? "Null" : "Nonnull");
        Button setNull = findViewById(R.id.button_set_null);
        setNull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_OK, null);
                finish();
            }
        });
        Button set = findViewById(R.id.button_set);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putInt("aa", 114514);
                setResult(Activity.RESULT_OK, new Intent().putExtra(SdkConstants.EXTRA_SETTINGS, bundle));
                finish();
            }
        });
        Button cancel = findViewById(R.id.button_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED, null);
                finish();
            }
        });
    }
}
