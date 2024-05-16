package com.kubicix.smartnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.kubicix.smartnavigation.databinding.ActivityMapsBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var baslangic: LatLng
    private lateinit var bitis: LatLng
    private lateinit var binding: ActivityMapsBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var duraklar: MutableList<LatLng>
    private lateinit var tts: TextToSpeech

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
        private const val REQUEST_SPEECH_RECOGNIZER = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // SpeechRecognizer ve Intent'i başlat
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Implementasyonu buraya ekleyin (boş bırakabilirsiniz)
            }

            override fun onBeginningOfSpeech() {
                TODO("Not yet implemented")
            }

            override fun onRmsChanged(p0: Float) {
                TODO("Not yet implemented")
            }

            override fun onBufferReceived(p0: ByteArray?) {
                TODO("Not yet implemented")
            }

            override fun onEndOfSpeech() {
                TODO("Not yet implemented")
            }

            override fun onError(p0: Int) {
                TODO("Not yet implemented")
            }

            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    val spokenText = matches[0]
                    val destination = extractCity(spokenText)
                    if (destination != null) {
                        runOnUiThread {
                            binding.targetTextView.text = "Hedef: $destination"
                        }
                    }
                }
            }

            override fun onPartialResults(p0: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }

            // Diğer RecognitionListener metodları...
        })

        // Mikrofon butonuna tıklama dinleyicisi ekleyin
        binding.micButton.setOnClickListener {
            startActivityForResult(speechRecognizerIntent, REQUEST_SPEECH_RECOGNIZER)
        }
    }

    private fun extractCity(spokenText: String): String? {
        val citiesAndDistricts = listOf(
            // Kocaeli Üniversitesi fakülte ve kampüs isimleri
            "kocaeli üniversitesi teknoloji fakültesi",
            "kocaeli üniversitesi mühendislik fakültesi",
            "kocaeli üniversitesi sağlık bilimleri fakültesi",
            "kocaeli üniversitesi iktisat fakültesi",
            "kocaeli üniversitesi fen edebiyat fakültesi",
            "kocaeli üniversitesi hukuk fakültesi",
            "kocaeli üniversitesi işletme fakültesi",
            "kocaeli üniversitesi tıp fakültesi",
            "kocaeli üniversitesi ahmet keleşoğlu eğitim fakültesi",
            "kocaeli üniversitesi çenesuyu kampüsü",
            "kocaeli üniversitesi gölcük uygulama oteli",
            "kocaeli üniversitesi kandıra uygulama kampüsü",
            // Diğer şehir ve ilçeler
            "sakarya", "istanbul", "kocaeli", "adapazarı", "izmit",
            "derince", "umuttepe", "düzce", "bursa", "yalova",
            // Kocaeli ve çevresindeki diğer ilçeler ve iller
            "gebze", "çayırova", "darıca", "dilovası", "kandıra",
            // İstanbul ve Sakarya için diğer ilçeler ve iller
            "pendik", "kadıköy", "üsküdar", "sarıyer", "şile",
            "adalar", "teşvikiye", "sakarya merkez", "serdivan", "hendek",
            // Kocaeli'nin sahil ilçeleri
            "kerpe", "kandıra", "karamürsel", "başiskele","yuvacık",
            // Özel kelimeler
            "durak"
        )
        val spokenWords = spokenText.toLowerCase(Locale.getDefault())

        // İlk olarak fakülte ve kampüs isimlerini kontrol et
        val foundUniversity = citiesAndDistricts.find { location ->
            spokenWords.contains(location)
        }

        // Eğer fakülte veya kampüs ismi bulunursa, onu dön
        if (foundUniversity != null) {
            return foundUniversity
        }

        // Bulunamazsa diğer şehir ve ilçe isimlerine bak
        val foundCity = citiesAndDistricts.find { city ->
            spokenWords.contains(city)
        }

        return foundCity
    }



    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SPEECH_RECOGNIZER) {
            if (resultCode == Activity.RESULT_OK) {
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (matches != null) {
                    val spokenText = matches[0]
                    val destination = extractCity(spokenText)
                    if (destination != null) {
                        binding.targetTextView.text = "Hedef: $destination"
                        if (::duraklar.isInitialized) {
                            navigateToNearestStop()
                        } else {
                            // duraklar listesi başlatılmamışsa uyarı ver
                            Toast.makeText(this, "Duraklar listesi henüz başlatılmadı", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Hedef belirlenemedi, telefonu titret
                        // 2 titreme hedef bulunmadı demek
                        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val pattern = longArrayOf(0, 500, 500, 500) // Titreşim deseni: başlangıç yok, 0.5 saniye titreşim, 0.5 saniye duraklama, bu işlemi 2 kez tekrar et
                            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1, titreşim desenini sadece bir kez yineler
                        } else {
                            vibrator.vibrate(longArrayOf(0, 500, 500, 500), -1)
                        }

                    }
                }
            }
            else {
                // Anlaşılamadı, tekrar söyleme isteği, telefonu titret
                // 3 titreme sesli sohbetten çıkıldı demek
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 500, 500, 500, 500, 500) // Titreşim deseni: başlangıç yok, 0.5 saniye titreşim, 0.5 saniye duraklama, bu işlemi 3 kez tekrar et
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1, titreşim desenini sadece bir kez yineler
                } else {
                    vibrator.vibrate(longArrayOf(0, 500, 500, 500, 500, 500), -1)
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(listener: (Location?) -> Location) {
        // Konum izinlerini kontrol et
        if (ActivityCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Konum izinleri yoksa, hata mesajı gönder
            Log.e("LocationUtils", "Konum izinleri eksik")
            listener.invoke(null) // null konum gönder
            return
        }

        // Konum yöneticisini oluştur
        val locationManager = this@MapsActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            // Son bilinen konumu al
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastLocation != null) {
                // Son bilinen konum varsa, dinleyiciye iletilir
                listener.invoke(lastLocation)
            } else {
                // Son bilinen konum yoksa, bir LocationListener oluşturarak dinlemeye başla
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Konum güncellemesi alındığında, dinleyiciye konumu ileterek çağrı
                        listener.invoke(location)

                        // Konum güncellemelerini dinlemeyi durdur
                        locationManager.removeUpdates(this)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                    override fun onProviderEnabled(provider: String) {}

                    override fun onProviderDisabled(provider: String) {}
                }

                // Konum güncellemelerini dinlemek için LocationManager'a istek yap
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
            }
        } catch (e: SecurityException) {
            // Konum bilgisi alırken bir hata oluşursa
            Log.e("LocationUtils", "Konum bilgisi alınamadı: ${e.message}")
            e.printStackTrace()
            listener.invoke(null) // null konum gönder
        }
    }




    private var navigateCounter = 0 // counter'ı başlat
    @SuppressLint("MissingPermission")
    private fun navigateToNearestStop() {
        // Konum izinlerini kontrol et
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Konum izinleri yoksa, izin iste
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }

        // Duraklar listesi henüz başlatılmadıysa uyarı ver ve işlemi sonlandır
        if (!::duraklar.isInitialized) {
            Toast.makeText(this, "Duraklar listesi henüz başlatılmadı", Toast.LENGTH_SHORT).show()
            return
        }

        // Konum yöneticisini oluştur
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        try {
            // Konum güncellemelerini dinlemek için bir LocationListener oluştur
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Konum güncellemesi alındığında işlenecek kodlar buraya gelecek
                    try {
                        Toast.makeText(this@MapsActivity, "Konum tespit edildi", Toast.LENGTH_SHORT).show()
                        // Konumu kullan
                        val userLocation = location
                        baslangic = convertLocationToLatLng(userLocation)
                        var nearestStop: LatLng? = null
                        var shortestDistance = Double.MAX_VALUE

                        // Tüm durakları dolaşarak en yakın olanı bul
                        for (durak in duraklar) {
                            val distance = calculateDistance(userLocation, durak)
                            if (distance < shortestDistance) {
                                shortestDistance = distance
                                nearestStop = durak
                            }
                        }
                        if (nearestStop != null) {
                            bitis = nearestStop
                        }
                        val userLat = LatLng(location.latitude, location.longitude)
                        nearestStop?.let { destination ->
                            // En yakın durak bulundu, rotayı çiz
                            drawRoute(userLat, destination)
                            // Navigasyonu başlat
                            // showNavigationDialog(destination)
                        }

                        // Mevcut kullanıcı marker'ını sil
                        userMarker?.remove()

                        // Mevcut konumu işaretle
                        userMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(userLat)
                                .title("Mevcut Konum")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon))
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    Toast.makeText(this@MapsActivity, "Konum sağlayıcı devre dışı bırakıldı", Toast.LENGTH_SHORT).show()
                }
            }

            // Tek bir konum güncellemesi almak için requestSingleUpdate yöntemini kullanın
            Toast.makeText(this, "Konum dinleme başlatıldı", Toast.LENGTH_SHORT).show()
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_FINE // veya Criteria.ACCURACY_COARSE
            criteria.isAltitudeRequired = false // Yükseklik bilgisi gerekli mi?
            criteria.isBearingRequired = false // Yön bilgisi gerekli mi?
            criteria.isSpeedRequired = false // Hız bilgisi gerekli mi?
            criteria.powerRequirement = Criteria.POWER_HIGH// Güç gereksinimi (düşük, orta, yüksek)

            locationManager.requestSingleUpdate(criteria, locationListener, null)


        } catch (e: SecurityException) {
            // Konum bilgisi alırken bir hata oluşursa
            Toast.makeText(this, "Konum bilgisi alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }


        navigateCounter++ // counter'ı artır

        // Counter'ı toast olarak göster
        //Toast.makeText(this, "Fonksiyon çağrıldı: $navigateCounter", Toast.LENGTH_SHORT).show()
    }





    private fun calculateDistance(userLocation: Location, destination: LatLng): Double {
        try {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                destination.latitude, destination.longitude,
                results
            )
            return results[0].toDouble()
        } catch (e: Exception) {
            // Hata durumunda logları kaydet
            Log.e("CalculateDistance", "Distance calculation error: ${e.message}")
            // Hata durumunda 0.0 döndür
            return 0.0
        }
    }




    private var userMarker: Marker? = null // Sınıf seviyesinde userMarker tanımlama


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)

        // Kullanıcının gerçek konumunu al
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Konum izinleri yoksa, izin iste
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }

        // Konum hizmetlerini kullanarak kullanıcının gerçek konumunu al
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        val userLocation = if (location != null) {
            LatLng(location.latitude, location.longitude)
        } else {
            // Varsayılan konum atanabilir veya hata işlemi yapılabilir
            LatLng(0.0, 0.0) // Varsayılan olarak (0, 0) konumu kullanıldı
        }

        // Mevcut konumu işaretle
        userMarker = mMap.addMarker(
            MarkerOptions()
                .position(userLocation)
                .title("Mevcut Konum")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon))
        )



        //durak koordinatları bulunduktan sonra onları ekleyecek kod parçası
        // Durakların koordinatları, api ile alınabilir
        val durakKoordinatlari = listOf(
            Pair(40.8240889, 29.9209926),
            Pair(40.8232829, 29.9251148),
            Pair(40.8217259, 29.9338225),
            Pair(40.813839, 29.9381805),
            Pair(40.7617012,29.9234567),
            Pair(40.802867, 29.933484),
            Pair(40.753435, 29.805417),
            Pair(40.754958, 29.805794),
            Pair(40.755174, 29.808015),
            Pair(40.7536699, 29.8084591)
        )


// Durakları ekleyeceğimiz listeyi başlat
        duraklar = mutableListOf()

        // Koordinatları kullanarak durakları ekleme
        for ((lat, lng) in durakKoordinatlari) {
            val durak = LatLng(lat, lng)
            duraklar.add(durak)
        }


        // Mevcut konumu merkez olarak kullanarak 10 farklı durak konumu belirleme
        // daha sonra durak konumları bulunması halinde eklenecek
        /*
        duraklar = mutableListOf() // duraklar listesini başlat
        val radius = 0.01 // Yaklaşık olarak 1 kilometrelik bir yarıçap
        val random = Random()

        for (i in 0 until 10) {
            val latOffset = random.nextDouble() * radius * if (random.nextBoolean()) 1 else -1
            val lngOffset = random.nextDouble() * radius * if (random.nextBoolean()) 1 else -1

            val durak = LatLng(userLocation.latitude + latOffset, userLocation.longitude + lngOffset)
            duraklar.add(durak)
        }
         */

        // Belirlenen her bir durak için bir işaretçi (marker) ekleme ve rengini ve simgesini özelleştirme
        for ((index, durak) in duraklar.withIndex()) {
            val durakMarker = MarkerOptions()
                .position(durak)
                .title("Durak ${index + 1}")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop)) // İkonu bus_stop drawable'ı olarak ayarla
            mMap.addMarker(durakMarker)
        }

        // Kamera açısını ve yakınlaştırmayı ayarlama
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17f))
    }

    // Marker tıklaması sonucu konum tarifi
    @SuppressLint("MissingPermission")
    override fun onMarkerClick(marker: Marker): Boolean {
        // Konum yöneticisini oluştur
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Tıklanan işaretçi durak ise, kullanıcıya yönlendirme yap
        if (!marker.title.equals("Mevcut Konum", ignoreCase = true)) {
            // Mevcut konumu güncellemek için konum güncelleme isteği gönder
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_FINE // veya Criteria.ACCURACY_COARSE
            criteria.isAltitudeRequired = false // Yükseklik bilgisi gerekli mi?
            criteria.isBearingRequired = false // Yön bilgisi gerekli mi?
            criteria.isSpeedRequired = false // Hız bilgisi gerekli mi?
            criteria.powerRequirement = Criteria.POWER_HIGH// Güç gereksinimi (düşük, orta, yüksek)

            // Konum güncellemelerini dinlemek için bir LocationListener oluştur
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Konum güncellemesi alındığında işlenecek kodlar buraya gelecek
                    try {
                        Toast.makeText(this@MapsActivity, "Konum tespit edildi", Toast.LENGTH_SHORT).show()
                        // Konumu kullan
                        val userLocation = location
                        baslangic = convertLocationToLatLng(userLocation)
                        var nearestStop: LatLng? = null
                        var shortestDistance = Double.MAX_VALUE

                        // Tüm durakları dolaşarak en yakın olanı bul
                        for (durak in duraklar) {
                            val distance = calculateDistance(userLocation, durak)
                            if (distance < shortestDistance) {
                                shortestDistance = distance
                                nearestStop = durak
                            }
                        }
                        if (nearestStop != null) {
                            bitis = nearestStop
                        }
                        val userLat = LatLng(location.latitude, location.longitude)

                        drawRoute(userLat, marker.position)
                        // Mevcut kullanıcı marker'ını sil
                        userMarker?.remove()

                        // Mevcut konumu işaretle
                        userMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(userLat)
                                .title("Mevcut Konum")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon))
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    Toast.makeText(this@MapsActivity, "Konum sağlayıcı devre dışı bırakıldı", Toast.LENGTH_SHORT).show()
                }
            }

            // Tek bir konum güncellemesi almak için requestSingleUpdate yöntemini kullanın
            Toast.makeText(this, "Konum dinleme başlatıldı", Toast.LENGTH_SHORT).show()
            locationManager.requestSingleUpdate(criteria, locationListener, null)
        }
        return false
    }

    // Polyline'leri saklamak için liste
    private val polylineList: MutableList<Polyline> = mutableListOf()

    // Google Directions API ile konum rotası oluşturma fonksyinu
    private fun drawRoute(startPoint: LatLng, endPoint: LatLng) {
        // Önceki polylineları temizleyen fonks
        clearPolylines()

        // Toast mesajları ekle
        //Toast.makeText(applicationContext, "Yol bulma isteği oluşturuluyor...", Toast.LENGTH_SHORT).show()

        // Yol bulma isteği oluştur
        val url = getDirectionsUrl(startPoint, endPoint)

        // Yol bulma isteğini başlat
        val downloadTask = DownloadTask()
        downloadTask.execute(url)
    }

    // Tüm polyline'leri temizler
    private fun clearPolylines() {
        for (polyline in polylineList) {
            polyline.remove()
        }
        polylineList.clear()
    }


    private fun getDirectionsUrl(startPoint: LatLng, endPoint: LatLng): String {
        // Yol bulma isteğinin URL'sini oluştur
        val origin = "origin=" + startPoint.latitude + "," + startPoint.longitude
        val dest = "destination=" + endPoint.latitude + "," + endPoint.longitude
        val sensor = "sensor=false"
        val mode = "mode=walking" // If you want walking directions, change to "mode=walking"
        val parameters = "$origin&$dest&$sensor&$mode"
        val output = "json"
        val apiKey = "AIzaSyBDgcmevAqDJ423-zs2esluM01A3b_mW2o" // Replace with your actual API key
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&language=tr&key=$apiKey"
    }

    private inner class DownloadTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String): String {
            val url = urls[0]
            val result = StringBuilder()
            val connection: HttpURLConnection
            try {
                val url = URL(url)
                connection = url.openConnection() as HttpURLConnection
                connection.connect()

                // Yanıtı oku
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    result.append(line)
                }

                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result.toString()
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            Log.d("DownloadTask", "Yol bulma isteği tamamlandı, sonuç: $result")
            // ParserTask'ı ana iş parçacığından çağır
            val parserTask = ParserTask()
            parserTask.execute(result)
        }
    }



    private inner class ParserTask : AsyncTask<String, Int, List<LatLng>>() {

        @SuppressLint("MissingPermission")
        override fun doInBackground(vararg jsonData: String): List<LatLng> {
            val routes: List<List<HashMap<String, String>>>
            val path: ArrayList<LatLng> = ArrayList()
            try {
                val jsonObject = JSONObject(jsonData[0])
                val parser = DirectionsJSONParser()
                val (routes, maneuvers) = parser.parse(jsonObject)
                for (i in routes.indices) {
                    val points: List<HashMap<String, String>> = routes[i]
                    for (j in points.indices) {
                        val point = points[j]
                        val lat = point["lat"]!!.toDouble()
                        val lng = point["lng"]!!.toDouble()
                        val position = LatLng(lat, lng)
                        path.add(position)
                    }
                }
                // Hedef belirlenemedi, telefonu titret
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.EFFECT_TICK))
                } else {
                    vibrator.vibrate(800)
                }

                runOnUiThread {
                    binding.directionsTextView.text = "Yol Tarifi:\n" // Başlangıç olarak "Yol Tarifi:" metnini ayarlayın

                    // Manevraları tek tek ekle
                    for (maneuver in maneuvers) {
                        binding.directionsTextView.append("\n$maneuver") // Her manevrayı yeni satırlarla ekleyin
                        binding.directionsTextView.append("\n----------------") // Satır ayrımı için pratik

                    }
                }


                // TextToSpeech nesnesini oluştur ve sesli yönlendirmeyi başlat
                tts = TextToSpeech(applicationContext) { status ->
                    if (status != TextToSpeech.ERROR) {
                        // Manevraları sesli olarak söyle
                        for (maneuver in maneuvers) {
                            tts.speak(maneuver, TextToSpeech.QUEUE_ADD, null, null)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Log.d("ParserTask", "doInBackground çalıştı ve path boyutu: ${path.size}")

            return path
        }


        // ParserTask sınıfında onPostExecute metodu içinde
        override fun onPostExecute(result: List<LatLng>?) {
            super.onPostExecute(result)
            if (result != null) {
                // Rotayı çizmek için bir PolylineOptions oluştur
                val polylineOptions = PolylineOptions()
                    .addAll(result) // Yol noktalarını ekle
                    .color(Color.BLUE) // Rota rengini belirle
                    .width(12f)       // Rota kalınlığını belirle

                // Haritaya Polyline ekleyerek rotayı çiz
                val polyline = mMap.addPolyline(polylineOptions)

                // Oluşturulan polyline'ı listeye ekle
                polylineList.add(polyline)

                //Toast.makeText(applicationContext, "Başlangıç: $baslangic, Bitiş: $bitis", Toast.LENGTH_SHORT).show()
                // Yolu takip etmek için sesli yönlendirme talimatlarını sağla
                val dirurl = getManeuversUrl(baslangic, bitis)

            } else {
                // Toast mesajı ekle
                Toast.makeText(applicationContext, "Rota çizilemedi.", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun getManeuversUrl(startPoint: LatLng, endPoint: LatLng): String {
        // Yol bulma isteğinin URL'sini oluştur
        val origin = "origin=${startPoint.latitude},${startPoint.longitude}"
        val destination = "destination=${endPoint.latitude},${endPoint.longitude}"
        val apiKey = "key=AIzaSyBDgcmevAqDJ423-zs2esluM01A3b_mW2o" // API anahtarınızı buraya ekleyin

        return "https://maps.googleapis.com/maps/api/directions/json?$origin&$destination&$apiKey"
    }





    private fun calculateAngleAndDistance(startPoint: LatLng, endPoint: LatLng): Pair<Double, Double> {
        val startLat = Math.toRadians(startPoint.latitude)
        val startLng = Math.toRadians(startPoint.longitude)
        val endLat = Math.toRadians(endPoint.latitude)
        val endLng = Math.toRadians(endPoint.longitude)

        // Açıyı hesapla
        val dLng = endLng - startLng
        val y = Math.sin(dLng) * Math.cos(endLat)
        val x = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng)
        val angle = Math.toDegrees(Math.atan2(y, x))

        // Mesafeyi hesapla
        val startPoint = convertLatLngToLocation(startPoint)
        val distance = calculateDistance(startPoint, endPoint)

        return Pair(angle, distance)
    }

    private fun createInstruction(angle: Double, distance: Double): String {
        if (distance == 0.0) {
            return "İlerleyin."
        }

        return when {
            angle in -45.0..45.0 -> "Yaklaşık ${distance.toInt()} metre ilerleyin."
            angle < -45 -> "Sol tarafa dönün ve yaklaşık ${distance.toInt()} metre ilerleyin."
            angle > 45 -> "Sağ tarafa dönün ve yaklaşık ${distance.toInt()} metre ilerleyin."
            else -> "Hedefe ilerleyin ve yaklaşık ${distance.toInt()} metre ilerleyin."
        }
    }



    // Metin okuma (TextToSpeech) özelliğini kullanarak sesli yönlendirme sağlama
    private fun speakInstruction(instruction: String) {
        // Metin okuma motorunu başlat
        tts = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.ERROR) {
                // Dil ayarını yap
                tts.language = Locale.getDefault()
                // Metni oku
                tts.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun convertLatLngToLocation(latLng: LatLng): Location {
        val location = Location("provider")
        location.latitude = latLng.latitude
        location.longitude = latLng.longitude
        return location
    }

    private fun convertLocationToLatLng(location: Location): LatLng {
        return LatLng(location.latitude, location.longitude)
    }


    //googlemapse yönlendirme kodu eğer kabul edilirse kullanılacak
    private fun showNavigationDialog(destination: LatLng) {
        // Kullanıcıya hangi navigasyon uygulamasını kullanmak istediğini sormalı ve o uygulamayı başlatmalıyız
        val navigationIntentUri = Uri.parse("google.navigation:mode=w&q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps") // Google Haritalar uygulamasını belirtmek için
        startActivity(mapIntent)
    }


}