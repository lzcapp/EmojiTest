package app.lzc.emoji;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private String currentVersion;

    private int numValid = 0, numMax = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        TextView titleTextView = MainActivity.this.findViewById(R.id.titleTextView);
        titleTextView.setText(getResources().getString(R.string.app_name));
        TextView versionTextView = MainActivity.this.findViewById(R.id.versionTextView);
        versionTextView.setText(packageInfo.versionName);
        new Thread(() -> {
            try {
                testUnicode();
            } catch (IOException | InterruptedException | JSONException e) {
                Log.e("Error", e.toString());
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void testUnicode() throws IOException, InterruptedException, JSONException {
        File path = getApplication().getFilesDir();

        URL urlVersion = new URL("https://app.lzc.app/emoji/version");
        HttpURLConnection connect = (HttpURLConnection) urlVersion.openConnection();
        InputStream input = connect.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        String str;
        System.out.println(connect.getResponseCode());
        StringBuilder sbVersion = new StringBuilder();
        while ((str = in.readLine()) != null) {
            sbVersion.append(str);
        }
        currentVersion = sbVersion.toString();

        URL urlEmojiTest = new URL("https://app.lzc.app/emoji/emoji-test.txt");
        URLConnection connection = urlEmojiTest.openConnection();
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

        ArrayList<String[]> map = new ArrayList<>();
        URL url = new URL("https://app.lzc.app/emoji/emoji.txt");
        URLConnection connectionE = url.openConnection();
        connectionE.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(connectionE.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String emojiLine;
        while ((emojiLine = br.readLine()) != null) {
            sb.append(emojiLine).append("\n");
        }
        br.close();
        JSONObject obj = new JSONObject(sb.toString());
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = obj.get(key).toString();
            map.add(new String[]{key, value});
        }

        InputStreamReader inputReader;
        File file = new File(path + "/" + "emoji-test.txt");
        inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));

        BufferedReader bufReader = new BufferedReader(inputReader);
        String line;
        while ((line = bufReader.readLine()) != null) {
            if (line.startsWith("#") || line.isEmpty() || line.contains("minimally-qualified") || line.contains("unqualified")) {
                continue;
            }
            String finalLine = line;

            runOnUiThread(() -> updateUI(finalLine));
            //noinspection BusyWait
            Thread.sleep(1);
        }
        runOnUiThread(() -> {
            LinearLayout emojiLinearLayout = MainActivity.this.findViewById(R.id.emojiLinearLayout);
            ProgressBar progressBar = MainActivity.this.findViewById(R.id.progressBar);
            emojiLinearLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);

            String emojiVersion = "";
            for (String[] entry : map) {
                String key = entry[0];
                String value = entry[1];
                if (validEmoji(value)) {
                    emojiVersion = key;
                    break;
                }
            }

            TextView textView4 = MainActivity.this.findViewById(R.id.resultTextView);
            textView4.setVisibility(View.VISIBLE);
            textView4.setText("≈  Emoji " + emojiVersion + " / " + currentVersion);
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

        TextView textView2 = MainActivity.this.findViewById(R.id.countTextView);
        TextView textView = MainActivity.this.findViewById(R.id.emojiTextView);
        TextView textView3 = MainActivity.this.findViewById(R.id.codeTextView);
        TextView textView5 = MainActivity.this.findViewById(R.id.percentTextView);
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