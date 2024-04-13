package com.kubicix.smartnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.kubicix.smartnavigation.databinding.ActivityMapsBinding
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var duraklar: MutableList<LatLng> // Durakları saklamak için değişken ekledik

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
            "kerpe", "kandıra", "karamürsel", "başiskele"
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



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SPEECH_RECOGNIZER && resultCode == Activity.RESULT_OK) {
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
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

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
            // Son bilinen konumu al
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Konumu kontrol et ve kullan
            if (location != null) {
                // Konumu direkt olarak kullanabiliriz
                val userLocation = location

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
                val userLat = LatLng(location.latitude, location.longitude)
                nearestStop?.let { destination ->
                    // En yakın durak bulundu, rotayı çiz
                    drawRoute(userLat, destination)
                    // Navigasyonu başlat
                    //showNavigationDialog(destination)
                }
            } else {
                // Konum bilgisi yoksa, kullanıcıya bir hata mesajı gösterilebilir
                Toast.makeText(this, "Konum bilgisi alınamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            // Konum bilgisi alırken bir hata oluşursa
            Toast.makeText(this, "Konum bilgisi alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
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
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val userLocation = if (location != null) {
            LatLng(location.latitude, location.longitude)
        } else {
            // Varsayılan konum atanabilir veya hata işlemi yapılabilir
            LatLng(0.0, 0.0) // Varsayılan olarak (0, 0) konumu kullanıldı
        }

        // Mevcut konumu işaretle
        val userMarker = MarkerOptions()
            .position(userLocation)
            .title("Mevcut Konum")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        mMap.addMarker(userMarker)

        /* durak koordinatları bulunduktan sonra onları ekleyecek kod parçası
        // Durakların koordinatları
        val durakKoordinatlari = listOf(
            Pair(40.822583, 29.810823),
            Pair(40.822367, 29.810067),
            Pair(40.820559, 29.806401),
            Pair(40.819794, 29.805169),
            Pair(40.819122, 29.804325),
            Pair(40.818235, 29.803148),
            Pair(40.817903, 29.802289),
            Pair(40.817439, 29.801303),
            Pair(40.816981, 29.800312),
            Pair(40.816527, 29.799335),
            Pair(40.816071, 29.798359),
            Pair(40.815611, 29.797381),
            Pair(40.815152, 29.796404),
            Pair(40.814696, 29.795426),
            Pair(40.814238, 29.794449),
            Pair(40.813779, 29.793472),
            Pair(40.813322, 29.792494),
            Pair(40.812864, 29.791517),
            Pair(40.812407, 29.790539),
            Pair(40.811949, 29.789562)
        )

// Durakları ekleyeceğimiz listeyi başlat
        val duraklar = mutableListOf<LatLng>()

// Koordinatları kullanarak durakları ekleme
        for ((lat, lng) in durakKoordinatlari) {
            val durak = LatLng(lat, lng)
            duraklar.add(durak)
        } */


        // Mevcut konumu merkez olarak kullanarak 10 farklı durak konumu belirleme
        // daha sonra durak konumları bulunması halinde eklenecek
        duraklar = mutableListOf() // duraklar listesini başlat
        val radius = 0.01 // Yaklaşık olarak 1 kilometrelik bir yarıçap
        val random = Random()

        for (i in 0 until 10) {
            val latOffset = random.nextDouble() * radius * if (random.nextBoolean()) 1 else -1
            val lngOffset = random.nextDouble() * radius * if (random.nextBoolean()) 1 else -1

            val durak = LatLng(userLocation.latitude + latOffset, userLocation.longitude + lngOffset)
            duraklar.add(durak)
        }

        // Belirlenen her bir durak için bir işaretçi (marker) ekleme ve rengini ve simgesini özelleştirme
        for ((index, durak) in duraklar.withIndex()) {
            val durakMarker = MarkerOptions()
                .position(durak)
                .title("Durak ${index + 1}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            mMap.addMarker(durakMarker)
        }

        // Kamera açısını ve yakınlaştırmayı ayarlama
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13f))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Tıklanan işaretçi durak ise, kullanıcıya yönlendirme yap
        if (!marker.title.equals("Mevcut Konum", ignoreCase = true)) {
            showNavigationDialog(marker.position)
        }
        return false
    }

    private fun drawRoute(startPoint: LatLng, endPoint: LatLng) {
        // Başlangıç ve bitiş noktalarını işaretlemek için MarkerOptions oluştur
        val startMarkerOptions = MarkerOptions().position(startPoint).title("Başlangıç Noktası").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        val endMarkerOptions = MarkerOptions().position(endPoint).title("Bitiş Noktası").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

        // Haritaya işaretçileri (marker) ekleyerek başlangıç ve bitiş noktalarını göster
        mMap.addMarker(startMarkerOptions)
        mMap.addMarker(endMarkerOptions)

        // Rotayı çizmek için bir PolylineOptions oluştur
        val polylineOptions = PolylineOptions()
            .add(startPoint) // Başlangıç noktasını ekle
            .add(endPoint)   // Varış noktasını ekle
            .color(Color.GREEN) // Rota rengini belirle
            .width(10f)       // Rota kalınlığını belirle

        // Haritaya Polyline ekleyerek rotayı çiz
        mMap.addPolyline(polylineOptions)
    }





    private fun showNavigationDialog(destination: LatLng) {
        // Kullanıcıya hangi navigasyon uygulamasını kullanmak istediğini sormalı ve o uygulamayı başlatmalıyız
        val navigationIntentUri = Uri.parse("google.navigation:mode=w&q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps") // Google Haritalar uygulamasını belirtmek için
        startActivity(mapIntent)
    }
}


