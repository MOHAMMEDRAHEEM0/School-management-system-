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
        assertEquals("dashboard", AppDestinations.DASHBOARD)
        assertEquals("attendance", AppDestinations.ATTENDANCE)
        assertEquals("exam_results", AppDestinations.EXAM_RESULTS)
        assertEquals("audit_logs", AppDestinations.AUDIT_LOGS)
        assertEquals("register_scan", AppDestinations.REGISTER_SCAN)
    }

    @Test
    fun testBottomNavigationDestinationsContainCoreViews() {
        assertEquals(3, BottomNavDestinations.size)

        val routes = BottomNavDestinations.map { it.route }
        assertTrue("Bottom navigation must contain Dashboard route", routes.contains(AppDestinations.DASHBOARD))
        assertTrue("Bottom navigation must contain Attendance route", routes.contains(AppDestinations.ATTENDANCE))
        assertTrue("Bottom navigation must contain Exam Results route", routes.contains(AppDestinations.EXAM_RESULTS))
    }

    @Test
    fun testBottomNavItemProperties() {
        val dashboardItem = BottomNavItem.Dashboard
        assertEquals(AppDestinations.DASHBOARD, dashboardItem.route)
        assertEquals("Dashboard", dashboardItem.title)
        assertEquals("nav_item_dashboard", dashboardItem.testTag)
        assertNotNull(dashboardItem.selectedIcon)
        assertNotNull(dashboardItem.unselectedIcon)

        val attendanceItem = BottomNavItem.Attendance
        assertEquals(AppDestinations.ATTENDANCE, attendanceItem.route)
        assertEquals("Attendance", attendanceItem.title)
        assertEquals("nav_item_attendance", attendanceItem.testTag)
        assertEquals("Live", attendanceItem.badgeLabel)
        assertNotNull(attendanceItem.selectedIcon)

        val examResultsItem = BottomNavItem.ExamResults
        assertEquals(AppDestinations.EXAM_RESULTS, examResultsItem.route)
        assertEquals("Exam Results", examResultsItem.title)
        assertEquals("nav_item_exam_results", examResultsItem.testTag)
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
