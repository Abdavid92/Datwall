package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IEventDao
import com.smartsolutions.paquetes.repositories.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EventRepository @Inject constructor(
    private val dao: IEventDao
) : IEventRepository {

    override fun flow(): Flow<List<Event>> = dao.flow()

    override suspend fun all(): List<Event> = withContext(Dispatchers.IO){
        dao.all()
    }

    override suspend fun create(vararg event: Event) = withContext(Dispatchers.IO) {
        dao.create(event.toList())
    }

    override suspend fun update(vararg event: Event) = withContext(Dispatchers.IO) {
        dao.update(event.toList())
    }

    override suspend fun delete(vararg event: Event) = withContext(Dispatchers.IO) {
        dao.delete(event.toList())
    }

}