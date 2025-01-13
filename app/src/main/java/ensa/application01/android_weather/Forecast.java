package ensa.application01.android_weather;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Activité principale pour afficher les prévisions météorologiques d'une ville donnée.
 * Cette activité récupère les données météo via l'API OpenWeather,
 * les analyse et affiche les prévisions sous forme de liste interactive.
 *
 * L'utilisateur peut saisir une ville dans un champ de texte pour obtenir les prévisions météo correspondantes.
 * Le résultat est affiché avec des informations telles que la température, la description de l'état météo, et l'icône correspondante.
 */
public class Forecast extends AppCompatActivity {

    private String CITY; // Ville pour laquelle la météo est récupérée
    private final String API = "f01e80368f05c66b03425d3f08ab1a1c"; // Clé API OpenWeather
    private LinearLayout listLayout; // Layout pour afficher les prévisions météo

    /**
     * Classe interne pour stocker les données météorologiques associées à une période spécifique.
     * Cette classe contient des informations sur la date, la température, la description du temps, et l'URL de l'icône météo.
     */
    public static class WeatherData {
        public String date; // Date de la prévision
        public String temperature; // Température de la prévision
        public String weatherDescription; // Description de l'état météo
        public String weatherIconUrl; // URL de l'icône météo

        /**
         * Constructeur pour initialiser un objet WeatherData.
         *
         * @param date La date de la prévision
         * @param temperature La température de la prévision
         * @param weatherDescription La description de l'état météo
         * @param weatherIconUrl L'URL de l'icône météo
         */
        public WeatherData(String date, String temperature, String weatherDescription, String weatherIconUrl) {
            this.date = date;
            this.temperature = temperature;
            this.weatherDescription = weatherDescription;
            this.weatherIconUrl = weatherIconUrl;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        // Initialiser les vues
        EditText editTextCity = findViewById(R.id.editTextCity);
        Button buttonHome = findViewById(R.id.buttonHome);
        Button buttonForecast = findViewById(R.id.buttonForecast);
        listLayout = findViewById(R.id.listLayout);

        // Ajouter un TextWatcher pour détecter les changements dans le champ de texte de la ville
        editTextCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                CITY = s.toString(); // Mettre à jour la ville
                new WeatherTask().execute(); // Réexécuter la tâche météo pour la nouvelle ville
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // Action du bouton "Home" pour revenir à l'activité principale
        buttonHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(Forecast.this, MainActivity.class);
            startActivity(homeIntent);
            finish();
        });

        // Action du bouton "Forecast" pour afficher les prévisions météo de la ville actuelle
        buttonForecast.setOnClickListener(v -> {
            Intent forecastIntent = new Intent(Forecast.this, Forecast.class);
            startActivity(forecastIntent);
            finish();
        });
    }

    /**
     * Classe asynchrone pour récupérer les données météorologiques à partir de l'API OpenWeather.
     * Cette tâche est exécutée en arrière-plan pour éviter de bloquer l'interface utilisateur.
     */
    protected class WeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Afficher le loader pendant la récupération des données
            findViewById(R.id.loader).setVisibility(View.VISIBLE);
            findViewById(R.id.mainContainer).setVisibility(View.GONE);
            findViewById(R.id.errorText).setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... params) {
            String response = null;
            try {
                // Construire l'URL pour appeler l'API OpenWeather
                URL url = new URL("https://api.openweathermap.org/data/2.5/forecast?q=" + CITY + "&units=metric&appid=" + API + "&lang=fr");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Vérifier si la connexion a réussi
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    response = responseBuilder.toString();
                } else {
                    throw new Exception("Échec de la récupération des données: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("WeatherData", "Erreur de récupération des données", e);
                response = null;
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("WeatherData", "Réponse: " + result); // Log pour afficher la réponse JSON

            // Si les données sont disponibles
            if (result != null) {
                try {
                    // Analyser la réponse JSON pour extraire les informations météorologiques
                    JSONObject jsonObj = new JSONObject(result);
                    JSONObject cityObj = jsonObj.getJSONObject("city");
                    String cityName = cityObj.optString("name", "N/A");
                    String country = cityObj.optString("country", "N/A");

                    // Créer une liste pour stocker les prévisions météo
                    List<WeatherData> weatherDataList = new ArrayList<>();
                    ((TextView) findViewById(R.id.address)).setText(cityName + ", " + country);

                    JSONArray forecasts = jsonObj.getJSONArray("list");
                    for (int i = 0; i < 40; i += 5) {
                        JSONObject forecast = forecasts.getJSONObject(i);
                        JSONObject main = forecast.getJSONObject("main");
                        JSONObject weather = forecast.getJSONArray("weather").getJSONObject(0);

                        String updatedAt = forecast.getString("dt_txt");
                        String temp = main.getString("temp");
                        String weatherDescription = weather.getString("description");
                        String weatherIcon = weather.getString("icon");
                        String iconUrl = "https://openweathermap.org/img/w/" + weatherIcon + ".png";

                        // Ajouter les données météo à la liste
                        WeatherData weatherData = new WeatherData(updatedAt, temp, weatherDescription, iconUrl);
                        weatherDataList.add(weatherData);
                    }

                    // Mettre à jour le layout avec les prévisions
                    updateListLayout(weatherDataList);

                    // Masquer le loader et afficher les données
                    findViewById(R.id.loader).setVisibility(View.GONE);
                    findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);
                    findViewById(R.id.errorText).setVisibility(View.GONE);

                } catch (Exception e) {
                    Log.e("WeatherData", "Erreur lors du parsing du JSON", e);
                    findViewById(R.id.loader).setVisibility(View.GONE);
                    findViewById(R.id.errorText).setVisibility(View.VISIBLE);
                }
            } else {
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Met à jour le layout avec les prévisions météo sous forme de liste.
     * Chaque prévision est affichée avec la date, l'icône météo, la température, et une description de l'état météo.
     *
     * @param weatherDataList Liste des prévisions météorologiques à afficher
     */
    protected void updateListLayout(List<WeatherData> weatherDataList) {
        listLayout.removeAllViews();

        for (WeatherData weatherData : weatherDataList) {
            // Créer une vue pour chaque prévision météo
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_edittext));
            linearLayout.setPadding(20, 20, 20, 20);
            linearLayout.setGravity(Gravity.CENTER_VERTICAL); // Alignement vertical des éléments

            // Ajouter des marges entre les vues
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, // Largeur
                    LinearLayout.LayoutParams.WRAP_CONTENT  // Hauteur
            );
            layoutParams.setMargins(0, 30, 0, 10); // Marges : haut=10, bas=10
            linearLayout.setLayoutParams(layoutParams);

            // Afficher la date
            TextView dateTextView = new TextView(this);
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(weatherData.date, inputFormat);
            String formattedDate = outputFormat.format(dateTime);
            dateTextView.setText(formattedDate);
            dateTextView.setTextColor(ContextCompat.getColor(this, R.color.white)); // Couleur du texte
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Taille du texte
            dateTextView.setPadding(0, 0, 10, 0); // Espacement entre la date et les autres éléments

            // Afficher l'icône météo
            ImageView iconImageView = new ImageView(this);
            Picasso.get().load(weatherData.weatherIconUrl).into(iconImageView);
            iconImageView.setLayoutParams(new LinearLayout.LayoutParams(80, 80)); // Taille de l'icône

            // Afficher la température
            TextView temperature = new TextView(this);
            temperature.setText(weatherData.temperature + " °C");
            temperature.setTextColor(ContextCompat.getColor(this, R.color.white)); // Couleur de la température
            temperature.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Taille de la température
            temperature.setPadding(10, 0, 10, 0); // Espacement autour de la température

            // Afficher la description de l'état météo
            TextView description = new TextView(this);
            description.setText(weatherData.weatherDescription);
            description.setTextColor(ContextCompat.getColor(this, R.color.white)); // Couleur de la description
            description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Taille de la description

            // Ajouter les éléments au layout
            linearLayout.addView(dateTextView);
            linearLayout.addView(iconImageView);
            linearLayout.addView(temperature);
            linearLayout.addView(description);

            // Ajouter le layout dans la vue principale
            listLayout.addView(linearLayout);
        }
    }

}
