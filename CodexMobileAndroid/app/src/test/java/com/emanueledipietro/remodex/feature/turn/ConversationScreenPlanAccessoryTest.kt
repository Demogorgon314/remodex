package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexConversationItem
import com.emanueledipietro.remodex.model.RemodexPlanState
import com.emanueledipietro.remodex.model.RemodexPlanStep
import com.emanueledipietro.remodex.model.RemodexPlanStepStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationScreenPlanAccessoryTest {
    @Test
    fun `timeline layout pins only the latest active plan and hides all plan rows from timeline`() {
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Wrap up", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )
        val activePlan = planItem(
            id = "plan-active",
            steps = listOf(
                RemodexPlanStep(id = "2", step = "Compare iOS behavior", status = RemodexPlanStepStatus.IN_PROGRESS),
                RemodexPlanStep(id = "3", step = "Patch Android", status = RemodexPlanStepStatus.PENDING),
            ),
        )
        val chatItem = RemodexConversationItem(
            id = "assistant-chat",
            speaker = ConversationSpeaker.ASSISTANT,
            text = "Working on it",
        )

        val layout = buildConversationTimelineLayout(
            listOf(completedPlan, chatItem, activePlan),
        )

        assertEquals("plan-active", layout.pinnedPlanItem?.id)
        assertEquals(listOf("assistant-chat"), layout.timelineItems.map(RemodexConversationItem::id))
    }

    @Test
    fun `timeline layout does not pin completed plans`() {
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val layout = buildConversationTimelineLayout(listOf(completedPlan))

        assertNull(layout.pinnedPlanItem)
        assertEquals(emptyList<RemodexConversationItem>(), layout.timelineItems)
    }

    @Test
    fun `plan accessory snapshot prioritizes current actionable step`() {
        val snapshot = planAccessorySnapshot(
            planItem(
                id = "plan",
                explanation = "High-level explanation",
                steps = listOf(
                    RemodexPlanStep(id = "1", step = "Finished", status = RemodexPlanStepStatus.COMPLETED),
                    RemodexPlanStep(id = "2", step = "Current task", status = RemodexPlanStepStatus.IN_PROGRESS),
                    RemodexPlanStep(id = "3", step = "Next task", status = RemodexPlanStepStatus.PENDING),
                ),
            ),
        )

        assertEquals("Current task", snapshot.summary)
        assertEquals(PlanAccessoryStatus.IN_PROGRESS, snapshot.status)
        assertEquals("1/3", snapshot.progressText)
    }

    @Test
    fun `plan accessory content description includes status progress and summary`() {
        val snapshot = planAccessorySnapshot(
            planItem(
                id = "plan",
                steps = listOf(
                    RemodexPlanStep(id = "1", step = "Investigate issue", status = RemodexPlanStepStatus.IN_PROGRESS),
                    RemodexPlanStep(id = "2", step = "Ship fix", status = RemodexPlanStepStatus.PENDING),
                ),
            ),
        )

        val description = planAccessoryContentDescription(snapshot)

        assertTrue(description.contains("Open active plan."))
        assertTrue(description.contains("In progress"))
        assertTrue(description.contains("0 of 2 complete"))
        assertTrue(description.contains("Investigate issue"))
    }

    private fun planItem(
        id: String,
        explanation: String? = null,
        steps: List<RemodexPlanStep>,
    ): RemodexConversationItem {
        return RemodexConversationItem(
            id = id,
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.PLAN,
            text = explanation ?: "Plan update",
            planState = RemodexPlanState(
                explanation = explanation,
                steps = steps,
            ),
        )
    }
}
