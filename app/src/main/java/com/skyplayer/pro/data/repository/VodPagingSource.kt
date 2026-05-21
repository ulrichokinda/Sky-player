package com.skyplayer.pro.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skyplayer.pro.data.local.ChannelDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType

/**
 * PagingSource pour charger les films VOD par morceaux (Suggestion 5)
 * Optimise la RAM et la fluidité de défilement pour des listes de 10k+ films
 */
class VodPagingSource(
    private val channelDao: ChannelDao,
    private val category: String? = null
) : PagingSource<Int, Channel>() {

    override fun getRefreshKey(state: PagingState<Int, Channel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Channel> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        
        return try {
            // Dans une vraie implémentation, on ajouterait LIMIT et OFFSET au DAO
            // Pour l'exemple, on simule le chargement par morceaux
            val allVod = if (category != null) {
                // Simulé: channelDao.getVodByCategory(category, pageSize, page * pageSize)
                emptyList<Channel>() 
            } else {
                // Simulé: channelDao.getAllVod(pageSize, page * pageSize)
                emptyList<Channel>()
            }

            LoadResult.Page(
                data = allVod,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (allVod.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
