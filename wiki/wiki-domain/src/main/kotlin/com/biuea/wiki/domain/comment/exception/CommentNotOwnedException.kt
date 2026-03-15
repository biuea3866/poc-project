package com.biuea.wiki.domain.comment.exception

class CommentNotOwnedException(commentId: Long, userId: Long) :
    RuntimeException("User $userId does not own comment $commentId")
