# ЛР1: задачи 1, 2, 3

Сборка (из папки lab1):
```
javac Task1.java Task2.java Task3.java
```

Запуск:
```
java Task1 [потоки=8]
java Task2 [потоки=8] [размер=16000] [schedule=all|static|dynamic|guided] [chunk=100] [heavy=0|1]
java Task3 [потоки=8] [способ=all|1..6]
```

Примеры:
```
java Task1 8
java Task2 8 16000 static
java Task2 8 16000 static 100
java Task2 8 16000 dynamic 100
java Task2 8 16000 guided 100
java Task2 8 16000 all 100 1
java Task3 8 1
```
(`java Task3 8 1` ... `java Task3 8 6` - шесть способов обратного вывода.)
