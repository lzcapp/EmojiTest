package app.lzc.emoji;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private int numValid = 0, numMax = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView title = MainActivity.this.findViewById(R.id.title);
        title.setText(getString(R.string.ui_title));
        new Thread(() -> {
            try {
                testUnicode();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void testUnicode() throws IOException, InterruptedException {
        File path = getApplication().getFilesDir();
        String version = "";
        try {
            URL urlVersion = new URL("https://app.lzc.app/emoji/version");
            HttpURLConnection connect = (HttpURLConnection) urlVersion.openConnection();
            InputStream input = connect.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            String line;
            System.out.println(connect.getResponseCode());
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            version = sb.toString();

            if (Double.parseDouble(version) > 14.0) {
                URL url = new URL("https://app.lzc.app/emoji/emoji-test.txt");
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream is = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(path + "/" + "emoji-test.txt");
                byte[] buffer = new byte[1024];
                do {
                    int len = is.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    fos.write(buffer, 0, len);
                } while (true);
                is.close();
                fos.close();
            }
        } catch (Exception ignored) {}

        InputStreamReader inputReader;
        File file = new File(path + "/" + "emoji-test.txt");
        if (file.exists()) {
            //noinspection IOStreamConstructor
            inputReader = new InputStreamReader(new FileInputStream(file));
        } else {
            inputReader = new InputStreamReader(getResources().getAssets().open("emoji-test.txt"));
        }


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
            ProgressBar progressBar = MainActivity.this.findViewById(R.id.progressBar);
            textView.setVisibility(View.GONE);
            textView3.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        });
        checkVersion(version);
    }

    @SuppressLint("SetTextI18n")
    private void checkVersion(String latest) {
        runOnUiThread(() -> {
            String emoji = "";
            TreeMap<String, String> map = new TreeMap<>();
            try {
                URL url = new URL("https://app.lzc.app/emoji/emoji.txt");
                URLConnection connection = url.openConnection();
                connection.connect();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                emoji = sb.toString();

                JSONObject jobj = new JSONObject(emoji);
                Iterator<String> keys = jobj.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    String value = jobj.get(key).toString();
                    map.put(key, value);
                }
            } catch (Exception ignored) {}

            String version = "";
            for (TreeMap.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (validEmoji(value)) {
                    version = key;
                    break;
                }
            }

            TextView textView4 = MainActivity.this.findViewById(R.id.textView4);
            textView4.setVisibility(View.VISIBLE);
            textView4.setText("≈  Emoji " + version + "/" + latest);
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