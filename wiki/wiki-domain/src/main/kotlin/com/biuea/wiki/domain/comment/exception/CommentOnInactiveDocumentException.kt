package com.biuea.wiki.domain.comment.exception

class CommentOnInactiveDocumentException(documentId: Long) :
    RuntimeException("Document $documentId is not ACTIVE. Comments can only be written on ACTIVE documents.")
