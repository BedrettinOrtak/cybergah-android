# 🛡️ Cybergah Android Uygulaması

Cybergah siber güvenlik platformunun resmi Android uygulaması.

## ✨ Özellikler

- 🌐 **WebView Tabanlı** — Tam site deneyimi, native performans
- 🔔 **Push Bildirimler** — Yeni yazı, rehber, forum cevabı bildirimleri (Firebase Cloud Messaging)
- 🎨 **Splash Screen** — Cybergah markalı açılış ekranı
- 🌙 **Dark Mode** — Sistem temasına otomatik uyum
- ↩️ **Akıllı Geri** — WebView geçmişinde gezinme
- 📥 **İndirme** — Dosya indirme desteği
- 🔗 **Deep Link** — cybergah.com linkleri direkt uygulamada açılır
- 📱 **Pull-to-Refresh** — Aşağı çekerek sayfayı yenile
- 🎬 **Fullscreen Video** — Tam ekran video desteği
- 📤 **Paylaşım** — İçerik paylaşma (JavaScript bridge)
- 🛡️ **Güvenlik** — HTTPS zorunlu, network security config

## 🏗️ Proje Yapısı

```
app/src/main/
├── java/com/cybergah/app/
│   ├── MainActivity.kt              # Ana WebView activity
│   ├── WebAppInterface.kt           # JS ↔ Android köprüsü
│   └── CybergahMessagingService.kt  # FCM bildirim servisi
└── res/
    ├── layout/activity_main.xml     # Ana layout
    ├── drawable/                     # İkonlar ve grafikler
    ├── values/                       # Renkler, temalar, string'ler
    ├── values-night/                 # Dark mode kaynakları
    ├── mipmap-anydpi-v26/           # Adaptive launcher ikonu
    └── xml/                          # Network security config
```

## 🚀 Kurulum & Build

### Gereksinimler
- Android Studio Hedgehog (2023.1.1) veya üzeri
- JDK 17
- Android SDK 34
- Firebase projesi (FCM için)

### Adımlar

1. **Repoyu klonla:**
   ```bash
   git clone git@github.com:BedrettinOrtak/cybergah-android.git
   cd cybergah-android
   ```

2. **Firebase ayarla:**
   - [Firebase Console](https://console.firebase.google.com)'da yeni proje oluştur
   - Android uygulama ekle (`com.cybergah.app`)
   - `google-services.json` dosyasını indir
   - `app/` klasörüne koy

3. **Android Studio'da aç:**
   - File → Open → cybergah-android klasörünü seç
   - Gradle sync otomatik başlayacak

4. **APK oluştur:**
   ```bash
   # Debug APK
   ./gradlew assembleDebug
   
   # Release APK (signing gerekli)
   ./gradlew assembleRelease
   ```

## 🔔 Bildirim Sistemi

### FCM Konuları (Topics)
| Topic | Açıklama |
|-------|----------|
| `all` | Tüm genel bildirimler |
| `new_content` | Yeni yazı/rehber/inceleme |
| `forum` | Forum cevapları |

### Bildirim Gönderme (Sunucu tarafı)
```python
# Django'dan FCM bildirimi gönder
import firebase_admin
from firebase_admin import messaging

message = messaging.Message(
    notification=messaging.Notification(
        title="Yeni Yazı: Güvenlik İpuçları",
        body="10 maddede siber güvenlik...",
    ),
    data={"url": "https://cybergah.com/tr/yazi/...", "type": "content"},
    topic="new_content",
)
messaging.send(message)
```

## 🔗 JavaScript Bridge

Web sayfasından Android'e erişim:

```javascript
// Uygulama mı kontrol et
if (typeof CybergahApp !== 'undefined') {
    // Uygulama versiyonunu al
    let version = CybergahApp.getAppVersion();
    
    // Toast mesajı göster
    CybergahApp.showToast("Merhaba!");
    
    // Paylaş
    CybergahApp.share("Başlık", "https://cybergah.com/...");
    
    // Cihaz teması
    let theme = CybergahApp.getDeviceTheme(); // "dark" veya "light"
}
```

## 📋 Release Checklist

- [ ] Firebase Console'da proje oluştur
- [ ] `google-services.json` ekle
- [ ] Signing key oluştur (release build için)
- [ ] ProGuard kurallarını doğrula
- [ ] APK'yı test cihazda dene
- [ ] Google Play Console'a yükle (opsiyonel)

## 📄 Lisans

Bu proje Cybergah'a aittir. Tüm hakları saklıdır.
