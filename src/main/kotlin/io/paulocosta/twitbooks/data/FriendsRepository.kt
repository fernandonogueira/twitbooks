package io.paulocosta.twitbooks.data

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FriendsRepository : JpaRepository<Friend, Long>
