package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.AppListSectionType
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.repositories.models.TrafficType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListRulesTest {

    private fun app(
        name: String,
        access: Boolean = true,
        traffic: TrafficType = TrafficType.International,
        packageName: String = "pkg.$name"
    ): App = mockk(relaxed = true) {
        every { this@mockk.name } returns name
        every { this@mockk.access } returns access
        every { trafficType } returns traffic
        every { this@mockk.packageName } returns packageName
    }

    private fun names(apps: List<IApp>) = apps.map { it.name }

    @Test
    fun alphabeticIsOneSectionWithoutHeaderSortedByName() {
        val sections = AppListRules.sections(listOf(app("b"), app("a"), app("c")), AppsFilter.Alphabetic)

        assertEquals(1, sections.size)
        assertEquals(null, sections[0].type)
        assertEquals(listOf("a", "b", "c"), names(sections[0].apps))
    }

    @Test
    fun internetAccessSplitsAllowedThenBlocked() {
        val sections = AppListRules.sections(
            listOf(app("z", access = false), app("b"), app("a", access = false), app("c")),
            AppsFilter.InternetAccess
        )

        assertEquals(listOf(AppListSectionType.ALLOWED, AppListSectionType.BLOCKED), sections.map { it.type })
        assertEquals(listOf("b", "c"), names(sections[0].apps))
        assertEquals(listOf("a", "z"), names(sections[1].apps))
    }

    @Test
    fun emptySectionsAreOmitted() {
        val sections = AppListRules.sections(listOf(app("a")), AppsFilter.InternetAccess)

        assertEquals(listOf(AppListSectionType.ALLOWED), sections.map { it.type })
    }

    @Test
    fun trafficTypeFollowsTheTrafficTypeOrder() {
        val sections = AppListRules.sections(
            listOf(
                app("i", traffic = TrafficType.International),
                app("m", traffic = TrafficType.Messaging),
                app("f", traffic = TrafficType.Free),
                app("n2", traffic = TrafficType.National),
                app("n1", traffic = TrafficType.National)
            ),
            AppsFilter.TrafficType
        )

        assertEquals(
            listOf(
                AppListSectionType.FREE,
                AppListSectionType.NATIONAL,
                AppListSectionType.MESSAGING,
                AppListSectionType.INTERNATIONAL
            ),
            sections.map { it.type }
        )
        assertEquals(listOf("n1", "n2"), names(sections[1].apps))
    }

    @Test
    fun groupsUseTheTrafficTypeOfTheirMasterApp() {
        val master = app("master", traffic = TrafficType.Free, packageName = "group.pkg")
        val group = AppGroup("group.pkg", 1, "Grupo 1", mutableListOf(master), null, null)

        val sections = AppListRules.sections(listOf(group), AppsFilter.TrafficType)

        assertEquals(listOf(AppListSectionType.FREE), sections.map { it.type })
    }

    @Test
    fun searchMatchesNamesIgnoringCaseAndLooksInsideGroups() {
        val inGroup = app("WhatsApp Business")
        val group = AppGroup("group.pkg", 1, "Grupo 1", mutableListOf(inGroup, app("Otra")), null, null)

        val result = AppListRules.search(listOf(app("Facebook"), app("whatsapp"), group), "whats")

        assertEquals(listOf("whatsapp", "WhatsApp Business"), names(result))
    }

    @Test
    fun blankSearchReturnsTheListUnchanged() {
        val apps = listOf(app("b"), app("a"))

        assertEquals(apps, AppListRules.search(apps, " "))
        assertEquals(apps, AppListRules.search(apps, null))
    }
}
