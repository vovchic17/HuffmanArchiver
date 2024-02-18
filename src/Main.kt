package huffman

import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.PriorityQueue

data class Node(
    val symbol: Byte,
    val freq: Int,
    val right: Node? = null,
    val left: Node? = null
)

class Huffman {
    fun compressFile(inputFile: String, outputFile: String) {
        val data = FileInputStream(inputFile).readBytes()
        val freqs = getFreq(data)
        val root = createTree(freqs)
        val codes = createCodes(root)
        val head = createHeader(data.size, freqs)
        val bits = compress(data, codes)
        FileOutputStream(outputFile).write(head + bits)
    }

    fun decompressFile(inputFile: String, outputFile: String) {
        val arch = FileInputStream(inputFile).readBytes()
        val (length, freqs) =  parseHeader(arch)
        val root = createTree(freqs)
        val data = decompress(arch, length, root)
        FileOutputStream(outputFile).write(data)
    }

    private fun compress(data: ByteArray, codes: MutableMap<Byte, String>): ByteArray {
        val bits = mutableListOf<Byte>()
        var sum = 0
        var bit = 1

        for (symbol in data) {
            for (c in codes[symbol]!!) {
                if (c == '1') sum = sum or bit
                if (bit < 128) bit = bit shl 1
                else {
                    bits.add(sum.toByte())
                    sum = 0
                    bit = 1
                }
            }
        }
        if (bit > 1)
            bits.add(sum.toByte())
        return bits.toByteArray()
    }

    private fun decompress(arch: ByteArray, length: Int, root: Node): ByteArray {
        var size = 0
        var curr = root
        val data = mutableListOf<Byte>()
        for (i in 260 ..< arch.size)
            for (bit in (0 ..< 8).map { 1 shl it }) {
                curr = if (arch[i].toUByte().toInt() and bit == 0)
                    curr.right!!
                else
                    curr.left!!
                if (curr.right != null)
                    continue
                if (size++ < length)
                    data.add(curr.symbol)
                curr = root
            }
        return data.toByteArray()
    }

    private fun createHeader(size: Int, freqs: MutableMap<Byte, Int>): ByteArray {
        val head = mutableListOf<Byte>()
        head.add((size and 255).toByte())
        head.add(((size shr 8) and 255).toByte())
        head.add(((size shr 16) and 255).toByte())
        head.add(((size shr 24) and 255).toByte())
        freqs.forEach {
            head.add(it.value.toByte())
        }
        return head.toByteArray()
    }

    private fun parseHeader(arch: ByteArray): Pair<Int, MutableMap<Byte, Int>> {
        val length = arch[0].toUByte().toInt() or
                (arch[1].toUByte().toInt() shl 8) or
                (arch[2].toUByte().toInt() shl 16) or
                (arch[3].toUByte().toInt() shl 24)
        val freqs = mutableMapOf<Byte, Int>()
        for (i in 0..255)
            freqs[i.toByte()] = arch[4 + i].toUByte().toInt()
        return Pair(length, freqs)
    }

    private fun createCodes(root: Node): MutableMap<Byte, String> {
        val codes = mutableMapOf<Byte, String>()
        fun next(node: Node, code: String) {
            if (node.right == null)
                codes[node.symbol] = code
            else {
                next(node.right, code + '0')
                next(node.left!!, code + '1')
            }
        }
        next(root, "")
        return codes
    }

    private fun getFreq(data: ByteArray): MutableMap<Byte, Int> {
        val freqs = mutableMapOf<Byte, Int>()
        for (i in 0..255)
            freqs[i.toByte()] = 0
        for (d in data)
            freqs[d] = freqs[d]!!.inc()
        return freqs
    }

    private fun createTree(freqs: MutableMap<Byte, Int>): Node {
        val pq = PriorityQueue<Node>(compareBy{it.freq})
        freqs.forEach { (k, v) -> pq.add(Node(k, v)) }
        while (pq.size > 1) {
            val r = pq.poll()
            val l = pq.poll()
            val next = Node(0, r.freq + l.freq, r, l)
            pq.add(next)
        }
        return pq.poll()
    }
}

fun main() {
    val huffman = Huffman()
    huffman.compressFile("input.txt", "arch.bin")
    huffman.decompressFile("arch.bin", "output.txt")
}