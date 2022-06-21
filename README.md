# java-advanced course
Реализации заданий курса [java-advanced](https://www.kgeorgiy.info/courses/java-advanced/homeworks.html) и тестирующие модули к ним.
Для каждого задания реализован сложный и продвинутый варианты.

## ДЗ-1. Обход файлов
> Реализация: [info.kgeorgiy.ja.erov.walk](/java-solutions/info/kgeorgiy/ja/erov/walk)

1. Разработайте класс _Walk_, осуществляющий подсчет хеш-сумм файлов.
    1. Формат запуска:
        ``` java Walk <входной файл> <выходной файл>```
    2. Входной файл содержит список файлов, которые требуется обойти.
    3. Выходной файл должен содержать по одной строке для каждого файла. Формат строки:
        ```<шестнадцатеричная хеш-сумма> <путь к файлу>```
    4. Для подсчета хеш-суммы используйте алгоритм [SHA-1](https://en.wikipedia.org/wiki/SHA-1) (поддержка есть в стандартной библиотеке).
    5. Если при чтении файла возникают ошибки, укажите в качестве его хеш-суммы 40 нулей.
    6. Кодировка входного и выходного файлов — UTF-8.
    7. Если родительская директория выходного файла не существует, то соответствующий путь надо создать.
    8. Размеры файлов могут превышать размер оперативной памяти.
    9. Пример
        * Входной файл
            ```
            samples/1
            samples/12
            samples/123
            samples/1234
            samples/1
            samples/binary
            samples/no-such-file
            ```
        * Выходной файл
            ```
            356a192b7913b04c54574d18c28d46e6395428ab samples/1
            7b52009b64fd0a2a49e6d8a939753077792b0554 samples/12
            40bd001563085fc35165329ea1ff5c5ecbdbbeef samples/123
            7110eda4d09e062aa5e4a390b0a572ac0d2c0220 samples/1234
            356a192b7913b04c54574d18c28d46e6395428ab samples/1
            4916d6bdb7f78e6803698cab32d1586ea457dfc8 samples/binary
            0000000000000000000000000000000000000000 samples/no-such-file
            ```
2. Сложный вариант:
    1. Разработайте класс _RecursiveWalk_, осуществляющий подсчет хеш-сумм файлов в директориях.
    2. Входной файл содержит список файлов и директорий, которые требуется обойти. Обход директорий осуществляется рекурсивно.
    3. Пример:
        * Входной файл
            ```
            samples/binary
            samples
            samples/no-such-file
            ```
        * Выходной файл
            ```
            4916d6bdb7f78e6803698cab32d1586ea457dfc8 samples/binary
            356a192b7913b04c54574d18c28d46e6395428ab samples/1
            7b52009b64fd0a2a49e6d8a939753077792b0554 samples/12
            40bd001563085fc35165329ea1ff5c5ecbdbbeef samples/123
            7110eda4d09e062aa5e4a390b0a572ac0d2c0220 samples/1234
            4916d6bdb7f78e6803698cab32d1586ea457dfc8 samples/binary
            0000000000000000000000000000000000000000 samples/no-such-file
            ```
3. При выполнении задания следует обратить внимание на:
    * Дизайн и обработку исключений, диагностику ошибок.
    * Программа должна корректно завершаться даже в случае ошибки.
    * Корректная работа с вводом-выводом.
    * Отсутствие утечки ресурсов.
4. Требования к оформлению задания.
    * Проверяется исходный код задания.
    * Весь код должен находиться в пакете info.kgeorgiy.ja.фамилия.walk.
5. Тестирование
     * простой вариант (`Walk`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.walk/info/kgeorgiy/java/advanced/walk/WalkTest.java)
     * сложный вариант (`RecursiveWalk`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.walk/info/kgeorgiy/java/advanced/walk/RecursiveWalkTest.java)
     * продвинутый вариант (`AdvancedWalk`):
        должны проходить тесты от простого и сложного вариантов
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.walk](test-repo/artifacts/info.kgeorgiy.java.advanced.walk.jar)
    
    Для того, чтобы протестировать программу:
    
     * Скачайте
        * тесты
            * [базовый модуль](test-repo/artifacts/info.kgeorgiy.java.advanced.base.jar)
            * [тестовый модуль](test-repo/artifacts/info.kgeorgiy.java.advanced.walk.jar) (свой для каждого ДЗ)
        * [библиотеки](test-repo/lib)
     * Откомпилируйте решение домашнего задания
     * Протестируйте домашнее задание
        * Текущая директория должна:
           * содержать все скачанные `.jar` файлы;
           * содержать скомпилированное решение;
           * __не__ содержать скомпилированные самостоятельно тесты.
        * Запустите тесты:
            `java -cp . -p . -m <тестовый модуль> <вариант> <полное имя класса>`
        * Пример для простого варианта ДЗ-1:
            `java -cp . -p . -m info.kgeorgiy.java.advanced.walk Walk <полное имя класса>`

## ДЗ-2. Множество на массиве
> Реализация: [info.kgeorgiy.ja.erov.arrayset](/java-solutions/info/kgeorgiy/ja/erov/arrayset)

1. Разработайте класс _ArraySet_, реализующий неизменяемое упорядоченное множество.
    * Класс ArraySet должен реализовывать интерфейс [SortedSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/SortedSet.html) (простой вариант) или [NavigableSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/NavigableSet.html) (сложный вариант).
    * Все операции над множествами должны производиться с максимально возможной асимптотической эффективностью.
2. При выполнении задания следует обратить внимание на:
    * Применение стандартных коллекций.
    * Избавление от повторяющегося кода.
3. Тестирование
     * простой вариант (`SortedSet`): 
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.arrayset/info/kgeorgiy/java/advanced/arrayset/SortedSetTest.java)
     * сложный вариант (`NavigableSet`): 
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.arrayset/info/kgeorgiy/java/advanced/arrayset/NavigableSetTest.java)
     * продвинутый вариант (`AdvancedSet`): 
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.arrayset/info/kgeorgiy/java/advanced/arrayset/AdvancedSetTest.java)
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.arrayset](test-repo/artifacts/info.kgeorgiy.java.advanced.arrayset.jar)

## ДЗ-3. Студенты
> Реализация: [info.kgeorgiy.ja.erov.student](/java-solutions/info/kgeorgiy/ja/erov/student)

1. Разработайте класс _StudentDB_, осуществляющий поиск по базе данных студентов.
    * Класс StudentDB должен реализовывать интерфейс StudentQuery (простой вариант) или GroupQuery (сложный вариант).
    * Каждый метод должен состоять из ровно одного оператора. При этом длинные операторы надо разбивать на несколько строк.
2. При выполнении задания следует обратить внимание на:
    * применение лямбда-выражений и потоков;
    * избавление от повторяющегося кода.
3. Тестирование
     * простой вариант (`StudentQuery`):
        [интерфейс](test-repo/modules/info.kgeorgiy.java.advanced.student/info/kgeorgiy/java/advanced/student/StudentQuery.java),
        [тесты](modules/info.kgeorgiy.java.advanced.student/info/kgeorgiy/java/advanced/student/StudentQueryTest.java)
     * сложный вариант (`GroupQuery`):
        [интерфейс](test-repo/modules/info.kgeorgiy.java.advanced.student/info/kgeorgiy/java/advanced/student/GroupQuery.java),
        [тесты](modules/info.kgeorgiy.java.advanced.student/info/kgeorgiy/java/advanced/student/GroupQueryTest.java)
     * продвинутый вариант (`AdvancedQuery`):
        [интерфейс](test-repo/modules/info.kgeorgiy.java.advanced.student/info/kgeorgiy/java/advanced/student/AdvancedQuery.java),
        [тесты](modules/info.kgeorgiy.java.advanced.student/info/kgeorgiy/java/advanced/student/AdvancedQueryTest.java)
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.student](test-repo/artifacts/info.kgeorgiy.java.advanced.student.jar)

## ДЗ-4. Implementor
> Реализация: [info.kgeorgiy.ja.erov.implementor](/java-solutions/info/kgeorgiy/ja/erov/implementor)

1. Реализуйте класс _Implementor_, генерирующий реализации классов и интерфейсов.
    * Аргумент командной строки: полное имя класса/интерфейса, для которого требуется сгенерировать реализацию.
    * В результате работы должен быть сгенерирован java-код класса с суффиксом Impl, расширяющий (реализующий) указанный класс (интерфейс).
    * Сгенерированный класс должен компилироваться без ошибок.
    * Сгенерированный класс не должен быть абстрактным.
    * Методы сгенерированного класса должны игнорировать свои аргументы и возвращать значения по умолчанию.
2. В задании выделяются три варианта:
    * Простой — Implementor должен уметь реализовывать только интерфейсы (но не классы). Поддержка generics не требуется.
    * Сложный — Implementor должен уметь реализовывать и классы, и интерфейсы. Поддержка generics не требуется.
3. Тестирование
    Класс `Implementor` должен реализовывать интерфейс
    [Impler](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/Impler.java).
    
     * простой вариант (`interface`): 
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/InterfaceImplementorTest.java)
     * сложный вариант (`class`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ClassImplementorTest.java)
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.implementor](test-repo/artifacts/info.kgeorgiy.java.advanced.implementor.jar)

## ДЗ-5. Jar и Javadoc
> Реализация: [info.kgeorgiy.ja.erov.implementor](/java-solutions/info/kgeorgiy/ja/erov/implementor)
> Jar: [scripts/implementor-jar.sh](scripts/implementor-jar.sh)
> Javadoc: [scripts/implementor-javadoc.sh](scripts/implementor-javadoc.sh)

1. Jar
    1. Модифицируйте Implementor так, чтобы при запуске с аргументами -jar имя-класса файл.jar он генерировал .jar-файл с реализацией соответствующего класса (интерфейса). Для компиляции используйте код из тестов.
    2. Создайте .jar-файл, содержащий скомпилированный Implementor и сопутствующие классы.
        * Созданный .jar-файл должен запускаться командой java -jar.
        * Запускаемый .jar-файл должен принимать те же аргументы командной строки, что и класс Implementor.
    3. Для проверки, кроме исходного кода, также должны быть представлены:
        * скрипт для создания запускаемого .jar-файла, в том числе, исходный код манифеста;
        * запускаемый .jar-файл.
    4. __Сложный вариант__. Решение должно быть модуляризовано.
2. Javadoc
    1. Документируйте класс Implementor и сопутствующие классы с применением Javadoc.
        * Должны быть документированы все классы и все члены классов, в том числе private.
        * Документация должна генерироваться без предупреждений.
        * Сгенерированная документация должна содержать корректные ссылки на классы стандартной библиотеки.
    2. Для проверки, кроме исходного кода, также должны быть представлены:
        * скрипт для генерации документации (он может рассчитывать, что рядом с вашим репозиторием склонирован репозиторий курса);
        * сгенерированная документация.
3. В последующих заданиях все public и protected сущности должны быть документированы.
4. Тестирование
    Класс `Implementor` должен дополнительно реализовывать интерфейс
    [JarImpler](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/JarImpler.java).
    
     * простой вариант (`jar-interface`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/InterfaceJarImplementorTest.java)
     * сложный вариант (`jar-class`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ClassJarImplementorTest.java)
     * продвинутый вариант (`jar-advanced`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/AdvancedJarImplementorTest.java)
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.implementor](test-repo/artifacts/info.kgeorgiy.java.advanced.implementor.jar)

## ДЗ-6. Итеративный параллелизм
> Реализация: [info.kgeorgiy.ja.erov.concurrent.IterativeParallelism](/java-solutions/info/kgeorgiy/ja/erov/concurrent/IterativeParallelism.java)

1. Реализуйте класс _IterativeParallelism_, который будет обрабатывать списки в несколько потоков.
2. В простом варианте должны быть реализованы следующие методы:
    * minimum(threads, list, comparator) — первый минимум;
    * maximum(threads, list, comparator) — первый максимум;
    * all(threads, list, predicate) — проверка, что все элементы списка, удовлетворяют предикату;
    * any(threads, list, predicate) — проверка, что существует элемент списка, удовлетворяющий предикату.
3. В сложном варианте должны быть дополнительно реализованы следующие методы:
    * filter(threads, list, predicate) — вернуть список, содержащий элементы удовлетворяющие предикату;
    * map(threads, list, function) — вернуть список, содержащий результаты применения функции;
    * join(threads, list) — конкатенация строковых представлений элементов списка.
4. Во все функции передается параметр threads — сколько потоков надо использовать при вычислении. Вы можете рассчитывать, что число потоков относительно мало.
5. Не следует рассчитывать на то, что переданные компараторы, предикаты и функции работают быстро.
6. При выполнении задания __нельзя__ использовать Concurrency Utilities.
7. Рекомендуется подумать, какое отношение к заданию имеют [моноиды](https://en.wikipedia.org/wiki/Monoid).
8. Тестирование
     * простой вариант (`scalar`):
        * Класс должен реализовывать интерфейс
          [ScalarIP](test-repo/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ScalarIP.java).
        * [тесты](test-repo/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ScalarIPTest.java)
    
     * сложный вариант (`list`):
       ```info.kgeorgiy.java.advanced.concurrent list <полное имя класса>```
        * Класс должен реализовывать интерфейс
          [ListIP](test-repo/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ListIP.java).
        * [тесты](test-repo/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ListIPTest.java)
     * продвинутый вариант (`advanced`):
        * Класс должен реализовывать интерфейс
          [AdvancedIP](test-repo/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/AdvancedIP.java).
        * [тесты](test-repo/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/AdvancedIPTest.java)
    
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.concurrent](test-repo/artifacts/info.kgeorgiy.java.advanced.concurrent.jar)

## ДЗ-7. Параллельный запуск
> Реализация: [info.kgeorgiy.ja.erov.concurrent.ParallelMapperImpl](/java-solutions/info/kgeorgiy/ja/erov/concurrent/ParallelMapperImpl.java)

1. Напишите класс _ParallelMapperImpl_, реализующий интерфейс ParallelMapper.
    ```
    public interface ParallelMapper extends AutoCloseable {
        <T, R> List<R> map(
            Function<? super T, ? extends R> f,
            List<? extends T> args
        ) throws InterruptedException;
    
        @Override
        void close();
    }
    ```
    * Метод run должен параллельно вычислять функцию f на каждом из указанных аргументов (args).
    * Метод close должен останавливать все рабочие потоки.
    * Конструктор ParallelMapperImpl(int threads) создает threads рабочих потоков, которые могут быть использованы для распараллеливания.
    * К одному ParallelMapperImpl могут одновременно обращаться несколько клиентов.
    * Задания на исполнение должны накапливаться в очереди и обрабатываться в порядке поступления.
    * В реализации не должно быть активных ожиданий.
2. Доработайте класс IterativeParallelism так, чтобы он мог использовать ParallelMapper.
    * Добавьте конструктор IterativeParallelism(ParallelMapper)
    * Методы класса должны делить работу на threads фрагментов и исполнять их при помощи ParallelMapper.
    * При наличии ParallelMapper сам IterativeParallelism новые потоки создавать не должен.
    * Должна быть возможность одновременного запуска и работы нескольких клиентов, использующих один ParallelMapper.
3. Тестирование
    * простой вариант (`scalar`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.mapper/info/kgeorgiy/java/advanced/mapper/ScalarMapperTest.java)
     * сложный вариант (`list`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.mapper/info/kgeorgiy/java/advanced/mapper/ListMapperTest.java)
     * продвинутый вариант (`advanced`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.mapper/info/kgeorgiy/java/advanced/mapper/AdvancedMapperTest.java)
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.mapper](test-repo/artifacts/info.kgeorgiy.java.advanced.mapper.jar)

## ДЗ-8. Web Crawler
> Реализация: [info.kgeorgiy.ja.erov.crawler](/java-solutions/info/kgeorgiy/ja/erov/crawler)

1. Напишите потокобезопасный класс _WebCrawler_, который будет рекурсивно обходить сайты.
    1. Класс WebCrawler должен иметь конструктор
        ```
        public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost)
        ```
                    
        * downloader позволяет скачивать страницы и извлекать из них ссылки;
        * downloaders — максимальное число одновременно загружаемых страниц;
        * extractors — максимальное число страниц, из которых одновременно извлекаются ссылки;
        * perHost — максимальное число страниц, одновременно загружаемых c одного хоста. Для определения хоста следует использовать метод getHost класса URLUtils из тестов.
    2. Класс WebCrawler должен реализовывать интерфейс Crawler
        ```
        public interface Crawler extends AutoCloseable {
            Result download(String url, int depth);
        
            void close();
        }
        ```
        
        * Метод download должен рекурсивно обходить страницы, начиная с указанного URL, на указанную глубину и возвращать список загруженных страниц и файлов. Например, если глубина равна 1, то должна быть загружена только указанная страница. Если глубина равна 2, то указанная страница и те страницы и файлы, на которые она ссылается, и так далее. Этот метод может вызываться параллельно в нескольких потоках.
        * Загрузка и обработка страниц (извлечение ссылок) должна выполняться максимально параллельно, с учетом ограничений на число одновременно загружаемых страниц (в том числе с одного хоста) и страниц, с которых загружаются ссылки.
        * Для распараллеливания разрешается создать до downloaders + extractors вспомогательных потоков.
        * Загружать и/или извлекать ссылки из одной и той же страницы в рамках одного обхода (download) запрещается.
        * Метод close должен завершать все вспомогательные потоки.
    3. Для загрузки страниц должен применяться Downloader, передаваемый первым аргументом конструктора.
        ```
        public interface Downloader {
            public Document download(final String url) throws IOException;
        }
        ```
                
        * Метод download загружает документ по его адресу (URL).
        * Документ позволяет получить ссылки по загруженной странице:
            ```
            public interface Document {
                List<String> extractLinks() throws IOException;
            }
            ```
            Ссылки, возвращаемые документом, являются абсолютными и имеют схему http или https.

    4. Должен быть реализован метод main, позволяющий запустить обход из командной строки
        * Командная строка
        ```WebCrawler url [depth [downloads [extractors [perHost]]]]```
                            
        * Для загрузки страниц требуется использовать реализацию CachingDownloader из тестов.
2. Версии задания
    1. Простая — не требуется учитывать ограничения на число одновременных закачек с одного хоста (perHost >= downloaders).
    2. Полная — требуется учитывать все ограничения.
3. Задание подразумевает активное использование Concurrency Utilities, в частности, в решении не должно быть «велосипедов», аналогичных/легко сводящихся к классам из Concurrency Utilities.
4. Тестирование
    Тесты используют только внутренние данные и ничего не скачивают из интернета.
    
     * простой вариант (`easy`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/EasyCrawlerTest.java)
     * сложный вариант (`hard`):
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/HardCrawlerTest.java)
     * продвинутый вариант (`advanced`): 
        [интерфейс](test-repo/modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/AdvancedCrawler.java),
        [тесты](test-repo/modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/AdvancedCrawlerTest.java)
    
    [Интерфейсы и вспомогательные классы](test-repo/modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/)
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.crawler](test-repo/artifacts/info.kgeorgiy.java.advanced.crawler.jar)

## ДЗ-9. HelloUDP
> Реализация: [info.kgeorgiy.ja.erov.hello](/java-solutions/info/kgeorgiy/ja/erov/hello)

1. Реализуйте клиент и сервер, взаимодействующие по UDP.
2. Класс _HelloUDPClient_ должен отправлять запросы на сервер, принимать результаты и выводить их на консоль.
    * Аргументы командной строки:
        1. имя или ip-адрес компьютера, на котором запущен сервер;
        2. номер порта, на который отсылать запросы;
        3. префикс запросов (строка);
        4. число параллельных потоков запросов;
        5. число запросов в каждом потоке.
    * Запросы должны одновременно отсылаться в указанном числе потоков. Каждый поток должен ожидать обработки своего запроса и выводить сам запрос и результат его обработки на консоль. Если запрос не был обработан, требуется послать его заново.
    * Запросы должны формироваться по схеме <префикс запросов><номер потока>_<номер запроса в потоке>.
3. Класс _HelloUDPServer_ должен принимать задания, отсылаемые классом HelloUDPClient и отвечать на них.
    * Аргументы командной строки:
        1. номер порта, по которому будут приниматься запросы;
        2. число рабочих потоков, которые будут обрабатывать запросы.
    * Ответом на запрос должно быть Hello, <текст запроса>.
    * Несмотря на то, что текущий способ получения ответа по запросу очень прост, сервер должен быть рассчитан на ситуацию, когда этот процесс может требовать много ресурсов и времени.
    * Если сервер не успевает обрабатывать запросы, прием запросов может быть временно приостановлен.
4. Тестирование
    Интерфейсы
    
     * `HelloUDPClient` должен реализовывать интерфейс
        [HelloClient](test-repo/modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloClient.java)
     * `HelloUDPServer` должен реализовывать интерфейс
        [HelloServer](test-repo/modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloServer.java)
    
    Тестирование
    
     * простой вариант (`client` и `server`)
     * сложный вариант (`client-i18n` и `server-i18n`)
        * на противоположной стороне находится система, дающая ответы на различных языках
     * продвинутый вариант (`client-evil` и `server-evil`)
        * на противоположной стороне находится старая система,
          не полностью соответствующая последней версии спецификации
    
    Тестовый модуль: [info.kgeorgiy.java.advanced.hello](test-repo/artifacts/info.kgeorgiy.java.advanced.hello.jar)
    
    Исходный код тестов:
    
    * [Клиент](test-repo/modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloClientTest.java)
    * [Сервер](test-repo/modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloServerTest.java)
