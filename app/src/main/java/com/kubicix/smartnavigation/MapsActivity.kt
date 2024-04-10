package com.kubicix.smartnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.kubicix.smartnavigation.databinding.ActivityMapsBinding
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var speechRecognizer: SpeechRecognizer

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
        val citiesAndDistricts = listOf("sakarya", "istanbul", "kocaeli", "adapazarı", "izmit", "derince", "umuttepe", "düzce", "bursa", "yalova")
        val spokenWords = spokenText.toLowerCase(Locale.getDefault())

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
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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

        // Mevcut konumu merkez olarak kullanarak 10 farklı durak konumu belirleme
        val duraklar = mutableListOf<LatLng>()
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

    private fun showNavigationDialog(destination: LatLng) {
        // Kullanıcıya hangi navigasyon uygulamasını kullanmak istediğini sormalı ve o uygulamayı başlatmalıyız
        val navigationIntentUri = Uri.parse("google.navigation:mode=w&q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps") // Google Haritalar uygulamasını belirtmek için
        startActivity(mapIntent)
    }
}


