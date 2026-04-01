package com.opencode.nfccardmanager.testutil

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.opencode.nfccardmanager.MainActivity
import com.opencode.nfccardmanager.core.security.UserRole
import com.opencode.nfccardmanager.ui.test.AppTestTags

data class Phase6DemoAccount(
    val username: String,
    val displayName: String,
    val role: UserRole,
)

typealias MainActivityComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

object Phase6TestFixtures {
    const val defaultPassword = "123456"

    val operator = Phase6DemoAccount(
        username = "operator",
        displayName = "一线操作员",
        role = UserRole.OPERATOR,
    )

    val supervisor = Phase6DemoAccount(
        username = "supervisor",
        displayName = "审核主管",
        role = UserRole.SUPERVISOR,
    )

    val admin = Phase6DemoAccount(
        username = "admin",
        displayName = "系统管理员",
        role = UserRole.ADMIN,
    )

    val auditor = Phase6DemoAccount(
        username = "auditor",
        displayName = "审计员",
        role = UserRole.AUDITOR,
    )

    val accounts = listOf(operator, supervisor, admin, auditor)

    fun accountFor(role: UserRole): Phase6DemoAccount {
        return accounts.first { it.role == role }
    }
}

fun MainActivityComposeRule.loginAs(
    account: Phase6DemoAccount,
    password: String = Phase6TestFixtures.defaultPassword,
) {
    onNodeWithTag(AppTestTags.LOGIN_USERNAME_INPUT).performTextClearance()
    onNodeWithTag(AppTestTags.LOGIN_USERNAME_INPUT).performTextInput(account.username)
    onNodeWithTag(AppTestTags.LOGIN_PASSWORD_INPUT).performTextClearance()
    onNodeWithTag(AppTestTags.LOGIN_PASSWORD_INPUT).performTextInput(password)
    onNodeWithTag(AppTestTags.LOGIN_SUBMIT_BUTTON).performClick()
    waitForIdle()
}
