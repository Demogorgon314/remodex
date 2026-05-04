package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.feature.appshell.PlanComposerSessionUiState
import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexConversationItem
import com.emanueledipietro.remodex.model.RemodexPlanningMode
import com.emanueledipietro.remodex.model.RemodexPlanState
import com.emanueledipietro.remodex.model.RemodexPlanStep
import com.emanueledipietro.remodex.model.RemodexPlanStepStatus
import com.emanueledipietro.remodex.model.RemodexStructuredUserInputAnswer
import com.emanueledipietro.remodex.model.RemodexStructuredUserInputQuestion
import com.emanueledipietro.remodex.model.RemodexStructuredUserInputRequest
import com.emanueledipietro.remodex.model.RemodexStructuredUserInputResponse
import com.emanueledipietro.remodex.model.RemodexTurnTerminalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

class ConversationScreenPlanAccessoryTest {
    @Test
    fun `timeline layout pins active plan outside plan mode while keeping completed plan in timeline`() {
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
            activePlanningMode = RemodexPlanningMode.AUTO,
        )

        assertEquals("plan-active", layout.pinnedPlanItem?.id)
        assertEquals(
            listOf("plan-complete", "assistant-chat"),
            layout.timelineItems.map(RemodexConversationItem::id),
        )
    }

    @Test
    fun `timeline layout keeps active plan in timeline during plan mode`() {
        val activePlan = planItem(
            id = "plan-active",
            steps = listOf(
                RemodexPlanStep(id = "2", step = "Compare iOS behavior", status = RemodexPlanStepStatus.IN_PROGRESS),
                RemodexPlanStep(id = "3", step = "Patch Android", status = RemodexPlanStepStatus.PENDING),
            ),
            isStreaming = true,
        )
        val chatItem = RemodexConversationItem(
            id = "assistant-chat",
            speaker = ConversationSpeaker.ASSISTANT,
            text = "Working on it",
        )

        val layout = buildConversationTimelineLayout(
            listOf(chatItem, activePlan),
            activePlanningMode = RemodexPlanningMode.PLAN,
        )

        assertNull(layout.pinnedPlanItem)
        assertEquals(
            listOf("assistant-chat", "plan-active"),
            layout.timelineItems.map(RemodexConversationItem::id),
        )
    }

    @Test
    fun `timeline layout keeps completed system plans visible when nothing is pinned`() {
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val layout = buildConversationTimelineLayout(
            listOf(completedPlan),
            activePlanningMode = RemodexPlanningMode.PLAN,
        )

        assertNull(layout.pinnedPlanItem)
        assertEquals(listOf("plan-complete"), layout.timelineItems.map(RemodexConversationItem::id))
    }

    @Test
    fun `timeline layout pins checklist plan updates above the composer`() {
        val planUpdate = RemodexConversationItem(
            id = "plan-update",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.PLAN_UPDATE,
            text = "Track migration work",
            isStreaming = true,
            planState = RemodexPlanState(
                explanation = "Track migration work",
                steps = listOf(
                    RemodexPlanStep(id = "1", step = "Inspect Android path", status = RemodexPlanStepStatus.IN_PROGRESS),
                    RemodexPlanStep(id = "2", step = "Split plan semantics", status = RemodexPlanStepStatus.PENDING),
                ),
            ),
        )

        val layout = buildConversationTimelineLayout(
            listOf(planUpdate),
            activePlanningMode = RemodexPlanningMode.AUTO,
        )

        assertEquals("plan-update", layout.pinnedPlanItem?.id)
        assertTrue(layout.timelineItems.isEmpty())
    }

    @Test
    fun `timeline layout filters non streaming metadata only file change rows`() {
        val metadataOnlyFileChange = RemodexConversationItem(
            id = "file-change-metadata",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.FILE_CHANGE,
            text = """
                Status: completed

                Path: src/Empty.kt
                Kind: update
            """.trimIndent(),
        )

        val layout = buildConversationTimelineLayout(
            listOf(metadataOnlyFileChange),
            activePlanningMode = RemodexPlanningMode.AUTO,
        )

        assertTrue(layout.timelineItems.isEmpty())
    }

    @Test
    fun `timeline layout keeps streaming metadata only file change rows visible`() {
        val metadataOnlyFileChange = RemodexConversationItem(
            id = "file-change-streaming",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.FILE_CHANGE,
            text = """
                Status: in_progress

                Path: src/Empty.kt
                Kind: update
            """.trimIndent(),
            isStreaming = true,
        )

        val layout = buildConversationTimelineLayout(
            listOf(metadataOnlyFileChange),
            activePlanningMode = RemodexPlanningMode.AUTO,
        )

        assertEquals(listOf("file-change-streaming"), layout.timelineItems.map(RemodexConversationItem::id))
    }

    @Test
    fun `timeline layout hides composer takeover prompt while keeping completed plan visible`() {
        val prompt = promptItem(id = "prompt")
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val layout = buildConversationTimelineLayout(
            messages = listOf(prompt, completedPlan),
            hiddenPromptItemId = "prompt",
            activePlanningMode = RemodexPlanningMode.PLAN,
        )

        assertEquals(listOf("plan-complete"), layout.timelineItems.map(RemodexConversationItem::id))
    }

    @Test
    fun `timeline layout pins streaming system plan outside plan mode`() {
        val streamingPlan = planItem(
            id = "plan-streaming",
            steps = emptyList(),
            isStreaming = true,
        )

        val layout = buildConversationTimelineLayout(
            listOf(streamingPlan),
            activePlanningMode = RemodexPlanningMode.AUTO,
        )

        assertEquals("plan-streaming", layout.pinnedPlanItem?.id)
        assertTrue(layout.timelineItems.isEmpty())
    }

    @Test
    fun `timeline empty state presentation surfaces pinned plan when timeline has no rows`() {
        val pinnedPlan = planItem(
            id = "plan-active",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Patch Android", status = RemodexPlanStepStatus.IN_PROGRESS),
            ),
        )

        val presentation = resolveConversationTimelineEmptyStatePresentation(
            timelineItems = emptyList(),
            pinnedPlanItem = pinnedPlan,
            takeoverPromptItem = null,
        )

        assertEquals(
            ConversationTimelineEmptyStatePresentation.PinnedPlan(
                snapshot = planAccessorySnapshot(pinnedPlan),
            ),
            presentation,
        )
    }

    @Test
    fun `timeline empty state presentation prioritizes pending structured input over pinned plan`() {
        val pinnedPlan = planItem(
            id = "plan-active",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Patch Android", status = RemodexPlanStepStatus.IN_PROGRESS),
            ),
        )
        val prompt = promptItem(id = "prompt")

        val presentation = resolveConversationTimelineEmptyStatePresentation(
            timelineItems = emptyList(),
            pinnedPlanItem = pinnedPlan,
            takeoverPromptItem = prompt,
        )

        assertEquals(
            ConversationTimelineEmptyStatePresentation.StructuredUserInput(questionCount = 1),
            presentation,
        )
    }

    @Test
    fun `timeline empty state presentation stays welcome when completed plan remains in timeline`() {
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val presentation = resolveConversationTimelineEmptyStatePresentation(
            timelineItems = listOf(completedPlan),
            pinnedPlanItem = null,
            takeoverPromptItem = null,
        )

        assertEquals(ConversationTimelineEmptyStatePresentation.Welcome, presentation)
    }

    @Test
    fun `timeline layout keeps streaming system plan in timeline during plan mode`() {
        val streamingPlan = planItem(
            id = "plan-streaming",
            steps = emptyList(),
            isStreaming = true,
        )

        val layout = buildConversationTimelineLayout(
            listOf(streamingPlan),
            activePlanningMode = RemodexPlanningMode.PLAN,
        )

        assertNull(layout.pinnedPlanItem)
        assertEquals(listOf("plan-streaming"), layout.timelineItems.map(RemodexConversationItem::id))
    }

    @Test
    fun `plan composer flow surfaces remote prompt before completed plan`() {
        val anchor = assistantMessage(id = "anchor")
        val prompt = promptItem(id = "prompt")
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val snapshot = resolvePlanComposerFlow(
            messages = listOf(anchor, prompt, completedPlan),
            session = PlanComposerSessionUiState(anchorMessageId = "anchor"),
            latestTurnTerminalState = null,
            activePlanningMode = RemodexPlanningMode.PLAN,
            hasQueuedFollowUps = false,
        )

        assertEquals("prompt", snapshot.takeoverPromptItem?.id)
    }

    @Test
    fun `plan composer flow does not keep resolved prompt in composer takeover`() {
        val anchor = assistantMessage(id = "anchor")
        val resolvedPrompt = promptItem(id = "prompt").copy(
            text = "Asked 1 question",
            isStreaming = false,
        )

        val snapshot = resolvePlanComposerFlow(
            messages = listOf(anchor, resolvedPrompt),
            session = PlanComposerSessionUiState(anchorMessageId = "anchor"),
            latestTurnTerminalState = null,
            activePlanningMode = RemodexPlanningMode.PLAN,
            hasQueuedFollowUps = false,
        )

        assertNull(snapshot.takeoverPromptItem)
    }

    @Test
    fun `plan composer flow leaves completed plans in timeline for inline implementation`() {
        val anchor = assistantMessage(id = "anchor")
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val snapshot = resolvePlanComposerFlow(
            messages = listOf(anchor, completedPlan),
            session = PlanComposerSessionUiState(anchorMessageId = "anchor"),
            latestTurnTerminalState = RemodexTurnTerminalState.COMPLETED,
            activePlanningMode = RemodexPlanningMode.PLAN,
            hasQueuedFollowUps = false,
        )

        assertNull(snapshot.takeoverPromptItem)
    }

    @Test
    fun `plan composer flow does not surface completed plan before turn completion`() {
        val anchor = assistantMessage(id = "anchor")
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val snapshot = resolvePlanComposerFlow(
            messages = listOf(anchor, completedPlan),
            session = PlanComposerSessionUiState(anchorMessageId = "anchor"),
            latestTurnTerminalState = null,
            activePlanningMode = RemodexPlanningMode.PLAN,
            hasQueuedFollowUps = false,
        )

        assertNull(snapshot.takeoverPromptItem)
    }

    @Test
    fun `plan composer flow does not surface completed plan when queued follow ups exist`() {
        val anchor = assistantMessage(id = "anchor")
        val completedPlan = planItem(
            id = "plan-complete",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Done", status = RemodexPlanStepStatus.COMPLETED),
            ),
        )

        val snapshot = resolvePlanComposerFlow(
            messages = listOf(anchor, completedPlan),
            session = PlanComposerSessionUiState(anchorMessageId = "anchor"),
            latestTurnTerminalState = RemodexTurnTerminalState.COMPLETED,
            activePlanningMode = RemodexPlanningMode.PLAN,
            hasQueuedFollowUps = true,
        )
        assertNull(snapshot.takeoverPromptItem)
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
    fun `proposed plan presentation extracts official envelope body and summary`() {
        val item = RemodexConversationItem(
            id = "plan-result",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.PLAN,
            text = "Intro\n<proposed_plan>\n# Final plan\n- First change\n- Second change\n</proposed_plan>\nPostscript",
        )

        val presentation = proposedPlanPresentation(item)

        assertEquals("# Final plan\n- First change\n- Second change", presentation?.body)
        assertEquals("Final plan", presentation?.summary)
        assertEquals("Intro\nPostscript", strippedProposedPlanText(item.text))
    }

    @Test
    fun `proposed plan presentation uses completed plan item text as authoritative body`() {
        val item = RemodexConversationItem(
            id = "plan-result",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.PLAN,
            text = "# Final plan\n- First change",
        )

        val presentation = proposedPlanPresentation(item)

        assertEquals("# Final plan\n- First change", presentation?.body)
        assertEquals("Final plan", presentation?.summary)
    }

    @Test
    fun `proposed plan presentation ignores checklist progress plans`() {
        val item = planItem(
            id = "plan-progress",
            explanation = "Plan update",
            steps = listOf(
                RemodexPlanStep(id = "1", step = "Still planning", status = RemodexPlanStepStatus.IN_PROGRESS),
            ),
        )

        assertNull(proposedPlanPresentation(item))
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

    @Test
    fun `plan conversation row collapse heuristic keeps short plans expanded`() {
        assertTrue(
            shouldCollapsePlanConversationRow(
                bodyText = "Short plan body",
                explanationText = "Short explanation",
                stepCount = 2,
            ).not(),
        )
    }

    @Test
    fun `plan conversation row collapse heuristic collapses long plans`() {
        assertTrue(
            shouldCollapsePlanConversationRow(
                bodyText = "x".repeat(421),
                explanationText = "Short explanation",
                stepCount = 2,
            ),
        )
        assertTrue(
            shouldCollapsePlanConversationRow(
                bodyText = "Short body",
                explanationText = "y".repeat(161),
                stepCount = 2,
            ),
        )
        assertTrue(
            shouldCollapsePlanConversationRow(
                bodyText = "Short body",
                explanationText = "Short explanation",
                stepCount = 5,
            ),
        )
    }

    @Test
    fun `structured user input summary label pluralizes by request question count`() {
        assertEquals("Asked 1 question", structuredUserInputSummaryLabel(1))
        assertEquals("Asked 3 questions", structuredUserInputSummaryLabel(3))
    }

    @Test
    fun `structured user input answer lines return recorded non secret answers`() {
        val question = RemodexStructuredUserInputQuestion(
            id = "path",
            header = "",
            question = "Which path should we take?",
        )

        val answerLines = structuredUserInputAnswerLines(
            question = question,
            response = RemodexStructuredUserInputResponse(
                answersByQuestionId = mapOf(
                    "path" to RemodexStructuredUserInputAnswer(
                        answers = listOf("Android local persistence"),
                    ),
                ),
            ),
        )

        assertEquals(listOf("Android local persistence"), answerLines)
    }

    @Test
    fun `structured user input answer lines hide secret answers`() {
        val question = RemodexStructuredUserInputQuestion(
            id = "token",
            header = "",
            question = "API token",
            isSecret = true,
        )

        val answerLines = structuredUserInputAnswerLines(
            question = question,
            response = RemodexStructuredUserInputResponse(
                answersByQuestionId = mapOf(
                    "token" to RemodexStructuredUserInputAnswer(
                        answers = listOf("sk-live-123"),
                    ),
                ),
            ),
        )

        assertEquals(listOf("Answered"), answerLines)
    }

    private fun planItem(
        id: String,
        explanation: String? = null,
        steps: List<RemodexPlanStep>,
        isStreaming: Boolean = false,
    ): RemodexConversationItem {
        return RemodexConversationItem(
            id = id,
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.PLAN,
            text = explanation ?: "Plan update",
            isStreaming = isStreaming,
            planState = RemodexPlanState(
                explanation = explanation,
                steps = steps,
            ),
        )
    }

    private fun promptItem(id: String): RemodexConversationItem {
        return RemodexConversationItem(
            id = id,
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.USER_INPUT_PROMPT,
            text = "Needs input",
            structuredUserInputRequest = RemodexStructuredUserInputRequest(
                requestId = JsonPrimitive("request-1"),
                questions = listOf(
                    RemodexStructuredUserInputQuestion(
                        id = "q1",
                        header = "",
                        question = "What should we do?",
                    ),
                ),
            ),
        )
    }

    private fun assistantMessage(id: String): RemodexConversationItem {
        return RemodexConversationItem(
            id = id,
            speaker = ConversationSpeaker.ASSISTANT,
            text = "Anchor",
        )
    }
}
