# Barkod Tarayıcı (Barcode Scanner) Android Uygulaması

Bu proje, Android cihazlarda kamera kullanarak barkod okuma işlevi sunan bir Kotlin uygulamasıdır.

## Özellikler

- 📷 Kamera ile gerçek zamanlı barkod tarama
- 📝 Taranan barkodları listeye ekleme
- 🔄 Otomatik barkod algılama
- 📱 Modern Material Design arayüzü
- 🌙 Gece/Gündüz tema desteği
- 🔒 Kamera izni yönetimi

## Kullanılan Teknolojiler

- **Kotlin** - Ana programlama dili
- **Android CameraX** - Kamera işlemleri için
- **ML Kit Barcode Scanning** - Google'ın makine öğrenmesi tabanlı barkod tarama API'si
- **ViewBinding** - UI bileşenlerine güvenli erişim
- **RecyclerView** - Barkod listesi görüntüleme
- **Material Design Components** - Modern UI tasarımı

## Gereksinimler

- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Kamera izni** gereklidir
- **Arka kamera** bulunmalıdır

## Kurulum ve Çalıştırma

1. **Projeyi klonlayın veya indirin**
2. **Android Studio'da açın**
3. **Gradle sync** işlemini bekleyin
4. **Android cihaz veya emülatör** bağlayın
5. **Uygulamayı çalıştırın**

### Gradle Build Komutu
```bash
./gradlew assembleDebug
```

### APK Yükleme
```bash
./gradlew installDebug
```

## Kullanım

1. **Ana Ekran:** Uygulamayı açtığınızda taranan barkodların listesini görebilirsiniz
2. **Barkod Tarama:** Sağ alt köşedeki kamera butonuna basarak barkod tarama ekranını açın
3. **Tarama İşlemi:** Kamerayı barkoda yönlendirin, otomatik olarak algılanacak
4. **Sonuç:** Taranan barkod ana ekrandaki listeye eklenir

## Proje Yapısı

```
app/
├── src/main/java/com/example/barcodescanner/
│   ├── MainActivity.kt              # Ana ekran ve liste yönetimi
│   ├── BarcodeScannerActivity.kt    # Kamera ve barkod tarama
│   ├── BarcodeAdapter.kt           # RecyclerView adaptörü
│   └── BarcodeItem.kt              # Barkod veri modeli
├── src/main/res/
│   ├── layout/                     # XML layout dosyaları
│   ├── drawable/                   # İkon ve drawable kaynakları
│   ├── values/                     # String, renk ve tema kaynakları
│   └── xml/                        # Backup ve data extraction kuralları
└── src/main/AndroidManifest.xml    # Uygulama izinleri ve aktivite tanımları
```

## İzinler

Uygulama aşağıdaki izinleri kullanır:

- `android.permission.CAMERA` - Barkod tarama için kamera erişimi
- `android.hardware.camera` - Kamera donanımı gereksinimi
- `android.hardware.camera.autofocus` - Otofokus özelliği

## Desteklenen Barkod Formatları

ML Kit API sayesinde şu barkod formatları desteklenir:

- **1D Barkodlar:** Code 128, Code 39, Code 93, Codabar, EAN-13, EAN-8, ITF, UPC-A, UPC-E
- **2D Barkodlar:** QR Code, Data Matrix, PDF417, Aztec

## Geliştirme Notları

### Kamera İşlemleri
- `CameraX` API kullanılarak modern ve güvenli kamera entegrasyonu
- `ImageAnalysis` use case ile gerçek zamanlı görüntü analizi
- Otomatik odaklama ve görüntü stabilizasyonu

### ML Kit Entegrasyonu
- Google ML Kit'in ücretsiz barkod tarama API'si
- Cihazda çalışan model (internet bağlantısı gerekmez)
- Yüksek doğruluk oranı ve hızlı algılama

### UI/UX Tasarımı
- Material Design 3 prensipleri
- Responsive tasarım
- Erişilebilirlik desteği
- Türkçe yerelleştirme

## Sorun Giderme

### Kamera Açılmıyor
- Kamera izninin verildiğinden emin olun
- Cihazın kamerası olduğundan emin olun
- Uygulamayı yeniden başlatın

### Barkod Algılanmıyor
- Barkodun net ve iyi aydınlatılmış olduğundan emin olun
- Kamerayı barkoda daha yakın veya uzak tutun
- Barkodun hasar görmediğinden emin olun

### Performans Sorunları
- Arka planda çalışan diğer uygulamaları kapatın
- Cihazın depolama alanının yeterli olduğundan emin olun

## Katkıda Bulunma

1. Bu repoyu fork edin
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add amazing feature'`)
4. Branch'inizi push edin (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için LICENSE dosyasına bakınız.

## İletişim

Sorular veya öneriler için lütfen bir GitHub issue açın.

---

**Not:** Bu uygulama eğitim amaçlı geliştirilmiştir. Ticari kullanım öncesi gerekli testler yapılmalıdır.
