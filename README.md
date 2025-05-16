# JxslTool_Java
Консольная утилита на java для работы с xml и xslt

Установка в систему

Поместите все файлы в папку, например C:\apps\jox\:

jox.jar (после сборки)

jox.bat

picocli-4.7.4.jar

Добавьте путь в переменную PATH:

bat
setx PATH "%PATH%;C:\apps\jox"
Шаг 4: Проверка работы

cmd
jox --version
jox transform --help
jox find-xslt -d C:\xslt -i input.xml 

Примеры использования после настройки
   Первое использование (сохранит параметры):

cmd
jox transform -x template.xslt -i data.xml
jox find-xslt -d C:\templates -i input.xml
Последующие вызовы (без параметров):

cmd
jox transform -i new_data.xml  # использует сохраненный xsltPath
jox find-xslt                  # использует сохраненные dir и input
Переопределение параметров:

cmd
jox find-xslt -d C:\new_templates  # обновит только директорию
