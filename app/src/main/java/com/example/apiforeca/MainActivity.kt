package com.example.apiforeca

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private val forecaBaseUrl = "https://fnw-us.foreca.com"

    private var token = ""

    private val retrofit = Retrofit.Builder()
        .baseUrl(forecaBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        // Добавляем CallAdapterFactory для RxJava
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())

        .build()

    private val forecaService = retrofit.create(ForecaApi::class.java)

    private val locations = ArrayList<ForecastLocation>()
    private val adapter = LocationAdapter {
        showWeather(it)
    }

    private lateinit var searchButton: Button
    private lateinit var queryInput: EditText
    private lateinit var placeholderMessage: TextView
    private lateinit var locationsList: RecyclerView

    private fun showMessage(text: String, additionalMessage: String) {
        if (text.isNotEmpty()) {
            placeholderMessage.visibility = View.VISIBLE
            locations.clear()
            adapter.notifyDataSetChanged()
            placeholderMessage.text = text
            if (additionalMessage.isNotEmpty()) {
                Toast.makeText(applicationContext, additionalMessage, Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            placeholderMessage.visibility = View.GONE
        }
    }

    // запрос с применением RxJava сразу и на аутентификацию и на локацию и на прогноз погоды
    fun getCurrentWeather() {
        forecaService.authenticate(ForecaAuthRequest("sv05061986", "rE6LlOxViB8a"))
            .flatMap { tokenResponse ->
                // Конвертируем полученный accessToken в новый запрос
                token = tokenResponse.token

                // Переключаемся на следующий сетевой запрос
                val bearerToken = "Bearer ${tokenResponse.token}"
                forecaService.getLocation(bearerToken, queryInput.text.toString())
                    // Добавляем конвертацию результата в Pair,
                    // чтобы пробросить и результат, и access token
                    // дальше по цепочке
                    .map { Pair(it.locations, bearerToken) }
            }
            .flatMap { pairLocationsAndToken ->
                // Получаем данные из Pair
                val (locations, bearerToken) = pairLocationsAndToken
                // Опускаем обработку кейса с отсутствием локаций
                val firstLocation = locations.first()

                // Делаем запрос на текущую погоду
                forecaService.getForecast(bearerToken, firstLocation.id)
            }
            .retry { count, throwable ->
                count < 3 && throwable is HttpException && throwable.code() == 401
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { forecastResponse ->
                    // В итоговый subscribe теперь приходит прогноз
                    Log.d("RxJava", "Current forecast: ${forecastResponse.current}")
                },
                { error -> Log.e("RxJava", "Got error with auth or locations, or forecast", error) }
            )
    }

    private fun searchLocations(query: String) {
        forecaService.getLocation("Bearer $token", query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    if (response.locations.isNotEmpty()) {
                        locations.clear()
                        locations.addAll(response.locations)
                        adapter.notifyDataSetChanged()
                        showMessage("", "")
                    } else {
                        showMessage(getString(R.string.nothing_found), "")
                    }
                },
                { error ->
                    if (error is HttpException && error.code() == 401) {
                        token = ""
                        getCurrentWeather()
                    } else {
                        showMessage(getString(R.string.something_went_wrong), error.message ?: "")
                    }
                }
            )
    }

    private fun showWeather(location: ForecastLocation) {
        forecaService.getForecast("Bearer $token", location.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    response.current?.let {
                        val message = "${location.name}: ${it.temperature}°C (ощущается ${it.feelsLikeTemp}°C)"
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                },
                { error ->
                    showMessage(getString(R.string.something_went_wrong), error.message ?: "")
                }
            )
    }


// Использовалось до создания запросов посредством RxJava
//    fun authenticate() {
//        forecaService.authenticate(ForecaAuthRequest("sv05061986", "rE6LlOxViB8a"))  //JdC2NmRWDKeK rE6LIO(notnull)xViB8a
//            .enqueue(object : Callback<ForecaAuthResponse> { // из библиотеки Retrofit
//                override fun onResponse(
//                    call: Call<ForecaAuthResponse>,
//                    response: Response<ForecaAuthResponse>) {
//                   if (response.code() == 200) {
//                       token = response.body()?.token.toString()
//                       search()
//                   } else {
//                       showMessage(getString(R.string.something_went_wrong), response.code().toString())
//                   }
//                }
//
//                override fun onFailure(call: Call<ForecaAuthResponse>, t: Throwable) {
//                    showMessage(getString(R.string.something_went_wrong), t.message.toString())
//                }
//
//            })

//    private fun search() {
//        forecaService.getLocation("Bearer $token", queryInput.text.toString())
//            .enqueue(object : Callback<LocationsResponse> {
//                override fun onResponse(
//                    call: Call<LocationsResponse>,
//                    response: Response<LocationsResponse>) {
//                  when (response.code()) {
//                      200 -> {
//                          if (response.body()?.locations?.isNotEmpty() == true) {
//                              locations.clear()
//                              locations.addAll(response.body()?.locations!!)
//                              adapter.notifyDataSetChanged()
//                              showMessage("", "")
//                          } else {
//                              showMessage(getString(R.string.nothing_found), "")
//                          }
//                      }
//                      401 -> authenticate()
//                      else -> showMessage(getString(R.string.something_went_wrong), response.code().toString())
//                  }
//
//                }
//
//                override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
//                    showMessage(getString(R.string.something_went_wrong), t.message.toString())
//                }
//            })
//    }
// Использовалось до создания запросов посредством RxJava
//    private fun showWeather(location: ForecastLocation) {
//        forecaService.getForecast("Bearer $token", location.id)
//            .enqueue(object : Callback<ForecastResponce> {
//                override fun onResponse(
//                    call: Call<ForecastResponce>,
//                    response: Response<ForecastResponce>) {
//                  if (response.body()?.current != null) {
//                      val message = "${location.name} t: ${response.body()?.current?.temperature}\n(Ощущается как ${response.body()?.current?.feelsLikeTemp})"
//                      Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
//                  }
//                }
//
//                override fun onFailure(call: Call<ForecastResponce>, t: Throwable) {
//                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
//                }
//            })
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        placeholderMessage = findViewById(R.id.placeholderMessage)
        searchButton = findViewById(R.id.searchButton)
        queryInput = findViewById(R.id.queryInput)
        locationsList = findViewById(R.id.locations)

        adapter.locations = locations

        locationsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        locationsList.adapter = adapter

        searchButton.setOnClickListener {
            val query = queryInput.text.toString()
            if (query.isNotEmpty()) {
                if (token.isEmpty()) {
                    getCurrentWeather() // делает всё сразу (авторизация → локация → прогноз)
                } else {
                    searchLocations(query) // если токен уже есть
                }
            }
        }
    }

    private val disposables = CompositeDisposable() // Это предотвратит утечки памяти при пересоздании Activity

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}

