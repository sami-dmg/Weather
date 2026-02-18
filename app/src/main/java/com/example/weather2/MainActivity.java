package com.example.weather2;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {

    EditText etCity;
    Button btnSearch;
    TextView tvCity, tvTemp, tvCondition, tvWind, tvHumidity, tvUVIndex,
            tvSunrise, tvSunset, tvDayNight,
            tvFeelsLike, tvPrecipitation, tvPressure, tvVisibility, tvLocalTime, tvAlert;
    ImageView ivWeather;
    ToggleButton toggleUnit;
    LinearLayout forecastLayout;

    RadioGroup rgTheme;
    RadioButton rbLight, rbDark, rbSystem;

    ScrollView rootLayout; // <-- Root layout for background change

    String apiKey = "9c635a9b9bdc4eb5a66150012252008";
    boolean isCelsius = true;
    String lastSearchedCity = "";

    String currentJsonData = null;

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Views ---
        rootLayout = findViewById(R.id.rootLayout);
        etCity = findViewById(R.id.etCity);
        btnSearch = findViewById(R.id.btnSearch);
        tvCity = findViewById(R.id.tvCity);
        tvTemp = findViewById(R.id.tvTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvWind = findViewById(R.id.tvWind);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvUVIndex = findViewById(R.id.tvUVIndex);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvSunset = findViewById(R.id.tvSunset);
        tvDayNight = findViewById(R.id.tvDayNight);
        ivWeather = findViewById(R.id.ivWeather);
        toggleUnit = findViewById(R.id.toggleUnit);
        forecastLayout = findViewById(R.id.forecastLayout);

        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvPrecipitation = findViewById(R.id.tvPrecipitation);
        tvPressure = findViewById(R.id.tvPressure);
        tvVisibility = findViewById(R.id.tvVisibility);
        tvLocalTime = findViewById(R.id.tvLocalTime);
        tvAlert = findViewById(R.id.tvAlert);

        rgTheme = findViewById(R.id.rgTheme);
        rbLight = findViewById(R.id.rbLight);
        rbDark = findViewById(R.id.rbDark);
        rbSystem = findViewById(R.id.rbSystem);

        // Restore saved state
        if (savedInstanceState != null) {
            lastSearchedCity = savedInstanceState.getString("lastCity");
            currentJsonData = savedInstanceState.getString("currentJson");
            isCelsius = savedInstanceState.getBoolean("isCelsius", true);
            toggleUnit.setChecked(!isCelsius);
            if (currentJsonData != null) {
                updateUI(currentJsonData);
            }
        }

        // Theme setup
        setupThemeSwitcher();

        // Search button
        btnSearch.setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (!city.isEmpty()) {
                lastSearchedCity = city;
                fetchWeather(city);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle °C / °F
        toggleUnit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isCelsius = !isChecked;
            if (!lastSearchedCity.isEmpty()) {
                fetchWeather(lastSearchedCity);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastCity", lastSearchedCity);
        outState.putString("currentJson", currentJsonData);
        outState.putBoolean("isCelsius", isCelsius);
    }

    private void setupThemeSwitcher() {
        int savedTheme = ThemeHelper.loadTheme(this);

        switch (savedTheme) {
            case ThemeHelper.THEME_LIGHT:
                rbLight.setChecked(true);
                break;
            case ThemeHelper.THEME_DARK:
                rbDark.setChecked(true);
                break;
            case ThemeHelper.THEME_SYSTEM:
            default:
                rbSystem.setChecked(true);
                break;
        }

        // Set initial background
        updateBackgroundForTheme(savedTheme);

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedTheme;
            if (checkedId == R.id.rbLight) selectedTheme = ThemeHelper.THEME_LIGHT;
            else if (checkedId == R.id.rbDark) selectedTheme = ThemeHelper.THEME_DARK;
            else selectedTheme = ThemeHelper.THEME_SYSTEM;

            ThemeHelper.saveTheme(this, selectedTheme);
            ThemeHelper.applyTheme(selectedTheme);

            updateBackgroundForTheme(selectedTheme);
        });
    }

    private void updateBackgroundForTheme(int themeMode) {
        switch (themeMode) {
            case ThemeHelper.THEME_LIGHT:
                rootLayout.setBackgroundResource(R.drawable.bg_light);
                break;
            case ThemeHelper.THEME_DARK:
                rootLayout.setBackgroundResource(R.drawable.bg_dark);
                break;
            default: // THEME_SYSTEM
                int currentNightMode = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    rootLayout.setBackgroundResource(R.drawable.bg_dark);
                } else {
                    rootLayout.setBackgroundResource(R.drawable.bg_light);
                }
                break;
        }
    }

    void fetchWeather(String city) {
        try { city = java.net.URLEncoder.encode(city, "UTF-8"); } catch (Exception e) { e.printStackTrace(); }

        String urlCurrent = "https://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + city + "&alerts=yes";
        Request requestCurrent = new Request.Builder().url(urlCurrent).build();

        client.newCall(requestCurrent).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String resData = response.body().string();
                    runOnUiThread(() -> updateUI(resData));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "City not found", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    void updateUI(String resData) {
        this.currentJsonData = resData;
        try {
            JsonObject jsonObject = JsonParser.parseString(resData).getAsJsonObject();
            JsonObject location = jsonObject.getAsJsonObject("location");
            String cityName = location.get("name").getAsString();
            String region = location.get("region").getAsString();
            String localtime = location.get("localtime").getAsString();

            JsonObject current = jsonObject.getAsJsonObject("current");
            float tempC = current.get("temp_c").getAsFloat();
            float tempF = current.get("temp_f").getAsFloat();
            float windKph = current.get("wind_kph").getAsFloat();
            String windDir = current.get("wind_dir").getAsString();
            float humidity = current.get("humidity").getAsFloat();
            float uv = current.get("uv").getAsFloat();
            float feelsLikeC = current.get("feelslike_c").getAsFloat();
            float feelsLikeF = current.get("feelslike_f").getAsFloat();
            float precipMm = current.get("precip_mm").getAsFloat();
            float pressureMb = current.get("pressure_mb").getAsFloat();
            float visibilityKm = current.get("vis_km").getAsFloat();
            JsonObject conditionObj = current.getAsJsonObject("condition");
            String condition = conditionObj.get("text").getAsString();
            String iconUrl = "https:" + conditionObj.get("icon").getAsString();
            int isDay = current.get("is_day").getAsInt();

            String alertText = "";
            if (jsonObject.has("alerts")) {
                JsonObject alerts = jsonObject.getAsJsonObject("alerts");
                JsonArray alertArray = alerts.getAsJsonArray("alert");
                if (alertArray != null && alertArray.size() > 0) {
                    JsonObject firstAlert = alertArray.get(0).getAsJsonObject();
                    String event = firstAlert.get("event").getAsString();
                    alertText = "⚠️ " + event;
                }
            }

            String localTime12h = convertTo12Hour(localtime);

            tvCity.setText(cityName + (region.isEmpty() ? "" : ", " + region));
            tvTemp.setText(isCelsius ? tempC + " °C" : tempF + " °F");
            tvCondition.setText(condition);
            tvWind.setText(String.format("Wind: %.1f kph %s", windKph, windDir));
            tvHumidity.setText(String.format("Humidity: %.0f%%", humidity));
            tvUVIndex.setText(String.format("UV Index: %.1f", uv));
            tvDayNight.setText(isDay == 1 ? "Day" : "Night");
            tvFeelsLike.setText(isCelsius ? "Feels Like: " + feelsLikeC + " °C" : "Feels Like: " + feelsLikeF + " °F");
            tvPrecipitation.setText(String.format("Precipitation: %.1f mm", precipMm));
            tvPressure.setText(String.format("Pressure: %.1f mb", pressureMb));
            tvVisibility.setText(String.format("Visibility: %.1f km", visibilityKm));
            tvLocalTime.setText("Local Time: " + localTime12h);
            tvAlert.setText(alertText);

            Glide.with(this)
                    .load(iconUrl)
                    .override(250, 250)
                    .fitCenter()
                    .into(ivWeather);

            double lat = location.get("lat").getAsDouble();
            double lon = location.get("lon").getAsDouble();
            fetchForecast(lat, lon);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
        }
    }

    void fetchForecast(double lat, double lon) {
        String urlForecast = "https://api.weatherapi.com/v1/forecast.json?key=" + apiKey +
                "&q=" + lat + "," + lon +
                "&days=7&aqi=no&alerts=no";

        Request requestForecast = new Request.Builder().url(urlForecast).build();

        client.newCall(requestForecast).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed to fetch forecast", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String resData = response.body().string();
                    runOnUiThread(() -> updateForecast(resData));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Failed to get forecast", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    void updateForecast(String resData) {
        try {
            JsonObject jsonObject = JsonParser.parseString(resData).getAsJsonObject();
            JsonObject forecast = jsonObject.getAsJsonObject("forecast");
            JsonArray forecastDays = forecast.getAsJsonArray("forecastday");

            forecastLayout.removeAllViews();

            for (int i = 0; i < forecastDays.size(); i++) {
                JsonObject day = forecastDays.get(i).getAsJsonObject();
                String date = day.get("date").getAsString();

                JsonObject dayObj = day.getAsJsonObject("day");
                float minTempC = dayObj.get("mintemp_c").getAsFloat();
                float maxTempC = dayObj.get("maxtemp_c").getAsFloat();
                float minTempF = dayObj.get("mintemp_f").getAsFloat();
                float maxTempF = dayObj.get("maxtemp_f").getAsFloat();

                JsonObject conditionObj = dayObj.getAsJsonObject("condition");
                String dayCondition = conditionObj.get("text").getAsString();
                String iconUrl = "https:" + conditionObj.get("icon").getAsString();

                String displayText = date + ": " + dayCondition + ", " +
                        (isCelsius
                                ? String.format("Min: %.1f°C, Max: %.1f°C", minTempC, maxTempC)
                                : String.format("Min: %.1f°F, Max: %.1f°F", minTempF, maxTempF));

                LinearLayout dayLayout = new LinearLayout(MainActivity.this);
                dayLayout.setOrientation(LinearLayout.HORIZONTAL);
                dayLayout.setPadding(20, 20, 20, 20);
                dayLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
                dayLayout.setBackgroundResource(R.drawable.rounded_background);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 8, 0, 8);
                dayLayout.setLayoutParams(params);

                ImageView iconView = new ImageView(MainActivity.this);
                int iconSize = (int) (40 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconParams.setMarginEnd(16);
                iconView.setLayoutParams(iconParams);

                Glide.with(MainActivity.this)
                        .load(iconUrl)
                        .override(iconSize, iconSize)
                        .fitCenter()
                        .into(iconView);

                TextView tv = new TextView(MainActivity.this);
                tv.setText(displayText);
                tv.setTextSize(16f);

                dayLayout.addView(iconView);
                dayLayout.addView(tv);

                forecastLayout.addView(dayLayout);
            }

            if (forecastDays.size() > 0) {
                JsonObject today = forecastDays.get(0).getAsJsonObject();
                JsonObject astro = today.getAsJsonObject("astro");
                String sunrise = astro.get("sunrise").getAsString();
                String sunset = astro.get("sunset").getAsString();

                tvSunrise.setText("Sunrise: " + sunrise);
                tvSunset.setText("Sunset: " + sunset);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error parsing forecast data", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertTo12Hour(String localtime24h) {
        try {
            String[] parts = localtime24h.split(" ");
            if (parts.length < 2) return localtime24h;

            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = sdf24.parse(parts[1]);
            SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf12.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return localtime24h;
        }
    }
}