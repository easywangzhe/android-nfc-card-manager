package com.opencode.nfccardmanager.feature.home

import com.opencode.nfccardmanager.core.security.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeShellContractTest {

    @Test
    fun `operator only gets primary and management entries`() {
        val sections = buildHomeSections(UserRole.OPERATOR)
        val destinations = sections.flatMap { section -> section.entries.map { entry -> entry.destination } }
        val sectionKinds = sections.map(HomeSection::kind)
        val bottomNav = buildBottomNavDestinations(UserRole.OPERATOR).map(BottomNavDestination::label)

        assertEquals(listOf(HomeSectionKind.PRIMARY, HomeSectionKind.MANAGEMENT), sectionKinds)
        assertTrue(destinations.contains(HomeEntryDestination.READ))
        assertTrue(destinations.contains(HomeEntryDestination.WRITE))
        assertTrue(destinations.contains(HomeEntryDestination.AUDIT))
        assertTrue(destinations.contains(HomeEntryDestination.SETTINGS))
        assertFalse(destinations.contains(HomeEntryDestination.LOCK))
        assertFalse(destinations.contains(HomeEntryDestination.UNLOCK))
        assertFalse(destinations.contains(HomeEntryDestination.TEMPLATE))
        assertFalse(bottomNav.contains("模板"))
    }

    @Test
    fun `supervisor gets high risk entries but not template`() {
        val sections = buildHomeSections(UserRole.SUPERVISOR)
        val destinations = sections.flatMap { section -> section.entries.map { entry -> entry.destination } }
        val sectionKinds = sections.map(HomeSection::kind)
        val bottomNav = buildBottomNavDestinations(UserRole.SUPERVISOR).map(BottomNavDestination::label)

        assertEquals(
            listOf(HomeSectionKind.PRIMARY, HomeSectionKind.HIGH_RISK, HomeSectionKind.MANAGEMENT),
            sectionKinds,
        )
        assertTrue(destinations.contains(HomeEntryDestination.LOCK))
        assertTrue(destinations.contains(HomeEntryDestination.UNLOCK))
        assertFalse(destinations.contains(HomeEntryDestination.TEMPLATE))
        assertFalse(bottomNav.contains("模板"))
    }

    @Test
    fun `admin gets all sections and template nav`() {
        val sections = buildHomeSections(UserRole.ADMIN)
        val destinations = sections.flatMap { section -> section.entries.map { entry -> entry.destination } }
        val sectionKinds = sections.map(HomeSection::kind)
        val bottomNav = buildBottomNavDestinations(UserRole.ADMIN).map(BottomNavDestination::label)

        assertEquals(
            listOf(HomeSectionKind.PRIMARY, HomeSectionKind.HIGH_RISK, HomeSectionKind.MANAGEMENT),
            sectionKinds,
        )
        assertTrue(destinations.contains(HomeEntryDestination.TEMPLATE))
        assertTrue(bottomNav.contains("模板"))
    }

    @Test
    fun `auditor only gets management entries without template nav`() {
        val sections = buildHomeSections(UserRole.AUDITOR)
        val sectionKinds = sections.map(HomeSection::kind)
        val destinations = sections.flatMap { section -> section.entries.map { entry -> entry.destination } }
        val bottomNav = buildBottomNavDestinations(UserRole.AUDITOR).map(BottomNavDestination::label)

        assertEquals(listOf(HomeSectionKind.MANAGEMENT), sectionKinds)
        assertEquals(
            listOf(HomeEntryDestination.AUDIT, HomeEntryDestination.SETTINGS),
            destinations,
        )
        assertFalse(bottomNav.contains("模板"))
    }
}
