package com.opencode.nfccardmanager.feature.verification

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.opencode.nfccardmanager.MainActivity
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.core.security.UserRole
import com.opencode.nfccardmanager.feature.read.ReadResultStore
import com.opencode.nfccardmanager.testutil.Phase6TestFixtures
import com.opencode.nfccardmanager.testutil.loginAs
import com.opencode.nfccardmanager.ui.test.AppTestTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Phase6PermissionAndAuthenticityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.activityRule.scenario.onActivity {
            SecurityManager.logout()
            AuditLogManager.clearAll()
            ReadResultStore.clear()
        }
        composeRule.waitForIdle()
        waitUntilTagExists(AppTestTags.LOGIN_USERNAME_INPUT)
    }

    @Test
    fun operator_only_sees_primary_and_management_entries() {
        composeRule.loginAs(Phase6TestFixtures.operator)
        waitUntilTagExists(AppTestTags.HOME_ROOT)

        assertTagExists(AppTestTags.HOME_SECTION_PRIMARY)
        assertTagMissing(AppTestTags.HOME_SECTION_HIGH_RISK)
        scrollHomeTo(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_ENTRY_READ)
        assertTagExists(AppTestTags.HOME_ENTRY_WRITE)
        assertTagMissing(AppTestTags.HOME_ENTRY_LOCK)
        assertTagMissing(AppTestTags.HOME_ENTRY_UNLOCK)
        assertTagMissing(AppTestTags.HOME_ENTRY_TEMPLATE)
        assertTagExists(AppTestTags.HOME_ENTRY_AUDIT)
        assertTagExists(AppTestTags.HOME_ENTRY_SETTINGS)
    }

    @Test
    fun supervisor_sees_high_risk_but_not_template_entry() {
        composeRule.loginAs(Phase6TestFixtures.supervisor)
        waitUntilTagExists(AppTestTags.HOME_ROOT)

        assertTagExists(AppTestTags.HOME_SECTION_PRIMARY)
        assertTagExists(AppTestTags.HOME_SECTION_HIGH_RISK)
        scrollHomeTo(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_ENTRY_LOCK)
        assertTagExists(AppTestTags.HOME_ENTRY_UNLOCK)
        assertTagMissing(AppTestTags.HOME_ENTRY_TEMPLATE)
        assertTagExists(AppTestTags.HOME_ENTRY_AUDIT)
    }

    @Test
    fun admin_sees_template_entry() {
        composeRule.loginAs(Phase6TestFixtures.admin)
        waitUntilTagExists(AppTestTags.HOME_ROOT)

        assertTagExists(AppTestTags.HOME_SECTION_PRIMARY)
        assertTagExists(AppTestTags.HOME_SECTION_HIGH_RISK)
        scrollHomeTo(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_ENTRY_TEMPLATE)
    }

    @Test
    fun auditor_only_keeps_management_section() {
        composeRule.loginAs(Phase6TestFixtures.auditor)
        waitUntilTagExists(AppTestTags.HOME_ROOT)

        assertTagMissing(AppTestTags.HOME_SECTION_PRIMARY)
        assertTagMissing(AppTestTags.HOME_SECTION_HIGH_RISK)
        scrollHomeTo(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagExists(AppTestTags.HOME_SECTION_MANAGEMENT)
        assertTagMissing(AppTestTags.HOME_ENTRY_READ)
        assertTagMissing(AppTestTags.HOME_ENTRY_WRITE)
        assertTagMissing(AppTestTags.HOME_ENTRY_LOCK)
        assertTagMissing(AppTestTags.HOME_ENTRY_UNLOCK)
        assertTagMissing(AppTestTags.HOME_ENTRY_TEMPLATE)
        assertTagExists(AppTestTags.HOME_ENTRY_AUDIT)
        assertTagExists(AppTestTags.HOME_ENTRY_SETTINGS)
    }

    @Test
    fun lock_and_unlock_pages_fall_back_to_denied_state_after_role_switch() {
        composeRule.loginAs(Phase6TestFixtures.supervisor)
        scrollHomeTo(AppTestTags.HOME_ENTRY_LOCK)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_LOCK)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_LOCK, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.LOCK_RISK_SUMMARY_CARD)

        composeRule.activityRule.scenario.onActivity {
            SecurityManager.switchRole(UserRole.AUDITOR)
        }
        composeRule.waitForIdle()

        waitUntilTagExists(AppTestTags.PERMISSION_DENIED_ROOT)
        assertTagMissing(AppTestTags.LOCK_RISK_SUMMARY_CARD)

        pressBack()

        composeRule.activityRule.scenario.onActivity {
            SecurityManager.switchRole(UserRole.SUPERVISOR)
        }
        composeRule.waitForIdle()
        scrollHomeTo(AppTestTags.HOME_ENTRY_UNLOCK)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_UNLOCK)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_UNLOCK, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.UNLOCK_BOUNDARY_CARD)

        composeRule.activityRule.scenario.onActivity {
            SecurityManager.switchRole(UserRole.OPERATOR)
        }
        composeRule.waitForIdle()

        waitUntilTagExists(AppTestTags.PERMISSION_DENIED_ROOT)
        assertTagMissing(AppTestTags.UNLOCK_BOUNDARY_CARD)
    }

    @Test
    fun unlock_demo_result_keeps_demo_only_authenticity_boundary() {
        composeRule.loginAs(Phase6TestFixtures.supervisor)
        scrollHomeTo(AppTestTags.HOME_ENTRY_UNLOCK)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_UNLOCK)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_UNLOCK, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.UNLOCK_BOUNDARY_CARD)
        assertTagExists(AppTestTags.UNLOCK_AUTHENTICITY_CARD)

        composeRule.onNodeWithTag(AppTestTags.UNLOCK_SIMULATE_SUCCESS_BUTTON, useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        waitUntilTagExists(AppTestTags.UNLOCK_RESULT_CARD)

        assertTagExists(AppTestTags.UNLOCK_RESULT_SOURCE)
        assertTagExists(AppTestTags.UNLOCK_DEMO_ONLY_BADGE)
    }

    private fun assertTagExists(tag: String) {
        assertTrue("Expected tag to exist: $tag", hasTag(tag))
    }

    private fun assertTagMissing(tag: String) {
        assertFalse("Expected tag to be absent: $tag", hasTag(tag))
    }

    private fun hasTag(tag: String): Boolean {
        return runCatching {
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        }.isSuccess
    }

    private fun waitUntilTagExists(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { hasTag(tag) }
    }

    private fun scrollHomeTo(tag: String) {
        composeRule.onNodeWithTag(AppTestTags.HOME_ROOT).performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }
}
