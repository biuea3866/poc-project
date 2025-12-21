package com.example.filepractice.service

import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

@Service
class FileStorageService {

    fun saveFile(directory: String, fileName: String, bytes: ByteArray) {
        val downloadDir = File(directory)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        FileOutputStream(File(downloadDir, fileName)).use { fos ->
            fos.write(bytes)
        }
    }
}
