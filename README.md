# M-ONUR Aile Kontrol — Çocuk Cihazı (v0.1.0)

Ebeveyn kontrol sisteminin çocuk cihazına kurulan parçası.
Bu sürüm **silinme koruması çekirdeğini** içerir (Device Admin).

## Bu sürümde ne var
- Uygulama "cihaz yöneticisi" olarak kaydolur → çocuk doğrudan silemez.
- "Korumayı Etkinleştir" ekranı ve durum göstergesi.
- Konum izinleri manifeste eklendi (kodlaması sonraki fazda).

## Henüz yok (sonraki adımlar)
- Ebeveyn parolası + silme denemesinde bildirim
- Aileye kayıt / eşleştirme (kod veya QR)
- Uygulama kısıtlama, ekran süresi, internet filtresi
- Canlı konum takibi

## Derleme
Android Studio'da aç → Gradle senkronu → `app` çalıştır.
- compileSdk 35, minSdk 24
- Derlemede sürüm uyarısı çıkarsa bildir, güncellerim.

## Not
Uygulama görünürdür ve yalnızca kendi reşit olmayan çocuklarınızın,
sizin sahip olduğunuz cihazlarda kullanılmak içindir.
