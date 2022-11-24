package com.kx.kxsdksample.tools

import java.io.*


class WavWriter(
//    wavFile: File,
    private val path:String,
    /** 采样率 */
    private val sampleRate: Int,
    /** 通道数量  */
    private val channelCount: Int,
) {
    private lateinit var bufWriter:BufferedOutputStream
    //private val raf = RandomAccessFile(wavFile, "rwd")
    private var dataTotalLength = 0

    init {
        val f = File(path)
        if (!f.parentFile.exists()) {
            f.mkdirs()
        }
        if (f.exists()) {
            f.delete()
        }
        f.createNewFile()
        bufWriter = createFileOutputStream("${path}_pcm")

    }

    private fun createFileOutputStream(filePath:String):BufferedOutputStream {
        val f = File(filePath)
        if (!f.parentFile.exists()) {
            f.mkdirs()
        }
        if (f.exists()) {
            f.delete()
        }
        f.createNewFile()
        return BufferedOutputStream(FileOutputStream(f, true))
    }

    //添加WAV格式的文件头
    private fun writeHead(total:Long, os:FileOutputStream){
        val byteRate = 16 * sampleRate * channelCount / 8
        val totalDataLen = 36 + total
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channelCount.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (2 * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (total and 0xff).toByte()
        header[41] = (total shr 8 and 0xff).toByte()
        header[42] = (total shr 16 and 0xff).toByte()
        header[43] = (total shr 24 and 0xff).toByte()
        os.write(header, 0, 44)
    }

    fun writeData(data: ByteArray, dataLength: Int) {
//        raf.write(data, 0, dataLength)
//        if (data.size > 8) {
//            var str = ""
//            for ( i in 0..7) {
//                str = if (str.isEmpty()) {
//                    "[${data[i]}]"
//                }else {
//                    "$str,[${data[i]}]"
//                }
//            }
//            Utils.log("writeData:$str")
//        }
        bufWriter.write(data, 0, dataLength)
        bufWriter.flush()
        dataTotalLength += dataLength
    }

    fun close() {
        // 数据写完了，长度也知道了，根据长度重写文件头。
        // 按道理只需要重写关于长度的那个数据即可，但是因为文件头很小，写入很快，就全部重写吧！
//        raf.seek(0)
//        fos.
        bufWriter.close()
        val fis = FileInputStream("${path}_pcm")
        val rfos = FileOutputStream(path, true)
        Utils.log("pcm len:${fis.channel.size()}, len:$dataTotalLength")
        writeHead(fis.channel.size(),rfos)
        val buffer = ByteArray(8192)
        var count: Int
        while (fis.read(buffer).also { count = it } > 0) {
            rfos.write(buffer, 0, count)
        }
        rfos.close()
        fis.close()
//        val f = File("${path}_pcm")
//        f.delete()
        Utils.log("channels:$channelCount, sampleRage:$sampleRate")
    }



    /** 保存4个字符 */
//    private fun writeString(str: String, raf:FileOutputStream) {
//        raf.write(str.toByteArray(), 0, 4)
//    }

    /** 写入一个Int（以小端方式写入） */
//    private fun writeInt(value: Int, raf:FileOutputStream) {
//        // raf.writeInt(value) 这是以大端方式写入的
//        raf.writeByte(value ushr 0 and 0xFF)
//        raf.writeByte(value ushr 8 and 0xFF)
//        raf.writeByte(value ushr 16 and 0xFF)
//        raf.writeByte(value ushr 24 and 0xFF)
//    }
//
//    /** 写入一个Short（以小端方式写入） */
//    private fun writeShort(value: Int) {
//        // raf.writeShort(value) 这是以大端方式写入的
//        raf.write(value ushr 0 and 0xFF)
//        raf.write(value ushr 8 and 0xFF)
//    }

}
