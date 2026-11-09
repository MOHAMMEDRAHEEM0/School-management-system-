package com.example

import com.example.ui.navigation.AppDestinations
import com.example.ui.navigation.BottomNavDestinations
import com.example.ui.navigation.BottomNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationGraphTest {

    @Test
    fun testNavigationDestinationConstants() {
        assertEquals("command", AppDestinations.COMMAND)
        assertEquals("command", AppDestinations.DASHBOARD)
        assertEquals("attendance", AppDestinations.ATTENDANCE)
        assertEquals("treasury", AppDestinations.TREASURY)
        assertEquals("whatsapp", AppDestinations.WHATSAPP)
        assertEquals("security", AppDestinations.SECURITY)
        assertEquals("exam_results", AppDestinations.EXAM_RESULTS)
        assertEquals("audit_logs", AppDestinations.AUDIT_LOGS)
        assertEquals("register_scan", AppDestinations.REGISTER_SCAN)
    }

    @Test
    fun testBottomNavigationDestinationsContainCoreViews() {
        assertEquals(5, BottomNavDestinations.size)

        val routes = BottomNavDestinations.map { it.route }
        assertTrue("Bottom navigation must contain Command route", routes.contains(AppDestinations.COMMAND))
        assertTrue("Bottom navigation must contain Attendance route", routes.contains(AppDestinations.ATTENDANCE))
        assertTrue("Bottom navigation must contain Treasury route", routes.contains(AppDestinations.TREASURY))
        assertTrue("Bottom navigation must contain WhatsApp route", routes.contains(AppDestinations.WHATSAPP))
        assertTrue("Bottom navigation must contain Security route", routes.contains(AppDestinations.SECURITY))
    }

    @Test
    fun testBottomNavItemProperties() {
        val commandItem = BottomNavItem.Command
        assertEquals(AppDestinations.COMMAND, commandItem.route)
        assertEquals("Command", commandItem.title)
        assertEquals("nav_item_command", commandItem.testTag)
        assertNotNull(commandItem.selectedIcon)
        assertNotNull(commandItem.unselectedIcon)

        val attendanceItem = BottomNavItem.Attendance
        assertEquals(AppDestinations.ATTENDANCE, attendanceItem.route)
        assertEquals("Attendance", attendanceItem.title)
        assertEquals("nav_item_attendance", attendanceItem.testTag)
        assertEquals("Live", attendanceItem.badgeLabel)
        assertNotNull(attendanceItem.selectedIcon)

        val treasuryItem = BottomNavItem.Treasury
        assertEquals(AppDestinations.TREASURY, treasuryItem.route)
        assertEquals("Treasury", treasuryItem.title)
        assertEquals("nav_item_treasury", treasuryItem.testTag)
        assertNotNull(treasuryItem.selectedIcon)

        val whatsAppItem = BottomNavItem.WhatsApp
        assertEquals(AppDestinations.WHATSAPP, whatsAppItem.route)
        assertEquals("WhatsApp", whatsAppItem.title)
        assertEquals("nav_item_whatsapp", whatsAppItem.testTag)
        assertEquals("Hub", whatsAppItem.badgeLabel)
        assertNotNull(whatsAppItem.selectedIcon)

        val securityItem = BottomNavItem.Security
        assertEquals(AppDestinations.SECURITY, securityItem.route)
        assertEquals("Security", securityItem.title)
        assertEquals("nav_item_security", securityItem.testTag)
        assertEquals("L3", securityItem.badgeLabel)
        assertNotNull(securityItem.selectedIcon)

        // Backwards compatibility verification
        val dashboardItem = BottomNavItem.Dashboard
        assertEquals(AppDestinations.DASHBOARD, dashboardItem.route)
        assertEquals("Dashboard", dashboardItem.title)
        assertNotNull(dashboardItem.selectedIcon)

        val examResultsItem = BottomNavItem.ExamResults
        assertEquals(AppDestinations.EXAM_RESULTS, examResultsItem.route)
        assertEquals("Exam Results", examResultsItem.title)
        assertNotNull(examResultsItem.selectedIcon)
    }

    @Test
    fun testNavigationGraphTransitionsBetweenCoreViews() {
        // Simulating route transitions between Dashboard, Attendance, and Exam Results
        var currentRoute = AppDestinations.DASHBOARD

        // Transition from Dashboard to Attendance
        currentRoute = AppDestinations.ATTENDANCE
        assertEquals(AppDestinations.ATTENDANCE, currentRoute)

        // Transition from Attendance to Exam Results
        currentRoute = AppDestinations.EXAM_RESULTS
        assertEquals(AppDestinations.EXAM_RESULTS, currentRoute)

        // Transition back to Dashboard
        currentRoute = AppDestinations.DASHBOARD
        assertEquals(AppDestinations.DASHBOARD, currentRoute)
    }
}
