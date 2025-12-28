package com.example.cachepractice.redis.caching

import jakarta.persistence.*

@Entity
@Table(name = "cache_user")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column
    var age: Int = 0
) {
    fun toUser(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email,
            age = this.age
        )
    }

    companion object {
        fun from(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                name = user.name,
                email = user.email,
                age = user.age
            )
        }
    }
}
