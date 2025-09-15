Project for Faculty course - Development of mobile applications 

---

# CycleLink 🚲

**CycleLink** je Android aplikacija razvijena u Kotlinu korišćenjem Jetpack Compose-a, napravljena kao studentski projekat za predmet "Razvoj mobilnih aplikacija i servisa". Aplikacija implementira koncept "Mobile Crowd Sensing & Collaboration" kako bi povezala bicikliste, omogućila im da organizuju zajedničke vožnje, prate jedni druge na mapi u realnom vremenu i takmiče se na rang listi.

## ✨ Ključne Funkcionalnosti

Aplikacija se sastoji iz mobilnog i serverskog dela, sa Firebase-om kao backend platformom.

### 1. Korisnički Nalozi i Autentifikacija
- **Registracija i Prijava:** Korisnici mogu da kreiraju nalog pomoću email adrese i šifre. Implementirana je validacija unosa kako bi se osigurala ispravnost email formata.
- **Verifikacija Emaila:** Prilikom registracije, korisniku se šalje verifikacioni email, čime se osigurava validnost naloga.
- **Upravljanje Profilom:** Korisnici mogu da uređuju svoj profil, uključujući korisničko ime, ime, prezime, opis (bio) i profilnu sliku.
- **Isecanje Slike:** Aplikacija koristi modernu biblioteku za isecanje (cropping) slika, omogućavajući korisnicima da savršeno uklope svoju sliku u kružni format.
- **Brisanje Naloga:** Implementirana je sigurna procedura za brisanje naloga koja zahteva ponovnu autentifikaciju korisnika.

### 2. Interaktivna Mapa i Vožnje
- **Prikaz Mape:** `HomeScreen` prikazuje Google Mapu sa trenutnom lokacijom korisnika.
- **Kreiranje Vožnji:** Korisnici mogu da kreiraju nove vožnje postavljanjem niza tačaka (waypoints) na mapi. Svaka vožnja ima naziv, opis, težinu (laka, srednja, teška) i zakazano vreme polaska.
- **Prava Ruta i Distanca:** Korišćenjem **Google Directions API**-ja, aplikacija automatski izračunava i prikazuje stvarnu, drumsku putanju i njenu ukupnu dužinu u kilometrima.
- **Prikaz Vožnji:** Sve aktivne, planirane vožnje su prikazane na mapi markerima na svojim početnim tačkama. Klikom na marker, iscrtava se cela ruta sa svim definisanim tačkama.
- **Brisanje Vožnji:** Autor vožnje može da je obriše.

### 3. Kolaboracija i Praćenje u Realnom Vremenu
- **Deljenje Lokacije:** Korisnici mogu da započnu "solo vožnju" i dele svoju lokaciju u realnom vremenu. Ova funkcionalnost radi i kada je aplikacija u pozadini, zahvaljujući **Foreground Service**-u.
- **Prikaz Aktivnih Biciklista:** Na mapi se prikazuju prilagođeni markeri sa profilnim slikama svih korisnika koji trenutno dele svoju lokaciju.
- **Pridruživanje i Napuštanje Vožnji:** Korisnici se mogu pridružiti ili napustiti organizovane vožnje, a lista učesnika se dinamički ažurira.
- **Lista Učesnika:** Klikom na broj učesnika, otvara se panel sa listom svih prijavljenih korisnika.
- **Pregled Profila:** Moguće je videti profil bilo kog učesnika klikom na njegovo ime u listi.

### 4. Filtriranje i Notifikacije
- **Filtriranje Vožnji:** `HomeScreen` poseduje napredni sistem za filtriranje vožnji po težini, radijusu od trenutne lokacije korisnika i opsegu datuma.
- **Lokalne Notifikacije:** Aplikacija šalje **lokalnu notifikaciju** korisniku kada se drugi aktivni biciklista nađe u njegovoj blizini (unutar 500 metara).

### 5. Gamifikacija
- **Sistem Bodovanja:** Korisnici dobijaju poene za aktivnosti kao što su kreiranje vožnje (+15 poena) i pridruživanje vožnji (+5 poena).
- **Rang Lista (Leaderboard):** Aplikacija sadrži poseban ekran sa rang listom svih verifikovanih korisnika, sortiranih po broju sakupljenih poena, sa vizuelno istaknutim podijumom za prva tri mesta.

## 🛠️ Tehnologije i Arhitektura

- **Jezik:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Arhitektura:** MVVM (Model-View-ViewModel) pristup
- **Navigacija:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Asinhrone Operacije:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- **Backend:** [Firebase](https://firebase.google.com/)
  - **Authentication:** Za upravljanje korisničkim nalozima.
  - **Cloud Firestore:** Kao NoSQL real-time baza podataka za čuvanje podataka o korisnicima i vožnjama.
  - **Storage:** Za skladištenje profilnih slika.
- **Mape i Lokacija:**
  - [Google Maps Compose Library](https://github.com/googlemaps/android-maps-compose)
  - [Google Play Services Location](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary)
  - [Google Directions API](https://developers.google.com/maps/documentation/directions/overview)
- **Učitavanje Slika:** [Coil](https://coil-kt.github.io/coil/)
- **Isecanje Slika:** [Android Image Cropper](https://github.com/CanHub/Android-Image-Cropper)

## 🚀 Pokretanje Projekta

1.  Klonirajte repozitorijum: `git clone [URL]`
2.  Otvorite projekat u Android Studiju.
3.  Povežite projekat sa svojim Firebase projektom i preuzmite `google-services.json` fajl u `app/` direktorijum.
4.  Kreirajte `gradle.properties` fajl u root direktorijumu projekta.
5.  Dodajte svoj Google Maps API ključ u `gradle.properties`:
    ```properties
    MAPS_API_KEY=VAS_API_KLJUC_OVDE
    ```
6.  Pokrenite build i pokrenite aplikaciju.

---