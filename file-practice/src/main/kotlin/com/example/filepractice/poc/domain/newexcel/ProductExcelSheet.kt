package com.example.filepractice.poc.domain.newexcel

class ProductExcelSheet : AbstractExcelSheet<ProductExcelData>() {
    override val sheetType: SheetType
        get() = SheetType.PRODUCT

    override fun getSheetName(): String = "상품"

    override fun getNodes(): List<HeaderNode> = PRODUCT_HEADER_NODES_TEMPLATE

    override fun extractValue(data: Any, key: String): Any? {
        return when (data) {
            is ProductExcelData -> when (key) {
                "productId" -> data.productId
                "productName" -> data.productName
                "amount" -> data.amount
                "qty" -> data.qty
                "content" -> data.content
                "images" -> data.images
                else -> null
            }
            is ProductImage -> when (key) {
                "imageId" -> data.imageId
                "imageName" -> data.imageName
                "imageSize" -> data.imageSize
                "imageComments" -> data.imageComments
                else -> null
            }
            is ProductImageComment -> when (key) {
                "imageCommentId" -> data.imageCommentId
                "comment" -> data.comment
                else -> null
            }
            else -> null
        }
    }

    private val PRODUCT_HEADER_NODES_TEMPLATE = listOf(
        HeaderNode(name = "상품 ID", key = "productId"),
        HeaderNode(name = "상품명", key = "productName"),
        HeaderNode(name = "상품 가격", key = "amount"),
        HeaderNode(name = "상품 갯수", key = "qty"),
        HeaderNode(name = "상품 내역", key = "content"),
        HeaderNode(
            name = "상품 이미지 리스트",
            key = "images",
            _repeat = 3,
            _children = mutableListOf(
                HeaderNode(name = "상품 이미지 {{i}} - ID", key = "imageId"),
                HeaderNode(name = "상품 이미지 {{i}} - 이름", key = "imageName"),
                HeaderNode(name = "상품 이미지 {{i}} - 크기", key = "imageSize"),
                HeaderNode(
                    name = "상품 이미지 댓글 리스트",
                    key = "imageComments",
                    _repeat = 10,
                    _children = mutableListOf(
                        HeaderNode(name = "댓글 {{i}} - ID", key = "imageCommentId"),
                        HeaderNode(name = "댓글 {{i}} - 내용", key = "comment"),
                    )
                ),
            )
        )
    )
}