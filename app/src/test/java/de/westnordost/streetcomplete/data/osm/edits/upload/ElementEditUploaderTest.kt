package de.westnordost.streetcomplete.data.osm.edits.upload

import de.westnordost.osmapi.common.errors.OsmConflictException
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApi
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenQuestChangesetsManager
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.upload.ConflictException
import de.westnordost.streetcomplete.testutils.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doThrow

class ElementEditUploaderTest {

    private lateinit var changesetManager: OpenQuestChangesetsManager
    private lateinit var mapDataApi: MapDataApi
    private lateinit var uploader: ElementEditUploader

    @Before fun setUp() {
        changesetManager = mock()
        mapDataApi = mock()

        uploader = ElementEditUploader(changesetManager, mapDataApi)
    }

    @Test(expected = ConflictException::class)
    fun `throws deleted exception if node is no more`() {
        on(mapDataApi.getNode(anyLong())).thenReturn(null)
        uploader.upload(edit(elementType = ElementType.NODE, elementId = 12), mock())
    }

    @Test(expected = ConflictException::class)
    fun `throws deleted exception if way is no more`() {
        on(mapDataApi.getWay(anyLong())).thenReturn(null)
        uploader.upload(edit(elementType = ElementType.WAY, elementId = 12), mock())
    }

    @Test(expected = ConflictException::class)
    fun `throws deleted exception if relation is no more`() {
        on(mapDataApi.getRelation(anyLong())).thenReturn(null)
        uploader.upload(edit(elementType = ElementType.RELATION, elementId = 12), mock())
    }

    @Test(expected = ConflictException::class)
    fun `passes on element conflict exception`() {
        val node = node(1)
        on(mapDataApi.getNode(anyLong())).thenReturn(node)
        on(changesetManager.getOrCreateChangeset(any(), any())).thenReturn(1)
        on(changesetManager.createChangeset(any(), any())).thenReturn(1)
        on(mapDataApi.uploadChanges(anyLong(), any()))
            .thenThrow(OsmConflictException(1, "", ""))
            .thenThrow(OsmConflictException(1, "", ""))

        uploader.upload(edit(elementType = ElementType.NODE, elementId = 1), mock())
    }

    @Test fun `handles changeset conflict exception`() {
        val node = node(1)
        on(mapDataApi.getNode(anyLong())).thenReturn(node)
        on(changesetManager.getOrCreateChangeset(any(), any())).thenReturn(1)
        on(changesetManager.createChangeset(any(), any())).thenReturn(1)
        doThrow(OsmConflictException(1, "", "")).doAnswer {  }
            .on(mapDataApi).uploadChanges(anyLong(), any())

        uploader.upload(edit(elementType = ElementType.NODE, elementId = 1), mock())
    }
}
