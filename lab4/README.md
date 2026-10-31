# ЛР4: задача 6 (формула (Σx + Σy + Σz) / 3N)

Сборка (из папки lab4):
```
javac Task6.java GenPoints.java
```

Запуск:
```
java GenPoints <число_точек> <файл> [seed]
java Task6 <файл> [потоки=3] [режим=all|critical|critical-each|race]
```
Число потоков кратно 3 (по группе потоков на x, y, z); иное значение округляется вверх.

Примеры:
```
java Task6 sample.txt 3 all
java GenPoints 5000000 points.txt
java Task6 points.txt 3 all
java Task6 points.txt 6 all
```
Режим `race` намеренно без синхронизации: показывает состояние гонки (неверный результат).
