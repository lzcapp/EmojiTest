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
import android.widget.Toast; // Import for showing toast messages

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EmojiTestApp"; // Tag for logging
    private static final String UNICODE_EMOJI_TEST_URL = "https://www.unicode.org/Public/emoji/latest/emoji-test.txt";
    private static final String EMOJI_TEST_FILENAME = "emoji-test.txt";

    private int numValid = 0;
    private int numMax = 0;
    private String latestUnicodeEmojiVersion = "Unknown"; // Version from unicode.org emoji-test.txt

    // UI Elements
    private TextView titleTextView;
    private TextView versionTextView;
    private TextView countTextView;
    private TextView emojiTextView;
    private TextView codeTextView;
    private TextView percentTextView;
    private ProgressBar progressBar;
    private LinearLayout emojiLinearLayout;
    private TextView resultTextView;

    // Executor for background tasks to manage threads more efficiently
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Inner class to hold emoji version information
    private static class EmojiVersionInfo {
        String version;
        String unicodeCodepoints; // Space-separated codepoints like "1F600" or "1F468 200D 1F469"

        EmojiVersionInfo(String version, String unicodeCodepoints) {
            this.version = version;
            this.unicodeCodepoints = unicodeCodepoints;
        }
    }

    // Hardcoded list of representative emojis for version detection.
    // Sorted in descending order of version.
    private static final ArrayList<EmojiVersionInfo> EMOJI_VERSIONS_TO_TEST = new ArrayList<>();

    static {
        // Initialize the list of emojis and their introduction versions.
        // These are carefully chosen as representative for their respective versions.
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("16.0", "1FAE9")); // Alembic
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("15.1", "1F90F")); // Ginger Root
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("15.0", "1FA7B")); // Jellyfish
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("14.0", "1FAE0")); // Melting Face
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("13.1", "1F9D1 200D 2764 FE0F 200D 1F9D1")); // Couple with Heart: Person, Person
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("13.0", "1FA72")); // Bubble Tea
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("12.0", "1F97A")); // Woozy Face
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("11.0", "1F973")); // Partying Face
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("5.0", "1F9D0"));  // Face with Monocle
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("4.0", "1F926"));  // Face with Palm Over Mouth
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("3.0", "1F918"));  // Sign of the Horns
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("2.0", "1F914"));  // Thinking Face
        EMOJI_VERSIONS_TO_TEST.add(new EmojiVersionInfo("1.0", "1F600"));  // Grinning Face

        // Sort the list in descending order of version to find the highest supported version first.
        Collections.sort(EMOJI_VERSIONS_TO_TEST, new Comparator<EmojiVersionInfo>() {
            @Override
            public int compare(EmojiVersionInfo o1, EmojiVersionInfo o2) {
                return compareVersions(o2.version, o1.version); // o2 vs o1 for descending
            }
        });
    }

    /**
     * Helper method to compare two version strings (e.g., "16.0" vs "15.1").
     * @param v1 Version string 1
     * @param v2 Version string 2
     * @return A negative integer, zero, or a positive integer as the first version
     * is less than, equal to, or greater than the second.
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        titleTextView = findViewById(R.id.titleTextView);
        versionTextView = findViewById(R.id.versionTextView);
        countTextView = findViewById(R.id.countTextView);
        emojiTextView = findViewById(R.id.emojiTextView);
        codeTextView = findViewById(R.id.codeTextView);
        percentTextView = findViewById(R.id.percentTextView);
        progressBar = findViewById(R.id.progressBar);
        emojiLinearLayout = findViewById(R.id.emojiLinearLayout);
        resultTextView = findViewById(R.id.resultTextView);

        // Set app name and version
        titleTextView.setText(getResources().getString(R.string.app_name));
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionTextView.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found: " + e.getMessage());
            versionTextView.setText("N/A");
        }

        // Start the emoji test in a background thread using the executor service
        executorService.execute(this::startEmojiTest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service when the activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Interrupt any running tasks
        }
    }

    /**
     * Orchestrates the emoji test process: fetching test data and running checks.
     */
    private void startEmojiTest() {
        File emojiTestFile = downloadEmojiTestFile(); // Downloads or loads from assets
        if (emojiTestFile != null) {
            processEmojiTestFile(emojiTestFile);
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to initialize emoji data. No emoji test file available.", Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Downloads the emoji test file from the Unicode.org server.
     * If download fails, it attempts to load the file from the app's assets folder.
     * The file (either downloaded or from assets) is saved to internal storage.
     * The method also extracts the version from the loaded file.
     * @return The downloaded/loaded file in internal storage, or null if an error occurred.
     */
    private File downloadEmojiTestFile() {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        File destinationFile = new File(getApplication().getFilesDir(), EMOJI_TEST_FILENAME);
        boolean fileObtainedSuccessfully = false;

        // --- Attempt to download from network first ---
        try {
            URL urlEmojiTest = new URL(UNICODE_EMOJI_TEST_URL);
            connection = (HttpURLConnection) urlEmojiTest.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds connection timeout
            connection.setReadTimeout(5000);    // 5 seconds read timeout
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                fileOutputStream = new FileOutputStream(destinationFile);
                byte[] buffer = new byte[4096]; // Increased buffer size for efficiency
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                }
                Log.d(TAG, "Emoji test file downloaded successfully to: " + destinationFile.getAbsolutePath());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Emoji test file downloaded from network.", Toast.LENGTH_SHORT).show());
                fileObtainedSuccessfully = true;
            } else {
                Log.e(TAG, "Failed to download emoji test file from network. HTTP error code: " + connection.getResponseCode());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to download emoji test file. Trying local asset...", Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error downloading emoji test file: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network error downloading emoji test file. Trying local asset...", Toast.LENGTH_SHORT).show());
        } finally {
            // Ensure streams and connections are closed after network attempt
            try {
                if (inputStream != null) inputStream.close();
                if (fileOutputStream != null) fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams after network attempt: " + e.getMessage());
            }
            if (connection != null) connection.disconnect();
        }

        // --- If network download failed, attempt to load from assets ---
        if (!fileObtainedSuccessfully) {
            try {
                inputStream = getAssets().open(EMOJI_TEST_FILENAME);
                fileOutputStream = new FileOutputStream(destinationFile); // Write asset to internal storage for consistent processing
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                }
                Log.d(TAG, "Emoji test file loaded from assets to: " + destinationFile.getAbsolutePath());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Using emoji test file from assets.", Toast.LENGTH_LONG).show());
                fileObtainedSuccessfully = true;
            } catch (IOException e) {
                Log.e(TAG, "Error loading emoji test file from assets: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to load emoji test file from assets.", Toast.LENGTH_LONG).show());
                return null; // Both network and asset failed
            } finally {
                // Ensure streams are closed after asset attempt
                try {
                    if (inputStream != null) inputStream.close();
                    if (fileOutputStream != null) fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams after asset attempt: " + e.getMessage());
                }
            }
        }

        // --- Parse version regardless of source (network or asset) ---
        if (fileObtainedSuccessfully) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(destinationFile)))) {
                String line;
                // Pattern to match '# Version: X.Y' at the beginning of the file
                Pattern versionPattern = Pattern.compile("^# Version: (\\d+\\.\\d+)$");
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = versionPattern.matcher(line);
                    if (matcher.find()) {
                        latestUnicodeEmojiVersion = matcher.group(1);
                        Log.d(TAG, "Detected Unicode Emoji Version: " + latestUnicodeEmojiVersion);
                        break; // Found the version, no need to read further
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading file to extract version: " + e.getMessage(), e);
            }
            return destinationFile;
        }
        return null; // No file could be obtained successfully from either source
    }

    /**
     * Processes the downloaded emoji-test.txt file line by line.
     * @param emojiTestFile The file containing emoji test data.
     */
    private void processEmojiTestFile(File emojiTestFile) {
        BufferedReader bufReader = null;
        try {
            bufReader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(emojiTestFile)));
            String line;
            while ((line = bufReader.readLine()) != null) {
                // Filter out comments, empty lines, and specific test types
                if (line.startsWith("#") || line.trim().isEmpty() || line.contains("minimally-qualified") || line.contains("unqualified")) {
                    continue;
                }
                String finalLine = line;
                runOnUiThread(() -> updateUI(finalLine));
                try {
                    // This sleep is problematic. For a real app, consider a more efficient way
                    // to display progress without blocking the thread or making it slow.
                    // For example, update the UI less frequently or process multiple lines at once.
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    Log.w(TAG, "Emoji test processing interrupted: " + e.getMessage());
                    break; // Exit loop if interrupted
                }
            }
            runOnUiThread(this::finalizeUI);
        } catch (IOException e) {
            Log.e(TAG, "Error reading emoji test file: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error reading emoji test file.", Toast.LENGTH_LONG).show());
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing BufferedReader: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if a given unicode string represents a valid emoji on the device.
     * Uses Paint.hasGlyph to determine if the system can render the emoji.
     * @param unicode The unicode string in "&#xXXXX;" format.
     * @return true if the emoji is valid and can be rendered, false otherwise.
     */
    private boolean validEmoji(String unicode) {
        Paint paint = new Paint();
        // Html.fromHtml is deprecated in newer APIs but used here for compatibility with original code.
        // For new projects, consider using HtmlCompat.fromHtml from AndroidX.
        String emoji = String.valueOf(Html.fromHtml(unicode, Html.FROM_HTML_MODE_COMPACT));
        return paint.hasGlyph(emoji);
    }

    /**
     * Updates the UI with the progress of the emoji test.
     * This method runs on the UI thread.
     * @param line The current line from the emoji test file being processed.
     */
    @SuppressLint("SetTextI18n")
    private void updateUI(String line) {
        numMax++;
        String formattedUnicode = formatUnicode(line);

        emojiTextView.setText(Html.fromHtml(formattedUnicode, Html.FROM_HTML_MODE_COMPACT));

        if (validEmoji(formattedUnicode)) {
            numValid++;
        }

        @SuppressLint("DefaultLocale")
        String percentage = String.format("%.2f", ((double) numValid / numMax) * 100);
        countTextView.setText(numValid + " / " + numMax + " = ");

        // Clean up the string for display in codeTextView
        String strDisplay = formattedUnicode.replace("&#x", " ").trim();
        codeTextView.setText(strDisplay);
        percentTextView.setText(percentage + "%");

        progressBar.setProgress(numMax);
    }

    /**
     * Finalizes the UI after all emoji tests are complete.
     * This method runs on the UI thread.
     */
    @SuppressLint("SetTextI18n")
    private void finalizeUI() {
        emojiLinearLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Determine the device's highest supported emoji version
        String deviceSupportedEmojiVersion = "Unknown";
        for (EmojiVersionInfo info : EMOJI_VERSIONS_TO_TEST) {
            String formattedEmoji = formatUnicode(info.unicodeCodepoints);
            if (validEmoji(formattedEmoji)) {
                deviceSupportedEmojiVersion = info.version;
                break; // Found the highest supported version
            }
        }

        // Calculate percentage of supported emojis from the current emoji-test.txt
        @SuppressLint("DefaultLocale")
        String supportedPercentage = (numMax > 0) ? String.format("%.2f", ((double) numValid / numMax) * 100) : "0.00";

        resultTextView.setVisibility(View.VISIBLE);
        // Display the Unicode standard emoji version, the device supported version,
        // and the percentage of supported emojis from the loaded test file.
        resultTextView.setText(
                "Latest Emoji Version: " + latestUnicodeEmojiVersion + "\n" +
                        "Device Supported: " + "\n" +
                        numValid + " / " + numMax + " (" + supportedPercentage + "%)" + "\n" +
                        "≈ Emoji " + deviceSupportedEmojiVersion
        );
    }


    /**
     * Formats a raw line from emoji-test.txt into HTML-compatible unicode escape sequences.
     * Example: "1F600" becomes "&#x1F600;"
     * Handles multiple codepoints: "1F468 200D 1F469 200D 1F467" becomes "&#x1F468;&#x200D;&#x1F469;&#x200D;&#x1F467;"
     * @param line The raw line from emoji-test.txt.
     * @return The formatted unicode string.
     */
    private String formatUnicode(String line) {
        // Extract the part of the line before the '#' comment, then before the ';' property.
        // This ensures we only get the hexadecimal codepoints.
        String tempLine = line.split("#")[0]; // Get everything before the # comment
        String codepointString = tempLine.split(";")[0].trim(); // Get everything before the ; property, then trim

        // Replace one or more spaces with "&#x" to prepare for the HTML format.
        // Then add "&#x" prefix to the beginning.
        // Example: "1F600" -> "&#x1F600"
        // Example: "1F468 200D 1F469" -> "&#x1F468;&#x200D;&#x1F469"
        String result = "&#x" + codepointString.replaceAll("\\s+", "&#x");

        // Remove any trailing "&#x" that might occur if the line ended with a space
        // before the comment or semicolon. This is a safety measure.
        return result.replaceAll("&#x$", "");
    }
}
