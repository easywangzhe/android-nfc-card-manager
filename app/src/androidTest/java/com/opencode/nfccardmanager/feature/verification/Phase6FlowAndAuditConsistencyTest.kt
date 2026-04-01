package com.opencode.nfccardmanager.feature.verification

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.opencode.nfccardmanager.MainActivity
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.database.AuditLogRecord
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.feature.read.ReadResultStore
import com.opencode.nfccardmanager.testutil.Phase6TestFixtures
import com.opencode.nfccardmanager.testutil.loginAs
import com.opencode.nfccardmanager.ui.test.AppTestTags
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Phase6FlowAndAuditConsistencyTest {
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
    fun read_result_keeps_recommendation_cta_and_authenticity_visible() {
        composeRule.loginAs(Phase6TestFixtures.operator)
        scrollHomeTo(AppTestTags.HOME_ENTRY_READ)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_READ)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_READ, useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("模拟识别卡片（仅演示）").performClick()

        waitUntilTagExists(AppTestTags.READ_RESULT_CARD)
        assertTagExists(AppTestTags.READ_AUTHENTICITY_BADGE)
        assertTagExists(AppTestTags.READ_RECOMMENDATION_CARD)
        assertTagExists(AppTestTags.READ_PRIMARY_CTA)
        assertTagExists(AppTestTags.READ_SECONDARY_CTA)
    }

    @Test
    fun write_demo_result_keeps_execution_verification_and_next_step_sections() {
        composeRule.loginAs(Phase6TestFixtures.operator)
        scrollHomeTo(AppTestTags.HOME_ENTRY_WRITE)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_WRITE)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_WRITE, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.WRITE_STAGE_CARD)
        scrollWriteTo(AppTestTags.WRITE_SIMULATE_SUCCESS_BUTTON)

        composeRule.onNodeWithTag(AppTestTags.WRITE_SIMULATE_SUCCESS_BUTTON, useUnmergedTree = true).performClick()

        scrollWriteTo(AppTestTags.WRITE_EXECUTION_SECTION)
        assertTagExists(AppTestTags.WRITE_VERIFICATION_SECTION)
        assertTagExists(AppTestTags.WRITE_NEXT_STEP_SECTION)
        assertTagExists(AppTestTags.WRITE_DEMO_ONLY_BADGE)
    }

    @Test
    fun format_error_keeps_what_happened_why_and_next_step_structure() {
        composeRule.loginAs(Phase6TestFixtures.operator)
        scrollHomeTo(AppTestTags.HOME_ENTRY_READ)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_READ)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_READ, useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("模拟识别卡片（仅演示）").performClick()
        waitUntilTagExists(AppTestTags.READ_PRIMARY_CTA)

        composeRule.onNodeWithTag(AppTestTags.READ_PRIMARY_CTA, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.FORMAT_STATUS_CARD)

        composeRule.onNodeWithTag(AppTestTags.FORMAT_START_BUTTON, useUnmergedTree = true).performClick()

        waitUntilTagExists(AppTestTags.FORMAT_RESULT_CARD)
        assertTagExists(AppTestTags.FORMAT_WHAT_HAPPENED_SECTION)
        assertTagExists(AppTestTags.FORMAT_WHY_SECTION)
        assertTagExists(AppTestTags.FORMAT_NEXT_STEP_SECTION)
    }

    @Test
    fun audit_detail_shows_who_stage_authenticity_impact_and_message_for_ui_generated_logs() {
        composeRule.loginAs(Phase6TestFixtures.supervisor)
        scrollHomeTo(AppTestTags.HOME_ENTRY_READ)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_READ)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_READ, useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("模拟识别卡片（仅演示）").performClick()
        waitUntilTagExists(AppTestTags.READ_RESULT_CARD)

        pressBack()
        pressBack()
        scrollHomeTo(AppTestTags.HOME_ENTRY_UNLOCK)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_UNLOCK)

        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_UNLOCK, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.UNLOCK_BOUNDARY_CARD)
        composeRule.onNodeWithTag(AppTestTags.UNLOCK_SIMULATE_SUCCESS_BUTTON, useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        waitUntilTagExists(AppTestTags.UNLOCK_RESULT_CARD)

        pressBack()
        scrollHomeTo(AppTestTags.HOME_ENTRY_AUDIT)
        waitUntilTagExists(AppTestTags.HOME_ENTRY_AUDIT)
        composeRule.onNodeWithTag(AppTestTags.HOME_ENTRY_AUDIT, useUnmergedTree = true).performClick()
        waitUntilTagExists(AppTestTags.AUDIT_LIST_ROOT)

        val targetLog = waitForLatestLog("UNLOCK")
        scrollAuditListTo(AppTestTags.auditListItem(targetLog.id))
        composeRule.onNodeWithTag(AppTestTags.auditListItem(targetLog.id), useUnmergedTree = true).performClick()

        waitUntilTagExists(AppTestTags.AUDIT_DETAIL_RESULT_CARD)
        assertTagExists(AppTestTags.AUDIT_DETAIL_WHO_SECTION)
        assertTagExists(AppTestTags.AUDIT_DETAIL_SEMANTICS_SECTION)
        assertTagExists(AppTestTags.AUDIT_DETAIL_IMPACT_SECTION)
        assertTagExists(AppTestTags.AUDIT_DETAIL_MESSAGE_SECTION)
        assertTrue("Expected unlock audit to stay demo-only", targetLog.authenticity.storageValue == "DEMO_ONLY")
    }

    private fun waitForLatestLog(operationType: String, timeoutMillis: Long = 5_000): AuditLogRecord {
        var result: AuditLogRecord? = null
        composeRule.waitUntil(timeoutMillis) {
            composeRule.activityRule.scenario.onActivity {
                result = AuditLogManager.list().firstOrNull { log -> log.operationType == operationType }
            }
            result != null
        }
        return checkNotNull(result)
    }

    private fun assertTagExists(tag: String) {
        assertTrue("Expected tag to exist: $tag", hasTag(tag))
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

    private fun scrollWriteTo(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag(AppTestTags.WRITE_ROOT).performScrollToNode(hasTestTag(tag))
                composeRule.waitForIdle()
            }.isSuccess
        }
    }

    private fun scrollAuditListTo(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag(AppTestTags.AUDIT_LIST_ROOT).performScrollToNode(hasTestTag(tag))
                composeRule.waitForIdle()
            }.isSuccess
        }
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }
}
