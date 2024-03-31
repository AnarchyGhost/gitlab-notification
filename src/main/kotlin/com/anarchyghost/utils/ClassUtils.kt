package com.anarchyghost.utils

import java.net.URLClassLoader
import java.nio.file.Paths

class JarClassLoader(jarPaths: Set<String>) {
    private val urls = jarPaths.map { Paths.get(it).toUri().toURL() }.toTypedArray()
    private val classLoader = URLClassLoader(urls)
    fun loadClass(className: String): Any? {
        val clazz = classLoader.loadClass(className)
        return clazz?.declaredConstructors?.lastOrNull()?.newInstance()
    }
}
