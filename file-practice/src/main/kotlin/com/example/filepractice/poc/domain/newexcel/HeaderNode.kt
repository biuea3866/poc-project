package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row

data class HeaderNode(
    val name: String,
    val key: String,
    val width: Int = 15,
    private var _repeat: Int = 1,
    private var _isVisible: Boolean = true,
    private var _children: MutableList<HeaderNode> = mutableListOf(),
) {
    val isVisible: Boolean get() = this._isVisible
    val repeat: Int get() = this._repeat
    val children: List<HeaderNode> get() = _children

    fun applyContext(context: HeaderNodeContext) {
        _repeat = context.repeat
        _isVisible = context.isVisible
    }

    fun applyContextRecursively(contextMap: Map<String, HeaderNodeContext>) {
        // 현재 노드에 적용
        contextMap[key]?.let { context ->
            applyContext(context)
        }

        // 자식 노드들에게 재귀 적용
        children.forEach { child ->
            child.applyContextRecursively(contextMap)
        }
    }

    fun replaceName(iteration: Int): String {
        return if (this.name.contains("{{i}}")) this.name.replace("{{i}}", iteration.toString())
        else this.name
    }

    fun hasChildren(): Boolean {
        return this._children.isNotEmpty()
    }

    fun writeHeader(row: Row, startCol: Int, style: CellStyle): Int {
        return writeHeaderWithRepeatIndex(row, startCol, style, repeatIndex = 0)
    }

    private fun writeHeaderWithRepeatIndex(
        row: Row,
        startCol: Int,
        style: CellStyle,
        repeatIndex: Int
    ): Int {
        var col = startCol

        repeat(repeat) { index ->
            val currentRepeatIndex = if (repeat > 1) index + 1 else repeatIndex

            if (hasChildren()) {
                // 자식 노드가 존재하면 재귀 호출
                children.forEach { child ->
                    col = child.writeHeaderWithRepeatIndex(row, col, style, currentRepeatIndex)
                }
            } else if (isVisible) {
                // 리프 노드이고, 노출 헤더면 셀 작성
                val headerName = replaceName(currentRepeatIndex)
                CellWriter.writeCell(row, col, headerName, style)
                col++
            } else {
                // 리프 노드이고, 미노출 헤더면 column 인덱스만 증가
                col++
            }
        }

        return col
    }

    fun writeData(row: Row, data: Any?, startCol: Int, extractValue: (Any, String) -> Any?): Int {
        return if (hasChildren()) {
            writeParentNodeData(row, startCol, data, extractValue)
        } else {
            writeLeafNodeData(row, startCol, data, extractValue)
        }
    }

    private fun writeParentNodeData(
        row: Row,
        startCol: Int,
        data: Any?,
        extractValue: (Any, String) -> Any?
    ): Int {
        var col = startCol
        val childDataIterator = extractChildDataIterator(data, extractValue)

        repeat(repeat) { _ ->
            val childData = childDataIterator?.takeIf { it.hasNext() }?.next()

            if (childData != null) {
                // 자식 데이터가 있으면 재귀 호출
                children.forEach { child ->
                    col = child.writeData(row, childData, col, extractValue)
                }
            } else {
                // repeat = 3인데 실제 데이터는 1개만 있으면 나머지 2개는 빈 셀 처리
                children.forEach { child ->
                    col = child.skipColumns(col)
                }
            }
        }

        return col
    }

    private fun writeLeafNodeData(
        row: Row,
        startCol: Int,
        data: Any?,
        extractValue: (Any, String) -> Any?
    ): Int {
        if (!isVisible) {
            // 리프 노드이고, 미노출 데이터면 column 인덱스만 증가
            return startCol + 1
        }

        val cellValue = data?.let { extractValue(it, key) }
        CellWriter.writeCell(row, startCol, cellValue, null)
        return startCol + 1
    }

    private fun extractChildDataIterator(
        data: Any?,
        extractValue: (Any, String) -> Any?
    ): Iterator<*>? {
        val value = data?.let { extractValue(it, key) }
        return (value as? Iterable<*>)?.iterator()
    }

    private fun skipColumns(startCol: Int): Int {
        var currentCol = startCol

        if (this.hasChildren()) {
            this.children.forEach { child ->
                currentCol = child.skipColumns(currentCol)
            }
        } else {
            currentCol++
        }

        return currentCol
    }
}