package app.lzc.emoji;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private int numValid = 0, numMax = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(() -> {
            try {
                testUnicode();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void testUnicode() throws IOException, InterruptedException {
        InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open("emoji-test.txt"));
        BufferedReader bufReader = new BufferedReader(inputReader);
        String line;
        while ((line = bufReader.readLine()) != null) {
            if (line.startsWith("#") || line.equals("") || line.contains("minimally-qualified") || line.contains("unqualified")) {
                continue;
            }
            String finalLine = line;
            runOnUiThread(() -> updateUI(finalLine));
            //noinspection BusyWait
            Thread.sleep(1);
        }
        runOnUiThread(() -> {
            TextView textView = MainActivity.this.findViewById(R.id.textView);
            TextView textView3 = MainActivity.this.findViewById(R.id.textView3);
            textView.setVisibility(View.GONE);
            textView3.setVisibility(View.GONE);
        });
        checkVersion();
    }

    @SuppressLint("SetTextI18n")
    private void checkVersion() {
        runOnUiThread(() -> {
            String version = "";
            if (validEmoji("&#x1F62E&#x200D&#x1F4A8")) {
                version = "13.1";
            } else if (validEmoji("&#x1F972")) {
                version = "13.0";
            } else if (validEmoji("&#x1F9D1&#x200D&#x1F9B0")) {
                version = "12.1";
            } else if (validEmoji("&#x1F971")) {
                version = "12.0";
            } else if (validEmoji("&#x1F970")) {
                version = "11.0";
            } else if (validEmoji("&#x1F929")) {
                version = "5.0";
            } else if (validEmoji("&#x1F471")) {
                version = "4.0";
            } else if (validEmoji("&#x1F923")) {
                version = "3.0";
            } else if (validEmoji("&#x1F441")) {
                version = "2.0";
            } else if (validEmoji("&#x1F600")) {
                version = "2.0";
            }
            TextView textView4 = MainActivity.this.findViewById(R.id.textView4);
            textView4.setVisibility(View.VISIBLE);
            textView4.setText("≈ Emoji " + version);
        });
    }

    private boolean validEmoji(String unicode) {
        Paint paint = new Paint();
        String emoji = String.valueOf(Html.fromHtml(unicode, Html.FROM_HTML_MODE_COMPACT));
        return paint.hasGlyph(emoji);
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(String line) {
        numMax++;
        String formatU = MainActivity.this.formatUnicode(line);

        TextView textView2 = MainActivity.this.findViewById(R.id.textView2);
        TextView textView = MainActivity.this.findViewById(R.id.textView);
        TextView textView3 = MainActivity.this.findViewById(R.id.textView3);
        TextView textView5 = MainActivity.this.findViewById(R.id.textView5);
        textView.setText(Html.fromHtml(formatU, Html.FROM_HTML_MODE_COMPACT));

        if (validEmoji(formatU)) {
            numValid++;
        }

        @SuppressLint("DefaultLocale") String percentage = String.format("%.2f", ((double) numValid / numMax) * 100);
        textView2.setText(numValid + " / " + numMax + " = ");
        String strDisplay = formatU.replace("&#x", " ");
        strDisplay = strDisplay.replaceFirst(" ", "");
        textView3.setText(strDisplay);
        textView5.setText(percentage + "%");

        ProgressBar progressBar = MainActivity.this.findViewById(R.id.progressBar);
        progressBar.setProgress(numMax);

        progressBar.invalidate();
        textView.invalidate();
        textView2.invalidate();
        textView3.invalidate();
        textView5.invalidate();
    }

    private String formatUnicode(String line) {
        String result = "&#x";
        result += line.replaceAll("\\s{2,}", "");
        result = result.replaceAll(";.*", "");
        result = result.replaceAll("\\s", "&#x");
        result = result.replaceAll("&#x$", "");
        return result;
    }
}