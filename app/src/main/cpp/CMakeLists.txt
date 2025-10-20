cmake_minimum_required(VERSION 3.22.1)
project("native-lib")

# выполняется только если изменён
message("!!! CMAKE RECEIVED: AES_KEY = ${AES_KEY}")
message("!!! CMAKE RECEIVED: AES_IV = ${AES_IV}")
# Добавляем все наши исходники: и библиотеку AES, и наш код
add_library(native-lib SHARED aes.c native-lib.cpp)

# Для native-lib явно указываем определения препроцессора
# PRIVATE: Означает, что эти определения видны
# только при компиляции самой библиотеки, а не тем, кто будет ее использовать
target_compile_definitions(native-lib PRIVATE
    AES_KEY="${AES_KEY}"
    AES_IV="${AES_IV}"
)

find_library(log-lib log)
target_link_libraries(native-lib ${log-lib})