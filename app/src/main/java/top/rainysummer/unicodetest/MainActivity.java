package top.rainysummer.unicodetest;

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

    private int numTotal = 0, numValid = 0;

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
            if (line.startsWith("#") || line.equals("")) {
                continue;
            }
            numTotal++;
            String finalLine = line;
            runOnUiThread((Runnable) () -> updateUI(finalLine));
            Thread.sleep(3);
        }
        TextView textView = (TextView) MainActivity.this.findViewById(R.id.textView);
        TextView textView3 = (TextView) MainActivity.this.findViewById(R.id.textView3);
        textView.setVisibility(View.GONE);
        textView3.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(String line) {
        String formatU = MainActivity.this.formatUnicode(line);
        if (formatU.equals("&#x1F1F9&#x1F1FC")) {
            numValid++;
            return;
        }
        Paint paint = new Paint();
        boolean hasGlyph = paint.hasGlyph(String.valueOf(Html.fromHtml(formatU)));
        TextView textView2 = (TextView) MainActivity.this.findViewById(R.id.textView2);
        TextView textView = (TextView) MainActivity.this.findViewById(R.id.textView);
        TextView textView3 = (TextView) MainActivity.this.findViewById(R.id.textView3);
        TextView textView5 = (TextView) MainActivity.this.findViewById(R.id.textView5);
        textView.setText(Html.fromHtml(formatU));
        if (hasGlyph) {
            if (formatU.contains("&#x200D&#x")) {
                String strN = formatU.replaceAll("&#x200D&#x", "&#x");
                String strN2 = String.valueOf(Html.fromHtml(strN));
                String strN1 = String.valueOf(Html.fromHtml(formatU));
                int nL = strN1.length();
                int oL = strN2.length();
                if (nL != oL) {
                    numValid++;
                }
            } else {
                numValid++;
            }
        }
        @SuppressLint("DefaultLocale") String percentage = String.format("%.2f", ((double) numValid / numTotal) * 100);
        textView2.setText(numValid + " / " + numTotal + " = ");
        textView3.setText(MainActivity.this.formatUnicode(line));
        textView5.setText(percentage + "%");

        ProgressBar progressBar = (ProgressBar) MainActivity.this.findViewById(R.id.progressBar);
        progressBar.setMax(numTotal);
        progressBar.setProgress(numValid);

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
        return result;
    }
}