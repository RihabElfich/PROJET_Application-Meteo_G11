package ensa.application01.android_weather;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity est la principale activité de l'application météo.
 * Elle permet de récupérer les prévisions météo pour une ville donnée,
 * et d'afficher des informations comme la température, la pression, et les conditions météorologiques.
 */
public class MainActivity extends AppCompatActivity {

    /** Ville pour laquelle les prévisions sont récupérées */
    private String CITY = "TANGER";

    /** Clé API pour accéder aux données météorologiques de OpenWeather */
    private final String API = "f01e80368f05c66b03425d3f08ab1a1c";

    /**
     * Méthode appelée lors de la création de l'activité.
     * Elle configure les vues, initialise les événements, et lance la récupération des données météo.
     * @param savedInstanceState L'état précédent de l'activité (non utilisé ici).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues (EditText, Button)
        EditText editTextCity = findViewById(R.id.editTextCity);
        Button buttonHome = findViewById(R.id.buttonHome);
        Button buttonForecast = findViewById(R.id.buttonForecast);

        // Lancement de la tâche pour récupérer les données météo
        new WeatherTask().execute();

        // Ajouter un TextWatcher pour détecter les changements dans la ville
        editTextCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                CITY = s.toString(); // Mettre à jour la ville à chaque modification
                new WeatherTask().execute(); // Lancer à nouveau la récupération des données
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // Non utilisé
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Non utilisé
            }
        });

        // Actions des boutons
        buttonHome.setOnClickListener(v -> {
            // Rediriger vers MainActivity pour réinitialiser la ville
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });

        buttonForecast.setOnClickListener(v -> {
            // Rediriger vers l'activité Forecast pour afficher les prévisions détaillées
            Intent intent = new Intent(MainActivity.this, Forecast.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });
    }

    /**
     * Classe interne qui exécute une tâche asynchrone pour récupérer les données météo depuis l'API OpenWeather.
     * Les données récupérées sont ensuite affichées sur l'interface utilisateur.
     */
    protected class WeatherTask extends AsyncTask<String, Void, String> {

        /**
         * Méthode appelée avant l'exécution de la tâche en arrière-plan.
         * Affiche le loader et cache les autres vues avant la récupération des données.
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.loader).setVisibility(View.VISIBLE); // Afficher le loader
            findViewById(R.id.mainContainer).setVisibility(View.GONE); // Cacher les données principales
            findViewById(R.id.errorText).setVisibility(View.GONE); // Cacher les messages d'erreur
        }

        /**
         * Méthode exécutée en arrière-plan pour récupérer les données météo via l'API OpenWeather.
         * @param params Paramètres de la tâche (non utilisés ici).
         * @return La réponse JSON sous forme de chaîne de caractères.
         */
        @Override
        protected String doInBackground(String... params) {
            String response = null;
            try {
                // Faire une requête HTTP pour obtenir les données météo en JSON
                response = new String(new URL("https://api.openweathermap.org/data/2.5/weather?q=" + CITY + "&units=metric&appid=" + API + "&lang=fr").openStream().readAllBytes(), "UTF-8");
            } catch (Exception e) {
                response = null; // Retourner null si une erreur se produit
            }
            return response;
        }

        /**
         * Méthode appelée après l'exécution de la tâche en arrière-plan.
         * Elle traite la réponse JSON et met à jour l'interface utilisateur avec les informations météo.
         * @param result La chaîne JSON contenant les données météo.
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                // Analyser la réponse JSON
                JSONObject jsonObj = new JSONObject(result);
                JSONObject main = jsonObj.getJSONObject("main");
                JSONObject sys = jsonObj.getJSONObject("sys");
                JSONObject wind = jsonObj.getJSONObject("wind");
                JSONObject weather = jsonObj.getJSONArray("weather").getJSONObject(0);

                // Extraire les informations météorologiques
                long updatedAt = jsonObj.getLong("dt");
                String updatedAtText = "Mise à jour à: " + new SimpleDateFormat("dd/MM/yyyy hh:mm", Locale.FRENCH).format(new Date(updatedAt * 1000));
                String temp = main.getString("temp") + "°C";
                String tempMin = "Min: " + main.getString("temp_min") + "°C";
                String tempMax = "Max: " + main.getString("temp_max") + "°C";
                String pressure = main.getString("pressure");
                String humidity = main.getString("humidity");
                long sunrise = sys.getLong("sunrise");
                long sunset = sys.getLong("sunset");
                String windSpeed = wind.getString("speed");
                String weatherDescription = weather.getString("description");
                String address = jsonObj.getString("name") + ", " + sys.getString("country");

                // Mettre à jour l'interface utilisateur avec les données
                ((TextView) findViewById(R.id.address)).setText(address);
                ((TextView) findViewById(R.id.updated_at)).setText(updatedAtText);
                ((TextView) findViewById(R.id.status)).setText(weatherDescription.substring(0, 1).toUpperCase() + weatherDescription.substring(1));
                ((TextView) findViewById(R.id.temp)).setText(temp);
                ((TextView) findViewById(R.id.temp_min)).setText(tempMin);
                ((TextView) findViewById(R.id.temp_max)).setText(tempMax);
                ((TextView) findViewById(R.id.sunrise)).setText(new SimpleDateFormat("hh:mm", Locale.FRENCH).format(new Date(sunrise * 1000)));
                ((TextView) findViewById(R.id.sunset)).setText(new SimpleDateFormat("hh:mm", Locale.FRENCH).format(new Date(sunset * 1000)));
                ((TextView) findViewById(R.id.wind)).setText(windSpeed);
                ((TextView) findViewById(R.id.pressure)).setText(pressure);
                ((TextView) findViewById(R.id.humidity)).setText(humidity);

                // Afficher les données et masquer le loader
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);
                findViewById(R.id.errorText).setVisibility(View.GONE);

            } catch (Exception e) {
                // En cas d'erreur, afficher un message d'erreur
                findViewById(R.id.loader).setVisibility(View.GONE);
                findViewById(R.id.errorText).setVisibility(View.VISIBLE);
            }
        }

    }
}
