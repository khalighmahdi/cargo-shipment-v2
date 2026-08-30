package com.example.cargo.data

import kotlinx.coroutines.flow.Flow

class ContactRepository(private val dao: ContactDao) {

    val allContacts: Flow<List<Contact>> = dao.getAll()

    fun search(query: String): Flow<List<Contact>> = dao.search(query)
    fun getById(id: Int): Flow<Contact?> = dao.getById(id)

    suspend fun insert(contact: Contact): Long = dao.insert(contact)
    suspend fun update(contact: Contact) = dao.update(contact)
    suspend fun delete(contact: Contact) = dao.delete(contact)
    suspend fun getByPhone(phone: String): Contact? = dao.getByPhone(phone)
}
