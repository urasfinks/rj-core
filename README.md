# rj-core
Ядро

Периферия для каких-либо проектов на основе Promise 

# rj-vfs
Виртуальная файловая система

Суть модуля - выйти на абстракцию IO в память.
Есть файл, у него есть директория, имя и расширение.
Дальше начинается синтетика: время жизни и Supplier который загружает содержимое в память.
При каждом получении файла, происходит автоматическое продление жизни этого файла в памяти.
Самое интересное в этой истории расширяемые View.
То есть вы просто можете создать Класс наследующий интерфейс FileView и попросить предоставить файл в этом Классе.
Время жизни полностью распространяется на View экземпляры, изменение содержимого будет вызывать метод createCache.


У File есть 4 страндартных метода получения содержимого файла:

1) getBytes
1) InputStream
2) String
3) Base64

Всё остальное расширяете через View.
За изменение содержимого файла вы отвечаете полностью самостоятельно.
Для перечитывания данных вы должны вызвать метод reload, что бы повтрорно отработал Supplier загрузчик.

Так как создание экзепляра View происходит под капотом, для передачи данных можете использовать при создании файла
File.setProp(key, value), а в экземпляре View file.getProp(key)