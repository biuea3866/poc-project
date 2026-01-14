package com.biuea.kotlinpractice.jvm

class ClassLoader {
    companion object {
        val value = "> Outer 클래스의 static 필드 입니다."
        fun getInstance() {
            println("> Outer 클래스의 static 메서드 호출")
        }
    }

    class Outer {
        init {
            println("> Outer 생성자 초기화")
        }
    }

    // inner 클래스
    class Inner {
        init { System.out.println("> Inner 생성자 초기화"); }

        companion object {
            val value = "> Holder 클래스의 static 필드 입니다.";

            val inner = Inner()
        }
    }
}

fun main() {
    val loader = ClassLoader()
}