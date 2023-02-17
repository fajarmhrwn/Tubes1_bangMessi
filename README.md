# Tugas Besar 1 IF2211 Strategi Algoritma Semester II Tahun 2022/2023  
## Pemanfaatan Algoritma Greedy dalam Aplikasi Permainan “Galaxio”
Galaxio adalah sebuah game battle royale yang mempertandingkan bot kapal anda dengan beberapa bot kapal yang lain. Setiap pemain akan memiliki sebuah bot kapal dan tujuan dari permainan adalah agar bot kapal anda yang tetap hidup hingga akhir permainan. Agar dapat memenangkan pertandingan, setiap bot harus mengimplementasikan strategi tertentu untuk dapat memenangkan permainan.
Algoritma greedy adalah algoritma yang memakai konsep “greedy” atau serakah dalam solusi dari persoalan optimisasi yang memaksimumkan atau meminimumkan suatu parameter. Algoritma greedy diimplementasi dengan memecah permasalahan dan membentuk solusi setiap langkahnya.

## Penjelasan Algoritma Greedy yang Digunakan
Algoritma greedy yang digunakan pada bot `bangMessi` memaksimalkan seluruh informasi yang didapatkan pada setiap tick-game yang diberikan oleh game engine `galaxio` untuk menentukan arah `heading` player dan `action` player yang akan digunakan pada tick-game selanjutnya. Penentuan `heading` dan `action` dilakukan berdasarkan urutan prioritas tindakan yang harus dilakukan berdasarkan informasi yang dimiliki pada tick-game yang sama tanpa melakukan backtracking maupun prediksi masa depan (greedy).

## Requirement Program
1. Java (min Java 11): ```https://www.oracle.com/java/technologies/downloads/#java ```
2. IntelIiJ IDEA: ```https://www.jetbrains.com/idea/ ```
3. .Net Core 3.1
4. .Net Core 5

## Menjalankan Program
### Windows
1. Download latest release `starter pack.zip` dari tautan berikut ```https://github.com/EntelectChallenge/2021-Galaxio/releases/tag/2021.3.2``` dan extract ke direktori lokal.
2. Download repository ini dan Extract ke dalam ```starter-pack/starter-bots/JavaBot```.
3. Build `JavaBot` menjadi jar.
4. Pada folder `starter-pack`, buka file `run.bat` dan ubah `dotnet ReferenceBot.dll` menjadi ```java -jar {direktori_file_jar}```.
5. Buka terminal dan arahkan ke direktori `starter-pack`.
6. Jalankan ```./run.bat``` pada terminal.
7. Extract file `starter-pack/visualiser/Galaxio Windows.zip`.
8. Jalankan ```./visualiser/Galaxio-windows/Galaxio.exe```.
9. Pada opsi `Option` isi direktori log yang mengarah pada folder ```starter-pack/logger-publish/``` dan tekan `Save`.
10. Jalankan opsi `Load` dan pilih gamelog yang diinginkan.

## Author
| Nama        | NIM           |
| ------------- |:-------------:|
| Fajar Maulana Herawan     | 13521080 |
| Mohammad Farhan Fahrezy   | 13521106 |   
| Muhammad Abdul Aziz Ghazali | 13521128| 

