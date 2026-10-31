# ЛР3: задача 5 (геометрический центр точек)

Сборка (из папки lab3):
```
javac Task5.java GenPoints.java
```

Запуск:
```
java GenPoints <число_точек> <файл> [seed]
java Task5 <файл> [потоки=4]
```

Примеры:
```
java Task5 sample.txt 2
java GenPoints 5000000 points.txt
java Task5 points.txt 4
java Task5 points.txt 8
```
`sample.txt` - 8 точек, центр которых легко проверить вручную: (3.25; 4.75; 6.625).
Версия A - параллельный цикл (число потоков задаётся), версия B - функциональная декомпозиция (3 потока: x, y, z).
