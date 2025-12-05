package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row

data class HeaderNode(
    val name: String,
    val key: String,
    val width: Int = 15,
    private var _repeat: Int = 1,
    private var _isVisible: Boolean = false,
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

    private fun render(
        row: Row,
        data: Any?,
        startCol: Int,
        currentRepeatIndex: Int = 0,
        style: CellStyle? = null,
        extractValue: ((Any, String) -> Any?)? = null
    ): Int {
        var currentCol = startCol

        if (data == null) {
            // 헤더 모드
            repeat(this.repeat) { myRepeatIndex ->
                if (this.hasChildren()) {
                    this.children.forEach { child ->
                        val indexToPass = if (this.repeat > 1) myRepeatIndex + 1 else currentRepeatIndex
                        currentCol = child.render(row, data, currentCol, indexToPass, style = style)
                    }
                } else {
                    if (isVisible) {
                        val indexToUse = if (this.repeat > 1) myRepeatIndex + 1 else currentRepeatIndex
                        CellWriter.writeCell(row, currentCol, this.replaceName(indexToUse), style = style)
                    }
                    currentCol++
                }
            }
        } else {
            // 데이터 모드
            if (this.hasChildren()) {
                val value = extractValue?.invoke(data, key)

                if (value is List<*>) {
                    val repeatCount = if (this.repeat > 1) this.repeat else value.size
                    repeat(repeatCount) { i ->
                        val itemData = value.getOrNull(i)
                        if (itemData != null) {
                            this.children.forEach { child ->
                                currentCol = child.render(row, itemData, currentCol, i, extractValue = extractValue)
                            }
                        } else {
                            // 데이터 없으면 빈 셀
                            this.children.forEach { child ->
                                currentCol = child.skipColumns(currentCol)
                            }
                        }
                    }
                } else {
                    // 일반 그룹 노드: 같은 데이터를 자식들에게 전달하여 셀 작성 분기로 보냄
                    this.children.forEach { child ->
                        currentCol = child.render(row, data, currentCol, currentRepeatIndex, extractValue = extractValue)
                    }
                }
            } else {
                // 리프 노드: isVisible이 true면 셀 작성
                if (isVisible) {
                    val excelData = extractValue?.invoke(data, key)
                    CellWriter.writeCell(row, currentCol, excelData)
                }
                currentCol++
            }
        }

        return currentCol
    }

    fun writeHeader(row: Row, startCol: Int, style: CellStyle): Int {
        return render(row, null, startCol, style = style)
    }

    fun writeData(row: Row, data: Any, startCol: Int, extractValue: (Any, String) -> Any?): Int {
        return render(row, data, startCol, extractValue = extractValue)
    }

    /**
     * 컬럼을 건너뜁니다 (데이터 없을 때 빈 셀)
     */
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