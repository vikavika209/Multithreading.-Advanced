# ImageProcessingService

## Описание

Класс `ImageProcessingService` предназначен для обработки изображений, включая удаление фона и конвертацию изображений в различные форматы (например, JPEG, PNG, SVG). Этот сервис также поддерживает параллельную обработку изображений с использованием многопоточности.

## Основные методы

### `convertImages`

Этот метод принимает массив файлов (`MultipartFile[]`), формат выходного изображения (`String`) и качество выходного изображения (`float`). Он конвертирует каждое изображение, удаляя фон, и возвращает массив байтов для каждого изображения в заданном формате.

**Пример использования:**

```java
MultipartFile[] files = ...; // файлы для конвертации
String format = "png"; // целевой формат
float quality = 0.8f; // качество для JPEG, игнорируется для PNG и SVG

ImageProcessingService service = new ImageProcessingService(svgOptimizationService);
byte[][] result = service.convertImages(files, format, quality);
```

### `convertImagesParallel`

Этот метод аналогичен `convertImages`, но использует параллельную обработку для повышения производительности. Он выполняет все операции в отдельных потоках, используя `ForkJoinPool`.

**Пример использования:**

```java
MultipartFile[] files = ...; // файлы для конвертации
String format = "png"; // целевой формат
float quality = 0.8f; // качество для JPEG, игнорируется для PNG и SVG

ImageProcessingService service = new ImageProcessingService(svgOptimizationService);
byte[][] result = service.convertImagesParallel(files, format, quality);
```

## Внутренние методы и логика

### `findDominantColor`

Этот метод находит доминирующий цвет в углах изображения. Используется для определения фона, который можно удалить.

### `addColorCount`

Помогает подсчитать количество каждого цвета в изображении.

### `isBackgroundColor`

Проверяет, является ли цвет фоновым с учетом допустимого отклонения (tolerance).

### `removeBackground`

Удаляет фон изображения, заменяя фоновые пиксели на прозрачные.

### `bufferedImageToSvg`

Конвертирует `BufferedImage` в SVG-формат.

### `bufferedImageToByteArray`

Конвертирует `BufferedImage` в массив байтов заданного формата (JPEG, PNG).

## Пример реализации сервиса

```java
@Service
public class SvgOptimizationService {
    public byte[] optimizeSvg(byte[] svgBytes) {
        // Логика оптимизации SVG
        return svgBytes;
    }
}

ImageProcessingService imageProcessingService = new ImageProcessingService(new SvgOptimizationService());
```

## Поддержка многопоточности

Этот сервис использует `ForkJoinPool` для параллельной обработки изображений, что позволяет значительно ускорить обработку большого количества изображений. Метод `convertImagesParallel` использует эту возможность для параллельной конвертации изображений.

**Пример использования многопоточности:**

```java
ForkJoinPool forkJoinPool = new ForkJoinPool();
forkJoinPool.submit(() -> {
    IntStream.range(0, files.length)
        .parallel()
        .mapToObj(i -> ...)
        .toArray(byte[][]::new);
}).join();
```
# SvgOptimizationService

`SvgOptimizationService` - это класс, который предоставляет методы для оптимизации SVG (Scalable Vector Graphics) изображений. Этот сервис может использоваться как часть более крупного приложения для обработки изображений, преобразования форматов и оптимизации.

## Содержание

- [Описание](#описание)
- [Используемые библиотеки](#используемые-библиотеки)
- [Методы класса](#методы-класса)
    - [optimizeSvg](#optimizeSvg)
- [Логирование](#логирование)
- [Пример использования](#пример-использования)
- [Заключение](#заключение)

## Описание

Класс `SvgOptimizationService` предоставляет единственный публичный метод `optimizeSvg`, который принимает на вход SVG-данные в виде массива байтов, оптимизирует их и возвращает обратно в виде массива байтов.

Этот класс также логирует все основные этапы процесса оптимизации.

## Используемые библиотеки

В этом классе используются следующие библиотеки:
- `org.apache.batik.transcoder`: Для преобразования и оптимизации SVG.
- `java.util.logging.Logger`: Для логирования событий.
- `org.springframework.stereotype.Service`: Для обозначения класса как Spring-сервис.

## Методы класса

### optimizeSvg

```java
public byte[] optimizeSvg(byte[] svgData) throws IOException {
    ...
}
```

Этот метод принимает массив байтов с данными SVG и возвращает оптимизированную версию этих данных также в виде массива байтов.

- **Входные параметры:**
    - `svgData`: Массив байтов, содержащий SVG-данные для оптимизации.

- **Возвращаемое значение:**
    - Массив байтов с оптимизированными SVG-данными.

- **Логика метода:**
    1. Логирует начало процесса оптимизации.
    2. Проверяет, чтобы входные данные не были null или пустыми. В противном случае выбрасывает `IllegalArgumentException`.
    3. Конвертирует входной массив байтов в строку UTF-8.
    4. Использует `TranscoderInput` и `TranscoderOutput` для подготовки данных для трансформации.
    5. Создает экземпляр `SVGTranscoder` и использует его для трансформации входных данных.
    6. Логирует завершение процесса оптимизации.
    7. Возвращает оптимизированные данные в виде массива байтов.

## Логирование

Для логирования используется `java.util.logging.Logger`. Логируются основные этапы процесса:
- Начало и завершение оптимизации.
- Любые исключения, возникающие в процессе, включая `TranscoderException` и другие неожиданные исключения.

### Пример логов:

```
Sep 14, 2023 10:10:10 AM com.edu.imageconversion.services.SvgOptimizationService optimizeSvg
INFO: Starting SVG optimization

Sep 14, 2023 10:10:10 AM com.edu.imageconversion.services.SvgOptimizationService optimizeSvg
INFO: Transcoding SVG...

Sep 14, 2023 10:10:11 AM com.edu.imageconversion.services.SvgOptimizationService optimizeSvg
INFO: Transcoding completed.
```

## Пример использования

```java
@Service
public class ImageConversionService {
    
    private final SvgOptimizationService svgOptimizationService;
    
    public ImageConversionService(SvgOptimizationService svgOptimizationService) {
        this.svgOptimizationService = svgOptimizationService;
    }
    
    public byte[] optimizeSvgFile(MultipartFile file) throws IOException {
        byte[] svgData = file.getBytes();
        return svgOptimizationService.optimizeSvg(svgData);
    }
}
```
# ZipService

`ZipService` - это класс, предоставляющий возможность архивирования нескольких файлов в формат ZIP. Этот сервис можно использовать для пакетной обработки файлов и их сжатия в один архив.

## Описание

Класс `ZipService` предоставляет единственный публичный метод `zipFiles`, который принимает на вход массив байтовых массивов (файлов) и формат файла, и возвращает архивированный контент в виде байтового массива.

## Содержание

- [Используемые библиотеки](#используемые-библиотеки)
- [Методы](#методы)
    - [zipFiles](#zipFiles)
- [Пример использования](#пример-использования)

## Используемые библиотеки

В этом классе используются следующие библиотеки:

- `java.io.ByteArrayOutputStream`: Для создания потока байтов, в который записывается архив.
- `java.io.IOException`: Для обработки возможных ошибок ввода-вывода.
- `java.util.zip.ZipEntry`: Для представления отдельного файла в архиве ZIP.
- `java.util.zip.ZipOutputStream`: Для записи файлов в формат ZIP.
- `org.springframework.stereotype.Service`: Для обозначения класса как Spring-сервис.

## Методы

### zipFiles

```java
public byte[] zipFiles(byte[][] files, String format) throws IOException {
    ...
}
```

Этот метод принимает массив байтовых массивов с файлами и формат файла в виде строки и возвращает архивированный контент в виде байтового массива.

- **Входные параметры:**
    - `files`: Массив байтовых массивов, каждый из которых представляет один файл для архивирования.
    - `format`: Формат файлов (например, `png`, `jpg`), который будет использован для именования файлов внутри архива.

- **Возвращаемое значение:**
    - Байтовый массив, представляющий ZIP-архив.

- **Логика метода:**
    1. Создает экземпляр `ByteArrayOutputStream` для записи байтов.
    2. Создает экземпляр `ZipOutputStream`, используя созданный поток байтов.
    3. В цикле проходит по всем файлам, добавляя каждый файл как новый `ZipEntry` в архив.
    4. Записывает содержимое каждого файла в текущий ZIP-элемент и закрывает его.
    5. Возвращает результат в виде байтового массива.

### Исключения

Метод бросает исключение `IOException` в случае проблем с вводом-выводом. Ресурсы закрываются в блоке `try-with-resources`, что гарантирует освобождение любых занятых ресурсов.

## Пример использования

```java
@Service
public class FileController {

    private final ZipService zipService;

    public FileController(ZipService zipService) {
        this.zipService = zipService;
    }

    public byte[] createZipArchive(byte[][] files, String format) throws IOException {
        return zipService.zipFiles(files, format);
    }
}
```

В этом примере `FileController` использует `ZipService` для создания ZIP-архива из переданных файлов.
