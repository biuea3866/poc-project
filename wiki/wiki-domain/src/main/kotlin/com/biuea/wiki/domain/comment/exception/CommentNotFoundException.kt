package com.biuea.wiki.domain.comment.exception

class CommentNotFoundException(id: Long) : RuntimeException("Comment not found: $id")
